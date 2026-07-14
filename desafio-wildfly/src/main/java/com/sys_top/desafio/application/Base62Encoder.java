package com.sys_top.desafio.application;

/**
 * Utilitário de codificação/decodificação em base62 (0-9, A-Z, a-z).
 *
 * Usado para transformar o valor numérico do contador no código curto
 * exposto na URL encurtada. Stateless e thread-safe. Java puro, sem
 * dependência de framework.
 */
public final class Base62Encoder {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = ALPHABET.length;

    private Base62Encoder() {
    }

    /** Codifica um valor não negativo em uma string base62. */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET[0]);
        }

        StringBuilder builder = new StringBuilder();
        long remainingValue = value;
        while (remainingValue > 0) {
            int remainder = (int) (remainingValue % BASE);
            builder.append(ALPHABET[remainder]);
            remainingValue /= BASE;
        }
        return builder.reverse().toString();
    }

    /** Decodifica uma string base62 de volta para o valor numérico original. */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Código não pode ser vazio");
        }

        long result = 0;
        for (char character : code.toCharArray()) {
            result = result * BASE + indexOf(character);
        }
        return result;
    }

    private static int indexOf(char character) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == character) {
                return i;
            }
        }
        throw new IllegalArgumentException("Caractere inválido para base62: " + character);
    }
}
