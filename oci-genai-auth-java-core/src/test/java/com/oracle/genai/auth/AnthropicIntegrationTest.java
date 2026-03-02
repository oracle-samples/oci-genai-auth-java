/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientImpl;
import com.anthropic.core.ClientOptions;
import com.anthropic.core.RequestOptions;
import com.anthropic.core.http.HttpClient;
import com.anthropic.core.http.HttpRequest;
import com.anthropic.core.http.HttpRequestBody;
import com.anthropic.core.http.HttpResponse;
import com.anthropic.core.http.Headers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import okhttp3.*;
import okio.BufferedSink;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: OCI auth library + Anthropic Java SDK against PPE endpoint.
 *
 * <p>Validates that oci-genai-auth-java-core produces a correctly signed
 * OkHttpClient that works with the Anthropic SDK.
 *
 * <p>To run:
 * <pre>
 * oci session authenticate
 * mvn -pl oci-genai-auth-java-core test -Dtest=AnthropicIntegrationTest
 * </pre>
 */
class AnthropicIntegrationTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    private static final String BASE_URL =
            "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic";

    @Test
    @Disabled("Requires live OCI session — run: oci session authenticate")
    void anthropic_via_oci_auth_library() {
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        HttpClient signingHttpClient = new AnthropicOkHttpAdapter(ociHttpClient);

        ClientOptions clientOptions = ClientOptions.builder()
                .httpClient(signingHttpClient)
                .baseUrl(BASE_URL)
                .putHeader("anthropic-version", "2023-06-01")
                .build();

        AnthropicClient client = new AnthropicClientImpl(clientOptions);

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model("anthropic.claude-haiku-4-5")
                    .maxTokens(256)
                    .addUserMessage("What is 2 + 2? Answer in one word.")
                    .build());

            assertNotNull(message, "Response should not be null");
            assertFalse(message.content().isEmpty(), "Response should have content");

            for (ContentBlock block : message.content()) {
                block.text().ifPresent(textBlock -> {
                    System.out.println("Anthropic response: " + textBlock.text());
                    assertFalse(textBlock.text().isBlank(), "Response text should not be blank");
                });
            }
        } finally {
            client.close();
        }
    }

    /**
     * Adapter: bridges Anthropic SDK's HttpClient to OCI-signed OkHttpClient.
     */
    private static class AnthropicOkHttpAdapter implements HttpClient {

        private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
        private final OkHttpClient okHttpClient;

        AnthropicOkHttpAdapter(OkHttpClient okHttpClient) {
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
            HttpUrl parsedUrl = HttpUrl.parse(request.url());
            if (parsedUrl == null) {
                throw new IllegalArgumentException("Invalid URL: " + request.url());
            }

            HttpUrl.Builder urlBuilder = parsedUrl.newBuilder();
            var queryParams = request.queryParams();
            for (String key : queryParams.keys()) {
                for (String value : queryParams.values(key)) {
                    urlBuilder.addQueryParameter(key, value);
                }
            }

            okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
            var headers = request.headers();
            for (String name : headers.names()) {
                if ("x-api-key".equalsIgnoreCase(name) || "authorization".equalsIgnoreCase(name)) {
                    continue;
                }
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
}
