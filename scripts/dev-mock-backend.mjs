import { createServer } from 'node:http';

const policy = {
  passwordMinLength: 8,
  passwordMaxUtf8Bytes: 72,
  paginationDefaultSize: 20,
  paginationMaxSize: 100,
  paginationMaxPage: 100000,
  reportMaxPeriodSeconds: 31622400,
  garageTimeZone: 'Asia/Bangkok',
};

const admin = {
  id: 1,
  username: 'admin',
  fullName: 'Quản trị Gara',
  email: 'admin@autogarage.local',
  role: 'ADMIN',
  active: true,
  version: 0,
  createdAt: '2026-10-15T01:00:00Z',
};

const member = {
  id: 2,
  username: 'reception',
  fullName: 'Nguyễn Minh Anh',
  email: 'reception@autogarage.local',
  role: 'RECEPTIONIST',
  active: true,
  version: 0,
  createdAt: '2026-10-16T08:00:00Z',
};

const customers = [
  { id: 1, fullName: 'Trần Văn An', phone: '0901234567', email: 'an.tran@example.com', address: '123 Lê Lợi, Q1, TP.HCM', debt: 0 },
  { id: 2, fullName: 'Lê Thị Bình', phone: '0912345678', email: 'binh.le@example.com', address: '456 Hai Bà Trưng, Q3, TP.HCM', debt: 500000 },
];

const vehicles = [
  { id: 1, customerId: 1, licensePlate: '51A-123.45', brand: 'Toyota', model: 'Vios', year: 2022, vin: 'VN123456789012345', currentOdo: 25000 },
  { id: 2, customerId: 2, licensePlate: '51B-678.90', brand: 'Honda', model: 'City', year: 2023, vin: 'VN987654321098765', currentOdo: 15000 },
];

const services = [
  { id: 1, code: 'SRV-001', name: 'Thay nhớt & lọc nhớt động cơ', price: 350000, estimatedMinutes: 30, active: true },
  { id: 2, code: 'SRV-002', name: 'Bảo dưỡng định kỳ 20,000 km', price: 1200000, estimatedMinutes: 90, active: true },
  { id: 3, code: 'SRV-003', name: 'Kiểm tra & cân chỉnh góc đặt bánh xe', price: 450000, estimatedMinutes: 45, active: true },
];

const bookings = [
  { id: 1, code: 'BK-20261027-001', customerId: 1, customerName: 'Trần Văn An', phone: '0901234567', licensePlate: '51A-123.45', serviceName: 'Thay nhớt & lọc nhớt', appointmentTime: '2026-10-27T09:00:00Z', status: 'CONFIRMED' },
  { id: 2, code: 'BK-20261027-002', customerId: 2, customerName: 'Lê Thị Bình', phone: '0912345678', licensePlate: '51B-678.90', serviceName: 'Bảo dưỡng 20,000 km', appointmentTime: '2026-10-27T14:30:00Z', status: 'PENDING' },
];

const suppliers = [
  {
    id: '1',
    supplierCode: 'SUP-BOSCH',
    supplierName: 'Bosch Vietnam Co.',
    phone: '02838383838',
    email: 'sales@bosch.com.vn',
    address: '33 Lê Duẩn, Q1, TP.HCM',
    taxCode: '0300123456',
    paymentTermDays: 30,
    isActive: true,
    version: '0',
    createdAt: '2026-10-15T08:00:00Z',
    updatedAt: '2026-10-15T08:00:00Z',
  },
  {
    id: '2',
    supplierCode: 'SUP-CASTROL',
    supplierName: 'Castrol Vietnam Distribution',
    phone: '02839393939',
    email: 'orders@castrol.vn',
    address: 'KCN Hiệp Phước, Nhà Bè, TP.HCM',
    taxCode: '0300987654',
    paymentTermDays: 45,
    isActive: true,
    version: '0',
    createdAt: '2026-10-16T09:00:00Z',
    updatedAt: '2026-10-16T09:00:00Z',
  },
  {
    id: '3',
    supplierCode: 'SUP-DENSO',
    supplierName: 'Denso Auto Parts VN',
    phone: '02437654321',
    email: 'contact@denso.vn',
    address: 'KCN Thăng Long, Đông Anh, Hà Nội',
    taxCode: '0100234567',
    paymentTermDays: 15,
    isActive: false,
    version: '0',
    createdAt: '2026-10-17T10:00:00Z',
    updatedAt: '2026-10-17T10:00:00Z',
  },
];

