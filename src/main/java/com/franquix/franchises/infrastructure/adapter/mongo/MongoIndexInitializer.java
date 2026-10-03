package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.RefreshTokenDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.UserDocument;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Creates the indexes the API relies on. Index creation is idempotent, so it runs on every start
 * in every environment instead of relying on {@code auto-index-creation} (disabled by default and
 * discouraged in production). The unique indexes are what actually enforce the name rules.
 *
 * <p>Names are unique <em>case-insensitively</em>: the unique indexes use a strength-2 collation, so
 * "Main" and "main" collide while "Cafe" and "Café" stay distinct.
 *
 * <p>This is the only blocking call of the application and it happens once, on the main thread,
 * before the HTTP server accepts traffic - never on an event-loop thread.
 */
@Component
public class MongoIndexInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    /** Case-insensitive, accent-sensitive comparison. */
    static final Collation CASE_INSENSITIVE = Collation.of("en").strength(Collation.ComparisonLevel.secondary());

    /** Case-sensitive unique indexes from 1.0.0, replaced by the {@code *_ci} ones. */
    private static final List<LegacyIndex> LEGACY_INDEXES = List.of(
            new LegacyIndex(FranchiseDocument.COLLECTION, "ux_franchise_name"),
            new LegacyIndex(BranchDocument.COLLECTION, "ux_branch_franchise_name"),
            new LegacyIndex(ProductDocument.COLLECTION, "ux_product_branch_name"));

    private final ReactiveMongoTemplate template;

    public MongoIndexInitializer(ReactiveMongoTemplate template) {
        this.template = template;
    }

    @Override
    public void afterPropertiesSet() {
        dropLegacyIndexes().then(createIndexes()).block(TIMEOUT);
    }

    Mono<Void> createIndexes() {
        return Flux.concat(
                        create(FranchiseDocument.COLLECTION, new Index()
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .collation(CASE_INSENSITIVE)
                                .named("ux_franchise_name_ci")),
                        create(BranchDocument.COLLECTION, new Index()
                                .on("franchiseId", Sort.Direction.ASC)
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .collation(CASE_INSENSITIVE)
                                .named("ux_branch_franchise_name_ci")),
                        create(ProductDocument.COLLECTION, new Index()
                                .on("branchId", Sort.Direction.ASC)
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .collation(CASE_INSENSITIVE)
                                .named("ux_product_branch_name_ci")),
                        create(ProductDocument.COLLECTION, new Index()
                                .on("branchId", Sort.Direction.ASC)
                                .on("stock", Sort.Direction.DESC)
                                .on("name", Sort.Direction.ASC)
                                .named("ix_product_branch_top_stock")),
                        create(FranchiseDocument.COLLECTION, new Index()
                                .on("name", Sort.Direction.ASC)
                                .on("_id", Sort.Direction.ASC)
                                .named("ix_franchise_name_id")),
                        create(UserDocument.COLLECTION, new Index()
                                .on("email", Sort.Direction.ASC)
                                .unique()
                                .named("ux_user_email")),
                        create(RefreshTokenDocument.COLLECTION, new Index()
                                .on("expiresAt", Sort.Direction.ASC)
                                .expire(Duration.ZERO)
                                .named("ttl_refresh_token_expires_at")),
                        create(RefreshTokenDocument.COLLECTION, new Index()
                                .on("userId", Sort.Direction.ASC)
                                .on("revokedAt", Sort.Direction.ASC)
                                .named("ix_refresh_token_user")))
                .doOnNext(name -> log.info("MongoDB index ready: {}", name))
                .then();
    }

    Mono<Void> dropLegacyIndexes() {
        return Flux.fromIterable(LEGACY_INDEXES)
                .concatMap(legacy -> template.indexOps(legacy.collection()).getIndexInfo()
                        .map(IndexInfo::getName)
                        .filter(legacy.name()::equals)
                        .concatMap(name -> template.indexOps(legacy.collection()).dropIndex(name)
                                .doOnSuccess(ignored -> log.info("Dropped legacy MongoDB index {}", name))))
                .then();
    }

    private Mono<String> create(String collection, Index index) {
        return template.indexOps(collection).createIndex(index);
    }

    private record LegacyIndex(String collection, String name) {
    }
}
