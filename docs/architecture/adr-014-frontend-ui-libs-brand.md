# ADR-014: Frontend UI complementar — libs táticas e identidade de marca

## Status
Accepted — 2026-04-30

## Context
ADR-002 fixou a stack frontend fundacional (Next 14 + TS strict + Tailwind + shadcn/ui + Radix + react-hook-form + zod + lucide-react). Durante a implementação do PRD-001, ao desenhar shell de aplicação, dashboard, listagens e auth com qualidade visual real, surgiram necessidades não cobertas pela ADR-002:

- **Motion expressivo** — page transitions, sidebar indicator animado, AnimatePresence em grids de cards. Tailwind animations + `tailwindcss-animate` cobrem keyframes simples mas não orquestração de layout (`layoutId`, `popLayout`).
- **Charts** — dashboard precisa funil de prospecção e KPIs com visualização. Não há lib na stack base.
- **Command palette (Cmd+K)** — padrão moderno (Linear/Notion/Vercel) que precisa de motor de fuzzy match + portal acessível.
- **Toasts** — feedback de sucesso/erro em ações async; shadcn pode usar Radix Toast nativo OU `sonner`.
- **Variants de componente** — buttons/badges com múltiplos variants tipados; `class-variance-authority` é o padrão do shadcn-style.
- **Tema dark/light** — ADR-002 lista "next-intl OR next-themes"; precisa fixar.
- **Data fetching client** — ADR-002 lista TanStack Query; PRD-001 ficou simples o bastante para usar `fetch` cru em `lib/api.ts`. Decidir se mantém ou aciona TanStack agora.

Identidade de marca também não estava decidida em ADR-002 (apenas tokens placeholder em `design-system/README.md`). Brand definida com o usuário durante a sessão.

## Decision

### Brand
- **Cores**: `#C2E000` (lime, primary), `#141414` (ink, foreground), `#FFFFFF` (background)
- **Personalidade**: vibrante criador (Notion-like espaçoso + acento lime)
- **Tipo display**: **Bricolage Grotesque** (Google Fonts, livre, variável) — escolhida como alternativa free a "PP Right Grotesk" (Pangram Pangram, comercial paga não disponível)
- **Tipo body**: **Inter** (Google Fonts, livre)
- **Tipo mono**: **JetBrains Mono** (Google Fonts, livre) — kbd, slugs, hostnames
- **Logo**: wordmark "feat. creators" com period em lime + mark grafema lime sobre quadrado escuro. Componente em `apps/web/components/brand/logo.tsx` com prop `tone: default | invert`.

### Tema
- **`next-themes`** com `attribute="data-theme"` + `enableSystem` + `disableTransitionOnChange`
- Persistência localStorage; respeita `prefers-color-scheme`
- Dark mode é **first-class** (não opcional) — toggle (claro/escuro/sistema) na topbar

### Bibliotecas adicionadas
| Lib | Versão | Uso | Por quê |
|---|---|---|---|
| `next-themes` | latest | dark/light toggle | padrão de fato no ecossistema shadcn |
| `framer-motion` | 11+ | page transitions, sidebar indicator, AnimatePresence em listagens | API mais madura para orquestração de layout; Tailwind animate insuficiente |
| `recharts` | 2.x | charts (funil prospecção, futuro: KPIs temporais) | leve, integração natural com tokens via SVG, suficiente para MVP. Alternativas (Tremor, Visx) overkill ou pesadas |
| `cmdk` | 1.x | command palette ⌘K | a referência da indústria; usado pelo próprio shadcn `command` |
| `sonner` | 2.x | toasts | melhor DX que Radix Toast; rich colors out-of-box; preferido pelo shadcn na "new-york" style |
| `class-variance-authority` | 0.7+ | variants tipados | dependência transitiva do shadcn; explicitada para clareza |
| `clsx` + `tailwind-merge` | latest | `cn()` utility | padrão shadcn |
| `@tanstack/react-table` | 8.x | tabelas densas com sort/filter (a usar futuramente) | instalado mas usado de forma simples por enquanto |

### TanStack Query — adiado
- **Não instalado no PRD-001.** `lib/api.ts` usa `fetch` cru.
- Razão: cada listagem tem 1 endpoint, sem cache cross-page, sem mutations otimistas. Adicionar TanStack agora seria over-engineering.
- **Trigger para adotar**: PRD-002 (prospecção) traz pipeline com mutations frequentes, busca paginada e pre-fetch de detalhes. Migrar `lib/api.ts` para hooks `useQuery`/`useMutation` antes de entregar PRD-002.
- ADR-002 fica desatualizado nesse ponto — este ADR-014 supersede.

### Componentes compostos (em `apps/web/components/app/`)
Estabelecidos durante PRD-001, viram contrato pra PRDs seguintes:
- `AppShell` (sidebar colapsável + topbar + main + Cmd+K)
- `PageHeader` (eyebrow badge + h1 display + description + actions)
- `FilterBar` (search debounced + count + toggle cards/tabela)
- `EntityFormModal` (Dialog reutilizável para create/edit)
- `StatCard` (KPI card com loading/trend/accent)
- `EmptyState` + `EmptyIllustration` (sparkles | inbox | chart)

