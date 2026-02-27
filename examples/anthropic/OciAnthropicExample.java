/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Example: Using the Anthropic Java SDK with OCI GenAI via oci-genai-auth-java-core.
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
 *     <groupId>com.anthropic</groupId>
 *     <artifactId>anthropic-java</artifactId>
 *     <version>1.2.0</version>
 * </dependency>
 * }</pre>
 *
 * <h3>Run</h3>
 * <pre>
 * javac -cp "lib/*" OciAnthropicExample.java
 * java  -cp "lib/*:." OciAnthropicExample
 * </pre>
 */

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.Model;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciEndpointResolver;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

public class OciAnthropicExample {

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

        // 2. Resolve the OCI GenAI endpoint for Anthropic
        String baseUrl = OciEndpointResolver.resolveBaseUrl(
                REGION, null, null, API_PATH);

        // 3. Plug the OCI-signed client into the Anthropic SDK
        //    The Anthropic SDK expects an HTTP transport — we provide the signed OkHttpClient.
        AnthropicClient anthropicClient = AnthropicOkHttpClient.builder()
                .baseUrl(baseUrl)
                .okHttpClient(ociHttpClient)
                .apiKey("OCI_AUTH")  // placeholder; OCI signing replaces API key auth
                .build();

        // 4. Send a chat completion request
        Message message = anthropicClient.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_HAIKU_4_5_20251001)
                        .maxTokens(256)
                        .addUserMessage("What is the capital of France? Answer in one sentence.")
                        .build());

        // 5. Print the response
        for (ContentBlock block : message.content()) {
            block.text().ifPresent(textBlock ->
                    System.out.println("Response: " + textBlock.text()));
        }
    }
}
