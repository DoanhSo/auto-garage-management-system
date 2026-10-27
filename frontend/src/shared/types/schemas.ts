import { z } from 'zod';
import { roles, customerTypes } from './contracts';
export const idSchema = z.string().regex(/^[1-9]\d*$/);
export const userSchema = z.object({
  id: idSchema,
  identityId: z.string().nullable().optional(),
  username: z.string(),
  email: z.string().nullable(),
  fullName: z.string(),
  role: z.enum(roles),
  active: z.boolean(),
  version: z.string().regex(/^\d+$/),
  createdAt: z.string(),
});
export const browserSessionSchema = z.object({
  accessToken: z.string().min(1),
  expiresIn: z.number().int().positive(),
  tokenType: z.literal('Bearer'),
});
export const oidcTokenSchema = z.object({
  access_token: z.string().min(1),
  refresh_token: z.string().min(1).optional(),
  expires_in: z.number().int().positive().default(1800),
  token_type: z.string().default('Bearer'),
});
export const loginResultSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().nullable().optional(),
  expiresIn: z.number().int().positive().optional(),
  tokenType: z.literal('Bearer'),
  expiresAt: z.string(),
  user: userSchema,
});
export function pageSchema<T extends z.ZodType>(item: T) {
  return z.object({
    content: z.array(item),
    totalElements: z.string().regex(/^\d+$/),
    totalPages: z.number().int().nonnegative(),
    number: z.number().int().nonnegative(),
    size: z.number().int().positive(),
  });
}
export const noticeSchema = z.object({
  id: idSchema,
  userId: idSchema.nullable().optional(),
  channel: z.string(),
  subject: z.string().nullable(),
  payloadJson: z.string(),
  status: z.string(),
  readAt: z.string().nullable(),
  createdAt: z.string(),
});
export const noticeBroadcastResultSchema = z.object({
  eventKey: z.string().optional(),
  recipientCount: z.number().int().nonnegative(),
  targetRole: z.enum(roles).nullable().optional().default(null),
  targetRoles: z.array(z.enum(roles)).optional(),
  channel: z.string().optional(),
  subject: z.string().nullable().optional(),
});
export const auditSchema = z.object({
  id: idSchema,
  actorUserId: idSchema.nullable(),
  action: z.string(),
  entityType: z.string(),
  entityId: idSchema.nullable(),
  afterJson: z.string().nullable(),
  createdAt: z.string(),
});
const decimal = z.string().regex(/^-?\d+(\.\d+)?([eE][+-]?\d+)?$/);
export const dashboardSchema = z.object({
  revenue: decimal,
  cashIn: decimal,
  cashOut: decimal,
  customerDebt: decimal,
  supplierDebt: decimal,
  stockValue: decimal,
  laborHours: decimal,
  from: z.string(),
  until: z.string(),
  snapshotAt: z.string(),
});
export const invoiceSchema = z.object({
  id: idSchema,
  invoiceCode: z.string(),
  customerId: idSchema,
  totalAmount: decimal,
  paidAmount: decimal,
  balanceAmount: decimal,
});

