export type BayType =
  | 'TWO_POST'
  | 'FOUR_POST'
  | 'SCISSOR'
  | 'DIAGNOSTIC'
  | 'INSPECTION'
  | 'PAINT'
  | 'WASH'
  | 'GENERAL';

export interface ServiceBay {
  id: number;
  bayCode: string;
  bayName: string;
  bayType: string;
  description?: string | null;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export type ReservationStatus = 'HELD' | 'IN_USE' | 'RELEASED' | 'EXPIRED' | 'CANCELLED';
export type ReservationType = 'BOOKING' | 'REPAIR' | 'MAINTENANCE';

export interface BayReservation {
  id: number;
  bayId: number;
  bookingId?: number | null;
  repairOrderId?: number | null;
  reservationType: ReservationType;
  status: ReservationStatus;
  startsAt: string;
  endsAt: string;
  expiresAt?: string | null;
  releasedAt?: string | null;
  notes?: string | null;
  createdBy?: number | null;
  createdAt?: string;
  version?: number;
}
