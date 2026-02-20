/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.core.endpoint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OciEndpointResolverTest {

    @Test
    void resolveOpenAiBaseUrl_fromRegion() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl("us-chicago-1", null, null);
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/openai/v1",
                url);
    }

    @Test
    void resolveOpenAiBaseUrl_fromServiceEndpoint() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl(
                null, "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com", null);
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/openai/v1",
                url);
    }

    @Test
    void resolveOpenAiBaseUrl_fromBaseUrl() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl(
                null, null, "https://custom-endpoint.example.com/openai/v1");
        assertEquals("https://custom-endpoint.example.com/openai/v1", url);
    }

    @Test
    void resolveOpenAiBaseUrl_baseUrlTakesPrecedence() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl(
                "us-chicago-1",
                "https://service.example.com",
                "https://override.example.com/v1");
        assertEquals("https://override.example.com/v1", url);
    }

    @Test
    void resolveOpenAiBaseUrl_serviceEndpointTakesPrecedenceOverRegion() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl(
                "us-chicago-1",
                "https://custom-service.example.com",
                null);
        assertEquals("https://custom-service.example.com/20231130/openai/v1", url);
    }

    @Test
    void resolveOpenAiBaseUrl_stripsTrailingSlash() {
        String url = OciEndpointResolver.resolveOpenAiBaseUrl(
                null, "https://service.example.com/", null);
        assertEquals("https://service.example.com/20231130/openai/v1", url);
    }

    @Test
    void resolveAnthropicBaseUrl_fromRegion() {
        String url = OciEndpointResolver.resolveAnthropicBaseUrl("us-chicago-1", null, null);
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/anthropic",
                url);
    }

    @Test
    void resolveBaseUrl_throwsWhenNothingProvided() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveOpenAiBaseUrl(null, null, null));
    }

    @Test
    void buildServiceEndpoint_fromRegion() {
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com",
                OciEndpointResolver.buildServiceEndpoint("us-chicago-1"));
    }

    @Test
    void buildServiceEndpoint_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.buildServiceEndpoint(null));
    }
}
