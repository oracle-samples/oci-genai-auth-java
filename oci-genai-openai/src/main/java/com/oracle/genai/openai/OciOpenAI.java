/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.ClientOptions;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.genai.core.OciHttpClientFactory;
import com.oracle.genai.core.auth.OciAuthProviderFactory;
import com.oracle.genai.core.endpoint.OciEndpointResolver;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OCI-authenticated OpenAI client builder.
 *
 * <p>Creates an {@link OpenAIClient} that routes requests through OCI Generative AI
 * endpoints with OCI IAM request signing. The underlying OpenAI Java SDK is used
 * for all API operations — users get the full OpenAI API surface (chat completions,
 * responses, embeddings) with OCI auth handled transparently.
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * OpenAIClient client = OciOpenAI.builder()
 *         .compartmentId("<COMPARTMENT_OCID>")
 *         .authType("security_token")
 *         .profile("DEFAULT")
 *         .region("us-chicago-1")
 *         .build();
 *
 * Response response = client.responses().create(ResponseCreateParams.builder()
 *         .model("openai.gpt-4o")
 *         .store(false)
 *         .input("Write a short poem about cloud computing.")
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
public final class OciOpenAI {

    /** Header key for conversation store OCID. */
    private static final String CONVERSATION_STORE_ID_HEADER = "opc-conversation-store-id";

    private OciOpenAI() {
        // static builder entry point only
    }

    /**
     * Returns a new builder for configuring an OCI-authenticated OpenAI client.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String authType;
        private String profile;
        private String apiKey;
        private BasicAuthenticationDetailsProvider authProvider;
        private String compartmentId;
        private String conversationStoreId;
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
         * {@code instance_principal}, {@code resource_principal},
         * {@code api_key}.
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
         * Sets the API key for direct authentication (no OCI signing).
         * When set, requests are authenticated with this key via the
         * {@code Authorization: Bearer} header, bypassing OCI IAM signing entirely.
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
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
         * Sets the optional Conversation Store OCID, attached to every request
         * as the {@code opc-conversation-store-id} header.
         */
        public Builder conversationStoreId(String conversationStoreId) {
            this.conversationStoreId = conversationStoreId;
            return this;
        }

        /**
         * Sets the OCI region code (e.g., {@code "us-chicago-1"}).
         * Auto-derives the service endpoint URL.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the OCI service endpoint (without API path).
         * {@code /openai/v1} is appended automatically.
         * Takes precedence over {@code region}.
         */
        public Builder serviceEndpoint(String serviceEndpoint) {
            this.serviceEndpoint = serviceEndpoint;
            return this;
        }

        /**
         * Sets the fully qualified base URL (including API path).
         * Used as-is without modification.
         * Takes precedence over {@code serviceEndpoint} and {@code region}.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the request timeout. Defaults to 2 minutes.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Enables debug logging of request/response bodies.
         */
        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) {
            this.logRequestsAndResponses = logRequestsAndResponses;
            return this;
        }

        /**
         * Builds the OpenAI client.
         *
         * <p>When {@code apiKey} is set (or {@code authType} is {@code "api_key"}),
         * creates a native OpenAI SDK client with direct API key auth.
         * Otherwise, creates an OCI-authenticated client with IAM request signing.
         *
         * @return a configured {@link OpenAIClient}
         * @throws IllegalArgumentException if required parameters are missing
         */
        public OpenAIClient build() {
            if (isApiKeyMode()) {
                return buildApiKeyClient();
            }
            return buildOciSignedClient();
        }

        private boolean isApiKeyMode() {
            return (apiKey != null && !apiKey.isBlank())
                    || "api_key".equals(authType);
        }

        private OpenAIClient buildApiKeyClient() {
            String resolvedApiKey = apiKey;
            if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
                throw new IllegalArgumentException(
                        "apiKey is required when authType is 'api_key'.");
            }

            String resolvedBaseUrl = OciEndpointResolver.resolveOpenAiBaseUrl(
                    region, serviceEndpoint, baseUrl);

            OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                    .apiKey(resolvedApiKey)
                    .baseUrl(resolvedBaseUrl);

            if (timeout != null) {
                builder.timeout(timeout);
            }

            return builder.build();
        }

        private OpenAIClient buildOciSignedClient() {
            BasicAuthenticationDetailsProvider resolvedAuth = resolveAuthProvider();

            String resolvedBaseUrl = OciEndpointResolver.resolveOpenAiBaseUrl(
                    region, serviceEndpoint, baseUrl);

            if (resolvedBaseUrl.contains("generativeai") && (compartmentId == null || compartmentId.isBlank())) {
                throw new IllegalArgumentException(
                        "compartmentId is required to access the OCI Generative AI Service.");
            }

            Map<String, String> additionalHeaders = buildAdditionalHeaders();

            okhttp3.OkHttpClient signedOkHttpClient = OciHttpClientFactory.create(
                    resolvedAuth, compartmentId, additionalHeaders, timeout, logRequestsAndResponses);

            OciSigningHttpClient signingHttpClient = new OciSigningHttpClient(signedOkHttpClient, resolvedBaseUrl);

            ClientOptions clientOptions = ClientOptions.builder()
                    .httpClient(signingHttpClient)
                    .baseUrl(resolvedBaseUrl)
                    .apiKey("OCI_AUTH_NOT_USED")
                    .build();

            return new OpenAIClientImpl(clientOptions);
        }

        private Map<String, String> buildAdditionalHeaders() {
            Map<String, String> headers = new LinkedHashMap<>();
            if (conversationStoreId != null && !conversationStoreId.isBlank()) {
                headers.put(CONVERSATION_STORE_ID_HEADER, conversationStoreId);
            }
            return headers.isEmpty() ? null : headers;
        }

        private BasicAuthenticationDetailsProvider resolveAuthProvider() {
            if (authProvider != null) {
                return authProvider;
            }
            if (authType == null || authType.isBlank()) {
                throw new IllegalArgumentException(
                        "Either authType, authProvider, or apiKey must be provided.");
            }
            return OciAuthProviderFactory.create(authType, profile);
        }
    }
}
