/**
 * Catálogo de roles 4-letter para UI. Espelha PermissionCodes.java do backend.
 * Sincronizar manualmente quando ADR-015 evoluir.
 */

export interface RoleDef {
  code: string;
  label: string;
  description?: string;
}

export interface RoleGroup {
  key: string;
  label: string;
  roles: RoleDef[];
}

const crud = (entity: string, label: string): RoleDef[] => [
  { code: 'B' + entity, label: 'Browse', description: `Listar / ver ${label.toLowerCase()}` },
  { code: 'C' + entity, label: 'Change', description: `Criar ${label.toLowerCase()}` },
  { code: 'E' + entity, label: 'Edit', description: `Editar ${label.toLowerCase()}` },
  { code: 'D' + entity, label: 'Delete', description: `Remover ${label.toLowerCase()}` },
];

export const ROLE_GROUPS: RoleGroup[] = [
  { key: 'PRO', label: 'Prospecções', roles: crud('PRO', 'prospecções') },
  { key: 'MAR', label: 'Marcas', roles: crud('MAR', 'marcas') },
  { key: 'INF', label: 'Influenciadores', roles: crud('INF', 'influenciadores') },
  { key: 'CON', label: 'Contatos', roles: crud('CON', 'contatos') },
  { key: 'TAR', label: 'Tarefas', roles: crud('TAR', 'tarefas') },
  { key: 'EML', label: 'E-mail', roles: crud('EML', 'e-mails') },
  { key: 'WAP', label: 'WhatsApp', roles: crud('WAP', 'WhatsApp') },
  { key: 'USU', label: 'Usuários', roles: crud('USU', 'usuários') },
  { key: 'PRF', label: 'Perfis (RBAC)', roles: crud('PRF', 'perfis') },
  {
    key: 'REL',
    label: 'Relatórios',
    roles: [{ code: 'BREL', label: 'Browse', description: 'Acessar dashboard / relatórios' }],
  },
  {
    key: 'NOT',
    label: 'Notificações',
    roles: [{ code: 'BNOT', label: 'Browse', description: 'Ver notificações in-app' }],
  },
  {
    key: 'HIS',
    label: 'Histórico',
    roles: [{ code: 'BHIS', label: 'Browse', description: 'Ver histórico unificado por entidade' }],
  },
  {
    key: 'SPECIAL',
    label: 'Especiais',
    roles: [
      { code: 'OWNR', label: 'Godmode', description: 'Bypassa todas as verificações da assessoria' },
      { code: 'INVT', label: 'Convidar', description: 'Convidar novos usuários' },
      { code: 'EXPT', label: 'Exportar', description: 'Exportar CSV / relatórios (sensível LGPD)' },
      { code: 'BLLG', label: 'Audit log', description: 'Visualizar log de auditoria' },
    ],
  },
];

export const ALL_ROLE_CODES = ROLE_GROUPS.flatMap(g => g.roles.map(r => r.code));
