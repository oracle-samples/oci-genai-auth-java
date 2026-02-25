/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OciAnthropic} builder validation logic.
 *
 * <p>These tests verify builder parameter validation and mode selection
 * without making any network calls.
 */
class OciAnthropicTest {

    @Test
    void builder_throwsWhenNoAuthProvided() {
        OciAnthropic.Builder builder = OciAnthropic.builder()
                .region("us-chicago-1")
                .compartmentId("ocid1.compartment.oc1..test");

        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when neither authType, authProvider, nor apiKey is provided");
    }

    @Test
    void builder_throwsWhenApiKeyAuthTypeButNoKey() {
        OciAnthropic.Builder builder = OciAnthropic.builder()
                .authType("api_key")
                .region("us-chicago-1");

        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when authType is 'api_key' but no apiKey is set");
    }

    @Test
    void builder_apiKeyModeReturnsClient() {
        var client = OciAnthropic.builder()
                .apiKey("sk-test-key-12345")
                .baseUrl("https://example.com/anthropic")
                .build();

        assertNotNull(client, "API key mode should return a valid client");
        client.close();
    }

    @Test
    void builder_apiKeyModeWithRegionReturnsClient() {
        var client = OciAnthropic.builder()
                .apiKey("sk-test-key-12345")
                .region("us-chicago-1")
                .build();

        assertNotNull(client, "API key mode with region should return a valid client");
        client.close();
    }

    @Test
    void builder_throwsWhenNoEndpointInfo() {
        OciAnthropic.Builder builder = OciAnthropic.builder()
                .apiKey("sk-test-key-12345");

        assertThrows(IllegalArgumentException.class, builder::build,
                "Should throw when no endpoint information is provided");
    }

    @Test
    void builder_apiKeyModeWithAuthTypeReturnsClient() {
        var client = OciAnthropic.builder()
                .authType("api_key")
                .apiKey("sk-test-key-12345")
                .baseUrl("https://example.com/anthropic")
                .build();

        assertNotNull(client, "Explicit api_key authType should return a valid client");
        client.close();
    }
}
