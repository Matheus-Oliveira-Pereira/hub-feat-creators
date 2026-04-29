# ADR-007: App mobile com Expo (usuário final apenas)

## Status
Accepted — 2026-04-29

## Context
Usuário final (influenciador, contato de marca) precisa acesso mobile para acompanhar prospecções, tarefas atribuídas e notificações. Telas administrativas (configuração de conta SMTP, cadastro de WhatsApp business, gestão de usuários) permanecem exclusivas da web.

## Decision
- **React Native + Expo (SDK 50+)** com TypeScript strict.
- Path: `/apps/mobile`.
- **Escopo**: somente telas de usuário final. Toda configuração admin fica na web.
- Auth: JWT do mesmo backend (Spring Boot), token em `expo-secure-store` (Keychain iOS / Keystore Android). Nunca AsyncStorage.
- Build/distribuição: **EAS Build** + **EAS Submit**. OTA via `eas update` para mudanças JS-only.
- Push notifications: Expo Push Service (APNs/FCM por baixo).
- Navegação: `expo-router` (file-based).
- Estado: TanStack Query + Zustand (alinhado com web).
- Design tokens compartilhados de `docs/specs/design-system/` (mesmo Claude Design Flow A).
- Test: Jest + React Native Testing Library (unit) + Detox (e2e).

## Alternatives considered
1. **PWA mobile-first** — sem app stores, sem custo Apple Dev. Rejeitado: push notifications em iOS PWA são limitados; experiência inferior; sem deep link nativo.
2. **React Native bare workflow** — controle total sobre código nativo. Rejeitado: custo de manutenção alto; Expo cobre 95% das necessidades; podemos `expo prebuild` se algum dia precisar.
3. **Flutter** — performance, single codebase. Rejeitado: time já em TS/React; reuso de tipos/queries com web zera curva.
4. **Expo (escolhido)** — DX excelente, OTA, EAS gerencia builds, ecossistema RN, compartilha types com web.

## Consequences
- Positive: codebase TS unificado web↔mobile; OTA acelera correções; EAS abstrai certificados/provisioning; reuso de design tokens.
- Negative: bundle size maior que nativo puro; algumas libs nativas exigem `expo prebuild`; custo EAS em volume alto.
- Risks: Apple/Google podem rejeitar OTA que muda comportamento substancial sem review; SDK upgrade quebra libs.

## Impact on specs
- **Security**: token em SecureStore; certificate pinning opcional; biometria (`expo-local-authentication`).
- **Accessibility**: WCAG mobile (touch target ≥44pt, screen reader, dynamic type).
- **Design-system**: tokens precisam ser plataforma-agnósticos (cores/spacing/type) — Tailwind via `nativewind` ou tokens JSON consumidos por StyleSheet.
- **Testing-strategy**: pirâmide adapta — unit Jest, e2e Detox, smoke EAS preview.
- **Devops**: workflow EAS no GitHub Actions; secrets EAS_TOKEN.
- **API**: contratos REST já versionados servem mobile sem mudança; considerar paginação cursor-based para listas longas.
- Novo módulo: `docs/specs/mobile/` (a criar).

## References
- PRD: `docs/product/vision.md`
- Expo: https://docs.expo.dev
- EAS: https://docs.expo.dev/eas/
- ADR-002 (stack base)
