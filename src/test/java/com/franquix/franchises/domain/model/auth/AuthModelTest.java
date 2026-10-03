package com.franquix.franchises.domain.model.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.exception.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthModelTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Nested
    class Users {

        @Test
        void createNormalizesTheEmailAndStartsActive() {
            User user = User.create("  Admin@Franquix.LOCAL ", "Admin", "hash", Role.ADMIN, NOW);

            assertThat(user.email()).isEqualTo("admin@franquix.local");
            assertThat(user.active()).isTrue();
            assertThat(user.id()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"no-at-sign", "a@b", "two@@signs.com", "spa ce@x.com", "@x.com"})
        void rejectsInvalidEmails(String email) {
            assertThatThrownBy(() -> User.normalizeEmail(email)).isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsOverlongEmails() {
            String email = "a".repeat(250) + "@x.com";

            assertThatThrownBy(() -> User.normalizeEmail(email))
                    .hasMessage("email must be a valid email address");
        }

        @Test
        void requiresARoleAndAHash() {
            assertThatThrownBy(() -> User.create("a@b.co", "A", "hash", null, NOW))
                    .hasMessage("role is required");
            assertThatThrownBy(() -> User.create("a@b.co", "A", " ", Role.VIEWER, NOW))
                    .hasMessage("passwordHash is required");
        }
    }

    @Nested
    class Passwords {

        @ParameterizedTest
        @ValueSource(strings = {"correct horse battery", "Tr0ub4dor&3-extended", "ñandú-café-2026!"})
        void acceptsStrongPasswords(String password) {
            assertThatCode(() -> PasswordPolicy.validate(password, "admin@franquix.local")).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlank(String password) {
            assertThatThrownBy(() -> PasswordPolicy.validate(password, "a@b.co"))
                    .hasMessage("password must not be blank");
        }

        @Test
        void rejectsShortPasswords() {
            assertThatThrownBy(() -> PasswordPolicy.validate("short-pw-11", "a@b.co"))
                    .hasMessage("password must be at least 12 characters");
        }

        @Test
        void countsCharactersNotBytesForTheMinimum() {
            assertThatCode(() -> PasswordPolicy.validate("ññññññññññññ", "a@b.co")).doesNotThrowAnyException();
        }

        @Test
        void rejectsPasswordsLongerThan72Bytes() {
            // 37 two-byte characters = 74 bytes, below 72 characters but above the BCrypt limit
            assertThatThrownBy(() -> PasswordPolicy.validate("ñ".repeat(37), "a@b.co"))
                    .hasMessage("password must be at most 72 bytes");
        }

        @Test
        void rejectsNumericPasswords() {
            assertThatThrownBy(() -> PasswordPolicy.validate("982734651209", "a@b.co"))
                    .hasMessage("password must not be entirely numeric");
        }

        @ParameterizedTest
        @ValueSource(strings = {"password1234", "PASSWORD1234", "qwertyuiop12"})
        void rejectsCommonPasswordsCaseInsensitively(String password) {
            assertThatThrownBy(() -> PasswordPolicy.validate(password, "a@b.co"))
                    .hasMessage("password is too common");
        }

        @Test
        void rejectsPasswordsContainingTheEmailName() {
            assertThatThrownBy(() -> PasswordPolicy.validate("my-Santiago-2026!", "santiago@franquix.local"))
                    .hasMessage("password is too similar to the email");
        }

        @Test
        void ignoresVeryShortEmailNames() {
            assertThatCode(() -> PasswordPolicy.validate("ab-is-in-here-2026", "ab@x.co")).doesNotThrowAnyException();
        }
    }

    @Test
    void refreshTokensStartUsable() {
        RefreshToken token = RefreshToken.issue("hash", "u1", NOW, NOW.plusSeconds(60));

        assertThat(token.revoked()).isFalse();
        assertThat(new RefreshToken("hash", "u1", NOW, NOW, NOW).revoked()).isTrue();
    }
}
