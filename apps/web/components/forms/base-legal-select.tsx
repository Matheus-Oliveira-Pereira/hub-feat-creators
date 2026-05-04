'use client';

import * as React from 'react';
import { Label } from '@/components/ui/label';
import { baseLegalLabels, type BaseLegal } from '@/lib/schemas';

interface Props {
  id?: string;
  value: BaseLegal | '';
  onChange: (value: BaseLegal) => void;
  error?: string;
  required?: boolean;
}

const OPTIONS: BaseLegal[] = [
  'LEGITIMO_INTERESSE',
  'EXECUCAO_CONTRATO',
  'CONSENTIMENTO',
  'OBRIGACAO_LEGAL',
];

export function BaseLegalSelect({ id = 'baseLegal', value, onChange, error, required }: Props) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>
        Base legal (LGPD){required && ' *'}
      </Label>
      <select
        id={id}
        value={value}
        onChange={e => onChange(e.target.value as BaseLegal)}
        aria-invalid={!!error}
        aria-required={required}
        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50"
      >
        <option value="" disabled>Selecione a base legal…</option>
        {OPTIONS.map(opt => (
          <option key={opt} value={opt}>
            {baseLegalLabels[opt]}
          </option>
        ))}
      </select>
      {error ? (
        <p className="text-xs text-destructive" role="alert">{error}</p>
      ) : (
        <p className="text-xs text-muted-foreground">
          Base legal para tratamento de dados pessoais conforme LGPD.
        </p>
      )}
    </div>
  );
}
