/* api.js — Fetch-based REST client with Bearer token */

const BASE = '/api';

function getToken()        { return localStorage.getItem('ep_token'); }
function setToken(t)       { localStorage.setItem('ep_token', t); }
function clearToken()      { localStorage.removeItem('ep_token'); localStorage.removeItem('ep_user'); }
function getUser()         { return JSON.parse(localStorage.getItem('ep_user') || 'null'); }
function setUser(u)        { localStorage.setItem('ep_user', JSON.stringify(u)); }

function authHeaders() {
  return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() };
}

async function apiFetch(path, opts = {}) {
  const res = await fetch(BASE + path, {
    ...opts,
    headers: { ...authHeaders(), ...(opts.headers || {}) }
  });
  if (res.status === 401) { clearToken(); window.location.href = '/'; return; }
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data?.error || 'Request failed');
  return data;
}

const api = {
  auth: {
    login:  (email, password) => apiFetch('/auth/login',  { method: 'POST', body: JSON.stringify({ email, password }) }),
    logout: ()                 => apiFetch('/auth/logout', { method: 'POST' }),
    me:     ()                 => apiFetch('/auth/me'),
  },
  exams: {
    list:      ()   => apiFetch('/exams'),
    get:       (id) => apiFetch(`/exams/${id}`),
    questions: (id) => apiFetch(`/exams/${id}/questions`),
  },
  attempts: {
    start:  (examId)           => apiFetch('/attempts/start',          { method: 'POST', body: JSON.stringify({ examId }) }),
    submit: (attemptId, answers, force) => apiFetch(`/attempts/${attemptId}/submit`, { method: 'POST', body: JSON.stringify({ answers, force: !!force }) }),
    result: (attemptId)        => apiFetch(`/attempts/${attemptId}/result`),
  },
  admin: {
    stats:          ()          => apiFetch('/admin/stats'),
    exams:          ()          => apiFetch('/admin/exams'),
    createExam:     (data)      => apiFetch('/admin/exams',      { method: 'POST',   body: JSON.stringify(data) }),
    updateExam:     (id, data)  => apiFetch(`/admin/exams/${id}`,{ method: 'PUT',    body: JSON.stringify(data) }),
    deleteExam:     (id)        => apiFetch(`/admin/exams/${id}`,{ method: 'DELETE' }),
    students:       ()          => apiFetch('/admin/students'),
    toggleStudent:  (id)        => apiFetch(`/admin/students/${id}/toggle`, { method: 'PATCH' }),
    logs:           ()          => apiFetch('/admin/logs'),
  }
};

// Auth guard — call on every protected page
function requireAuth(role) {
  const user = getUser();
  if (!user || !getToken()) { window.location.href = '/'; return null; }
  if (role && user.role !== role) {
    window.location.href = user.role === 'ADMIN' ? '/admin' : '/student';
    return null;
  }
  return user;
}

// Logout helper
async function logout() {
  try { await api.auth.logout(); } catch {}
  clearToken();
  window.location.href = '/';
}

// Format date
function fmtDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

// Format duration
function fmtDuration(seconds) {
  const m = Math.floor(seconds / 60), s = seconds % 60;
  return `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
}
