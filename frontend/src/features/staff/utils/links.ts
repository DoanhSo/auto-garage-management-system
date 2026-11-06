/** Addresses that tie the staff, workshop and payroll screens together. */
export const employeeHref = (id: string | number) => `/staff?tab=profiles&employeeId=${id}`;

export const employeeShiftsHref = (id: string | number) =>
  `/scheduling?tab=shifts&employeeId=${id}`;

export const employeeWorkHref = (id: string | number) => `/payroll?tab=monthly&employeeId=${id}`;

export const bayShiftsHref = (bayCode: string) =>
  `/scheduling?tab=shifts&q=${encodeURIComponent(bayCode)}`;
