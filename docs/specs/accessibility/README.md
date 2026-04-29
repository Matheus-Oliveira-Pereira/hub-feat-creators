# Module: Accessibility — HUB Feat Creator

Acessibilidade digital, WCAG e inclusão.

## Conformidade alvo

- [x] **WCAG 2.1 Level AA**
- [ ] WCAG 2.2 AA (avaliar adoção em 2026)
- [ ] AAA — não obrigatório, mas perseguir em fluxos críticos quando viável

Cumprimento de **eMAG** (modelo brasileiro) também — alinhado com WCAG 2.1 AA, é referência adicional para sites públicos no Brasil.

## POUR — checklist obrigatório

### Perceptível
- [x] `alt` em imagens informativas (descrição funcional, não "imagem de...")
- [x] Imagens decorativas: `alt=""` + `role="presentation"`
- [x] Vídeos (futuro): legendas + transcrição
- [x] Ícones-only buttons: `aria-label` obrigatório
- [x] Contraste mínimo: **4.5:1** (texto normal), **3:1** (texto grande ≥ 18pt ou 14pt bold), **3:1** (UI components e foco)
- [x] Conteúdo não depende **só de cor** — sempre texto/ícone reforçando (ex: status com texto + cor + ícone)
- [x] Reflow em 320px sem perda de informação (responsivo)
- [x] Zoom 200% sem quebrar layout

### Operável
- [x] **Toda funcionalidade via teclado** — sem dependência de mouse
- [x] Sem keyboard traps (Esc fecha modal, Tab nunca fica preso)
- [x] **Skip link** "Pular para conteúdo principal" no topo de cada página
- [x] **Foco visível** em **todo** elemento interativo (outline ≥ 2px, contraste ≥ 3:1 com fundo)
- [x] Tab order lógico (segue ordem visual)
- [x] Timeouts (sessão) configuráveis ou avisam usuário com 1 min de antecedência + opção de estender
- [x] Animações respeitam `prefers-reduced-motion`

### Compreensível
- [x] `<html lang="pt-BR">` em todas páginas
- [x] **Labels em todo input** (visíveis ou via `aria-label`); placeholder **não** substitui label
- [x] Mensagem de erro descritiva e próxima ao campo (não só vermelho — texto explicando o problema)
- [x] Validação em tempo real para campos críticos, mas não a cada keystroke (debounce + on blur)
- [x] Navegação consistente entre páginas (sidebar/header não muda de posição)
- [x] Identificação consistente de ícones/ações (mesmo ícone = mesma ação em todo app)

### Robusto
- [x] HTML semântico: `<nav>`, `<main>`, `<header>`, `<footer>`, `<article>`, `<section>`, `<aside>`, `<button>` (não `<div onclick>`)
- [x] ARIA somente quando HTML semântico não basta — primeira regra de ARIA: "não use ARIA"
- [x] Testado com leitor de tela: **NVDA** (Windows, principal — público BR maioria Windows) + **VoiceOver** (macOS/iOS)
- [x] Testado em navegação só por teclado em todo fluxo crítico (login, criar prospecção, enviar e-mail, concluir tarefa)

## Componentes — regras

### Botões
- `<button>` para ações; `<a>` para navegação. Nunca `<div>`/`<span>` clicável
- Botão com só ícone: `aria-label` obrigatório
- Estado disabled: `disabled` + `aria-disabled="true"` + tooltip explicando porquê (quando útil)
- Loading: `aria-busy="true"` + texto live region "Carregando..." para SR

### Modais / dialogs
- Componente Radix `<Dialog>` (já implementa pattern correto)
- Foco move para o modal ao abrir; foco volta ao trigger ao fechar
- Esc fecha
- Foco fica preso (focus trap) enquanto aberto
- `aria-labelledby` aponta para título do modal
- Fundo: `aria-hidden="true"` no main quando modal aberto

### Formulários
- `<label for="...">` ou wrapping `<label>` em todo `<input>`
- `<fieldset>` + `<legend>` para grupos relacionados (ex: "Endereço")
- Erros: `aria-invalid="true"` + `aria-describedby` apontando para mensagem
- `required` HTML + indicador visual (asterisco com `aria-label="obrigatório"`)
- Validação on submit + indicação clara dos campos com erro (lista de erros no topo + foco no primeiro)

### Tabelas (data tables)
- `<table>` com `<thead>`, `<tbody>`, `<th scope="col">` ou `scope="row">`
- Caption ou `aria-label` descrevendo a tabela
- Ordenação: `aria-sort="ascending|descending|none"`
- Em mobile: transformar em cards (não scroll horizontal infinito)

### Notificações dinâmicas
- `aria-live="polite"` para info (toasts não-críticos)
- `aria-live="assertive"` ou `role="alert"` para erros críticos
- Toasts não desaparecem antes de SR ler (mínimo 5s + opção de pausar)

### Navegação
- `<nav aria-label="Principal">` para sidebar
- Item ativo: `aria-current="page"`
- Breadcrumb: `<nav aria-label="Caminho">` + `<ol>`

## Tools

### Automatizada
- **axe-core** integrado em testes Playwright E2E — falha se nova violação introduzida
- **Lighthouse CI** roda em PRs — score a11y mínimo: **95**
- ESLint plugin `eslint-plugin-jsx-a11y` — falha em CI

### Manual
- **Keyboard-only**: testar fluxo crítico só com Tab/Shift+Tab/Enter/Esc/setas
- **Screen reader**: NVDA (principal) trimestral em fluxos críticos
- **Zoom 200%**: verificar reflow
- **High contrast mode** Windows: layout não quebra
- **Dark mode**: contrastes mantidos

### CI gate
PR não merge se:
- axe-core encontrou violação `serious` ou `critical` em rota nova
- Lighthouse a11y < 95
- ESLint a11y plugin com erro

## Checklist por release
- [ ] Auditoria axe-core completa (CLI ou DevTools)
- [ ] Lighthouse a11y em 5 rotas-chave
- [ ] Smoke test keyboard-only nos fluxos críticos
- [ ] Smoke test NVDA em login + criar prospecção (uma vez por release)
- [ ] Verificar contrastes após qualquer mudança de token de cor

## Política
- Bug de a11y reportado por usuário = **prioridade máxima** dentro da categoria (mesmo nível que bug funcional)
- Toda nova feature passa por checklist a11y antes de merge — incluído no template de PR
- Componentes do design system são revisados em release de design system

## Referências
- WCAG 2.1 — https://www.w3.org/TR/WCAG21/
- ARIA Authoring Practices — https://www.w3.org/WAI/ARIA/apg/
- eMAG (Brasil) — https://emag.governoeletronico.gov.br/
- Radix UI accessibility — https://www.radix-ui.com/primitives/docs/overview/accessibility
