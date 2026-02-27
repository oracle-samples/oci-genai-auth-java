/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

/**
 * Thrown when an OCI authentication provider cannot be created or
 * when request signing fails.
 */
public class OciAuthException extends RuntimeException {

    public OciAuthException(String message) {
        super(message);
    }

    public OciAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
