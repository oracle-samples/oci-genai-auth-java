/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: OCI auth library + OpenAI Java SDK against PPE endpoint.
 *
 * <p>To run:
 * <pre>
 * oci session authenticate
 * mvn -pl oci-genai-auth-java-core test -Dtest=OpenAIIntegrationTest
 * </pre>
 */
class OpenAIIntegrationTest {

    private static final String COMPARTMENT_ID =
            System.getenv().getOrDefault("OCI_COMPARTMENT_ID", "");

    private static final String BASE_URL =
            System.getenv().getOrDefault("OCI_GENAI_ENDPOINT",
                    "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com")
            + "/20231130/actions/v1";

    @Test
    @Disabled("Requires live OCI session — run: oci session authenticate")
    void openai_via_oci_auth_library() {
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
            // 3. Send a request
            ChatCompletion completion = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model("xai.grok-3")
                            .addUserMessage("What is 2 + 2? Answer in one word.")
                            .build());

            // 4. Verify response
            assertNotNull(completion, "Response should not be null");
            assertFalse(completion.choices().isEmpty(), "Should have choices");

            completion.choices().forEach(choice ->
                    choice.message().content().ifPresent(content -> {
                        System.out.println("OpenAI response: " + content);
                        assertFalse(content.isBlank(), "Response content should not be blank");
                    }));
        } finally {
            client.close();
        }
    }

}
