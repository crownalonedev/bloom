package io.github.crownalonedev.bloom.doctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** What the doctor needs from one class file: hierarchy, members, class version and outgoing references. */
final class ClassInfo {

    final String name;
    final String superName;
    final List<String> interfaces;
    final int access;
    final int majorVersion;
    /** name + descriptor → access flags. */
    final Map<String, Integer> methods = new HashMap<String, Integer>();
    /** name + ":" + descriptor → access flags. */
    final Map<String, Integer> fields = new HashMap<String, Integer>();
    /** Constant value of static final fields (used to read BloomVersion.VERSION). */
    final Map<String, Object> constants = new HashMap<String, Object>();

    final List<Reference> references;

    private ClassInfo(
            String name, String superName, String[] interfaces, int access, int majorVersion, List<Reference> refs) {
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces == null ? Collections.<String>emptyList() : Arrays.asList(interfaces);
        this.access = access;
        this.majorVersion = majorVersion;
        this.references = refs;
    }

    boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    /** Java feature release of the class file, e.g. 8 for major version 52. */
    int javaVersion() {
        return majorVersion - 44;
    }

    /**
     * Parses a class file.
     *
     * @param bytes class file
     * @param withReferences whether to collect references (only needed for scanned jars, not API jars)
     * @return parsed info
     */
    static ClassInfo read(byte[] bytes, final boolean withReferences) {
        ClassReader reader = new ClassReader(bytes);
        final int major = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        final List<Reference> refs = withReferences ? new ArrayList<Reference>() : Collections.<Reference>emptyList();
        final ClassInfo[] holder = new ClassInfo[1];
        int flags = withReferences ? ClassReader.SKIP_FRAMES : ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES;
        reader.accept(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(
                            int version,
                            int access,
                            String name,
                            String signature,
                            String superName,
                            String[] interfaces) {
                        holder[0] = new ClassInfo(name, superName, interfaces, access, major, refs);
                        if (withReferences) {
                            if (superName != null) {
                                refs.add(Reference.type(name, "<class>", superName));
                            }
                            if (interfaces != null) {
                                for (String i : interfaces) {
                                    refs.add(Reference.type(name, "<class>", i));
                                }
                            }
                        }
                    }

                    @Override
                    public FieldVisitor visitField(
                            int access, String name, String descriptor, String signature, Object value) {
                        holder[0].fields.put(name + ":" + descriptor, access);
                        if (value != null) {
                            holder[0].constants.put(name, value);
                        }
                        return null;
                    }

                    @Override
                    public MethodVisitor visitMethod(
                            int access, final String methodName, String descriptor, String signature, String[] ex) {
                        holder[0].methods.put(methodName + descriptor, access);
                        if (!withReferences) {
                            return null;
                        }
                        final String location = methodName + descriptor;
                        final String owner = holder[0].name;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(
                                    int opcode, String refOwner, String name, String desc, boolean isInterface) {
                                if (refOwner.startsWith("[")) {
                                    return; // array clone()
                                }
                                refs.add(Reference.method(owner, location, opcode, refOwner, name, desc, isInterface));
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String refOwner, String name, String desc) {
                                refs.add(Reference.field(owner, location, opcode, refOwner, name, desc));
                            }

                            @Override
                            public void visitTypeInsn(int opcode, String type) {
                                addType(type);
                            }

                            @Override
                            public void visitMultiANewArrayInsn(String descriptor, int dims) {
                                addType(Type.getType(descriptor)
                                        .getElementType()
                                        .getInternalName());
                            }

                            @Override
                            public void visitLdcInsn(Object value) {
                                if (value instanceof Type && ((Type) value).getSort() == Type.OBJECT) {
                                    addType(((Type) value).getInternalName());
                                } else if (value instanceof Handle) {
                                    addHandle((Handle) value);
                                }
                            }

                            @Override
                            public void visitInvokeDynamicInsn(
                                    String name, String desc, Handle bootstrap, Object... args) {
                                for (Object arg : args) {
                                    if (arg instanceof Handle) {
                                        addHandle((Handle) arg);
                                    }
                                }
                            }

                            private void addType(String type) {
                                if (type.startsWith("[")) {
                                    Type element = Type.getType(type).getElementType();
                                    if (element.getSort() != Type.OBJECT) {
                                        return;
                                    }
                                    type = element.getInternalName();
                                }
                                refs.add(Reference.type(owner, location, type));
                            }

                            private void addHandle(Handle handle) {
                                int tag = handle.getTag();
                                if (tag <= Opcodes.H_PUTSTATIC) {
                                    int opcode = tag == Opcodes.H_GETFIELD
                                            ? Opcodes.GETFIELD
                                            : tag == Opcodes.H_GETSTATIC
                                                    ? Opcodes.GETSTATIC
                                                    : tag == Opcodes.H_PUTFIELD ? Opcodes.PUTFIELD : Opcodes.PUTSTATIC;
                                    refs.add(Reference.field(
                                            owner,
                                            location,
                                            opcode,
                                            handle.getOwner(),
                                            handle.getName(),
                                            handle.getDesc()));
                                } else {
                                    int opcode = tag == Opcodes.H_INVOKESTATIC
                                            ? Opcodes.INVOKESTATIC
                                            : tag == Opcodes.H_INVOKEINTERFACE
                                                    ? Opcodes.INVOKEINTERFACE
                                                    : tag == Opcodes.H_INVOKEVIRTUAL
                                                            ? Opcodes.INVOKEVIRTUAL
                                                            : Opcodes.INVOKESPECIAL;
                                    refs.add(Reference.method(
                                            owner,
                                            location,
                                            opcode,
                                            handle.getOwner(),
                                            handle.getName(),
                                            handle.getDesc(),
                                            handle.isInterface()));
                                }
                            }
                        };
                    }
                },
                flags);
        return holder[0];
    }
}
