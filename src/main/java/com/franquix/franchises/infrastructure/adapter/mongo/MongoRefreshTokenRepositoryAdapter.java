package com.franquix.franchises.infrastructure.adapter.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.franquix.franchises.domain.model.auth.RefreshToken;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.document.RefreshTokenDocument;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MongoRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private static final String REVOKED_AT = "revokedAt";

    private final ReactiveMongoTemplate template;

    public MongoRefreshTokenRepositoryAdapter(ReactiveMongoTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<RefreshToken> create(RefreshToken token) {
        return template.insert(toDocument(token)).map(MongoRefreshTokenRepositoryAdapter::toDomain);
    }

    /** One findAndModify: only the first of two concurrent refreshes with the same token wins. */
    @Override
    public Mono<RefreshToken> consume(String tokenHash, Instant now) {
        Query usable = Query.query(where("_id").is(tokenHash)
                .and(REVOKED_AT).is(null)
                .and("expiresAt").gt(now));
        return template.findAndModify(usable, new Update().set(REVOKED_AT, now),
                        FindAndModifyOptions.options().returnNew(false).upsert(false), RefreshTokenDocument.class)
                .map(MongoRefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public Mono<RefreshToken> findByHash(String tokenHash) {
        return template.findById(tokenHash, RefreshTokenDocument.class).map(MongoRefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public Mono<Void> revoke(String tokenHash, String userId, Instant now) {
        if (!ObjectIds.allValid(userId)) {
            return Mono.empty();
        }
        Query owned = Query.query(where("_id").is(tokenHash).and("userId").is(new ObjectId(userId)).and(REVOKED_AT).is(null));
        return template.updateFirst(owned, new Update().set(REVOKED_AT, now), RefreshTokenDocument.class).then();
    }

    @Override
    public Mono<Void> revokeAllForUser(String userId, Instant now) {
        if (!ObjectIds.allValid(userId)) {
            return Mono.empty();
        }
        Query active = Query.query(where("userId").is(new ObjectId(userId)).and(REVOKED_AT).is(null));
        return template.updateMulti(active, new Update().set(REVOKED_AT, now), RefreshTokenDocument.class).then();
    }

    private static RefreshTokenDocument toDocument(RefreshToken token) {
        return new RefreshTokenDocument(
                token.tokenHash(), token.userId(), token.issuedAt(), token.expiresAt(), token.revokedAt());
    }

    private static RefreshToken toDomain(RefreshTokenDocument document) {
        return new RefreshToken(document.tokenHash(), document.userId(), document.issuedAt(),
                document.expiresAt(), document.revokedAt());
    }
}
