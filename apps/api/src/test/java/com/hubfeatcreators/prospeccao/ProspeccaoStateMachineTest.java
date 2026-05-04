package com.hubfeatcreators.prospeccao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hubfeatcreators.domain.prospeccao.ProspeccaoStateMachine;
import com.hubfeatcreators.domain.prospeccao.ProspeccaoStatus;
import com.hubfeatcreators.infra.web.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProspeccaoStateMachineTest {

    @ParameterizedTest
    @CsvSource({
        "NOVA, CONTATADA",
        "CONTATADA, NEGOCIANDO",
        "CONTATADA, FECHADA_PERDIDA",
        "NEGOCIANDO, FECHADA_GANHA",
        "NEGOCIANDO, FECHADA_PERDIDA",
        "FECHADA_PERDIDA, NOVA"
    })
    void transicoes_validas_passam(String from, String to) {
        var f = ProspeccaoStatus.valueOf(from);
        var t = ProspeccaoStatus.valueOf(to);
        assertThat(ProspeccaoStateMachine.isValid(f, t)).isTrue();
        ProspeccaoStateMachine.assertTransition(f, t); // não lança
    }

    @ParameterizedTest
    @CsvSource({
        "NOVA, NEGOCIANDO",
        "NOVA, FECHADA_GANHA",
        "NOVA, FECHADA_PERDIDA",
        "CONTATADA, FECHADA_GANHA",
        "CONTATADA, NOVA",
        "NEGOCIANDO, NOVA",
        "NEGOCIANDO, CONTATADA",
        "FECHADA_GANHA, NOVA",
        "FECHADA_GANHA, FECHADA_PERDIDA",
        "FECHADA_PERDIDA, CONTATADA",
        "FECHADA_PERDIDA, NEGOCIANDO",
        "FECHADA_PERDIDA, FECHADA_GANHA"
    })
    void transicoes_invalidas_falham(String from, String to) {
        var f = ProspeccaoStatus.valueOf(from);
        var t = ProspeccaoStatus.valueOf(to);
        assertThat(ProspeccaoStateMachine.isValid(f, t)).isFalse();
        assertThatThrownBy(() -> ProspeccaoStateMachine.assertTransition(f, t))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição inválida");
    }

    @ParameterizedTest
    @CsvSource({"NOVA", "CONTATADA", "NEGOCIANDO", "FECHADA_GANHA", "FECHADA_PERDIDA"})
    void status_inalterado_e_rejeitado(String status) {
        var s = ProspeccaoStatus.valueOf(status);
        assertThatThrownBy(() -> ProspeccaoStateMachine.assertTransition(s, s))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já é");
    }
}
