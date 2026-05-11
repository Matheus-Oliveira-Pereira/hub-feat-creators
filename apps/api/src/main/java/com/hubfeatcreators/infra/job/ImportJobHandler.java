package com.hubfeatcreators.infra.job;

import com.hubfeatcreators.domain.importacao.CsvXlsxParser;
import com.hubfeatcreators.domain.importacao.ImportJobLinha;
import com.hubfeatcreators.domain.importacao.ImportJobRepository;
import com.hubfeatcreators.domain.importacao.ImportService;
import com.hubfeatcreators.domain.importacao.ImportValidator;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("IMPORT_BULK")
public class ImportJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(ImportJobHandler.class);
    private static final int CHUNK = 500;

    private final ImportJobRepository jobRepo;
    private final ImportService importService;
    private final CsvXlsxParser parser;

    public ImportJobHandler(ImportJobRepository jobRepo, ImportService importService, CsvXlsxParser parser) {
        this.jobRepo = jobRepo;
        this.importService = importService;
        this.parser = parser;
    }

    @Override
    public void handle(Job job) throws Exception {
        UUID jobId = UUID.fromString((String) job.getPayload().get("importJobId"));

        com.hubfeatcreators.domain.importacao.ImportJob importJob = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("ImportJob não encontrado: " + jobId));

        String entidade = importJob.getEntidade();
        UUID assessoriaId = importJob.getAssessoriaId();
        UUID usuarioId = importJob.getUsuarioId();
        String baseLegal = importJob.getBaseLegal() != null ? importJob.getBaseLegal() : "LEGITIMO_INTERESSE";
        String dedupStrategy = importJob.getDedupStrategy() != null ? importJob.getDedupStrategy() : "SKIP";
        Map<String, String> mapeamento = importJob.getMapeamento();

        String encoding = "UTF-8";
        char separator = ',';
        // detect from stored values or re-probe
        try {
            CsvXlsxParser.ParseResult probe = parser.probe(Paths.get(importJob.getArquivoPath()));
            encoding = probe.encoding();
            separator = probe.separator();
        } catch (Exception e) {
            log.warn("Falha ao probar encoding/separator para job {}: {}", jobId, e.getMessage());
        }

        List<ImportJobLinha> chunk = new ArrayList<>();
        int[] counters = {0, 0, 0}; // processadas, ok, falha

        final String enc = encoding;
        final char sep = separator;

        parser.stream(
                Paths.get(importJob.getArquivoPath()),
                mapeamento,
                enc,
                sep,
                (rowIdx, row) -> {
                    // Re-check cancel mid-execution
                    if (rowIdx % CHUNK == 0) {
                        com.hubfeatcreators.domain.importacao.ImportJob current = jobRepo
                                .findById(jobId).orElseThrow();
                        if ("CANCELADO".equals(current.getStatus())) {
                            throw new RuntimeException("Job cancelado");
                        }
                    }

                    List<String> erros = validateRow(entidade, row);
                    String status;
                    UUID entidadeId = null;

                    if (erros.isEmpty()) {
                        try {
                            entidadeId = persist(entidade, row, assessoriaId, usuarioId, baseLegal, dedupStrategy);
                            status = entidadeId != null ? "OK" : "SKIP";
                        } catch (Exception e) {
                            erros = List.of("Erro ao persistir: " + e.getMessage());
                            status = "ERRO";
                        }
                    } else {
                        status = "ERRO";
                    }

                    if ("OK".equals(status)) counters[1]++;
                    else if ("ERRO".equals(status)) counters[2]++;
                    counters[0]++;

                    chunk.add(new ImportJobLinha(jobId, rowIdx, status, entidadeId, erros));

                    if (chunk.size() >= CHUNK) {
                        flushChunk(importJob, chunk, counters);
                        chunk.clear();
                    }
                });

        if (!chunk.isEmpty()) {
            flushChunk(importJob, chunk, counters);
        }

        importJob.setStatus("CONCLUIDO");
        importJob.setConcluidoEm(Instant.now());
        importJob.setUpdatedAt(Instant.now());
        jobRepo.save(importJob);

        log.info("import.concluido jobId={} total={} ok={} falha={}", jobId, counters[0], counters[1], counters[2]);
    }

    private void flushChunk(
            com.hubfeatcreators.domain.importacao.ImportJob importJob,
            List<ImportJobLinha> chunk,
            int[] counters) {
        importService.executeChunk(
                importJob.getId(), chunk,
                importJob.getEntidade(),
                importJob.getAssessoriaId(),
                importJob.getUsuarioId(),
                importJob.getBaseLegal(),
                importJob.getDedupStrategy());
    }

    private List<String> validateRow(String entidade, Map<String, String> row) {
        return switch (entidade) {
            case "INFLUENCIADOR" -> ImportValidator.validateInfluenciador(row);
            case "MARCA" -> ImportValidator.validateMarca(row);
            case "CONTATO" -> ImportValidator.validateContato(row);
            default -> List.of("entidade desconhecida");
        };
    }

    private UUID persist(String entidade, Map<String, String> row, UUID assessoriaId, UUID usuarioId,
            String baseLegal, String dedupStrategy) {
        return switch (entidade) {
            case "INFLUENCIADOR" -> importService.persistInfluenciador(row, assessoriaId, usuarioId, baseLegal, dedupStrategy);
            case "MARCA" -> importService.persistMarca(row, assessoriaId, usuarioId, baseLegal, dedupStrategy);
            case "CONTATO" -> importService.persistContato(row, assessoriaId, usuarioId, baseLegal, dedupStrategy);
            default -> null;
        };
    }
}
