# Module: Design System — HUB Feat Creator

Tokens visuais, padrões de componente e estratégia de UI.

## Design flow

- [x] **Flow A — Claude Design** (escolha do projeto)
- [ ] Figma
- [x] **Agent flow (frontend-agent)** — em uso até PROMPT.md ser exportado

Razão: solo dev sem designer dedicado. Claude Design (claude.ai/design, plano Pro+) gera DS a partir de código + brand assets. Enquanto não há PROMPT.md, o frontend-agent compõe UI a partir destes tokens + biblioteca de componentes.

### Como funciona (Flow A)
1. Conectar repo no Claude Design (claude.ai → ícone paleta na sidebar)
2. Subir brand assets (logo, paleta primária, fontes desejadas)
3. Claude Design gera tokens + patterns coerentes com Next + Tailwind
4. Botão "Send to Claude Code" → exporta `docs/design/<slug>-PROMPT.md`
5. `/implement` detecta PROMPT.md → skill `claude-design-handoff` reconcilia com PRD + CLAUDE.md
6. Hierarquia em conflito: **PRD > CLAUDE.md > PROMPT.md**

## Brand

- **Cores brand**: `#C2E000` (lime/primary) + `#141414` (ink/foreground) + `#FFFFFF` (background)
- **Personalidade**: vibrante criador (Notion-like espaçoso + acento lime expressivo)
- **Logo**: `apps/web/components/brand/logo.tsx` — variants `full | mark | wordmark`, prop `tone: 'default' | 'invert'` para uso sobre fundo escuro

## Design tokens (implementação real)

Implementação via **Tailwind CSS** (`apps/web/tailwind.config.ts`) + CSS variables em `:root` (`apps/web/app/globals.css`) com tema runtime-switchable via `next-themes` (atributo `data-theme="dark"`).

> Cores armazenadas como **componentes HSL sem `hsl()` wrapper** (ex: `--primary: 68 100% 44%`); o `tailwind.config.ts` aplica `hsl(var(--primary))`.

### Cores — light

```css
:root {
  /* Surfaces */
  --background:        0 0% 100%;     /* #FFFFFF */
  --foreground:        0 0% 8%;       /* #141414 */
  --card:              0 0% 100%;
  --popover:           0 0% 100%;
  --elevated:          0 0% 99%;

  /* Brand — lime */
  --primary:           68 100% 44%;   /* #C2E000 */
  --primary-foreground: 0 0% 8%;
  --primary-hover:     68 100% 38%;

  /* Secondary */
  --secondary:         60 5% 96%;
  --muted:             60 5% 96%;
  --muted-foreground:  25 5% 35%;
  --accent:            60 5% 95%;

  /* States */
  --destructive:       0 84% 55%;
  --success:           142 71% 38%;
  --warning:           38 92% 50%;

  /* Borders + focus */
  --border:            30 6% 90%;
  --border-strong:     30 6% 82%;
  --input:             30 6% 90%;
  --ring:              68 100% 44%;

  --radius:            0.625rem;
}
```

### Cores — dark

```css
[data-theme='dark'] {
  --background:        0 0% 8%;       /* #141414 */
  --foreground:        60 9% 98%;     /* near white */
  --card:              0 0% 11%;
  --popover:           0 0% 11%;
  --elevated:          0 0% 12%;

  --primary:           68 100% 44%;   /* lime mantém */
  --primary-foreground: 0 0% 8%;
  --primary-hover:     68 100% 50%;

  --secondary:         0 0% 14%;
  --muted:             0 0% 14%;
  --muted-foreground:  30 5% 65%;
  --accent:            0 0% 16%;

  --destructive:       0 70% 55%;
  --success:           142 65% 45%;
  --warning:           38 92% 55%;

  --border:            0 0% 16%;
  --border-strong:     0 0% 22%;
  --input:             0 0% 16%;
  --ring:              68 100% 44%;
}
```

Contraste ≥ 4.5:1 (texto normal) e ≥ 3:1 (texto grande), conforme WCAG 2.1 AA. Texto `#141414` sobre primary `#C2E000` ≈ 12:1 (AAA). Validar com axe + contrast checker antes de release.

