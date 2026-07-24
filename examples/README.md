# Examples

This directory contains Java examples for OCI Generative AI compatibility endpoints:

- `enterprise_ai_agents/` uses the OpenAI Java SDK with OCI Enterprise AI Agents' OpenAI-compatible `/openai/v1` endpoint.
- `google/` uses the Google Gen AI Java SDK with OCI Generative AI's Google-compatible `/google` endpoint.

All examples are standalone Java classes. They require Java 17 or later and Maven 3.8 or later.

## Example catalog

| File | Authentication | Description |
| --- | --- | --- |
| `enterprise_ai_agents/QuickstartResponsesOciIam.java` | OCI IAM | Create a Responses API response. |
| `enterprise_ai_agents/QuickstartResponsesApiKey.java` | API key | Create a Responses API response. |
| `enterprise_ai_agents/responses/CreateResponse.java` | OCI IAM | Create a response. |
| `enterprise_ai_agents/responses/StreamingTextDelta.java` | OCI IAM | Stream response text deltas. |
| `enterprise_ai_agents/tools/FunctionCalling.java` | OCI IAM | Use function calling. |
| `enterprise_ai_agents/tools/WebSearch.java` | OCI IAM | Use web search. |
| `google/GenerateContentApiKey.java` | API key | Generate content through the Google Gen AI SDK. |

## Configure authentication

### API key examples

Set an OCI Generative AI API key in the environment. Do not add credentials to source files.

```bash
export OCI_GENAI_API_KEY=<your_oci_genai_api_key>
```

### OCI IAM examples

Ensure your OCI CLI profile is configured and authenticated. The Enterprise AI Agents IAM examples use the profile configured in their source files (normally `DEFAULT`).

## Configure the selected example

Enterprise AI Agents examples require values such as `PROJECT_OCID` and `REGION`. Set the constants in the chosen source file before compiling it. They use an endpoint in the form:

```text
https://inference.generativeai.<region>.oci.oraclecloud.com/openai/v1
```

The Google example can be configured through environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `OCI_GENAI_REGION` | `us-chicago-1` | Region used to construct the endpoint. |
| `OCI_GENAI_GOOGLE_BASE_URL` | Derived from `OCI_GENAI_REGION` | Full endpoint override. |
| `OCI_GENAI_GOOGLE_MODEL` | `google.gemini-2.5-flash` | Google model ID. |

Its default endpoint is:

```text
https://inference.generativeai.<region>.oci.oraclecloud.com/google
```

## Build and run any example

Run these commands from the repository root. First build the core module and create a classpath containing its SDK dependencies:

```bash
mvn -pl oci-genai-auth-java-core package -DskipTests
mvn -pl oci-genai-auth-java-core dependency:build-classpath \
  -Dmdep.outputFile=/tmp/oci-genai-auth-java-examples.classpath
```

Set `EXAMPLE` to the Java file you want to run and `CLASS_NAME` to its class name. All current examples use the default package, so the class name is the filename without `.java`.

```bash
EXAMPLE=examples/google/GenerateContentApiKey.java
CLASS_NAME=GenerateContentApiKey
CLASS_DIR=/tmp/oci-genai-auth-java-example-classes

mkdir -p "$CLASS_DIR"
javac -cp "oci-genai-auth-java-core/target/classes:$(cat /tmp/oci-genai-auth-java-examples.classpath)" \
  -d "$CLASS_DIR" "$EXAMPLE"

java -cp "$CLASS_DIR:oci-genai-auth-java-core/target/classes:$(cat /tmp/oci-genai-auth-java-examples.classpath)" \
  "$CLASS_NAME"
```

For example, to run the IAM Responses quickstart, use:

```bash
EXAMPLE=examples/enterprise_ai_agents/QuickstartResponsesOciIam.java
CLASS_NAME=QuickstartResponsesOciIam
```

On Windows, replace the `:` classpath separator with `;`, and use the platform-equivalent environment-variable commands.
