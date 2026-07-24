# Google Gen AI SDK examples

This directory contains standalone examples that use the official [Google Gen AI Java SDK](https://github.com/googleapis/java-genai) with OCI Generative AI's Google-compatible endpoint.

`GenerateContentApiKey.java` is an API-key example. It sends a simple prompt and prints the generated text.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- An OCI Generative AI API key for the target endpoint

## Configuration

Set the API key in your shell. Never add it to example source code or commit it.

```bash
export OCI_GENAI_API_KEY=<your_oci_genai_api_key>
```

The example accepts the following optional environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `OCI_GENAI_REGION` | `us-chicago-1` | OCI region used to construct the Google-compatible endpoint. |
| `OCI_GENAI_GOOGLE_BASE_URL` | Derived from `OCI_GENAI_REGION` | Full endpoint override, for example a private or custom endpoint. |
| `OCI_GENAI_GOOGLE_MODEL` | `google.gemini-2.5-flash` | Model ID passed to the Google Gen AI SDK. |

For example:

```bash
export OCI_GENAI_REGION=<your_oci_region>
export OCI_GENAI_GOOGLE_MODEL=<your_google_model_id>
```

## Run

From the repository root, build the example classpath, compile the example, and run it:

```bash
mvn -pl oci-genai-auth-java-core dependency:build-classpath \
  -Dmdep.outputFile=/tmp/oci-genai-auth-java-google.classpath

mkdir -p /tmp/oci-genai-auth-java-google-classes
javac -cp "$(cat /tmp/oci-genai-auth-java-google.classpath)" \
  -d /tmp/oci-genai-auth-java-google-classes \
  examples/google/GenerateContentApiKey.java

java -cp "/tmp/oci-genai-auth-java-google-classes:$(cat /tmp/oci-genai-auth-java-google.classpath)" \
  GenerateContentApiKey
```

The default endpoint is `https://inference.generativeai.<region>.oci.oraclecloud.com/google`.

On Windows, replace the `:` classpath separator in the last command with `;`, and use the equivalent commands for setting environment variables.
