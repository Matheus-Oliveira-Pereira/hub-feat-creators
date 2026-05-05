package com.hubfeatcreators.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Secrets secrets = new Secrets();

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Jwt {
        private String secret = "dev-only-not-for-prod-replace-with-256bit-secret";
        private long expirationMinutes = 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class Secrets {
        private String emailKey = "dev-only-not-for-prod-32-byte-aes-key";
        private String whatsappKey = "dev-only-not-for-prod-whatsapp-32b!";

        public String getEmailKey() {
            return emailKey;
        }

        public void setEmailKey(String emailKey) {
            this.emailKey = emailKey;
        }

        public String getWhatsappKey() {
            return whatsappKey;
        }

        public void setWhatsappKey(String whatsappKey) {
            this.whatsappKey = whatsappKey;
        }
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public void setSecrets(Secrets secrets) {
        this.secrets = secrets;
    }

    private Webpush webpush = new Webpush();
    private Web web = new Web();
    private Features features = new Features();

    public static class Web {
        private String baseUrl = "http://localhost:3000";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Webpush {
        private String publicKey = "";
        private String privateKey = "";
        private String subject = "mailto:admin@example.com";

        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
    }

    public Webpush getWebpush() { return webpush; }
    public void setWebpush(Webpush webpush) { this.webpush = webpush; }

    public Web getWeb() { return web; }
    public void setWeb(Web web) { this.web = web; }

    public static class Features {
        private boolean complianceStrict = true;
        private boolean signupEnabled = true;
        private boolean whatsappEnabled = false;

        public boolean isComplianceStrict() { return complianceStrict; }
        public void setComplianceStrict(boolean complianceStrict) { this.complianceStrict = complianceStrict; }
        public boolean isSignupEnabled() { return signupEnabled; }
        public void setSignupEnabled(boolean signupEnabled) { this.signupEnabled = signupEnabled; }
        public boolean isWhatsappEnabled() { return whatsappEnabled; }
        public void setWhatsappEnabled(boolean whatsappEnabled) { this.whatsappEnabled = whatsappEnabled; }
    }

    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }
}
