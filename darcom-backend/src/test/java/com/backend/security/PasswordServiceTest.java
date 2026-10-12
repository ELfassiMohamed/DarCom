package com.backend.security;

import com.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwords = new PasswordService();

    @Test
    void policyAcceptsStrongPassword() {
        assertNull(PasswordService.policyError("Aa1!aaaa"));
        passwords.validate("Str0ng!Pass");
    }

    @Test
    void policyRejectsTooShort() {
        assertThrows(ApiException.class, () -> passwords.validate("Aa1!aa"));
    }

    @Test
    void policyRejectsOver72Bytes() {
        // 73 ASCII chars = 73 bytes > bcrypt's 72-byte limit.
        String tooLong = "Aa1!" + "a".repeat(69);
        assertThrows(ApiException.class, () -> passwords.validate(tooLong));
    }

    @Test
    void policyRejectsMissingCharacterClasses() {
        assertThrows(ApiException.class, () -> passwords.validate("aa1!aaaa")); // no uppercase
        assertThrows(ApiException.class, () -> passwords.validate("AA1!AAAA")); // no lowercase
        assertThrows(ApiException.class, () -> passwords.validate("Aaa!aaaa")); // no digit
        assertThrows(ApiException.class, () -> passwords.validate("Aa1aaaaa")); // no special
    }

    @Test
    void hashAndVerify() {
        String hash = passwords.hash("Str0ng!Pass");
        assertNotEquals("Str0ng!Pass", hash);
        assertTrue(passwords.matches("Str0ng!Pass", hash));
        assertFalse(passwords.matches("Wr0ng!Pass", hash));
    }

    @Test
    void dummyVerifyReturnsFalseWithoutThrowing() {
        assertFalse(passwords.matchesDummy("whatever@example.com"));
    }
}
