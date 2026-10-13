package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.event.Subscription;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Event snippets used in the documentation. */
final class EventSnippets {

    private EventSnippets() {}

    static void subscribe(Bloom bloom) {
        // @snippet:events.subscribe
        Subscription welcome = bloom.events()
                .subscribe(PlayerJoinEvent.class, event -> bloom.messages().send(event.getPlayer(), "&dWelcome!"));
        welcome.unsubscribe(); // or let Bloom remove it on shutdown
        // @end
    }

    static void builder(Bloom bloom) {
        // @snippet:events.builder
        bloom.events()
                .listen(EntityDamageByEntityEvent.class)
                .priority(EventPriority.HIGH)
                .ignoreCancelled()
                .filter(event -> event.getDamager().getType().name().equals("PLAYER"))
                .handler(event -> event.setDamage(event.getDamage() * 0.8));

        bloom.events()
                .listen(BlockBreakEvent.class)
                .limit(1) // unsubscribes itself after the first matching event
                .handler(event -> event.getPlayer().sendMessage("Your first block!"));
        // @end
    }

    // @snippet:events.listener
    static final class QuitListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            event.setQuitMessage(null);
        }
    }

    static void register(Bloom bloom) {
        bloom.events().register(new QuitListener()); // unregistered when Bloom shuts down
    }
    // @end
}
