package com.st20313779.fixtures;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

/**
 * Loads test data from src/test/resources/test-data.json for
 * Equivalence Partitioning and Boundary Value Analysis.
 *
 * RTM Mapping: Global test data provider
 */
public class TestDataLoader {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonNode testData;

    static {
        try (InputStream is = TestDataLoader.class.getResourceAsStream("/test-data.json")) {
            testData = mapper.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test-data.json", e);
        }
    }

    public static JsonNode getTestData() {
        return testData;
    }

    public static String getValidUuid() {
        return testData.path("boundaryValues").path("uuids").path("validUuid").asText();
    }

    public static String getValidDate() {
        return testData.path("boundaryValues").path("dates").path("validDate").asText();
    }

    public static String getInvalidDateFormat() {
        return testData.path("boundaryValues").path("dates").path("invalidFormat").asText();
    }

    public static String getSearchQueryValid() {
        return testData.path("equivalencePartitions").path("recipeSearch").path("partition1_validString").asText();
    }

    public static String getSearchQueryEmpty() {
        return testData.path("equivalencePartitions").path("recipeSearch").path("partition2_emptyString").asText();
    }

    public static String getSearchQuerySpecialChars() {
        return testData.path("equivalencePartitions").path("recipeSearch").path("partition3_specialChars").asText();
    }
}

