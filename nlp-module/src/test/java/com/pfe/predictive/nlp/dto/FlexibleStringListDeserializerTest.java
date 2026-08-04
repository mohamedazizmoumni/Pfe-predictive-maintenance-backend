package com.pfe.predictive.nlp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Python NLP service sometimes returns `keywords` as a JSON array and
 * sometimes as a single comma-separated string (depending on model path).
 * This deserializer (wired onto NlpResponseDTO.keywords) must normalize
 * both shapes — plus null/blank/whitespace noise — into a clean list.
 */
class FlexibleStringListDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private List<String> deserializeKeywords(String keywordsJson) throws Exception {
        String json = "{\"keywords\": " + keywordsJson + "}";
        NlpResponseDTO dto = mapper.readValue(json, NlpResponseDTO.class);
        return dto.getKeywords();
    }

    @Test
    void parsesJsonArray() throws Exception {
        assertEquals(List.of("bearing", "vibration"), deserializeKeywords("[\"bearing\", \"vibration\"]"));
    }

    @Test
    void trimsAndDropsBlankEntriesInArray() throws Exception {
        assertEquals(List.of("bearing", "vibration"), deserializeKeywords("[\"  bearing \", \"\", \"  \", \"vibration\"]"));
    }

    @Test
    void dropsNullEntriesInArray() throws Exception {
        assertEquals(List.of("bearing"), deserializeKeywords("[\"bearing\", null]"));
    }

    @Test
    void parsesCommaSeparatedString() throws Exception {
        assertEquals(List.of("bearing", "vibration", "overheating"), deserializeKeywords("\"bearing, vibration,overheating\""));
    }

    @Test
    void blankStringYieldsEmptyList() throws Exception {
        assertTrue(deserializeKeywords("\"   \"").isEmpty());
    }

    @Test
    void emptyStringYieldsEmptyList() throws Exception {
        assertTrue(deserializeKeywords("\"\"").isEmpty());
    }

    @Test
    void jsonNullYieldsEmptyList() throws Exception {
        assertTrue(deserializeKeywords("null").isEmpty());
    }

    @Test
    void missingFieldYieldsNullList() throws Exception {
        NlpResponseDTO dto = mapper.readValue("{}", NlpResponseDTO.class);
        assertEquals(null, dto.getKeywords());
    }

    @Test
    void csvStringWithTrailingCommaDoesNotProduceBlankEntry() throws Exception {
        assertEquals(List.of("bearing", "vibration"), deserializeKeywords("\"bearing, vibration,\""));
    }
}
