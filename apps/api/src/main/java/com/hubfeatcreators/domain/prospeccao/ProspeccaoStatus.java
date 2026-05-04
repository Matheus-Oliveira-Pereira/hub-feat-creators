package com.hubfeatcreators.domain.prospeccao;

public enum ProspeccaoStatus {
    NOVA,
    CONTATADA,
    NEGOCIANDO,
    FECHADA_GANHA,
    FECHADA_PERDIDA;

    public boolean isFechada() {
        return this == FECHADA_GANHA || this == FECHADA_PERDIDA;
    }
}
