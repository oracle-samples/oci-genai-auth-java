# AgentHub Examples

AgentHub provides a unified interface for interacting with models and agentic capabilities.
It is compatible with OpenAI's Responses API and the Open Responses Spec, enabling
developers to build agents with the OpenAI SDK.

## Prerequisites

1. Create a **Generative AI Project** on OCI Console.
2. Update `PROJECT_OCID` and `REGION` in each example file.
3. Authenticate with OCI:
   - **OCI IAM**: `oci session authenticate`
   - **API Key**: Set `OCI_GENAI_API_KEY` environment variable

## Base URL

```
https://inference.generativeai.<REGION>.oci.oraclecloud.com/openai/v1
```

## Examples

| File | Description |
|------|-------------|
| `openai/QuickstartResponsesOciIam.java` | Quickstart with OCI IAM authentication |
| `openai/QuickstartResponsesApiKey.java` | Quickstart with API key authentication |
| `openai/responses/CreateResponse.java` | Create a response |
| `openai/responses/StreamingTextDelta.java` | Stream response text deltas |
| `openai/tools/WebSearch.java` | Web search tool |
| `openai/tools/FunctionCalling.java` | Function calling tool |

## Running

These are standalone Java files. Copy them into your project with the required dependencies
(`oci-genai-auth-java-core` and `openai-java`), then compile and run.
