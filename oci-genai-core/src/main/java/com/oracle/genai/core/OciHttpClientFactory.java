/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.core;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.genai.core.interceptor.OciHeaderInterceptor;
import com.oracle.genai.core.interceptor.OciSigningInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for creating OkHttp clients pre-configured with OCI signing and header interceptors.
 *
 * <p>Provider modules (OpenAI, Anthropic) use this factory to obtain an OkHttpClient
 * that transparently handles OCI IAM authentication on every request.
 */
public final class OciHttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OciHttpClientFactory.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    private OciHttpClientFactory() {
        // utility class
    }

    /**
     * Creates an OkHttpClient with OCI signing and header injection.
     *
     * @param authProvider      the OCI authentication provider
     * @param compartmentId     the OCI compartment OCID (may be null)
     * @param additionalHeaders extra headers to inject on every request
     * @param timeout           request timeout (null for default 2 minutes)
     * @param logRequests       whether to log request/response bodies
     * @return a configured OkHttpClient
     */
    public static OkHttpClient create(
            BasicAuthenticationDetailsProvider authProvider,
            String compartmentId,
            Map<String, String> additionalHeaders,
            Duration timeout,
            boolean logRequests) {

        Objects.requireNonNull(authProvider, "authProvider must not be null");

        Duration resolvedTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new OciHeaderInterceptor(compartmentId, additionalHeaders))
                .addInterceptor(new OciSigningInterceptor(authProvider))
                .connectTimeout(resolvedTimeout)
                .readTimeout(resolvedTimeout)
                .writeTimeout(resolvedTimeout);

        if (logRequests) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    message -> LOG.debug("{}", message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        LOG.debug("Created OCI-signed OkHttpClient [compartmentId={}, timeout={}]",
                compartmentId, resolvedTimeout);

        return builder.build();
    }

    /**
     * Creates an OkHttpClient with OCI signing, compartment header, and default settings.
     */
    public static OkHttpClient create(
            BasicAuthenticationDetailsProvider authProvider,
            String compartmentId) {
        return create(authProvider, compartmentId, null, null, false);
    }
}
