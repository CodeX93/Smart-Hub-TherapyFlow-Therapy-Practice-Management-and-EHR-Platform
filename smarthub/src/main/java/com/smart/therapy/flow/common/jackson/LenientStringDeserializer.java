package com.smart.therapy.flow.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Accepts JSON strings, numbers, and common select/dropdown object shapes for optional text fields.
 */
public class LenientStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            return value != null && value.isBlank() ? null : value.trim();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            for (String field : new String[] {"optionKey", "value", "key", "id", "label", "optionLabel", "name"}) {
                JsonNode candidate = node.get(field);
                if (candidate != null && !candidate.isNull()) {
                    if (candidate.isTextual()) {
                        String value = candidate.asText();
                        return value.isBlank() ? null : value.trim();
                    }
                    if (candidate.isNumber()) {
                        return candidate.asText();
                    }
                }
            }
        }
        String fallback = node.asText(null);
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }
}
