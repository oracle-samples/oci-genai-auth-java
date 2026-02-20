/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for OciAnthropic against live endpoints.
 *
 * <p>These tests are disabled by default. To run them, remove the {@code @Disabled}
 * annotations and ensure you have:
 * <ul>
 *   <li>A valid OCI config at {@code ~/.oci/config} with a session token (for PPE test)</li>
 *   <li>A valid API key (for Dev test)</li>
 * </ul>
 */
class AnthropicIntegrationTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    /**
     * Test against PPE endpoint with OCI session token auth.
     *
     * <p>Equivalent Python:
     * <pre>
     * client = OciAnthropic(
     *     auth=OciSessionAuth(profile_name="DEFAULT"),
     *     base_url="https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic",
     *     compartment_id="ocid1.tenancy.oc1..aaaaaaaau...",
     * )
     * </pre>
     */
    @Test
    @Disabled("Requires live OCI credentials and PPE endpoint access")
    void testPpeEndpointWithOciAuth() {
        AnthropicClient client = OciAnthropic.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .baseUrl("https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic")
                .build();

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model("anthropic.claude-haiku-4-5")
                    .maxTokens(256)
                    .addUserMessage("Write a one-sentence bedtime story about a unicorn.")
                    .build());

            System.out.println("PPE Response: " + message.content());
            assert !message.content().isEmpty() : "Response should not be empty";
        } finally {
            client.close();
        }
    }

    /**
     * Test against Dev endpoint with API key auth.
     *
     * <p>Equivalent Python:
     * <pre>
     * client = Anthropic(
     *     api_key="sk-...",
     *     base_url="https://dev.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic",
     * )
     * </pre>
     */
    @Test
    @Disabled("Requires valid API key and Dev endpoint access")
    void testDevEndpointWithApiKey() {
        AnthropicClient client = OciAnthropic.builder()
                .apiKey("YOUR_API_KEY_HERE")
                .baseUrl("https://dev.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic")
                .build();

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model("anthropic.claude-haiku-4-5")
                    .maxTokens(256)
                    .addUserMessage("Write a one-sentence bedtime story about a unicorn.")
                    .build());

            System.out.println("Dev Response: " + message.content());
            assert !message.content().isEmpty() : "Response should not be empty";
        } finally {
            client.close();
        }
    }
}
