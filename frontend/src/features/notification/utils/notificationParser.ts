import { dateTime } from '../../../shared/utils/format';
import type { Notice } from '../../../shared/types/contracts';

export type NotificationCategory =
  | 'maintenance'
  | 'warranty'
  | 'estimate'
  | 'repair'
  | 'payroll'
  | 'inventory'
  | 'system'
  | 'broadcast';

export type NotificationTone = 'info' | 'success' | 'warning' | 'danger' | 'neutral';

export interface NotificationMetaItem {
  label: string;
  value: string;
}

export interface ParsedNotification {
  category: NotificationCategory;
  categoryLabel: string;
  tone: NotificationTone;
  title: string;
  summary: string;
  fullMessage: string;
  linkUrl?: string;
  linkText?: string;
  metadata: NotificationMetaItem[];
  isUnread: boolean;
  timeFormatted: string;
  timeRelative: string;
}

/** Priority an admin picks when broadcasting; stored in the payload as `priority`. */
export const BROADCAST_PRIORITIES = [
  { value: 'normal', label: 'Tin tức chung', tone: 'neutral' },
  { value: 'reminder', label: 'Nhắc việc', tone: 'info' },
  { value: 'urgent', label: 'Cảnh báo khẩn', tone: 'danger' },
] as const;
export type BroadcastPriority = (typeof BROADCAST_PRIORITIES)[number]['value'];

/**
 * The only destinations a broadcast may link to. A payload's `actionUrl` outside this list is
 * dropped, so a crafted notice can never send the reader off-site.
 */
export const BROADCAST_LINK_TARGETS = [
  { url: '/reception', label: 'Lịch hẹn & Tiếp nhận' },
  { url: '/repair-orders', label: 'Lệnh sửa chữa' },
  { url: '/estimates', label: 'Báo giá' },
  { url: '/warranties', label: 'Bảo hành' },
  { url: '/scheduling?tab=shifts', label: 'Lịch ca làm việc' },
  { url: '/payroll', label: 'Bảng lương' },
  { url: '/inventory/parts', label: 'Kho phụ tùng' },
  { url: '/billing/invoices', label: 'Hóa đơn' },
] as const;

/** Inbox filter groups; `null` categories means "all". */
export const NOTIFICATION_GROUPS = [
  { key: 'service', label: 'Dịch vụ & Xe', categories: ['maintenance', 'warranty', 'repair'] },
  { key: 'money', label: 'Báo giá & Tiền tệ', categories: ['estimate', 'payroll', 'inventory'] },
] as const satisfies ReadonlyArray<{
  key: string;
  label: string;
  categories: readonly NotificationCategory[];
}>;
export type NotificationGroupKey = (typeof NOTIFICATION_GROUPS)[number]['key'];

export const FALLBACK_TITLE = 'Thông báo từ gara';
export const FALLBACK_MESSAGE =
  'Thông báo từ gara. Liên hệ nhân viên phụ trách nếu cần thêm thông tin.';

type Payload = Record<string, unknown>;

const categoryLabels: Record<NotificationCategory, string> = {
  maintenance: 'Bảo dưỡng định kỳ',
  warranty: 'Bảo hành xe',
  estimate: 'Báo giá dịch vụ',
  repair: 'Lệnh sửa chữa',
  payroll: 'Bảng lương',
  inventory: 'Kho & Vật tư',
  system: 'Hệ thống',
  broadcast: 'Thông báo chung',
};

function readPayload(raw: string): Payload | null {
  try {
    const data: unknown = JSON.parse(raw);
    return data && typeof data === 'object' && !Array.isArray(data) ? (data as Payload) : null;
  } catch {
    return null;
  }
}

const text = (value: unknown) =>
  typeof value === 'string' && value.trim() ? value.trim() : undefined;
const id = (value: unknown) =>
  typeof value === 'number' || (typeof value === 'string' && /^\d+$/.test(value))
    ? String(value)
    : undefined;

/** MySQL JSON dates arrive as "2026-11-05 00:00:00.000000"; only the calendar day matters here. */
function day(value: unknown) {
  const raw = text(value);
  const match = raw?.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : raw;
}

function km(value: unknown) {
  const raw = id(value);
  return raw ? `${new Intl.NumberFormat('vi-VN').format(Number(raw))} km` : undefined;
}

function safeLink(value: unknown) {
  const url = text(value);
  return BROADCAST_LINK_TARGETS.find((target) => target.url === url);
}

