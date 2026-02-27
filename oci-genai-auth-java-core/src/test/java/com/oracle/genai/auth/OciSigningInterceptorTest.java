/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OciSigningInterceptorTest {

    private MockWebServer server;
    private OkHttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        // Generate a real RSA key pair for OCI SDK signing
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // PEM-encode the private key (PKCS#8 format) so getPrivateKey() returns an InputStream
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(
                        keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        // Mock the OCI auth provider with real key material
        BasicAuthenticationDetailsProvider authProvider =
                mock(BasicAuthenticationDetailsProvider.class);
        when(authProvider.getKeyId()).thenReturn(
                "ocid1.tenancy.oc1..test/ocid1.user.oc1..test/aa:bb:cc:dd");
        when(authProvider.getPrivateKey()).thenAnswer(
                inv -> new ByteArrayInputStream(pem.getBytes()));

        client = new OkHttpClient.Builder()
                .addInterceptor(new OciSigningInterceptor(authProvider))
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void addsAuthorizationHeader() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        String auth = request.getHeader("Authorization");
        assertNotNull(auth, "Authorization header should be present");
        assertTrue(auth.startsWith("Signature"), "Should be OCI signature format");
    }

    @Test
    void addsDateHeader() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertNotNull(request.getHeader("date"), "date header should be present");
    }

    @Test
    void signsPostBodyWithContentHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        RequestBody body = RequestBody.create(
                "{\"message\":\"hello\"}", MediaType.parse("application/json"));

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .post(body)
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertNotNull(request.getHeader("Authorization"), "Authorization should be present");
        assertNotNull(request.getHeader("x-content-sha256"), "x-content-sha256 should be present for POST");
        assertEquals("{\"message\":\"hello\"}", request.getBody().readUtf8(),
                "Body should be preserved after signing");
    }

    @Test
    void preservesOriginalHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .header("X-Custom-Header", "custom-value")
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertEquals("custom-value", request.getHeader("X-Custom-Header"));
        assertNotNull(request.getHeader("Authorization"));
    }

    @Test
    void throwsOnNullAuthProvider() {
        assertThrows(NullPointerException.class, () ->
                new OciSigningInterceptor(null));
    }
}
