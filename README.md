# oci-genai-auth-java

The **OCI GenAI Auth** Java library provides OCI request-signing helpers for SDKs that call OCI Generative AI, including the OpenAI and Google Gen AI SDKs.

## Table of Contents

- [Installation](#installation)
- [Using OCI IAM Auth](#using-oci-iam-auth)
- [Using API Key Auth](#using-api-key-auth)
- [Using the Google Gen AI SDK](#using-the-google-gen-ai-sdk)
- [Using OCI Enterprise AI Agents APIs](#using-oci-enterprise-ai-agents-apis)
- [Examples](#examples)
- [Release Notes](#release-notes)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

## Installation

Requires **Java 17+** and **Maven 3.8+**.

`oci-genai-auth-java` is designed to work together with the official [OpenAI Java SDK](https://github.com/openai/openai-java).

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.oracle.genai</groupId>
            <artifactId>oci-genai-auth-java-bom</artifactId>
            <version>1.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.oracle.genai</groupId>
        <artifactId>oci-genai-auth-java-core</artifactId>
    </dependency>
</dependencies>
```

## Using the Google Gen AI SDK

For OCI Generative AI API-key authentication, configure the official Google Gen AI SDK with OCI's `/google` endpoint:

```java
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;

Client client = Client.builder()
        .apiKey(System.getenv("OCI_GENAI_API_KEY"))
        .httpOptions(HttpOptions.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/google")
                .build())
        .build();
```

The signing interceptor also removes `Authorization`, `X-Api-Key`, `x-goog-api-key`, and the `key` query parameter before applying OCI IAM signing. This prevents SDK credentials from conflicting when using an OCI-signing OkHttp transport.

## Using OCI IAM Auth

Use OCI IAM auth when you want to sign requests with your OCI profile (session/user/resource/instance principal). Recommended if you are building OCI-native production workloads.

```java
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import com.oracle.genai.auth.OciOpenAIHttpClient;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import okhttp3.OkHttpClient;

OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

String baseUrl = "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1";

OpenAIClient client = new OpenAIClientImpl(ClientOptions.builder()
        .httpClient(OciOpenAIHttpClient.of(ociHttpClient, baseUrl))
        .baseUrl(baseUrl)
        .apiKey("not-used")
        .build());
```

## Using API Key Auth

Use OCI Generative AI API Keys if you want a long-lived API key style auth. Recommended if you are migrating from other OpenAI-compatible API providers.

To create the OCI Generative AI API Keys, follow [this guide](https://docs.oracle.com/en-us/iaas/Content/generative-ai/api-keys.htm).

You don't need to install `oci-genai-auth-java` if you use API key auth.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
        .apiKey(System.getenv("OCI_GENAI_API_KEY"))
        .build();
```

## Using OCI Enterprise AI Agents APIs

OCI Enterprise AI Agents provides a unified API for interacting with models and agentic capabilities.

- It is compatible with OpenAI's Responses API and the [Open Responses Spec](https://www.openresponses.org/specification), enabling developers to build agents with OpenAI SDK, OpenAI Agents SDK, LangChain, LangGraph, AI SDK, CrewAI, and more.
- It offers a uniform interface, auth, billing to access multiple model providers including OpenAI, Gemini, xAI, and GPT-OSS models hosted in OCI and your Dedicated AI Cluster.
- It provides built-in agentic primitives such as agent loop, reasoning, short-term memory, long-term memory, web search, file search, image generation, code execution, and more.

In addition to the compatible endpoint to Responses API, OCI Enterprise AI Agents also offers compatible endpoints to Files API, Vector Stores API, and Containers API.

Explore [examples](examples/enterprise_ai_agents) to get started.

Note: OpenAI commercial models and image generation are only available to Oracle internal teams at this moment.

```java
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import com.oracle.genai.auth.OciOpenAIHttpClient;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import okhttp3.OkHttpClient;

OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

String baseUrl = "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1";

OpenAIClient client = new OpenAIClientImpl(ClientOptions.builder()
        .httpClient(OciOpenAIHttpClient.of(ociHttpClient, baseUrl))
        .baseUrl(baseUrl)
        .apiKey("not-used")
        .putHeader("openai-project", "ocid1.generativeaiproject.oc1.us-chicago-1.aaaaaaaaexample")
        .build());
```

## Examples
Demo code and instructions on how to run them can be found in [examples](examples/) folder.

## Release Notes

See [RELEASE_NOTES.md](./RELEASE_NOTES.md).

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License

Copyright (c) 2026 Oracle and/or its affiliates.

Released under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/.
