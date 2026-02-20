/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.core.endpoint;

/**
 * Resolves the OCI Generative AI service base URL from region, service endpoint,
 * or an explicit base URL override.
 *
 * <p>Resolution priority (highest to lowest):
 * <ol>
 *   <li>{@code baseUrl} — fully qualified URL, used as-is</li>
 *   <li>{@code serviceEndpoint} — service root; the provider-specific API path is appended</li>
 *   <li>{@code region} — auto-derives the service endpoint from the OCI region code</li>
 * </ol>
 */
public final class OciEndpointResolver {

    private static final String SERVICE_ENDPOINT_TEMPLATE =
            "https://inference.generativeai.%s.oci.oraclecloud.com";

    private OciEndpointResolver() {
        // utility class
    }

    /**
     * Resolves the base URL for an OpenAI-compatible endpoint.
     * Appends {@code /openai/v1} to the service endpoint.
     */
    public static String resolveOpenAiBaseUrl(String region, String serviceEndpoint, String baseUrl) {
        return resolveBaseUrl(region, serviceEndpoint, baseUrl, "/20231130/openai/v1");
    }

    /**
     * Resolves the base URL for an Anthropic-compatible endpoint.
     * Appends {@code /20231130/anthropic} to the service endpoint.
     */
    public static String resolveAnthropicBaseUrl(String region, String serviceEndpoint, String baseUrl) {
        return resolveBaseUrl(region, serviceEndpoint, baseUrl, "/20231130/anthropic");
    }

    /**
     * Resolves a base URL with a custom API path suffix.
     *
     * @param region          OCI region code (e.g., "us-chicago-1")
     * @param serviceEndpoint service root URL (without API path)
     * @param baseUrl         fully qualified URL override
     * @param apiPath         the API path to append to the service endpoint
     * @return the resolved base URL
     * @throws IllegalArgumentException if none of region, serviceEndpoint, or baseUrl is provided
     */
    public static String resolveBaseUrl(String region, String serviceEndpoint, String baseUrl, String apiPath) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return stripTrailingSlash(baseUrl);
        }

        if (serviceEndpoint != null && !serviceEndpoint.isBlank()) {
            return stripTrailingSlash(serviceEndpoint) + apiPath;
        }

        if (region != null && !region.isBlank()) {
            return String.format(SERVICE_ENDPOINT_TEMPLATE, region) + apiPath;
        }

        throw new IllegalArgumentException(
                "At least one of region, serviceEndpoint, or baseUrl must be provided.");
    }

    /**
     * Builds the service endpoint URL from a region code.
     */
    public static String buildServiceEndpoint(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region must not be null or blank");
        }
        return String.format(SERVICE_ENDPOINT_TEMPLATE, region);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