const parts = [
  {
    id: '1',
    sku: 'PRT-OIL-5W30',
    partName: 'Nhớt tổng hợp Castrol Magnatec 5W-30 (Can 4L)',
    unit: 'Can',
    description: 'Dầu nhớt bôi trơn cao cấp dành cho động cơ xăng & diesel',
    preferredSupplierId: '2',
    preferredSupplierName: 'Castrol Vietnam Distribution',
    purchasePrice: '450000',
    sellingPrice: '650000',
    stockQuantity: '45',
    reservedQuantity: '5',
    availableQuantity: '40',
    reorderLevel: '10',
    warrantyDays: 180,
    warrantyKm: 5000,
    warrantyTerms: 'Lỗi nhà sản xuất',
    isActive: true,
    version: '0',
    createdAt: '2026-10-18T08:00:00Z',
    updatedAt: '2026-10-18T08:00:00Z',
  },
  {
    id: '2',
    sku: 'PRT-FLT-TOY',
    partName: 'Lọc nhớt Toyota Vios / Yaris chính hãng',
    unit: 'Cái',
    description: 'Lọc cặn và mạt sắt bảo vệ động cơ',
    preferredSupplierId: '1',
    preferredSupplierName: 'Bosch Vietnam Co.',
    purchasePrice: '80000',
    sellingPrice: '120000',
    stockQuantity: '8',
    reservedQuantity: '3',
    availableQuantity: '5',
    reorderLevel: '15',
    warrantyDays: 90,
    warrantyKm: 5000,
    warrantyTerms: '1 đổi 1 trong 3 tháng',
    isActive: true,
    version: '0',
    createdAt: '2026-10-18T09:00:00Z',
    updatedAt: '2026-10-18T09:00:00Z',
  },
  {
    id: '3',
    sku: 'PRT-PAD-BRK',
    partName: 'Bộ má phanh trước Vios / City Ceramic',
    unit: 'Bộ',
    description: 'Má phanh gốm giảm ồn và chịu nhiệt độ cao',
    preferredSupplierId: '1',
    preferredSupplierName: 'Bosch Vietnam Co.',
    purchasePrice: '550000',
    sellingPrice: '850000',
    stockQuantity: '20',
    reservedQuantity: '2',
    availableQuantity: '18',
    reorderLevel: '5',
    warrantyDays: 365,
    warrantyKm: 15000,
    warrantyTerms: 'Bảo hành hao mòn bất thường',
    isActive: true,
    version: '0',
    createdAt: '2026-10-19T08:00:00Z',
    updatedAt: '2026-10-19T08:00:00Z',
  },
];

const inventoryDocuments = [
  {
    id: '1',
    documentCode: 'PNK-202610-001',
    documentType: 'IN',
    status: 'POSTED',
    supplierId: '1',
    supplierName: 'Bosch Vietnam Co.',
    repairOrderId: null,
    repairOrderCode: null,
    supplierInvoiceNumber: 'HD-88912',
    totalAmount: '12400000',
    totalQuantity: '50',
    reason: 'Nhập phụ tùng định kỳ tháng 10',
    postedAt: '2026-10-20T10:00:00Z',
    postedByName: 'Quản trị Gara',
    cancelledAt: null,
    cancelledByName: null,
    version: '0',
    createdAt: '2026-10-20T09:00:00Z',
    items: [
      {
        id: '1',
        lineNo: 1,
        partId: '2',
        partSku: 'PRT-FLT-TOY',
        partName: 'Lọc nhớt Toyota Vios / Yaris',
        unit: 'Cái',
        quantity: '30',
        unitCost: '80000',
        lineTotal: '2400000',
      },
      {
        id: '2',
        lineNo: 2,
        partId: '3',
        partSku: 'PRT-PAD-BRK',
        partName: 'Bộ má phanh trước Vios / City',
        unit: 'Bộ',
        quantity: '20',
        unitCost: '500000',
        lineTotal: '10000000',
      },
    ],
  },
  {
    id: '2',
    documentCode: 'PXK-202610-002',
    documentType: 'OUT',
    status: 'POSTED',
    supplierId: null,
    supplierName: null,
    repairOrderId: '101',
    repairOrderCode: 'RO-202610-045',
    supplierInvoiceNumber: null,
    totalAmount: '1500000',
    totalQuantity: '3',
    reason: 'Xuất thay nhớt & lọc bảo dưỡng xe 51A-123.45',
    postedAt: '2026-10-22T14:30:00Z',
    postedByName: 'Quản trị Gara',
    cancelledAt: null,
    cancelledByName: null,
    version: '0',
    createdAt: '2026-10-22T14:00:00Z',
    items: [
      {
        id: '3',
        lineNo: 1,
        partId: '1',
        partSku: 'PRT-OIL-5W30',
        partName: 'Nhớt tổng hợp Castrol 5W-30',
        unit: 'Can',
        quantity: '1',
        unitCost: '450000',
        lineTotal: '450000',
      },
    ],
  },
  {
    id: '3',
    documentCode: 'PNK-202610-003',
    documentType: 'IN',
    status: 'DRAFT',
    supplierId: '2',
    supplierName: 'Castrol Vietnam Distribution',
    repairOrderId: null,
    repairOrderCode: null,
    supplierInvoiceNumber: 'CAS-99120',
    totalAmount: '9000000',
    totalQuantity: '20',
    reason: 'Đơn đặt nhớt can bổ sung kho',
    postedAt: null,
    postedByName: null,
    cancelledAt: null,
    cancelledByName: null,
    version: '0',
    createdAt: '2026-10-25T11:00:00Z',
    items: [
      {
        id: '4',
        lineNo: 1,
        partId: '1',
        partSku: 'PRT-OIL-5W30',
        partName: 'Nhớt tổng hợp Castrol 5W-30',
        unit: 'Can',
        quantity: '20',
        unitCost: '450000',
        lineTotal: '9000000',
      },
    ],
  },
];

