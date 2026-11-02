export type AuditDomain =
  | 'security'
  | 'hr'
  | 'shift'
  | 'payroll'
  | 'bay'
  | 'booking'
  | 'repair'
  | 'qc_warranty'
  | 'inventory_billing';

export type AuditSeverity = 'info' | 'success' | 'warning' | 'danger';

export interface AuditActionMeta {
  label: string;
  description: string;
  domain: AuditDomain;
  domainLabel: string;
  severity: AuditSeverity;
}

export const auditDomains: { key: AuditDomain; label: string }[] = [
  { key: 'security', label: 'Bảo mật & Tài khoản' },
  { key: 'hr', label: 'Hồ sơ nhân sự' },
  { key: 'shift', label: 'Ca trực & Điểm danh' },
  { key: 'payroll', label: 'Bảng lương & Hoa hồng' },
  { key: 'bay', label: 'Khoang xưởng & Thiết bị' },
  { key: 'booking', label: 'Lịch hẹn & Tiếp nhận' },
  { key: 'repair', label: 'Lệnh sửa chữa & Thi công' },
  { key: 'qc_warranty', label: 'Nghiệm thu & Bảo hành' },
  { key: 'inventory_billing', label: 'Kho vật tư & Hóa đơn' },
];

const domainLabels = Object.fromEntries(auditDomains.map((d) => [d.key, d.label])) as Record<
  AuditDomain,
  string
>;

/** Severity → shared Badge variant. */
export const auditSeverityBadge = {
  info: 'primary',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
} as const satisfies Record<AuditSeverity, string>;

export const auditSeverityLabels: Record<AuditSeverity, string> = {
  info: 'Thông tin',
  success: 'Tạo mới / Hoàn tất',
  warning: 'Cần lưu ý',
  danger: 'Hủy / Xóa / Ngừng',
};

type Entry = [label: string, description: string, severity: AuditSeverity];

