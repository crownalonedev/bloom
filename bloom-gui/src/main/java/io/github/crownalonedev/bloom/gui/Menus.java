package io.github.crownalonedev.bloom.gui;

import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.event.Subscription;
import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Opens and manages {@link Menu menus}. Obtain with {@link #of(Bloom)}; the service registers its own listeners, so
 * no per-menu listener registration is needed.
 *
 * <h2>Safety guarantees</h2>
 *
 * <ul>
 *   <li>Every click and drag while a Bloom menu is open is cancelled (including shift-click, number-key swaps,
 *       double-click collection and clicks in the player's own inventory), and the cancellation is re-applied at
 *       {@code HIGHEST} priority if another plugin un-cancels it. Items cannot be taken out of or put into a menu.
 *   <li>Button handlers run at most once per click and are debounced per player (100 ms by default; see
 *       {@link #clickDebounce(Duration)}) to stop double-triggered purchases from spam clicking.
 *   <li>On legacy servers the player's inventory is resynchronised after cancelled clicks to remove client-side
 *       ghost items.
 *   <li>Sessions are cleaned up on close, disconnect and plugin shutdown; open menus are closed on shutdown.
 * </ul>
 *
 * <p>All methods must be called on the main thread.
 *
 * @since 0.1.0
 */
public final class Menus implements AutoCloseable {

    private static final int MAX_HISTORY = 16;

    private final Bloom bloom;
    private final Logger logger;
    private final PlayerPlatform players;
    private final BloomScheduler scheduler;
    private final boolean legacy;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final List<Subscription> subscriptions = new ArrayList<Subscription>();
    private long debounceNanos = Duration.ofMillis(100).toNanos();
    private boolean closed;
    private boolean warnedUncancel;

    /** Per-player navigation state. */
    static final class Session {
        MenuView current;
        MenuView opening;
        final Deque<MenuView> history = new ArrayDeque<MenuView>();
        boolean transitioning;
        long lastClickNanos;
    }

    private Menus(Bloom bloom) {
        this.bloom = bloom;
        this.logger = bloom.plugin().getLogger();
        this.players = bloom.adapter().players();
        this.scheduler = bloom.scheduler();
        this.legacy = bloom.adapter().family() == PlatformFamily.LEGACY;
        subscriptions.add(bloom.events()
                .listen(InventoryClickEvent.class)
                .priority(EventPriority.LOWEST)
                .handler(this::onClick));
        subscriptions.add(bloom.events()
                .listen(InventoryClickEvent.class)
                .priority(EventPriority.HIGHEST)
                .handler(this::reassertCancel));
        subscriptions.add(bloom.events()
                .listen(InventoryDragEvent.class)
                .priority(EventPriority.LOWEST)
                .handler(this::onDrag));
        subscriptions.add(bloom.events()
                .listen(InventoryCloseEvent.class)
                .priority(EventPriority.MONITOR)
                .handler(this::onClose));
        subscriptions.add(bloom.events()
                .listen(PlayerQuitEvent.class)
                .priority(EventPriority.MONITOR)
                .handler(this::onQuit));
    }

    /**
     * Returns the menu service of a Bloom instance, creating it (and registering its listeners) on first use.
     *
     * @param bloom running Bloom instance
     * @return the service
     */
    public static Menus of(final Bloom bloom) {
        Objects.requireNonNull(bloom, "bloom");
        return bloom.services().getOrRegister(Menus.class, () -> new Menus(bloom));
    }

    /**
     * Sets the minimum time between two handled button clicks of the same player.
     *
     * @param debounce debounce, {@link Duration#ZERO} to disable
     * @return this service
     */
    public Menus clickDebounce(Duration debounce) {
        if (debounce.isNegative()) {
            throw new IllegalArgumentException("Debounce must not be negative");
        }
        this.debounceNanos = debounce.toNanos();
        return this;
    }

    /**
     * Opens a menu. If the player is viewing another Bloom menu, it is kept in the history for {@link #back}.
     *
     * <p>Do not call this from inside a click handler; use {@link ClickContext#open(Menu)} instead.
     *
     * @param player viewer
     * @param menu menu to open
     * @return the player's view; {@link MenuView#isOpen()} is {@code false} if another plugin cancelled the opening
     */
    public MenuView open(Player player, Menu menu) {
        scheduler.ensurePrimaryThread("Menus.open");
        ensureOpen();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            session = new Session();
            sessions.put(player.getUniqueId(), session);
        }
        MenuView previous = session.current != null && isShowing(player, session.current) ? session.current : null;
        MenuView view = new MenuView(this, player, Objects.requireNonNull(menu, "menu"));
        if (show(player, session, view, false)) {
            if (previous != null) {
                session.history.push(previous);
                while (session.history.size() > MAX_HISTORY) {
                    session.history.removeLast();
                }
            }
        }
        return view;
    }

    /**
     * Returns to the previous menu in the player's history.
     *
     * @param player viewer
     * @return {@code true} if a previous menu was opened
     */
    public boolean back(Player player) {
        scheduler.ensurePrimaryThread("Menus.back");
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.history.isEmpty()) {
            return false;
        }
        MenuView previous = session.history.pop();
        return show(player, session, previous, false);
    }

    /**
     * Closes the player's Bloom menu, if one is open.
     *
     * @param player viewer
     */
    public void close(Player player) {
        scheduler.ensurePrimaryThread("Menus.close");
        Session session = sessions.get(player.getUniqueId());
        if (session != null && session.current != null && isShowing(player, session.current)) {
            player.closeInventory();
        }
    }

    /**
     * Re-renders the player's open menu.
     *
     * @param player viewer
     */
    public void refresh(Player player) {
        Optional<MenuView> view = view(player);
        if (view.isPresent()) {
            view.get().refresh();
        }
    }

    /**
     * Re-renders a menu for everyone currently viewing it.
     *
     * @param menu menu
     * @return number of refreshed viewers
     */
    public int refreshAll(Menu menu) {
        scheduler.ensurePrimaryThread("Menus.refreshAll");
        int count = 0;
        for (Session session : new ArrayList<Session>(sessions.values())) {
            if (session.current != null && session.current.menu() == menu && session.current.isOpen()) {
                session.current.refresh();
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the player's open Bloom menu view.
     *
     * @param player player
     * @return the view, or empty
     */
    public Optional<MenuView> view(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.current == null || !session.current.isOpen()) {
            return Optional.empty();
        }
        return Optional.of(session.current);
    }

    /**
     * Returns how many players have a Bloom menu open.
     *
     * @return open view count
     */
    public int openCount() {
        int count = 0;
        for (Session session : sessions.values()) {
            if (session.current != null && session.current.isOpen()) {
                count++;
            }
        }
        return count;
    }

    /** Closes all open menus and unregisters the listeners. Called automatically when Bloom shuts down. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        for (Map.Entry<UUID, Session> entry : new ArrayList<Map.Entry<UUID, Session>>(sessions.entrySet())) {
            Player player = bloom.plugin().getServer().getPlayer(entry.getKey());
            MenuView current = entry.getValue().current;
            if (player != null && current != null && isShowing(player, current)) {
                player.closeInventory();
            }
            if (current != null && current.isOpen()) {
                closeView(current);
            }
        }
        sessions.clear();
        closed = true;
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
    }

    // ---- deferred navigation used by ClickContext ----------------------------------------------------------

    void openLater(final Player player, final Menu menu) {
        scheduler.sync(() -> {
            if (!closed && player.isOnline()) {
                open(player, menu);
            }
        });
    }

    boolean backLater(final Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.history.isEmpty()) {
            return false;
        }
        scheduler.sync(() -> {
            if (!closed && player.isOnline()) {
                back(player);
            }
        });
        return true;
    }

    void closeLater(final Player player) {
        scheduler.sync(() -> {
            if (!closed && player.isOnline()) {
                close(player);
            }
        });
    }

    // ---- rendering ----------------------------------------------------------------------------------------

    private boolean show(Player player, Session session, MenuView view, boolean reopen) {
        Inventory inventory = render(view, true);
        session.opening = view;
        session.transitioning = true;
        try {
            player.openInventory(inventory);
        } finally {
            session.transitioning = false;
            session.opening = null;
        }
        if (!isShowing(player, view)) {
            // Another plugin cancelled InventoryOpenEvent. The previous menu (if any) was closed by the server.
            view.open(false);
            session.current = null;
            session.history.clear();
            sessions.remove(player.getUniqueId());
            return false;
        }
        session.current = view;
        view.open(true);
        if (!reopen) {
            startRefreshTask(view);
            fire(view.menu().openHandlers(), view, "onOpen");
        }
        return true;
    }

    void refreshView(final MenuView view) {
        scheduler.ensurePrimaryThread("MenuView.refresh");
        if (!view.isOpen()) {
            return;
        }
        String previousTitle = view.renderedTitle();
        String newTitle = renderTitle(view).content();
        if (newTitle.equals(previousTitle)) {
            render(view, false);
            return;
        }
        // Titles cannot be changed in place on every version: reopen with a new inventory on the next tick.
        scheduler.sync(() -> {
            Session session = sessions.get(view.player().getUniqueId());
            if (!closed && session != null && session.current == view && view.isOpen()) {
                show(view.player(), session, view, true);
            }
        });
    }

    private FormattedText renderTitle(MenuView view) {
        Messenger messenger = bloom.messages();
        if (view.menu().format() != null) {
            messenger = messenger.withFormat(view.menu().format());
        }
        return messenger.render(
                view.menu().title(),
                Placeholder.of("player", view.player().getName()),
                Placeholder.of("page", view.page() + 1),
                Placeholder.of("pages", view.pageCount()));
    }

    private Inventory render(MenuView view, boolean createInventory) {
        Menu menu = view.menu();
        int size = menu.size();
        Button[] slotButtons = new Button[size];
        ItemStack[] contents = new ItemStack[size];

        for (Map.Entry<Integer, Button> entry : menu.buttons().entrySet()) {
            place(view, entry.getKey(), entry.getValue(), slotButtons, contents);
        }
        if (menu.isPaginated()) {
            List<Button> content = safePage(view);
            int[] slots = menu.contentSlots();
            int pages = Math.max(1, (content.size() + slots.length - 1) / slots.length);
            view.pageCount(pages);
            int offset = view.page() * slots.length;
            for (int i = 0; i < slots.length; i++) {
                int slot = slots[i];
                slotButtons[slot] = null;
                contents[slot] = null;
                int index = offset + i;
                if (index < content.size()) {
                    place(view, slot, content.get(index), slotButtons, contents);
                }
            }
            if (menu.previousButton() != null && view.hasPreviousPage()) {
                place(view, menu.previousSlot(), menu.previousButton(), slotButtons, contents);
            }
            if (menu.nextButton() != null && view.hasNextPage()) {
                place(view, menu.nextSlot(), menu.nextButton(), slotButtons, contents);
            }
        }
        if (menu.filler() != null) {
            for (int slot = 0; slot < size; slot++) {
                if (contents[slot] == null) {
                    place(view, slot, menu.filler(), slotButtons, contents);
                }
            }
        }
        view.slotButtons(slotButtons);

        Inventory inventory = view.inventory();
        if (createInventory || inventory == null) {
            MenuHolder holder = new MenuHolder(view);
            FormattedText title = renderTitle(view);
            inventory = players.createInventory(holder, size, title);
            holder.inventory(inventory);
            view.attach(holder, inventory, title.content());
        }
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, contents[slot]);
        }
        return inventory;
    }

    private List<Button> safePage(MenuView view) {
        try {
            List<Button> content = view.menu().pageSource().apply(view);
            return content == null ? new ArrayList<Button>() : content;
        } catch (RuntimeException e) {
            logger.log(
                    Level.SEVERE, "Menu page source failed for " + view.player().getName(), e);
            return new ArrayList<Button>();
        }
    }

    private void place(MenuView view, int slot, Button button, Button[] slotButtons, ItemStack[] contents) {
        Button.Resolved resolved;
        try {
            resolved = button.resolve(view);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Menu button in slot " + slot + " failed to render", e);
            return;
        }
        if (resolved == null) {
            return;
        }
        contents[slot] = resolved.item;
        slotButtons[slot] = resolved.clickable ? button : null;
    }

    private void startRefreshTask(final MenuView view) {
        long ticks = view.menu().refreshTicks();
        if (ticks <= 0) {
            return;
        }
        view.refreshTask(scheduler.syncRepeating(
                task -> {
                    if (!view.isOpen()) {
                        task.cancel();
                    } else {
                        view.refresh();
                    }
                },
                ticks,
                ticks));
    }

    private void closeView(MenuView view) {
        view.open(false);
        ScheduledTask task = view.refreshTask();
        if (task != null) {
            task.cancel();
            view.refreshTask(null);
        }
        fire(view.menu().closeHandlers(), view, "onClose");
    }

    private void fire(List<Consumer<MenuView>> handlers, MenuView view, String phase) {
        for (Consumer<MenuView> handler : handlers) {
            try {
                handler.accept(view);
            } catch (RuntimeException e) {
                logger.log(
                        Level.SEVERE,
                        "Menu " + phase + " handler failed for " + view.player().getName(),
                        e);
            }
        }
    }

    private boolean isShowing(Player player, MenuView view) {
        if (view.holder() == null) {
            return false;
        }
        Inventory top = players.openTopInventory(player);
        return top != null && players.holder(top) == view.holder();
    }

    private MenuHolder menuHolder(Inventory inventory) {
        InventoryHolder holder = players.holder(inventory);
        return holder instanceof MenuHolder ? (MenuHolder) holder : null;
    }

    // ---- listeners ----------------------------------------------------------------------------------------

    private void onClick(InventoryClickEvent event) {
        MenuHolder holder = menuHolder(event.getInventory());
        if (holder == null) {
            return;
        }
        event.setCancelled(true);
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        final Player player = (Player) who;
        if (legacy) {
            scheduler.sync(player::updateInventory);
        }
        MenuView view = holder.view();
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= view.menu().size()) {
            return;
        }
        ClickKind kind = ClickKind.fromBukkit(event.getClick().name());
        if (kind == null) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        Button[] slotButtons = view.slotButtons();
        if (session == null || session.current != view || slotButtons == null) {
            return;
        }
        Button button = slotButtons[raw];
        if (button == null || !button.hasHandler()) {
            return;
        }
        long now = System.nanoTime();
        if (debounceNanos > 0 && session.lastClickNanos != 0 && now - session.lastClickNanos < debounceNanos) {
            return;
        }
        session.lastClickNanos = now;
        try {
            button.click(new ClickContext(view, raw, kind, event.getHotbarButton()));
        } catch (RuntimeException e) {
            logger.log(
                    Level.SEVERE, "Menu click handler failed (slot " + raw + ", player " + player.getName() + ")", e);
        }
    }

    private void reassertCancel(InventoryClickEvent event) {
        if (!event.isCancelled() && menuHolder(event.getInventory()) != null) {
            event.setCancelled(true);
            if (!warnedUncancel) {
                warnedUncancel = true;
                logger.warning("Another plugin un-cancelled a click in a Bloom menu; Bloom cancelled it again to "
                        + "prevent item duplication. (This warning is shown once.)");
            }
        }
    }

    private void onDrag(InventoryDragEvent event) {
        if (menuHolder(event.getInventory()) != null) {
            event.setCancelled(true);
        }
    }

    private void onClose(InventoryCloseEvent event) {
        MenuHolder holder = menuHolder(event.getInventory());
        if (holder == null) {
            return;
        }
        MenuView view = holder.view();
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            if (view.isOpen()) {
                closeView(view);
            }
            return;
        }
        if (session.transitioning) {
            // Navigating to another menu (or reopening this one): keep the session and history.
            if (view != session.opening && view.isOpen()) {
                closeView(view);
            }
            return;
        }
        if (view.isOpen()) {
            closeView(view);
        }
        if (session.current == view) {
            session.current = null;
            session.history.clear();
            sessions.remove(event.getPlayer().getUniqueId());
        }
    }

    private void onQuit(PlayerQuitEvent event) {
        Session session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null && session.current != null && session.current.isOpen()) {
            closeView(session.current);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("The menu service has shut down.");
        }
    }
}
