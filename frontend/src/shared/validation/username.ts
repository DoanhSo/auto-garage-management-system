import { z } from 'zod';

// Public username contract, shared with the account editor and registration.
export const USERNAME_MAX_LENGTH = 100;
export const USERNAME_HINT = 'Dùng 3–100 chữ/số ASCII, dấu chấm, gạch dưới hoặc gạch ngang.';
export const usernameSchema = z.string().regex(/^[A-Za-z0-9._-]{3,100}$/, USERNAME_HINT);
