import { api } from './api.js';
import { escapeHtml, formatDate, showError } from './util.js';

export async function loadCustomers() {
  const listEl = document.getElementById('customers-list');
  listEl.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const customers = await api.getCustomers();
    if (customers.length === 0) {
      listEl.innerHTML = '<p class="text-gray-500 text-sm">No customers loaded.</p>';
      return;
    }
    const rows = customers.map((c) => `
      <tr data-id="${c.id}">
        <td>${escapeHtml(c.customerRef)}</td>
        <td>${escapeHtml(c.maskedName)}</td>
        <td>${escapeHtml(c.customerType)}</td>
        <td>${escapeHtml(c.baseRiskRating)}</td>
      </tr>`).join('');
    listEl.innerHTML = `<table class="data-table">
      <thead><tr><th>Ref</th><th>Name</th><th>Type</th><th>Base Risk</th></tr></thead>
      <tbody>${rows}</tbody></table>`;
    listEl.querySelectorAll('tbody tr').forEach((tr) => tr.addEventListener('click', () => loadTimeline(tr.dataset.id)));
  } catch (err) {
    showError(listEl, err);
  }
}

async function loadTimeline(customerId) {
  const el = document.getElementById('customer-timeline');
  el.innerHTML = '<p class="text-gray-500 text-sm">Loading...</p>';
  try {
    const txns = await api.getCustomerTimeline(customerId);
    if (txns.length === 0) {
      el.innerHTML = '<p class="text-gray-500 text-sm">No transactions for this customer.</p>';
      return;
    }
    const rows = txns.map((t) => `
      <tr>
        <td>${formatDate(t.txnTimestamp)}</td>
        <td>${escapeHtml(t.accountNumber)}</td>
        <td>${escapeHtml(t.direction)}</td>
        <td>${t.amount} ${escapeHtml(t.currency)}</td>
        <td>${escapeHtml(t.counterpartyName || '-')}</td>
        <td>${escapeHtml(t.counterpartyJurisdiction || '-')}</td>
        <td>${escapeHtml(t.channel)}</td>
      </tr>`).join('');
    el.innerHTML = `<table class="data-table">
      <thead><tr><th>Time</th><th>Account</th><th>Dir</th><th>Amount</th><th>Counterparty</th><th>Jurisdiction</th><th>Channel</th></tr></thead>
      <tbody>${rows}</tbody></table>`;
  } catch (err) {
    showError(el, err);
  }
}
