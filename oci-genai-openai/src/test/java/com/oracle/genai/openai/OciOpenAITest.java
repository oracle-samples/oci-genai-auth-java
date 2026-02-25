/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.openai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OciOpenAI} builder validation logic.
 *
 * <p>These tests verify builder parameter validation and mode selection
 * without making any network calls.
 */
class OciOpenAITest {

    @Test
    void builder_throwsWhenNoAuthProvided() {
        OciOpenAI.Builder builder = OciOpenAI.builder()
                .region("us-chicago-1")
                .compartmentId("ocid1.compartment.oc1..test");

        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when neither authType, authProvider, nor apiKey is provided");
    }

    @Test
    void builder_throwsWhenApiKeyAuthTypeButNoKey() {
        OciOpenAI.Builder builder = OciOpenAI.builder()
                .authType("api_key")
                .region("us-chicago-1");

        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when authType is 'api_key' but no apiKey is set");
    }

    @Test
    void builder_apiKeyModeReturnsClient() {
        // API key mode should build successfully without OCI credentials
        var client = OciOpenAI.builder()
                .apiKey("sk-test-key-12345")
                .baseUrl("https://example.com/v1")
                .build();

        assertNotNull(client, "API key mode should return a valid client");
        client.close();
    }

    @Test
    void builder_apiKeyModeWithRegionReturnsClient() {
        var client = OciOpenAI.builder()
                .apiKey("sk-test-key-12345")
                .region("us-chicago-1")
                .build();

        assertNotNull(client, "API key mode with region should return a valid client");
        client.close();
    }

    @Test
    void builder_throwsWhenNoEndpointInfo() {
        OciOpenAI.Builder builder = OciOpenAI.builder()
                .apiKey("sk-test-key-12345");

        // No region, serviceEndpoint, or baseUrl
        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when no endpoint information is provided");
    }

    @Test
    void builder_apiKeyModeWithAuthTypeReturnsClient() {
        var client = OciOpenAI.builder()
                .authType("api_key")
                .apiKey("sk-test-key-12345")
                .baseUrl("https://example.com/v1")
                .build();

        assertNotNull(client, "Explicit api_key authType should return a valid client");
        client.close();
    }
}
