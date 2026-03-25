/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates streaming Responses API output and handling text deltas.
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class StreamingTextDelta {

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

        client.responses().createStreaming(
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input("What are the shapes of OCI GPUs?")
                        .build())
                .stream()
                .forEach(event -> {
                    event.asResponseOutputTextDeltaEvent().ifPresent(delta ->
                            System.out.print(delta.delta()));
                });

        System.out.println();
    }
}
