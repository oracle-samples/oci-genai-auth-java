# OCI GenAI Unified Java SDK

Unified Java SDK family for integrating third-party Generative AI providers (OpenAI, Anthropic) with Oracle Cloud Infrastructure authentication and routing.

## Table of Contents

- [Architecture](#architecture)
- [Installation](#installation)
- [Quick Start](#quick-start)
  - [OpenAI](#openai)
  - [Anthropic](#anthropic)
- [Authentication](#authentication)
- [Client Configuration](#client-configuration)
- [Async Clients](#async-clients)
- [Base URL and Endpoint Overrides](#base-url-and-endpoint-overrides)
- [Error Handling](#error-handling)
- [Cleanup](#cleanup)
- [Module Reference](#module-reference)
- [Building from Source](#building-from-source)
- [License](#license)

## Architecture

This SDK follows the **core + provider modules + BOM** pattern used by AWS SDK v2, Azure SDK for Java, Google Cloud Java, and OCI's own existing SDK. Users import only the provider modules they need — no forced dependency bloat.

```
oci-genai-bom           Version management only (BOM)
oci-genai-core          OCI IAM auth, request signing, header injection, endpoint resolution
oci-genai-openai        Wraps openai-java SDK with OCI signing
oci-genai-anthropic     Wraps anthropic-sdk-java with OCI signing
```

All modules share `oci-genai-core` for OCI authentication — signing logic is implemented once and applied consistently across all providers.

## Installation

The SDK requires **Java 17+**. Add the BOM and the provider modules you need:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.oracle.genai</groupId>
            <artifactId>oci-genai-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- OpenAI provider (includes core transitively) -->
    <dependency>
        <groupId>com.oracle.genai</groupId>
        <artifactId>oci-genai-openai</artifactId>
    </dependency>

    <!-- Anthropic provider (includes core transitively) -->
    <dependency>
        <groupId>com.oracle.genai</groupId>
        <artifactId>oci-genai-anthropic</artifactId>
    </dependency>
</dependencies>
```

Import only the providers you use. Each module brings in only its own dependencies.

## Quick Start

### OpenAI

```java
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.oracle.genai.openai.OciOpenAI;

public class OpenAIQuickStart {
    public static void main(String[] args) {
        OpenAIClient client = OciOpenAI.builder()
                .compartmentId("<COMPARTMENT_OCID>")
                .authType("security_token")
                .profile("DEFAULT")
                .region("us-chicago-1")
                .build();

        try {
            Response response = client.responses().create(ResponseCreateParams.builder()
                    .model("openai.gpt-4o")
                    .store(false)
                    .input("Write a short poem about cloud computing.")
                    .build());

            System.out.println(response.output());
        } finally {
            client.close();
        }
    }
}
```

### Anthropic

```java
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.oracle.genai.anthropic.OciAnthropic;

public class AnthropicQuickStart {
    public static void main(String[] args) {
        AnthropicClient client = OciAnthropic.builder()
                .compartmentId("<COMPARTMENT_OCID>")
                .authType("security_token")
                .profile("DEFAULT")
                .region("us-chicago-1")
                .build();

        try {
            Message message = client.messages().create(MessageCreateParams.builder()
                    .model(Model.CLAUDE_SONNET_4_20250514)
                    .addUserMessage("Hello from OCI!")
                    .maxTokens(1024)
                    .build());

            System.out.println(message.content());
        } finally {
            client.close();
        }
    }
}
```

## Authentication

Both `OciOpenAI` and `OciAnthropic` support all four OCI IAM authentication types through the `authType` parameter:

| Auth Type | Use Case |
|-----------|----------|
| `oci_config` | Local development with API key in `~/.oci/config` |
| `security_token` | Local development with OCI CLI session token |
| `instance_principal` | OCI Compute instances with dynamic group policies |
| `resource_principal` | OCI Functions, Container Instances |

```java
// 1) User principal (API key)
OpenAIClient client = OciOpenAI.builder()
        .authType("oci_config")
        .profile("DEFAULT")
        .compartmentId("<COMPARTMENT_OCID>")
        .region("us-chicago-1")
        .build();

// 2) Session token (local dev)
OpenAIClient client = OciOpenAI.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("<COMPARTMENT_OCID>")
        .region("us-chicago-1")
        .build();

// 3) Instance principal (OCI Compute)
OpenAIClient client = OciOpenAI.builder()
        .authType("instance_principal")
        .compartmentId("<COMPARTMENT_OCID>")
        .region("us-chicago-1")
        .build();

// 4) Resource principal (OCI Functions)
OpenAIClient client = OciOpenAI.builder()
        .authType("resource_principal")
        .compartmentId("<COMPARTMENT_OCID>")
        .region("us-chicago-1")
        .build();

// 5) Custom auth provider
BasicAuthenticationDetailsProvider authProvider = /* your provider */;
OpenAIClient client = OciOpenAI.builder()
        .authProvider(authProvider)
        .compartmentId("<COMPARTMENT_OCID>")
        .region("us-chicago-1")
        .build();
```

The same `authType` and `authProvider` parameters work identically for `OciAnthropic`.

## Client Configuration

| Parameter | Description | Required |
|-----------|-------------|----------|
| `compartmentId` | OCI compartment OCID | Yes (for GenAI endpoints) |
| `authType` or `authProvider` | Authentication mechanism | Yes |
| `region` | OCI region code (e.g., `us-chicago-1`) | Yes (unless `baseUrl` or `serviceEndpoint` is set) |
| `baseUrl` | Fully qualified endpoint override | No |
| `serviceEndpoint` | Service endpoint without API path | No |
| `conversationStoreId` | Conversation Store OCID (OpenAI only) | No |
| `timeout` | Request timeout (default: 2 minutes) | No |
| `logRequestsAndResponses` | Debug logging of HTTP bodies | No |
| `profile` | OCI config profile name (default: `DEFAULT`) | No |

## Async Clients

Both providers include async client builders that return `CompletableFuture`-based clients:

```java
import com.oracle.genai.openai.AsyncOciOpenAI;
import com.oracle.genai.anthropic.AsyncOciAnthropic;

// Async OpenAI
OpenAIClientAsync openaiAsync = AsyncOciOpenAI.builder()
        .compartmentId("<COMPARTMENT_OCID>")
        .authType("security_token")
        .region("us-chicago-1")
        .build();

openaiAsync.responses().create(params)
        .thenAccept(response -> System.out.println(response.output()));

// Async Anthropic
AnthropicClientAsync anthropicAsync = AsyncOciAnthropic.builder()
        .compartmentId("<COMPARTMENT_OCID>")
        .authType("security_token")
        .region("us-chicago-1")
        .build();

anthropicAsync.messages().create(params)
        .thenAccept(message -> System.out.println(message.content()));
```

## Base URL and Endpoint Overrides

Endpoint resolution priority (highest to lowest): `baseUrl` > `serviceEndpoint` > `region`.

```java
// From region (most common)
OciOpenAI.builder()
        .region("us-chicago-1")
        // resolves to: https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/openai/v1
        ...

// From service endpoint (SDK appends API path)
OciOpenAI.builder()
        .serviceEndpoint("https://inference.generativeai.us-chicago-1.oci.oraclecloud.com")
        // resolves to: https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/openai/v1
        ...

// From explicit base URL (used as-is)
OciOpenAI.builder()
        .baseUrl("https://custom-endpoint.example.com/v1")
        ...
```

## Error Handling

The underlying provider SDK exceptions still apply. Catch provider-specific exceptions for error handling:

```java
// OpenAI
try {
    Response response = client.responses().create(params);
} catch (com.openai.errors.NotFoundException | com.openai.errors.UnauthorizedException e) {
    System.err.println("Error: " + e.getMessage());
}

// Anthropic
try {
    Message message = client.messages().create(params);
} catch (com.anthropic.errors.NotFoundException | com.anthropic.errors.UnauthorizedException e) {
    System.err.println("Error: " + e.getMessage());
}
```

## Cleanup

Both client types implement `AutoCloseable`. Close them when finished to release HTTP resources:

```java
try (OpenAIClient client = OciOpenAI.builder()
        .compartmentId("<COMPARTMENT_OCID>")
        .authType("security_token")
        .region("us-chicago-1")
        .build()) {
    // use client
}
```

## Module Reference

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `oci-genai-bom` | `com.oracle.genai:oci-genai-bom` | Pins all module and transitive dependency versions |
| `oci-genai-core` | `com.oracle.genai:oci-genai-core` | OCI IAM auth providers, per-request signing, header injection, endpoint resolution |
| `oci-genai-openai` | `com.oracle.genai:oci-genai-openai` | Wraps `openai-java` with OCI signing via custom `HttpClient` |
| `oci-genai-anthropic` | `com.oracle.genai:oci-genai-anthropic` | Wraps `anthropic-sdk-java` with OCI signing via custom `HttpClient` |

## Building from Source

Requires Java 17+ and Maven 3.8+.

```bash
# Compile all modules
mvn clean compile

# Run tests
mvn test

# Install to local Maven repository
mvn install -DskipTests
```

## License

Copyright (c) 2025 Oracle and/or its affiliates.

Released under the [Universal Permissive License v1.0](https://oss.oracle.com/licenses/upl/).
