# Partner Examples

Partner endpoints provide access to OpenAI, xAI Grok, and Meta Llama models via the
OpenAI Chat Completions API. The compartment OCID header is required.

## Prerequisites

1. Update `COMPARTMENT_ID` and `REGION` in each example file.
2. Authenticate with OCI:
   - **OCI IAM**: `oci session authenticate`
   - **API Key**: Set `OCI_GENAI_API_KEY` environment variable

## Base URL

```
https://inference.generativeai.<REGION>.oci.oraclecloud.com/20231130/actions/v1
```

## Examples

| File | Description |
|------|-------------|
| `BasicChatCompletion.java` | Basic chat completion with OCI IAM auth |
| `BasicChatCompletionApiKey.java` | Chat completion with API key auth |
| `StreamingChatCompletion.java` | Streaming chat completion |
| `ToolCallChatCompletion.java` | Function/tool calling with chat completions |

## Running

These are standalone Java files. Copy them into your project with the required dependencies
(`oci-genai-auth-java-core` and `openai-java`), then compile and run.
