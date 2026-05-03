package com.hubfeatcreators.domain.prospeccao;

import com.hubfeatcreators.infra.web.BusinessException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Matriz de transições válidas do enum {@link ProspeccaoStatus} (PRD-002 Scope).
 *
 * <ul>
 *   <li>NOVA → CONTATADA
 *   <li>CONTATADA → NEGOCIANDO | FECHADA_PERDIDA
 *   <li>NEGOCIANDO → FECHADA_GANHA | FECHADA_PERDIDA
 *   <li>FECHADA_PERDIDA → NOVA (reabrir)
 *   <li>FECHADA_GANHA → ∅ (terminal)
 * </ul>
 */
public final class ProspeccaoStateMachine {

  private static final Map<ProspeccaoStatus, Set<ProspeccaoStatus>> TRANSICOES =
      Map.of(
          ProspeccaoStatus.NOVA, EnumSet.of(ProspeccaoStatus.CONTATADA),
          ProspeccaoStatus.CONTATADA,
              EnumSet.of(ProspeccaoStatus.NEGOCIANDO, ProspeccaoStatus.FECHADA_PERDIDA),
          ProspeccaoStatus.NEGOCIANDO,
              EnumSet.of(ProspeccaoStatus.FECHADA_GANHA, ProspeccaoStatus.FECHADA_PERDIDA),
          ProspeccaoStatus.FECHADA_PERDIDA, EnumSet.of(ProspeccaoStatus.NOVA),
          ProspeccaoStatus.FECHADA_GANHA, EnumSet.noneOf(ProspeccaoStatus.class));

  private ProspeccaoStateMachine() {}

  public static boolean isValid(ProspeccaoStatus from, ProspeccaoStatus to) {
    return TRANSICOES.getOrDefault(from, EnumSet.noneOf(ProspeccaoStatus.class)).contains(to);
  }

  public static void assertTransition(ProspeccaoStatus from, ProspeccaoStatus to) {
    if (from == to) {
      throw BusinessException.unprocessable(
          "STATUS_INALTERADO", "Status já é " + from.name() + ".");
    }
    if (!isValid(from, to)) {
      throw BusinessException.unprocessable(
          "TRANSICAO_INVALIDA",
          "Transição inválida: " + from.name() + " → " + to.name());
    }
  }

  public static Set<ProspeccaoStatus> proximasValidas(ProspeccaoStatus from) {
    return TRANSICOES.getOrDefault(from, EnumSet.noneOf(ProspeccaoStatus.class));
  }
}
