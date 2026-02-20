/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.AnthropicClientAsyncImpl;
import com.anthropic.core.ClientOptions;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.genai.core.OciHttpClientFactory;
import com.oracle.genai.core.auth.OciAuthProviderFactory;
import com.oracle.genai.core.endpoint.OciEndpointResolver;

import java.time.Duration;

/**
 * Async OCI-authenticated Anthropic client builder.
 *
 * <p>Creates an {@link AnthropicClientAsync} that routes requests through OCI
 * Generative AI endpoints with OCI IAM request signing.
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * AnthropicClientAsync client = AsyncOciAnthropic.builder()
 *         .compartmentId("<COMPARTMENT_OCID>")
 *         .authType("security_token")
 *         .region("us-chicago-1")
 *         .build();
 *
 * client.messages().create(MessageCreateParams.builder()
 *         .model("anthropic.claude-3-sonnet")
 *         .addUserMessage("Hello from OCI!")
 *         .maxTokens(1024)
 *         .build())
 *     .thenAccept(message -> System.out.println(message.content()));
 * }</pre>
 */
public final class AsyncOciAnthropic {

    private AsyncOciAnthropic() {
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

        public Builder authType(String authType) { this.authType = authType; return this; }
        public Builder profile(String profile) { this.profile = profile; return this; }
        public Builder authProvider(BasicAuthenticationDetailsProvider authProvider) { this.authProvider = authProvider; return this; }
        public Builder compartmentId(String compartmentId) { this.compartmentId = compartmentId; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder serviceEndpoint(String serviceEndpoint) { this.serviceEndpoint = serviceEndpoint; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder logRequestsAndResponses(boolean logRequestsAndResponses) { this.logRequestsAndResponses = logRequestsAndResponses; return this; }

        public AnthropicClientAsync build() {
            BasicAuthenticationDetailsProvider resolvedAuth = resolveAuthProvider();

            String resolvedBaseUrl = OciEndpointResolver.resolveAnthropicBaseUrl(
                    region, serviceEndpoint, baseUrl);

            if (resolvedBaseUrl.contains("generativeai") && (compartmentId == null || compartmentId.isBlank())) {
                throw new IllegalArgumentException(
                        "compartmentId is required to access the OCI Generative AI Service.");
            }

            okhttp3.OkHttpClient signedOkHttpClient = OciHttpClientFactory.create(
                    resolvedAuth, compartmentId, null, timeout, logRequestsAndResponses);

            OciSigningHttpClient signingHttpClient = new OciSigningHttpClient(signedOkHttpClient);

            ClientOptions clientOptions = ClientOptions.builder()
                    .httpClient(signingHttpClient)
                    .baseUrl(resolvedBaseUrl)
                    .build();

            return new AnthropicClientAsyncImpl(clientOptions);
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
