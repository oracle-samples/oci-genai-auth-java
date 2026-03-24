# Migration Guide: `oci-genai-openai` to `oci-genai-auth-java`

This guide helps existing `oci-genai-openai` users migrate to `oci-genai-auth-java`.

## Summary

- Replace dependency `oci-genai-openai` with `oci-genai-auth-java-core`.
- Continue using the `openai-java` SDK client.
- Use `OciOkHttpClientFactory` to build a signed OkHttpClient.
- Choose endpoint/config based on API mode:
  - AgentHub: `https://inference.generativeai.<region>.oci.oraclecloud.com/openai/v1` + `openai-project` header
  - Partner : `https://inference.generativeai.<region>.oci.oraclecloud.com/20231130/actions/v1` + `compartmentId`

## 1) Dependency Changes

Replace the old dependency with the new one in your `pom.xml`:

```xml
<!-- Remove old dependency -->
<!-- <artifactId>oci-genai-openai</artifactId> -->

<!-- Add new dependency -->
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

## 2) Import Changes

```java
// Old
import com.oracle.genai.openai.OciOpenAI;

// New
import com.oracle.genai.auth.OciAuthConfig;
import com.oracle.genai.auth.OciOkHttpClientFactory;
```

## 3) Client Initialization Changes

### AgentHub

Use the OpenAI-compatible endpoint and provide project OCID:

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
        .baseUrl("https://inference.generativeai.<region>.oci.oraclecloud.com/openai/v1")
        .okHttpClient(ociHttpClient)
        .apiKey("not-used")
        .addHeader("openai-project", "<ocid1.generativeaiproject...>")
        .build();
```

### Partner APIs

Use `/20231130/actions/v1` and include compartment ID:

```java
OciAuthConfig config = OciAuthConfig.builder()
        .authType("security_token")
        .profile("DEFAULT")
        .compartmentId("<ocid1.compartment...>")
        .build();

OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);

OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl("https://inference.generativeai.<region>.oci.oraclecloud.com/20231130/actions/v1")
        .okHttpClient(ociHttpClient)
        .apiKey("not-used")
        .build();
```

## 4) Endpoint and required parameters

- AgentHub:
  - `baseUrl`: `https://inference.generativeai.<region>.oci.oraclecloud.com/openai/v1`
  - required: `openai-project` header with project OCID
- Partner:
  - `baseUrl`: `https://inference.generativeai.<region>.oci.oraclecloud.com/20231130/actions/v1`
  - required: `compartmentId` in `OciAuthConfig`
