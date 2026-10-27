// Non-secret logout intent only; tokens and user data never enter browser storage.
const LOGOUT_PENDING_KEY = 'autogarage-logout-pending';
let pendingInMemory = false;
export function logoutPending(): boolean {
  try {
    return pendingInMemory || localStorage.getItem(LOGOUT_PENDING_KEY) === '1';
  } catch {
    return pendingInMemory;
  }
}
export function markLogoutPending(pending: boolean) {
  pendingInMemory = pending;
  try {
    if (pending) localStorage.setItem(LOGOUT_PENDING_KEY, '1');
    else localStorage.removeItem(LOGOUT_PENDING_KEY);
  } catch {
    // Storage may be blocked. Cookie deletion and in-memory logout still apply.
  }
}
