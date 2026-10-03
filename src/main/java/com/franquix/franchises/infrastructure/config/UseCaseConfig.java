package com.franquix.franchises.infrastructure.config;

import com.franquix.franchises.application.usecase.AddBranchService;
import com.franquix.franchises.application.usecase.AddProductService;
import com.franquix.franchises.application.usecase.CreateFranchiseService;
import com.franquix.franchises.application.usecase.DeleteProductService;
import com.franquix.franchises.application.usecase.GetFranchiseService;
import com.franquix.franchises.application.usecase.GetTopStockProductsService;
import com.franquix.franchises.application.usecase.HierarchyResolver;
import com.franquix.franchises.application.usecase.ListFranchisesService;
import com.franquix.franchises.application.usecase.RenameBranchService;
import com.franquix.franchises.application.usecase.RenameFranchiseService;
import com.franquix.franchises.application.usecase.RenameProductService;
import com.franquix.franchises.application.usecase.UpdateProductStockService;
import com.franquix.franchises.domain.port.in.AddBranchUseCase;
import com.franquix.franchises.domain.port.in.AddProductUseCase;
import com.franquix.franchises.domain.port.in.CreateFranchiseUseCase;
import com.franquix.franchises.domain.port.in.DeleteProductUseCase;
import com.franquix.franchises.domain.port.in.GetFranchiseUseCase;
import com.franquix.franchises.domain.port.in.GetTopStockProductsUseCase;
import com.franquix.franchises.domain.port.in.ListFranchisesUseCase;
import com.franquix.franchises.domain.port.in.RenameBranchUseCase;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
import com.franquix.franchises.domain.port.in.RenameProductUseCase;
import com.franquix.franchises.domain.port.in.UpdateProductStockUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the framework-free use cases as Spring beans, so the application layer carries no
 * Spring annotations.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    /** Millisecond ticks: MongoDB stores millisecond precision, so responses match what is persisted. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofMillis(1));
    }

    @Bean
    HierarchyResolver hierarchyResolver(FranchiseRepositoryPort franchises, BranchRepositoryPort branches) {
        return new HierarchyResolver(franchises, branches);
    }

    @Bean
    CreateFranchiseUseCase createFranchiseUseCase(FranchiseRepositoryPort franchises, Clock clock) {
        return new CreateFranchiseService(franchises, clock);
    }

    @Bean
    ListFranchisesUseCase listFranchisesUseCase(FranchiseRepositoryPort franchises) {
        return new ListFranchisesService(franchises);
    }

    @Bean
    GetFranchiseUseCase getFranchiseUseCase(
            HierarchyResolver hierarchy, BranchRepositoryPort branches, ProductRepositoryPort products) {
        return new GetFranchiseService(hierarchy, branches, products);
    }

    @Bean
    RenameFranchiseUseCase renameFranchiseUseCase(FranchiseRepositoryPort franchises, Clock clock) {
        return new RenameFranchiseService(franchises, clock);
    }

    @Bean
    AddBranchUseCase addBranchUseCase(HierarchyResolver hierarchy, BranchRepositoryPort branches, Clock clock) {
        return new AddBranchService(hierarchy, branches, clock);
    }

    @Bean
    RenameBranchUseCase renameBranchUseCase(HierarchyResolver hierarchy, BranchRepositoryPort branches, Clock clock) {
        return new RenameBranchService(hierarchy, branches, clock);
    }

    @Bean
    AddProductUseCase addProductUseCase(HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        return new AddProductService(hierarchy, products, clock);
    }

    @Bean
    DeleteProductUseCase deleteProductUseCase(HierarchyResolver hierarchy, ProductRepositoryPort products) {
        return new DeleteProductService(hierarchy, products);
    }

    @Bean
    UpdateProductStockUseCase updateProductStockUseCase(
            HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        return new UpdateProductStockService(hierarchy, products, clock);
    }

    @Bean
    RenameProductUseCase renameProductUseCase(
            HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        return new RenameProductService(hierarchy, products, clock);
    }

    @Bean
    GetTopStockProductsUseCase getTopStockProductsUseCase(
            HierarchyResolver hierarchy, BranchRepositoryPort branches, ProductRepositoryPort products) {
        return new GetTopStockProductsService(hierarchy, branches, products);
    }
}
