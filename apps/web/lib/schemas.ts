import { z } from 'zod';

const trim = (v: unknown) => (typeof v === 'string' ? v.trim() : v);
const emptyToUndefined = (v: unknown) =>
  typeof v === 'string' && v.trim() === '' ? undefined : v;

// ────────────────────────────────────────────────────────────────────────────
// Auth
// ────────────────────────────────────────────────────────────────────────────

export const loginSchema = z.object({
  email: z.string().trim().email('E-mail inválido'),
  senha: z.string().min(1, 'Senha obrigatória'),
});
export type LoginInput = z.infer<typeof loginSchema>;

export const signupSchema = z.object({
  assessoriaNome: z
    .string()
    .trim()
    .min(2, 'Nome muito curto')
    .max(120, 'Nome muito longo'),
  slug: z
    .string()
    .min(3, 'Mínimo 3 caracteres')
    .max(50, 'Máximo 50 caracteres')
    .regex(/^[a-z0-9-]+$/, 'Apenas letras minúsculas, números e hífens'),
  email: z.string().trim().email('E-mail inválido'),
  senha: z.string().min(8, 'Mínimo 8 caracteres'),
});
export type SignupInput = z.infer<typeof signupSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Influenciador
// ────────────────────────────────────────────────────────────────────────────

export const influenciadorSchema = z.object({
  nome: z.preprocess(
    trim,
    z.string().min(1, 'Nome obrigatório').max(160, 'Máx 160 caracteres')
  ),
  nicho: z
    .preprocess(emptyToUndefined, z.string().max(80).optional())
    .nullable()
    .optional(),
  instagram: z
    .preprocess(
      v => {
        if (typeof v !== 'string') return v;
        return v.trim().replace(/^@/, '');
      },
      z
        .string()
        .max(60)
        .regex(/^[A-Za-z0-9._]*$/, 'Use apenas letras, números, ponto ou underline')
        .optional()
    )
    .optional(),
  audienciaTotal: z
    .preprocess(
      v => {
        if (v === '' || v === null || v === undefined) return undefined;
        const n = typeof v === 'number' ? v : Number(String(v).replace(/\D/g, ''));
        return Number.isFinite(n) ? n : undefined;
      },
      z.number().int().nonnegative('Não pode ser negativo').optional()
    )
    .optional(),
  observacoes: z
    .preprocess(emptyToUndefined, z.string().max(2000).optional())
    .nullable()
    .optional(),
  tags: z.array(z.string().min(1).max(40)).max(20).default([]),
});
export type InfluenciadorInput = z.infer<typeof influenciadorSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Marca
// ────────────────────────────────────────────────────────────────────────────

export const marcaSchema = z.object({
  nome: z.preprocess(
    trim,
    z.string().min(1, 'Nome obrigatório').max(160, 'Máx 160 caracteres')
  ),
  segmento: z
    .preprocess(emptyToUndefined, z.string().max(80).optional())
    .nullable()
    .optional(),
  site: z
    .preprocess(
      v => {
        if (typeof v !== 'string' || v.trim() === '') return undefined;
        const t = v.trim();
        return /^https?:\/\//i.test(t) ? t : `https://${t}`;
      },
      z.string().url('URL inválida').max(200).optional()
    )
    .optional(),
  observacoes: z
    .preprocess(emptyToUndefined, z.string().max(2000).optional())
    .nullable()
    .optional(),
  tags: z.array(z.string().min(1).max(40)).max(20).default([]),
});
export type MarcaInput = z.infer<typeof marcaSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────────────

export function fieldError(error: unknown, field: string): string | undefined {
  if (typeof error !== 'object' || error === null) return;
  const errs = (error as { errors?: { field?: string; message?: string }[] }).errors;
  return errs?.find(e => e.field === field)?.message;
}
