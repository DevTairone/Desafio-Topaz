package com.sys_top.desafio.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62EncoderTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1, 1",
            "9, 9",
            "10, A",
            "35, Z",
            "36, a",
            "61, z",
            "62, 10",
            "125, 21"
    })
    void deveCodificarValoresConhecidos(long valor, String esperado) {
        assertEquals(esperado, Base62Encoder.encode(valor));
    }

    @Test
    void deveDecodificarDeVoltaParaOValorOriginal() {
        for (long valor : new long[]{0, 1, 61, 62, 12345, 999999}) {
            String codigo = Base62Encoder.encode(valor);
            assertEquals(valor, Base62Encoder.decode(codigo));
        }
    }

    @Test
    void deveRejeitarValorNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }

    @Test
    void deveRejeitarCaractereInvalidoNaDecodificacao() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("@@"));
    }
}
