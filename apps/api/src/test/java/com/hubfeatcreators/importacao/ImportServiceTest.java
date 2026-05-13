package com.hubfeatcreators.importacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.contato.ContatoRepository;
import com.hubfeatcreators.domain.importacao.*;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.domain.marca.MarcaRepository;
import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock ImportJobRepository jobRepo;
    @Mock ImportJobLinhaRepository linhaRepo;
    @Mock ImportTemplateRepository templateRepo;
    @Mock CsvXlsxParser csvParser;
    @Mock JdbcTemplate jdbc;
    @Mock InfluenciadorRepository infRepo;
    @Mock MarcaRepository marcaRepo;
    @Mock ContatoRepository contatoRepo;
    @Mock JobService jobService;

    @TempDir Path tmpDir;

    AppProperties props;
    ImportService service;
    AuthPrincipal principal;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.getImport().setUploadDir(tmpDir.toString());
        props.getFeatures().setImportEnabled(true);

        service =
                new ImportService(
                        jobRepo,
                        linhaRepo,
                        templateRepo,
                        csvParser,
                        props,
                        jdbc,
                        infRepo,
                        marcaRepo,
                        contatoRepo,
                        jobService);

        principal =
                new AuthPrincipal(
                        UUID.randomUUID(), UUID.randomUUID(), "ASSESSOR", Set.of("CIMP", "BIMP"));
    }

    // ---- upload ---

    @Test
    void upload_featureDisabled_throws() {
        props.getFeatures().setImportEnabled(false);

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file",
                        "test.csv",
                        "text/csv",
                        "nome\nJoão\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.upload(principal, file, "INFLUENCIADOR"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desabilitada");
    }

    @Test
    void upload_unsupportedFormat_throws() {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> service.upload(principal, file, "INFLUENCIADOR"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void upload_invalidEntidade_throws() {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "test.csv", "text/csv", "nome\n".getBytes());

        assertThatThrownBy(() -> service.upload(principal, file, "INVALIDO"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void upload_ok_createsJob() throws Exception {
        byte[] csvData = "nome,email\nJoão,j@x.com\n".getBytes(StandardCharsets.UTF_8);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "test.csv", "text/csv", csvData);

        when(csvParser.probe(any()))
                .thenReturn(
                        new CsvXlsxParser.ParseResult(
                                new String[] {"nome", "email"}, 1, "UTF-8", ','));
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportJob result = service.upload(principal, file, "INFLUENCIADOR");

        assertThat(result.getEntidade()).isEqualTo("INFLUENCIADOR");
        assertThat(result.getArquivoNome()).isEqualTo("test.csv");
        assertThat(result.getStatus()).isEqualTo("UPLOADADO");
        verify(jobRepo).save(any());
    }

    // ---- setMapeamento ---

    @Test
    void setMapeamento_invalidBaseLegal_throws() {
        ImportJob job = makeJob("UPLOADADO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));

        assertThatThrownBy(
                        () ->
                                service.setMapeamento(
                                        principal,
                                        job.getId(),
                                        Map.of("nome", "nome"),
                                        "INVALIDA",
                                        "SKIP"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void setMapeamento_invalidStatus_throws() {
        ImportJob job = makeJob("EXECUTANDO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));

        assertThatThrownBy(
                        () ->
                                service.setMapeamento(
                                        principal,
                                        job.getId(),
                                        Map.of("nome", "nome"),
                                        "LEGITIMO_INTERESSE",
                                        "SKIP"))
                .isInstanceOf(BusinessException.class);
    }

    // ---- dryRun ---

    @Test
    void dryRun_noMapeamento_throws() {
        ImportJob job = makeJob("UPLOADADO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));
        // requireMapeamento throws before save is reached

        assertThatThrownBy(() -> service.dryRun(principal, job.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mapeamento");
    }

    // ---- execute ---

    @Test
    void execute_statusNotProntoDryRun_throws() {
        ImportJob job = makeJob("UPLOADADO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.execute(principal, job.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_ok_enqueuesToJobService() {
        ImportJob job = makeJob("PRONTO_DRY_RUN");
        job.setMapeamento(Map.of("nome", "nome"));
        job.setBaseLegal("LEGITIMO_INTERESSE");
        job.setDedupStrategy("SKIP");

        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));
        when(jobRepo.countExecutandoByAssessoria(any())).thenReturn(0L);
        when(jobRepo.countExecutandoByUsuario(any())).thenReturn(0L);
        // Return a job with a non-null ID so the service can call getId().toString()
        when(jobRepo.save(any()))
                .thenAnswer(
                        inv -> {
                            ImportJob j = inv.getArgument(0);
                            return withId(j, UUID.randomUUID());
                        });

        service.execute(principal, job.getId());

        verify(jobService).enqueue(eq(principal.assessoriaId()), eq("IMPORT_BULK"), any(), any());
    }

    @Test
    void execute_tooManyConcurrent_throws() {
        ImportJob job = makeJob("PRONTO_DRY_RUN");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));
        when(jobRepo.countExecutandoByAssessoria(any())).thenReturn(5L);

        assertThatThrownBy(() -> service.execute(principal, job.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limite");
    }

    // ---- cancel ---

    @Test
    void cancel_terminalStatus_throws() {
        ImportJob job = makeJob("CONCLUIDO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancel(principal, job.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_executando_ok() {
        ImportJob job = makeJob("EXECUTANDO");
        when(jobRepo.findByIdAndAssessoriaId(any(), any())).thenReturn(Optional.of(job));
        when(linhaRepo.findByJobIdAndStatus(any(), eq("OK"))).thenReturn(List.of());
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportJob result = service.cancel(principal, job.getId());

        assertThat(result.getStatus()).isEqualTo("CANCELADO");
    }

    // ---- templates ---

    @Test
    void saveTemplate_ok() {
        when(templateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportTemplate t =
                service.saveTemplate(
                        principal, "Meu template", "INFLUENCIADOR", Map.of("Nome", "nome"));

        assertThat(t.getNome()).isEqualTo("Meu template");
        assertThat(t.getEntidade()).isEqualTo("INFLUENCIADOR");
        verify(templateRepo).save(any());
    }

    @Test
    void deleteTemplate_notFound_throws() {
        when(templateRepo.findByIdAndAssessoriaIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTemplate(principal, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
    }

    // ---- validateInfluenciador via validateRow (indirectly through dryRun) ---

    @Test
    void persistInfluenciador_dedup_skip() {
        com.hubfeatcreators.domain.influenciador.Influenciador existing =
                new com.hubfeatcreators.domain.influenciador.Influenciador(
                        principal.assessoriaId(), "Existing", principal.usuarioId());
        when(infRepo.findByHandleAndAssessoria(any(), eq("instagram"), any()))
                .thenReturn(Optional.of(existing));

        UUID result =
                service.persistInfluenciador(
                        Map.of("nome", "João", "instagram", "joao_ig"),
                        principal.assessoriaId(),
                        principal.usuarioId(),
                        "LEGITIMO_INTERESSE",
                        "SKIP");

        assertThat(result).isNull();
    }

    @Test
    void persistInfluenciador_noDedup_creates() {
        // No instagram/email in row → no dedup lookup; Influenciador.id is initialized inline
        when(infRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID result =
                service.persistInfluenciador(
                        Map.of("nome", "João"),
                        principal.assessoriaId(),
                        principal.usuarioId(),
                        "LEGITIMO_INTERESSE",
                        "SKIP");

        // Influenciador sets id = UUID.randomUUID() in field initializer
        assertThat(result).isNotNull();
    }

    // ---- Helpers ---

    private ImportJob makeJob(String status) {
        ImportJob job =
                new ImportJob(
                        principal.assessoriaId(),
                        principal.usuarioId(),
                        "INFLUENCIADOR",
                        tmpDir + "/test.csv",
                        "test.csv");
        job.setStatus(status);
        return job;
    }

    private ImportJob withId(ImportJob job, UUID id) {
        try {
            java.lang.reflect.Field f = ImportJob.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(job, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return job;
    }
}
