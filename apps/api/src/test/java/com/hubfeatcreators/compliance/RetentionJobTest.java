package com.hubfeatcreators.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.compliance.RetentionJob;
import com.hubfeatcreators.domain.compliance.RetentionRun;
import com.hubfeatcreators.domain.compliance.RetentionRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

class RetentionJobTest {

    @Mock JdbcTemplate jdbc;
    @Mock RetentionRunRepository runRepo;

    @InjectMocks RetentionJob job;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jdbc.update(any(String.class), any(Object.class))).thenReturn(0);
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executar_salva_runs_para_todas_tabelas() {
        job.executar();

        // 5 runs: influenciadores, marcas, contatos, job, audit_log
        verify(runRepo, times(5)).save(any(RetentionRun.class));
    }

    @Test
    void executar_queries_influenciadores_com_deleted_at() {
        job.executar();
        verify(jdbc, atLeastOnce()).update(contains("influenciadores"), any(Object.class));
    }

    @Test
    void executar_purga_jobs_antigos() {
        when(jdbc.update(contains("DELETE FROM job"), any(Object.class))).thenReturn(3);
        job.executar();
        verify(jdbc).update(contains("DELETE FROM job"), any(Object.class));
    }

    @Test
    void executar_nao_lanca_excecao_quando_zero_registros() {
        when(jdbc.update(any(String.class), any(Object.class))).thenReturn(0);
        // Should not throw
        job.executar();
    }
}
