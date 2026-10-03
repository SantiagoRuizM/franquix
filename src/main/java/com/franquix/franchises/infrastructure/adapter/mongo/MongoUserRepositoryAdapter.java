package com.franquix.franchises.infrastructure.adapter.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import com.franquix.franchises.infrastructure.adapter.mongo.document.UserDocument;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MongoUserRepositoryAdapter implements UserRepositoryPort {

    private final ReactiveMongoTemplate template;

    public MongoUserRepositoryAdapter(ReactiveMongoTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<User> create(User user) {
        return template.insert(toDocument(user))
                .map(MongoUserRepositoryAdapter::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> DuplicateNameException.email(user.email()));
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return template.findOne(Query.query(where("email").is(email)), UserDocument.class)
                .map(MongoUserRepositoryAdapter::toDomain);
    }

    @Override
    public Mono<User> findById(String userId) {
        return ObjectIds.ifValid(userId, id -> template.findById(id, UserDocument.class))
                .map(MongoUserRepositoryAdapter::toDomain);
    }

    @Override
    public Mono<User> updatePasswordHash(String userId, String passwordHash, Instant updatedAt) {
        if (!ObjectIds.allValid(userId)) {
            return Mono.empty();
        }
        return template.findAndModify(AtomicUpdates.byId(userId),
                        new Update().set("passwordHash", passwordHash).set("updatedAt", updatedAt),
                        AtomicUpdates.RETURN_NEW_NO_UPSERT, UserDocument.class)
                .map(MongoUserRepositoryAdapter::toDomain);
    }

    private static UserDocument toDocument(User user) {
        return new UserDocument(user.id(), user.email(), user.name(), user.passwordHash(), user.role().name(),
                user.active(), user.createdAt(), user.updatedAt());
    }

    private static User toDomain(UserDocument document) {
        return new User(document.id(), document.email(), document.name(), document.passwordHash(),
                Role.valueOf(document.role()), document.active(), document.createdAt(), document.updatedAt());
    }
}
