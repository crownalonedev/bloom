package io.github.crownalonedev.bloom.doctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * Resolves every reference from a plugin's classes against one server API the way the JVM would, and reports the
 * references that would fail with {@code NoClassDefFoundError}, {@code NoSuchMethodError}, {@code NoSuchFieldError}
 * or {@code IncompatibleClassChangeError} on that server.
 *
 * <p>Only references decided by the API are reported. A reference into a class that is neither in the plugin, the API
 * jars nor the JDK (for example another plugin's API) is skipped, as is any resolution whose hierarchy contains such a
 * class.
 */
final class LinkageChecker {

    /** Packages that belong to the server API even when the class is absent from the API jars. */
    static final List<String> API_PACKAGES = Arrays.asList(
            "org/bukkit/",
            "org/spigotmc/",
            "net/md_5/bungee/",
            "com/destroystokyo/paper/",
            "io/papermc/paper/",
            "net/kyori/adventure/",
            "co/aikar/timings/");

    /** One linkage problem with up to a few places it occurs. */
    static final class Problem {
        final String error;
        final String target;
        final String detail;
        final List<String> locations = new ArrayList<String>();
        int count;

        Problem(String error, String target, String detail) {
            this.error = error;
            this.target = target;
            this.detail = detail;
        }
    }

    private static final int MAX_LOCATIONS = 3;

    private final ClassPath classPath;
    private final Map<String, Problem> problems = new LinkedHashMap<String, Problem>();
    private int checkedReferences;

    LinkageChecker(ClassPath classPath) {
        this.classPath = classPath;
    }

    int checkedReferences() {
        return checkedReferences;
    }

    Collection<Problem> check(Collection<ClassInfo> classes) {
        for (ClassInfo type : classes) {
            checkSupertypes(type);
            for (Reference ref : type.references) {
                switch (ref.kind) {
                    case TYPE:
                        checkType(ref);
                        break;
                    case METHOD:
                        checkMethod(ref);
                        break;
                    default:
                        checkField(ref);
                        break;
                }
            }
        }
        return problems.values();
    }

    private void checkSupertypes(ClassInfo type) {
        if (type.superName != null && classPath.isApiClass(type.superName)) {
            ClassInfo parent = classPath.find(type.superName);
            if (parent != null && parent.isInterface()) {
                report(
                        "IncompatibleClassChangeError",
                        type.superName.replace('/', '.'),
                        "is an interface on this server, but the plugin extends it as a class",
                        type.name.replace('/', '.') + " (supertype)");
            } else if (parent != null && (parent.access & Opcodes.ACC_FINAL) != 0) {
                report(
                        "VerifyError",
                        type.superName.replace('/', '.'),
                        "is final on this server, but the plugin extends it",
                        type.name.replace('/', '.') + " (supertype)");
            }
        }
        for (String itf : type.interfaces) {
            if (classPath.isApiClass(itf)) {
                ClassInfo parent = classPath.find(itf);
                if (parent != null && !parent.isInterface()) {
                    report(
                            "IncompatibleClassChangeError",
                            itf.replace('/', '.'),
                            "is a class on this server, but the plugin implements it as an interface",
                            type.name.replace('/', '.') + " (supertype)");
                }
            }
        }
    }

    private void checkType(Reference ref) {
        if (isApiPackage(ref.owner) && classPath.find(ref.owner) == null) {
            checkedReferences++;
            report("NoClassDefFoundError", ref.target(), "class does not exist on this server", ref.location());
        }
    }

    private void checkMethod(Reference ref) {
        ClassInfo owner = classPath.find(ref.owner);
        if (owner == null) {
            if (isApiPackage(ref.owner)) {
                checkedReferences++;
                report(
                        "NoClassDefFoundError",
                        ref.owner.replace('/', '.'),
                        "class does not exist on this server",
                        ref.location());
            }
            return;
        }
        Lookup lookup = new Lookup();
        Found found = owner.isInterface()
                ? resolveInterfaceMethod(owner, ref.name, ref.descriptor, lookup)
                : resolveClassMethod(owner, ref.name, ref.descriptor, lookup);
        boolean decidedByApi = found != null ? classPath.isApiClass(found.declaring.name) : lookup.touchedApi;
        if (!decidedByApi) {
            return;
        }
        checkedReferences++;
        if (owner.isInterface() != ref.interfaceCall && classPath.isApiClass(owner.name)) {
            report(
                    "IncompatibleClassChangeError",
                    ref.owner.replace('/', '.'),
                    owner.isInterface()
                            ? "is an interface on this server, but the plugin was compiled against a class"
                            : "is a class on this server, but the plugin was compiled against an interface",
                    ref.location());
            return;
        }
        if (found == null) {
            if (!lookup.incomplete && !isSignaturePolymorphic(ref)) {
                report("NoSuchMethodError", ref.target(), "method does not exist on this server", ref.location());
            }
            return;
        }
        boolean isStatic = (found.access & Opcodes.ACC_STATIC) != 0;
        if ((ref.opcode == Opcodes.INVOKESTATIC) != isStatic) {
            report(
                    "IncompatibleClassChangeError",
                    ref.target(),
                    isStatic
                            ? "is static on this server, but called as an instance method"
                            : "is an instance method on this server, but called as static",
                    ref.location());
        }
    }

    private void checkField(Reference ref) {
        ClassInfo owner = classPath.find(ref.owner);
        if (owner == null) {
            if (isApiPackage(ref.owner)) {
                checkedReferences++;
                report(
                        "NoClassDefFoundError",
                        ref.owner.replace('/', '.'),
                        "class does not exist on this server",
                        ref.location());
            }
            return;
        }
        Lookup lookup = new Lookup();
        Found found = resolveField(owner, ref.name + ":" + ref.descriptor, lookup, new HashSet<String>());
        boolean decidedByApi = found != null ? classPath.isApiClass(found.declaring.name) : lookup.touchedApi;
        if (!decidedByApi) {
            return;
        }
        checkedReferences++;
        if (found == null) {
            if (!lookup.incomplete) {
                report("NoSuchFieldError", ref.target(), "field does not exist on this server", ref.location());
            }
            return;
        }
        boolean isStatic = (found.access & Opcodes.ACC_STATIC) != 0;
        boolean staticAccess = ref.opcode == Opcodes.GETSTATIC || ref.opcode == Opcodes.PUTSTATIC;
        if (staticAccess != isStatic) {
            report(
                    "IncompatibleClassChangeError",
                    ref.target(),
                    isStatic
                            ? "is static on this server, but accessed as an instance field"
                            : "is an instance field on this server, but accessed as static",
                    ref.location());
        }
    }

    /** Tracks whether a resolution walked through API classes or unknown classes. */
    private static final class Lookup {
        boolean touchedApi;
        boolean incomplete;
    }

    private static final class Found {
        final ClassInfo declaring;
        final int access;

        Found(ClassInfo declaring, int access) {
            this.declaring = declaring;
            this.access = access;
        }
    }

    private void visit(ClassInfo type, Lookup lookup) {
        if (classPath.isApiClass(type.name)) {
            lookup.touchedApi = true;
        }
    }

    /** JVMS 5.4.3.3: the class and its superclasses, then the superinterfaces. */
    private Found resolveClassMethod(ClassInfo owner, String name, String desc, Lookup lookup) {
        String key = name + desc;
        for (ClassInfo c = owner; c != null; ) {
            visit(c, lookup);
            Integer access = c.methods.get(key);
            if (access != null) {
                return new Found(c, access);
            }
            if (c.superName == null) {
                break;
            }
            ClassInfo parent = classPath.find(c.superName);
            if (parent == null) {
                lookup.incomplete = true;
                break;
            }
            c = parent;
        }
        return resolveInSuperinterfaces(owner, key, lookup, new HashSet<String>());
    }

    /** JVMS 5.4.3.4: the interface, then Object, then the superinterfaces. */
    private Found resolveInterfaceMethod(ClassInfo owner, String name, String desc, Lookup lookup) {
        String key = name + desc;
        visit(owner, lookup);
        Integer access = owner.methods.get(key);
        if (access != null) {
            return new Found(owner, access);
        }
        ClassInfo object = classPath.find("java/lang/Object");
        if (object != null) {
            Integer objectAccess = object.methods.get(key);
            if (objectAccess != null && (objectAccess & Opcodes.ACC_PUBLIC) != 0) {
                return new Found(object, objectAccess);
            }
        }
        return resolveInSuperinterfaces(owner, key, lookup, new HashSet<String>());
    }

    private Found resolveInSuperinterfaces(ClassInfo type, String key, Lookup lookup, Set<String> seen) {
        for (ClassInfo c = type; c != null; ) {
            for (String itfName : c.interfaces) {
                if (!seen.add(itfName)) {
                    continue;
                }
                ClassInfo itf = classPath.find(itfName);
                if (itf == null) {
                    lookup.incomplete = true;
                    continue;
                }
                visit(itf, lookup);
                Integer access = itf.methods.get(key);
                if (access != null && (access & Opcodes.ACC_PRIVATE) == 0) {
                    return new Found(itf, access);
                }
                Found deeper = resolveInSuperinterfaces(itf, key, lookup, seen);
                if (deeper != null) {
                    return deeper;
                }
            }
            if (c.superName == null || c.isInterface()) {
                break;
            }
            c = classPath.find(c.superName);
        }
        return null;
    }

    /** JVMS 5.4.3.2: the class, its superinterfaces (recursively), then its superclass. */
    private Found resolveField(ClassInfo type, String key, Lookup lookup, Set<String> seen) {
        visit(type, lookup);
        Integer access = type.fields.get(key);
        if (access != null) {
            return new Found(type, access);
        }
        for (String itfName : type.interfaces) {
            if (!seen.add(itfName)) {
                continue;
            }
            ClassInfo itf = classPath.find(itfName);
            if (itf == null) {
                lookup.incomplete = true;
                continue;
            }
            Found found = resolveField(itf, key, lookup, seen);
            if (found != null) {
                return found;
            }
        }
        if (type.superName != null) {
            ClassInfo parent = classPath.find(type.superName);
            if (parent == null) {
                lookup.incomplete = true;
                return null;
            }
            return resolveField(parent, key, lookup, seen);
        }
        return null;
    }

    private static boolean isSignaturePolymorphic(Reference ref) {
        return ref.owner.equals("java/lang/invoke/MethodHandle") || ref.owner.equals("java/lang/invoke/VarHandle");
    }

    static boolean isApiPackage(String internalName) {
        for (String prefix : API_PACKAGES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void report(String error, String target, String detail, String location) {
        String key = error + "|" + target;
        Problem problem = problems.get(key);
        if (problem == null) {
            problem = new Problem(error, target, detail);
            problems.put(key, problem);
        }
        problem.count++;
        if (problem.locations.size() < MAX_LOCATIONS && !problem.locations.contains(location)) {
            problem.locations.add(location);
        }
    }
}
