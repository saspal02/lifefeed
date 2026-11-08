package dev.saswat.lifefeed;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
public abstract class BaseIT {
    @Autowired
    protected RestTestClient restTestClient;

    protected String getAuthToken(String email, String password) {
        record LoginResponse(String accessToken, String name, String email, String role) {}

        LoginResponse response = restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                                    {
                                        "email":"%s",
                                        "password":"%s"
                                    }
                                    """.formatted(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(LoginResponse.class)
                .getResponseBody();
        return response.accessToken();
    }
}
