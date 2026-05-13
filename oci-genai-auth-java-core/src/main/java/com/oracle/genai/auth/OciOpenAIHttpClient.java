/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.openai.core.RequestOptions;
import com.openai.core.http.HttpClient;
import com.openai.core.http.HttpRequest;
import com.openai.core.http.HttpRequestBody;
import com.openai.core.http.HttpResponse;
import com.openai.core.http.Headers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges the OpenAI Java SDK's {@link HttpClient} interface to an OCI-signed
 * {@link OkHttpClient}, enabling OCI IAM authentication with the OpenAI Java SDK.
 *
 * <p>The OpenAI Java SDK does not expose a way to inject a custom {@link OkHttpClient}
 * directly into its builder. This adapter implements the SDK's {@link HttpClient} interface
 * and delegates every request to an {@link OkHttpClient} that already has OCI signing
 * interceptors attached (see {@link OciOkHttpClientFactory}).
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * String baseUrl = "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1";
 *
 * OciAuthConfig config = OciAuthConfig.builder()
 *         .authType("security_token")
 *         .profile("DEFAULT")
 *         .build();
 * OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);
 *
 * OpenAIClient client = new OpenAIClientImpl(ClientOptions.builder()
 *         .httpClient(OciOpenAIHttpClient.of(ociHttpClient, baseUrl))
 *         .baseUrl(baseUrl)
 *         .apiKey("not-used")
 *         .build());
 * }</pre>
 */
public final class OciOpenAIHttpClient implements HttpClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient okHttpClient;
    private final HttpUrl baseUrl;

    private OciOpenAIHttpClient(OkHttpClient okHttpClient, String baseUrl) {
        this.okHttpClient = Objects.requireNonNull(okHttpClient, "okHttpClient must not be null");
        HttpUrl parsedUrl = HttpUrl.parse(
                Objects.requireNonNull(baseUrl, "baseUrl must not be null"));
        if (parsedUrl == null) {
            throw new IllegalArgumentException("Invalid baseUrl: " + baseUrl);
        }
        this.baseUrl = parsedUrl;
    }

    /**
     * Creates an adapter that routes OpenAI SDK requests through the OCI-signed {@code okHttpClient}.
     *
     * @param okHttpClient an OCI-signed OkHttpClient (e.g., from {@link OciOkHttpClientFactory})
     * @param baseUrl      the fully-qualified base URL including path prefix
     *                     (e.g., {@code https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1})
     * @return the adapter
     */
    public static OciOpenAIHttpClient of(OkHttpClient okHttpClient, String baseUrl) {
        return new OciOpenAIHttpClient(okHttpClient, baseUrl);
    }

    @Override
    public HttpResponse execute(HttpRequest request, RequestOptions requestOptions) {
        Request okRequest = toOkHttpRequest(request);
        try {
            Response response = okHttpClient.newCall(okRequest).execute();
            return new OkHttpResponseAdapter(response);
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
                future.completeExceptionally(e);
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
        HttpUrl.Builder urlBuilder;
        String url = request.url();
        if (url != null && !url.isBlank()) {
            HttpUrl parsedUrl = HttpUrl.parse(url);
            if (parsedUrl == null) {
                throw new IllegalArgumentException("Invalid URL: " + url);
            }
            urlBuilder = parsedUrl.newBuilder();
        } else {
            urlBuilder = baseUrl.newBuilder();
            List<String> pathSegments = request.pathSegments();
            if (pathSegments != null) {
                for (String segment : pathSegments) {
                    urlBuilder.addPathSegment(segment);
                }
            }
        }

        for (String key : request.queryParams().keys()) {
            for (String value : request.queryParams().values(key)) {
                urlBuilder.addQueryParameter(key, value);
            }
        }

        okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
        for (String name : request.headers().names()) {
            for (String value : request.headers().values(name)) {
                headersBuilder.add(name, value);
            }
        }

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

        return new Request.Builder()
                .url(urlBuilder.build())
                .headers(headersBuilder.build())
                .method(request.method().name(), body)
                .build();
    }

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
        public int statusCode() { return response.code(); }

        @Override
        public Headers headers() { return headers; }

        @Override
        public InputStream body() {
            ResponseBody b = response.body();
            return b != null ? b.byteStream() : InputStream.nullInputStream();
        }

        @Override
        public void close() { response.close(); }
    }
}
