import { api } from './api.js';
import { escapeHtml, formatDate, severityBadgeClass, showError } from './util.js';

let selectedAlertId = null;

export async function loadAlerts() {
  const listEl = document.getElementById('alerts-list');
  listEl.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const alerts = await api.getAlerts();
    if (alerts.length === 0) {
      listEl.innerHTML = '<p class="text-gray-500 text-sm">No alerts - clean queue.</p>';
      return;
    }
    const rows = alerts.map((a) => `
      <tr data-id="${a.id}">
        <td>${escapeHtml(a.customerRef)}</td>
        <td>${escapeHtml(a.maskedCustomerName)}</td>
        <td>${escapeHtml(a.accountNumber || '-')}</td>
        <td><span class="${severityBadgeClass(a.riskScore)}">${a.riskScore}</span></td>
        <td>${escapeHtml(a.status)}</td>
        <td>${escapeHtml(a.ruleCodes)}</td>
        <td>${formatDate(a.createdAt)}</td>
      </tr>`).join('');
    listEl.innerHTML = `<table class="data-table">
      <thead><tr><th>Customer</th><th>Name</th><th>Account</th><th>Risk</th><th>Status</th><th>Rule(s)</th><th>Created</th></tr></thead>
      <tbody>${rows}</tbody></table>`;
    listEl.querySelectorAll('tbody tr').forEach((tr) => tr.addEventListener('click', () => selectAlert(tr.dataset.id)));
  } catch (err) {
    showError(listEl, err);
  }
}

async function selectAlert(id) {
  selectedAlertId = id;
  const detailEl = document.getElementById('alert-detail');
  detailEl.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const a = await api.getAlert(id);
    detailEl.innerHTML = `
      <div class="space-y-2 text-sm">
        <div><span class="font-semibold">Customer:</span> ${escapeHtml(a.customerRef)} - ${escapeHtml(a.customerName)} (${escapeHtml(a.idNumber)})</div>
        <div><span class="font-semibold">Account:</span> ${escapeHtml(a.accountNumber || '-')}</div>
        <div><span class="font-semibold">Risk score:</span> <span class="${severityBadgeClass(a.riskScore)}">${a.riskScore}</span></div>
        <div><span class="font-semibold">Status:</span> ${escapeHtml(a.status)}</div>
        <div><span class="font-semibold">Rule(s) triggered:</span> ${escapeHtml(a.ruleCodes)}</div>
        <div><span class="font-semibold">Evidence transaction id(s):</span>
          <div class="text-xs text-gray-600 break-all">${escapeHtml(a.evidenceTxnIds)}</div></div>
        <div><span class="font-semibold">Why it fired:</span>
          <div class="text-gray-700">${escapeHtml(a.explanation)}</div></div>
        ${a.dispositionReason ? `<div><span class="font-semibold">Last disposition:</span> ${escapeHtml(a.dispositionReason)}
          <div class="text-xs text-gray-500">by ${escapeHtml(a.disposedBy)} at ${formatDate(a.disposedAt)}</div></div>` : ''}
        <hr class="my-2"/>
        <div class="font-semibold">Disposition action</div>
        <select id="disposition-status" class="border rounded px-2 py-1 w-full">
          <option value="IN_REVIEW">IN_REVIEW</option>
          <option value="ESCALATED">ESCALATED</option>
          <option value="CLEARED">CLEARED</option>
          <option value="SAR_FILED">SAR_FILED</option>
          <option value="CLOSED">CLOSED</option>
        </select>
        <textarea id="disposition-reason" placeholder="Reason (required for CLEARED/CLOSED)"
          class="border rounded px-2 py-1 w-full mt-1" rows="2"></textarea>
        <div class="flex gap-2 mt-2">
          <button id="disposition-submit" class="bg-indigo-600 text-white px-3 py-1 rounded hover:bg-indigo-500">Submit</button>
          <button id="open-case-btn" class="bg-slate-600 text-white px-3 py-1 rounded hover:bg-slate-500">Open Case from this Alert</button>
        </div>
        <div id="alert-action-msg" class="text-sm mt-1"></div>
      </div>`;
    document.getElementById('disposition-submit').addEventListener('click', submitDisposition);
    document.getElementById('open-case-btn').addEventListener('click', openCaseFromAlert);
  } catch (err) {
    showError(detailEl, err);
  }
}

function setActionMessage(text, isError) {
  const msgEl = document.getElementById('alert-action-msg');
  if (!msgEl) return;
  msgEl.textContent = text;
  msgEl.className = `text-sm mt-1 ${isError ? 'text-red-600' : 'text-emerald-600'}`;
}

async function submitDisposition() {
  const status = document.getElementById('disposition-status').value;
  const reason = document.getElementById('disposition-reason').value;
  try {
    await api.dispositionAlert(selectedAlertId, status, reason);
    setActionMessage('Alert updated.', false);
    await loadAlerts();
    await selectAlert(selectedAlertId);
  } catch (err) {
    setActionMessage(err.message, true);
  }
}

async function openCaseFromAlert() {
  try {
    const created = await api.openCase([selectedAlertId]);
    setActionMessage(`Case ${created.caseNumber} opened.`, false);
  } catch (err) {
    setActionMessage(err.message, true);
  }
}
