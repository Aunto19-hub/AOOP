package com.seamline.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seamline.config.SeamlineProperties;
import com.seamline.exception.InvalidCredentialsException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Verifies a Google Identity Services credential by asking Google itself, via the
 * {@code tokeninfo} endpoint. That endpoint checks the token's signature, issuer and
 * expiry for us; we only need to confirm it was issued for this app and that Google
 * has verified the email on it.
 */
@Component
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final SeamlineProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GoogleTokenVerifier(SeamlineProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public GoogleIdentity verify(String credential) {
        String clientId = properties.google() == null ? null : properties.google().clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidCredentialsException("Google sign-in is not configured on this server");
        }

        JsonNode payload = fetchTokenInfo(credential);

        if (!clientId.equals(payload.path("aud").asText())) {
            throw new InvalidCredentialsException("That Google credential was not issued for this app");
        }
        if (!"true".equals(payload.path("email_verified").asText())) {
            throw new InvalidCredentialsException("Google has not verified that email address");
        }
        String email = payload.path("email").asText(null);
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException("Google did not share an email address");
        }

        return new GoogleIdentity(email, payload.path("name").asText(null));
    }

    private JsonNode fetchTokenInfo(String credential) {
        String encoded = java.net.URLEncoder.encode(credential, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKENINFO_URL + encoded))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new InvalidCredentialsException("Google rejected that sign-in attempt");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new InvalidCredentialsException("Could not reach Google to verify sign-in");
        }
    }
}
