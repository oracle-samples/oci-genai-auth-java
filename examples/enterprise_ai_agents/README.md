# OCI Enterprise AI Agents Examples

This folder contains examples for OCI Enterprise AI Agents APIs using the OpenAI Java SDK.

## Prerequisites

1. Install dependencies:

   ```bash
   mvn install -DskipTests
   ```

2. Configure constants in each example file:
   - `PROJECT_OCID`
   - `REGION`

3. (Optional) You can override project at runtime:

   ```bash
   export OCI_GENAI_PROJECT_ID=<your_project_ocid>
   ```

4. If running API-key based examples, set:

   ```bash
   export OCI_GENAI_API_KEY=<your_oci_genai_api_key>
   ```

## Examples

Quickstarts:

| File | Description |
|------|-------------|
| `QuickstartResponsesOciIam.java` | Quickstart with OCI IAM authentication |
| `QuickstartResponsesApiKey.java` | Quickstart with API key authentication |

Responses API examples:

| File | Description |
|------|-------------|
| `responses/CreateResponse.java` | Create a response |
| `responses/StreamingTextDelta.java` | Stream response text deltas |

Tools examples:

| File | Description |
|------|-------------|
| `tools/FunctionCalling.java` | Function calling tool |
| `tools/WebSearch.java` | Web search tool |

## Notes

- Most examples use IAM signing through `oci-genai-auth-java`.
- OCI Enterprise AI Agents examples use OpenAI-compatible `/openai/v1` endpoints and require a project OCID.