const reconciliations = [
  {
    partId: '1',
    sku: 'PRT-OIL-5W30',
    partName: 'Nhớt tổng hợp Castrol Magnatec 5W-30',
    stockQuantity: '45',
    transactionBalance: '45',
    difference: '0',
    matched: true,
    reservedQuantity: '5',
    availableQuantity: '40',
    unitCost: '450000',
  },
  {
    partId: '2',
    sku: 'PRT-FLT-TOY',
    partName: 'Lọc nhớt Toyota Vios / Yaris',
    stockQuantity: '8',
    transactionBalance: '8',
    difference: '0',
    matched: true,
    reservedQuantity: '3',
    availableQuantity: '5',
    unitCost: '80000',
  },
  {
    partId: '3',
    sku: 'PRT-PAD-BRK',
    partName: 'Bộ má phanh trước Vios / City Ceramic',
    stockQuantity: '20',
    transactionBalance: '20',
    difference: '0',
    matched: true,
    reservedQuantity: '2',
    availableQuantity: '18',
    unitCost: '550000',
  },
];

const invoices = [
  {
    id: '1',
    invoiceCode: 'INV-202610-001',
    repairOrderId: '101',
    customerId: '1',
    customerSnapshot: {
      fullName: 'Trần Văn An',
      phone: '0901234567',
      address: '123 Lê Lợi, Q1, TP.HCM',
    },
    vehicleSnapshot: {
      plateNumber: '51A-123.45',
      make: 'Toyota',
      model: 'Vios',
      year: 2022,
    },
    status: 'PAID',
    subtotal: '1550000',
    discountAmount: '50000',
    taxAmount: '150000',
    totalAmount: '1650000',
    paidAmount: '1650000',
    balanceAmount: '0',
    issuedByName: 'Nguyễn Minh Anh',
    issuedAt: '2026-10-22T16:00:00Z',
    dueDate: '2026-10-29T16:00:00Z',
    voidReason: null,
    voidedByName: null,
    voidedAt: null,
    version: '0',
    items: [
      {
        id: '1',
        lineNo: 1,
        itemType: 'SERVICE',
        repairTaskId: '1',
        partId: null,
        itemName: 'Bảo dưỡng định kỳ & kiểm tra hệ thống phanh',
        unit: 'Gói',
        quantity: '1',
        unitPrice: '900000',
        unitCost: '0',
        lineTotal: '900000',
        warrantyDays: 30,
        warrantyKm: 1000,
        warrantyTerms: 'Bảo hành dịch vụ gara',
      },
      {
        id: '2',
        lineNo: 2,
        itemType: 'PART',
        repairTaskId: null,
        partId: '1',
        itemName: 'Nhớt tổng hợp Castrol Magnatec 5W-30',
        unit: 'Can',
        quantity: '1',
        unitPrice: '650000',
        unitCost: '450000',
        lineTotal: '650000',
        warrantyDays: 180,
        warrantyKm: 5000,
        warrantyTerms: 'Lỗi nhà sản xuất',
      },
    ],
  },
  {
    id: '2',
    invoiceCode: 'INV-202610-002',
    repairOrderId: '102',
    customerId: '2',
    customerSnapshot: {
      fullName: 'Lê Thị Bình',
      phone: '0912345678',
      address: '456 Hai Bà Trưng, Q3, TP.HCM',
    },
    vehicleSnapshot: {
      plateNumber: '51B-678.90',
      make: 'Honda',
      model: 'City',
      year: 2023,
    },
    status: 'PARTIALLY_PAID',
    subtotal: '2400000',
    discountAmount: '0',
    taxAmount: '240000',
    totalAmount: '2640000',
    paidAmount: '1500000',
    balanceAmount: '1140000',
    issuedByName: 'Nguyễn Minh Anh',
    issuedAt: '2026-10-25T11:30:00Z',
    dueDate: '2026-11-01T11:30:00Z',
    voidReason: null,
    voidedByName: null,
    voidedAt: null,
    version: '0',
    items: [
      {
        id: '3',
        lineNo: 1,
        itemType: 'SERVICE',
        repairTaskId: '2',
        partId: null,
        itemName: 'Thay thế má phanh trước và láng đĩa phanh',
        unit: 'Gói',
        quantity: '1',
        unitPrice: '1550000',
        unitCost: '0',
        lineTotal: '1550000',
        warrantyDays: 60,
        warrantyKm: 2000,
        warrantyTerms: 'Bảo hành lắp đặt gara',
      },
      {
        id: '4',
        lineNo: 2,
        itemType: 'PART',
        repairTaskId: null,
        partId: '3',
        itemName: 'Bộ má phanh trước Vios / City Ceramic',
        unit: 'Bộ',
        quantity: '1',
        unitPrice: '850000',
        unitCost: '550000',
        lineTotal: '850000',
        warrantyDays: 365,
        warrantyKm: 15000,
        warrantyTerms: 'Bảo hành hao mòn',
      },
    ],
  },
];

