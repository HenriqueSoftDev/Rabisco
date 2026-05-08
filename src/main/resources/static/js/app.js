/* Shared JS utilities */

function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const id = 'toast-' + Date.now();
  const icons = { success: 'bi-check-circle-fill', danger: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill', info: 'bi-info-circle-fill' };
  container.insertAdjacentHTML('beforeend', `
    <div id="${id}" class="toast toast-custom align-items-center text-bg-${type} border-0 show mb-2" role="alert">
      <div class="d-flex">
        <div class="toast-body"><i class="bi ${icons[type] || ''} me-2"></i>${message}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>
    </div>`);
  setTimeout(() => document.getElementById(id)?.remove(), 4000);
}

/* Delete book flow (shared across dashboard and detail) */
let pendingDeleteId = null;

function deleteBook(btn) {
  pendingDeleteId = btn.getAttribute('data-id');
  const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
  modal.show();
}

document.addEventListener('DOMContentLoaded', () => {
  const confirmBtn = document.getElementById('confirmDelete');
  if (confirmBtn) {
    confirmBtn.addEventListener('click', async () => {
      if (!pendingDeleteId) return;
      const res = await fetch('/api/books/' + pendingDeleteId, { method: 'DELETE' });
      if (res.ok) {
        window.location.href = '/dashboard';
      } else {
        showToast('Erro ao remover livro', 'danger');
        bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
      }
    });
  }
});
