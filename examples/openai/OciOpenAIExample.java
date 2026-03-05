/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Example: Using the OpenAI Java SDK with OCI GenAI via oci-genai-auth-java-core.
 *
 * <p>This file is a standalone example — it is NOT compiled as part of the Maven build.
 * Copy it into your own project and add the required dependencies (see below).
 *
 * <h3>Dependencies (Maven)</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.oracle.genai</groupId>
 *     <artifactId>oci-genai-auth-java-core</artifactId>
 *     <version>0.1.0-SNAPSHOT</version>
 * </dependency>
 * <dependency>
 *     <groupId>com.openai</groupId>
 *     <artifactId>openai-java</artifactId>
 *     <version>0.34.1</version>
 * </dependency>
 * }</pre>
 *
 * <h3>Run</h3>
 * <pre>
 * javac -cp "lib/*" OciOpenAIExample.java
 * java  -cp "lib/*:." OciOpenAIExample
 * </pre>
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciEndpointResolver;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class OciOpenAIExample {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String AUTH_TYPE       = "security_token"; // or oci_config, instance_principal, resource_principal
    private static final String PROFILE         = "DEFAULT";
    private static final String REGION          = "us-chicago-1";
    private static final String COMPARTMENT_ID  = "ocid1.compartment.oc1..YOUR_COMPARTMENT_ID";
    private static final String API_PATH        = "/20231130/actions/chat";
    // ────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // 1. Build an OCI-signed OkHttpClient
        OciAuthConfig config = OciAuthConfig.builder()
                .authType(AUTH_TYPE)
                .profile(PROFILE)
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // 2. Resolve the OCI GenAI endpoint for OpenAI-compatible API
        String baseUrl = OciEndpointResolver.resolveBaseUrl(
                REGION, null, null, API_PATH);

        // 3. Plug the OCI-signed client into the OpenAI SDK
        OpenAIClient openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .okHttpClient(ociHttpClient)
                .apiKey("OCI_AUTH")  // placeholder; OCI signing replaces API key auth
                .build();

        // 4. Send a chat completion request
        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model("meta.llama-3.1-405b-instruct")
                        .addUserMessage("What is the capital of France? Answer in one sentence.")
                        .build());

        // 5. Print the response
        completion.choices().forEach(choice ->
                choice.message().content().ifPresent(content ->
                        System.out.println("Response: " + content)));
    }
}
