import { z } from 'zod';
import { roles, customerTypes } from './contracts';
export const idSchema = z.string().regex(/^[1-9]\d*$/);
export const userSchema = z.object({
  id: idSchema,
  username: z.string(),
  email: z.string().nullable(),
  fullName: z.string(),
  role: z.enum(roles),
  active: z.boolean(),
  version: z.string().regex(/^\d+$/),
  createdAt: z.string(),
});
export const loginResultSchema = z.object({
  accessToken: z.string().min(1),
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
  channel: z.string(),
  subject: z.string().nullable(),
  payloadJson: z.string(),
  status: z.string(),
  readAt: z.string().nullable(),
  createdAt: z.string(),
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
  phone: z.string(),
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
  customerPhone: z.string(),
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
