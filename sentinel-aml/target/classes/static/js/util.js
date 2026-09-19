export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

export function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}

/** Works for both numeric risk scores and priority labels (LOW/MEDIUM/HIGH/CRITICAL). */
export function severityBadgeClass(value) {
  if (typeof value === 'number') {
    if (value >= 90) return 'badge badge-critical';
    if (value >= 70) return 'badge badge-high';
    if (value >= 40) return 'badge badge-medium';
    return 'badge badge-low';
  }
  switch (String(value).toUpperCase()) {
    case 'CRITICAL': return 'badge badge-critical';
    case 'HIGH': return 'badge badge-high';
    case 'MEDIUM': return 'badge badge-medium';
    default: return 'badge badge-low';
  }
}

export function showError(container, err) {
  container.innerHTML = `<p class="text-red-600 text-sm">${escapeHtml(err.message || String(err))}</p>`;
}