function meta(items: [string, string | undefined][]): NotificationMetaItem[] {
  return items.flatMap(([label, value]) => (value ? [{ label, value }] : []));
}

type Body = Omit<
  ParsedNotification,
  'categoryLabel' | 'isUnread' | 'timeFormatted' | 'timeRelative'
> & {
  /** Overrides the category's default badge text (e.g. an urgent broadcast). */
  categoryLabel?: string;
};

function describe(payload: Payload | null, subject: string | null): Body {
  const type = text(payload?.type);
  const message = text(payload?.message) ?? text(payload?.text);
  const vehicle = id(payload?.vehicleId) ? `Xe #${id(payload?.vehicleId)}` : undefined;

  if (type === 'MAINTENANCE') {
    const service = text(payload?.serviceName) ?? 'bảo dưỡng định kỳ';
    const due = day(payload?.dueAt);
    const dueKm = km(payload?.dueOdometer);
    const deadline = [due && `ngày ${due}`, dueKm && `mốc ${dueKm}`].filter(Boolean).join(' hoặc ');
    const summary = `${vehicle ?? 'Xe của bạn'} đã đến hạn "${service}".${deadline ? ` Hạn khuyến nghị: ${deadline}.` : ''}`;
    return {
      category: 'maintenance',
      tone: 'info',
      title: `Nhắc lịch bảo dưỡng${vehicle ? ` ${vehicle.toLowerCase()}` : ''}`,
      summary,
      fullMessage: summary,
      linkUrl: '/reception',
      linkText: 'Đặt lịch bảo dưỡng',
      metadata: meta([
        ['Phương tiện', vehicle],
        ['Dịch vụ', service],
        ['Hạn theo ngày', due],
        ['Hạn theo số km', dueKm],
      ]),
    };
  }
  if (type === 'WARRANTY_EXPIRY') {
    const expires = day(payload?.expiresAt);
    const endKm = km(payload?.endOdometer);
    const warranty = id(payload?.warrantyId);
    const summary = `Sổ bảo hành${warranty ? ` #${warranty}` : ''} của ${vehicle?.toLowerCase() ?? 'xe'} sắp hết hiệu lực${expires ? ` vào ${expires}` : ''}${endKm ? ` hoặc khi đạt ${endKm}` : ''}.`;
    return {
      category: 'warranty',
      tone: 'warning',
      title: 'Sắp hết hạn bảo hành xe',
      summary,
      fullMessage: summary,
      linkUrl: '/warranties?tab=warranties',
      linkText: 'Xem sổ bảo hành',
      metadata: meta([
        ['Phương tiện', vehicle],
        ['Mã sổ bảo hành', warranty && `#${warranty}`],
        ['Hết hạn', expires],
        ['Giới hạn km', endKm],
      ]),
    };
  }
  if (type === 'ESTIMATE_SENT' || id(payload?.estimateId)) {
    const code = text(payload?.code) ?? (id(payload?.estimateId) && `#${id(payload?.estimateId)}`);
    const summary = `Cố vấn dịch vụ đã gửi báo giá${code ? ` ${code}` : ''}. Vui lòng xem và duyệt các hạng mục sửa chữa.`;
    return {
      category: 'estimate',
      tone: 'info',
      title: `Báo giá dịch vụ${code ? ` ${code}` : ''} đã sẵn sàng`,
      summary,
      fullMessage: summary,
      linkUrl: '/estimates',
      linkText: 'Xem & duyệt báo giá',
      metadata: meta([['Mã báo giá', code]]),
    };
  }
  if (type === 'PAYROLL_APPROVED' || type === 'PAYROLL_DISBURSED') {
    const month = text(payload?.periodMonth);
    const label = month?.match(/^(\d{4})-(\d{2})$/)
      ? `${month.slice(5)}/${month.slice(0, 4)}`
      : month;
    const disbursed = type === 'PAYROLL_DISBURSED';
    const summary =
      message ??
      `Bảng lương${label ? ` tháng ${label}` : ''} đã được ${disbursed ? 'chi trả' : 'phê duyệt'}.`;
    return {
      category: 'payroll',
      tone: 'success',
      title: `Bảng lương${label ? ` tháng ${label}` : ''} ${disbursed ? 'đã chi trả' : 'đã duyệt'}`,
      summary,
      fullMessage: summary,
      linkUrl: '/payroll',
      linkText: 'Xem phiếu lương',
      metadata: meta([['Kỳ lương', label]]),
    };
  }
  if (type === 'REPAIR_COMPLETED' || type === 'QC_PASSED') {
    const qc = type === 'QC_PASSED';
    const order =
      text(payload?.orderCode) ?? (id(payload?.repairOrderId) && `#${id(payload?.repairOrderId)}`);
    const summary =
      message ??
      `${vehicle ?? 'Xe của bạn'} ${qc ? 'đã nghiệm thu đạt chuẩn và sẵn sàng bàn giao' : 'đã sửa chữa xong'}.`;
    return {
      category: 'repair',
      tone: 'success',
      title: qc ? 'Xe đã nghiệm thu xuất xưởng' : 'Xe sửa chữa hoàn tất',
      summary,
      fullMessage: summary,
      linkUrl: '/repair-orders',
      linkText: qc ? 'Chi tiết nghiệm thu' : 'Xem lệnh sửa',
      metadata: meta([
        ['Lệnh sửa chữa', order],
        ['Phương tiện', vehicle],
      ]),
    };
  }
  if (type === 'STOCK_ALERT') {
    const summary = message ?? 'Một số phụ tùng đã xuống dưới mức tồn kho tối thiểu.';
    return {
      category: 'inventory',
      tone: 'danger',
      title: 'Cảnh báo tồn kho vật tư',
      summary,
      fullMessage: summary,
      linkUrl: '/inventory/parts',
      linkText: 'Kiểm tra kho',
      metadata: meta([['Phụ tùng', text(payload?.partName)]]),
    };
  }
  if (message) {
    const priority =
      BROADCAST_PRIORITIES.find((item) => item.value === payload?.priority) ??
      BROADCAST_PRIORITIES[0];
    const link = safeLink(payload?.actionUrl);
    return {
      category: 'broadcast',
      categoryLabel: priority.value === 'normal' ? undefined : priority.label,
      tone: priority.tone,
      title: subject?.trim() || FALLBACK_TITLE,
      summary: message,
      fullMessage: message,
      linkUrl: link?.url,
      linkText: link ? `Mở ${link.label}` : undefined,
      metadata: meta([['Loại thông báo', priority.label]]),
    };
  }
  return {
    category: 'system',
    tone: 'neutral',
    title: subject?.trim() || FALLBACK_TITLE,
    summary: FALLBACK_MESSAGE,
    fullMessage: FALLBACK_MESSAGE,
    metadata: [],
  };
}

