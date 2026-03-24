/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates streaming chat completion responses for the Partner (pass-through) endpoint.
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class StreamingChatCompletion {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String REGION         = "us-chicago-1";
    private static final String COMPARTMENT_ID = "<<ENTER_COMPARTMENT_ID>>";
    private static final String MODEL          = "openai.gpt-4.1";
    // ────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/20231130/actions/v1";

    public static void main(String[] args) {
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .okHttpClient(ociHttpClient)
                .apiKey("not-used")
                .build();

        // Stream the response
        client.chat().completions().createStreaming(
                ChatCompletionCreateParams.builder()
                        .model(MODEL)
                        .addSystemMessage("You are a concise assistant who answers in one paragraph.")
                        .addUserMessage("Explain why the sky is blue as if you were a physics teacher.")
                        .build())
                .stream()
                .flatMap(chunk -> chunk.choices().stream())
                .forEach(choice -> choice.delta().content().ifPresent(
                        content -> System.out.print(content)));

        System.out.println();
    }
}
