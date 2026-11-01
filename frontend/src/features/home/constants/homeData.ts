export interface ServiceCardData {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly tag: string;
  readonly description: string;
  readonly priceDisplay: string;
  readonly durationDisplay: string;
  readonly image: string;
  readonly iconName: string;
}

export interface WorkshopBayInfo {
  readonly bayCode: string;
  readonly bayName: string;
  readonly bayType: string;
  readonly specialty: string;
  readonly equipment: string;
  readonly image: string;
}

export interface GarageStats {
  readonly carsServiced: string;
  readonly technicalBays: number;
  readonly genuinePartsRatio: string;
  readonly ratingScore: string;
  readonly reviewCount: string;
}

export interface WorkflowStep {
  readonly title: string;
  readonly detail: string;
}

export interface Testimonial {
  readonly name: string;
  readonly vehicle: string;
  readonly quote: string;
}

export interface NavAnchor {
  readonly id: string;
  readonly label: string;
}

/** Ảnh cục bộ dùng khi ảnh không tải được. */
export const FALLBACK_IMAGE = '/images/home/hero-banner.jpg';

export const GARAGE_INFO = {
  name: 'AutoGarage',
  hotline: '0963 943 872',
  mobile: '0963 943 872',
  address: 'Số 226 Phố Tây Sơn, P. Trung Liệt, Q. Đống Đa, Hà Nội',
  openingHours: '08:00 – 18:00 hằng ngày',
} as const;

export const HERO_IMAGE = '/images/home/hero-banner.jpg';
export const LOUNGE_IMAGE = '/images/home/garage-lounge.jpg';

export const NAV_ANCHORS: readonly NavAnchor[] = [
  { id: 'services', label: 'Dịch vụ' },
  { id: 'workflow', label: 'Quy trình' },
  { id: 'workshop', label: 'Khoang kỹ thuật' },
  { id: 'parts', label: 'Phụ tùng chính hãng' },
  { id: 'pricing', label: 'Bảng giá' },
  { id: 'contact', label: 'Liên hệ' },
];

export const GARAGE_STATS: GarageStats = {
  carsServiced: '15.000+',
  technicalBays: 6,
  genuinePartsRatio: '100%',
  ratingScore: '4.9 / 5.0',
  reviewCount: '3.000',
};

export const SERVICES: readonly ServiceCardData[] = [
  {
    id: 'periodic',
    code: 'SV-PERIODIC',
    name: 'Bảo dưỡng định kỳ cấp 1 – 4',
    tag: '5.000 – 40.000 km',
    description: 'Thay dầu, lọc, kiểm tra 56 hạng mục theo khuyến cáo nhà sản xuất.',
    priceDisplay: 'Từ 850.000đ',
    durationDisplay: '60 – 120 phút',
    image: '/images/home/service-periodic.jpg',
    iconName: 'Wrench',
  },
  {
    id: 'alignment',
    code: 'SV-ALIGN',
    name: 'Cân chỉnh góc lái & độ chụm 3D',
    tag: 'Hunter Hawkeye Elite',
    description: 'Đo và hiệu chỉnh góc đặt bánh xe bằng hệ thống 3D, in báo cáo trước và sau.',
    priceDisplay: 'Từ 450.000đ',
    durationDisplay: '45 phút',
    image: '/images/home/service-alignment.jpg',
    iconName: 'Gauge',
  },
  {
    id: 'engine',
    code: 'SV-ENGINE',
    name: 'Sửa chữa & đại tu máy – gầm – hộp số',
    tag: 'Hộp số tự động',
    description: 'Tháo kiểm, thay thế cụm chi tiết, chạy thử và nghiệm thu theo QC.',
    priceDisplay: 'Báo giá theo xe',
    durationDisplay: '1 – 5 ngày',
    image: '/images/home/service-engine.jpg',
    iconName: 'Cog',
  },
  {
    id: 'diagnostic',
    code: 'SV-DIAG',
    name: 'Chẩn đoán điện tử OBD-II & điện lạnh',
    tag: 'Quét mã lỗi',
    description: 'Đọc lỗi ECU, kiểm tra hệ thống điện, nạp gas và vệ sinh dàn lạnh.',
    priceDisplay: 'Từ 300.000đ',
    durationDisplay: '30 – 90 phút',
    image: '/images/home/service-diagnostic.jpg',
    iconName: 'ScanLine',
  },
  {
    id: 'paint',
    code: 'SV-PAINT',
    name: 'Gò nắn thân vỏ & sơn sấy',
    tag: 'Phòng sơn sấy',
    description: 'Phục hồi thân vỏ, sơn sấy hấp và đánh bóng, phủ ceramic bảo vệ nước sơn.',
    priceDisplay: 'Từ 1.200.000đ',
    durationDisplay: '2 – 4 ngày',
    image: '/images/home/service-paint.jpg',
    iconName: 'PaintBucket',
  },
  {
    id: 'brake',
    code: 'SV-BRAKE',
    name: 'Hệ thống phanh & thay lốp chính hãng',
    tag: 'Phanh – Lốp',
    description: 'Thay má phanh, đĩa phanh, cân bằng động và thay lốp chính hãng.',
    priceDisplay: 'Từ 600.000đ',
    durationDisplay: '60 – 90 phút',
    image: '/images/home/service-brake.jpg',
    iconName: 'Disc3',
  },
];

