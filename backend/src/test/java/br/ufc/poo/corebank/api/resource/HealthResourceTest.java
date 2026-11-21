package br.ufc.poo.corebank.api.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class HealthResourceTest {

    @Test
    void deveRetornarStatusUpDeFormaReativa() {
        given()
          .when().get("/api/v1/health")
          .then()
             .statusCode(200)
             .body("service", is("core-bank-service"))
             .body("status", is("UP"))
             .body("reactiveEngine", is("SmallRye Mutiny"))
             .body("timestamp", notNullValue());
    }
}
