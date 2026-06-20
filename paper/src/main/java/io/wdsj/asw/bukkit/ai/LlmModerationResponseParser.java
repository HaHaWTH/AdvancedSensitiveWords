package io.wdsj.asw.bukkit.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;

final class LlmModerationResponseParser {
    static final int MAX_RAW_RESPONSE_BYTES = 8 * 1024;
    private static final Set<String> EXPECTED_FIELDS = Set.of(
            "category",
            "secondary_categories",
            "confidence",
            "severity",
            "signals",
            "explanation"
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private LlmModerationResponseParser() {
    }

    static Optional<LlmChatModerationResult> parse(String rawResponse) {
        if (rawResponse == null || rawResponse.getBytes(StandardCharsets.UTF_8).length > MAX_RAW_RESPONSE_BYTES) {
            return Optional.empty();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
            if (root == null || !root.isObject() || root.size() != EXPECTED_FIELDS.size()) {
                return Optional.empty();
            }
            Set<String> fields = new HashSet<>();
            root.fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(EXPECTED_FIELDS)) {
                return Optional.empty();
            }

            LlmModerationCategory category = LlmModerationCategory.fromWireName(requiredText(root, "category"));
            List<LlmModerationCategory> secondaryCategories = parseCategories(root.get("secondary_categories"));
            JsonNode confidenceNode = root.get("confidence");
            if (confidenceNode == null || !confidenceNode.isNumber()) {
                return Optional.empty();
            }
            double confidence = confidenceNode.doubleValue();
            LlmModerationSeverity severity = LlmModerationSeverity.fromWireName(requiredText(root, "severity"));
            List<String> signals = parseStrings(root.get("signals"));
            String explanation = requiredText(root, "explanation");
            return Optional.of(new LlmChatModerationResult(
                    category,
                    secondaryCategories,
                    confidence,
                    severity,
                    signals,
                    explanation
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("Expected a string for " + field);
        }
        return value.textValue();
    }

    private static List<LlmModerationCategory> parseCategories(JsonNode node) {
        List<String> names = parseStrings(node);
        List<LlmModerationCategory> categories = new ArrayList<>(names.size());
        for (String name : names) {
            categories.add(LlmModerationCategory.fromWireName(name));
        }
        return categories;
    }

    private static List<String> parseStrings(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("Expected an array");
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode value : node) {
            if (!value.isTextual()) {
                throw new IllegalArgumentException("Expected an array of strings");
            }
            values.add(value.textValue());
        }
        return values;
    }
}
