package org.cobbzilla.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FilenameSuffixFilter;
import org.cobbzilla.util.string.StringUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    public static final FileFilter JSON_FILES = new FileSuffixFilter(".json");
    public static final FilenameFilter JSON_FILENAMES = new FilenameSuffixFilter(".json");

    public static final ObjectMapper FULL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    public static final ObjectWriter FULL_WRITER = FULL_MAPPER.writer();

    public static final ObjectMapper NOTNULL_MAPPER = FULL_MAPPER
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    public static final ObjectMapper PUBLIC_MAPPER = buildMapper();

    public static final ObjectWriter PUBLIC_WRITER = buildWriter(PUBLIC_MAPPER, PublicView.class);

    public static ObjectMapper buildMapper() {
        return new ObjectMapper()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public static ObjectWriter buildWriter(Class<? extends PublicView> view) {
        return buildMapper().writerWithView(view);
    }
    public static ObjectWriter buildWriter(ObjectMapper mapper, Class<? extends PublicView> view) {
        return mapper.writerWithView(view);
    }

    public static class PublicView {}

    public static String toJson (Object o) throws Exception {
        return JsonUtil.NOTNULL_MAPPER.writeValueAsString(o);
    }

    public static String toJsonOrDie (Object o) {
        try {
            return toJson(o);
        } catch (Exception e) {
            throw new IllegalStateException("toJson: exception writing object ("+o+"): "+e, e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return JsonUtil.FULL_MAPPER.readValue(json, clazz);
    }

    public static <T> T fromJson(String json, JavaType type) throws Exception {
        return JsonUtil.FULL_MAPPER.readValue(json, type);
    }

    public static <T> T fromJsonOrDie(String json, Class<T> clazz) {
        if (StringUtil.empty(json)) return null;
        try {
            return JsonUtil.FULL_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("fromJson: exception while reading: "+json+": "+e, e);
        }
    }

    public static <T> T fromJson(String json, String path, Class<T> clazz) throws Exception {
        JsonNode node = findNode(FULL_MAPPER.readTree(json), path);
        return FULL_MAPPER.convertValue(node, clazz);
    }

    public static JsonNode findNode(JsonNode node, String path) throws IOException {
        final List<JsonNode> nodePath = findNodePath(node, path);
        return nodePath == null || nodePath.isEmpty() ? null : nodePath.get(nodePath.size()-1);
    }

    public static List<JsonNode> findNodePath(JsonNode node, String path) throws IOException {
        final List<JsonNode> nodePath = new ArrayList<>();
        final String[] pathParts = path.split("\\.");
        for (String pathPart : pathParts) {
            int index = -1;
            int bracketPos = pathPart.indexOf("[");
            int bracketClosePos = pathPart.indexOf("]");
            if (bracketPos != -1 && bracketClosePos != -1 && bracketClosePos > bracketPos) {
                index = Integer.parseInt(pathPart.substring(bracketPos+1, bracketClosePos));
                pathPart = pathPart.substring(0, bracketPos);
            }
            node = node.get(pathPart);
            if (node == null) throw new IllegalArgumentException("JSON path '"+path+"' not found");
            nodePath.add(node);
            if (index != -1) {
                node = node.get(index);
                nodePath.add(node);
            }
        }
        return nodePath;
    }

    public static ObjectNode replaceNode(File file, String path, String replacement) throws Exception {
        return replaceNode((ObjectNode) FULL_MAPPER.readTree(file), path, replacement);
    }

    public static ObjectNode replaceNode(String json, String path, String replacement) throws Exception {
        return replaceNode((ObjectNode) FULL_MAPPER.readTree(json), path, replacement);
    }

    public static ObjectNode replaceNode(ObjectNode document, String path, String replacement) throws Exception {

        final String simplePath = path.contains(".") ? path.substring(path.lastIndexOf(".")+1) : path;
        Integer index = null;
        if (simplePath.contains("[")) {
            index = Integer.parseInt(simplePath.substring(simplePath.indexOf("[")+1, simplePath.indexOf("]")));
        }
        final List<JsonNode> found = findNodePath(document, path);
        if (found == null || found.isEmpty()) throw new IllegalArgumentException("path not found: "+path);

        final JsonNode parent = found.size() > 1 ? found.get(found.size()-2) : document;
        if (index != null) {
            final JsonNode origNode = ((ArrayNode) parent).get(index);
            ((ArrayNode) parent).set(index, getValueNode(origNode, path, replacement));
        } else {
            // what is the original node type?
            final JsonNode origNode = parent.get(simplePath);
            ((ObjectNode) parent).put(simplePath, getValueNode(origNode, path, replacement));
        }
        return document;
    }

    public static JsonNode getValueNode(JsonNode node, String path, String replacement) {
        final String nodeClass = node.getClass().getName();
        if ( ! (node instanceof ValueNode) ) throw new IllegalStateException("Path "+path+" does not refer to a value (it is a "+ nodeClass +")");
        if (node instanceof TextNode) return new TextNode(replacement);
        if (node instanceof BooleanNode) return BooleanNode.valueOf(Boolean.parseBoolean(replacement));
        if (node instanceof IntNode) return new IntNode(Integer.parseInt(replacement));
        if (node instanceof LongNode) return new LongNode(Long.parseLong(replacement));
        if (node instanceof DoubleNode) return new DoubleNode(Double.parseDouble(replacement));
        if (node instanceof DecimalNode) return new DecimalNode(new BigDecimal(replacement));
        if (node instanceof BigIntegerNode) return new BigIntegerNode(new BigInteger(replacement));
        throw new IllegalArgumentException("Path "+path+" refers to an unsupported ValueNode: "+ nodeClass);
    }

}
