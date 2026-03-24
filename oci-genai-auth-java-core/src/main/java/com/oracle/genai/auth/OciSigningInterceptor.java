/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.http.signing.DefaultRequestSigner;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.SigningStrategy;
import com.oracle.bmc.http.client.io.DuplicatableInputStream;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OkHttp {@link Interceptor} that applies OCI request signing to every outgoing request.
 *
 * <p>This interceptor computes the OCI signature (RSA-SHA256 with timestamp, nonce,
 * and body digest) and injects the {@code Authorization}, {@code date},
 * {@code (request-target)}, {@code host}, and content-related headers required by
 * the OCI API gateway.
 *
 * <p>Thread-safe: this interceptor can be shared across multiple OkHttp clients.
 * Token refresh for {@code security_token} and principal-based auth is handled
 * transparently by the underlying {@link BasicAuthenticationDetailsProvider}.
 */
public class OciSigningInterceptor implements Interceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OciSigningInterceptor.class);

    private final RequestSigner requestSigner;

    /**
     * Creates a signing interceptor using the given OCI auth provider.
     *
     * @param authProvider the OCI authentication details provider (must not be null)
     */
    public OciSigningInterceptor(BasicAuthenticationDetailsProvider authProvider) {
        Objects.requireNonNull(authProvider, "authProvider must not be null");
        this.requestSigner = DefaultRequestSigner.createRequestSigner(
                authProvider, SigningStrategy.STANDARD);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        URI uri = originalRequest.url().uri();
        String method = originalRequest.method();

        // Build the headers map that OCI signing expects.
        // Normalize content-type to strip "; charset=utf-8" that OkHttp appends
        // to string bodies — some OCI endpoints strip it before signature verification,
        // causing SIGNATURE_NOT_VALID errors.
        Map<String, List<String>> existingHeaders = new HashMap<>();
        for (String name : originalRequest.headers().names()) {
            List<String> values = originalRequest.headers(name);
            if ("content-type".equalsIgnoreCase(name)) {
                values = values.stream()
                        .map(v -> v.replaceAll(";\\s*charset=utf-8", "").trim())
                        .toList();
            }
            existingHeaders.put(name, values);
        }

        // Read the request body for signing (OCI signs the body digest)
        byte[] bodyBytes = null;
        if (originalRequest.body() != null) {
            Buffer buffer = new Buffer();
            originalRequest.body().writeTo(buffer);
            bodyBytes = buffer.readByteArray();
        }

        // Compute OCI signature headers
        Map<String, String> signedHeaders;
        if (bodyBytes != null && bodyBytes.length > 0) {
            signedHeaders = requestSigner.signRequest(
                    uri, method, existingHeaders,
                    new DuplicatableByteArrayInputStream(bodyBytes));
        } else {
            signedHeaders = requestSigner.signRequest(
                    uri, method, existingHeaders, null);
        }

        // Build a new request with all signed headers applied
        Request.Builder signedRequestBuilder = originalRequest.newBuilder();
        for (Map.Entry<String, String> entry : signedHeaders.entrySet()) {
            signedRequestBuilder.header(entry.getKey(), entry.getValue());
        }

        // Re-attach the body (it was consumed during signing).
        // Use byte[] to prevent OkHttp from re-appending "; charset=utf-8".
        if (bodyBytes != null) {
            MediaType contentType = originalRequest.body() != null
                    ? originalRequest.body().contentType()
                    : MediaType.parse("application/json");
            // Strip charset to keep content-type consistent with what was signed
            if (contentType != null && contentType.charset() != null) {
                contentType = MediaType.parse(contentType.type() + "/" + contentType.subtype());
            }
            signedRequestBuilder.method(method, RequestBody.create(bodyBytes, contentType));
        }

        Request signedRequest = signedRequestBuilder.build();
        LOG.debug("OCI-signed request: {} {}", method, uri);

        return chain.proceed(signedRequest);
    }

    /**
     * A {@link ByteArrayInputStream} that implements {@link DuplicatableInputStream},
     * required by OCI SDK 3.x {@code RequestSignerImpl} for body signing.
     */
    private static class DuplicatableByteArrayInputStream
            extends ByteArrayInputStream implements DuplicatableInputStream {

        private final byte[] data;

        DuplicatableByteArrayInputStream(byte[] data) {
            super(data);
            this.data = data;
        }

        @Override
        public InputStream duplicate() {
            return new DuplicatableByteArrayInputStream(data);
        }
    }
}
