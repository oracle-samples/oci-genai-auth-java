/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OciOkHttpClientFactoryTest {

    private MockWebServer server;
    private BasicAuthenticationDetailsProvider authProvider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(
                        keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        authProvider = mock(BasicAuthenticationDetailsProvider.class);
        when(authProvider.getKeyId()).thenReturn(
                "ocid1.tenancy.oc1..test/ocid1.user.oc1..test/aa:bb:cc:dd");
        when(authProvider.getPrivateKey()).thenAnswer(
                inv -> new ByteArrayInputStream(pem.getBytes()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void create_installsInterceptors() {
        OkHttpClient client = OciOkHttpClientFactory.create(
                authProvider, "ocid1.compartment.oc1..test");

        List<Interceptor> interceptors = client.interceptors();
        assertTrue(interceptors.stream().anyMatch(i -> i instanceof OciHeaderInterceptor),
                "Should contain OciHeaderInterceptor");
        assertTrue(interceptors.stream().anyMatch(i -> i instanceof OciSigningInterceptor),
                "Should contain OciSigningInterceptor");
    }

    @Test
    void create_requestGoesThoughMockServer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        OkHttpClient client = OciOkHttpClientFactory.create(
                authProvider, "ocid1.compartment.oc1..test");

        var response = client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute();

        assertEquals(200, response.code());
        response.close();

        RecordedRequest request = server.takeRequest();
        assertEquals("ocid1.compartment.oc1..test", request.getHeader("CompartmentId"));
        assertNotNull(request.getHeader("Authorization"));
    }

    @Test
    void create_setsCustomTimeout() {
        OkHttpClient client = OciOkHttpClientFactory.create(
                authProvider, null, null, Duration.ofSeconds(30), false);

        assertEquals(30_000, client.connectTimeoutMillis());
        assertEquals(30_000, client.readTimeoutMillis());
        assertEquals(30_000, client.writeTimeoutMillis());
    }

    @Test
    void create_usesDefaultTimeoutWhenNull() {
        OkHttpClient client = OciOkHttpClientFactory.create(
                authProvider, null, null, null, false);

        assertEquals(120_000, client.connectTimeoutMillis());
        assertEquals(120_000, client.readTimeoutMillis());
        assertEquals(120_000, client.writeTimeoutMillis());
    }

    @Test
    void create_throwsOnNullAuthProvider() {
        assertThrows(NullPointerException.class, () ->
                OciOkHttpClientFactory.create(null, null));
    }
}
