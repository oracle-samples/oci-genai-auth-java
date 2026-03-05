/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Example: Calling Google Gemini via OCI GenAI using direct HTTP (no vendor SDK).
 *
 * <p>This demonstrates using oci-genai-auth-java-core with raw OkHttp when a vendor
 * SDK does not support transport injection. The OCI-signed OkHttpClient handles
 * authentication transparently — you just build and send requests.
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
 * }</pre>
 *
 * <h3>Run</h3>
 * <pre>
 * javac -cp "lib/*" OciGeminiDirectExample.java
 * java  -cp "lib/*:." OciGeminiDirectExample
 * </pre>
 */

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciEndpointResolver;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class OciGeminiDirectExample {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String AUTH_TYPE       = "security_token"; // or oci_config, instance_principal, resource_principal
    private static final String PROFILE         = "DEFAULT";
    private static final String REGION          = "us-chicago-1";
    private static final String COMPARTMENT_ID  = "ocid1.compartment.oc1..YOUR_COMPARTMENT_ID";
    private static final String API_PATH        = "/google";
    private static final String MODEL           = "google.gemini-2.5-flash";
    // ────────────────────────────────────────────────────────────────────

    private static final MediaType JSON = MediaType.parse("application/json");

    public static void main(String[] args) throws IOException {
        // 1. Build an OCI-signed OkHttpClient
        OciAuthConfig config = OciAuthConfig.builder()
                .authType(AUTH_TYPE)
                .profile(PROFILE)
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // 2. Resolve the OCI GenAI endpoint for Google Gemini
        String baseUrl = OciEndpointResolver.resolveBaseUrl(
                REGION, null, null, API_PATH);
        String url = baseUrl + "/v1beta/models/" + MODEL + ":generateContent";

        // 3. Build the request JSON (Google Gemini generateContent format)
        String requestJson = """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [
                        {
                          "text": "What is the capital of France? Answer in one sentence."
                        }
                      ]
                    }
                  ]
                }
                """;

        // 4. Send the request using the OCI-signed OkHttpClient
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson, JSON))
                .build();

        try (Response response = ociHttpClient.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            if (response.body() != null) {
                System.out.println("Response: " + response.body().string());
            }
        }
    }
}
