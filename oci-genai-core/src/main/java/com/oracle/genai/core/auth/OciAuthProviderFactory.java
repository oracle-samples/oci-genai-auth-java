/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.core.auth;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;

import java.io.IOException;

/**
 * Factory that creates OCI {@link BasicAuthenticationDetailsProvider} instances
 * based on the requested authentication type.
 *
 * <p>Supported auth types:
 * <ul>
 *   <li>{@code oci_config} — User principal from {@code ~/.oci/config}</li>
 *   <li>{@code security_token} — Session token from OCI CLI session</li>
 *   <li>{@code instance_principal} — For OCI Compute instances</li>
 *   <li>{@code resource_principal} — For OCI Functions, Container Instances, etc.</li>
 * </ul>
 */
public final class OciAuthProviderFactory {

    public static final String AUTH_TYPE_OCI_CONFIG = "oci_config";
    public static final String AUTH_TYPE_SECURITY_TOKEN = "security_token";
    public static final String AUTH_TYPE_INSTANCE_PRINCIPAL = "instance_principal";
    public static final String AUTH_TYPE_RESOURCE_PRINCIPAL = "resource_principal";

    private static final String DEFAULT_PROFILE = "DEFAULT";

    private OciAuthProviderFactory() {
        // utility class
    }

    /**
     * Creates an authentication provider for the given auth type.
     *
     * @param authType the OCI authentication type
     * @param profile  the OCI config profile name (used for oci_config and security_token)
     * @return a configured {@link BasicAuthenticationDetailsProvider}
     * @throws IllegalArgumentException if authType is not recognized
     * @throws OciAuthException         if the provider cannot be created
     */
    public static BasicAuthenticationDetailsProvider create(String authType, String profile) {
        if (authType == null || authType.isBlank()) {
            throw new IllegalArgumentException("authType must not be null or blank");
        }

        String resolvedProfile = (profile == null || profile.isBlank()) ? DEFAULT_PROFILE : profile;

        return switch (authType) {
            case AUTH_TYPE_OCI_CONFIG -> createConfigProvider(resolvedProfile);
            case AUTH_TYPE_SECURITY_TOKEN -> createSessionTokenProvider(resolvedProfile);
            case AUTH_TYPE_INSTANCE_PRINCIPAL -> createInstancePrincipalProvider();
            case AUTH_TYPE_RESOURCE_PRINCIPAL -> createResourcePrincipalProvider();
            default -> throw new IllegalArgumentException(
                    "Unsupported authType: '" + authType + "'. " +
                    "Supported values: oci_config, security_token, instance_principal, resource_principal");
        };
    }

    /**
     * Creates an authentication provider for the given auth type using the DEFAULT profile.
     */
    public static BasicAuthenticationDetailsProvider create(String authType) {
        return create(authType, DEFAULT_PROFILE);
    }

    private static BasicAuthenticationDetailsProvider createConfigProvider(String profile) {
        try {
            return new ConfigFileAuthenticationDetailsProvider(profile);
        } catch (IOException e) {
            throw new OciAuthException(
                    "Failed to create OCI config auth provider for profile '" + profile + "'. " +
                    "Ensure ~/.oci/config exists and the profile is valid.", e);
        }
    }

    private static BasicAuthenticationDetailsProvider createSessionTokenProvider(String profile) {
        try {
            return new SessionTokenAuthenticationDetailsProvider(profile);
        } catch (IOException e) {
            throw new OciAuthException(
                    "Failed to create session token auth provider for profile '" + profile + "'. " +
                    "Run 'oci session authenticate' to create a session.", e);
        }
    }

    private static BasicAuthenticationDetailsProvider createInstancePrincipalProvider() {
        try {
            return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        } catch (Exception e) {
            throw new OciAuthException(
                    "Failed to create instance principal auth provider. " +
                    "Ensure this code is running on an OCI Compute instance with a configured dynamic group.", e);
        }
    }

    private static BasicAuthenticationDetailsProvider createResourcePrincipalProvider() {
        try {
            return ResourcePrincipalAuthenticationDetailsProvider.builder().build();
        } catch (Exception e) {
            throw new OciAuthException(
                    "Failed to create resource principal auth provider. " +
                    "Ensure this code is running in an OCI Function or Container Instance with RP configured.", e);
        }
    }
}
