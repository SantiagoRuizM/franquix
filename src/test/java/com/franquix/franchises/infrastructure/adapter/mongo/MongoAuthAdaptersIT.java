package com.franquix.franchises.infrastructure.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.auth.RefreshToken;
import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.infrastructure.adapter.mongo.document.RefreshTokenDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.UserDocument;
import com.franquix.franchises.support.MongoTestcontainersConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataMongoTest
@Import({
        MongoTestcontainersConfiguration.class,
        MongoIndexInitializer.class,
        MongoUserRepositoryAdapter.class,
        MongoRefreshTokenRepositoryAdapter.class
})
class MongoAuthAdaptersIT {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant LATER = NOW.plus(Duration.ofDays(1));

    @Autowired
    private ReactiveMongoTemplate template;
    @Autowired
    private MongoUserRepositoryAdapter users;
    @Autowired
    private MongoRefreshTokenRepositoryAdapter tokens;

    @BeforeEach
    void clean() {
        Flux.just(UserDocument.COLLECTION, RefreshTokenDocument.COLLECTION)
                .concatMap(collection -> template.remove(new Query(), collection))
                .blockLast();
    }

    private User saveUser(String email) {
        return users.create(User.create(email, "Someone", "hash", Role.VIEWER, NOW)).block();
    }

    @Nested
    class Users {

        @Test
        void createsAndFindsByEmailAndId() {
            User saved = saveUser("viewer@franquix.local");

            assertThat(ObjectId.isValid(saved.id())).isTrue();
            StepVerifier.create(users.findByEmail("viewer@franquix.local")).expectNext(saved).verifyComplete();
            StepVerifier.create(users.findById(saved.id())).expectNext(saved).verifyComplete();
            StepVerifier.create(users.findById("bad")).verifyComplete();
        }

        @Test
        void emailsAreUnique() {
            saveUser("viewer@franquix.local");

            StepVerifier.create(users.create(User.create("VIEWER@franquix.local", "Other", "hash", Role.ADMIN, NOW)))
                    .expectErrorMessage("A user with email 'viewer@franquix.local' already exists")
                    .verify();
        }

        @Test
        void updatesOnlyThePasswordHash() {
            User saved = saveUser("viewer@franquix.local");

            StepVerifier.create(users.updatePasswordHash(saved.id(), "new-hash", LATER))
                    .assertNext(updated -> {
                        assertThat(updated.passwordHash()).isEqualTo("new-hash");
                        assertThat(updated.email()).isEqualTo(saved.email());
                        assertThat(updated.updatedAt()).isEqualTo(LATER);
                    })
                    .verifyComplete();
            StepVerifier.create(users.updatePasswordHash(new ObjectId().toHexString(), "x", LATER)).verifyComplete();
            StepVerifier.create(users.updatePasswordHash("bad", "x", LATER)).verifyComplete();
        }
    }

    @Nested
    class RefreshTokens {

        private String userId;

        @BeforeEach
        void setUp() {
            userId = saveUser("viewer@franquix.local").id();
        }

        private RefreshToken store(String hash, Instant expiresAt) {
            return tokens.create(RefreshToken.issue(hash, userId, NOW, expiresAt)).block();
        }

        @Test
        void aTokenCanBeConsumedOnlyOnce() {
            store("h1", LATER);

            StepVerifier.create(tokens.consume("h1", NOW)).expectNextCount(1).verifyComplete();
            StepVerifier.create(tokens.consume("h1", NOW)).verifyComplete();
            StepVerifier.create(tokens.findByHash("h1"))
                    .assertNext(token -> assertThat(token.revoked()).isTrue())
                    .verifyComplete();
        }

        @Test
        void concurrentConsumesHaveExactlyOneWinner() {
            store("h1", LATER);

            // 20 in-flight findAndModify calls on the same token; the database serializes them
            StepVerifier.create(Flux.range(0, 20)
                            .flatMap(i -> tokens.consume("h1", NOW), 20)
                            .count())
                    .expectNext(1L)
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));
        }

        @Test
        void expiredTokensCannotBeConsumed() {
            store("h1", NOW.minusSeconds(1));

            StepVerifier.create(tokens.consume("h1", NOW)).verifyComplete();
        }

        @Test
        void revokeOnlyAffectsTheOwner() {
            store("h1", LATER);
            String intruder = saveUser("intruder@franquix.local").id();

            tokens.revoke("h1", intruder, NOW).block();
            StepVerifier.create(tokens.findByHash("h1"))
                    .assertNext(token -> assertThat(token.revoked()).isFalse())
                    .verifyComplete();

            tokens.revoke("h1", userId, NOW).block();
            StepVerifier.create(tokens.findByHash("h1"))
                    .assertNext(token -> assertThat(token.revoked()).isTrue())
                    .verifyComplete();
            StepVerifier.create(tokens.revoke("h1", "bad", NOW)).verifyComplete();
        }

        @Test
        void revokeAllEndsEverySessionOfTheUserOnly() {
            store("h1", LATER);
            store("h2", LATER);
            String otherUser = saveUser("other@franquix.local").id();
            tokens.create(RefreshToken.issue("h3", otherUser, NOW, LATER)).block();

            tokens.revokeAllForUser(userId, NOW).block();

            StepVerifier.create(Mono.zip(tokens.findByHash("h1"), tokens.findByHash("h2"), tokens.findByHash("h3")))
                    .assertNext(found -> {
                        assertThat(found.getT1().revoked()).isTrue();
                        assertThat(found.getT2().revoked()).isTrue();
                        assertThat(found.getT3().revoked()).isFalse();
                    })
                    .verifyComplete();
            StepVerifier.create(tokens.revokeAllForUser("bad", NOW)).verifyComplete();
        }

        @Test
        void mongoPurgesExpiredTokensThroughATtlIndex() {
            StepVerifier.create(template.indexOps(RefreshTokenDocument.COLLECTION).getIndexInfo()
                            .filter(index -> index.getName().equals("ttl_refresh_token_expires_at"))
                            .map(IndexInfo::getExpireAfter))
                    .assertNext(expireAfter -> assertThat(expireAfter).contains(Duration.ZERO))
                    .verifyComplete();
        }
    }
}