const byDomain: Record<AuditDomain, Record<string, Entry>> = {
  security: {
    AUTH_LOGIN: ['Đăng nhập hệ thống', 'Người dùng đăng nhập thành công vào hệ thống.', 'info'],
    AUTH_LOGIN_FAILED: [
      'Đăng nhập thất bại',
      'Nhập sai tên đăng nhập, mật khẩu hoặc tài khoản bị khóa.',
      'warning',
    ],
    AUTH_LOGOUT_ALL: [
      'Đăng xuất toàn bộ',
      'Đăng xuất và thu hồi mọi phiên đăng nhập trên tất cả thiết bị.',
      'info',
    ],
    ADMIN_BOOTSTRAPPED: [
      'Khởi tạo Quản trị viên',
      'Hệ thống tự động tạo tài khoản quản trị đầu tiên khi khởi chạy.',
      'warning',
    ],
    CUSTOMER_JIT_PROVISIONED: [
      'Tự tạo hồ sơ khách hàng',
      'Hệ thống tự động tạo tài khoản khách hàng khi đăng nhập lần đầu.',
      'info',
    ],
    USER_CREATED: [
      'Tạo tài khoản mới',
      'Thêm mới tài khoản người dùng và phân quyền ban đầu.',
      'success',
    ],
    USER_UPDATED: [
      'Cập nhật tài khoản',
      'Chỉnh sửa thông tin cá nhân, phân quyền vai trò hoặc kích hoạt/khóa tài khoản.',
      'info',
    ],
    USER_DELETED: ['Xóa tài khoản', 'Xóa tài khoản người dùng khỏi hệ thống.', 'danger'],
    USER_PASSWORD_CHANGED: [
      'Đổi mật khẩu',
      'Mật khẩu đăng nhập của tài khoản đã được thay đổi thành công.',
      'warning',
    ],
    NOTIFICATION_ENQUEUED: ['Tạo thông báo', 'Gửi một thông báo tới người dùng.', 'info'],
    NOTIFICATION_BROADCAST: [
      'Gửi thông báo hàng loạt',
      'Gửi một thông báo tới nhiều người dùng cùng lúc.',
      'info',
    ],
    NOTIFICATION_READ: ['Đọc thông báo', 'Người dùng đã đọc một thông báo.', 'info'],
    NOTIFICATION_DELETED: ['Xóa thông báo', 'Xóa một thông báo khỏi hộp thư.', 'danger'],
  },
  hr: {
    CREATE_EMPLOYEE: [
      'Thêm nhân viên',
      'Khởi tạo hồ sơ nhân sự mới trong gara kèm thông tin ngân hàng và chuyên môn.',
      'success',
    ],
    UPDATE_EMPLOYEE: [
      'Cập nhật hồ sơ nhân viên',
      'Sửa đổi thông tin nhân viên, chuyên môn kỹ thuật hoặc tài khoản ngân hàng.',
      'info',
    ],
    DEACTIVATE_EMPLOYEE: [
      'Ngừng hoạt động nhân viên',
      'Chuyển trạng thái nhân viên sang ngừng hoạt động, không xếp ca làm việc.',
      'danger',
    ],
  },
  shift: {
    CREATE_SHIFT: [
      'Tạo ca làm việc',
      'Thiết lập ca làm việc mới (Sáng / Chiều / Tối) trong ngày cho xưởng.',
      'success',
    ],
    CANCEL_SHIFT: [
      'Hủy ca làm việc',
      'Hủy bỏ ca làm việc đã thiết lập kèm giải phóng phân công nhân viên.',
      'danger',
    ],
    REGISTER_SHIFT: [
      'Đăng ký ca trực',
      'Kỹ thuật viên gửi yêu cầu tham gia ca làm việc trong tuần.',
      'info',
    ],
    APPROVE_SHIFT_REGISTRATION: [
      'Phê duyệt đăng ký ca',
      'Quản lý chấp thuận cho kỹ thuật viên tham gia ca trực.',
      'success',
    ],
    REJECT_SHIFT_REGISTRATION: [
      'Từ chối đăng ký ca',
      'Quản lý từ chối yêu cầu đăng ký ca của kỹ thuật viên kèm lý do.',
      'warning',
    ],
    SHIFT_CHECK_IN: [
      'Điểm danh vào ca',
      'Kỹ thuật viên bấm điểm danh bắt đầu thời gian làm việc trong ca.',
      'info',
    ],
    SHIFT_CHECK_OUT: [
      'Điểm danh tan ca',
      'Kỹ thuật viên điểm danh kết thúc ca và chốt số giờ làm việc thực tế.',
      'info',
    ],
  },
  payroll: {
    CREATE_PAYROLL_PERIOD: [
      'Khởi tạo kỳ tính lương',
      'Tạo kỳ lương mới cho tháng (từ ngày đầu tháng đến cuối tháng).',
      'success',
    ],
    CALCULATE_PAYROLL: [
      'Tính toán bảng lương',
      'Tổng hợp giờ công, hoa hồng và tính thực nhận cho toàn xưởng.',
      'info',
    ],
    ADJUST_PAYROLL_ITEM: [
      'Điều chỉnh lương nhân viên',
      'Cập nhật phụ cấp, tiền phạt/giảm trừ hoặc ghi chú cho từng nhân viên.',
      'warning',
    ],
    APPROVE_PAYROLL: [
      'Phê duyệt bảng lương',
      'Quản lý chốt duyệt số liệu bảng lương tháng, khóa không cho sửa đổi tự do.',
      'success',
    ],
    DISBURSE_PAYROLL: [
      'Chi trả lương toàn xưởng',
      'Xác nhận đã hoàn tất chuyển khoản/thanh toán tiền lương cho nhân viên trong kỳ.',
      'success',
    ],
    CALCULATE_ORDER_COMMISSION: [
      'Tính hoa hồng lệnh sửa',
      'Tính mức hoa hồng cho kỹ thuật viên dựa trên lệnh sửa chữa đã hoàn thành.',
      'info',
    ],
    UPDATE_COMMISSION_STATUS: [
      'Chuyển trạng thái hoa hồng',
      'Cập nhật trạng thái duyệt chi hoặc từ chối khoản hoa hồng của thợ.',
      'info',
    ],
  },
  bay: {
    CREATE_BAY: [
      'Thêm khoang sửa chữa',
      'Khởi tạo khoang sửa chữa mới (cầu nâng, khoang chẩn đoán, khoang đồng sơn).',
      'success',
    ],
    UPDATE_BAY: [
      'Cập nhật khoang sửa chữa',
      'Sửa đổi thông tin, thiết bị chuyên dụng hoặc công suất phục vụ của khoang.',
      'info',
    ],
    DEACTIVATE_BAY: [
      'Ngừng sử dụng khoang',
      'Tạm ngừng hoạt động khoang, không điều phối xe vào khu vực này.',
      'danger',
    ],
    SCHEDULE_MAINTENANCE: [
      'Lên lịch bảo trì khoang',
      'Đặt lịch bảo dưỡng định kỳ cầu nâng/thiết bị, tạm khóa lịch nhận xe.',
      'warning',
    ],
  },
  booking: {
    CREATE_BOOKING: [
      'Đặt lịch hẹn dịch vụ',
      'Tiếp nhận và xếp lịch hẹn bảo dưỡng/sửa chữa cho khách hàng.',
      'success',
    ],
    CANCEL_BOOKING: [
      'Hủy lịch hẹn',
      'Hủy lịch hẹn dịch vụ của khách theo yêu cầu kèm lý do.',
      'danger',
    ],
    CHECK_IN_VEHICLE: [
      'Tiếp nhận xe vào xưởng',
      'Lễ tân lập phiếu tiếp nhận xe, ghi nhận số ODO, lượng xăng và tình trạng vỏ xe.',
      'info',
    ],
    HANDOVER_VEHICLE: [
      'Bàn giao xe cho khách',
      'Hoàn tất sửa chữa, bàn giao chìa khóa và xe cho khách xuất xưởng.',
      'success',
    ],
  },
  repair: {
    CREATE_REPAIR_ORDER: [
      'Mở lệnh sửa chữa',
      'Lập lệnh sửa chữa mới cho xe đang ở xưởng kèm chẩn đoán sơ bộ.',
      'success',
    ],
    UPDATE_REPAIR_ORDER: [
      'Cập nhật lệnh sửa chữa',
      'Bổ sung hạng mục công việc hoặc thay đổi tiến độ thực hiện lệnh sửa chữa.',
      'info',
    ],
    ASSIGN_TECHNICIAN: [
      'Phân công kỹ thuật viên',
      'Chỉ định kỹ thuật viên phụ trách chính thi công lệnh sửa chữa.',
      'info',
    ],
    START_LABOR: [
      'Bắt đầu thi công hạng mục',
      'Kỹ thuật viên bấm giờ bắt đầu thực hiện công việc sửa chữa tại khoang.',
      'info',
    ],
    STOP_LABOR: [
      'Tạm dừng/Kết thúc thi công',
      'Dừng bấm giờ và ghi nhận số phút thao tác thực tế của thợ.',
      'info',
    ],
    COMPLETE_REPAIR: [
      'Hoàn thành sửa chữa',
      'Toàn bộ các hạng mục kỹ thuật đã thi công xong, sẵn sàng chuyển kiểm định KCS.',
      'success',
    ],
    CANCEL_REPAIR_ORDER: [
      'Hủy lệnh sửa chữa',
      'Hủy lệnh sửa chữa khi khách không đồng ý phương án kỹ thuật.',
      'danger',
    ],
  },
  qc_warranty: {
    CREATE_ESTIMATE: [
      'Lập báo giá dịch vụ',
      'Soạn phiếu báo giá chi tiết phụ tùng và tiền công gửi khách hàng.',
      'success',
    ],
    APPROVE_ESTIMATE_ITEMS: [
      'Khách hàng duyệt báo giá',
      'Khách xác nhận đồng ý các hạng mục thay thế và phương án thi công.',
      'success',
    ],
    REJECT_ESTIMATE_ITEMS: [
      'Khách từ chối báo giá',
      'Khách hàng từ chối một số hoặc toàn bộ hạng mục sửa chữa.',
      'warning',
    ],
    QC_PASSED: [
      'Nghiệm thu KCS đạt chuẩn',
      'Tổ trưởng kiểm tra chất lượng xuất xưởng xác nhận xe đạt tiêu chuẩn an toàn.',
      'success',
    ],
    QC_FAILED: [
      'Nghiệm thu KCS không đạt',
      'Xe chưa đạt yêu cầu kỹ thuật, yêu cầu kỹ thuật viên xử lý lại.',
      'danger',
    ],
    CREATE_WARRANTY: [
      'Kích hoạt bảo hành',
      'Kích hoạt thời hạn bảo hành cho phụ tùng thay mới và dịch vụ thi công.',
      'success',
    ],
    CLAIM_WARRANTY: [
      'Tiếp nhận yêu cầu bảo hành',
      'Ghi nhận sự cố phát sinh của xe còn trong thời hạn bảo hành.',
      'warning',
    ],
    DECIDE_CLAIM: [
      'Thẩm định yêu cầu bảo hành',
      'Phê duyệt hoặc từ chối xử lý bảo hành miễn phí cho khách hàng.',
      'info',
    ],
  },
  inventory_billing: {
    IMPORT_PARTS: [
      'Nhập kho phụ tùng',
      'Lập phiếu nhập kho phụ tùng/vật tư từ nhà cung cấp kèm giá vốn.',
      'success',
    ],
    EXPORT_PARTS: [
      'Xuất kho phụ tùng',
      'Xuất vật tư phụ tùng phục vụ thi công lệnh sửa chữa.',
      'info',
    ],
    RECONCILE_INVENTORY: [
      'Cân đối kiểm kê kho',
      'Điều chỉnh số lượng tồn kho thực tế sau khi kiểm kê đối soát.',
      'warning',
    ],
    ISSUE_INVOICE: [
      'Phát hành hóa đơn',
      'Xuất hóa đơn thanh toán tiền dịch vụ và phụ tùng cho khách hàng.',
      'success',
    ],
    CANCEL_INVOICE: [
      'Hủy hóa đơn dịch vụ',
      'Hủy bỏ hóa đơn do nhầm lẫn hoặc điều chỉnh thỏa thuận khách hàng.',
      'danger',
    ],
    RECORD_PAYMENT: [
      'Thu tiền thanh toán',
      'Ghi nhận khoản tiền mặt hoặc chuyển khoản khách hàng thanh toán cho hóa đơn.',
      'success',
    ],
  },
};

