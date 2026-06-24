/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl/
 */
package com.oracle.genai.auth;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live transcription test for OCI IAM signing with multipart audio requests.
 *
 * <p>To run:
 * <pre>
 * oci session authenticate --region eu-frankfurt-1 --profile-name BoatOc1
 * mvn -pl oci-genai-auth-java-core test -Dtest=OpenAITranscriptionIntegrationTest -DliveTranscription=true \
 *   -Doci.genai.transcription.baseUrl=https://inference.generativeai.eu-frankfurt-1.oci.oraclecloud.com/openai/v1 \
 *   -Doci.genai.transcription.model=<endpoint_ocid> \
 *   -Doci.genai.transcription.compartmentId=<compartment_ocid>
 * </pre>
 */
class OpenAITranscriptionIntegrationTest {

    private static final String BASE_URL = System.getProperty("oci.genai.transcription.baseUrl",
            "https://inference.generativeai.eu-frankfurt-1.oci.oraclecloud.com/20231130/actions/v1");
    private static final String PROFILE = System.getProperty("oci.genai.transcription.profile", "BoatOc1");
    private static final String MODEL = System.getProperty("oci.genai.transcription.model", "vllm-model");
    private static final String COMPARTMENT_ID = System.getProperty("oci.genai.transcription.compartmentId", "");

    @Test
    @EnabledIfSystemProperty(named = "liveTranscription", matches = "true")
    void transcription_api_via_oci_auth_library() throws Exception {
        Path audio = Files.createTempFile("oci-transcription-integration-", ".wav");
        writeWav(audio);

        OciAuthConfig.Builder configBuilder = OciAuthConfig.builder()
                .authType("security_token")
                .profile(PROFILE);
        if (!COMPARTMENT_ID.isBlank()) {
            configBuilder.compartmentId(COMPARTMENT_ID);
        }
        OciAuthConfig config = configBuilder.build();
        OkHttpClient ociHttpClient = OciOkHttpClientFactory.build(config);
        OpenAIClient client = new OpenAIClientImpl(ClientOptions.builder()
                .httpClient(OciOpenAIHttpClient.of(ociHttpClient, BASE_URL))
                .baseUrl(BASE_URL)
                .apiKey("OCI_AUTH")
                .build());

        try {
            Object response = client.audio().transcriptions().create(
                    TranscriptionCreateParams.builder()
                            .file(audio)
                            .model(MODEL)
                            .language("en")
                            .build());

            System.out.println("Transcription response: " + response);
            assertNotNull(response, "Transcription response should not be null");
        } finally {
            client.close();
            Files.deleteIfExists(audio);
        }
    }

    private static void writeWav(Path path) throws Exception {
        int sampleRate = 16_000;
        int durationSeconds = 2;
        byte[] pcm = new byte[sampleRate * durationSeconds * 2];
        for (int i = 0; i < sampleRate * durationSeconds; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / sampleRate;
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * 0.25);
            pcm[i * 2] = (byte) (sample & 0xff);
            pcm[i * 2 + 1] = (byte) ((sample >>> 8) & 0xff);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), format, sampleRate * durationSeconds)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, path.toFile());
        }
    }
}