export const customerTypeSchema = z.enum(customerTypes);
export const customerSchema = z.object({
  id: idSchema,
  userId: idSchema.nullable(),
  customerCode: z.string(),
  fullName: z.string(),
  customerType: customerTypeSchema,
  phone: z
    .string()
    .nullable()
    .transform((value) => value ?? ''),
  email: z.string().nullable(),
  address: z.string().nullable(),
  companyName: z.string().nullable(),
  taxCode: z.string().nullable(),
  creditLimit: decimal,
  paymentTermDays: z.number().int().nonnegative(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.string().regex(/^\d+$/),
});

export const vehicleSchema = z.object({
  id: idSchema,
  customerId: idSchema,
  customerName: z.string(),
  customerPhone: z
    .string()
    .nullable()
    .transform((value) => value ?? ''),
  licensePlate: z.string(),
  vin: z.string().nullable(),
  engineNumber: z.string().nullable(),
  brand: z.string(),
  model: z.string(),
  manufactureYear: z.number().int().nullable(),
  color: z.string().nullable(),
  fuelType: z.string().nullable(),
  odometer: z.number().int().nonnegative(),
  inspectionDueDate: z.string().nullable(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.string().regex(/^\d+$/),
});

export const technicalAttachmentSchema = z.object({
  id: idSchema,
  fileName: z.string(),
  fileUrl: z.string(),
  category: z.string(),
  fileSize: z.number().optional(),
  contentType: z.string().optional(),
});

export const vehicleHistoryEntrySchema = z.object({
  repairOrderId: idSchema,
  orderCode: z.string(),
  receivedAt: z.string(),
  completedAt: z.string().nullable(),
  odometerIn: z.number().int().nonnegative(),
  customerComplaint: z.string().nullable(),
  diagnosis: z.string().nullable(),
  status: z.string(),
  orderCustomerId: idSchema,
  isCurrentOwnerRecord: z.boolean(),
  totalAmount: decimal.nullable(),
  tasksDone: z.array(z.string()),
  partsReplaced: z.array(z.string()),
  technicalAttachments: z.array(technicalAttachmentSchema),
});

export const vehicleTimelineSchema = z.object({
  vehicleId: idSchema,
  licensePlate: z.string(),
  brand: z.string(),
  model: z.string(),
  currentOdometer: z.number().int().nonnegative(),
  currentCustomerId: idSchema,
  currentCustomerName: z.string(),
  historyEntries: z.array(vehicleHistoryEntrySchema),
});

export const serviceTypeSchema = z.enum(['SINGLE', 'PACKAGE']);

export const serviceSchema = z.object({
  id: idSchema,
  serviceCode: z.string(),
  serviceName: z.string(),
  serviceType: serviceTypeSchema,
  description: z.string().nullable(),
  unit: z.string(),
  unitPrice: decimal,
  standardMinutes: z.number().int(),
  requiredBayType: z.string().nullable(),
  requiredSkillsJson: z.string().nullable(),
  intervalKm: z.number().int().nullable(),
  intervalMonths: z.number().int().nullable(),
  warrantyDays: z.number().int(),
  warrantyKm: z.number().int(),
  warrantyTerms: z.string().nullable(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.string().regex(/^\d+$/),
});

export const bookingStatusSchema = z.enum([
  'PENDING',
  'CONFIRMED',
  'CANCELLED',
  'NO_SHOW',
  'RECEIVED',
]);

export const bookingSchema = z.object({
  id: idSchema,
  bookingCode: z.string(),
  customerId: idSchema,
  customerName: z.string(),
  customerPhone: z
    .string()
    .nullable()
    .transform((value) => value ?? ''),
  vehicleId: idSchema,
  licensePlate: z.string(),
  vehicleModel: z.string().nullable().optional(),
  serviceId: idSchema.nullable().optional(),
  serviceName: z.string().nullable().optional(),
  standardMinutes: z.number().int().nullable().optional(),
  bayId: idSchema.nullable().optional(),
  appointmentAt: z.string(),
  expectedEndAt: z.string(),
  requestDescription: z.string().nullable().optional(),
  status: bookingStatusSchema,
  cancellationReason: z.string().nullable().optional(),
  createdBy: idSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.string().regex(/^\d+$/),
});

export const checkInVehicleResultSchema = z.object({
  repairOrderId: idSchema,
  orderCode: z.string(),
  bookingId: idSchema.nullable().optional(),
  vehicleId: idSchema,
  status: z.string(),
  receivedAt: z.string(),
});

export const checkTypeSchema = z.enum(['CHECK_IN', 'QC', 'HANDOVER']);
export const checkResultSchema = z.enum(['PASS', 'FAIL', 'CONDITIONAL']);
export const attachmentCategorySchema = z.enum([
  'CHECK_IN',
  'DIAGNOSIS',
  'QC',
  'HANDOVER',
  'WARRANTY',
]);

export const attachmentSchema = z.object({
  id: idSchema,
  repairOrderId: idSchema.nullable().optional(),
  vehicleCheckId: idSchema.nullable().optional(),
  warrantyClaimId: idSchema.nullable().optional(),
  category: attachmentCategorySchema,
  fileName: z.string(),
  fileSize: z.number().optional(),
  contentType: z.string().optional(),
  fileUrl: z.string(),
  createdAt: z.string(),
});

export const vehicleCheckSchema = z.object({
  id: idSchema,
  repairOrderId: idSchema,
  checkType: checkTypeSchema,
  attemptNo: z.number().int(),
  result: checkResultSchema,
  checklistJson: z.string(),
  schemaVersion: z.number().int(),
  odometer: z.number().int().nullable().optional(),
  notes: z.string().nullable().optional(),
  checkedBy: idSchema,
  checkedAt: z.string(),
  attachments: z.array(attachmentSchema).default([]),
});