export const auditCatalog: Record<string, AuditActionMeta> = Object.fromEntries(
  (Object.entries(byDomain) as [AuditDomain, Record<string, Entry>][]).flatMap(
    ([domain, actions]) =>
      Object.entries(actions).map(([code, [label, description, severity]]) => [
        code,
        { label, description, domain, domainLabel: domainLabels[domain], severity },
      ]),
  ),
);

/** Backend class/entity names → Vietnamese. */
export const auditEntityLabels: Record<string, string> = {
  USER: 'Tài khoản',
  NOTIFICATION: 'Thông báo',
  Employee: 'Nhân viên',
  EmployeeShift: 'Ca làm việc',
  ServiceBay: 'Khoang sửa chữa',
  BayReservation: 'Lịch giữ khoang',
  PayrollPeriod: 'Kỳ lương',
  PayrollItem: 'Dòng lương nhân viên',
  OrderCommission: 'Khoản hoa hồng',
  Booking: 'Lịch hẹn',
  VehicleCheckIn: 'Phiếu tiếp nhận xe',
  Vehicle: 'Phương tiện',
  RepairOrder: 'Lệnh sửa chữa',
  LaborSession: 'Phiên thi công',
  Estimate: 'Báo giá',
  QualityInspection: 'Phiếu nghiệm thu KCS',
  WarrantyContract: 'Sổ bảo hành',
  WarrantyClaim: 'Yêu cầu bảo hành',
  InventoryReceipt: 'Phiếu nhập kho',
  InventoryIssue: 'Phiếu xuất kho',
  StockReconciliation: 'Phiếu kiểm kê',
  Part: 'Phụ tùng',
  Invoice: 'Hóa đơn',
  PaymentReceipt: 'Phiếu thu',
};

/** Unknown codes (newer backend than client) still render, flagged as plain information. */
export function getAuditActionMeta(action: string): AuditActionMeta {
  return (
    auditCatalog[action] ?? {
      label: action,
      description: 'Hành động chưa có mô tả trong từ điển kiểm toán.',
      domain: 'security',
      domainLabel: 'Chưa phân loại',
      severity: 'info',
    }
  );
}

export function formatAuditEntity(type: string, id: string | null, label?: string | null) {
  const name = auditEntityLabels[type] ?? type;
  return `${name}${label ? `: ${label}` : id ? ` #${id}` : ''}`;
}

export function auditActionsOf(domain: AuditDomain | '') {
  return Object.entries(auditCatalog).filter(([, meta]) => !domain || meta.domain === domain);
}
