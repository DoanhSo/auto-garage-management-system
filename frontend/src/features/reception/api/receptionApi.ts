import { request, queryString } from '../../../shared/api/client';
import {
  bookingSchema,
  checkInVehicleResultSchema,
  vehicleCheckSchema,
  attachmentSchema,
} from '../../../shared/types/schemas';
import type {
  Booking,
  BookingCreate,
  BookingReschedule,
  BookingCancel,
  BookingStatus,
  CheckInVehicle,
  CheckInVehicleResult,
  VehicleCheck,
  VehicleCheckCreate,
  Attachment,
} from '../../../shared/types/contracts';
import { z } from 'zod';

export interface BookingQueryParams {
  customerId?: string;
  vehicleId?: string;
  status?: BookingStatus;
  fromTime?: string;
  toTime?: string;
}

export const receptionApi = {
  // Bookings
  listBookings: async (params?: BookingQueryParams, signal?: AbortSignal): Promise<Booking[]> => {
    const raw = await request<unknown>(
      '/bookings' +
        queryString({
          customerId: params?.customerId,
          vehicleId: params?.vehicleId,
          status: params?.status,
          fromTime: params?.fromTime,
          toTime: params?.toTime,
        }),
      { signal },
    );
    return z.array(bookingSchema).parse(raw);
  },

  getBooking: async (id: string, signal?: AbortSignal): Promise<Booking> => {
    const raw = await request<unknown>(`/bookings/${id}`, { signal });
    return bookingSchema.parse(raw);
  },

  getBookingByCode: async (code: string, signal?: AbortSignal): Promise<Booking> => {
    const raw = await request<unknown>(`/bookings/code/${encodeURIComponent(code)}`, { signal });
    return bookingSchema.parse(raw);
  },

  createBooking: async (values: BookingCreate): Promise<Booking> => {
    const raw = await request<unknown>('/bookings', {
      method: 'POST',
      body: values,
    });
    return bookingSchema.parse(raw);
  },

  confirmBooking: async (id: string, actorUserId?: string): Promise<Booking> => {
    const raw = await request<unknown>(
      `/bookings/${id}/confirm` + queryString({ actorUserId }),
      { method: 'POST' },
    );
    return bookingSchema.parse(raw);
  },

  rescheduleBooking: async (id: string, values: BookingReschedule): Promise<Booking> => {
    const raw = await request<unknown>(`/bookings/${id}/reschedule`, {
      method: 'POST',
      body: values,
    });
    return bookingSchema.parse(raw);
  },

  cancelBooking: async (id: string, values: BookingCancel): Promise<Booking> => {
    const raw = await request<unknown>(`/bookings/${id}/cancel`, {
      method: 'POST',
      body: values,
    });
    return bookingSchema.parse(raw);
  },

  markNoShow: async (id: string, actorUserId?: string): Promise<Booking> => {
    const raw = await request<unknown>(
      `/bookings/${id}/no-show` + queryString({ actorUserId }),
      { method: 'POST' },
    );
    return bookingSchema.parse(raw);
  },

  // Reception check-in (TV2-07)
  checkIn: async (values: CheckInVehicle): Promise<CheckInVehicleResult> => {
    const raw = await request<unknown>('/reception/check-in', {
      method: 'POST',
      body: values,
    });
    return checkInVehicleResultSchema.parse(raw);
  },

  // Vehicle check inspection (TV2-08)
  recordVehicleCheck: async (values: VehicleCheckCreate): Promise<VehicleCheck> => {
    const raw = await request<unknown>('/vehicle-checks', {
      method: 'POST',
      body: values,
    });
    return vehicleCheckSchema.parse(raw);
  },

  getVehicleCheck: async (id: string, signal?: AbortSignal): Promise<VehicleCheck> => {
    const raw = await request<unknown>(`/vehicle-checks/${id}`, { signal });
    return vehicleCheckSchema.parse(raw);
  },

  getVehicleChecksByOrder: async (
    repairOrderId: string,
    signal?: AbortSignal,
  ): Promise<VehicleCheck[]> => {
    const raw = await request<unknown>(`/vehicle-checks/order/${repairOrderId}`, { signal });
    return z.array(vehicleCheckSchema).parse(raw);
  },

  // Attachments (TV2-08 / BR16)
  uploadAttachment: async (formData: FormData): Promise<Attachment> => {
    const raw = await request<unknown>('/attachments/upload', {
      method: 'POST',
      body: formData,
    });
    return attachmentSchema.parse(raw);
  },

  getAttachmentsByOrder: async (
    repairOrderId: string,
    signal?: AbortSignal,
  ): Promise<Attachment[]> => {
    const raw = await request<unknown>(`/attachments/order/${repairOrderId}`, { signal });
    return z.array(attachmentSchema).parse(raw);
  },
};
