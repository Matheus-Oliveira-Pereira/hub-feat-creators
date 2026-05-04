package com.hubfeatcreators.domain.email;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTemplateService {

    private static final PolicyFactory HTML_POLICY =
            new HtmlPolicyBuilder()
                    .allowElements(
                            "a", "b", "br", "div", "em", "h1", "h2", "h3", "hr", "i", "img", "li",
                            "ol", "p", "span", "strong", "table", "tbody", "td", "tfoot", "th",
                            "thead", "tr", "u", "ul")
                    .allowUrlProtocols("http", "https", "mailto")
                    .allowAttributes("href")
                    .onElements("a")
                    .allowAttributes("src", "alt", "width", "height")
                    .onElements("img")
                    .allowAttributes(
                            "style",
                            "class",
                            "align",
                            "bgcolor",
                            "border",
                            "cellpadding",
                            "cellspacing",
                            "width",
                            "height",
                            "colspan",
                            "rowspan")
                    .globally()
                    .toFactory();

    private final EmailTemplateRepository repo;
    private final EmailLayoutRepository layoutRepo;
    private final MustacheFactory mustache = new DefaultMustacheFactory();

    public EmailTemplateService(EmailTemplateRepository repo, EmailLayoutRepository layoutRepo) {
        this.repo = repo;
        this.layoutRepo = layoutRepo;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> listar(AuthPrincipal principal) {
        return repo.findByAssessoriaId(principal.assessoriaId());
    }

    @Transactional(readOnly = true)
    public EmailTemplate buscar(AuthPrincipal principal, UUID id) {
        return repo.findByIdAndAssessoriaId(id, principal.assessoriaId())
                .orElseThrow(() -> BusinessException.notFound("EMAIL_TEMPLATE"));
    }

    @Transactional
    public EmailTemplate criar(
            AuthPrincipal principal,
            String nome,
            String assunto,
            String corpoHtml,
            String corpoTexto,
            String[] variaveis) {
        String sanitized = HTML_POLICY.sanitize(corpoHtml);
        EmailTemplate t = new EmailTemplate(principal.assessoriaId(), nome, assunto, sanitized);
        t.setCorpoTexto(corpoTexto);
        t.setVariaveisDeclararadas(variaveis != null ? variaveis : new String[0]);
        return repo.save(t);
    }

    @Transactional
    public EmailTemplate atualizar(
            AuthPrincipal principal,
            UUID id,
            String nome,
            String assunto,
            String corpoHtml,
            String corpoTexto,
            String[] variaveis) {
        EmailTemplate t = buscar(principal, id);
        if (nome != null) t.setNome(nome);
        if (assunto != null) t.setAssunto(assunto);
        if (corpoHtml != null) t.setCorpoHtml(HTML_POLICY.sanitize(corpoHtml));
        if (corpoTexto != null) t.setCorpoTexto(corpoTexto);
        if (variaveis != null) t.setVariaveisDeclararadas(variaveis);
        t.setUpdatedAt(Instant.now());
        return repo.save(t);
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        EmailTemplate t = buscar(principal, id);
        t.setDeletedAt(Instant.now());
        repo.save(t);
    }

    /**
     * Renderiza template + layout com variáveis fornecidas. Variável nula ou ausente → string vazia
     * (AC-4).
     */
    public String renderizar(
            UUID assessoriaId,
            EmailTemplate template,
            Map<String, Object> vars,
            String unsubscribeUrl) {
        String corpoRendered = render(template.getCorpoHtml(), vars);

        EmailLayout layout =
                layoutRepo.findByAssessoriaId(assessoriaId).orElse(new EmailLayout(assessoriaId));

        Map<String, Object> layoutVars =
                Map.of("unsubscribe_url", unsubscribeUrl != null ? unsubscribeUrl : "#");
        String header = render(layout.getHeaderHtml(), layoutVars);
        String footer = render(layout.getFooterHtml(), layoutVars);

        return header + corpoRendered + footer;
    }

    /** Preview com unsubscribe_url mock. */
    public String preview(UUID assessoriaId, EmailTemplate template, Map<String, Object> vars) {
        return renderizar(assessoriaId, template, vars, "https://example.com/unsubscribe/preview");
    }

    private String render(String template, Map<String, Object> vars) {
        try {
            Mustache m = mustache.compile(new StringReader(template), "tpl-" + UUID.randomUUID());
            StringWriter w = new StringWriter();
            m.execute(w, vars).flush();
            return w.toString();
        } catch (Exception e) {
            return template;
        }
    }
}
