# oci-genai-auth-java

The **OCI GenAI Auth** Java library provides OCI request-signing helpers for the OpenAI-compatible REST APIs hosted by OCI Generative AI.

## Table of Contents

- [Before you start](#before-you-start)
- [Using OCI IAM Auth](#using-oci-iam-auth)
- [Using API Key Auth](#using-api-key-auth)
- [Using AgentHub APIs](#using-agenthub-apis)
- [Using Partner APIs](#using-partner-apis)
- [Running the Examples](#running-the-examples)
- [Building from Source](#building-from-source)

## Before you start

**Important!**

Note that this package, as well as API keys described below, only supports OpenAI, xAi Grok and Meta LLama models on OCI Generative AI.

Before you start using this package, determine if this is the right option for you.

If you are looking for a seamless way to port your code from an OpenAI compatible endpoint to OCI Generative AI endpoint, and you are currently using OpenAI-style API keys, you might want to use [OCI Generative AI API Keys](https://docs.oracle.com/en-us/iaas/Content/generative-ai/api-keys.htm) instead.

With OCI Generative AI API Keys, use the native `openai-java` SDK like before. Just update the `base_url`, create API keys in your OCI console, ensure the policy granting the key access to generative AI services is present and you are good to go.

- Create an API key in Console: **Generative AI** -> **API Keys**
- Create a security policy: **Identity & Security** -> **Policies**

To authorize a specific API Key
```
allow any-user to use generative-ai-family in compartment <compartment-name> where ALL { request.principal.type='generativeaiapikey', request.principal.id='ocid1.generativeaiapikey.oc1.us-chicago-1....' }
```

To authorize any API Key
```
allow any-user to use generative-ai-family in compartment <compartment-name> where ALL { request.principal.type='generativeaiapikey' }
```

## Installation

Requires **Java 17+** and **Maven 3.8+**.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.oracle.genai</groupId>
            <artifactId>oci-genai-auth-java-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
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

Use OCI IAM auth when you want to sign requests with your OCI profile (session/user/resource/instance principal) instead of API keys.

```java
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;

OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("ocid1.compartment.oc1..aaaaaaaaexample")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

// Plug the OCI-signed OkHttpClient into the OpenAI SDK
OpenAIClient client = new OpenAIClientImpl(
        ClientOptions.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1")
                .apiKey("not-used")
                .httpClient(new OpenAIOkHttpAdapter(ociHttpClient, baseUrl))
                .build());
```

## Using API Key Auth

Use OCI Generative AI API Keys if you want a direct API-key workflow with the OpenAI SDK.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;

OpenAIClient client = new OpenAIClientImpl(
        ClientOptions.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
                .apiKey(System.getenv("OCI_GENAI_API_KEY"))
                .build());
```

## Using AgentHub APIs

AgentHub provides a unified interface for interacting with models and agentic capabilities. It is compatible with OpenAI's Responses API and the Open Responses Spec, enabling developers to build agents with the OpenAI SDK. Only the project OCID is required.

```java
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

OpenAIClient client = new OpenAIClientImpl(
        ClientOptions.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/openai/v1")
                .apiKey("not-used")
                .httpClient(new OpenAIOkHttpAdapter(ociHttpClient, baseUrl))
                .build());
```

## Using Partner APIs

Partner endpoints require the compartment OCID header.

```java
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("ocid1.compartment.oc1..aaaaaaaaexample")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

// The compartment ID is automatically injected as a header by the library
OpenAIClient client = new OpenAIClientImpl(
        ClientOptions.builder()
                .baseUrl("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/v1")
                .apiKey("not-used")
                .httpClient(new OpenAIOkHttpAdapter(ociHttpClient, baseUrl))
                .build());
```

## Authentication Types

| Auth Type | Use Case |
|-----------|----------|
| `oci_config` | Local development with API key in `~/.oci/config` |
| `security_token` | Local development with OCI CLI session token |
| `instance_principal` | OCI Compute instances with dynamic group policies |
| `resource_principal` | OCI Functions, Container Instances |

## Running the Examples

1. Update the constants in each example with your `COMPARTMENT_ID`, `PROJECT_OCID`, and set the correct `REGION`.
2. Set the `OCI_GENAI_API_KEY` environment variable when an example uses API key authentication.
3. Install dependencies: `mvn install -DskipTests`.

The [examples/](examples/) directory is organized as follows:

| Directory | Description |
|-----------|-------------|
| [examples/agenthub/openai/](examples/agenthub/openai/) | AgentHub examples using the OpenAI Responses API |
| [examples/partner/openai/](examples/partner/openai/) | Partner examples using OpenAI Chat Completions |

These examples are **not** compiled as part of the Maven build. Copy them into your own project.

## Building from Source

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Full verification
mvn clean verify

# Install to local Maven repository
mvn install -DskipTests

# Confirm no vendor SDK dependencies
mvn dependency:tree -pl oci-genai-auth-java-core
```

## License

Copyright (c) 2026 Oracle and/or its affiliates.

Released under the [Universal Permissive License v1.0](https://oss.oracle.com/licenses/upl/).
