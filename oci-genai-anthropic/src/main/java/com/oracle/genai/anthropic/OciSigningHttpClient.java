/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import com.anthropic.core.RequestOptions;
import com.anthropic.core.http.HttpClient;
import com.anthropic.core.http.HttpRequest;
import com.anthropic.core.http.HttpRequestBody;
import com.anthropic.core.http.HttpResponse;
import com.anthropic.core.http.Headers;
import okhttp3.*;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of the Anthropic SDK's {@link HttpClient} interface backed by
 * an OCI-signed {@link okhttp3.OkHttpClient}.
 *
 * <p>This bridges the Anthropic SDK's HTTP abstraction with OCI request signing.
 * The underlying OkHttpClient has {@code OciSigningInterceptor} and
 * {@code OciHeaderInterceptor} already configured, so every request is
 * automatically signed with OCI IAM credentials.
 */
class OciSigningHttpClient implements HttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(OciSigningHttpClient.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final okhttp3.OkHttpClient okHttpClient;

    OciSigningHttpClient(okhttp3.OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public HttpResponse execute(HttpRequest request, RequestOptions requestOptions) {
        Request okRequest = toOkHttpRequest(request);
        try {
            Response okResponse = okHttpClient.newCall(okRequest).execute();
            return new OkHttpResponseAdapter(okResponse);
        } catch (IOException e) {
            throw new RuntimeException("OCI request failed: " + request.url(), e);
        }
    }

    @Override
    public CompletableFuture<HttpResponse> executeAsync(
            HttpRequest request, RequestOptions requestOptions) {
        Request okRequest = toOkHttpRequest(request);
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();

        okHttpClient.newCall(okRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(
                        new RuntimeException("OCI async request failed: " + request.url(), e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                future.complete(new OkHttpResponseAdapter(response));
            }
        });

        return future;
    }

    @Override
    public void close() {
        okHttpClient.dispatcher().executorService().shutdown();
        okHttpClient.connectionPool().evictAll();
    }

    private Request toOkHttpRequest(HttpRequest request) {
        String url = request.url();
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        HttpUrl.Builder urlBuilder = parsedUrl.newBuilder();

        // Add query params
        var queryParams = request.queryParams();
        for (String key : queryParams.keys()) {
            for (String value : queryParams.values(key)) {
                urlBuilder.addQueryParameter(key, value);
            }
        }

        // Build headers (strip X-Api-Key since OCI signing replaces it)
        okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
        var headers = request.headers();
        for (String name : headers.names()) {
            // Omit API key / auth headers — OCI signing handles authentication
            if ("x-api-key".equalsIgnoreCase(name) || "authorization".equalsIgnoreCase(name)) {
                continue;
            }
            for (String value : headers.values(name)) {
                headersBuilder.add(name, value);
            }
        }

        // Build request body
        RequestBody body = null;
        HttpRequestBody requestBody = request.body();
        if (requestBody != null) {
            body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    String ct = requestBody.contentType();
                    return ct != null ? MediaType.parse(ct) : JSON_MEDIA_TYPE;
                }

                @Override
                public long contentLength() {
                    return requestBody.contentLength();
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try (OutputStream os = sink.outputStream()) {
                        requestBody.writeTo(os);
                    }
                }
            };
        }

        String method = request.method().name();
        return new Request.Builder()
                .url(urlBuilder.build())
                .headers(headersBuilder.build())
                .method(method, body)
                .build();
    }

    /**
     * Adapts an OkHttp {@link Response} to the Anthropic SDK's {@link HttpResponse} interface.
     */
    private static class OkHttpResponseAdapter implements HttpResponse {

        private final Response response;
        private final Headers headers;

        OkHttpResponseAdapter(Response response) {
            this.response = response;
            Headers.Builder builder = Headers.builder();
            for (String name : response.headers().names()) {
                for (String value : response.headers(name)) {
                    builder.put(name, value);
                }
            }
            this.headers = builder.build();
        }

        @Override
        public int statusCode() {
            return response.code();
        }

        @Override
        public Headers headers() {
            return headers;
        }

        @Override
        public InputStream body() {
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.byteStream() : InputStream.nullInputStream();
        }

        @Override
        public void close() {
            response.close();
        }
    }
}
