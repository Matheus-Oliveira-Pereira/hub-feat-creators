package com.hubfeatcreators.domain.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token helpers for email unsubscribe links.
 *
 * <p>Format: base64url( assessoriaId ":" email ":" hmacHex )
 *
 * <p>hmacHex = HmacSHA256(emailKey, assessoriaId ":" email) — always 64 hex chars.
 */
final class EmailUnsubscribeTokens {

    private static final int HMAC_HEX_LEN = 64;

    private EmailUnsubscribeTokens() {}

    static String generate(String emailKey, String assessoriaId, String email) {
        String payload = assessoriaId + ":" + email;
        String hmac = hmacSha256Hex(emailKey, payload);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((payload + ":" + hmac).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns [assessoriaId, email] if signature is valid, or null if invalid/tampered.
     * Constant-time HMAC comparison.
     */
    static String[] verify(String emailKey, String token) {
        try {
            String decoded =
                    new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            // last HMAC_HEX_LEN+1 chars = ':' + hmac
            int hmacSep = decoded.length() - HMAC_HEX_LEN - 1;
            if (hmacSep < 37 || decoded.charAt(hmacSep) != ':') return null;
            String payload = decoded.substring(0, hmacSep);
            String receivedHmac = decoded.substring(hmacSep + 1);
            if (receivedHmac.length() != HMAC_HEX_LEN) return null;
            String expectedHmac = hmacSha256Hex(emailKey, payload);
            if (!MessageDigest.isEqual(
                    expectedHmac.getBytes(StandardCharsets.UTF_8),
                    receivedHmac.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            int sep = payload.indexOf(':');
            if (sep < 1) return null;
            return new String[] {payload.substring(0, sep), payload.substring(sep + 1)};
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