### Brand assets
- Logo SVG original em `apps/web/assets/logo-feat-creators.svg` (referência)
- Componente React inline em `components/brand/logo.tsx` — não usa o SVG diretamente; reconstrói a marca com tokens HSL para suportar troca de tema sem flicker

## Alternatives considered

1. **Tema: `next-intl`**
   - + parte do framework Next, integração SSR
   - − escopo é i18n, não tema; aplicaríamos para uso errado
   - Descartado: `next-themes` é dedicado e mais maduro

2. **Motion: usar só `tailwindcss-animate`**
   - + zero deps adicionais
   - − `layoutId`, `AnimatePresence`, orquestração de gestos não cobertos
   - Descartado para o nível de polish desejado

3. **Charts: Tremor (Vercel)**
   - + componentes prontos com look bonito
   - − bundle pesado; opinião visual conflita com brand custom (lime)
   - Reavaliar se precisarmos de dashboards complexos (>5 charts)

4. **Charts: Visx (Airbnb)**
   - + extremamente flexível, primitives D3
   - − verboso; over-engineering para 1-2 charts MVP
   - Reavaliar se precisarmos visualizações custom

5. **Command palette: kbar**
   - + também maduro
   - − cmdk integra direto com `components/ui/command` do shadcn

6. **Toasts: Radix Toast (via shadcn)**
   - + zero dep adicional, vai com o pacote shadcn
   - − DX inferior a sonner; APIs com mais boilerplate
   - Sonner é a recomendação atual da própria shadcn

7. **TanStack Query agora**
   - + cache, dedup, retry, optimistic updates
   - + alinha com ADR-002
   - − overkill para 2 listagens simples; adiciona ~13kb gz e mental overhead
   - Adiar até PRD-002

8. **Tipo: pagar PP Right Grotesk** (preferida pelo usuário)
   - + identidade única
   - − licença comercial Pangram Pangram (~$50–$300)
   - − sem urgência no MVP; Bricolage Grotesque entrega vibe similar livre
   - Reavaliar quando tiver receita

## Consequences

**Positivas**:
- UI tem qualidade de produto desde o MVP (motion, dark mode, Cmd+K, charts) — diferenciação real vs. dashboards genéricos do mercado
- Composição padronizada (PageHeader, FilterBar, EntityFormModal) acelera implementação dos PRDs seguintes; cada novo recurso herda o esqueleto de UI
- Brand tokens centralizados em `globals.css` permitem rebrand mudando 6 variáveis CSS

**Negativas**:
- Bundle inicial cresceu: First Load JS de ~88kB (PRD-001 antigo) para ~157kB (login) / ~188kB (listagens) / ~254kB (dashboard com Recharts). Aceitável para app autenticado pós-login; se SEO/landing precisar disso, splitar
- Mais deps para auditar em `npm audit` / Dependabot
- TanStack Query adiado significa retrabalho em `lib/api.ts` quando vier PRD-002

**Riscos**:
- Recharts 2.x pode ficar para trás (manutenção lenta) — alternativa de migração: Visx ou Chart.js. Risco baixo no MVP.
- `cmdk` v1+ é estável mas pequena equipe — se descontinuar, migrar para kbar é trivial (palette é isolado em `components/app/command-palette.tsx`)
- Bricolage Grotesque é uma fonte relativamente nova; se for retirada do Google Fonts (improvável), self-hostar via `next/font/local`

## Impact on specs
- **design-system**: tokens reais documentados, lista de primitives + compostos, padrões de motion (já atualizado no commit `ffa0b8f`)
- **accessibility**: `prefers-reduced-motion` respeitado em globals.css; focus-visible ring com `--ring` lime; Radix garante a11y nos primitives. Pendente: revisão de contraste WCAG AA em ambos os temas antes de produção
- **performance**: monitorar First Load JS; lazy-load Recharts no dashboard se ultrapassar orçamento (pendente: definir orçamento em performance spec)
- **versioning**: package.json é a fonte de verdade; bumps de major em framer-motion/recharts seguem semver-aware

## References
- PRD: `docs/product/01-cadastros-mvp.md` (implementação que motivou)
- ADR-002 (stack base) — supersede parcialmente: TanStack Query adiado, fontes/brand fixados, libs UI complementares adicionadas
- design-system spec: `docs/specs/design-system/README.md`
- Bricolage Grotesque: https://fonts.google.com/specimen/Bricolage+Grotesque
- shadcn/ui new-york style: https://ui.shadcn.com/
- Framer Motion layout animations: https://www.framer.com/motion/layout-animations/
- Recharts: https://recharts.org/
- cmdk: https://cmdk.paco.me/
- sonner: https://sonner.emilkowal.ski/
- next-themes: https://github.com/pacocoursey/next-themes
