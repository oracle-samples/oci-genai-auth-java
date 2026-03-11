/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.core.RequestOptions;
import com.openai.core.http.HttpClient;
import com.openai.core.http.HttpRequest;
import com.openai.core.http.HttpRequestBody;
import com.openai.core.http.HttpResponse;
import com.openai.core.http.Headers;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import okhttp3.*;
import okio.BufferedSink;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: OCI auth library + OpenAI Responses API against PPE endpoint.
 *
 * <p>Tests the Responses API (newer replacement for Chat Completions) which supports
 * compaction for long-running conversations.
 *
 * <p>To run:
 * <pre>
 * oci session authenticate
 * mvn -pl oci-genai-auth-java-core test -Dtest=OpenAIResponsesIntegrationTest
 * </pre>
 */
class OpenAIResponsesIntegrationTest {

    private static final String COMPARTMENT_ID =
            System.getenv().getOrDefault("OCI_COMPARTMENT_ID", "");

    private static final String BASE_URL =
            System.getenv().getOrDefault("OCI_GENAI_ENDPOINT",
                    "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com")
            + "/20231130/actions/v1";

    @Test
    @Disabled("Requires live OCI session — run: oci session authenticate")
    void responses_api_via_oci_auth_library() {
        // 1. Build OCI-signed OkHttpClient
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // 2. Wrap in OpenAI HttpClient adapter and build client
        HttpClient signingHttpClient = new OpenAIOkHttpAdapter(ociHttpClient, BASE_URL);

        ClientOptions clientOptions = ClientOptions.builder()
                .httpClient(signingHttpClient)
                .baseUrl(BASE_URL)
                .apiKey("OCI_AUTH")
                .build();

        OpenAIClient client = new OpenAIClientImpl(clientOptions);

        try {
            // 3. Send a Responses API request
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model("xai.grok-3")
                    .input("What is 2 + 2? Answer in one word.")
                    .build();

            Response response = client.responses().create(params);

            // 4. Verify response
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.output(), "Output should not be null");
            assertFalse(response.output().isEmpty(), "Output should not be empty");

            // 5. Extract text from output
            for (ResponseOutputItem item : response.output()) {
                item.message().ifPresent(message -> {
                    for (var content : message.content()) {
                        content.outputText().ifPresent(text -> {
                            System.out.println("Responses API output: " + text.text());
                            assertFalse(text.text().isBlank(), "Response text should not be blank");
                        });
                    }
                });
            }
        } finally {
            client.close();
        }
    }

    /**
     * Minimal adapter: bridges OpenAI SDK's HttpClient to OCI-signed OkHttpClient.
     * The OpenAI SDK uses pathSegments instead of full URLs, so we need baseUrl.
     */
    private static class OpenAIOkHttpAdapter implements HttpClient {

        private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
        private final OkHttpClient okHttpClient;
        private final HttpUrl baseUrl;

        OpenAIOkHttpAdapter(OkHttpClient okHttpClient, String baseUrl) {
            this.okHttpClient = okHttpClient;
            this.baseUrl = HttpUrl.parse(baseUrl);
        }

        @Override
        public HttpResponse execute(HttpRequest request, RequestOptions requestOptions) {
            Request okRequest = toOkHttpRequest(request);
            try {
                okhttp3.Response okResponse = okHttpClient.newCall(okRequest).execute();
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
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) {
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
                if (parsedUrl == null) throw new IllegalArgumentException("Invalid URL: " + url);
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

            var queryParams = request.queryParams();
            for (String key : queryParams.keys()) {
                for (String value : queryParams.values(key)) {
                    urlBuilder.addQueryParameter(key, value);
                }
            }

            okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
            var headers = request.headers();
            for (String name : headers.names()) {
                for (String value : headers.values(name)) {
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
            private final okhttp3.Response response;
            private final Headers headers;

            OkHttpResponseAdapter(okhttp3.Response response) {
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
}
