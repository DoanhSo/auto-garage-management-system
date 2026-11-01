import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { receptionApi, type BookingQueryParams } from '../api/receptionApi';
import { customerKeys } from '../../customer/hooks/useCustomers';
import { vehicleKeys } from '../../vehicle/hooks/useVehicles';
import type {
  BookingCreate,
  BookingReschedule,
  BookingCancel,
  CheckInVehicle,
  VehicleCheckCreate,
} from '../../../shared/types/contracts';

export const receptionKeys = {
  all: ['reception'] as const,
  bookings: () => [...receptionKeys.all, 'bookings'] as const,
  bookingLists: () => [...receptionKeys.bookings(), 'list'] as const,
  bookingList: (params?: BookingQueryParams) => [...receptionKeys.bookingLists(), params] as const,
  bookingDetails: () => [...receptionKeys.bookings(), 'detail'] as const,
  bookingDetail: (id: string) => [...receptionKeys.bookingDetails(), id] as const,
  vehicleChecks: (orderId: string) => [...receptionKeys.all, 'checks', orderId] as const,
  attachments: (orderId: string) => [...receptionKeys.all, 'attachments', orderId] as const,
};

export function useBookingsQuery(params?: BookingQueryParams) {
  return useQuery({
    queryKey: receptionKeys.bookingList(params),
    queryFn: ({ signal }) => receptionApi.listBookings(params, signal),
  });
}

export function useBookingQuery(id: string | null) {
  return useQuery({
    queryKey: id ? receptionKeys.bookingDetail(id) : ['bookings', 'empty'],
    queryFn: ({ signal }) => (id ? receptionApi.getBooking(id, signal) : Promise.reject('No ID')),
    enabled: Boolean(id),
  });
}

export function useCreateBookingMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: BookingCreate) => receptionApi.createBooking(values),
    onSuccess: (_booking, values) => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
      if (values.newCustomer) queryClient.invalidateQueries({ queryKey: customerKeys.all });
      if (values.newVehicle) queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

export function useConfirmBookingMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, actorUserId }: { id: string; actorUserId?: string }) =>
      receptionApi.confirmBooking(id, actorUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
    },
  });
}

export function useRescheduleBookingMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: BookingReschedule }) =>
      receptionApi.rescheduleBooking(id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
    },
  });
}

export function useCancelBookingMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: BookingCancel }) =>
      receptionApi.cancelBooking(id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
    },
  });
}

export function useMarkNoShowMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, actorUserId }: { id: string; actorUserId?: string }) =>
      receptionApi.markNoShow(id, actorUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
    },
  });
}

export function useCheckInVehicleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: CheckInVehicle) => receptionApi.checkIn(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
    },
  });
}

export function useRecordVehicleCheckMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: VehicleCheckCreate) => receptionApi.recordVehicleCheck(values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: receptionKeys.vehicleChecks(variables.repairOrderId),
      });
    },
  });
}

export function useVehicleChecksQuery(repairOrderId: string | null) {
  return useQuery({
    queryKey: repairOrderId ? receptionKeys.vehicleChecks(repairOrderId) : ['checks', 'empty'],
    queryFn: ({ signal }) =>
      repairOrderId
        ? receptionApi.getVehicleChecksByOrder(repairOrderId, signal)
        : Promise.reject('No ID'),
    enabled: Boolean(repairOrderId),
  });
}

export function useUploadAttachmentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => receptionApi.uploadAttachment(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.all });
    },
  });
}

export function useUploadOrderAttachmentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: Parameters<typeof receptionApi.uploadOrderAttachment>[0]) =>
      receptionApi.uploadOrderAttachment(values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: receptionKeys.attachments(variables.repairOrderId),
      });
      queryClient.invalidateQueries({
        queryKey: receptionKeys.vehicleChecks(variables.repairOrderId),
      });
    },
  });
}

export function useOrderAttachmentsQuery(repairOrderId: string | null) {
  return useQuery({
    queryKey: repairOrderId ? receptionKeys.attachments(repairOrderId) : ['attachments', 'empty'],
    queryFn: ({ signal }) =>
      repairOrderId
        ? receptionApi.getAttachmentsByOrder(repairOrderId, signal)
        : Promise.reject('No ID'),
    enabled: Boolean(repairOrderId),
  });
}

/** Object URL của ảnh đính kèm (tải kèm token); revoke khi đổi ảnh hoặc unmount. */
export function useAttachmentBlobUrl(attachmentId: string | null) {
  const [state, setState] = useState<{ id: string; url: string | null; failed: boolean } | null>(
    null,
  );
  useEffect(() => {
    if (!attachmentId) return;
    const controller = new AbortController();
    let created: string | null = null;
    receptionApi
      .fetchAttachmentBlobUrl(attachmentId, controller.signal)
      .then((url) => {
        created = url;
        setState({ id: attachmentId, url, failed: false });
      })
      .catch(() => {
        if (!controller.signal.aborted) setState({ id: attachmentId, url: null, failed: true });
      });
    return () => {
      controller.abort();
      if (created) URL.revokeObjectURL(created);
    };
  }, [attachmentId]);
  const current = state?.id === attachmentId ? state : null;
  return { url: current?.url ?? null, failed: current?.failed ?? false };
}
