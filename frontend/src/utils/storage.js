const KEY = 'incident-portal-auth';

/** Persists {token, userId, username, role}; falls back silently if storage is unavailable. */
export function saveAuth(auth) {
  try {
    localStorage.setItem(KEY, JSON.stringify(auth));
  } catch {
    /* storage unavailable - session simply won't survive a refresh */
  }
}

export function loadAuth() {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearAuth() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
