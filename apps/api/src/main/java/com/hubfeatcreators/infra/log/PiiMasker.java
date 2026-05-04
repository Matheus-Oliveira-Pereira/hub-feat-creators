package com.hubfeatcreators.infra.log;

public class PiiMasker {

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return "***@" + email.substring(atIndex + 1);
        }
        String first = String.valueOf(email.charAt(0));
        String domain = email.substring(atIndex + 1);
        return first + "***@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.length() < 4) return "***";
        return digits.substring(0, 2) + "*****" + digits.substring(digits.length() - 4);
    }

    /** Masks CPF (11 digits) — with or without formatting. */
    public static String maskCpf(String cpf) {
        return "***.***.***-**";
    }

    public static String maskPassword(String password) {
        return "***REDACTED***";
    }
}
