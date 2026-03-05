# OCI GenAI Auth for Java

Vendor-neutral OCI authentication and request signing library for Java. Provides an OCI-signed `OkHttpClient` that you can plug into **any** vendor SDK or use directly with raw HTTP.

## What This Library Does

- **OCI IAM request signing** — RSA-SHA256 signatures on every request, including body digest for POST/PUT
- **Auth provider factory** — supports `oci_config`, `security_token`, `instance_principal`, and `resource_principal`
- **Header injection** — automatically adds `CompartmentId` and custom headers
- **Endpoint resolution** — derives OCI GenAI service URLs from region codes
- **Token refresh** — handled automatically by the underlying OCI Java SDK auth providers

## What This Library Does NOT Do

- Does **not** generate provider request/response models (no OpenAPI/codegen)
- Does **not** wrap or re-export any vendor SDK (OpenAI, Anthropic, Gemini, etc.)
- Does **not** include provider-specific shim classes

This is an **auth utilities library**. Vendor SDK integration is shown in the [examples/](examples/) directory.

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

## Quick Start

### Using OciAuthConfig (recommended)

```java
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import okhttp3.OkHttpClient;

OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("ocid1.compartment.oc1..xxx")
        .build();

OkHttpClient client = OciOkHttpClientFactory.build(config);
// Use this client with any vendor SDK that accepts an OkHttpClient,
// or make direct HTTP calls — every request is signed automatically.
```

### Direct factory method

```java
import com.oracle.genai.auth.OciAuthProviderFactory;
import com.oracle.genai.auth.OciOkHttpClientFactory;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

BasicAuthenticationDetailsProvider authProvider =
        OciAuthProviderFactory.create("security_token", "DEFAULT");

OkHttpClient client = OciOkHttpClientFactory.create(authProvider, "ocid1.compartment.oc1..xxx");
```

## Authentication Types

| Auth Type | Use Case |
|-----------|----------|
| `oci_config` | Local development with API key in `~/.oci/config` |
| `security_token` | Local development with OCI CLI session token |
| `instance_principal` | OCI Compute instances with dynamic group policies |
| `resource_principal` | OCI Functions, Container Instances |

```java
// Session token (local dev)
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("<COMPARTMENT_OCID>")
        .build();

// Instance principal (OCI Compute)
OciAuthConfig config = OciAuthConfig.builder()
        .authType("instance_principal")
        .compartmentId("<COMPARTMENT_OCID>")
        .build();
```

## Endpoint Resolution

Use `OciEndpointResolver` to derive service URLs from region codes:

```java
import com.oracle.genai.auth.OciEndpointResolver;

// From region — most common
String url = OciEndpointResolver.resolveBaseUrl("us-chicago-1", null, null, "/20231130/actions/chat");
// → https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/chat

// From service endpoint (API path appended)
String url = OciEndpointResolver.resolveBaseUrl(null,
        "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com",
        null, "/20231130/actions/chat");

// From explicit base URL (used as-is)
String url = OciEndpointResolver.resolveBaseUrl(null, null,
        "https://custom-endpoint.example.com/v1", null);
```

Resolution priority: `baseUrl` > `serviceEndpoint` > `region`.

## Configuration

| Parameter | Description | Required |
|-----------|-------------|----------|
| `authType` | OCI authentication type (see table above) | Yes |
| `profile` | OCI config profile name (default: `DEFAULT`) | No |
| `compartmentId` | OCI compartment OCID | Yes (for GenAI endpoints) |
| `region` | OCI region code (e.g., `us-chicago-1`) | No (for endpoint resolution) |
| `baseUrl` | Fully qualified endpoint override | No |
| `timeout` | Request timeout (default: 2 minutes) | No |

## Examples

The [examples/](examples/) directory contains standalone Java files showing how to use the OCI-signed `OkHttpClient` with different vendor SDKs:

| Example | Description |
|---------|-------------|
| [examples/anthropic/](examples/anthropic/) | Anthropic Claude via the `anthropic-java` SDK |
| [examples/openai/](examples/openai/) | OpenAI-compatible models via the `openai-java` SDK |
| [examples/gemini-direct-http/](examples/gemini-direct-http/) | Google Gemini via direct OkHttp POST (no vendor SDK) |

These examples are **not** compiled as part of the Maven build. Copy them into your own project.

## Module Reference

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `oci-genai-auth-java-bom` | `com.oracle.genai:oci-genai-auth-java-bom` | Pins dependency versions |
| `oci-genai-auth-java-core` | `com.oracle.genai:oci-genai-auth-java-core` | OCI IAM auth, request signing, header injection, endpoint resolution |

## Building from Source

```bash
# Compile
mvn clean compile

# Run tests (27 tests)
mvn test

# Full verification
mvn clean verify

# Install to local Maven repository
mvn install -DskipTests

# Confirm no vendor SDK dependencies
mvn dependency:tree -pl oci-genai-auth-java-core
```

## Design Notes

- **Token refresh** is handled by OCI Java SDK auth providers (`SessionTokenAuthenticationDetailsProvider`, etc.) — no custom refresh logic needed.
- **Spec/codegen** is a separate follow-up track. This library provides auth utilities only.
- **Gemini example** uses direct HTTP because the Google Gemini Java SDK does not currently support transport injection.

## License

Copyright (c) 2026 Oracle and/or its affiliates.

Released under the [Universal Permissive License v1.0](https://oss.oracle.com/licenses/upl/).
