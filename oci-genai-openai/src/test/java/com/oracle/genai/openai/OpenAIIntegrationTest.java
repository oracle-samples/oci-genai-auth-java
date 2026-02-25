/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.openai;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for OciOpenAI against live endpoints.
 *
 * <p>These tests are disabled by default. To run them, remove the {@code @Disabled}
 * annotations and ensure you have:
 * <ul>
 *   <li>A valid OCI config at {@code ~/.oci/config} with a session token (for PPE test)</li>
 *   <li>A valid API key (for Dev test)</li>
 * </ul>
 */
class OpenAIIntegrationTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    /**
     * Test against PPE endpoint with OCI session token auth.
     */
    @Test
    @Disabled("Requires live OCI credentials and PPE endpoint access")
    void testPpeEndpointWithOciAuth() {
        OpenAIClient client = OciOpenAI.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .baseUrl("https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1")
                .build();

        try {
            ChatCompletion completion = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model("xai.grok-3")
                            .addUserMessage("Write a one-sentence bedtime story about a unicorn.")
                            .build());

            System.out.println("PPE Response: " + completion.choices());
            assert !completion.choices().isEmpty() : "Response should not be empty";
        } finally {
            client.close();
        }
    }

    /**
     * Test against Dev endpoint with API key auth.
     */
    @Test
    @Disabled("Requires valid API key and Dev endpoint access")
    void testDevEndpointWithApiKey() {
        OpenAIClient client = OciOpenAI.builder()
                .apiKey("YOUR_API_KEY_HERE")
                .baseUrl("https://dev.inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1")
                .build();

        try {
            ChatCompletion completion = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model("xai.grok-3")
                            .addUserMessage("Write a one-sentence bedtime story about a unicorn.")
                            .build());

            System.out.println("Dev Response: " + completion.choices());
            assert !completion.choices().isEmpty() : "Response should not be empty";
        } finally {
            client.close();
        }
    }
}
