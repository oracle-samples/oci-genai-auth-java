/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
        ClientOptions clientOptions = ClientOptions.builder()
                .httpClient(OciOpenAIHttpClient.of(ociHttpClient, BASE_URL))
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

}
