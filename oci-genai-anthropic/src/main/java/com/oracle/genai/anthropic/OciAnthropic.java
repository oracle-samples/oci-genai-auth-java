/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientImpl;
import com.anthropic.core.ClientOptions;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.genai.core.OciHttpClientFactory;
import com.oracle.genai.core.auth.OciAuthProviderFactory;
import com.oracle.genai.core.endpoint.OciEndpointResolver;

import java.time.Duration;

/**
 * OCI-authenticated Anthropic client builder.
 *
 * <p>Creates an {@link AnthropicClient} that routes requests through OCI Generative AI
 * endpoints with OCI IAM request signing. The underlying Anthropic Java SDK is used
 * for all API operations — users get the full Anthropic API surface (messages,
 * streaming, tool use) with OCI auth handled transparently.
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * AnthropicClient client = OciAnthropic.builder()
 *         .compartmentId("<COMPARTMENT_OCID>")
 *         .authType("security_token")
 *         .region("us-chicago-1")
 *         .build();
 *
 * Message message = client.messages().create(MessageCreateParams.builder()
 *         .model("anthropic.claude-3-sonnet")
 *         .addUserMessage("Hello from OCI!")
 *         .maxTokens(1024)
 *         .build());
 * }</pre>
 *
 * <h3>Authentication</h3>
 * <p>Supports all OCI IAM auth types via {@code authType}:
 * <ul>
 *   <li>{@code oci_config} — user principal from {@code ~/.oci/config}</li>
 *   <li>{@code security_token} — session token from OCI CLI</li>
 *   <li>{@code instance_principal} — OCI Compute instances</li>
 *   <li>{@code resource_principal} — OCI Functions, Container Instances</li>
 * </ul>
 * <p>Alternatively, pass a pre-built {@link BasicAuthenticationDetailsProvider}
 * via {@code authProvider()}.
 */
public final class OciAnthropic {

    private OciAnthropic() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String authType;
        private String profile;
        private BasicAuthenticationDetailsProvider authProvider;
        private String compartmentId;
        private String region;
        private String serviceEndpoint;
        private String baseUrl;
        private Duration timeout;
        private boolean logRequestsAndResponses;

        private Builder() {
        }

        /**
         * Sets the OCI authentication type.
         * One of: {@code oci_config}, {@code security_token},
         * {@code instance_principal}, {@code resource_principal}.
         */
        public Builder authType(String authType) {
            this.authType = authType;
            return this;
        }

        /**
         * Sets the OCI config profile name. Used with {@code oci_config} and
         * {@code security_token} auth types. Defaults to {@code "DEFAULT"}.
         */
        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        /**
         * Sets a pre-built OCI authentication provider.
         * When set, {@code authType} and {@code profile} are ignored.
         */
        public Builder authProvider(BasicAuthenticationDetailsProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        /**
         * Sets the OCI compartment OCID. Required for OCI Generative AI endpoints.
         */
        public Builder compartmentId(String compartmentId) {
            this.compartmentId = compartmentId;
            return this;
        }

        /**
         * Sets the OCI region code (e.g., {@code "us-chicago-1"}).
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the OCI service endpoint (without API path).
         * The Anthropic API path is appended automatically.
         */
        public Builder serviceEndpoint(String serviceEndpoint) {
            this.serviceEndpoint = serviceEndpoint;
            return this;
        }

        /**
         * Sets the fully qualified base URL. Used as-is without modification.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** Sets the request timeout. Defaults to 2 minutes. */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Enables debug logging of request/response bodies. */
        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) {
            this.logRequestsAndResponses = logRequestsAndResponses;
            return this;
        }

        /**
         * Builds the OCI-authenticated Anthropic client.
         *
         * @return a configured {@link AnthropicClient}
         * @throws IllegalArgumentException if required parameters are missing
         */
        public AnthropicClient build() {
            BasicAuthenticationDetailsProvider resolvedAuth = resolveAuthProvider();

            String resolvedBaseUrl = OciEndpointResolver.resolveAnthropicBaseUrl(
                    region, serviceEndpoint, baseUrl);

            if (resolvedBaseUrl.contains("generativeai") && (compartmentId == null || compartmentId.isBlank())) {
                throw new IllegalArgumentException(
                        "compartmentId is required to access the OCI Generative AI Service.");
            }

            // Create OCI-signed OkHttpClient from core
            okhttp3.OkHttpClient signedOkHttpClient = OciHttpClientFactory.create(
                    resolvedAuth, compartmentId, null, timeout, logRequestsAndResponses);

            // Create a signing HTTP client that implements the Anthropic SDK's HttpClient interface.
            // This strips X-Api-Key headers and delegates to the OCI-signed OkHttpClient.
            OciSigningHttpClient signingHttpClient = new OciSigningHttpClient(signedOkHttpClient);

            // Build ClientOptions with our signing HTTP client and base URL.
            // No API key is needed — OCI signing replaces Anthropic API key authentication.
            ClientOptions clientOptions = ClientOptions.builder()
                    .httpClient(signingHttpClient)
                    .baseUrl(resolvedBaseUrl)
                    .build();

            return new AnthropicClientImpl(clientOptions);
        }

        private BasicAuthenticationDetailsProvider resolveAuthProvider() {
            if (authProvider != null) {
                return authProvider;
            }
            if (authType == null || authType.isBlank()) {
                throw new IllegalArgumentException(
                        "Either authType or authProvider must be provided.");
            }
            return OciAuthProviderFactory.create(authType, profile);
        }
    }
}
