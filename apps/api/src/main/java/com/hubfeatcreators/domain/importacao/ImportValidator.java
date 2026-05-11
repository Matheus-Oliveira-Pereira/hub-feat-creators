package com.hubfeatcreators.domain.importacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ImportValidator {

    private ImportValidator() {}

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final Pattern PHONE_DIGITS = Pattern.compile("[^0-9+]");

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);

    public static List<String> validateInfluenciador(Map<String, String> row) {
        List<String> erros = new ArrayList<>();
        requireNonBlank(row, "nome", erros);

        String email = row.get("email");
        if (email != null && !email.isBlank() && !isValidEmail(email)) {
            erros.add("email inválido: " + email);
        }

        String telefone = row.get("telefone");
        if (telefone != null && !telefone.isBlank()) {
            String normalized = normalizePhone(telefone);
            if (normalized == null) {
                erros.add("telefone inválido (esperado E.164 BR): " + telefone);
            }
        }

        String cpf = row.get("cpf");
        if (cpf != null && !cpf.isBlank() && !isValidCpf(cpf)) {
            erros.add("CPF inválido: " + cpf);
        }

        return erros;
    }

    public static List<String> validateMarca(Map<String, String> row) {
        List<String> erros = new ArrayList<>();
        requireNonBlank(row, "nome", erros);

        String cnpj = row.get("cnpj");
        if (cnpj != null && !cnpj.isBlank() && !isValidCnpj(cnpj)) {
            erros.add("CNPJ inválido: " + cnpj);
        }

        String site = row.get("site");
        if (site != null && !site.isBlank() && !URL_PATTERN.matcher(site).matches()) {
            erros.add("site URL inválida: " + site);
        }

        return erros;
    }

    public static List<String> validateContato(Map<String, String> row) {
        List<String> erros = new ArrayList<>();
        requireNonBlank(row, "nome", erros);

        String email = row.get("email");
        if (email != null && !email.isBlank() && !isValidEmail(email)) {
            erros.add("email inválido: " + email);
        }

        String telefone = row.get("telefone");
        if (telefone != null && !telefone.isBlank()) {
            String normalized = normalizePhone(telefone);
            if (normalized == null) {
                erros.add("telefone inválido (esperado E.164 BR): " + telefone);
            }
        }

        return erros;
    }

    /** Normalizes Brazilian phone to E.164 (+55...). Returns null if invalid. */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = PHONE_DIGITS.matcher(raw.trim()).replaceAll("");
        if (digits.startsWith("55") && digits.length() >= 12) {
            digits = "+" + digits;
        } else if (!digits.startsWith("+")) {
            if (digits.length() == 11 || digits.length() == 10) {
                digits = "+55" + digits;
            } else if (digits.length() == 13 && digits.startsWith("55")) {
                digits = "+" + digits;
            } else {
                return null;
            }
        }
        // +55 + 10 or 11 digits = 13 or 14 total
        if (digits.length() < 13 || digits.length() > 14) return null;
        return digits;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidCpf(String raw) {
        if (raw == null) return false;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() != 11) return false;
        if (digits.chars().distinct().count() == 1) return false;
        return checkCpfDigit(digits, 9) && checkCpfDigit(digits, 10);
    }

    private static boolean checkCpfDigit(String digits, int pos) {
        int sum = 0;
        for (int i = 0; i < pos; i++) {
            sum += (digits.charAt(i) - '0') * (pos + 1 - i);
        }
        int rem = (sum * 10) % 11;
        if (rem == 10) rem = 0;
        return rem == (digits.charAt(pos) - '0');
    }

    public static boolean isValidCnpj(String raw) {
        if (raw == null) return false;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;
        return checkCnpjDigit(digits, 12) && checkCnpjDigit(digits, 13);
    }

    private static boolean checkCnpjDigit(String digits, int pos) {
        int[] weights = pos == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < pos; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int rem = sum % 11;
        int expected = rem < 2 ? 0 : 11 - rem;
        return expected == (digits.charAt(pos) - '0');
    }

    /** Levenshtein distance for auto-suggest column mapping (AC-2). */
    public static int levenshtein(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] curr = new int[b.length() + 1];
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = curr;
        }
        return prev[b.length()];
    }

    private static void requireNonBlank(Map<String, String> row, String field, List<String> erros) {
        String val = row.get(field);
        if (val == null || val.isBlank()) {
            erros.add("campo obrigatório ausente: " + field);
        }
    }
}
