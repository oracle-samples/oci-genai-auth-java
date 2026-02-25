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
 * ═══════════════════════════════════════════════════════════════════
 *  Phase 2 — Live Demo: Unified SDK PoC
 *  OpenAI-compatible via OCI GenAI
 * ═══════════════════════════════════════════════════════════════════
 *
 * BEFORE (what a developer does today WITHOUT this SDK):
 *
 *   // Same 50-line boilerplate as Anthropic:
 *   //   - manual OCI auth provider setup
 *   //   - manual request signing (RSA-SHA256)
 *   //   - know the endpoint: .../20231130/actions/v1/chat/completions
 *   //   - manual compartment header injection
 *   //   - manual JSON payload construction
 *   //   - manual response parsing
 *   //
 *   // AND the OpenAI endpoint path is DIFFERENT from Anthropic!
 *   //   Anthropic: /anthropic/v1/messages
 *   //   OpenAI:    /20231130/actions/v1/chat/completions
 *   //
 *   // Developers have to know both. The SDK handles this automatically.
 *
 *
 * AFTER (with the Unified SDK):
 *   Almost identical builder pattern — that's the point.
 *   See the test methods below.
 * ═══════════════════════════════════════════════════════════════════
 *
 * To run: remove @Disabled and execute:
 *   mvn -pl oci-genai-openai test -Dtest=LiveDemoTest
 */
class LiveDemoTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    private static final String BASE_URL =
            "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1";

    /**
     * Demo 2 — OpenAI-compatible via OCI (session token auth, for local dev)
     *
     * This is the demo to run from your laptop.
     * Requires: oci session authenticate (OCI CLI)
     */
    @Test
    // @Disabled("Remove to run live demo")
    void demo_OpenAI_SessionToken() {
        // ── AFTER: almost identical builder — that's the point ──
        OpenAIClient client = OciOpenAI.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .baseUrl(BASE_URL)
                .build();

        try {
            ChatCompletion completion = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model("xai.grok-3")
                            .addUserMessage("Explain OCI in one sentence.")
                            .build());

            System.out.println("\n══════════════════════════════════════");
            System.out.println("  OpenAI via OCI GenAI — Response");
            System.out.println("══════════════════════════════════════");
            completion.choices().forEach(choice ->
                    choice.message().content().ifPresent(System.out::println));
            System.out.println("══════════════════════════════════════\n");
        } finally {
            client.close();
        }
    }

    /**
     * Demo 2b — OpenAI-compatible via OCI (instance principal, for OCI Compute)
     *
     * This is the demo to run from an OCI VM/container.
     */
    @Test
    @Disabled("Remove to run live demo — requires OCI Compute instance")
    void demo_OpenAI_InstancePrincipal() {
        // ── AFTER: same builder, just swap authType ──
        OpenAIClient client = OciOpenAI.builder()
                .authType("instance_principal")
                .region("us-chicago-1")
                .compartmentId(COMPARTMENT_ID)
                .build();

        try {
            ChatCompletion completion = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model("xai.grok-3")
                            .addUserMessage("Explain OCI in one sentence.")
                            .build());

            System.out.println("\n══════════════════════════════════════");
            System.out.println("  OpenAI (Instance Principal)");
            System.out.println("══════════════════════════════════════");
            completion.choices().forEach(choice ->
                    choice.message().content().ifPresent(System.out::println));
            System.out.println("══════════════════════════════════════\n");
        } finally {
            client.close();
        }
    }
}
