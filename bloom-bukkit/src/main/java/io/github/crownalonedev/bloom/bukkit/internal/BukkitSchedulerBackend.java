package io.github.crownalonedev.bloom.bukkit.internal;

import io.github.crownalonedev.bloom.common.scheduler.SchedulerBackend;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler backend on top of {@link BukkitScheduler}. Uses only methods that exist with identical signatures from
 * 1.7.10 to 26.x. Not used on Folia (rejected at startup).
 */
public final class BukkitSchedulerBackend implements SchedulerBackend {

    private final Plugin plugin;
    private final Server server;

    /**
     * Creates the backend.
     *
     * @param plugin owning plugin
     */
    public BukkitSchedulerBackend(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public boolean isPrimaryThread() {
        return server.isPrimaryThread();
    }

    @Override
    public Handle scheduleSync(Runnable task, long delayTicks, long periodTicks) {
        BukkitScheduler scheduler = server.getScheduler();
        BukkitTask bukkitTask = periodTicks > 0
                ? scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks)
                : scheduler.runTaskLater(plugin, task, delayTicks);
        return handle(bukkitTask);
    }

    @Override
    public Handle scheduleAsync(Runnable task, long delayTicks, long periodTicks) {
        BukkitScheduler scheduler = server.getScheduler();
        BukkitTask bukkitTask = periodTicks > 0
                ? scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks)
                : scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
        return handle(bukkitTask);
    }

    private static Handle handle(final BukkitTask task) {
        return new Handle() {
            @Override
            public void cancel() {
                task.cancel();
            }
        };
    }
}
