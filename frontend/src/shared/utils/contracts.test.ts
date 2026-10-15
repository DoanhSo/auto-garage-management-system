import { describe, expect, it } from 'vitest';
import { decodeJson, encodeJson, numericVersion } from '../api/json';
import { dateRange, reportFileName } from './dates';
import { money, safeReturnTo } from './format';
import {
  loginSchema,
  passwordChangeSchema,
  validNewPassword,
} from '../../features/auth/schemas/authSchemas';
import { userFormSchema } from '../../features/identity/schemas/userSchema';
import { safeAuditDetails } from '../../features/audit/pages/AuditPage';
import { notificationMessage } from '../../features/notification/pages/NotificationsPage';
import { policy } from '../../test/fixtures';
describe('Exact transport and reporting', () => {
  it('names CSV using the garage date rather than the previous UTC date', () =>
    expect(
      reportFileName(
        { from: '2026-09-30T17:00:00Z', until: '2026-10-01T17:00:00Z' },
        'Asia/Bangkok',
      ),
    ).toBe('bao-cao-gara-2026-10-01.csv'));
  it('preserves bigint ids, versions and decimal money without JS rounding', () => {
    expect(
      decodeJson(
        '{"id":9223372036854775807,"version":9007199254740993,"revenue":9999999999999999.99,"size":20}',
      ),
    ).toEqual({
      id: '9223372036854775807',
      version: '9007199254740993',
      revenue: '9999999999999999.99',
      size: 20,
    });
    expect(encodeJson({ version: numericVersion('9007199254740993') })).toBe(
      '{"version":9007199254740993}',
    );
    expect(money('9999999999999999.99')).toBe('9.999.999.999.999.999,99 ₫');
  });
  it('rejects unsafe pagination and invalid numeric mutation version', () => {
    expect(() => decodeJson('{"size":9007199254740993}')).toThrow();
    expect(() => numericVersion('1e3')).toThrow();
  });
  it('converts inclusive garage days to UTC half open intervals', () =>
    expect(dateRange('2026-10-01', '2026-10-01', 'Asia/Bangkok', 86400)).toEqual({
      from: '2026-09-30T17:00:00Z',
      until: '2026-10-01T17:00:00Z',
    }));
  it('uses actual DST duration instead of assuming 24 hours', () => {
    const range = dateRange('2026-03-08', '2026-03-08', 'America/New_York', 23 * 3600);
    expect(range).toEqual({
      from: '2026-03-08T05:00:00Z',
      until: '2026-03-09T04:00:00Z',
    });
    expect(() => dateRange('2026-11-01', '2026-11-01', 'America/New_York', 86400)).toThrow();
  });
  it('rejects reversed, malformed and oversized periods', () => {
    expect(() => dateRange('2026-10-02', '2026-10-01', 'UTC', 86400)).toThrow();
    expect(() => dateRange('', '2026-10-01', 'UTC', 86400)).toThrow();
    expect(() => dateRange('2026-10-01', '2026-10-02', 'UTC', 86400)).toThrow();
  });
});
describe('Credential and mutation validation', () => {
  it('does not enforce new password minimum at login or transform passwords', () => {
    expect(loginSchema.parse({ identifier: ' admin ', password: ' x ' })).toEqual({
      identifier: 'admin',
      password: ' x ',
    });
  });
  it('counts Unicode codepoints and bcrypt UTF8 byte boundary', () => {
    expect(validNewPassword('😀'.repeat(18), policy)).toBe(true);
    expect(validNewPassword('😀'.repeat(19), policy)).toBe(false);
    expect(validNewPassword('😀'.repeat(6), policy)).toBe(false);
    expect(validNewPassword(' '.repeat(20), policy)).toBe(false);
  });
  it('validates confirmation and leaves email nullable input explicit', () => {
    expect(
      passwordChangeSchema(policy).safeParse({
        currentPassword: 'old',
        newPassword: 'a'.repeat(12),
        confirmPassword: 'different',
      }).success,
    ).toBe(false);
    const values = {
      fullName: 'Member',
      username: 'member',
      email: '',
      role: 'CUSTOMER',
      active: true,
      password: 'a'.repeat(12),
      confirmPassword: 'a'.repeat(12),
    };
    expect(userFormSchema(policy, true).safeParse(values).success).toBe(true);
    expect(userFormSchema(policy, true).safeParse({ ...values, role: 'ROOT' }).success).toBe(false);
  });
});
describe('Safe rendering and navigation', () => {
  it.each([
    'https://evil.test',
    '//evil.test',
    '/%2fevil.test',
    '/\\evil.test',
    '/%00admin',
    '/login',
  ])('rejects unsafe return path %s', (value) =>
    expect(safeReturnTo(value, '/notifications')).toBe('/notifications'),
  );
  it('retains a legitimate query and sanitizes secret metadata', () => {
    expect(safeReturnTo('/admin/users?page=2', '/')).toBe('/admin/users?page=2');
    expect(
      safeAuditDetails(
        '{"token":"hidden","nested":{"passwordHash":"hidden"},"id":9223372036854775807}',
      ),
    ).not.toContain('hidden');
    expect(safeAuditDetails('{"id":9223372036854775807}')).toContain('9223372036854775807');
  });
  it('only extracts notification text and safely handles malformed JSON', () => {
    expect(notificationMessage('{"message":"<script>text</script>"}')).toBe(
      '<script>text</script>',
    );
    expect(notificationMessage('{')).toContain('Thông báo từ gara');
  });
});
