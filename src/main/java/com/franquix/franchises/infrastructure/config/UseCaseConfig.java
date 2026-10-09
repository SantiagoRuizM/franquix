package com.franquix.franchises.infrastructure.config;

import com.franquix.franchises.application.usecase.BranchService;
import com.franquix.franchises.application.usecase.FranchiseService;
import com.franquix.franchises.application.usecase.ProductService;
import com.franquix.franchises.domain.port.in.BranchUseCase;
import com.franquix.franchises.domain.port.in.FranchiseUseCase;
import com.franquix.franchises.domain.port.in.ProductUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the framework-free use cases as Spring beans, so the application layer carries no
 * Spring annotations.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FranchiseUseCase franchiseUseCase(
            FranchiseRepositoryPort franchises,
            BranchRepositoryPort branches,
            ProductRepositoryPort products,
            Clock clock) {
        return new FranchiseService(franchises, branches, products, clock);
    }

    @Bean
    BranchUseCase branchUseCase(FranchiseRepositoryPort franchises, BranchRepositoryPort branches, Clock clock) {
        return new BranchService(franchises, branches, clock);
    }

    @Bean
    ProductUseCase productUseCase(
            FranchiseRepositoryPort franchises,
            BranchRepositoryPort branches,
            ProductRepositoryPort products,
            Clock clock) {
        return new ProductService(franchises, branches, products, clock);
    }
}