export const WORKSHOP_BAYS: readonly WorkshopBayInfo[] = [
  {
    bayCode: 'BAY-01',
    bayName: 'Khoang bảo dưỡng nhanh',
    bayType: 'Cầu 2 trụ',
    specialty: 'Bảo dưỡng định kỳ, thay dầu',
    equipment: 'Cầu nâng 2 trụ 4 tấn',
    image: '/images/home/bay-01.jpg',
  },
  {
    bayCode: 'BAY-02',
    bayName: 'Khoang cân chỉnh 3D',
    bayType: 'Cầu 4 trụ Hunter',
    specialty: 'Cân chỉnh góc đặt bánh xe',
    equipment: 'Hunter Hawkeye Elite',
    image: '/images/home/bay-02.jpg',
  },
  {
    bayCode: 'BAY-03',
    bayName: 'Khoang máy – gầm',
    bayType: 'Cầu 2 trụ',
    specialty: 'Đại tu máy, hộp số',
    equipment: 'Palăng nâng động cơ',
    image: '/images/home/bay-03.jpg',
  },
  {
    bayCode: 'BAY-04',
    bayName: 'Khoang chẩn đoán',
    bayType: 'Cầu cắt kéo',
    specialty: 'Điện – điện lạnh, OBD-II',
    equipment: 'Máy chẩn đoán đa hãng',
    image: '/images/home/bay-04.jpg',
  },
  {
    bayCode: 'BAY-05',
    bayName: 'Khoang phanh – lốp',
    bayType: 'Cầu cắt kéo',
    specialty: 'Phanh, lốp, cân bằng động',
    equipment: 'Máy ép lốp, cân bằng động',
    image: '/images/home/bay-05.jpg',
  },
  {
    bayCode: 'BAY-06',
    bayName: 'Khoang nghiệm thu QC',
    bayType: 'Khoang QC',
    specialty: 'Kiểm định xuất xưởng',
    equipment: 'Bàn kiểm tra 56 hạng mục',
    image: '/images/home/bay-06.jpg',
  },
];

export const WORKFLOW_STEPS: readonly WorkflowStep[] = [
  { title: 'Tiếp nhận & khám xe', detail: 'Lập biên bản tiếp nhận kỹ thuật số ngay khi xe vào xưởng.' },
  { title: 'Chẩn đoán & báo giá', detail: 'Tách rõ tiền công và phụ tùng, không phát sinh ngoài báo giá.' },
  { title: 'Khách hàng duyệt online', detail: 'Duyệt hoặc từ chối từng hạng mục qua SMS hoặc cổng khách hàng.' },
  { title: 'Sửa chữa chuẩn hãng', detail: 'Kỹ thuật viên bậc cao thực hiện, cập nhật tiến độ theo thời gian thực.' },
  { title: 'Nghiệm thu QC & bàn giao', detail: 'Kiểm định xuất xưởng, cấp bảo hành điện tử cho từng hạng mục.' },
];

export const PARTNER_BRANDS: readonly string[] = [
  'Michelin',
  'Brembo',
  'Denso',
  'Motul',
  'Bosch',
  'Castrol',
  'Prestone',
];

export const PARTS_IMAGE = '/images/home/parts-ecosystem.jpg';

export const PARTS_COMMITMENTS: readonly string[] = [
  'Nguồn gốc xuất xứ rõ ràng, có chứng từ nhập kho',
  'Bảo hành phụ tùng tối đa 36 tháng',
  'Theo dõi tồn kho và lô hàng theo từng lệnh sửa chữa',
];

export const TESTIMONIALS: readonly Testimonial[] = [
  {
    name: 'Anh Nguyễn Văn Hùng',
    vehicle: 'Toyota Camry',
    quote: 'Báo giá rõ từng hạng mục, duyệt qua điện thoại nên tôi không phải chờ ở xưởng.',
  },
  {
    name: 'Chị Trần Thu Hà',
    vehicle: 'Mazda CX-5',
    quote: 'Xưởng sạch sẽ, xe giao đúng hẹn và có biên bản nghiệm thu đầy đủ.',
  },
  {
    name: 'Anh Lê Quốc Bảo',
    vehicle: 'Ford Ranger Wildtrak',
    quote: 'Thay phụ tùng chính hãng, bảo hành điện tử tra cứu được ngay khi cần.',
  },
];
