/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OciEndpointResolverTest {

    @Test
    void resolveBaseUrl_fromRegion() {
        String url = OciEndpointResolver.resolveBaseUrl("us-chicago-1", null, null, "/v1/test");
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/v1/test",
                url);
    }

    @Test
    void resolveBaseUrl_fromServiceEndpoint() {
        String url = OciEndpointResolver.resolveBaseUrl(
                null, "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com", null, "/v1/test");
        assertEquals(
                "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/v1/test",
                url);
    }

    @Test
    void resolveBaseUrl_fromBaseUrl() {
        String url = OciEndpointResolver.resolveBaseUrl(
                null, null, "https://custom-endpoint.oci.oraclecloud.com/v1", "/ignored");
        assertEquals("https://custom-endpoint.oci.oraclecloud.com/v1", url);
    }

    @Test
    void resolveBaseUrl_baseUrlTakesPrecedence() {
        String url = OciEndpointResolver.resolveBaseUrl(
                "us-chicago-1",
                "https://service.oci.oraclecloud.com",
                "https://override.oci.oraclecloud.com/v1",
                "/v1/test");
        assertEquals("https://override.oci.oraclecloud.com/v1", url);
    }

    @Test
    void resolveBaseUrl_serviceEndpointTakesPrecedenceOverRegion() {
        String url = OciEndpointResolver.resolveBaseUrl(
                "us-chicago-1",
                "https://custom-service.oci.oraclecloud.com",
                null,
                "/v1/test");
        assertEquals("https://custom-service.oci.oraclecloud.com/v1/test", url);
    }

    @Test
    void resolveBaseUrl_stripsTrailingSlash() {
        String url = OciEndpointResolver.resolveBaseUrl(
                null, "https://service.oraclecloud.com/", null, "/v1/test");
        assertEquals("https://service.oraclecloud.com/v1/test", url);
    }

    @Test
    void resolveBaseUrl_serviceEndpointRejectsNonOciDomain() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(
                        null, "https://evil.example.com", null, "/v1/test"));
    }

    @Test
    void resolveBaseUrl_serviceEndpointAcceptsPpeDomain() {
        String url = OciEndpointResolver.resolveBaseUrl(
                null,
                "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com",
                null,
                "/v1/test");
        assertEquals(
                "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/v1/test",
                url);
    }

    @Test
    void resolveBaseUrl_baseUrlRejectsNonOciDomain() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(
                        null, null, "https://custom-proxy.example.com/v1", "/ignored"));
    }

    @Test
    void resolveBaseUrl_baseUrlRejectsMissingHost() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(
                        null, null, "https://", "/ignored"));
    }

    @Test
    void resolveBaseUrl_baseUrlRejectsUserInfo() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(
                        null, null, "https://user:pass@inference.generativeai.us-chicago-1.oci.oraclecloud.com/v1", "/ignored"));
    }

    @Test
    void resolveBaseUrl_serviceEndpointRejectsMissingHost() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(
                        null, "https://", null, "/v1/test"));
    }

    @Test
    void resolveBaseUrl_throwsWhenNothingProvided() {
        assertThrows(IllegalArgumentException.class, () ->
                OciEndpointResolver.resolveBaseUrl(null, null, null, "/v1/test"));
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
