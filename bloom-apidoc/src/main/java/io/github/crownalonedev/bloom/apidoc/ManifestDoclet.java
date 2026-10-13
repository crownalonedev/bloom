package io.github.crownalonedev.bloom.apidoc;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.util.DocTrees;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * Javadoc doclet that writes one module's public API as JSON: packages, types and their public (and, for
 * extensible types, protected) members with signatures and documentation. Packages named {@code internal} or
 * {@code common} are implementation details and are skipped.
 *
 * <p>Options: {@code -manifestOut <file>}, {@code -moduleName <name>}, {@code -moduleDescription <text>}. Options
 * of the standard doclet that the build passes to every Javadoc task are accepted and ignored.
 */
public final class ManifestDoclet implements Doclet {

    private static final Pattern PACKAGE_PREFIX = Pattern.compile("\\b(?:[a-z_][a-z0-9_]*\\.)+(?=[A-Z])");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private String out;
    private String moduleName = "";
    private String moduleDescription = "";
    private Reporter reporter;
    private DocTrees trees;
    private Elements elements;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "BloomManifest";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        Set<Option> options = new LinkedHashSet<>();
        options.add(new ValueOption("-manifestOut", value -> out = value));
        options.add(new ValueOption("-moduleName", value -> moduleName = value));
        options.add(new ValueOption("-moduleDescription", value -> moduleDescription = value));
        for (String ignored : Arrays.asList("-d", "-doctitle", "-windowtitle", "-docencoding", "-charset")) {
            options.add(new ValueOption(ignored, value -> {}));
        }
        for (String ignored : Arrays.asList("-notimestamp", "-author", "-version", "-use", "-nodeprecated")) {
            options.add(new FlagOption(ignored));
        }
        return options;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        if (out == null) {
            reporter.print(javax.tools.Diagnostic.Kind.ERROR, "-manifestOut is required");
            return false;
        }
        trees = environment.getDocTrees();
        elements = environment.getElementUtils();

        Set<TypeElement> types = new LinkedHashSet<>();
        for (TypeElement type : ElementFilter.typesIn(environment.getIncludedElements())) {
            collect(type, types);
        }
        Map<String, List<TypeElement>> byPackage = new TreeMap<>();
        for (TypeElement type : types) {
            String pkg = elements.getPackageOf(type).getQualifiedName().toString();
            if (isHidden(pkg)) {
                continue;
            }
            byPackage.computeIfAbsent(pkg, k -> new ArrayList<>()).add(type);
        }

        Json json = new Json();
        json.beginObject();
        json.field("name", moduleName);
        if (!moduleDescription.isEmpty()) {
            json.field("description", moduleDescription);
        }
        json.name("packages").beginArray();
        for (Map.Entry<String, List<TypeElement>> entry : byPackage.entrySet()) {
            json.beginObject().field("name", entry.getKey()).name("types").beginArray();
            entry.getValue().sort(Comparator.comparing(this::displayName));
            for (TypeElement type : entry.getValue()) {
                writeType(json, type);
            }
            json.endArray().endObject();
        }
        json.endArray().endObject();

