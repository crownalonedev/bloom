package io.github.crownalonedev.bloom.apidoc;

/** Minimal streaming JSON writer (pretty-printing is unnecessary: the site and tools parse it). */
final class Json {

    private final StringBuilder out = new StringBuilder();
    private boolean needsComma;

    Json beginObject() {
        separator();
        out.append('{');
        needsComma = false;
        return this;
    }

    Json endObject() {
        out.append('}');
        needsComma = true;
        return this;
    }

    Json beginArray() {
        separator();
        out.append('[');
        needsComma = false;
        return this;
    }

    Json endArray() {
        out.append(']');
        needsComma = true;
        return this;
    }

    Json name(String name) {
        separator();
        string(name);
        out.append(':');
        needsComma = false;
        return this;
    }

    Json field(String name, String value) {
        name(name);
        string(value);
        needsComma = true;
        return this;
    }

    private void separator() {
        if (needsComma) {
            out.append(',');
        }
    }

    private void string(String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
