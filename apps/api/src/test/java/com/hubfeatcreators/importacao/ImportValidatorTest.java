package com.hubfeatcreators.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.domain.importacao.ImportValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ImportValidatorTest {

    // ---- CPF ---

    @Test
    void cpfValido() {
        assertThat(ImportValidator.isValidCpf("529.982.247-25")).isTrue();
    }

    @Test
    void cpfInvalido_digitoErrado() {
        assertThat(ImportValidator.isValidCpf("529.982.247-26")).isFalse();
    }

    @Test
    void cpfInvalido_sequencia() {
        assertThat(ImportValidator.isValidCpf("111.111.111-11")).isFalse();
    }

    @Test
    void cpfInvalido_tamanho() {
        assertThat(ImportValidator.isValidCpf("12345")).isFalse();
    }

    // ---- CNPJ ---

    @Test
    void cnpjValido() {
        assertThat(ImportValidator.isValidCnpj("11.222.333/0001-81")).isTrue();
    }

    @Test
    void cnpjInvalido_digitoErrado() {
        assertThat(ImportValidator.isValidCnpj("11.222.333/0001-82")).isFalse();
    }

    @Test
    void cnpjInvalido_sequencia() {
        assertThat(ImportValidator.isValidCnpj("00.000.000/0000-00")).isFalse();
    }

    // ---- Phone E.164 ---

    @Test
    void phoneNormalize_11digits() {
        assertThat(ImportValidator.normalizePhone("11987654321")).isEqualTo("+5511987654321");
    }

    @Test
    void phoneNormalize_withDDI() {
        assertThat(ImportValidator.normalizePhone("+5511987654321")).isEqualTo("+5511987654321");
    }

    @Test
    void phoneNormalize_formatted() {
        assertThat(ImportValidator.normalizePhone("(11) 98765-4321")).isEqualTo("+5511987654321");
    }

    @Test
    void phoneNormalize_invalid() {
        assertThat(ImportValidator.normalizePhone("12345")).isNull();
    }

    @Test
    void phoneNormalize_blank() {
        assertThat(ImportValidator.normalizePhone("")).isNull();
        assertThat(ImportValidator.normalizePhone(null)).isNull();
    }

    // ---- Email ---

    @Test
    void emailValido() {
        assertThat(ImportValidator.isValidEmail("user@example.com")).isTrue();
    }

    @Test
    void emailInvalido() {
        assertThat(ImportValidator.isValidEmail("not-an-email")).isFalse();
        assertThat(ImportValidator.isValidEmail("@domain.com")).isFalse();
    }

    // ---- Levenshtein ---

    @Test
    void levenshtein_identical() {
        assertThat(ImportValidator.levenshtein("nome", "nome")).isEqualTo(0);
    }

    @Test
    void levenshtein_close() {
        assertThat(ImportValidator.levenshtein("name", "nome")).isLessThan(3);
    }

    @Test
    void levenshtein_far() {
        assertThat(ImportValidator.levenshtein("cnpj", "instagram")).isGreaterThanOrEqualTo(3);
    }

    // ---- validateInfluenciador ---

    @Test
    void validateInfluenciador_ok() {
        Map<String, String> row = Map.of("nome", "João Silva", "email", "joao@example.com");
        assertThat(ImportValidator.validateInfluenciador(row)).isEmpty();
    }

    @Test
    void validateInfluenciador_missingNome() {
        Map<String, String> row = Map.of("email", "joao@example.com");
        List<String> erros = ImportValidator.validateInfluenciador(row);
        assertThat(erros).anyMatch(e -> e.contains("nome"));
    }

    @Test
    void validateInfluenciador_badEmail() {
        Map<String, String> row = Map.of("nome", "João", "email", "not-email");
        assertThat(ImportValidator.validateInfluenciador(row)).anyMatch(e -> e.contains("email"));
    }

    @Test
    void validateInfluenciador_badPhone() {
        Map<String, String> row = Map.of("nome", "João", "telefone", "123");
        assertThat(ImportValidator.validateInfluenciador(row))
                .anyMatch(e -> e.contains("telefone"));
    }

    @Test
    void validateInfluenciador_badCpf() {
        Map<String, String> row = Map.of("nome", "João", "cpf", "000.000.000-00");
        assertThat(ImportValidator.validateInfluenciador(row)).anyMatch(e -> e.contains("CPF"));
    }

    // ---- validateMarca ---

    @Test
    void validateMarca_ok() {
        Map<String, String> row = Map.of("nome", "Acme Corp", "site", "https://acme.com");
        assertThat(ImportValidator.validateMarca(row)).isEmpty();
    }

    @Test
    void validateMarca_badCnpj() {
        Map<String, String> row = Map.of("nome", "Acme", "cnpj", "11.111.111/1111-11");
        assertThat(ImportValidator.validateMarca(row)).anyMatch(e -> e.contains("CNPJ"));
    }

    @Test
    void validateMarca_badSite() {
        Map<String, String> row = Map.of("nome", "Acme", "site", "not-a-url");
        assertThat(ImportValidator.validateMarca(row)).anyMatch(e -> e.contains("URL"));
    }

    // ---- validateContato ---

    @Test
    void validateContato_ok() {
        Map<String, String> row = Map.of("nome", "Maria", "email", "maria@brand.com");
        assertThat(ImportValidator.validateContato(row)).isEmpty();
    }

    @Test
    void validateContato_missingNome() {
        assertThat(ImportValidator.validateContato(Map.of())).anyMatch(e -> e.contains("nome"));
    }
}
