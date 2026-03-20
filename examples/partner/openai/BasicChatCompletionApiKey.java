/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates chat completion using OCI Generative AI API Key authentication.
 *
 * <p>No oci-genai-auth package needed for API Key auth — just the official OpenAI SDK.
 *
 * <p>Steps:
 * <ol>
 *   <li>Create API keys in Console: Generative AI → API Keys</li>
 *   <li>Set OPENAI_API_KEY environment variable</li>
 *   <li>Run this example</li>
 * </ol>
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class BasicChatCompletionApiKey {

    private static final String MODEL = "openai.gpt-4.1";

    public static void main(String[] args) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build();

        ChatCompletion completion = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(MODEL)
                        .addSystemMessage("You are a concise assistant who answers in one paragraph.")
                        .addUserMessage("Explain why the sky is blue as if you were a physics teacher.")
                        .build());

        completion.choices().forEach(choice ->
                choice.message().content().ifPresent(System.out::println));
    }
}
