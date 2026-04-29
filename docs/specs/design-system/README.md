# Module: Design System — HUB Feat Creator

Tokens visuais, padrões de componente e estratégia de UI.

## Design flow

- [x] **Flow A — Claude Design** (escolha do projeto)
- [ ] Figma
- [ ] Agent flow puro

Razão: solo dev sem designer dedicado. Claude Design gera DS a partir de código + brand assets.

### Como funciona
1. Conectar repo no Claude Design (app.claude.com → Design)
2. Subir brand assets (logo, paleta primária, fontes desejadas) quando definidos
3. Claude Design gera tokens + patterns coerentes com Next + Tailwind
4. Botão "Send to Claude Code" → exporta `docs/design/<slug>-PROMPT.md`
5. `/implement` detecta PROMPT.md → skill `claude-design-handoff` reconcilia com PRD + CLAUDE.md
6. Hierarquia em conflito: **PRD > CLAUDE.md > PROMPT.md**

> Brand assets ainda não definidos. Tokens abaixo são **placeholder funcional** — substituir após handoff.

## Design tokens (placeholder MVP)

Implementação via **Tailwind CSS** (`tailwind.config.ts`) + CSS variables em `:root` para tema runtime-switchable.

### Cores (paleta neutra + acento azul — provisória)

```css
:root {
  /* Brand */
  --color-primary:        #2563eb;  /* blue-600 */
  --color-primary-hover:  #1d4ed8;  /* blue-700 */
  --color-primary-fg:     #ffffff;

  /* Surfaces */
  --color-surface:        #ffffff;
  --color-surface-alt:    #f8fafc;  /* slate-50 */
  --color-surface-muted:  #f1f5f9;  /* slate-100 */
  --color-border:         #e2e8f0;  /* slate-200 */
  --color-border-strong:  #cbd5e1;  /* slate-300 */

  /* Text */
  --color-text:           #0f172a;  /* slate-900 */
  --color-text-muted:     #64748b;  /* slate-500 */
  --color-text-subtle:    #94a3b8;  /* slate-400 */

  /* States */
  --color-success:        #16a34a;  /* green-600 */
  --color-success-bg:     #dcfce7;
  --color-warning:        #d97706;  /* amber-600 */
  --color-warning-bg:     #fef3c7;
  --color-danger:         #dc2626;  /* red-600 */
  --color-danger-bg:      #fee2e2;
  --color-info:           #0284c7;  /* sky-600 */

  /* Focus */
  --ring-color:           #2563eb;
  --ring-offset:          #ffffff;
}

[data-theme="dark"] {
  --color-surface:        #0f172a;
  --color-surface-alt:    #1e293b;
  --color-surface-muted:  #334155;
  --color-border:         #334155;
  --color-text:           #f1f5f9;
  --color-text-muted:     #94a3b8;
  --color-text-subtle:    #64748b;
  /* ...demais ajustes */
}
```

Contrastes mínimos garantem WCAG 2.1 AA (≥ 4.5:1 texto normal, ≥ 3:1 texto grande). Validar via axe + contrast checker antes de release.

### Tipografia

```css
--font-sans:  "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
--font-mono:  "JetBrains Mono", "Fira Code", monospace;

/* size / line-height */
--text-xs:    0.75rem  / 1rem;       /* 12/16 */
--text-sm:    0.875rem / 1.25rem;    /* 14/20 */
--text-base:  1rem     / 1.5rem;     /* 16/24 — body default */
--text-lg:    1.125rem / 1.75rem;    /* 18/28 */
--text-xl:    1.25rem  / 1.75rem;    /* 20/28 */
--text-2xl:   1.5rem   / 2rem;       /* 24/32 */
--text-3xl:   1.875rem / 2.25rem;    /* 30/36 */
--text-4xl:   2.25rem  / 2.5rem;     /* 36/40 */

/* weight */
--font-normal:    400;
--font-medium:    500;
--font-semibold:  600;
--font-bold:      700;
```

Fontes via `next/font/google` (zero CLS, self-hosted automático).

### Espaçamento

```css
--space-0:   0;
--space-1:   0.25rem;  /* 4px */
--space-2:   0.5rem;   /* 8px */
--space-3:   0.75rem;  /* 12px */
--space-4:   1rem;     /* 16px */
--space-5:   1.25rem;  /* 20px */
--space-6:   1.5rem;   /* 24px */
--space-8:   2rem;     /* 32px */
--space-10:  2.5rem;
--space-12:  3rem;
--space-16:  4rem;
--space-20:  5rem;
--space-24:  6rem;
```

