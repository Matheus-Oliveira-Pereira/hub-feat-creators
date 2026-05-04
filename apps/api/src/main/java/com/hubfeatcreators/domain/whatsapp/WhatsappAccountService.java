package com.hubfeatcreators.domain.whatsapp;

import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappAccountService {

    private final WhatsappAccountRepository repo;
    private final WhatsappCipherService cipher;
    private final MetaApiClient meta;

    public WhatsappAccountService(WhatsappAccountRepository repo,
            WhatsappCipherService cipher, MetaApiClient meta) {
        this.repo = repo;
        this.cipher = cipher;
        this.meta = meta;
    }

    @Transactional(readOnly = true)
    public List<WhatsappAccount> list(UUID assessoriaId) {
        return repo.findByAssessoriaIdAndDeletedAtIsNull(assessoriaId);
    }

    @Transactional
    public WhatsappAccount create(UUID assessoriaId, String wabaId, String phoneNumberId,
            String phoneE164, String displayName, String accessToken, String appSecret) {
        meta.validateToken(accessToken);

        var encToken = cipher.encrypt(accessToken);
        var encSecret = cipher.encrypt(appSecret);

        var account = new WhatsappAccount(assessoriaId, wabaId, phoneNumberId, phoneE164,
                displayName, encToken.ciphertext(), encToken.nonce(),
                encSecret.ciphertext(), encSecret.nonce());
        return repo.save(account);
    }

    @Transactional
    public WhatsappAccount update(UUID assessoriaId, UUID id, String displayName,
            String accessToken, String appSecret) {
        WhatsappAccount account = requireAccount(assessoriaId, id);

        if (accessToken != null && !accessToken.isBlank()) {
            meta.validateToken(accessToken);
            var enc = cipher.encrypt(accessToken);
            account.setAccessTokenEnc(enc.ciphertext());
            account.setTokenNonce(enc.nonce());
        }
        if (appSecret != null && !appSecret.isBlank()) {
            var enc = cipher.encrypt(appSecret);
            account.setAppSecretEnc(enc.ciphertext());
            account.setAppSecretNonce(enc.nonce());
        }
        if (displayName != null) account.setDisplayName(displayName);
        return repo.save(account);
    }

    @Transactional
    public void delete(UUID assessoriaId, UUID id) {
        WhatsappAccount account = requireAccount(assessoriaId, id);
        account.setDeletedAt(Instant.now());
        repo.save(account);
    }

    public String decryptToken(WhatsappAccount account) {
        return cipher.decrypt(account.getAccessTokenEnc(), account.getTokenNonce());
    }

    public String decryptAppSecret(WhatsappAccount account) {
        return cipher.decrypt(account.getAppSecretEnc(), account.getAppSecretNonce());
    }

    WhatsappAccount requireAccount(UUID assessoriaId, UUID id) {
        return repo.findByIdAndAssessoriaIdAndDeletedAtIsNull(id, assessoriaId)
                .orElseThrow(BusinessException::notFound);
    }
}