        try {
            Path path = Paths.get(out);
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(json.toString());
                writer.write('\n');
            }
        } catch (IOException e) {
            reporter.print(javax.tools.Diagnostic.Kind.ERROR, "Could not write " + out + ": " + e);
            return false;
        }
        return true;
    }

    static boolean isHidden(String packageName) {
        for (String part : packageName.split("\\.")) {
            if (part.equals("internal") || part.equals("common")) {
                return true;
            }
        }
        return false;
    }

    private void collect(TypeElement type, Set<TypeElement> into) {
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            return;
        }
        into.add(type);
        for (TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements())) {
            collect(nested, into);
        }
    }

    private String displayName(TypeElement type) {
        PackageElement pkg = elements.getPackageOf(type);
        String qualified = type.getQualifiedName().toString();
        return pkg.isUnnamed()
                ? qualified
                : qualified.substring(pkg.getQualifiedName().length() + 1);
    }

    private void writeType(Json json, TypeElement type) {
        DocCommentTree doc = trees.getDocCommentTree(type);
        json.beginObject();
        json.field("name", displayName(type));
        json.field("qualifiedName", type.getQualifiedName().toString());
        json.field("kind", kindOf(type));
        json.field("signature", typeSignature(type));
        json.field("doc", doc == null ? "" : text(doc.getFullBody()));
        writeSinceAndDeprecated(json, type, doc);
        json.name("members").beginArray();
        boolean extensible = isExtensible(type);
        for (Element member : type.getEnclosedElements()) {
            Set<Modifier> modifiers = member.getModifiers();
            boolean visible = modifiers.contains(Modifier.PUBLIC)
                    || (extensible && modifiers.contains(Modifier.PROTECTED))
                    || type.getKind() == ElementKind.INTERFACE && !modifiers.contains(Modifier.PRIVATE);
            if (!visible || elements.getOrigin(member) == Elements.Origin.SYNTHETIC) {
                continue;
            }
            switch (member.getKind()) {
                case METHOD:
                    if (elements.getOrigin(member) != Elements.Origin.MANDATED) {
                        writeExecutable(json, (ExecutableElement) member, "method");
                    }
                    break;
                case CONSTRUCTOR:
                    writeExecutable(json, (ExecutableElement) member, "constructor");
                    break;
                case FIELD:
                    writeField(json, (VariableElement) member, "field");
                    break;
                case ENUM_CONSTANT:
                    writeField(json, (VariableElement) member, "enumConstant");
                    break;
                default:
                    break;
            }
        }
        json.endArray();
        json.endObject();
    }

    private static boolean isExtensible(TypeElement type) {
        return type.getKind() == ElementKind.CLASS && !type.getModifiers().contains(Modifier.FINAL);
    }

    private static String kindOf(TypeElement type) {
        switch (type.getKind()) {
            case INTERFACE:
                return "interface";
            case ENUM:
                return "enum";
            case ANNOTATION_TYPE:
                return "annotation";
            case RECORD:
                return "record";
            default:
                return type.getModifiers().contains(Modifier.ABSTRACT) ? "abstract class" : "class";
        }
    }

    private String typeSignature(TypeElement type) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifiers(type.getModifiers(), type.getKind() == ElementKind.INTERFACE));
        String kind = kindOf(type);
        sb.append(kind.equals("abstract class") ? "class" : kind.equals("annotation") ? "@interface" : kind);
        sb.append(' ').append(type.getSimpleName()).append(typeParameters(type.getTypeParameters()));
        TypeMirror superclass = type.getSuperclass();
        String superName = simplify(superclass.toString());
        if (type.getKind() == ElementKind.CLASS && !superName.equals("Object") && !superName.equals("none")) {
            sb.append(" extends ").append(superName);
        }
        List<String> interfaces =
                type.getInterfaces().stream().map(t -> simplify(t.toString())).collect(Collectors.toList());
        if (!interfaces.isEmpty()) {
            sb.append(type.getKind() == ElementKind.INTERFACE ? " extends " : " implements ")
                    .append(String.join(", ", interfaces));
        }
        return sb.toString();
    }

    private void writeExecutable(Json json, ExecutableElement method, String kind) {
        DocCommentTree doc = trees.getDocCommentTree(method);
        StringBuilder sig = new StringBuilder();
        sig.append(modifiers(method.getModifiers(), method.getEnclosingElement().getKind() == ElementKind.INTERFACE));
        String typeParams = typeParameters(method.getTypeParameters());
        if (!typeParams.isEmpty()) {
            sig.append(typeParams).append(' ');
        }
        String name = kind.equals("constructor")
                ? method.getEnclosingElement().getSimpleName().toString()
                : method.getSimpleName().toString();
        if (kind.equals("method")) {
            sig.append(simplify(method.getReturnType().toString())).append(' ');
        }
        sig.append(name).append('(');
        List<String> params = new ArrayList<>();
        List<? extends VariableElement> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement p = parameters.get(i);
            String type = simplify(p.asType().toString());
            if (method.isVarArgs() && i == parameters.size() - 1 && type.endsWith("[]")) {
                type = type.substring(0, type.length() - 2) + "...";
            }
            params.add(type + " " + p.getSimpleName());
        }
        sig.append(String.join(", ", params)).append(')');
        if (!method.getThrownTypes().isEmpty()) {
            sig.append(" throws ")
                    .append(method.getThrownTypes().stream()
                            .map(t -> simplify(t.toString()))
                            .collect(Collectors.joining(", ")));
        }

        json.beginObject();
        json.field("kind", kind);
        json.field("name", name);
        json.field("signature", sig.toString());
        if (kind.equals("method")) {
            json.field("returns", simplify(method.getReturnType().toString()));
        }
        json.field("doc", doc == null ? "" : text(doc.getFullBody()));
        json.name("params").beginArray();
        for (VariableElement p : parameters) {
            json.beginObject()
                    .field("name", p.getSimpleName().toString())
                    .field("type", simplify(p.asType().toString()))
                    .field("doc", paramDoc(doc, p.getSimpleName().toString()))
                    .endObject();
        }
        json.endArray();
        String returnsDoc = blockText(doc, DocTree.Kind.RETURN);
        if (!returnsDoc.isEmpty()) {
            json.field("returnsDoc", returnsDoc);
        }
        json.name("throws").beginArray();
        if (doc != null) {
            for (DocTree tag : doc.getBlockTags()) {
                if (tag.getKind() == DocTree.Kind.THROWS || tag.getKind() == DocTree.Kind.EXCEPTION) {
                    ThrowsTree t = (ThrowsTree) tag;
                    json.beginObject()
                            .field("type", simplify(t.getExceptionName().getSignature()))
                            .field("doc", text(t.getDescription()))
                            .endObject();
                }
            }
        }
        json.endArray();
        writeSinceAndDeprecated(json, method, doc);
        json.endObject();
    }

    private void writeField(Json json, VariableElement field, String kind) {
        DocCommentTree doc = trees.getDocCommentTree(field);
        String sig = kind.equals("enumConstant")
                ? field.getSimpleName().toString()
                : modifiers(field.getModifiers(), false)
                        + simplify(field.asType().toString()) + " " + field.getSimpleName();
        json.beginObject();
        json.field("kind", kind);
        json.field("name", field.getSimpleName().toString());
        json.field("signature", sig);
        json.field("doc", doc == null ? "" : text(doc.getFullBody()));
        json.name("params").beginArray().endArray();
        json.name("throws").beginArray().endArray();
        writeSinceAndDeprecated(json, field, doc);
        json.endObject();
    }

    private void writeSinceAndDeprecated(Json json, Element element, DocCommentTree doc) {
        String since = blockText(doc, DocTree.Kind.SINCE);
        if (!since.isEmpty()) {
            json.field("since", since);
        }
        boolean deprecated = elements.isDeprecated(element);
        if (deprecated) {
            json.field("deprecated", blockText(doc, DocTree.Kind.DEPRECATED));
        }
    }

    private String paramDoc(DocCommentTree doc, String name) {
        if (doc == null) {
            return "";
        }
        for (DocTree tag : doc.getBlockTags()) {
            if (tag.getKind() == DocTree.Kind.PARAM) {
                ParamTree param = (ParamTree) tag;
                if (!param.isTypeParameter() && param.getName().getName().contentEquals(name)) {
                    return text(param.getDescription());
                }
            }
        }
        return "";
    }

    private String blockText(DocCommentTree doc, DocTree.Kind kind) {
        if (doc == null) {
            return "";
        }
        for (DocTree tag : doc.getBlockTags()) {
            if (tag.getKind() != kind) {
                continue;
            }
            switch (kind) {
                case RETURN:
                    return text(((ReturnTree) tag).getDescription());
                case SINCE:
                    return text(((SinceTree) tag).getBody());
                case DEPRECATED:
                    return text(((DeprecatedTree) tag).getBody());
                default:
                    return "";
            }
        }
        return "";
    }

    /** Converts doc trees to plain text: inline tags become their text, HTML tags become spacing. */
    static String text(List<? extends DocTree> body) {
        StringBuilder sb = new StringBuilder();
        for (DocTree tree : body) {
            switch (tree.getKind()) {
                case TEXT:
                    sb.append(((TextTree) tree).getBody());
                    break;
                case CODE:
                case LITERAL:
                    sb.append(((LiteralTree) tree).getBody().getBody());
                    break;
                case LINK:
                case LINK_PLAIN:
                    LinkTree link = (LinkTree) tree;
                    String label = text(link.getLabel()).trim();
                    sb.append(label.isEmpty() ? reference(link.getReference().getSignature()) : label);
                    break;
                case VALUE:
                    ValueTree value = (ValueTree) tree;
                    sb.append(
                            value.getReference() == null
                                    ? ""
                                    : reference(value.getReference().getSignature()));
                    break;
                case ENTITY:
                    sb.append(entity(((EntityTree) tree).getName().toString()));
                    break;
                case START_ELEMENT:
                    String tag = ((StartElementTree) tree).getName().toString().toLowerCase(Locale.ROOT);
                    sb.append(tag.equals("li") ? " • " : " ");
                    break;
                case END_ELEMENT:
                    sb.append(' ');
                    break;
                default:
                    break;
            }
        }
        return WHITESPACE.matcher(sb).replaceAll(" ").trim();
    }

    /** {@code Items#setData(ItemStack, String, String)} becomes {@code Items.setData}. */
    static String reference(String signature) {
        String ref = signature;
        int paren = ref.indexOf('(');
        if (paren >= 0) {
            ref = ref.substring(0, paren);
        }
        ref = simplify(ref);
        if (ref.startsWith("#")) {
            ref = ref.substring(1);
        }
        return ref.replace('#', '.');
    }

    private static String entity(String name) {
        switch (name) {
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "amp":
                return "&";
            case "quot":
                return "\"";
            case "nbsp":
                return " ";
            default:
                return "&" + name + ";";
        }
    }

    /** Drops package prefixes: {@code java.util.List<org.bukkit.inventory.ItemStack>} becomes {@code List<ItemStack>}. */
    static String simplify(String type) {
        return PACKAGE_PREFIX.matcher(type).replaceAll("");
    }

    private static String typeParameters(List<? extends TypeParameterElement> params) {
        if (params.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (TypeParameterElement p : params) {
            List<String> bounds = p.getBounds().stream()
                    .map(b -> simplify(b.toString()))
                    .filter(b -> !b.equals("Object"))
                    .collect(Collectors.toList());
            parts.add(p.getSimpleName() + (bounds.isEmpty() ? "" : " extends " + String.join(" & ", bounds)));
        }
        return "<" + String.join(", ", parts) + ">";
    }

    private static String modifiers(Set<Modifier> modifiers, boolean inInterface) {
        StringBuilder sb = new StringBuilder();
        for (Modifier m : Arrays.asList(
                Modifier.PUBLIC,
                Modifier.PROTECTED,
                Modifier.ABSTRACT,
                Modifier.DEFAULT,
                Modifier.STATIC,
                Modifier.FINAL)) {
            if (!modifiers.contains(m) || (inInterface && m == Modifier.ABSTRACT)) {
                continue;
            }
            sb.append(m.toString()).append(' ');
        }
        return sb.toString();
    }

    private abstract static class BaseOption implements Option {
        private final String name;
        private final int arguments;

        BaseOption(String name, int arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public int getArgumentCount() {
            return arguments;
        }

        @Override
        public String getDescription() {
            return name;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return arguments == 0 ? "" : "<value>";
        }
    }

    private static final class ValueOption extends BaseOption {
        private final java.util.function.Consumer<String> sink;

        ValueOption(String name, java.util.function.Consumer<String> sink) {
            super(name, 1);
            this.sink = sink;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            sink.accept(arguments.get(0));
            return true;
        }
    }

    private static final class FlagOption extends BaseOption {
        FlagOption(String name) {
            super(name, 0);
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            return true;
        }
    }
}
