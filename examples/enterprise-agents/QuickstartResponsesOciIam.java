/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Quickstart using OCI IAM authentication with the Responses API on OCI Enterprise AI Agents.
 *
 * <p>Steps:
 * <ol>
 *   <li>Create a Generative AI Project on OCI Console</li>
 *   <li>Add oci-genai-auth-java-core dependency</li>
 *   <li>Run this example</li>
 * </ol>
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class QuickstartResponsesOciIam {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String REGION       = "us-chicago-1";
    private static final String PROJECT_OCID = "<<ENTER_PROJECT_ID>>";
    private static final String MODEL        = "xai.grok-3";
    // ────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/openai/v1";

    public static void main(String[] args) {
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // OCI Enterprise AI Agents only needs project OCID — no compartment ID required
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .okHttpClient(ociHttpClient)
                .apiKey("not-used")
                .addHeader("openai-project", PROJECT_OCID)
                .build();

        Response response = client.responses().create(
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input("What is 2x2?")
                        .build());

        System.out.println(response.outputText());
    }
}
