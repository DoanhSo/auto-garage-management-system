import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { receptionApi, type BookingQueryParams } from '../api/receptionApi';
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receptionKeys.bookings() });
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
