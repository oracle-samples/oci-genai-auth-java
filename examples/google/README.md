# Google Gen AI SDK example

`GenerateContentApiKey.java` calls OCI Generative AI's Google-compatible endpoint with the official Google Gen AI Java SDK and an OCI Generative AI API key.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- An OCI Generative AI API key that can access the `us-chicago-1` endpoint

Set the API key in your shell. Do not add it to the example source code.

```bash
export OCI_GENAI_API_KEY=<your_oci_genai_api_key>
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

The example sends a short prompt to `google.gemini-2.5-flash` through:

```text
https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/google
```

On Windows, replace the `:` classpath separator in the last command with `;`.
