import { api } from './api.js';
import { escapeHtml, formatDate, severityBadgeClass, showError } from './util.js';

let selectedCaseId = null;

export async function loadCases() {
  const listEl = document.getElementById('cases-list');
  listEl.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const cases = await api.getCases();
    if (cases.length === 0) {
      listEl.innerHTML = '<p class="text-gray-500 text-sm">No cases yet - open one from an alert.</p>';
      return;
    }
    const rows = cases.map((c) => `
      <tr data-id="${c.id}">
        <td>${escapeHtml(c.caseNumber)}</td>
        <td>${escapeHtml(c.customerRef)}</td>
        <td>${escapeHtml(c.maskedCustomerName)}</td>
        <td><span class="${severityBadgeClass(c.priority)}">${escapeHtml(c.priority)}</span></td>
        <td>${escapeHtml(c.status)}</td>
        <td>${c.linkedAlertCount}</td>
        <td>${formatDate(c.createdAt)}</td>
      </tr>`).join('');
    listEl.innerHTML = `<table class="data-table">
      <thead><tr><th>Case #</th><th>Customer</th><th>Name</th><th>Priority</th><th>Status</th><th># Alerts</th><th>Created</th></tr></thead>
      <tbody>${rows}</tbody></table>`;
    listEl.querySelectorAll('tbody tr').forEach((tr) => tr.addEventListener('click', () => selectCase(tr.dataset.id)));
  } catch (err) {
    showError(listEl, err);
  }
}

async function selectCase(id) {
  selectedCaseId = id;
  const detailEl = document.getElementById('case-detail');
  detailEl.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const c = await api.getCase(id);
    const alertIdsHtml = c.linkedAlertIds.length
      ? c.linkedAlertIds.map((id2) => `<span class="text-xs text-gray-600 break-all block">${escapeHtml(id2)}</span>`).join('')
      : '<span class="text-gray-500">(none)</span>';
    detailEl.innerHTML = `
      <div class="space-y-2 text-sm">
        <div><span class="font-semibold">Case #:</span> ${escapeHtml(c.caseNumber)}</div>
        <div><span class="font-semibold">Customer:</span> ${escapeHtml(c.customerRef)} - ${escapeHtml(c.customerName)}</div>
        <div><span class="font-semibold">Priority:</span> <span class="${severityBadgeClass(c.priority)}">${escapeHtml(c.priority)}</span></div>
        <div><span class="font-semibold">Status:</span> ${escapeHtml(c.status)}</div>
        <div><span class="font-semibold">Linked alerts:</span> ${alertIdsHtml}</div>
        <div><span class="font-semibold">Created:</span> ${formatDate(c.createdAt)}</div>
        ${c.closedAt ? `<div><span class="font-semibold">Closed:</span> ${formatDate(c.closedAt)}</div>` : ''}
        <hr class="my-2"/>
        <div class="font-semibold">Link another alert</div>
        <div class="flex gap-2">
          <input id="link-alert-id" placeholder="alert id" class="border rounded px-2 py-1 flex-1"/>
          <button id="link-alert-btn" class="bg-slate-600 text-white px-3 py-1 rounded hover:bg-slate-500">Link</button>
        </div>
        <div class="font-semibold mt-2">Update status</div>
        <select id="case-status" class="border rounded px-2 py-1 w-full">
          <option value="OPEN">OPEN</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="ESCALATED">ESCALATED</option>
          <option value="SAR_FILED">SAR_FILED</option>
          <option value="CLOSED">CLOSED</option>
        </select>
        <textarea id="case-reason" placeholder="Reason (required for CLOSED/SAR_FILED)"
          class="border rounded px-2 py-1 w-full mt-1" rows="2"></textarea>
        <button id="case-status-submit" class="bg-indigo-600 text-white px-3 py-1 rounded hover:bg-indigo-500 mt-1">Submit</button>
        <div id="case-action-msg" class="text-sm mt-1"></div>
      </div>`;
    document.getElementById('link-alert-btn').addEventListener('click', linkAlert);
    document.getElementById('case-status-submit').addEventListener('click', submitStatus);
  } catch (err) {
    showError(detailEl, err);
  }
}

function setActionMessage(text, isError) {
  const msgEl = document.getElementById('case-action-msg');
  if (!msgEl) return;
  msgEl.textContent = text;
  msgEl.className = `text-sm mt-1 ${isError ? 'text-red-600' : 'text-emerald-600'}`;
}

async function linkAlert() {
  const alertId = document.getElementById('link-alert-id').value.trim();
  if (!alertId) return;
  try {
    await api.linkAlertToCase(selectedCaseId, alertId);
    setActionMessage('Alert linked.', false);
    await loadCases();
    await selectCase(selectedCaseId);
  } catch (err) {
    setActionMessage(err.message, true);
  }
}

async function submitStatus() {
  const status = document.getElementById('case-status').value;
  const reason = document.getElementById('case-reason').value;
  try {
    await api.updateCaseStatus(selectedCaseId, status, reason);
    setActionMessage('Case updated.', false);
    await loadCases();
    await selectCase(selectedCaseId);
  } catch (err) {
    setActionMessage(err.message, true);
  }
}
