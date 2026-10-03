package com.franquix.franchises.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DomainRulesTest {

    @Test
    void requireValidNameTrimsSurroundingWhitespace() {
        assertThat(DomainRules.requireValidName("name", "  Coffee House  ")).isEqualTo("Coffee House");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "  \n "})
    void requireValidNameRejectsBlankValues(String value) {
        assertThatThrownBy(() -> DomainRules.requireValidName("name", value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void requireValidNameAcceptsExactlyTheMaximumLength() {
        String name = "x".repeat(DomainRules.NAME_MAX_LENGTH);

        assertThat(DomainRules.requireValidName("name", name)).hasSize(DomainRules.NAME_MAX_LENGTH);
    }

    @Test
    void requireValidNameRejectsNamesLongerThanTheMaximum() {
        String name = "x".repeat(DomainRules.NAME_MAX_LENGTH + 1);

        assertThatThrownBy(() -> DomainRules.requireValidName("name", name))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("name must be at most 100 characters");
    }

    @Test
    void lengthIsCheckedAfterTrimming() {
        String name = "  " + "x".repeat(DomainRules.NAME_MAX_LENGTH) + "  ";

        assertThat(DomainRules.requireValidName("name", name)).hasSize(DomainRules.NAME_MAX_LENGTH);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, Integer.MAX_VALUE})
    void requireValidStockAcceptsZeroAndPositiveValues(int stock) {
        assertThat(DomainRules.requireValidStock(stock)).isEqualTo(stock);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void requireValidStockRejectsNegativeValues(int stock) {
        assertThatThrownBy(() -> DomainRules.requireValidStock(stock))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("stock must be greater than or equal to 0");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void requireReferenceRejectsMissingValues(String value) {
        assertThatThrownBy(() -> DomainRules.requireReference("branchId", value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("branchId is required");
    }

    @Test
    void requireReferenceReturnsTheValue() {
        assertThat(DomainRules.requireReference("branchId", "b1")).isEqualTo("b1");
    }
}
