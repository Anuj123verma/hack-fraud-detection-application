// Thin fetch wrapper + credential storage. Basic Auth creds live only in
// sessionStorage (cleared when the tab closes) - never persisted to disk.

const AUTH_KEY = 'sentinel_creds';
const USER_KEY = 'sentinel_user';

export function setCredentials(user, pass) {
  sessionStorage.setItem(AUTH_KEY, btoa(`${user}:${pass}`));
  sessionStorage.setItem(USER_KEY, user);
}

export function isLoggedIn() {
  return !!sessionStorage.getItem(AUTH_KEY);
}

export function currentUser() {
  return sessionStorage.getItem(USER_KEY) || '';
}

export function logout() {
  sessionStorage.removeItem(AUTH_KEY);
  sessionStorage.removeItem(USER_KEY);
}

function authHeader() {
  const creds = sessionStorage.getItem(AUTH_KEY);
  return creds ? { Authorization: `Basic ${creds}` } : {};
}

async function request(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
      ...(options.headers || {}),
    },
  });

  if (res.status === 204) return null;

  const contentType = res.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await res.json() : await res.text();

  if (!res.ok) {
    const message = (body && body.message) ? body.message : `Request failed with status ${res.status}`;
    throw new Error(message);
  }
  return body;
}

export const api = {
  getAlerts: () => request('/api/v1/alerts'),
  getAlert: (id) => request(`/api/v1/alerts/${id}`),
  dispositionAlert: (id, status, reason) =>
    request(`/api/v1/alerts/${id}/disposition`, { method: 'PATCH', body: JSON.stringify({ status, reason }) }),

  getCases: () => request('/api/v1/cases'),
  getCase: (id) => request(`/api/v1/cases/${id}`),
  openCase: (alertIds) => request('/api/v1/cases', { method: 'POST', body: JSON.stringify({ alertIds }) }),
  linkAlertToCase: (caseId, alertId) => request(`/api/v1/cases/${caseId}/alerts/${alertId}`, { method: 'POST' }),
  updateCaseStatus: (caseId, status, reason) =>
    request(`/api/v1/cases/${caseId}/status`, { method: 'PATCH', body: JSON.stringify({ status, reason }) }),

  getCustomers: () => request('/api/v1/customers'),
  getCustomerTimeline: (id) => request(`/api/v1/customers/${id}/transactions`),
};
