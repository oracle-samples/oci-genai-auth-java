/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.openai;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.OpenAIClientAsyncImpl;
import com.openai.core.ClientOptions;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.genai.core.OciHttpClientFactory;
import com.oracle.genai.core.auth.OciAuthProviderFactory;
import com.oracle.genai.core.endpoint.OciEndpointResolver;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Async OCI-authenticated OpenAI client builder.
 *
 * <p>Creates an {@link OpenAIClientAsync} that routes requests through OCI
 * Generative AI endpoints with OCI IAM request signing. Supports async/await
 * patterns using {@link java.util.concurrent.CompletableFuture}.
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * OpenAIClientAsync client = AsyncOciOpenAI.builder()
 *         .compartmentId("<COMPARTMENT_OCID>")
 *         .authType("security_token")
 *         .region("us-chicago-1")
 *         .build();
 *
 * client.responses().create(ResponseCreateParams.builder()
 *         .model("openai.gpt-4o")
 *         .store(false)
 *         .input("Write a short poem about cloud computing.")
 *         .build())
 *     .thenAccept(response -> System.out.println(response.output()));
 * }</pre>
 */
public final class AsyncOciOpenAI {

    private static final String CONVERSATION_STORE_ID_HEADER = "opc-conversation-store-id";

    private AsyncOciOpenAI() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String authType;
        private String profile;
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

        public Builder authType(String authType) { this.authType = authType; return this; }
        public Builder profile(String profile) { this.profile = profile; return this; }
        public Builder authProvider(BasicAuthenticationDetailsProvider authProvider) { this.authProvider = authProvider; return this; }
        public Builder compartmentId(String compartmentId) { this.compartmentId = compartmentId; return this; }
        public Builder conversationStoreId(String conversationStoreId) { this.conversationStoreId = conversationStoreId; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder serviceEndpoint(String serviceEndpoint) { this.serviceEndpoint = serviceEndpoint; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) { this.logRequestsAndResponses = logRequestsAndResponses; return this; }

        public OpenAIClientAsync build() {
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

            OciSigningHttpClient signingHttpClient = new OciSigningHttpClient(signedOkHttpClient);

            ClientOptions clientOptions = ClientOptions.builder()
                    .httpClient(signingHttpClient)
                    .baseUrl(resolvedBaseUrl)
                    .apiKey("OCI_AUTH_NOT_USED")
                    .build();

            return new OpenAIClientAsyncImpl(clientOptions);
        }

        private Map<String, String> buildAdditionalHeaders() {
            Map<String, String> headers = new LinkedHashMap<>();
            if (conversationStoreId != null && !conversationStoreId.isBlank()) {
                headers.put(CONVERSATION_STORE_ID_HEADER, conversationStoreId);
            }
            return headers.isEmpty() ? null : headers;
        }

        private BasicAuthenticationDetailsProvider resolveAuthProvider() {
            if (authProvider != null) return authProvider;
            if (authType == null || authType.isBlank()) {
                throw new IllegalArgumentException("Either authType or authProvider must be provided.");
            }
            return OciAuthProviderFactory.create(authType, profile);
        }
    }
}
