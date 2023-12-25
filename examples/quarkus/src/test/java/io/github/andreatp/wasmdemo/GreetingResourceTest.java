package io.github.andreatp.wasmdemo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GreetingResourceTest {

    @Test
    @Disabled
    public void testHelloEndpoint() {
        given().when().get("/hello").then().statusCode(200).body(is("hello"));
    }

    @Test
    @Disabled
    public void testGreetingEndpoint() {
        String uuid = UUID.randomUUID().toString();
        given().pathParam("name", uuid)
                .when()
                .get("/hello/greeting/{name}")
                .then()
                .statusCode(200)
                .body(is("hello " + uuid));
    }
}
