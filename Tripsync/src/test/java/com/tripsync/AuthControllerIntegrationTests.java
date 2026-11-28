package com.tripsync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class AuthControllerIntegrationTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void registrationAndLoginReturnAnAccessToken() throws Exception {
        String email = "auth-test@example.com";
        String password = "auth-test-password";
        HttpResponse<String> registration = httpClient.send(
                HttpRequest.newBuilder(uri("/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"fullName\":\"Auth Test User\",\"email\":\"" + email
                                        + "\",\"password\":\"" + password + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, registration.statusCode());

        HttpResponse<String> login = httpClient.send(
                HttpRequest.newBuilder(uri("/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, login.statusCode());
        assertNotNull(login.body());
        assertTrue(login.body().contains("\"accessToken\""));
    }

    @Test
    void backendDoesNotAddCorsHeaders() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(uri("/actuator/health"))
                        .header("Origin", "http://localhost:5173")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertFalse(response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }

    @Test
    void changePasswordRequiresAuthentication() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(uri("/auth/change-password"))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                "{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
