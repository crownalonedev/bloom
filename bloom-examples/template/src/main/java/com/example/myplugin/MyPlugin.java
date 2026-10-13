package com.example.myplugin;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import org.bukkit.plugin.java.JavaPlugin;

// @snippet:install.plugin
public final class MyPlugin extends JavaPlugin {
    private Bloom bloom;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        bloom.messages().send(getServer().getConsoleSender(), "&dHello from Bloom on " + bloom.platform().describe());
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown();
        }
    }
}
// @end
