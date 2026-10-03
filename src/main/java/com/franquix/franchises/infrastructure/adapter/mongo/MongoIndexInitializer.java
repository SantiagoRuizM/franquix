package com.franquix.franchises.infrastructure.adapter.mongo;

import com.franquix.franchises.infrastructure.adapter.mongo.document.BranchDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.FranchiseDocument;
import com.franquix.franchises.infrastructure.adapter.mongo.document.ProductDocument;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Creates the indexes the API relies on. Index creation is idempotent, so it runs on every start
 * in every environment instead of relying on {@code auto-index-creation} (disabled by default and
 * discouraged in production). The unique indexes are what actually enforce the name rules.
 *
 * <p>This is the only blocking call of the application and it happens once, on the main thread,
 * before the HTTP server accepts traffic - never on an event-loop thread.
 */
@Component
public class MongoIndexInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ReactiveMongoTemplate template;

    public MongoIndexInitializer(ReactiveMongoTemplate template) {
        this.template = template;
    }

    @Override
    public void afterPropertiesSet() {
        createIndexes().block(TIMEOUT);
    }

    Mono<Void> createIndexes() {
        return Flux.concat(
                        create(FranchiseDocument.COLLECTION, new Index()
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .named("ux_franchise_name")),
                        create(BranchDocument.COLLECTION, new Index()
                                .on("franchiseId", Sort.Direction.ASC)
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .named("ux_branch_franchise_name")),
                        create(ProductDocument.COLLECTION, new Index()
                                .on("branchId", Sort.Direction.ASC)
                                .on("name", Sort.Direction.ASC)
                                .unique()
                                .named("ux_product_branch_name")),
                        create(ProductDocument.COLLECTION, new Index()
                                .on("branchId", Sort.Direction.ASC)
                                .on("stock", Sort.Direction.DESC)
                                .on("name", Sort.Direction.ASC)
                                .named("ix_product_branch_top_stock")))
                .doOnNext(name -> log.info("MongoDB index ready: {}", name))
                .then();
    }

    private Mono<String> create(String collection, Index index) {
        return template.indexOps(collection).createIndex(index);
    }
}
