package com.franquix.franchises.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full application over HTTP against MongoDB in Docker. {@link #client} sends the {@code local}
 * profile API token; {@link #http} sends none.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// generous timeout: the first requests of a cold JVM also open the MongoDB connection pool
@AutoConfigureWebTestClient(timeout = "PT30S")
@Import(MongoTestcontainersConfiguration.class)
public abstract class E2ESupport {

    protected static final String API_TOKEN = "local-dev-token";

    @Autowired
    protected WebTestClient http;

    protected WebTestClient client;

    @BeforeEach
    void authenticate() {
        client = http.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + API_TOKEN).build();
    }

    protected static String unique(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
