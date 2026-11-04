import { vehicleCheckSchema, vehicleTimelineSchema } from '../types/schemas';
import { describe, expect, it } from 'vitest';
import { decodeJson, encodeJson, numericVersion } from '../api/json';
import { dateRange, reportFileName, dateTimeInput } from './dates';
import { money, safeReturnTo } from './format';
import {
  loginSchema,
  passwordChangeSchema,
  validNewPassword,
} from '../../features/auth/schemas/authSchemas';
import { userFormSchema } from '../../features/identity/schemas/userSchema';
import { safeAuditDetails } from '../../features/audit/pages/AuditPage';
import { formatAuditEntity, getAuditActionMeta } from '../../features/audit/constants/auditCatalog';
import { auditSchema } from '../types/schemas';
import { notificationMessage } from '../../features/notification/pages/NotificationsPage';
import { policy } from '../../test/fixtures';
describe('Exact transport and reporting', () => {
  it('round-trips an appointment through a local date/time input without shifting hours', () => {
    const instant = '2026-10-24T03:30:00.000Z';
    expect(new Date(dateTimeInput(instant)).toISOString()).toBe(instant);
    expect(dateTimeInput('invalid')).toBe('');
  });
  it('keeps reception foreign keys and service prices exact for schema validation', () => {
    expect(
      decodeJson(
        '{"vehicleId":9223372036854775807,"serviceId":4,"bayId":5,"createdBy":6,"checkedBy":7,"unitPrice":9999999999999999.99,"odometer":200}',
      ),
    ).toEqual({
      vehicleId: '9223372036854775807',
      serviceId: '4',
      bayId: '5',
      createdBy: '6',
      checkedBy: '7',
      unitPrice: '9999999999999999.99',
      odometer: 200,
    });
  });
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
    expect(userFormSchema().safeParse(values).success).toBe(true);
    expect(userFormSchema().safeParse({ ...values, role: 'ROOT' }).success).toBe(false);
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
  it('keeps audit actor and entity labels and describes payroll actions in Vietnamese', () => {
    const event = auditSchema.parse({
      id: '1',
      actorUserId: '12',
      actorFullName: 'Lê Hoàng Minh',
      actorUsername: 'minh',
      actorRole: 'ADMIN',
      action: 'CREATE_PAYROLL_PERIOD',
      entityType: 'PayrollPeriod',
      entityId: '3',
      entityLabel: '11/2026',
      afterJson: null,
      createdAt: '2026-11-02T00:00:00Z',
    });
    expect(event.actorFullName).toBe('Lê Hoàng Minh');
    expect(formatAuditEntity(event.entityType, event.entityId, event.entityLabel)).toBe(
      'Kỳ lương: 11/2026',
    );
    expect(getAuditActionMeta('CREATE_PAYROLL_PERIOD').label).toBe('Khởi tạo kỳ tính lương');
    expect(getAuditActionMeta('UNKNOWN_ACTION').label).toBe('UNKNOWN_ACTION');
  });
  it('only extracts notification text and safely handles malformed JSON', () => {
    expect(notificationMessage('{"message":"<script>text</script>"}')).toBe(
      '<script>text</script>',
    );
    expect(notificationMessage('{')).toContain('Thông báo từ gara');
  });
});

describe('Reception response contracts', () => {
  const file = { id: '7', repairOrderId: '8', category: 'CHECK_IN',
    originalFilename: 'vehicle.jpg', sizeBytes: 4, contentType: 'image/jpeg',
    uploadedAt: '2026-11-03T00:00:00Z' };
  it('reads the recorded checklist and attachment names returned by the server', () => {
    const result = vehicleCheckSchema.parse({ id: '6', repairOrderId: '8', checkType: 'CHECK_IN',
      attemptNo: 1, result: 'RECORDED', checklistJson: '{}', schemaVersion: 1,
      checkedBy: '2', checkedAt: '2026-11-03T00:00:00Z', attachments: [file] });
    expect(result.attachments[0]).toMatchObject({ fileName: 'vehicle.jpg', fileUrl: '/attachments/7/content' });
  });
  it('accepts technical history with the previous owner and financial details hidden', () => {
    const result = vehicleTimelineSchema.parse({ vehicleId: '1', licensePlate: '51H12345',
      brand: 'Toyota', model: 'Vios', currentOdometer: 1000, currentCustomerId: '3', currentCustomerName: 'Buyer',
      historyEntries: [{ repairOrderId: '8', orderCode: 'RO-8', receivedAt: '2026-11-03T00:00:00Z',
        completedAt: null, odometerIn: 1000, customerComplaint: null, diagnosis: 'Brake inspection',
        status: 'COMPLETED', orderCustomerId: null, currentOwnerRecord: false, totalAmount: null,
        tasksDone: ['Brake service'], partsReplaced: [], technicalAttachments: [] }] });
    expect(result.historyEntries[0].isCurrentOwnerRecord).toBe(false);
    expect(result.historyEntries[0].totalAmount).toBeNull();
  });
});
