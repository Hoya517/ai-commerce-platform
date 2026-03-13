package com.hoya.aicommerce.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionConverterTest {

    private final EncryptionConverter converter = new EncryptionConverter("test-encryption-key-for-unit-test");

    @Test
    void 평문을_암호화하고_복호화할_수_있다() {
        String plainText = "110-1234-567890";

        String encrypted = converter.convertToDatabaseColumn(plainText);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void null_입력시_null이_반환된다() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void 동일한_평문도_매번_다른_암호문이_생성된다() {
        String plainText = "110-1234-567890";

        String encrypted1 = converter.convertToDatabaseColumn(plainText);
        String encrypted2 = converter.convertToDatabaseColumn(plainText);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }
}
