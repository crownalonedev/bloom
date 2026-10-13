package io.github.crownalonedev.bloom.doctor;

/** A symbolic reference from compiled code to a class, method or field, with where it occurs. */
final class Reference {

    enum Kind {
        TYPE,
        METHOD,
        FIELD
    }

    final Kind kind;
    /** Class containing the reference (internal name). */
    final String fromClass;
    /** Method containing the reference (name + descriptor), or {@code <class>} for supertypes. */
    final String fromMethod;

    final int opcode;
    /** Referenced class (internal name). */
    final String owner;

    final String name;
    final String descriptor;
    final boolean interfaceCall;

    private Reference(
            Kind kind,
            String fromClass,
            String fromMethod,
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean interfaceCall) {
        this.kind = kind;
        this.fromClass = fromClass;
        this.fromMethod = fromMethod;
        this.opcode = opcode;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.interfaceCall = interfaceCall;
    }

    static Reference type(String fromClass, String fromMethod, String type) {
        return new Reference(Kind.TYPE, fromClass, fromMethod, 0, type, null, null, false);
    }

    static Reference method(
            String fromClass, String fromMethod, int opcode, String owner, String name, String desc, boolean itf) {
        return new Reference(Kind.METHOD, fromClass, fromMethod, opcode, owner, name, desc, itf);
    }

    static Reference field(String fromClass, String fromMethod, int opcode, String owner, String name, String desc) {
        return new Reference(Kind.FIELD, fromClass, fromMethod, opcode, owner, name, desc, false);
    }

    /** Human-readable target, e.g. {@code org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;}. */
    String target() {
        String type = owner.replace('/', '.');
        switch (kind) {
            case METHOD:
                return type + "." + name + descriptor;
            case FIELD:
                return type + "." + name + " : " + descriptor;
            default:
                return type;
        }
    }

    /** Where the reference occurs, e.g. {@code com.example.MyPlugin.onEnable()V}. */
    String location() {
        String type = fromClass.replace('/', '.');
        return "<class>".equals(fromMethod) ? type + " (supertype)" : type + "." + fromMethod;
    }
}
