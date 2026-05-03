import { z } from 'zod';

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
// (form fields são todos string; transformação para payload em queries.ts)
// ────────────────────────────────────────────────────────────────────────────

export const influenciadorSchema = z.object({
  nome: z.string().trim().min(1, 'Nome obrigatório').max(160, 'Máx 160 caracteres'),
  nicho: z.string().max(80, 'Máx 80 caracteres'),
  instagram: z
    .string()
    .max(60, 'Máx 60 caracteres')
    .regex(/^@?[A-Za-z0-9._]*$/, 'Use apenas letras, números, ponto ou underline'),
  audienciaTotal: z.string().regex(/^\d*$/, 'Apenas números').max(15),
  observacoes: z.string().max(2000, 'Máx 2000 caracteres'),
  tags: z.array(z.string().min(1).max(40)).max(20),
});
export type InfluenciadorInput = z.infer<typeof influenciadorSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Marca
// ────────────────────────────────────────────────────────────────────────────

export const marcaSchema = z.object({
  nome: z.string().trim().min(1, 'Nome obrigatório').max(160, 'Máx 160 caracteres'),
  segmento: z.string().max(80),
  site: z
    .string()
    .max(200)
    .refine(
      v => {
        if (v.trim() === '') return true;
        const url = /^https?:\/\//i.test(v) ? v : `https://${v}`;
        try {
          new URL(url);
          return true;
        } catch {
          return false;
        }
      },
      { message: 'URL inválida' }
    ),
  observacoes: z.string().max(2000),
  tags: z.array(z.string().min(1).max(40)).max(20),
});
export type MarcaInput = z.infer<typeof marcaSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Contato (de Marca)
// ────────────────────────────────────────────────────────────────────────────

export const contatoSchema = z.object({
  nome: z.string().trim().min(1, 'Nome obrigatório').max(120),
  email: z
    .string()
    .max(160)
    .refine(v => v === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim()), {
      message: 'E-mail inválido',
    }),
  telefone: z.string().max(40),
  cargo: z.string().max(80),
});
export type ContatoInput = z.infer<typeof contatoSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Prospecção
// ────────────────────────────────────────────────────────────────────────────

export const prospeccaoSchema = z.object({
  marcaId: z.string().uuid('Marca obrigatória'),
  influenciadorId: z.string().uuid().or(z.literal('')),
  assessorResponsavelId: z.string().uuid().or(z.literal('')),
  titulo: z.string().trim().min(1, 'Título obrigatório').max(160),
  valorEstimado: z.string().regex(/^\d*([.,]\d{0,2})?$/, 'Valor inválido'),
  proximaAcao: z.string().max(240),
  proximaAcaoEm: z.string().regex(/^(\d{4}-\d{2}-\d{2})?$/, 'Data inválida'),
  observacoes: z.string().max(2000),
  tags: z.array(z.string().min(1).max(40)).max(20),
});
export type ProspeccaoInput = z.infer<typeof prospeccaoSchema>;

const motivoPerdaSchema = z.enum([
  'SEM_FIT',
  'ORCAMENTO',
  'TIMING',
  'CONCORRENTE',
  'SEM_RESPOSTA',
  'OUTRO',
]);

export const statusChangeSchema = z
  .object({
    status: z.enum(['NOVA', 'CONTATADA', 'NEGOCIANDO', 'FECHADA_GANHA', 'FECHADA_PERDIDA']),
    motivoPerda: motivoPerdaSchema.or(z.literal('')),
    motivoPerdaDetalhe: z.string().max(500),
  })
  .superRefine((data, ctx) => {
    if (data.status === 'FECHADA_PERDIDA' && !data.motivoPerda) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['motivoPerda'],
        message: 'Selecione o motivo da perda',
      });
    }
    if (data.motivoPerda === 'OUTRO' && !data.motivoPerdaDetalhe.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['motivoPerdaDetalhe'],
        message: 'Detalhe obrigatório quando motivo = OUTRO',
      });
    }
  });
export type StatusChangeInput = z.infer<typeof statusChangeSchema>;

export const comentarioSchema = z.object({
  texto: z.string().trim().min(1, 'Texto obrigatório').max(2000),
});
export type ComentarioInput = z.infer<typeof comentarioSchema>;

// ────────────────────────────────────────────────────────────────────────────
// Perfil (RBAC)
// ────────────────────────────────────────────────────────────────────────────

export const perfilSchema = z.object({
  nome: z.string().trim().min(2, 'Nome muito curto').max(80, 'Máx 80 caracteres'),
  descricao: z.string().max(240, 'Máx 240 caracteres'),
  roles: z.array(z.string()).min(1, 'Selecione ao menos uma permissão'),
});
export type PerfilInput = z.infer<typeof perfilSchema>;
