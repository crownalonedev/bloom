package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import java.time.Duration;
import org.bukkit.entity.Player;

/** Scheduling snippets used in the documentation. */
final class SchedulingSnippets {

    private SchedulingSnippets() {}

    static void basics(Bloom bloom, Player player) {
        // @snippet:scheduling.basics
        BloomScheduler scheduler = bloom.scheduler();
        scheduler.sync(() -> player.sendMessage("Next tick"));
        scheduler.syncLater(() -> player.sendMessage("Three seconds later"), Duration.ofSeconds(3));
        ScheduledTask task = scheduler.syncRepeating(self -> player.sendMessage("Every second"), 0, 20);
        task.cancel();
        // @end
    }

    static void countdown(Bloom bloom, final Player player) {
        // @snippet:scheduling.countdown
        final int[] seconds = {5};
        bloom.scheduler()
                .syncRepeating(
                        task -> {
                            if (!player.isOnline() || seconds[0] == 0) {
                                task.cancel(); // a repeating task receives its own handle
                                return;
                            }
                            bloom.messages().actionBar(player, "&eStarting in " + seconds[0]--);
                        },
                        Duration.ZERO,
                        Duration.ofSeconds(1));
        // @end
    }

    static String loadProfileFromWeb(java.util.UUID id) {
        return id.toString();
    }

    static void async(Bloom bloom, final Player player) {
        // @snippet:scheduling.async
        bloom.scheduler()
                .supplyAsync(() -> loadProfileFromWeb(player.getUniqueId())) // background thread: no Bukkit calls
                .thenAcceptAsync(
                        profile -> player.sendMessage("Loaded " + profile), // back on the main thread
                        bloom.scheduler().mainThreadExecutor());
        // @end
    }

    static void guard(Bloom bloom) {
        // @snippet:scheduling.guard
        bloom.scheduler().ensurePrimaryThread("Teleporting players");
        // IllegalStateException off the main thread: "Teleporting players must be called from the main server
        // thread, but was called from thread 'pool-3-thread-1'. Switch threads with scheduler().sync(...) ..."
        // @end
    }
}
