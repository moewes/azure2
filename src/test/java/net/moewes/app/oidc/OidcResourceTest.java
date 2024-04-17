package net.moewes.app.oidc;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;


@QuarkusTest
class OidcResourceTest {

    @Inject
    AuthRequestsBean authRequestsBean;

    @Test
    void authorize_BAD() {
        given()
                .when().get("/auth")
                .then().statusCode(400);
    }

    @Test
    void authorize_OK() {
        given()
                .when().get("/auth?scope=openid&state=123&response_type=code")
                .then().statusCode(200);
    }

    @Test
    void getMetaData() {
    }

    @Test
    void jwks() {
    }

    @Test
    void logout() {

        given()
                .when().get("/logout")
                .then().statusCode(200);
        // TODO
    }

    @Test
    void loginForm() {
    }

    @Test
    void login() {
    }

    @Test
    void revoke() {
    }

    @Test
    void token() {
    }

    @Test
    void userinfo() {
    }
}