const payments = [
  {
    id: '1',
    transactionCode: 'PAY-202610-001',
    direction: 'IN',
    invoiceId: '1',
    invoiceCode: 'INV-202610-001',
    inventoryDocumentId: null,
    documentCode: null,
    amount: '1650000',
    paymentMethod: 'VIETQR',
    providerReference: 'QR-BIDV-992182',
    paidAt: '2026-10-22T16:05:00Z',
    recordedByName: 'Nguyễn Minh Anh',
    notes: 'Khách thanh toán quét mã QR qua VietQR',
  },
  {
    id: '2',
    transactionCode: 'PAY-202610-002',
    direction: 'IN',
    invoiceId: '2',
    invoiceCode: 'INV-202610-002',
    inventoryDocumentId: null,
    documentCode: null,
    amount: '1500000',
    paymentMethod: 'CASH',
    providerReference: null,
    paidAt: '2026-10-25T11:35:00Z',
    recordedByName: 'Nguyễn Minh Anh',
    notes: 'Khách thanh toán tạm ứng tiền mặt tại quầy',
  },
  {
    id: '3',
    transactionCode: 'PAY-202610-003',
    direction: 'OUT',
    invoiceId: null,
    invoiceCode: null,
    inventoryDocumentId: '1',
    documentCode: 'PNK-202610-001',
    amount: '12400000',
    paymentMethod: 'BANK_TRANSFER',
    providerReference: 'FT2629810239',
    paidAt: '2026-10-24T09:15:00Z',
    recordedByName: 'Quản trị Gara',
    notes: 'Chuyển khoản thanh toán tiền hàng cho Bosch',
  },
];

const customerDebts = [
  {
    id: '2',
    customerId: '2',
    customerCode: 'CUST-002',
    fullName: 'Lê Thị Bình',
    phone: '0912345678',
    creditLimit: '5000000',
    totalInvoiceAmount: '2640000',
    paidAmount: '1500000',
    balanceAmount: '1140000',
    canCloseOrder: true,
  },
];

const supplierDebts = [
  {
    id: '1',
    supplierId: '1',
    supplierCode: 'SUP-BOSCH',
    supplierName: 'Bosch Vietnam Co.',
    phone: '02838383838',
    totalDocumentAmount: '24800000',
    paidAmount: '12400000',
    balanceAmount: '12400000',
    paymentTermDays: 30,
  },
];

function envelope(data, code = 1000, success = true, message = 'Thành công') {
  return JSON.stringify({
    code,
    message,
    success,
    data,
    timestamp: new Date().toISOString(),
    path: '/api/v1',
  });
}

function page(items) {
  return {
    content: items,
    totalElements: items.length,
    totalPages: 1,
    number: 0,
    size: 20,
  };
}

let activeSession = null;

