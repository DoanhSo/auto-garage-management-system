import { z } from 'zod';
import { roles } from './contracts';
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
