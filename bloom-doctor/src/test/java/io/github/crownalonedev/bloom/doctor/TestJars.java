package io.github.crownalonedev.bloom.doctor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Builds small jars of generated classes for doctor tests. */
final class TestJars {

    private final Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();

    TestJars clazz(String name, String superName, int access, String... interfaces) {
        return clazz(name, superName, access, Opcodes.V1_8, null, interfaces);
    }

    /**
     * Adds a class.
     *
     * @param body called with a visitor for a {@code public void run()} method, or {@code null} for no code
     */
    TestJars clazz(String name, String superName, int access, int version, Body body, String... interfaces) {
        return clazz(name, superName, access, version, body, null, interfaces);
    }

    TestJars clazz(
            String name, String superName, int access, int version, Body body, Members members, String... interfaces) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(version, access, name, null, superName, interfaces.length == 0 ? null : interfaces);
        if (members != null) {
            members.add(cw);
        }
        if (body != null) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
            mv.visitCode();
            body.emit(mv);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        entries.put(name + ".class", cw.toByteArray());
        return this;
    }

    TestJars file(String name, String content) {
        entries.put(name, content.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /** A plugin.yml naming the given main class. */
    TestJars pluginYml(String main) {
        return file("plugin.yml", "name: TestPlugin\nversion: 1.0\nmain: " + main + "\n");
    }

    File write(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }
        return file;
    }

    interface Body {
        void emit(MethodVisitor mv);
    }

    interface Members {
        void add(ClassWriter cw);
    }

    static Members method(final int access, final String name, final String desc) {
        return cw -> cw.visitMethod(access, name, desc, null, null).visitEnd();
    }

    static Members field(final int access, final String name, final String desc) {
        return cw -> cw.visitField(access, name, desc, null, null).visitEnd();
    }

    static Members all(final Members... members) {
        return cw -> {
            for (Members m : members) {
                m.add(cw);
            }
        };
    }
}
