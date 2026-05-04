# PRD-014: Mobile App (Expo) — MVP

## Context

Influenciador vive no celular. Portal web (PRD-013) atende, mas notificação push nativa, captura direta de foto/vídeo, e UX mobile-first acelerada exigem app. Para creator, web é "site"; app é "presença".
Vision: Fase 2 — usuário final.
Depende de: PRDs 06, 07, 09, 13.

## Objective

App React Native (Expo SDK 50+) para influenciador acessar campanhas, entregar materiais (com câmera), receber push e trocar mensagens — espelhando portal web. Sem telas admin/config (exclusivo web).

## Scope

### Includes
- [ ] **Auth**: login/senha + MFA TOTP; token JWT em SecureStore (iOS Keychain / Android Keystore — nunca AsyncStorage)
- [ ] **Sign-in social** (Google, Apple) opcional Fase 1.5
- [ ] **Telas**: Home (campanhas ativas), Detalhe campanha, Detalhe tarefa, Upload entregável, Mensagens, Perfil
- [ ] **Captura mídia**: câmera nativa (foto/vídeo) via `expo-camera`; galeria via `expo-image-picker`
- [ ] **Upload progressivo**: chunked + retry; background via `expo-task-manager` se OS permitir
- [ ] **Push notifications**: FCM (Android) + APNs (iOS) via `expo-notifications`; tipos espelham PRD-009
- [ ] **Deep link**: `featcreators://campanha/{id}` + Universal Links/App Links
- [ ] **Modo offline-leve**: lista de campanhas em cache (SWR pattern); mutations com queue em retry quando online
- [ ] **i18n**: pt-BR no MVP (espelha decisão web)
- [ ] **OTA updates**: `eas update` para mudanças JS sem store review
- [ ] **Tema**: dark/light + auto seguir sistema
- [ ] **Acessibilidade**: roles, labels, navegação por leitor de tela

### Excludes
- [ ] Telas admin/config — web only (decisão CLAUDE.md)
- [ ] Edição de vídeo no app — fora MVP
- [ ] Live streaming — fora escopo
- [ ] iPad/tablet layout otimizado — usa layout phone com escala

## Not Doing

- **App para assessora** (admin mobile) — produto não é mobile-first do lado operacional. Web cobre.
- **Bare workflow React Native** — Expo managed cobre 100% do escopo MVP. Bare só se precisar módulo nativo custom.

## User Stories

- Como creator, quero abrir app e ver minhas campanhas
- Como creator, quero gravar vídeo direto e enviar
- Como creator, quero receber push quando assessora aprovar
- Como creator, quero login biométrico (Face/Touch ID)

## Acceptance Criteria

- [ ] **AC-1**: Token JWT em SecureStore; logout apaga; biometria gateia abertura se ativada
- [ ] **AC-2**: Push: registra `device_token` no backend (`device_subscriptions(canal=APNS|FCM, token, ativa)`); evento PRD-009 dispara push
- [ ] **AC-3**: Captura vídeo respeita limite de 500MB e tempo razoável; mostra progresso real
- [ ] **AC-4**: Upload retoma após perder rede (retry exponencial até 4x, 1m/5m/30m/2h)
- [ ] **AC-5**: Deep link abre tela correta mesmo com app fechado (cold start)
- [ ] **AC-6**: Cache offline mostra "última sincronização há X" quando sem rede
- [ ] **AC-7**: OTA update aplica em próximo cold start; rollback nativo via versão anterior
- [ ] **AC-8**: Cross-tenant impossível: token JWT define assessoria; tentativa direta API bloqueada
- [ ] **AC-9**: Permissões Android/iOS solicitadas só no contexto (camera/storage/notif) — não no boot
- [ ] **AC-NF-1**: Tamanho APK/IPA inicial < 80MB
- [ ] **AC-NF-2**: Cold start < 2.5s em dispositivo médio (Galaxy A30 / iPhone SE 2nd)
- [ ] **AC-NF-3**: Crash-free rate ≥ 99.5% (Sentry mobile)
- [ ] **AC-NF-4**: Cobertura ≥ 70% em business logic; e2e Detox cobre fluxo crítico (login → ver campanha → upload)

## Technical Decisions

- **Stack**: Expo SDK 50+, React Native 0.73+, TypeScript strict, React Query, Zustand para state local
- **Auth**: lib `expo-secure-store` + Argon2 nunca no client; senha vai cifrada por TLS para `/auth/login`
- **Biometria**: `expo-local-authentication`
- **Push**: `expo-notifications` (server envia via Expo Push API ou direto FCM/APNs — preferir Expo Push para simplicidade MVP)
- **Storage upload**: URL assinada PUT direto no backend abstrato (mesma abstração `AttachmentStorage` PRD-004/13)
- **Build**: EAS Build (preview + production)
- **Submit**: EAS Submit + manual review iOS/Android
- **Crash/perf**: Sentry RN
- **Deep linking**: `expo-linking` + entitlements iOS + intent-filter Android

### Schema (backend)

```sql
device_subscriptions (id, user_id FK, user_tipo CHECK ('CREATOR','INTERNO'), canal CHECK ('APNS','FCM','WEBPUSH'),
  token TEXT NOT NULL, plataforma TEXT, ativa BOOL DEFAULT TRUE, ultimo_uso, created_at,
  UNIQUE (canal, token))
```

(Estende `webpush_subscriptions` PRD-009 → consolidar em `device_subscriptions`).

## Impact on Specs

- **Compliance**: app store policy (privacy nutrition labels iOS, data safety form Google); request mínimas de permissão; LGPD na captura de mídia
- **Security**: certificate pinning (opt-in); RASP básico (jailbreak/root detection log)
- **Scalability**: push fan-out reusa infra PRD-009
- **Observability**: Sentry; métricas backend `mobile_login_total`, `mobile_upload_total{status}`
- **Accessibility**: roles RN, labels, contraste tema dark
- **API**: paths `/api/v1/portal/*` reaproveitados; novos `/api/v1/devices/*`
- **Testing**: Detox iOS + Android em CI EAS

## Rollout

- **Feature flag**: backend `feature.mobile.enabled`; app respeita
- **Versão mínima suportada**: iOS 14+, Android 8+ (API 26)
- **Distribuição**: TestFlight + Play Internal Testing → externo após 2 semanas
- **OTA channel**: `production`, `staging`, `preview`
- **Rollback**: store rollback last build; OTA push de versão anterior
- **Monitoramento**: Sentry release tracking; métricas adoção via backend login

## Métricas

- ≥ 60% creators convidados instalam em 30d
- DAU/MAU ≥ 35%
- Crash-free ≥ 99.5%
- Tempo upload mediana < 2min em vídeo de 100MB

## Open questions

- [ ] Sign-in social Apple obrigatório se tiver Google? — sim por policy iOS
- [ ] Background upload limites iOS? — sim, ~30s; estratégia híbrida (foreground com user opt-in para grandes)
- [ ] Aceitar entregar via app sem login (link direto)? — não no MVP, segurança
