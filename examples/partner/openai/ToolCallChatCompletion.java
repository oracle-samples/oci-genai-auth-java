/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */

/**
 * Demonstrates tool calling with chat completions for the Partner (pass-through) endpoint.
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;

import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;

import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolCallChatCompletion {

    // ── Configuration ──────────────────────────────────────────────────
    private static final String REGION         = "us-chicago-1";
    private static final String COMPARTMENT_ID = "<<ENTER_COMPARTMENT_ID>>";
    private static final String MODEL          = "openai.gpt-4.1";
    // ────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
            "https://inference.generativeai." + REGION + ".oci.oraclecloud.com/v1";

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
                .compartmentId(COMPARTMENT_ID)
                .build();

        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .okHttpClient(ociHttpClient)
                .apiKey("not-used")
                .build();

        // Define the function tool
        ChatCompletionTool weatherTool = ChatCompletionTool.builder()
                .function(FunctionDefinition.builder()
                        .name("get_current_weather")
                        .description("Get the current weather for a specific location.")
                        .parameters(ChatCompletionTool.Function.Parameters.builder()
                                .putAdditionalProperty("type", "object")
                                .putAdditionalProperty("properties", Map.of(
                                        "location", Map.of(
                                                "type", "string",
                                                "description", "City and state, for example Boston, MA.")))
                                .putAdditionalProperty("required", List.of("location"))
                                .build())
                        .build())
                .build();

        // First request
        ChatCompletion first = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(MODEL)
                        .addUserMessage("What is the weather in Boston and San Francisco?")
                        .addTool(weatherTool)
                        .toolChoice(ChatCompletionToolChoiceOption.AUTO)
                        .build());

        ChatCompletion.Choice firstChoice = first.choices().get(0);

        if ("tool_calls".equals(firstChoice.finishReason().toString())) {
            // Execute tool calls and send results back
            List<ChatCompletionCreateParams.Message> messages = new ArrayList<>();
            messages.add(ChatCompletionCreateParams.Message.ofUser(
                    UserMessage.of("What is the weather in Boston and San Francisco?")));
            messages.add(ChatCompletionCreateParams.Message.ofAssistant(
                    firstChoice.message()));

            for (var toolCall : firstChoice.message().toolCalls().orElse(List.of())) {
                String result = getCurrentWeather(toolCall.function().name());
                messages.add(ChatCompletionCreateParams.Message.ofTool(
                        ToolMessage.builder()
                                .toolCallId(toolCall.id())
                                .content(result)
                                .build()));
            }

            ChatCompletion followUp = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(MODEL)
                            .messages(messages)
                            .build());

            followUp.choices().forEach(choice ->
                    choice.message().content().ifPresent(System.out::println));
        } else {
            firstChoice.message().content().ifPresent(System.out::println);
        }
    }
}
