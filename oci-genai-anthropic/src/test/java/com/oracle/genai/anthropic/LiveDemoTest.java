/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Phase 2 — Live Demo: Unified SDK PoC
 *  Anthropic via OCI GenAI
 * ═══════════════════════════════════════════════════════════════════
 *
 * BEFORE (what a developer does today WITHOUT this SDK):
 *
 *   // 1. Manually create an OCI auth provider
 *   ConfigFileAuthenticationDetailsProvider authProvider =
 *       new ConfigFileAuthenticationDetailsProvider("DEFAULT");
 *
 *   // 2. Build a request signer for OCI IAM
 *   RequestSigner signer = DefaultRequestSigner.createRequestSigner(
 *       authProvider, SigningStrategy.STANDARD);
 *
 *   // 3. Know the exact OCI endpoint URL per region
 *   String endpoint = "https://inference.generativeai.us-chicago-1"
 *       + ".oci.oraclecloud.com/anthropic/v1/messages";
 *
 *   // 4. Manually build the JSON payload
 *   String json = "{\"model\":\"anthropic.claude-3-sonnet\","
 *       + "\"max_tokens\":1024,"
 *       + "\"messages\":[{\"role\":\"user\","
 *       + "\"content\":\"Explain OCI in one sentence\"}]}";
 *
 *   // 5. Build the HTTP request with compartment headers
 *   Request request = new Request.Builder()
 *       .url(endpoint)
 *       .addHeader("CompartmentId", compartmentId)
 *       .addHeader("opc-compartment-id", compartmentId)
 *       .addHeader("anthropic-version", "2023-06-01")
 *       .post(RequestBody.create(json, JSON))
 *       .build();
 *
 *   // 6. Sign the request (compute RSA-SHA256 digest, inject Authorization header)
 *   Map<String, String> signedHeaders = signer.signRequest(
 *       request.url().uri(), "POST", existingHeaders, bodyStream);
 *   // ... rebuild request with signed headers ...
 *
 *   // 7. Execute and manually parse the response
 *   Response response = httpClient.newCall(signedRequest).execute();
 *   // ... parse JSON, extract content, handle errors ...
 *
 *   // That's ~50 lines of boilerplate BEFORE you even get to your business logic.
 *
 *
 * AFTER (with the Unified SDK — this is ALL the developer writes):
 *   See the test methods below. ~10 lines total.
 * ═══════════════════════════════════════════════════════════════════
 *
 * To run: remove @Disabled and execute:
 *   mvn -pl oci-genai-anthropic test -Dtest=LiveDemoTest
 */
class LiveDemoTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    private static final String BASE_URL =
            "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/anthropic";

    /**
     * Demo 1 — Anthropic via OCI (session token auth, for local dev)
     *
     * This is the demo to run from your laptop.
     * Requires: oci session authenticate (OCI CLI)
     */
    @Test
    // @Disabled("Remove to run live demo")
    void demo_Anthropic_SessionToken() {
        // ── AFTER: this is ALL the developer writes ──
        AnthropicClient client = OciAnthropic.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .baseUrl(BASE_URL)
                .build();

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model("anthropic.claude-haiku-4-5")
                    .maxTokens(256)
                    .addUserMessage("Explain OCI in one sentence.")
                    .build());

            System.out.println("\n══════════════════════════════════════");
            System.out.println("  Anthropic via OCI GenAI — Response");
            System.out.println("══════════════════════════════════════");
            for (ContentBlock block : message.content()) {
                block.text().ifPresent(textBlock ->
                        System.out.println(textBlock.text()));
            }
            System.out.println("══════════════════════════════════════\n");
        } finally {
            client.close();
        }
    }

    /**
     * Demo 1b — Anthropic via OCI (instance principal, for OCI Compute)
     *
     * This is the demo to run from an OCI VM/container.
     */
    @Test
    @Disabled("Remove to run live demo — requires OCI Compute instance")
    void demo_Anthropic_InstancePrincipal() {
        // ── AFTER: same builder, just swap authType ──
        AnthropicClient client = OciAnthropic.builder()
                .authType("instance_principal")
                .region("us-chicago-1")
                .compartmentId(COMPARTMENT_ID)
                .build();

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model("anthropic.claude-haiku-4-5")
                    .maxTokens(256)
                    .addUserMessage("Explain OCI in one sentence.")
                    .build());

            System.out.println("\n══════════════════════════════════════");
            System.out.println("  Anthropic (Instance Principal)");
            System.out.println("══════════════════════════════════════");
            for (ContentBlock block : message.content()) {
                block.text().ifPresent(textBlock ->
                        System.out.println(textBlock.text()));
            }
            System.out.println("══════════════════════════════════════\n");
        } finally {
            client.close();
        }
    }
}
