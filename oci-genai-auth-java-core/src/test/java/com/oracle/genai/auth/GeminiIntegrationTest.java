/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: OCI auth library + direct HTTP for Gemini via OCI GenAI.
 *
 * <p>Demonstrates that the OCI-signed OkHttpClient works with raw HTTP calls
 * (no vendor SDK needed) — useful when a vendor SDK doesn't support transport injection.
 *
 * <p>To run:
 * <pre>
 * oci session authenticate
 * mvn -pl oci-genai-auth-java-core test -Dtest=GeminiIntegrationTest
 * </pre>
 */
class GeminiIntegrationTest {

    private static final String COMPARTMENT_ID =
            "ocid1.tenancy.oc1..aaaaaaaaumuuscymm6yb3wsbaicfx3mjhesghplvrvamvbypyehh5pgaasna";

    private static final String BASE_URL =
            "https://ppe.inference.generativeai.us-chicago-1.oci.oraclecloud.com/google";

    private static final MediaType JSON = MediaType.parse("application/json");

    @Test
    @Disabled("Requires live OCI session + Gemini endpoint availability on PPE")
    void gemini_via_direct_http() throws IOException {
        // 1. Build OCI-signed OkHttpClient
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        // 2. Build request JSON (Google Gemini generateContent format)
        String model = "google.gemini-2.5-flash";
        String url = BASE_URL + "/v1beta/models/" + model + ":generateContent";

        String requestJson = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "What is 2 + 2? Answer in one word."
                        }
                      ]
                    }
                  ]
                }
                """;

        // 3. Send request
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson, JSON))
                .build();

        try (Response response = ociHttpClient.newCall(request).execute()) {
            System.out.println("Gemini status: " + response.code());
            String body = response.body() != null ? response.body().string() : "";
            System.out.println("Gemini response: " + body);

            // Accept 200 (success) or 4xx (model not available on PPE) —
            // the key validation is that we don't get 401 (auth works)
            assertNotEquals(401, response.code(), "Should not get 401 — auth signing should work");
            assertNotEquals(403, response.code(), "Should not get 403 — auth signing should work");
        }
    }
}
