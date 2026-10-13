package io.github.crownalonedev.bloom.config.internal;

import io.github.crownalonedev.bloom.config.ConfigException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * SnakeYAML access: safe loading, line tracking, comment-preserving merges and atomic writes. Internal.
 *
 * <p>Comments are kept by editing the composed node tree of the existing file instead of dumping a fresh map: key
 * nodes (which carry block comments) are reused, unchanged values keep their original nodes, and new keys copy their
 * comments from the defaults file when available.
 */
public final class YamlIO {

    private static final int MAX_ALIASES = 50;
    private static final int MAX_CODE_POINTS = 8 * 1024 * 1024;

    private YamlIO() {}

    private static LoaderOptions loaderOptions(boolean comments) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(MAX_ALIASES);
        options.setCodePointLimit(MAX_CODE_POINTS);
        options.setProcessComments(comments);
        return options;
    }

    private static DumperOptions dumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        options.setWidth(160);
        options.setProcessComments(true);
        return options;
    }

    private static Yaml valueYaml() {
        LoaderOptions loader = loaderOptions(false);
        return new Yaml(new SafeConstructor(loader), new Representer(dumperOptions()), dumperOptions(), loader);
    }

    private static Yaml nodeYaml() {
        LoaderOptions loader = loaderOptions(true);
        return new Yaml(new SafeConstructor(loader), new Representer(dumperOptions()), dumperOptions(), loader);
    }

    /**
     * Parses YAML text into a tree of maps, lists and scalars.
     *
     * @param text YAML text
     * @param source file name for error messages
     * @return root map (empty for an empty document)
     * @throws ConfigException if the text is not valid YAML or the root is not a mapping
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String text, String source) {
        Object loaded;
        try {
            loaded = valueYaml().load(text);
        } catch (MarkedYAMLException e) {
            Mark mark = e.getProblemMark() != null ? e.getProblemMark() : e.getContextMark();
            String where =
                    mark == null ? "" : " at line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1);
            throw new ConfigException(
                    "Invalid YAML in " + source + where + ": " + firstLine(e.getProblem())
                            + ". Check indentation (spaces only, no tabs) and quoting.",
                    e);
        } catch (YAMLException e) {
            throw new ConfigException("Invalid YAML in " + source + ": " + firstLine(e.getMessage()), e);
        }
        if (loaded == null) {
            return new LinkedHashMap<String, Object>();
        }
        if (!(loaded instanceof Map)) {
            throw new ConfigException(source + " must contain key: value pairs at the top level, but contains a "
                    + loaded.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT) + ".");
        }
        return (Map<String, Object>) ConfigPaths.deepCopy(loaded);
    }

    /**
     * Composes the node tree (with comments) of YAML text.
     *
     * @param text YAML text
     * @return root node, or {@code null} for an empty document
     */
    public static Node compose(String text) {
        return nodeYaml().compose(new StringReader(text));
    }

    /**
     * Records the 1-based line of every key path in a node tree.
     *
     * @param root root node, may be {@code null}
     * @return path to line
     */
    public static Map<String, Integer> lines(Node root) {
        Map<String, Integer> lines = new HashMap<String, Integer>();
        if (root instanceof MappingNode) {
            collectLines((MappingNode) root, "", lines);
        }
        return lines;
    }

    private static void collectLines(MappingNode node, String prefix, Map<String, Integer> lines) {
        for (NodeTuple tuple : node.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode)) {
                continue;
            }
            String key = ((ScalarNode) tuple.getKeyNode()).getValue();
            String path = ConfigPaths.join(prefix, key);
            lines.put(path, tuple.getKeyNode().getStartMark().getLine() + 1);
            if (tuple.getValueNode() instanceof MappingNode) {
                collectLines((MappingNode) tuple.getValueNode(), path, lines);
            }
        }
    }

    /**
     * Produces YAML for {@code desired} while preserving the comments and formatting of {@code existingText}.
     *
     * @param existingText current file content, may be {@code null} or empty
     * @param current values parsed from {@code existingText}
     * @param desired values to write
     * @param defaultsText defaults file content used to copy comments for new keys, may be {@code null}
     * @return YAML text
     */
    public static String render(
            String existingText, Map<String, Object> current, Map<String, Object> desired, String defaultsText) {
        Node existing = existingText == null || existingText.trim().isEmpty() ? null : compose(existingText);
        Node defaults = defaultsText == null || defaultsText.trim().isEmpty() ? null : compose(defaultsText);
        Yaml yaml = nodeYaml();
        MappingNode root = existing instanceof MappingNode
                ? (MappingNode) existing
                : new MappingNode(Tag.MAP, new ArrayList<NodeTuple>(), DumperOptions.FlowStyle.BLOCK);
        Node originalFirstKey =
                root.getValue().isEmpty() ? null : root.getValue().get(0).getKeyNode();
        merge(yaml, root, current, desired, defaults instanceof MappingNode ? (MappingNode) defaults : null);
        keepHeaderComment(root, originalFirstKey);
        if (root.getValue().isEmpty()) {
            return "{}\n";
        }
        StringWriter out = new StringWriter();
        yaml.serialize(root, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void merge(
            Yaml yaml,
            MappingNode node,
            Map<String, Object> current,
            Map<String, Object> desired,
            MappingNode defaults) {
        List<NodeTuple> result = new ArrayList<NodeTuple>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (NodeTuple tuple : node.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode)) {
                continue;
            }
            String key = ((ScalarNode) tuple.getKeyNode()).getValue();
            if (!desired.containsKey(key) || !seen.add(key)) {
                continue; // removed
            }
            Object want = desired.get(key);
            Object have = current == null ? null : current.get(key);
            Node value = tuple.getValueNode();
            if (want instanceof Map && value instanceof MappingNode) {
                merge(
                        yaml,
                        (MappingNode) value,
                        have instanceof Map ? (Map<String, Object>) have : null,
                        (Map<String, Object>) want,
                        childMapping(defaults, key));
            } else if (!Objects.equals(have, want)) {
                Node replacement = represent(yaml, want);
                replacement.setInLineComments(value.getInLineComments());
                value = replacement;
            }
            result.add(new NodeTuple(tuple.getKeyNode(), value));
        }
        for (Map.Entry<String, Object> entry : desired.entrySet()) {
            if (seen.contains(entry.getKey())) {
                continue;
            }
            NodeTuple template = childTuple(defaults, entry.getKey());
            Node keyNode;
            Node valueNode;
            if (template != null) {
                keyNode = template.getKeyNode();
                if (entry.getValue() instanceof Map && template.getValueNode() instanceof MappingNode) {
                    MappingNode fresh =
                            new MappingNode(Tag.MAP, new ArrayList<NodeTuple>(), DumperOptions.FlowStyle.BLOCK);
                    merge(yaml, fresh, null, (Map<String, Object>) entry.getValue(), (MappingNode)
                            template.getValueNode());
                    valueNode = fresh;
                } else {
                    valueNode = represent(yaml, entry.getValue());
                    valueNode.setInLineComments(template.getValueNode().getInLineComments());
                }
            } else {
                keyNode = new ScalarNode(Tag.STR, entry.getKey(), null, null, DumperOptions.ScalarStyle.PLAIN);
                valueNode = represent(yaml, entry.getValue());
            }
            result.add(new NodeTuple(keyNode, valueNode));
        }
        node.setValue(result);
    }

    /**
     * SnakeYAML attaches a file's header comment to the first key. If that key was removed or moved, carry the
     * header over to the new first key so it is not lost.
     */
    private static void keepHeaderComment(MappingNode root, Node originalFirstKey) {
        if (originalFirstKey == null || root.getValue().isEmpty()) {
            return;
        }
        Node newFirstKey = root.getValue().get(0).getKeyNode();
        if (newFirstKey == originalFirstKey || originalFirstKey.getBlockComments() == null) {
            return;
        }
        List<org.yaml.snakeyaml.comments.CommentLine> merged =
                new ArrayList<org.yaml.snakeyaml.comments.CommentLine>(originalFirstKey.getBlockComments());
        if (newFirstKey.getBlockComments() != null) {
            merged.addAll(newFirstKey.getBlockComments());
        }
        newFirstKey.setBlockComments(merged);
    }

    private static Node represent(Yaml yaml, Object value) {
        return yaml.represent(value);
    }

    private static NodeTuple childTuple(MappingNode parent, String key) {
        if (parent == null) {
            return null;
        }
        for (NodeTuple tuple : parent.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode
                    && ((ScalarNode) tuple.getKeyNode()).getValue().equals(key)) {
                return tuple;
            }
        }
        return null;
    }

    private static MappingNode childMapping(MappingNode parent, String key) {
        NodeTuple tuple = childTuple(parent, key);
        return tuple != null && tuple.getValueNode() instanceof MappingNode ? (MappingNode) tuple.getValueNode() : null;
    }

    /**
     * Reads a UTF-8 file.
     *
     * @param file file
     * @return content
     * @throws IOException on read failure
     */
    public static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /**
     * Writes a file atomically: the content goes to a temporary file in the same directory, which then replaces the
     * target. Readers never observe a half-written file.
     *
     * @param file target
     * @param content UTF-8 content
     * @throws IOException on write failure
     */
    public static void writeAtomically(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "syntax error";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }
}
