package io.github.crownalonedev.bloom.doctor;

import static io.github.crownalonedev.bloom.doctor.TestJars.all;
import static io.github.crownalonedev.bloom.doctor.TestJars.field;
import static io.github.crownalonedev.bloom.doctor.TestJars.method;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;

class DoctorTest {

    private static final int PUBLIC_CLASS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
    private static final int PUBLIC_INTERFACE = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
    private static final String MAIN = "com/example/Main";
    private static final String BLOOM = "com/example/libs/bloom/";

    @TempDir
    File dir;

    /** A fake server API: Server (interface), Inventory (interface), View (class), Material (enum-like). */
    private File api(boolean viewIsInterface) throws IOException {
        TestJars api = new TestJars()
                .clazz(
                        "org/bukkit/Base",
                        "java/lang/Object",
                        PUBLIC_CLASS,
                        Opcodes.V1_8,
                        null,
                        method(Opcodes.ACC_PUBLIC, "inherited", "()V"))
                .clazz(
                        "org/bukkit/Server",
                        "java/lang/Object",
                        PUBLIC_INTERFACE,
                        Opcodes.V1_8,
                        null,
                        all(
                                method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "getName", "()Ljava/lang/String;"),
                                method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "create", "()Lorg/bukkit/Server;")))
                .clazz(
                        "org/bukkit/Material",
                        "org/bukkit/Base",
                        PUBLIC_CLASS,
                        Opcodes.V1_8,
                        null,
                        all(
                                field(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "STONE", "Lorg/bukkit/Material;"),
                                field(Opcodes.ACC_PUBLIC, "id", "I")));
        if (viewIsInterface) {
            api.clazz(
                    "org/bukkit/View",
                    "java/lang/Object",
                    PUBLIC_INTERFACE,
                    Opcodes.V1_8,
                    null,
                    method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "top", "()V"));
        } else {
            api.clazz(
                    "org/bukkit/View",
                    "java/lang/Object",
                    PUBLIC_CLASS | Opcodes.ACC_ABSTRACT,
                    Opcodes.V1_8,
                    null,
                    method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "top", "()V"));
        }
        return api.write(dir, viewIsInterface ? "api-modern.jar" : "api-legacy.jar");
    }

    /** A correctly packaged plugin with relocated Bloom and both adapters. */
    private TestJars plugin(TestJars.Body body) {
        return new TestJars()
                .pluginYml("com.example.Main")
                .clazz(MAIN, "org/bukkit/Base", PUBLIC_CLASS, Opcodes.V1_8, body)
                .clazz(
                        BLOOM + "api/BloomVersion",
                        "java/lang/Object",
                        PUBLIC_CLASS,
                        Opcodes.V1_8,
                        null,
                        cw -> cw.visitField(
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                                        "VERSION",
                                        "Ljava/lang/String;",
                                        null,
                                        "1.2.3")
                                .visitEnd())
                .clazz(BLOOM + "platform/legacy/LegacyPlatformAdapter", "java/lang/Object", PUBLIC_CLASS)
                .clazz(
                        BLOOM + "platform/modern/ModernPlatformAdapter",
                        "java/lang/Object",
                        PUBLIC_CLASS,
                        Opcodes.V21,
                        null);
    }

    private String run(File plugin, File legacyApi, File modernApi, int expectedExit) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true);
        int exit = new Doctor(out).run(new String[] {
            "--legacy-api",
            "legacy=" + legacyApi.getPath(),
            "--modern-api",
            "modern=" + modernApi.getPath(),
            plugin.getPath()
        });
        String report = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(expectedExit, exit, report);
        return report;
    }

    @Test
    void cleanPluginPasses() throws IOException {
        File jar = plugin(mv -> {
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC, "org/bukkit/Server", "create", "()Lorg/bukkit/Server;", true);
                    mv.visitMethodInsn(
                            Opcodes.INVOKEINTERFACE, "org/bukkit/Server", "getName", "()Ljava/lang/String;", true);
                    mv.visitInsn(Opcodes.POP);
                    // Inherited from an API superclass through the plugin's own class.
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MAIN, "inherited", "()V", false);
                    // Object methods on an API interface.
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/bukkit/Server", "hashCode", "()I", true);
                    mv.visitInsn(Opcodes.POP);
                    // Unknown third-party API (e.g. another plugin): skipped, not reported.
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/milkbowl/vault/Economy", "get", "()V", false);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "org/bukkit/Material", "STONE", "Lorg/bukkit/Material;");
                    mv.visitInsn(Opcodes.POP);
                })
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 0);
        assertTrue(report.contains("Adapters: legacy (1.7.10 - 1.8.9), modern (Paper 1.21.1 - 26.3.x)"), report);
        assertTrue(report.contains("Bloom 1.2.3 at com.example.libs.bloom"), report);
        assertTrue(report.contains("No errors."), report);
    }

    @Test
    void classBecameInterface() throws IOException {
        // Compiled against the legacy API where View is an abstract class: invokevirtual.
        File jar = plugin(mv -> {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/bukkit/View", "top", "()V", false);
                })
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 1);
        assertTrue(report.contains("Linkage vs legacy: 1 API references"), report);
        assertTrue(
                report.contains("Linkage vs modern: IncompatibleClassChangeError: org.bukkit.View is an interface"),
                report);
        assertTrue(report.contains("com.example.Main.run()V"), report);
    }

    @Test
    void missingMembersAndClasses() throws IOException {
        File jar = plugin(mv -> {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/bukkit/Server", "removed", "()V", true);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "org/bukkit/Material", "WOOL", "Lorg/bukkit/Material;");
                    mv.visitInsn(Opcodes.POP);
                    mv.visitTypeInsn(Opcodes.NEW, "org/bukkit/boss/BossBar");
                    mv.visitInsn(Opcodes.POP);
                })
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 1);
        assertTrue(report.contains("NoSuchMethodError: org.bukkit.Server.removed()V"), report);
        assertTrue(report.contains("NoSuchFieldError: org.bukkit.Material.WOOL"), report);
        assertTrue(report.contains("NoClassDefFoundError: org.bukkit.boss.BossBar"), report);
    }

    @Test
    void staticMismatch() throws IOException {
        File jar = plugin(mv -> {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "org/bukkit/Material", "id", "I");
                    mv.visitInsn(Opcodes.POP);
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitMethodInsn(
                            Opcodes.INVOKEINTERFACE, "org/bukkit/Server", "create", "()Lorg/bukkit/Server;", true);
                    mv.visitInsn(Opcodes.POP);
                })
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 1);
        assertTrue(report.contains("org.bukkit.Material.id : I is an instance field on this server"), report);
        assertTrue(report.contains("org.bukkit.Server.create()Lorg/bukkit/Server; is static on this server"), report);
    }

    @Test
    void adapterOfTheOtherFamilyIsNotLinkedAgainstThisServer() throws IOException {
        // Code in the legacy adapter may use APIs that only exist on legacy servers.
        File jar = plugin(null)
                .clazz(BLOOM + "platform/legacy/Titles", "java/lang/Object", PUBLIC_CLASS, Opcodes.V1_8, mv -> {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/bukkit/View", "top", "()V", false);
                })
                .write(dir, "plugin.jar");
        run(jar, api(false), api(true), 0);
    }

    @Test
    void packagingProblems() throws IOException {
        File jar = new TestJars()
                .pluginYml("com.example.Missing")
                .clazz("io/github/crownalonedev/bloom/api/BloomVersion", "java/lang/Object", PUBLIC_CLASS)
                .clazz("org/yaml/snakeyaml/Yaml", "java/lang/Object", PUBLIC_CLASS)
                .clazz("org/bukkit/Bukkit", "java/lang/Object", PUBLIC_CLASS)
                .clazz(MAIN, "java/lang/Object", PUBLIC_CLASS, Opcodes.V11, null)
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 1);
        assertTrue(report.contains("main class com.example.Missing is not in the jar"), report);
        assertTrue(report.contains("Bloom is not relocated"), report);
        assertTrue(report.contains("org.yaml.snakeyaml is bundled but not relocated"), report);
        assertTrue(report.contains("contains the Bukkit/Spigot API"), report);
        assertTrue(report.contains("No Bloom platform adapter"), report);
        assertTrue(report.contains("relocate(\"io.github.crownalonedev.bloom.\""), report);
    }

    @Test
    void javaLevelMustMatchLegacyServers() throws IOException {
        File jar = plugin(null)
                .clazz("com/example/Modern", "java/lang/Object", PUBLIC_CLASS, Opcodes.V17, null)
                .write(dir, "plugin.jar");
        String report = run(jar, api(false), api(true), 1);
        assertTrue(report.contains("Classes need Java 17 (e.g. com.example.Modern)"), report);
        // The modern adapter itself is Java 21 and is ignored.
        assertFalse(report.contains("ModernPlatformAdapter"), report);
    }

    @Test
    void usageErrors() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertEquals(2, new Doctor(new PrintStream(bytes, true)).run(new String[] {"--bogus", "x.jar"}));
        assertEquals(2, new Doctor(new PrintStream(bytes, true)).run(new String[0]));
    }
}
