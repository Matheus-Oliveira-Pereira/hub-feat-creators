import { describe, it, expect } from 'vitest';
import { decodeJwt } from '../auth';

function buildJwt(claims: object): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify(claims)).toString('base64url');
  return `${header}.${payload}.fakesig`;
}

describe('decodeJwt', () => {
  it('returns null for null input', () => {
    expect(decodeJwt(null)).toBeNull();
  });

  it('returns null for empty string', () => {
    expect(decodeJwt('')).toBeNull();
  });

  it('returns null for malformed token (< 3 parts)', () => {
    expect(decodeJwt('only.two')).toBeNull();
  });

  it('returns null for invalid base64 payload', () => {
    expect(decodeJwt('header.!!!notbase64!!!.sig')).toBeNull();
  });

  it('decodes standard claims correctly', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const token = buildJwt({
      sub: 'user-001',
      role: 'ASSESSOR',
      perms: ['BPRO', 'BINF'],
      exp,
    });

    const claims = decodeJwt(token);

    expect(claims).not.toBeNull();
    expect(claims!.usuarioId).toBe('user-001');
    expect(claims!.role).toBe('ASSESSOR');
    expect(claims!.permissions).toEqual(['BPRO', 'BINF']);
    expect(claims!.exp).toBe(exp);
  });

  it('permissions defaults to [] when perms claim missing', () => {
    const token = buildJwt({ sub: 'u', role: 'ASSESSOR', exp: 0 });
    const claims = decodeJwt(token);
    expect(claims!.permissions).toEqual([]);
  });

  it('permissions defaults to [] when perms is not array', () => {
    const token = buildJwt({ sub: 'u', role: 'OWNER', perms: 'bad', exp: 0 });
    const claims = decodeJwt(token);
    expect(claims!.permissions).toEqual([]);
  });

  it('handles missing sub gracefully', () => {
    const token = buildJwt({ role: 'OWNER', perms: [], exp: 0 });
    const claims = decodeJwt(token);
    expect(claims!.usuarioId).toBe('');
  });

  it('OWNER token has OWNR in permissions', () => {
    const token = buildJwt({
      sub: 'owner-1',
      role: 'OWNER',
      perms: ['OWNR'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    });
    const claims = decodeJwt(token);
    expect(claims!.permissions).toContain('OWNR');
    expect(claims!.role).toBe('OWNER');
  });
});
