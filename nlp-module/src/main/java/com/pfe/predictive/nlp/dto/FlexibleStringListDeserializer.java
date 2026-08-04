package com.pfe.predictive.nlp.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlexibleStringListDeserializer extends JsonDeserializer<List<String>> {

    // Jackson short-circuits an explicit JSON `null` straight to `null`
    // without ever calling deserialize() — the VALUE_NULL branch below is
    // otherwise unreachable. Overriding this is what actually makes
    // `"keywords": null` normalize to an empty list instead of null.
    @Override
    public List<String> getNullValue(DeserializationContext context) {
        return Collections.emptyList();
    }

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.START_ARRAY) {
            JavaType listType = context.getTypeFactory().constructCollectionType(List.class, String.class);
            List<String> values = context.readValue(parser, listType);
            return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
        }

        if (token == JsonToken.VALUE_STRING) {
            String raw = parser.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return Collections.emptyList();
            }

            return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
        }

        if (token == JsonToken.VALUE_NULL) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }
}
