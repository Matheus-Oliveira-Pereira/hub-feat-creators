# ADR-002: Stack Base — Java 21 + Spring Boot 3 + Next.js 14 + PostgreSQL 16

## Status
Accepted — 2026-04-29

## Context
Decisão fundacional do projeto: linguagens, frameworks e banco de dados. Define ergonomia, comunidade, ferramental, custo de hosting e curva de aprendizado para anos.

Critérios:
- **Familiaridade do solo dev**: Java + React/Next em background; primeira vez com Spring Boot moderno (3.x) e Next App Router
- **Ecosistema**: bibliotecas maduras para casos de uso comerciais (auth, e-mail, queue, ORM, validação)
- **Hosting econômico**: rodar barato em Railway (api) + Vercel (web) no MVP
- **LTS**: estabilidade para 2-3 anos sem upgrades forçados
- **Curva de aprendizado**: solo dev pode produzir rápido sem afundar em ergonomia
- **Tipagem forte**: reduzir bugs no front (TS strict) e back (Java)
- **Comunidade B2B SaaS**: padrões conhecidos para multi-tenant, audit, observability

## Decision

### Backend
- **Java 21 LTS** (versão LTS atual; suporte até 2031)
- **Spring Boot 3.3+** (jakarta.* namespace estável; baseline para Spring 6)
- **Maven** como build tool
- **HikariCP** (default Spring) para connection pooling
- **JPA/Hibernate** como ORM (com `@Filter` para multi-tenant)
- **Flyway** para migrations
- **Spring Security** + **JWT** (auth0 java-jwt ou jjwt)
- **Bean Validation** (Hibernate Validator) para DTOs
- **springdoc-openapi** para documentação OpenAPI 3.1 auto-gerada
- **Micrometer** + **OpenTelemetry** para observability
- **Testcontainers** + **JUnit 5** + **Mockito** para testes

### Frontend
- **Next.js 14+** com **App Router**
- **TypeScript** com `strict: true`
- **React 18+**
- **Tailwind CSS** (utility-first; tokens via CSS variables)
- **shadcn/ui** + **Radix UI primitives** (acessível por padrão, copy-paste)
- **react-hook-form** + **zod** para forms e validação
- **TanStack Query** (React Query) para data fetching e cache cliente
- **next-intl** ou **next-themes** para temas (i18n só pt-BR no MVP)
- **lucide-react** para ícones
- **Vitest** + **Testing Library** + **Playwright** para testes
- **pnpm** como package manager

### Database
- **PostgreSQL 16** (LTS-equivalent — Postgres tem 5 anos de suporte por major)
- **Redis 7** (cache + filas + rate limit)
- **pgvector** (extensão Postgres para memory L4)

## Alternatives considered

### Backend

1. **Node.js + NestJS + TypeScript**
   - **+** stack único TS no projeto inteiro
   - **+** ecosistema npm vasto
   - **−** menos familiaridade do dev do que Java
   - **−** ergonomia de tipos em runtime (sem compile guarantees como Java)
   - **−** maturidade enterprise menor que Spring para auth/multi-tenant patterns
   - Decisão: descartado pela familiaridade + maturidade Spring no domínio B2B

2. **Kotlin + Spring Boot**
   - **+** mesmo Spring, sintaxe mais concisa
   - **+** null safety compile-time
   - **−** dev menos confortável; aprendizado adicional sem ganho concreto no MVP
   - Reavaliar após Java 21 estabilizar uso (talvez Fase 3)

3. **Go + Gin/Echo**
   - **+** binário pequeno, deploy simples, performance excelente
   - **−** ecosistema enterprise menos maduro (multi-tenant, ORM, auth ainda mais "monte você")
   - **−** familiaridade do dev menor

### Frontend

1. **Next.js Pages Router**
   - **+** mais maduro/estável; mais material de aprendizado
   - **−** App Router é o futuro; iniciar projeto novo em legacy é débito imediato
   - **−** server components não disponíveis (perde performance e DX)
   - Decisão: App Router

2. **Remix**
   - **+** modelo de mutações elegante, Web Standards-first
   - **−** comunidade menor; integração shadcn menos polida
   - **−** Vercel hosta Remix mas DX Next no Vercel é melhor

3. **Vite + React puro**
   - **+** mais leve, build rápido
   - **−** sem SSR/SSG/ISR de fábrica; precisa montar
   - **−** SEO público mais trabalhoso

### Database

1. **MySQL 8**
   - **+** familiar a muitos; Railway suporta
   - **−** features avançadas (CTE recursivo, window funcs, JSONB) menos polidas que Postgres
   - **−** sem pgvector (precisaria stack separado para memory L4)

2. **MongoDB**
   - **+** flexível para schema evolutivo
   - **−** projetos B2B com auditoria/relatórios são naturalmente relacionais
   - **−** transações multi-doc menos triviais
   - Decisão: descartado — domínio é claramente relacional

## Consequences

**Positivas**:
- Stack consolidado com 15+ anos de comunidade (Spring, React) — soluções para problemas comuns são procuráveis
- LTS de longo prazo (Java 21 até 2031, Postgres 16 por 5 anos) — sem pressão de upgrade no MVP
- TS strict + Java forte tipagem reduz classes inteiras de bug
- Testcontainers + Vitest/Playwright dão confiança real em testes
- pgvector permite memory L4 sem stack vetorial separado
- shadcn + Tailwind dão velocidade de UI sem perder customização

**Negativas**:
- Cold start Java em Railway é ~10s (mitigar com healthcheck + min replicas se atingir SLA crítico)
- Bundle Spring Boot ~50MB (vs Node ~10MB) — irrelevante em hosting moderno mas afeta tempo de build
- Curva inicial App Router (server components) tem armadilhas (hidratação, `'use client'`)
- Maven é mais verboso que Gradle, mas mais estável e familiar

**Riscos**:
- Lock-in moderado em Spring (ecosistema próprio); mitigação: usar abstrações JDBC/HTTP padrão onde possível
- Java 22+ traz features (records patterns, virtual threads aprimorados); migrar quando próxima LTS sair (Java 25 LTS em 2025)

## Impact on specs
- **security**: detalha auth JWT, Argon2id, multi-tenant via Hibernate `@Filter`
- **api**: REST + OpenAPI via springdoc; convenções pt-BR no domínio
- **data-architecture**: Postgres + Flyway + UUID v7
- **testing-strategy**: Testcontainers para integration; Playwright para E2E
- **devops**: builds Maven + pnpm; Railway api + Vercel web
- **scalability**: HikariCP, Redis, particionamento futuro de tabelas grandes
- **observability**: Micrometer + OTel
- **design-system**: Tailwind + shadcn + Radix
- **accessibility**: Radix primitives ajudam a11y por padrão

## References
- PRD: vision em `docs/product/vision.md`
- Java 21 LTS — https://openjdk.org/projects/jdk/21/
- Spring Boot 3.x — https://spring.io/projects/spring-boot
- Next.js App Router — https://nextjs.org/docs/app
- shadcn/ui — https://ui.shadcn.com/
- ADR-001 (estrutura monorepo) — relacionado
