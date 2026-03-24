# oci-genai-auth-java

The **OCI GenAI Auth** Java library provides OCI request-signing helpers for the OpenAI-compatible REST APIs hosted by OCI Generative AI.

## Table of Contents

- [Installation](#installation)
- [Using OCI IAM Auth](#using-oci-iam-auth)
- [Using API Key Auth](#using-api-key-auth)
- [Using AgentHub APIs](#using-agenthub-apis)
- [Using Partner APIs (passthrough)](#using-partner-apis-passthrough)
- [Examples](#examples)
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
            <version>1.0.0</version>
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

## Using OCI IAM Auth

Use OCI IAM auth when you want to sign requests with your OCI profile (session/user/resource/instance principal). Recommended if you are building OCI-native production workloads.

```java
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
        .okHttpClient(ociHttpClient)
        .apiKey("not-used")
        .build();
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

## Using AgentHub APIs

OCI AgentHub provides a unified API for interacting with models and agentic capabilities.

- It is compatible with OpenAI's Responses API and the [Open Responses Spec](https://www.openresponses.org/specification), enabling developers to build agents with OpenAI SDK, OpenAI Agents SDK, LangChain, LangGraph, AI SDK, CrewAI, and more.
- It offers a uniform interface, auth, billing to access multiple model providers including OpenAI, Gemini, xAI, and GPT-OSS models hosted in OCI and your Dedicated AI Cluster.
- It provides built-in agentic primitives such as agent loop, reasoning, short-term memory, long-term memory, web search, file search, image generation, code execution, and more.

In addition to the compatible endpoint to Responses API, AgentHub also offers compatible endpoints to Files API, Vector Stores API, and Containers API.

Explore [examples](examples/agenthub) to get started.

Note: OpenAI commercial models and image generation are only available to Oracle internal teams at this moment.

```java
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
        .okHttpClient(ociHttpClient)
        .apiKey("not-used")
        .addHeader("openai-project", "ocid1.generativeaiproject.oc1.us-chicago-1.aaaaaaaaexample")
        .build();
```

## Using Partner APIs (passthrough)

OCI also offers Partner API which passes through your calls to partners such as OpenAI. We will support more partners in the future.

You can leverage Partner API when you want to use OpenAI's API and GPT models, but with OCI auth and billing.

Note: Currently Partner API is only available to Oracle internal teams. Only features that meet partner's Zero Data Retention are available through Partner API.

If you want multi-provider model access and features unavailable under partner's Zero Data Retention (such as File Search), use the AgentHub APIs above.

```java
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("ocid1.compartment.oc1..aaaaaaaaexample")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1")
        .okHttpClient(ociHttpClient)
        .apiKey("not-used")
        .build();
```

## Examples
Demo code and instructions on how to run them, for both agenthub and partner usecases can be found in [examples](examples/) folder.

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License

Copyright (c) 2026 Oracle and/or its affiliates.

Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/.
