package com.franquix.franchises.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    void notFoundMessagesIdentifyTheMissingResourceAndItsParent() {
        assertThat(NotFoundException.franchise("f1")).hasMessage("Franchise 'f1' not found");
        assertThat(NotFoundException.branch("f1", "b1")).hasMessage("Branch 'b1' not found in franchise 'f1'");
        assertThat(NotFoundException.product("b1", "p1")).hasMessage("Product 'p1' not found in branch 'b1'");
    }

    @Test
    void duplicateNameMessagesDescribeTheUniquenessScope() {
        assertThat(DuplicateNameException.franchise("Acme"))
                .hasMessage("A franchise named 'Acme' already exists");
        assertThat(DuplicateNameException.branch("Downtown"))
                .hasMessage("A branch named 'Downtown' already exists in this franchise");
        assertThat(DuplicateNameException.product("Coffee"))
                .hasMessage("A product named 'Coffee' already exists in this branch");
    }

    @Test
    void everyDomainErrorIsABusinessException() {
        assertThat(new DomainValidationException("invalid")).isInstanceOf(BusinessException.class);
        assertThat(NotFoundException.franchise("f1")).isInstanceOf(BusinessException.class);
        assertThat(DuplicateNameException.franchise("Acme")).isInstanceOf(BusinessException.class);
    }
}
