package io.github.crownalonedev.bloom.doctor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command-line entry point.
 *
 * <pre>
 * java -jar bloom-doctor-all.jar [options] plugin.jar...
 *
 *   --legacy-api NAME=JAR[,JAR...]  check linkage against a 1.7.10 / 1.8.x API (e.g. spigot-api 1.8.8 + its deps)
 *   --modern-api NAME=JAR[,JAR...]  check linkage against a Paper 1.21+ API (paper-api + its deps)
 *   --exclude PACKAGE               skip classes in this package for linkage (repeatable), e.g. com.example.v1_21
 *   --quiet                         print only warnings and errors
 * </pre>
 *
 * <p>Exit code 0: no errors; 1: at least one error; 2: usage or I/O problem.
 */
public final class Doctor {

    static final String VERSION = Doctor.class.getPackage().getImplementationVersion() == null
            ? "dev"
            : Doctor.class.getPackage().getImplementationVersion();

    /** A server API to check linkage against. */
    static final class Target {
        final String name;
        final boolean legacy;
        final List<File> jars;

        Target(String name, boolean legacy, List<File> jars) {
            this.name = name;
            this.legacy = legacy;
            this.jars = jars;
        }
    }

    private final List<Target> targets = new ArrayList<Target>();
    private final List<String> excludes = new ArrayList<String>();
    private final List<File> pluginJars = new ArrayList<File>();
    private boolean quiet;
    private final PrintStream out;

    Doctor(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) {
        System.exit(new Doctor(System.out).run(args));
    }

    int run(String[] args) {
        try {
            parse(args);
        } catch (IllegalArgumentException e) {
            out.println("bloom-doctor: " + e.getMessage());
            out.println("Usage: java -jar bloom-doctor-all.jar [--legacy-api NAME=JAR[,JAR...]] "
                    + "[--modern-api NAME=JAR[,JAR...]] [--exclude PACKAGE] [--quiet] plugin.jar...");
            return 2;
        }
        int errors = 0;
        for (File file : pluginJars) {
            try {
                errors += check(file);
            } catch (IOException e) {
                out.println("bloom-doctor: cannot read " + file + ": " + e.getMessage());
                return 2;
            }
        }
        return errors > 0 ? 1 : 0;
    }

    private void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--legacy-api") || arg.equals("--modern-api")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException(arg + " needs NAME=JAR[,JAR...]");
                }
                targets.add(target(args[++i], arg.equals("--legacy-api")));
            } else if (arg.equals("--exclude")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("--exclude needs a package name");
                }
                String pkg = args[++i].replace('.', '/');
                excludes.add(pkg.endsWith("/") ? pkg : pkg + "/");
            } else if (arg.equals("--quiet")) {
                quiet = true;
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("unknown option " + arg);
            } else {
                pluginJars.add(new File(arg));
            }
        }
        if (pluginJars.isEmpty()) {
            throw new IllegalArgumentException("no plugin jar given");
        }
    }

    private static Target target(String spec, boolean legacy) {
        int eq = spec.indexOf('=');
        if (eq <= 0 || eq == spec.length() - 1) {
            throw new IllegalArgumentException("expected NAME=JAR[,JAR...], got " + spec);
        }
        List<File> jars = new ArrayList<File>();
        for (String path : spec.substring(eq + 1).split(",")) {
            File file = new File(path.trim());
            if (!file.isFile()) {
                throw new IllegalArgumentException("API jar not found: " + file);
            }
            jars.add(file);
        }
        return new Target(spec.substring(0, eq), legacy, jars);
    }

    /** Checks one jar and prints its report; returns the number of errors. */
    int check(File file) throws IOException {
        PluginJar jar = PluginJar.read(file);
        out.println("bloom-doctor " + VERSION + " - " + file.getName());
        JarChecks.Result packaging = JarChecks.run(jar);
        List<Finding> findings = new ArrayList<Finding>(packaging.findings);

        for (Target target : targets) {
            if (target.legacy && !packaging.legacyAdapter && packaging.modernAdapter) {
                findings.add(Finding.info("Linkage vs " + target.name + ": skipped (no legacy adapter in the jar)"));
                continue;
            }
            if (!target.legacy && packaging.legacyAdapter && !packaging.modernAdapter) {
                findings.add(Finding.info("Linkage vs " + target.name + ": skipped (no modern adapter in the jar)"));
                continue;
            }
            findings.addAll(linkage(jar, target));
        }

        int errors = 0;
        for (Finding finding : findings) {
            if (finding.severity == Finding.Severity.ERROR) {
                errors++;
            }
            if (quiet && finding.severity.compareTo(Finding.Severity.WARNING) < 0) {
                continue;
            }
            out.println("  " + label(finding.severity) + " " + finding.message);
            if (!finding.hint.isEmpty()) {
                out.println("          fix: " + finding.hint);
            }
        }
        out.println("  " + (errors == 0 ? "No errors." : errors + " error(s)."));
        out.println();
        return errors;
    }

    private List<Finding> linkage(PluginJar jar, Target target) throws IOException {
        // The adapter for the other server family is never loaded on this one.
        String skipAdapter = target.legacy ? "/bloom/platform/modern/" : "/bloom/platform/legacy/";
        List<ClassInfo> classes = new ArrayList<ClassInfo>();
        for (ClassInfo type : jar.classes.values()) {
            if (!type.name.contains(skipAdapter) && !isExcluded(type.name)) {
                classes.add(type);
            }
        }
        List<Finding> findings = new ArrayList<Finding>();
        ClassPath classPath = new ClassPath(jar.classes, target.jars);
        try {
            LinkageChecker checker = new LinkageChecker(classPath);
            Collection<LinkageChecker.Problem> problems = checker.check(classes);
            if (problems.isEmpty()) {
                findings.add(Finding.ok("Linkage vs " + target.name + ": " + checker.checkedReferences()
                        + " API references in " + classes.size() + " classes resolve"));
            }
            for (LinkageChecker.Problem problem : problems) {
                StringBuilder where = new StringBuilder();
                for (String location : problem.locations) {
                    where.append(where.length() == 0 ? "" : ", ").append(location);
                }
                if (problem.count > problem.locations.size()) {
                    where.append(" and ")
                            .append(problem.count - problem.locations.size())
                            .append(" more");
                }
                findings.add(Finding.error(
                        "Linkage vs " + target.name + ": " + problem.error + ": " + problem.target + " "
                                + problem.detail + ". Used in " + where,
                        "Call it through a Bloom API or capability check, or move it to version-specific code "
                                + "excluded with --exclude."));
            }
        } finally {
            classPath.close();
        }
        return findings;
    }

    private boolean isExcluded(String internalName) {
        for (String prefix : excludes) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String label(Finding.Severity severity) {
        switch (severity) {
            case OK:
                return "[ok]   ";
            case INFO:
                return "[info] ";
            case WARNING:
                return "[warn] ";
            default:
                return "[ERROR]";
        }
    }
}
