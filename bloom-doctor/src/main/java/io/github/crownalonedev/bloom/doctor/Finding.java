package io.github.crownalonedev.bloom.doctor;

/** One result line of a doctor run. */
final class Finding {

    enum Severity {
        OK,
        INFO,
        WARNING,
        ERROR
    }

    final Severity severity;
    final String message;
    /** How to fix it; may be empty. */
    final String hint;

    Finding(Severity severity, String message, String hint) {
        this.severity = severity;
        this.message = message;
        this.hint = hint == null ? "" : hint;
    }

    static Finding ok(String message) {
        return new Finding(Severity.OK, message, "");
    }

    static Finding info(String message) {
        return new Finding(Severity.INFO, message, "");
    }

    static Finding warning(String message, String hint) {
        return new Finding(Severity.WARNING, message, hint);
    }

    static Finding error(String message, String hint) {
        return new Finding(Severity.ERROR, message, hint);
    }
}
