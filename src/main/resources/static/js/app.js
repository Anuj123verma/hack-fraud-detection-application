import { setCredentials, isLoggedIn, currentUser } from './api.js';
import { loadAlerts } from './alerts.js';
import { loadCases } from './cases.js';
import { loadCustomers } from './customers.js';

const LOADERS = { alerts: loadAlerts, cases: loadCases, customers: loadCustomers };

function showTab(name) {
  Object.keys(LOADERS).forEach((t) => {
    document.getElementById(`tab-${t}`).classList.toggle('hidden', t !== name);
  });
  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === name);
  });
  LOADERS[name]();
}

document.querySelectorAll('.tab-btn').forEach((btn) => {
  btn.addEventListener('click', () => showTab(btn.dataset.tab));
});

document.getElementById('login-btn').addEventListener('click', () => {
  const user = document.getElementById('login-user').value.trim();
  const pass = document.getElementById('login-pass').value;
  if (!user || !pass) return;
  setCredentials(user, pass);
  document.getElementById('login-status').textContent = `Signed in as ${user}`;
  document.getElementById('login-pass').value = '';
  showTab('alerts');
});

// Enter key in either login field submits.
['login-user', 'login-pass'].forEach((id) => {
  document.getElementById(id).addEventListener('keydown', (e) => {
    if (e.key === 'Enter') document.getElementById('login-btn').click();
  });
});

if (isLoggedIn()) {
  document.getElementById('login-status').textContent = `Signed in as ${currentUser()}`;
}

// Load the default tab immediately - endpoints will 401 gracefully in the
// panel itself (rendered as a red error message) until the user signs in.
showTab('alerts');
