/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OciHeaderInterceptorTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void injectsCompartmentIdHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OciHeaderInterceptor("ocid1.compartment.oc1..test"))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertEquals("ocid1.compartment.oc1..test", request.getHeader("CompartmentId"));
        assertEquals("ocid1.compartment.oc1..test", request.getHeader("opc-compartment-id"));
    }

    @Test
    void injectsAdditionalHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OciHeaderInterceptor(
                        "ocid1.compartment.oc1..test",
                        Map.of("opc-conversation-store-id", "ocid1.store.oc1..test")))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertEquals("ocid1.compartment.oc1..test", request.getHeader("CompartmentId"));
        assertEquals("ocid1.store.oc1..test", request.getHeader("opc-conversation-store-id"));
    }

    @Test
    void skipsCompartmentHeadersWhenNull() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OciHeaderInterceptor(null))
                .build();

        client.newCall(new Request.Builder()
                .url(server.url("/test"))
                .build()).execute().close();

        RecordedRequest request = server.takeRequest();
        assertNull(request.getHeader("CompartmentId"));
        assertNull(request.getHeader("opc-compartment-id"));
    }
}