### Tipografia

```css
--font-sans:     "Inter", system-ui, sans-serif;       /* body */
--font-display:  "Bricolage Grotesque", "Inter", sans; /* headings, display */
--font-mono:     "JetBrains Mono", monospace;          /* slugs, keyboard kbd, code */
```

Carregadas via `next/font/google` em `apps/web/app/layout.tsx` — zero CLS, self-hosted automático. Variables `--font-inter`, `--font-bricolage`, `--font-jetbrains` aplicadas no `<html>` e mapeadas no `tailwind.config.ts`.

Hierarquia:
- `font-display` + `tracking-tight` para `h1`, `h2`, títulos de Card e KPIs
- `font-sans` para body
- `font-mono` para slugs, kbd, hostnames, IDs

### Espaçamento

Mantém escala Tailwind nativa (4px base). Padrão da aplicação:
- gap horizontal padrão: `gap-6` (24px)
- seções verticais: `space-y-12` (48px) ou `mt-8`
- max-width de conteúdo: `max-w-7xl` (1280px) com `px-4 md:px-8`
- container vertical de página: `py-8 md:py-12`

### Radii

```ts
--radius: 0.625rem;       /* 10px — base */
sm:  calc(var(--radius) - 4px)  // ~6px — inputs, badges
md:  calc(var(--radius) - 2px)  // ~8px — botões
lg:  var(--radius)              // 10px — cards
xl:  calc(var(--radius) + 4px)  // ~14px — modais, hero
full: 9999px                    // pills, avatars
```

### Shadows

Sombras tinted via `--shadow` (light: `0 0% 8%`, dark: `0 0% 0%`):
- `shadow-xs` — bordas sutis em inputs/cards
- `shadow-sm` — cards default
- `shadow-md` — hover de card
- `shadow-lg` — popovers, dropdowns
- `shadow-xl` — modals, drawers
- `shadow-glow` — ring lime para destaque (`0 0 0 4px hsl(var(--primary) / 0.20)`)

## Component library

- [x] **shadcn/ui** (style "new-york") + **Radix UI** primitives — cópia local, sem runtime
- Localização: `apps/web/components/ui/`
- Composições: `apps/web/components/app/` (shell, page-header, filter-bar, entity-form-modal, stat-card, empty-state)
- Brand: `apps/web/components/brand/`

### Primitives instalados
Button, Input, Label, Dialog, Sheet, DropdownMenu, Tooltip, Tabs, Popover, Command (cmdk), Switch, Skeleton, Badge, Card, Separator, Avatar.

### Compostos
- `AppShell` — sidebar colapsável + topbar + main + Cmd+K palette
- `Sidebar` — nav com ícones Lucide, collapse persistido em localStorage, indicator animado via framer-motion `layoutId`
- `Topbar` — breadcrumb dinâmico + theme toggle + user dropdown
- `CommandPalette` — Cmd+K / Ctrl+K, grupos: Navegar, Criar, Tema, Conta
- `PageHeader` — eyebrow badge + h1 display + description + actions
- `FilterBar` — search com debounce + count + toggle cards/tabela
- `EntityFormModal` — Dialog reutilizável para create/edit (slot de fields)
- `StatCard` — KPI card com loading skeleton, trend, accent variant
- `EmptyState` + `EmptyIllustration` (sparkles/inbox/chart) — estados vazios ilustrados

### Domain (futuro)
- `ProspeccaoCard`, `InfluenciadorAvatar`, `MarcaBadge`, `TarefaListItem`, `EmailTemplateEditor` (Tiptap)

## Layouts implementados
- [x] **Sidebar + content** — `app/(app)/layout.tsx` via `AppShell`
- [x] **Hero duo-painel** — `app/(auth)/layout.tsx` (hero esquerdo escuro com gradient mesh lime + form direito)
- [x] **Dashboard grid** — `app/(app)/page.tsx` com 4 StatCards + 2 cards principais
- [x] **Cards grid / table view toggle** — listagens com `FilterBar`
- [x] **Modal** — Dialog (shadcn) para create/edit; **Drawer** (Sheet `side="right"`) para detalhe lateral
- [x] **Mobile** — sidebar vira `Sheet side="left"` acionado por hambúrguer no Topbar

