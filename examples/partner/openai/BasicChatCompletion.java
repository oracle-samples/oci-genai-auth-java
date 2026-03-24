/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates a basic chat completion request for the Partner (pass-through) endpoint.
 *
 * <p>This file is a standalone example — it is NOT compiled as part of the Maven build.
 * Copy it into your own project and add the required dependencies.
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class BasicChatCompletion {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String REGION         = "us-chicago-1";
    private static final String COMPARTMENT_ID = "<<ENTER_COMPARTMENT_ID>>";
    private static final String MODEL          = "openai.gpt-4.1";
    // ────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/20231130/actions/v1";

    public static void main(String[] args) {
        // 1. Build an OCI-signed OkHttpClient
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // 2. Plug the OCI-signed client into the OpenAI SDK
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .okHttpClient(ociHttpClient)
                .apiKey("not-used")
                .build();

        // 3. Send a chat completion request
        ChatCompletion completion = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(MODEL)
                        .addSystemMessage("You are a concise assistant.")
                        .addUserMessage("List three creative uses for a paperclip.")
                        .maxTokens(128)
                        .build());

        // 4. Print the response
        completion.choices().forEach(choice ->
                choice.message().content().ifPresent(System.out::println));
    }
}