// Backend API Server on 8080
const apiServer = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const method = req.method;
  console.log(`[API 8080] ${method} ${url.pathname}`);

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Public UI Policy
  if (url.pathname === '/api/v1/public/ui-policy') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(policy));
    return;
  }

  // Session refresh: return 401 when unauthenticated
  if (url.pathname === '/api/v1/auth/session/refresh') {
    if (!activeSession) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(envelope(null, 3003, false, 'Chưa đăng nhập'));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope({ accessToken: 'mock-token', expiresIn: 3600, tokenType: 'Bearer' }));
    return;
  }

  // Session open
  if (url.pathname === '/api/v1/auth/session') {
    if (method === 'DELETE') {
      activeSession = null;
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(envelope(null));
      return;
    }
    activeSession = admin;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope({ accessToken: 'mock-token', expiresIn: 3600, tokenType: 'Bearer' }));
    return;
  }

  // Logout
  if (url.pathname === '/api/v1/auth/logout') {
    activeSession = null;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(null));
    return;
  }

  // Current user info
  if (url.pathname === '/api/v1/auth/me') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(activeSession || admin));
    return;
  }

  // Register
  if (url.pathname === '/api/v1/auth/register') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(null, 1000, true, 'Đăng ký thành công'));
    return;
  }

  // Admin users list
  if (url.pathname === '/api/v1/admin/users' || url.pathname === '/api/v1/users') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page([admin, member])));
    return;
  }

  // Customers
  if (url.pathname.startsWith('/api/v1/customers')) {
    if (url.pathname === '/api/v1/customers/me') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(envelope(customers[0]));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(customers)));
    return;
  }

  // Vehicles
  if (url.pathname.startsWith('/api/v1/vehicles')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(vehicles)));
    return;
  }

  // Services catalog
  if (url.pathname.startsWith('/api/v1/services') || url.pathname.startsWith('/api/v1/catalog/services')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(services)));
    return;
  }

  // Bookings
  if (url.pathname.startsWith('/api/v1/bookings')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(bookings)));
    return;
  }

  // Parts / Inventory
  if (url.pathname.startsWith('/api/v1/inventory/reconciliation') || url.pathname.startsWith('/api/v1/reconciliation')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(reconciliations));
    return;
  }

  if (url.pathname.startsWith('/api/v1/inventory/documents') || url.pathname.startsWith('/api/v1/documents')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(inventoryDocuments)));
    return;
  }

  if (url.pathname.startsWith('/api/v1/parts') || url.pathname.startsWith('/api/v1/inventory/parts')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(parts)));
    return;
  }

  // Suppliers
  if (url.pathname.startsWith('/api/v1/suppliers') || url.pathname.startsWith('/api/v1/inventory/suppliers')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(suppliers)));
    return;
  }

  // Billing: Invoices, Payments, Debts
  if (url.pathname.startsWith('/api/v1/billing/invoices') || url.pathname.startsWith('/api/v1/invoices')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(invoices)));
    return;
  }

  if (url.pathname.startsWith('/api/v1/billing/payments') || url.pathname.startsWith('/api/v1/payments')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(payments)));
    return;
  }

  if (url.pathname.startsWith('/api/v1/billing/debts/customers')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(customerDebts)));
    return;
  }

  if (url.pathname.startsWith('/api/v1/billing/debts/suppliers')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page(supplierDebts)));
    return;
  }

  // Notifications
  if (url.pathname.startsWith('/api/v1/notifications')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page([])));
    return;
  }

  // Audit
  if (url.pathname.startsWith('/api/v1/audit')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope(page([])));
    return;
  }

  // Reports
  if (url.pathname.startsWith('/api/v1/reports')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(envelope({ totalRevenue: 158000000, repairCount: 42, activeCustomers: 120 }));
    return;
  }

  // Fallback for any other API
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(envelope(page([])));
});

// Keycloak Server on 8081
const keycloakServer = createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const method = req.method;
  console.log(`[Keycloak 8081] ${method} ${url.pathname}`);

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (url.pathname.includes('/protocol/openid-connect/token')) {
    activeSession = admin;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      access_token: 'mock-access-token-12345',
      refresh_token: 'mock-refresh-token-12345',
      expires_in: 3600,
      token_type: 'Bearer',
    }));
    return;
  }

  if (url.pathname.includes('/protocol/openid-connect/logout')) {
    activeSession = null;
    res.writeHead(204);
    res.end();
    return;
  }

  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ realm: 'autogarage-realm', public_key: 'mock' }));
});

apiServer.listen(8080, '127.0.0.1', () => {
  console.log('Mock Backend API listening on http://127.0.0.1:8080');
});

keycloakServer.listen(8081, '127.0.0.1', () => {
  console.log('Mock Keycloak listening on http://127.0.0.1:8081');
});
