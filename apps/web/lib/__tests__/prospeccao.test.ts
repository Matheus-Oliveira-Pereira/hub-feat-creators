import { describe, it, expect } from 'vitest';
import { isValidTransition, formatBRL, STATUS_LABEL, STATUS_ORDER } from '../prospeccao';
import type { ProspeccaoStatus } from '../api';

describe('isValidTransition', () => {
  it('NOVA → CONTATADA is valid', () => {
    expect(isValidTransition('NOVA', 'CONTATADA')).toBe(true);
  });

  it('NOVA → NEGOCIANDO is invalid', () => {
    expect(isValidTransition('NOVA', 'NEGOCIANDO')).toBe(false);
  });

  it('NOVA → FECHADA_GANHA is invalid', () => {
    expect(isValidTransition('NOVA', 'FECHADA_GANHA')).toBe(false);
  });

  it('CONTATADA → NEGOCIANDO is valid', () => {
    expect(isValidTransition('CONTATADA', 'NEGOCIANDO')).toBe(true);
  });

  it('CONTATADA → FECHADA_PERDIDA is valid', () => {
    expect(isValidTransition('CONTATADA', 'FECHADA_PERDIDA')).toBe(true);
  });

  it('CONTATADA → FECHADA_GANHA is invalid', () => {
    expect(isValidTransition('CONTATADA', 'FECHADA_GANHA')).toBe(false);
  });

  it('NEGOCIANDO → FECHADA_GANHA is valid', () => {
    expect(isValidTransition('NEGOCIANDO', 'FECHADA_GANHA')).toBe(true);
  });

  it('NEGOCIANDO → FECHADA_PERDIDA is valid', () => {
    expect(isValidTransition('NEGOCIANDO', 'FECHADA_PERDIDA')).toBe(true);
  });

  it('NEGOCIANDO → NOVA is invalid', () => {
    expect(isValidTransition('NEGOCIANDO', 'NOVA')).toBe(false);
  });

  it('FECHADA_PERDIDA → NOVA is valid (reabrir)', () => {
    expect(isValidTransition('FECHADA_PERDIDA', 'NOVA')).toBe(true);
  });

  it('FECHADA_PERDIDA → NEGOCIANDO is invalid', () => {
    expect(isValidTransition('FECHADA_PERDIDA', 'NEGOCIANDO')).toBe(false);
  });

  it('FECHADA_GANHA → any is invalid (terminal)', () => {
    const others: ProspeccaoStatus[] = ['NOVA', 'CONTATADA', 'NEGOCIANDO', 'FECHADA_PERDIDA'];
    others.forEach(to => {
      expect(isValidTransition('FECHADA_GANHA', to)).toBe(false);
    });
  });

  it('same status → same is invalid', () => {
    expect(isValidTransition('NOVA', 'NOVA')).toBe(false);
    expect(isValidTransition('CONTATADA', 'CONTATADA')).toBe(false);
  });
});

describe('formatBRL', () => {
  it('null returns em dash', () => {
    expect(formatBRL(null)).toBe('—');
  });

  it('0 centavos formats as R$ 0,00', () => {
    expect(formatBRL(0)).toMatch(/R\$\s*0[,.]00/);
  });

  it('100 centavos formats as R$ 1,00', () => {
    expect(formatBRL(100)).toMatch(/R\$\s*1[,.]00/);
  });

  it('150000 centavos formats as R$ 1.500,00', () => {
    const result = formatBRL(150000);
    // Allow locale variations in separator
    expect(result).toMatch(/1[.,]500/);
    expect(result).toContain('R$');
  });
});

describe('STATUS_LABEL', () => {
  it('covers all 5 statuses', () => {
    const statuses: ProspeccaoStatus[] = [
      'NOVA',
      'CONTATADA',
      'NEGOCIANDO',
      'FECHADA_GANHA',
      'FECHADA_PERDIDA',
    ];
    statuses.forEach(s => {
      expect(STATUS_LABEL[s]).toBeTruthy();
    });
  });
});

describe('STATUS_ORDER', () => {
  it('has 5 entries in funnel order', () => {
    expect(STATUS_ORDER).toHaveLength(5);
    expect(STATUS_ORDER[0]).toBe('NOVA');
    expect(STATUS_ORDER[STATUS_ORDER.length - 1]).toBe('FECHADA_PERDIDA');
  });

  it('FECHADA_GANHA comes before FECHADA_PERDIDA', () => {
    const ganhaIdx = STATUS_ORDER.indexOf('FECHADA_GANHA');
    const perdidaIdx = STATUS_ORDER.indexOf('FECHADA_PERDIDA');
    expect(ganhaIdx).toBeLessThan(perdidaIdx);
  });
});
