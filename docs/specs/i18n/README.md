# Module: Internationalization (i18n) — HUB Feat Creator

> **Status**: ❌ **desativado no MVP**.

## Decisão

MVP é **pt-BR apenas**. Público-alvo é assessoria de influenciadores no Brasil; lançar i18n antes de PMF é overhead sem retorno.

Quando reavaliar:
- [ ] Pós-PMF (≥ 100 assessorias ativas)
- [ ] Demanda concreta de cliente em outro país
- [ ] Expansão LATAM (es, en) — provavelmente primeiro destino

## Idiomas (planejado para quando ativar)

| Idioma | Código | Status atual | Fallback |
|--------|--------|--------------|----------|
| Português (Brasil) | `pt-BR` | **Único — atual** | — |
| Espanhol (LATAM) | `es-419` | planejado Fase 5 | pt-BR |
| Inglês | `en-US` | planejado Fase 5 | pt-BR |

## Convenções já adotadas (mesmo no MVP pt-BR only)

Para reduzir custo de i18n futuro, **desde já**:

- [x] **HTML lang**: `<html lang="pt-BR">` em todas páginas
- [x] **Datas**: usar `Intl.DateTimeFormat` (não format strings hardcoded)
- [x] **Números/moedas**: usar `Intl.NumberFormat` com locale `pt-BR`, currency `BRL`
- [x] **Pluralização**: usar lib que suporta ICU (`react-intl` ou `next-intl`) **mesmo só com pt-BR** — tornar i18n-ready desde o primeiro componente
- [x] **Strings em arquivo separado** (não inline em JSX): `apps/web/locales/pt-BR/<namespace>.json` — facilita extração futura
- [x] **Imagens com texto**: evitar; SVG inline com `<text>` quando inevitável (extraível)
- [x] **Backend**: mensagens de erro user-facing em arquivo de mensagens (`apps/api/src/main/resources/messages_pt_BR.properties`) com Spring `MessageSource`

## Translation strategy (quando ativar)

- **Library frontend**: `next-intl` (suporte a App Router, RSC-compatible, ICU)
- **Library backend**: Spring `MessageSource` (já built-in)
- **Format**: JSON estruturado por namespace (frontend); `.properties` por locale (backend)
- **Localização frontend**: `apps/web/locales/<locale>/<namespace>.json`
- **Localização backend**: `apps/api/src/main/resources/messages_<locale>.properties`
- **Gestão**: manual no MVP+1 idioma; **Crowdin** ou **Lokalise** quando ≥ 3 idiomas
- **Translation key**: `<namespace>.<feature>.<elemento>` (ex: `prospeccoes.lista.botao_criar`)
- **Pseudo-locale** durante dev: opcional, ajuda a detectar strings hardcoded

## Detecção de idioma (quando ativar)

Ordem de prioridade:
1. Preferência salva do usuário (perfil)
2. Header `Accept-Language` do browser
3. Default `pt-BR`

## Code rules (quando ativar)

- ❌ **Nunca** texto hardcoded visível ao usuário — sempre via translation key
- ✅ Datas: `Intl.DateTimeFormat(locale).format(date)`
- ✅ Números: `Intl.NumberFormat(locale, opts).format(n)`
- ✅ Moeda: sempre exibir em locale do usuário (BRL no MVP)
- ✅ Pluralização: ICU message format (`"{count, plural, one {# tarefa} other {# tarefas}}"`)
- ✅ RTL: não aplicável (idiomas planejados são LTR)
- ❌ Imagens com texto embutido — manter texto em DOM/SVG

## QA (quando ativar)

- [ ] Visual test com texto longo (alemão expande ~30%, mas espanhol também tende +20% vs en/pt)
- [ ] Truncamento em componentes de largura fixa (botões, badges, cards)
- [ ] Pseudo-locale em dev para detectar strings escapando da extração
- [ ] Lighthouse i18n audit
- [ ] Revisão por nativo do idioma alvo antes de release

## Pendências quando ativar

- [ ] Adicionar `next-intl` em `apps/web` (setup App Router)
- [ ] Migrar todas strings para `apps/web/locales/pt-BR/`
- [ ] Criar `messages_pt_BR.properties` para backend (templates de e-mail e mensagens de erro)
- [ ] Adicionar seletor de idioma no header
- [ ] Persistir `locale` no perfil do usuário (`usuarios.locale`)
- [ ] Pipeline de extração de strings (ESLint plugin que detecta texto JSX não-key)
