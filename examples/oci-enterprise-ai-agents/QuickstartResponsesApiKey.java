/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Quickstart using Generative AI API Key authentication with the Responses API.
 *
 * <p>No oci-genai-auth package needed for API Key auth — just the official OpenAI SDK.
 *
 * <p>Steps:
 * <ol>
 *   <li>Create a Generative AI Project on OCI Console</li>
 *   <li>Create a Generative AI API Key on OCI Console</li>
 *   <li>Set OCI_GENAI_API_KEY environment variable</li>
 *   <li>Run this example</li>
 * </ol>
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class QuickstartResponsesApiKey {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String PROJECT_OCID = "<<ENTER_PROJECT_ID>>";
    // ────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // OCI Enterprise AI Agents only needs project OCID — no compartment ID required
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
                .apiKey(System.getenv("OCI_GENAI_API_KEY"))
                .addHeader("openai-project", PROJECT_OCID)
                .build();

        Response response = client.responses().create(
                ResponseCreateParams.builder()
                        .model("xai.grok-3")
                        .input("What is 2x2?")
                        .build());

        System.out.println(response.outputText());
    }
}
