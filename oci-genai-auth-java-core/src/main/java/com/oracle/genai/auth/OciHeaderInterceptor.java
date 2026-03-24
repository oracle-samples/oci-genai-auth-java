/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * OkHttp {@link Interceptor} that injects OCI-specific headers into every request.
 *
 * <p>This handles the {@code CompartmentId} and {@code opc-compartment-id} headers
 * required by the OCI Generative AI service, as well as any additional custom headers.
 */
public class OciHeaderInterceptor implements Interceptor {

    public static final String COMPARTMENT_ID_HEADER = "CompartmentId";
    public static final String OPC_COMPARTMENT_ID_HEADER = "opc-compartment-id";

    private final Map<String, String> headers;

    /**
     * Creates a header interceptor with compartment ID and optional extra headers.
     *
     * @param compartmentId     the OCI compartment OCID (may be null if not required)
     * @param additionalHeaders extra headers to inject on every request
     */
    public OciHeaderInterceptor(String compartmentId, Map<String, String> additionalHeaders) {
        var headerMap = new java.util.LinkedHashMap<String, String>();

        if (compartmentId != null && !compartmentId.isBlank()) {
            headerMap.put(COMPARTMENT_ID_HEADER, compartmentId);
            headerMap.put(OPC_COMPARTMENT_ID_HEADER, compartmentId);
        }

        if (additionalHeaders != null) {
            headerMap.putAll(additionalHeaders);
        }

        this.headers = Collections.unmodifiableMap(headerMap);
    }

    /**
     * Creates a header interceptor with compartment ID only.
     */
    public OciHeaderInterceptor(String compartmentId) {
        this(compartmentId, null);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        return chain.proceed(builder.build());
    }
}