function dayKey(date: Date, zone: string) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: zone }).format(date);
}

function clock(date: Date, zone: string) {
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: zone,
  }).format(date);
}

/** Friendly time in the garage zone: "vừa xong", "15 phút trước", "Hôm nay 08:30", "Hôm qua 17:30". */
export function timeAgo(value: string, zone: string, now: Date = new Date()): string {
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return 'Không xác định';
  const minutes = Math.floor((now.valueOf() - date.valueOf()) / 60_000);
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  const today = dayKey(now, zone);
  if (dayKey(date, zone) === today) return `Hôm nay ${clock(date, zone)}`;
  const yesterday = dayKey(new Date(now.valueOf() - 86_400_000), zone);
  if (dayKey(date, zone) === yesterday) return `Hôm qua ${clock(date, zone)}`;
  return dateTime(value, zone);
}

export type TimeGroup = 'today' | 'yesterday' | 'week' | 'older';
export const timeGroupLabels: Record<TimeGroup, string> = {
  today: 'Hôm nay',
  yesterday: 'Hôm qua',
  week: 'Tuần này',
  older: 'Cũ hơn',
};

export function timeGroup(value: string, zone: string, now: Date = new Date()): TimeGroup {
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return 'older';
  if (dayKey(date, zone) === dayKey(now, zone)) return 'today';
  if (dayKey(date, zone) === dayKey(new Date(now.valueOf() - 86_400_000), zone)) return 'yesterday';
  return now.valueOf() - date.valueOf() < 7 * 86_400_000 ? 'week' : 'older';
}

export function parseNotification(
  notice: Notice,
  zone: string,
  now: Date = new Date(),
): ParsedNotification {
  const body = describe(readPayload(notice.payloadJson), notice.subject);
  return {
    ...body,
    categoryLabel: body.categoryLabel ?? categoryLabels[body.category],
    isUnread: notice.channel === 'WEB' && !notice.readAt,
    timeFormatted: dateTime(notice.createdAt, zone),
    timeRelative: timeAgo(notice.createdAt, zone, now),
  };
}

/** Plain message text of a raw payload; never empty. */
export function payloadMessage(raw: string): string {
  return describe(readPayload(raw), null).fullMessage;
}
