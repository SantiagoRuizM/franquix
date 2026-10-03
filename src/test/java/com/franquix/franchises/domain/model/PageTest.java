package com.franquix.franchises.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageTest {

    @ParameterizedTest(name = "page={0} size={1}")
    @CsvSource({"0, 1", "0, 100", "5, 20", "2147483647, 100"})
    void acceptsValidPageQueries(int page, int size) {
        assertThat(new PageQuery(page, size).offset()).isEqualTo((long) page * size);
    }

    @Test
    void offsetDoesNotOverflowForHugePages() {
        assertThat(new PageQuery(Integer.MAX_VALUE, 100).offset()).isPositive();
    }

    @Test
    void rejectsNegativePages() {
        assertThatThrownBy(() -> new PageQuery(-1, 20))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("page must be greater than or equal to 0");
    }

    @ParameterizedTest(name = "size={0}")
    @CsvSource({"0", "-5", "101"})
    void rejectsSizesOutsideTheBounds(int size) {
        assertThatThrownBy(() -> new PageQuery(0, size))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @ParameterizedTest(name = "{0} items, size {1} -> {2} pages")
    @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "100, 10, 10"})
    void computesTotalPages(long total, int size, long pages) {
        assertThat(new PageResult<>(List.of(), 0, size, total).totalPages()).isEqualTo(pages);
    }

    @Test
    void mapKeepsPagingMetadataAndCopiesItems() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2), 3, 2, 10);

        assertThat(page.map(String::valueOf))
                .isEqualTo(new PageResult<>(List.of("1", "2"), 3, 2, 10));
    }
}
