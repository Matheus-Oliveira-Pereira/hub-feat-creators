package com.hubfeatcreators.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.infra.log.PiiMaskingFilter;
import org.junit.jupiter.api.Test;

class PiiMaskingFilterTest {

    @Test
    void masks_email_in_message() {
        String masked = PiiMaskingFilter.mask("Enviando para joao@empresa.com.br agora");
        assertThat(masked).doesNotContain("joao@empresa.com.br");
        assertThat(masked).contains("j***@empresa.com.br");
    }

    @Test
    void masks_cpf_formatted() {
        String masked = PiiMaskingFilter.mask("CPF do titular: 123.456.789-09 aprovado");
        assertThat(masked).doesNotContain("123.456.789-09");
        assertThat(masked).contains("***.***.***-**");
    }

    @Test
    void masks_cpf_digits_only() {
        String masked = PiiMaskingFilter.mask("cpf=12345678909 cadastrado");
        assertThat(masked).doesNotContain("12345678909");
    }

    @Test
    void masks_br_phone() {
        String masked = PiiMaskingFilter.mask("tel=+5511987654321");
        assertThat(masked).doesNotContain("987654321");
    }

    @Test
    void leaves_non_pii_unchanged() {
        String msg = "Iniciando job tipo=EMAIL_SEND assessoriaId=abc123";
        assertThat(PiiMaskingFilter.mask(msg)).isEqualTo(msg);
    }

    @Test
    void handles_null_gracefully() {
        assertThat(PiiMaskingFilter.mask(null)).isNull();
    }

    @Test
    void masks_multiple_emails_in_same_message() {
        String masked = PiiMaskingFilter.mask("De: a@x.com Para: b@y.com");
        assertThat(masked).doesNotContain("a@x.com");
        assertThat(masked).doesNotContain("b@y.com");
    }
}