### Radii

```css
--radius-sm:   0.25rem;   /* 4px — inputs */
--radius-md:   0.375rem;  /* 6px — botões */
--radius-lg:   0.5rem;    /* 8px — cards */
--radius-xl:   0.75rem;   /* 12px — modais */
--radius-full: 9999px;
```

### Shadows

```css
--shadow-xs:  0 1px 2px rgba(15, 23, 42, 0.05);
--shadow-sm:  0 1px 3px rgba(15, 23, 42, 0.08), 0 1px 2px rgba(15, 23, 42, 0.04);
--shadow-md:  0 4px 6px rgba(15, 23, 42, 0.07), 0 2px 4px rgba(15, 23, 42, 0.04);
--shadow-lg:  0 10px 15px rgba(15, 23, 42, 0.10), 0 4px 6px rgba(15, 23, 42, 0.05);
--shadow-xl:  0 20px 25px rgba(15, 23, 42, 0.12), 0 10px 10px rgba(15, 23, 42, 0.04);
```

## Component library

- [x] **shadcn/ui** + **Radix UI** (primitives)
- Razão: copy-paste, zero runtime extra, total controle de estilo, primitives Radix são acessíveis por padrão
- Localização: `apps/web/components/ui/` (gerados via CLI shadcn)
- Customizações ficam em `apps/web/components/<feature>/` que **compõem** primitives

### Categorias de componente
- **Primitives** (shadcn): Button, Input, Select, Dialog, DropdownMenu, Tooltip, Tabs, Accordion, Toast, Form
- **Composto**: DataTable (TanStack Table + shadcn), DateRangePicker, FileUpload, AvatarStack, EmptyState, ConfirmDialog
- **Domain**: ProspeccaoCard, InfluenciadorAvatar, MarcaBadge, TarefaListItem, EmailTemplateEditor (RichText via Tiptap)

## Layouts

- [x] **Sidebar + content** — main app pós-login
- [x] **Top nav + content** — landing pública e portal influenciador (Fase 2)
- [x] **Dashboard grid** — home assessora
- [x] **Form layouts** — single column (formulários curtos), two column (cadastros longos)
- [x] **Card grid / list view toggle** — listagens com switch
- [x] **Modal / drawer** — modal para ações rápidas; drawer para detalhe lateral

## Responsive breakpoints

```ts
sm:  640px
md:  768px
lg:  1024px
xl:  1280px
2xl: 1536px
```

Estratégia: **mobile-friendly, não mobile-first** — assessoras trabalham primariamente em desktop, mas portal influenciador (Fase 2) precisa ser ótimo em mobile.

## Dark mode

- [x] **Full dark mode com toggle + auto-detect (`prefers-color-scheme`)**
- Implementação: `next-themes` + atributo `data-theme` no `<html>`
- Persistência: localStorage; respeita preferência do sistema por default
- Auditar contraste em ambos os temas

## Iconografia
- **lucide-react** (open source, integrado ao shadcn, ~1k ícones consistentes)
- Tamanhos padrão: 16px (inline), 20px (botão), 24px (header)
- Sempre com `aria-hidden="true"` quando decorativo; `aria-label` quando único conteúdo do botão

## Motion
- Durações: `fast 150ms`, `normal 250ms`, `slow 400ms`
- Easing default: `cubic-bezier(0.4, 0, 0.2, 1)` (ease-in-out tailwind)
- Respeitar `prefers-reduced-motion: reduce` — desabilitar transições não-essenciais
- Library: CSS transitions + Framer Motion para sequências complexas (modais, drawers)

## Feedback ao usuário
- **Toasts**: shadcn `<Toaster />` (Sonner) — sucesso (verde), erro (vermelho), info (azul), warning (âmbar)
- **Skeletons** em listas durante loading inicial
- **Empty states** com call-to-action clara (nunca tela em branco)
- **Error boundaries** por rota com botão "Tentar novamente" + traceId visível

## Convenções de uso (regras para `frontend-agent` quando aplicável)
- **Sempre usar tokens** — nunca hardcodear cor/spacing/font
- Composição > customização — preferir compor primitives a fork
- Acessibilidade: ver `docs/specs/accessibility/`
- Formulários: usar `react-hook-form` + `zod` para validação client; mensagens em pt-BR
- Toda interação > 1s tem feedback visual (spinner, skeleton, toast)
- Toda lista tem estado vazio + estado de erro + paginação
