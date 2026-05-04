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
        String localPart = email.substring(0, atIndex);
        String first = localPart.substring(0, 1);
        String domain = email.substring(atIndex + 1);
        return first + "***@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, 2) + "*****" + phone.substring(phone.length() - 4);
    }

    public static String maskPassword(String password) {
        return "***REDACTED***";
    }
}