## Responsive breakpoints

```ts
sm:  640px
md:  768px
lg:  1024px
xl:  1280px
2xl: 1400px
```

Estratégia: **mobile-friendly, desktop-first**. Sidebar oculta em `< md`, vira drawer. Cards stack em 1 coluna no mobile, 2 em sm, 3 em lg.

## Dark mode

- [x] **Full dark mode com toggle (claro/escuro/sistema) + auto-detect**
- Implementação: `next-themes` com `attribute="data-theme"` + `enableSystem`
- Persistência: localStorage; respeita `prefers-color-scheme` por default
- `disableTransitionOnChange` ativo para evitar flash em troca de tema

## Iconografia
- **lucide-react** — tamanhos padrão: 16px (inline), 20px (botão), 24px (header)
- ⚠️ Ícone `Instagram` foi removido do lucide-react — usar `AtSign`
- Sempre `aria-hidden="true"` quando decorativo; `aria-label` quando único conteúdo do botão

## Motion
- Library: **Framer Motion** + Tailwind CSS animations + `tailwindcss-animate`
- Durações: 150ms (fast), 250ms (normal), 400ms (slow)
- Easing default: `cubic-bezier(0.4, 0, 0.2, 1)`
- `prefers-reduced-motion: reduce` desabilita transições não-essenciais (override em `globals.css` `@media`)
- Padrões:
  - Page transitions: `<motion.main>` fade+slide 250ms (em AppShell)
  - Card grids: `AnimatePresence mode="popLayout"` com stagger leve
  - Sidebar active indicator: `layoutId` para animar entre itens
  - Forms: fade-in-up entrada
  - Skeletons: shimmer via custom keyframe

## Feedback ao usuário
- **Toasts**: `sonner` (`<Toaster position="bottom-right" richColors closeButton />`) — cores por variant via `richColors`
- **Skeletons**: shimmer escuro/claro automaticamente conforme tema
- **Empty states**: `EmptyState` + `EmptyIllustration` (variants `sparkles | inbox | chart`)
- **Error boundaries**: a fazer — componente `<ErrorBoundary />` com botão "Tentar novamente" + `traceId`

## Charts
- Library: **Recharts** (lib leve, suficiente pra MVP)
- Tooltip estilizado pelos tokens (`background: hsl(var(--popover))`, etc.)
- Gradient `<defs>` para barras com transição lime suave
- Grid sutil via `hsl(var(--border))`

## Convenções de uso
- **Sempre tokens** — nunca hardcodear cor/spacing/font; sempre `hsl(var(--token))` ou classes Tailwind mapeadas
- Composição > customização — preferir compor primitives a fork
- Acessibilidade: ver `docs/specs/accessibility/`
- Formulários: `react-hook-form` + `zod` para validação client (a integrar — atualmente form state local nas páginas mais simples); mensagens em pt-BR
- Toda interação > 1s tem feedback visual (Loader2 spinner ou skeleton)
- Toda lista tem estado vazio + estado de erro + (futuro) paginação
- Ícones em botões com texto: padrão `[icon] [label]` (ícone à esquerda)

## Stack de dependências (web)
| Pkg | Uso |
|---|---|
| `next` 14.2 | App Router, typedRoutes |
| `react`/`react-dom` 18.3 | core |
| `tailwindcss` 3.4 + `tailwindcss-animate` | styling |
| `next-themes` | dark mode |
| `framer-motion` | motion |
| `sonner` | toasts |
| `lucide-react` | ícones |
| `@radix-ui/*` | primitives |
| `cmdk` | command palette |
| `class-variance-authority` + `clsx` + `tailwind-merge` | variants e cn() |
| `react-hook-form` + `zod` + `@hookform/resolvers` | forms |
| `recharts` | charts |
| `@tanstack/react-table` | data tables (a integrar nas listagens) |
