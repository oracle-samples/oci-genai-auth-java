/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OciAuthProviderFactoryTest {

    @Test
    void create_throwsOnNullAuthType() {
        assertThrows(IllegalArgumentException.class, () ->
                OciAuthProviderFactory.create(null));
    }

    @Test
    void create_throwsOnBlankAuthType() {
        assertThrows(IllegalArgumentException.class, () ->
                OciAuthProviderFactory.create("  "));
    }

    @Test
    void create_throwsOnUnsupportedAuthType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                OciAuthProviderFactory.create("unknown_type"));
        assertTrue(ex.getMessage().contains("Unsupported authType"));
        assertTrue(ex.getMessage().contains("unknown_type"));
    }

    @Test
    void create_ociConfigThrowsGracefullyWhenNoConfigFile() {
        assertThrows(OciAuthException.class, () ->
                OciAuthProviderFactory.create("oci_config", "NONEXISTENT_PROFILE_XYZ"));
    }

    @Test
    void create_securityTokenThrowsGracefullyWhenNoSession() {
        assertThrows(OciAuthException.class, () ->
                OciAuthProviderFactory.create("security_token", "NONEXISTENT_PROFILE_XYZ"));
    }
}
