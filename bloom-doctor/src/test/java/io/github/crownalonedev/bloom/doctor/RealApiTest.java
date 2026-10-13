package io.github.crownalonedev.bloom.doctor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;

/**
 * Known cross-version hazards (docs/architecture/ARCHITECTURE.md section 3) checked against the real spigot-api 1.8.8
 * and paper-api 1.21.11 jars, which the build passes in as system properties.
 */
class RealApiTest {

    @TempDir
    File dir;

    @Test
    void knownHazardsAreDetectedOnModernPaperButNotOnLegacy() throws IOException {
        File plugin = new TestJars()
                .pluginYml("com.example.Main")
                .clazz(
                        "com/example/Main",
                        "java/lang/Object",
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                        Opcodes.V1_8,
                        mv -> {
                            // InventoryView is an abstract class up to 1.20 and an interface from 1.21.
                            mv.visitInsn(Opcodes.ACONST_NULL);
                            mv.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "org/bukkit/inventory/InventoryView",
                                    "getTopInventory",
                                    "()Lorg/bukkit/inventory/Inventory;",
                                    false);
                            mv.visitInsn(Opcodes.POP);
                            // Material constants were renamed in 1.13.
                            mv.visitFieldInsn(
                                    Opcodes.GETSTATIC, "org/bukkit/Material", "WOOL", "Lorg/bukkit/Material;");
                            mv.visitInsn(Opcodes.POP);
                            // Stable across versions.
                            mv.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "org/bukkit/Bukkit",
                                    "getOnlinePlayers",
                                    "()Ljava/util/Collection;",
                                    false);
                            mv.visitInsn(Opcodes.POP);
                        })
                .write(dir, "hazards.jar");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int exit = new Doctor(new PrintStream(bytes, true)).run(new String[] {
            "--legacy-api",
            "spigot-api-1.8.8=" + System.getProperty("doctor.api.legacy"),
            "--modern-api",
            "paper-api-1.21.11=" + System.getProperty("doctor.api.modern"),
            plugin.getPath()
        });
        String report = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(1, exit, report);
        assertTrue(report.contains("Linkage vs spigot-api-1.8.8: 3 API references in 1 classes resolve"), report);
        assertTrue(
                report.contains("Linkage vs paper-api-1.21.11: IncompatibleClassChangeError: "
                        + "org.bukkit.inventory.InventoryView is an interface on this server"),
                report);
        assertTrue(report.contains("Linkage vs paper-api-1.21.11: NoSuchFieldError: org.bukkit.Material.WOOL"), report);
    }
}
