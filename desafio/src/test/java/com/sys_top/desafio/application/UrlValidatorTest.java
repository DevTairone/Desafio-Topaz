package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://exemplo.com",
            "https://exemplo.com",
            "https://exemplo.com/caminho?query=1"
    })
    void deveAceitarUrlsValidas(String url) {
        assertDoesNotThrow(() -> UrlValidator.validate(url));
    }

    @Test
    void deveRejeitarUrlVazia() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(""));
    }

    @Test
    void deveRejeitarUrlNula() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://exemplo.com",
            "exemplo.com",
            "javascript:alert(1)"
    })
    void deveRejeitarProtocoloNaoSuportado(String url) {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(url));
    }
}
