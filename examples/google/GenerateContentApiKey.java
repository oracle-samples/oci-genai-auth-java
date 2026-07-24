/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Generate content through the Google Gen AI Java SDK with an OCI Generative AI API key.
 *
 * <p>Set {@code OCI_GENAI_API_KEY} before running this example. The Google SDK dependency is
 * available as {@code com.google.genai:google-genai}.
 */

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;

public class GenerateContentApiKey {

    private static final String REGION = System.getenv().getOrDefault("OCI_GENAI_REGION", "us-chicago-1");
    private static final String MODEL =
            System.getenv().getOrDefault("OCI_GENAI_GOOGLE_MODEL", "google.gemini-2.5-flash");
    private static final String BASE_URL = System.getenv().getOrDefault(
            "OCI_GENAI_GOOGLE_BASE_URL",
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/google");

    public static void main(String[] args) {
        String apiKey = System.getenv("OCI_GENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set OCI_GENAI_API_KEY before running this example.");
        }

        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().baseUrl(BASE_URL).build())
                .build();

        GenerateContentResponse response = client.models.generateContent(
                MODEL, "Write a one-sentence bedtime story about a unicorn.", null);
        System.out.println(response.text());
    }
}
