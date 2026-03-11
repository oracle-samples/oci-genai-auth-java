/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import java.net.URI;
import java.util.Locale;

/**
 * Resolves the OCI Generative AI service base URL from region, service endpoint,
 * or an explicit base URL override.
 *
 * <p>Resolution priority (highest to lowest):
 * <ol>
 *   <li>{@code baseUrl} — fully qualified OCI URL, used as-is (HTTPS + OCI domain required)</li>
 *   <li>{@code serviceEndpoint} — service root; must be an OCI domain ({@code *.oraclecloud.com})</li>
 *   <li>{@code region} — auto-derives the service endpoint from the OCI region code</li>
 * </ol>
 */
public final class OciEndpointResolver {

    private static final String SERVICE_ENDPOINT_TEMPLATE =
            "https://inference.generativeai.%s.oci.oraclecloud.com";

    private static final String OCI_DOMAIN_SUFFIX = ".oraclecloud.com";

    private OciEndpointResolver() {
        // utility class
    }

    /**
     * Resolves a base URL with a caller-supplied API path suffix.
     *
     * @param region          OCI region code (e.g., "us-chicago-1")
     * @param serviceEndpoint service root URL (without API path; must be an OCI domain)
     * @param baseUrl         fully qualified OCI URL override (used as-is when provided)
     * @param apiPath         the API path to append to the service endpoint
     * @return the resolved base URL
     * @throws IllegalArgumentException if none of region, serviceEndpoint, or baseUrl is provided
     */
    public static String resolveBaseUrl(String region, String serviceEndpoint, String baseUrl, String apiPath) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            validateUrl(baseUrl, "baseUrl");
            return stripTrailingSlash(baseUrl);
        }

        if (serviceEndpoint != null && !serviceEndpoint.isBlank()) {
            validateUrl(serviceEndpoint, "serviceEndpoint");
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

    private static void validateUrl(String url, String paramName) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(paramName + " must be a valid absolute HTTPS URL: " + url, e);
        }

        if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(paramName + " must use HTTPS: " + url);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(paramName + " must include a valid host: " + url);
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException(paramName + " must not include user-info: " + url);
        }
        if (!host.toLowerCase(Locale.ROOT).endsWith(OCI_DOMAIN_SUFFIX)) {
            throw new IllegalArgumentException(
                    paramName + " must be an OCI domain (*" + OCI_DOMAIN_SUFFIX + "): " + url);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
