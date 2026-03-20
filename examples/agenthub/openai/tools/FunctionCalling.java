/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates function calling tools in AgentHub using the Responses API.
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.*;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

public class FunctionCalling {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String REGION       = "us-chicago-1";
    private static final String PROJECT_OCID = "<<ENTER_PROJECT_ID>>";
    private static final String MODEL        = "xai.grok-3";
    // ────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/openai/v1";

    /** Mock weather function. */
    private static String getCurrentWeather(String location) {
        return """
                {"location": "%s", "temperature": "72", "unit": "fahrenheit", "forecast": ["sunny", "windy"]}
                """.formatted(location).trim();
    }

    public static void main(String[] args) {
        OciAuthConfig config = OciAuthConfig.builder()
                .authType("security_token")
                .profile("DEFAULT")
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .okHttpClient(ociHttpClient)
                .apiKey("not-used")
                .build();

        // Define function tool
        Tool weatherTool = Tool.ofFunction(FunctionTool.builder()
                .name("get_current_weather")
                .description("Get current weather for a given location.")
                .strict(true)
                .parameters(FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", "object")
                        .putAdditionalProperty("properties", Map.of(
                                "location", Map.of(
                                        "type", "string",
                                        "description", "City and country e.g. Bogota, Colombia")))
                        .putAdditionalProperty("required", List.of("location"))
                        .putAdditionalProperty("additionalProperties", false)
                        .build())
                .build());

        // First request — model decides to call the function
        Response response = client.responses().create(
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input("What is the weather in Seattle?")
                        .addTool(weatherTool)
                        .build());

        System.out.println("First response: " + response.output());

        // If the model requested a function call, execute it and send the result back
        for (ResponseOutputItem item : response.output()) {
            item.functionCall().ifPresent(fc -> {
                String result = getCurrentWeather("Seattle");

                Response followUp = client.responses().create(
                        ResponseCreateParams.builder()
                                .model(MODEL)
                                .addInputItem(ResponseInputItem.ofFunctionCallOutput(
                                        ResponseInputItem.FunctionCallOutput.builder()
                                                .callId(fc.callId())
                                                .output(result)
                                                .build()))
                                .addTool(weatherTool)
                                .build());

                followUp.output().forEach(out ->
                        out.message().ifPresent(msg ->
                                msg.content().forEach(c ->
                                        c.outputText().ifPresent(t ->
                                                System.out.println("Final response: " + t.text())))));
            });
        }
    }
}
