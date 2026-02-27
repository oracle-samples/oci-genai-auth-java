/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import java.time.Duration;

/**
 * Configuration for OCI authentication and endpoint resolution.
 *
 * <p>Use the {@link #builder()} to create a config instance:
 * <pre>{@code
 * OciAuthConfig config = OciAuthConfig.builder()
 *         .authType("security_token")
 *         .profile("DEFAULT")
 *         .compartmentId("ocid1.compartment.oc1..xxx")
 *         .region("us-chicago-1")
 *         .build();
 * }</pre>
 *
 * <p>Supported auth types:
 * <ul>
 *   <li>{@code oci_config} — User principal from {@code ~/.oci/config}</li>
 *   <li>{@code security_token} — Session token from OCI CLI session</li>
 *   <li>{@code instance_principal} — For OCI Compute instances</li>
 *   <li>{@code resource_principal} — For OCI Functions, Container Instances, etc.</li>
 * </ul>
 */
public final class OciAuthConfig {

    private final String authType;
    private final String profile;
    private final String region;
    private final String baseUrl;
    private final String compartmentId;
    private final Duration timeout;

    private OciAuthConfig(Builder builder) {
        this.authType = builder.authType;
        this.profile = builder.profile;
        this.region = builder.region;
        this.baseUrl = builder.baseUrl;
        this.compartmentId = builder.compartmentId;
        this.timeout = builder.timeout;
    }

    public String authType() { return authType; }
    public String profile() { return profile; }
    public String region() { return region; }
    public String baseUrl() { return baseUrl; }
    public String compartmentId() { return compartmentId; }
    public Duration timeout() { return timeout; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String authType;
        private String profile;
        private String region;
        private String baseUrl;
        private String compartmentId;
        private Duration timeout;

        private Builder() {}

        /**
         * Sets the OCI authentication type.
         * One of: {@code oci_config}, {@code security_token},
         * {@code instance_principal}, {@code resource_principal}.
         */
        public Builder authType(String authType) { this.authType = authType; return this; }

        /**
         * Sets the OCI config profile name. Used with {@code oci_config} and
         * {@code security_token} auth types. Defaults to {@code "DEFAULT"}.
         */
        public Builder profile(String profile) { this.profile = profile; return this; }

        /** Sets the OCI region code (e.g., {@code "us-chicago-1"}). */
        public Builder region(String region) { this.region = region; return this; }

        /** Sets the fully qualified base URL (overrides region-based resolution). */
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        /** Sets the OCI compartment OCID. */
        public Builder compartmentId(String compartmentId) { this.compartmentId = compartmentId; return this; }

        /** Sets the request timeout. Defaults to 2 minutes. */
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public OciAuthConfig build() {
            if (authType == null || authType.isBlank()) {
                throw new IllegalArgumentException("authType is required");
            }
            return new OciAuthConfig(this);
        }
    }
}
