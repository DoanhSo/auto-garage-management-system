import { parse, stringify, isLosslessNumber, LosslessNumber } from 'lossless-json';
const exactFields = new Set([
  'id',
  'userId',
  'customerId',
  'vehicleId',
  'serviceId',
  'bayId',
  'bookingId',
  'repairOrderId',
  'vehicleCheckId',
  'warrantyClaimId',
  'orderCustomerId',
  'currentCustomerId',
  'createdBy',
  'checkedBy',
  'uploadedBy',
  'unitPrice',
  'actorUserId',
  'entityId',
  'version',
  'totalElements',
  'creditLimit',
  'revenue',
  'cashIn',
  'cashOut',
  'customerDebt',
  'supplierDebt',
  'stockValue',
  'laborHours',
  'totalAmount',
  'paidAmount',
  'balanceAmount',
]);
export function decodeJson(text: string): unknown {
  return parse(text, (key, value: unknown) => {
    if (!isLosslessNumber(value)) return value;
    if (exactFields.has(key)) return value.toString();
    const number = Number(value.toString());
    if (!Number.isFinite(number) || (Number.isInteger(number) && !Number.isSafeInteger(number)))
      throw new Error('Unsafe numeric response');
    return number;
  });
}
export function encodeJson(value: unknown): string {
  return stringify(value) ?? 'null';
}
export function numericVersion(version: string): LosslessNumber {
  if (!/^(0|[1-9]\d*)$/.test(version)) throw new Error('Invalid version');
  return new LosslessNumber(version);
}
