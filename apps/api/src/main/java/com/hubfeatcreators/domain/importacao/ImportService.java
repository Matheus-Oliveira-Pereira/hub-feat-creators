package com.hubfeatcreators.domain.importacao;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.compliance.BaseLegal;
import com.hubfeatcreators.domain.contato.Contato;
import com.hubfeatcreators.domain.contato.ContatoRepository;
import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.domain.marca.Marca;
import com.hubfeatcreators.domain.marca.MarcaRepository;
import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final int MAX_LINHAS = 10_000;
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int CHUNK_SIZE = 500;

    private final ImportJobRepository jobRepo;
    private final ImportJobLinhaRepository linhaRepo;
    private final ImportTemplateRepository templateRepo;
    private final CsvXlsxParser parser;
    private final AppProperties props;
    private final JdbcTemplate jdbc;
    private final InfluenciadorRepository infRepo;
    private final MarcaRepository marcaRepo;
    private final ContatoRepository contatoRepo;
    private final JobService jobService;

    public ImportService(
            ImportJobRepository jobRepo,
            ImportJobLinhaRepository linhaRepo,
            ImportTemplateRepository templateRepo,
            CsvXlsxParser parser,
            AppProperties props,
            JdbcTemplate jdbc,
            InfluenciadorRepository infRepo,
            MarcaRepository marcaRepo,
            ContatoRepository contatoRepo,
            JobService jobService) {
        this.jobRepo = jobRepo;
        this.linhaRepo = linhaRepo;
        this.templateRepo = templateRepo;
        this.parser = parser;
        this.props = props;
        this.jdbc = jdbc;
        this.infRepo = infRepo;
        this.marcaRepo = marcaRepo;
        this.contatoRepo = contatoRepo;
        this.jobService = jobService;
    }

    // ---- Upload ---

    @Transactional
    public ImportJob upload(AuthPrincipal principal, MultipartFile file, String entidade) {
        validateFeatureEnabled();
        validateEntidade(entidade);

        if (file.getSize() > MAX_BYTES) {
            throw BusinessException.badRequest("IMPORT_FILE_TOO_LARGE", "Arquivo excede 5MB.");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        if (!isSupportedExtension(filename)) {
            throw BusinessException.badRequest("IMPORT_FORMAT_UNSUPPORTED", "Formato não suportado. Use CSV ou XLSX.");
        }

        Path uploadDir = ensureUploadDir();
        String storedName = UUID.randomUUID() + "_" + filename;
        Path dest = uploadDir.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + e.getMessage(), e);
        }

        // Probe to estimate total rows
        CsvXlsxParser.ParseResult probe;
        try {
            probe = parser.probe(dest);
        } catch (IOException e) {
            silentDelete(dest);
            throw BusinessException.badRequest("IMPORT_PARSE_ERROR", "Erro ao ler arquivo: " + e.getMessage());
        }

        if (probe.estimatedTotal() > MAX_LINHAS) {
            silentDelete(dest);
            throw BusinessException.badRequest("IMPORT_TOO_MANY_ROWS",
                    "Arquivo excede " + MAX_LINHAS + " linhas.");
        }

        ImportJob job = new ImportJob(
                principal.assessoriaId(),
                principal.usuarioId(),
                entidade,
                dest.toString(),
                filename);
        job.setTotalLinhas(probe.estimatedTotal());
        return jobRepo.save(job);
    }

    // ---- Probe (return headers + auto-suggestions) ---

    @Transactional(readOnly = true)
    public ProbeResponse probe(AuthPrincipal principal, UUID jobId) {
        ImportJob job = requireJob(principal, jobId);
        try {
            CsvXlsxParser.ParseResult r = parser.probe(Paths.get(job.getArquivoPath()));
            List<ColumnSuggestion> suggestions = buildSuggestions(r.headers(), job.getEntidade());
            return new ProbeResponse(List.of(r.headers()), r.encoding(), r.separator(), suggestions);
        } catch (IOException e) {
            throw BusinessException.badRequest("IMPORT_PARSE_ERROR", e.getMessage());
        }
    }

    public record ProbeResponse(
            List<String> headers,
            String encoding,
            char separator,
            List<ColumnSuggestion> suggestions) {}

    public record ColumnSuggestion(String csvHeader, String suggestedField, int distance) {}

    private List<ColumnSuggestion> buildSuggestions(String[] headers, String entidade) {
        List<String> fields = entityFields(entidade);
        List<ColumnSuggestion> out = new ArrayList<>();
        for (String h : headers) {
            String best = null;
            int bestDist = Integer.MAX_VALUE;
            for (String f : fields) {
                int d = ImportValidator.levenshtein(h, f);
                if (d < bestDist) {
                    bestDist = d;
                    best = f;
                }
            }
            if (best != null && bestDist < 3) {
                out.add(new ColumnSuggestion(h, best, bestDist));
            } else {
                out.add(new ColumnSuggestion(h, null, bestDist));
            }
        }
        return out;
    }

    private List<String> entityFields(String entidade) {
        return switch (entidade) {
            case "INFLUENCIADOR" -> List.of("nome", "email", "telefone", "instagram", "youtube",
                    "tiktok", "nicho", "audiencia_total", "observacoes", "cpf");
            case "MARCA" -> List.of("nome", "cnpj", "segmento", "site", "observacoes");
            case "CONTATO" -> List.of("nome", "email", "telefone", "cargo", "marca_id");
            default -> List.of();
        };
    }

    // ---- Set mapeamento ---

    @Transactional
    public ImportJob setMapeamento(
            AuthPrincipal principal,
            UUID jobId,
            Map<String, String> mapeamento,
            String baseLegal,
            String dedupStrategy) {

        ImportJob job = requireJob(principal, jobId);
        requireStatus(job, "UPLOADADO", "PRONTO_DRY_RUN");

        validateBaseLegal(baseLegal);
        validateDedupStrategy(dedupStrategy);

        job.setMapeamento(mapeamento);
        job.setBaseLegal(baseLegal);
        job.setDedupStrategy(dedupStrategy);
        job.setStatus("UPLOADADO");
        job.setUpdatedAt(Instant.now());

        linhaRepo.deleteByJobId(jobId);

        return jobRepo.save(job);
    }

    // ---- Dry-run ---

    @Transactional
    public ImportJob dryRun(AuthPrincipal principal, UUID jobId) {
        ImportJob job = requireJob(principal, jobId);
        requireStatus(job, "UPLOADADO");
        requireMapeamento(job);

        job.setStatus("VALIDANDO");
        job.setUpdatedAt(Instant.now());
        jobRepo.save(job);

        linhaRepo.deleteByJobId(jobId);

        List<ImportJobLinha> batch = new ArrayList<>();
        int[] counters = {0, 0, 0}; // total, ok, erro

        try {
            parser.stream(
                    Paths.get(job.getArquivoPath()),
                    job.getMapeamento(),
                    detectEncoding(job.getArquivoPath()),
                    detectSeparator(job.getArquivoPath()),
                    (rowIdx, row) -> {
                        counters[0]++;
                        List<String> erros = validateRow(job.getEntidade(), row);
                        String status = erros.isEmpty() ? "OK" : "ERRO";
                        if (erros.isEmpty()) counters[1]++;
                        else counters[2]++;

                        batch.add(new ImportJobLinha(jobId, rowIdx, status, null, erros));

                        if (batch.size() >= CHUNK_SIZE) {
                            batchInsertLinhas(batch);
                            batch.clear();
                        }
                    });

            if (!batch.isEmpty()) {
                batchInsertLinhas(batch);
                batch.clear();
            }

            job.setStatus("PRONTO_DRY_RUN");
            job.setTotalLinhas(counters[0]);
            job.setSucesso(0);
            job.setFalha(0);
            job.setProcessadas(0);

        } catch (Exception e) {
            job.setStatus("FALHOU");
            log.error("Dry-run falhou jobId={}", jobId, e);
        }

        job.setUpdatedAt(Instant.now());
        return jobRepo.save(job);
    }

    @Transactional(readOnly = true)
    public Page<ImportJobLinha> listLinhas(AuthPrincipal principal, UUID jobId, int page, int size) {
        requireJob(principal, jobId);
        return linhaRepo.findByIdJobId(jobId, PageRequest.of(page, size));
    }

    // ---- Execute ---

    @Transactional
    public ImportJob execute(AuthPrincipal principal, UUID jobId) {
        ImportJob job = requireJob(principal, jobId);
        requireStatus(job, "PRONTO_DRY_RUN");

        if (jobRepo.countExecutandoByAssessoria(principal.assessoriaId()) >= 5) {
            throw BusinessException.tooManyRequests("IMPORT_CONCURRENT_TENANT",
                    "Limite de 5 imports simultâneos por assessoria atingido.");
        }
        if (jobRepo.countExecutandoByUsuario(principal.usuarioId()) >= 1) {
            throw BusinessException.tooManyRequests("IMPORT_CONCURRENT_USER",
                    "Aguarde o import em andamento concluir.");
        }

        UUID batchId = UUID.randomUUID();
        job.setStatus("EXECUTANDO");
        job.setIniciadoEm(Instant.now());
        job.setProcessadas(0);
        job.setSucesso(0);
        job.setFalha(0);
        job.setCancelableBatchId(batchId);
        job.setUpdatedAt(Instant.now());
        ImportJob saved = jobRepo.save(job);

        jobService.enqueue(
                principal.assessoriaId(),
                "IMPORT_BULK",
                Map.of("importJobId", saved.getId().toString()),
                batchId);

        return saved;
    }

    // ---- Cancel ---

    @Transactional
    public ImportJob cancel(AuthPrincipal principal, UUID jobId) {
        ImportJob job = requireJob(principal, jobId);

        if (!List.of("UPLOADADO", "VALIDANDO", "PRONTO_DRY_RUN", "EXECUTANDO").contains(job.getStatus())) {
            throw BusinessException.unprocessable("IMPORT_CANNOT_CANCEL",
                    "Job em status " + job.getStatus() + " não pode ser cancelado.");
        }

        // Soft-delete entities created by this job
        List<ImportJobLinha> criadas = linhaRepo.findByJobIdAndStatus(jobId, "OK");
        softDeleteCreated(job.getEntidade(), criadas);

        job.setStatus("CANCELADO");
        job.setConcluidoEm(Instant.now());
        job.setUpdatedAt(Instant.now());
        return jobRepo.save(job);
    }

    private void softDeleteCreated(String entidade, List<ImportJobLinha> criadas) {
        List<UUID> ids = criadas.stream()
                .map(ImportJobLinha::getEntidadeId)
                .filter(id -> id != null)
                .toList();
        if (ids.isEmpty()) return;

        Instant now = Instant.now();
        switch (entidade) {
            case "INFLUENCIADOR" -> ids.forEach(id ->
                    infRepo.findById(id).ifPresent(inf -> { inf.setDeletedAt(now); infRepo.save(inf); }));
            case "MARCA" -> ids.forEach(id ->
                    marcaRepo.findById(id).ifPresent(m -> { m.setDeletedAt(now); marcaRepo.save(m); }));
            case "CONTATO" -> ids.forEach(id ->
                    contatoRepo.findById(id).ifPresent(c -> { c.setDeletedAt(now); contatoRepo.save(c); }));
        }
    }

    // ---- Relatório CSV ---

    public void streamRelatorio(AuthPrincipal principal, UUID jobId, PrintWriter writer) {
        ImportJob job = requireJob(principal, jobId);

        writer.println("linha,status,entidade_id,erros");
        linhaRepo.findByIdJobId(jobId, PageRequest.of(0, Integer.MAX_VALUE)).forEach(l -> {
            String erros = l.getErros() == null ? "" : String.join("|", l.getErros());
            writer.printf("%d,%s,%s,\"%s\"%n",
                    l.getLinha(),
                    l.getStatus(),
                    l.getEntidadeId() != null ? l.getEntidadeId() : "",
                    erros.replace("\"", "\"\""));
        });
    }

    // ---- Status ---

    @Transactional(readOnly = true)
    public ImportJob get(AuthPrincipal principal, UUID jobId) {
        return requireJob(principal, jobId);
    }

    @Transactional(readOnly = true)
    public Page<ImportJob> list(AuthPrincipal principal, int page, int size) {
        return jobRepo.findByAssessoriaIdOrderByCreatedAtDesc(
                principal.assessoriaId(), PageRequest.of(page, size));
    }

    // ---- Templates ---

    @Transactional
    public ImportTemplate saveTemplate(
            AuthPrincipal principal, String nome, String entidade, Map<String, String> mapeamento) {
        validateEntidade(entidade);
        return templateRepo.save(
                new ImportTemplate(principal.assessoriaId(), nome, entidade, mapeamento));
    }

    @Transactional(readOnly = true)
    public List<ImportTemplate> listTemplates(AuthPrincipal principal, String entidade) {
        if (entidade != null) {
            return templateRepo.findByAssessoriaIdAndEntidadeAndDeletedAtIsNull(
                    principal.assessoriaId(), entidade);
        }
        return templateRepo.findByAssessoriaIdAndDeletedAtIsNull(principal.assessoriaId());
    }

    @Transactional
    public void deleteTemplate(AuthPrincipal principal, UUID templateId) {
        ImportTemplate t = templateRepo
                .findByIdAndAssessoriaIdAndDeletedAtIsNull(templateId, principal.assessoriaId())
                .orElseThrow(BusinessException::notFound);
        t.setDeletedAt(Instant.now());
        templateRepo.save(t);
    }

    // ---- Internal used by ImportJobHandler ---

    public void executeChunk(UUID jobId, List<ImportJobLinha> linhas, String entidade,
            UUID assessoriaId, UUID usuarioId, String baseLegal, String dedupStrategy) {

        ImportJob job = jobRepo.findById(jobId).orElseThrow();
        if ("CANCELADO".equals(job.getStatus())) return;

        List<ImportJobLinha> results = new ArrayList<>();
        int ok = 0, falha = 0;

        for (ImportJobLinha linha : linhas) {
            // entidade_id is null at this point; row data comes from reparse in handler
            results.add(linha);
            if ("OK".equals(linha.getStatus())) ok++;
            else falha++;
        }

        if (!results.isEmpty()) {
            batchInsertLinhas(results);
        }

        job.setProcessadas(job.getProcessadas() + linhas.size());
        job.setSucesso(job.getSucesso() + ok);
        job.setFalha(job.getFalha() + falha);
        job.setUpdatedAt(Instant.now());
        jobRepo.save(job);
    }

    public UUID persistInfluenciador(
            Map<String, String> row, UUID assessoriaId, UUID usuarioId,
            String baseLegal, String dedupStrategy) {

        String nome = row.getOrDefault("nome", "");
        String email = row.get("email");
        String instagram = row.get("instagram");

        // Dedup check
        Optional<Influenciador> existing = Optional.empty();
        if (instagram != null && !instagram.isBlank()) {
            existing = infRepo.findByHandleAndAssessoria(assessoriaId, "instagram", instagram);
        }
        if (existing.isEmpty() && email != null && !email.isBlank()) {
            existing = infRepo.findByHandleAndAssessoria(assessoriaId, "email", email);
        }

        if (existing.isPresent()) {
            return switch (dedupStrategy) {
                case "SKIP" -> null;
                case "UPDATE" -> {
                    Influenciador inf = existing.get();
                    applyInfluenciadorFields(inf, row, baseLegal);
                    infRepo.save(inf);
                    yield inf.getId();
                }
                case "DUPLICATE" -> createInfluenciador(row, assessoriaId, usuarioId, baseLegal);
                default -> null;
            };
        }

        return createInfluenciador(row, assessoriaId, usuarioId, baseLegal);
    }

    private UUID createInfluenciador(Map<String, String> row, UUID assessoriaId, UUID usuarioId, String baseLegal) {
        Influenciador inf = new Influenciador(assessoriaId, row.getOrDefault("nome", ""), usuarioId);
        applyInfluenciadorFields(inf, row, baseLegal);
        return infRepo.save(inf).getId();
    }

    private void applyInfluenciadorFields(Influenciador inf, Map<String, String> row, String baseLegal) {
        inf.setNome(row.getOrDefault("nome", inf.getNome()));
        Map<String, String> handles = new LinkedHashMap<>(inf.getHandles() != null ? inf.getHandles() : Map.of());
        if (row.containsKey("email") && !row.get("email").isBlank()) handles.put("email", row.get("email"));
        if (row.containsKey("instagram") && !row.get("instagram").isBlank()) handles.put("instagram", row.get("instagram"));
        if (row.containsKey("youtube") && !row.get("youtube").isBlank()) handles.put("youtube", row.get("youtube"));
        if (row.containsKey("tiktok") && !row.get("tiktok").isBlank()) handles.put("tiktok", row.get("tiktok"));
        if (row.containsKey("telefone") && !row.get("telefone").isBlank()) {
            String phone = ImportValidator.normalizePhone(row.get("telefone"));
            if (phone != null) handles.put("telefone", phone);
        }
        inf.setHandles(handles);
        if (row.containsKey("nicho")) inf.setNicho(row.get("nicho"));
        try {
            inf.setBaseLegal(BaseLegal.valueOf(baseLegal));
        } catch (Exception ignored) {}
    }

    public UUID persistMarca(
            Map<String, String> row, UUID assessoriaId, UUID usuarioId,
            String baseLegal, String dedupStrategy) {

        String nome = row.getOrDefault("nome", "");

        Optional<Marca> existing = marcaRepo.findByNomeIgnoreCaseAndAssessoriaId(nome, assessoriaId);

        if (existing.isPresent()) {
            return switch (dedupStrategy) {
                case "SKIP" -> null;
                case "UPDATE" -> {
                    Marca m = existing.get();
                    applyMarcaFields(m, row, baseLegal);
                    marcaRepo.save(m);
                    yield m.getId();
                }
                case "DUPLICATE" -> createMarca(row, assessoriaId, usuarioId, baseLegal);
                default -> null;
            };
        }

        return createMarca(row, assessoriaId, usuarioId, baseLegal);
    }

    private UUID createMarca(Map<String, String> row, UUID assessoriaId, UUID usuarioId, String baseLegal) {
        Marca m = new Marca(assessoriaId, row.getOrDefault("nome", ""), usuarioId);
        applyMarcaFields(m, row, baseLegal);
        return marcaRepo.save(m).getId();
    }

    private void applyMarcaFields(Marca m, Map<String, String> row, String baseLegal) {
        m.setNome(row.getOrDefault("nome", m.getNome()));
        if (row.containsKey("segmento")) m.setSegmento(row.get("segmento"));
        if (row.containsKey("site")) m.setSite(row.get("site"));
        if (row.containsKey("observacoes")) m.setObservacoes(row.get("observacoes"));
        try { m.setBaseLegal(BaseLegal.valueOf(baseLegal)); } catch (Exception ignored) {}
    }

    public UUID persistContato(
            Map<String, String> row, UUID assessoriaId, UUID usuarioId,
            String baseLegal, String dedupStrategy) {

        String email = row.get("email");
        String marcaIdStr = row.get("marca_id");
        UUID marcaId = null;
        try {
            if (marcaIdStr != null) marcaId = UUID.fromString(marcaIdStr);
        } catch (Exception ignored) {}

        if (marcaId == null) return null;

        if (email != null && !email.isBlank()) {
            Optional<Contato> existing = contatoRepo.findByEmailAndMarcaIdAndDeletedAtIsNull(email, marcaId);
            if (existing.isPresent()) {
                return switch (dedupStrategy) {
                    case "SKIP" -> null;
                    case "UPDATE" -> {
                        Contato c = existing.get();
                        applyContatoFields(c, row, baseLegal);
                        contatoRepo.save(c);
                        yield c.getId();
                    }
                    case "DUPLICATE" -> createContato(row, assessoriaId, marcaId, usuarioId, baseLegal);
                    default -> null;
                };
            }
        }

        return createContato(row, assessoriaId, marcaId, usuarioId, baseLegal);
    }

    private UUID createContato(Map<String, String> row, UUID assessoriaId, UUID marcaId, UUID usuarioId, String baseLegal) {
        Contato c = new Contato(marcaId, assessoriaId, row.getOrDefault("nome", ""));
        applyContatoFields(c, row, baseLegal);
        return contatoRepo.save(c).getId();
    }

    private void applyContatoFields(Contato c, Map<String, String> row, String baseLegal) {
        c.setNome(row.getOrDefault("nome", c.getNome()));
        if (row.containsKey("email")) c.setEmail(row.get("email"));
        if (row.containsKey("telefone")) {
            String phone = ImportValidator.normalizePhone(row.get("telefone"));
            if (phone != null) c.setTelefone(phone);
        }
        if (row.containsKey("cargo")) c.setCargo(row.get("cargo"));
        try { c.setBaseLegal(BaseLegal.valueOf(baseLegal)); } catch (Exception ignored) {}
    }

    // ---- Helpers ---

    private List<String> validateRow(String entidade, Map<String, String> row) {
        return switch (entidade) {
            case "INFLUENCIADOR" -> ImportValidator.validateInfluenciador(row);
            case "MARCA" -> ImportValidator.validateMarca(row);
            case "CONTATO" -> ImportValidator.validateContato(row);
            default -> List.of("entidade desconhecida");
        };
    }

    private void batchInsertLinhas(List<ImportJobLinha> linhas) {
        jdbc.batchUpdate(
                "INSERT INTO import_job_linhas (job_id, linha, status, entidade_id, erros) VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT DO NOTHING",
                linhas,
                linhas.size(),
                (ps, l) -> {
                    ps.setObject(1, l.getJobId());
                    ps.setInt(2, l.getLinha());
                    ps.setString(3, l.getStatus());
                    ps.setObject(4, l.getEntidadeId());
                    ps.setString(5, linhasErrosJson(l.getErros()));
                });
    }

    private String linhasErrosJson(List<String> erros) {
        if (erros == null || erros.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < erros.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(erros.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private ImportJob requireJob(AuthPrincipal principal, UUID jobId) {
        return jobRepo
                .findByIdAndAssessoriaId(jobId, principal.assessoriaId())
                .orElseThrow(BusinessException::notFound);
    }

    private void requireStatus(ImportJob job, String... allowed) {
        for (String s : allowed) {
            if (s.equals(job.getStatus())) return;
        }
        throw BusinessException.unprocessable("IMPORT_STATUS_CONFLICT",
                "Operação inválida em status " + job.getStatus() + ".");
    }

    private void requireMapeamento(ImportJob job) {
        if (job.getMapeamento() == null || job.getMapeamento().isEmpty()) {
            throw BusinessException.badRequest("IMPORT_NO_MAPEAMENTO", "Configure o mapeamento antes do dry-run.");
        }
    }

    private void validateFeatureEnabled() {
        if (!props.getFeatures().isImportEnabled()) {
            throw BusinessException.forbidden("IMPORT_DISABLED", "Funcionalidade de importação desabilitada.");
        }
    }

    private void validateEntidade(String entidade) {
        if (!List.of("INFLUENCIADOR", "MARCA", "CONTATO").contains(entidade)) {
            throw BusinessException.badRequest("IMPORT_ENTIDADE_INVALIDA", "Entidade inválida: " + entidade);
        }
    }

    private void validateBaseLegal(String bl) {
        try { BaseLegal.valueOf(bl); }
        catch (Exception e) {
            throw BusinessException.badRequest("IMPORT_BASE_LEGAL_INVALIDA", "Base legal inválida: " + bl);
        }
    }

    private void validateDedupStrategy(String s) {
        if (!List.of("SKIP", "UPDATE", "DUPLICATE").contains(s)) {
            throw BusinessException.badRequest("IMPORT_DEDUP_INVALIDO", "Estratégia dedup inválida: " + s);
        }
    }

    private boolean isSupportedExtension(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "upload.csv";
        return Paths.get(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private Path ensureUploadDir() {
        Path dir = Paths.get(props.getImport().getUploadDir());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar diretório de upload: " + dir, e);
        }
        return dir;
    }

    private void silentDelete(Path p) {
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    private String detectEncoding(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF)
                return "UTF-8";
            String utf8 = new String(bytes, 0, Math.min(bytes.length, 4096), java.nio.charset.StandardCharsets.UTF_8);
            return utf8.contains("�") ? "Windows-1252" : "UTF-8";
        } catch (IOException e) {
            return "UTF-8";
        }
    }

    private char detectSeparator(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            String preview = new String(bytes, 0, Math.min(bytes.length, 8192));
            long commas = preview.chars().filter(c -> c == ',').count();
            long semis = preview.chars().filter(c -> c == ';').count();
            long tabs = preview.chars().filter(c -> c == '\t').count();
            if (tabs >= commas && tabs >= semis) return '\t';
            if (semis >= commas) return ';';
            return ',';
        } catch (IOException e) {
            return ',';
        }
    }
}
