import { describe, expect, it } from 'vitest';
import type { Notice } from '../../../shared/types/contracts';
import { parseNotification, timeAgo, timeGroup } from './notificationParser';

const zone = 'Asia/Ho_Chi_Minh';
const now = new Date('2026-11-02T03:00:00Z');
const notice = (payloadJson: string, subject: string | null = null): Notice => ({
  id: '1',
  channel: 'WEB',
  subject,
  payloadJson,
  status: 'DELIVERED',
  readAt: null,
  createdAt: '2026-11-02T02:45:00Z',
});

describe('notificationParser', () => {
  it('describes a maintenance reminder with vehicle, service, due date and mileage', () => {
    const parsed = parseNotification(
      notice(
        '{"type":"MAINTENANCE","vehicleId":7,"serviceName":"Thay dầu","dueAt":"2026-11-05 00:00:00.000000","dueOdometer":45000}',
      ),
      zone,
      now,
    );
    expect(parsed.category).toBe('maintenance');
    expect(parsed.summary).toContain('Thay dầu');
    expect(parsed.summary).toContain('05/11/2026');
    expect(parsed.metadata).toContainEqual({ label: 'Hạn theo số km', value: '45.000 km' });
    expect(parsed.linkUrl).toBe('/reception');
    expect(parsed.timeRelative).toBe('15 phút trước');
  });
  it('recognises an estimate payload without a type field', () => {
    const parsed = parseNotification(notice('{"estimateId":12,"code":"BG-2026-088"}'), zone, now);
    expect(parsed.category).toBe('estimate');
    expect(parsed.title).toContain('BG-2026-088');
    expect(parsed.linkUrl).toBe('/estimates');
  });
  it('keeps broadcast links to the allow-list only', () => {
    const urgent = parseNotification(
      notice(
        '{"message":"Mất điện","priority":"urgent","actionUrl":"/scheduling?tab=shifts"}',
        'Khẩn',
      ),
      zone,
      now,
    );
    expect(urgent.tone).toBe('danger');
    expect(urgent.categoryLabel).toBe('Cảnh báo khẩn');
    expect(urgent.linkUrl).toBe('/scheduling?tab=shifts');
    const crafted = parseNotification(
      notice('{"message":"x","actionUrl":"https://evil.test"}', 'Tin'),
      zone,
      now,
    );
    expect(crafted.linkUrl).toBeUndefined();
  });
  it('never returns an empty message for malformed payloads', () => {
    const parsed = parseNotification(notice('{'), zone, now);
    expect(parsed.title).toBe('Thông báo từ gara');
    expect(parsed.summary.length).toBeGreaterThan(0);
  });
  it('groups and labels times in the garage zone', () => {
    expect(timeAgo('2026-11-02T02:59:40Z', zone, now)).toBe('Vừa xong');
    expect(timeAgo('2026-11-01T10:30:00Z', zone, now)).toMatch(/^Hôm qua/);
    expect(timeGroup('2026-11-01T10:30:00Z', zone, now)).toBe('yesterday');
    expect(timeGroup('2026-10-20T10:30:00Z', zone, now)).toBe('older');
  });
});
