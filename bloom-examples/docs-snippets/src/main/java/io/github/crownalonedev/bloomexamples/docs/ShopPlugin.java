package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.items.Items;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

// @snippet:home.plugin
public final class ShopPlugin extends JavaPlugin {
    private Bloom bloom;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this); // detects 1.7.10 ... 26.x and loads the matching adapter
        Items items = Items.of(bloom);

        Menu shop = Menu.builder()
                .title("&dBlossom Shop")
                .rows(3)
                .button(
                        13,
                        Button.of(items.builder("COOKED_BEEF").name("&6Feed me").build(), click -> {
                            click.player().setFoodLevel(20);
                            bloom.messages().actionBar(click.player(), "&aYum!");
                            click.close();
                        }))
                .build();

        bloom.events()
                .subscribe(
                        PlayerJoinEvent.class,
                        event -> bloom.scheduler()
                                .syncLater(() -> Menus.of(bloom).open(event.getPlayer(), shop), 20));
    }

    @Override
    public void onDisable() {
        bloom.shutdown(); // closes menus, cancels tasks, unregisters listeners
    }
}
// @end
