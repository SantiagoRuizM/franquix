package com.franquix.franchises.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throw-away MongoDB in Docker and wires {@code spring.data.mongodb.*} to it.
 * Spring's test context cache reuses the container across test classes sharing a context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestcontainersConfiguration {

    public static final String MONGO_IMAGE = "mongo:7.0";

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse(MONGO_IMAGE));
    }
}
