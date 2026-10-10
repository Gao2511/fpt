(() => {
  'use strict';
  const form = document.getElementById('cms-form');
  if (!form) return;
  const hint = document.getElementById('cms-save-hint');
  const save = form.querySelector('button[type="submit"]');
  const demo = form.dataset.demo === 'true';
  let pending = 0;
  const updateSave = () => { save.disabled = demo || pending > 0; hint.textContent = pending ? 'Đang tải ảnh… Vui lòng đợi trước khi lưu.' : 'Nội dung sẽ được cập nhật trên trang khách hàng.'; };
  document.querySelectorAll('.cms-sidebar a').forEach(link => link.addEventListener('click', () => {
    const section = document.getElementById(link.hash.slice(1));
    if (section) section.open = true;
    document.querySelectorAll('.cms-sidebar a').forEach(item => item.removeAttribute('aria-current'));
    link.setAttribute('aria-current', 'location');
  }));
  // Reveal collapsed invalid controls before the browser moves keyboard focus.
  form.addEventListener('invalid', event => { const section = event.target.closest('details'); if (section) section.open = true; }, true);
  form.addEventListener('submit', event => { if (pending || demo) { event.preventDefault(); updateSave(); } });
  form.querySelectorAll('[data-image-field]').forEach(card => {
    const input = card.querySelector('[data-image-upload]');
    const value = card.querySelector('[data-image-value]');
    const image = card.querySelector('img');
    const preview = card.querySelector('.cms-image-preview');
    const status = card.querySelector('.cms-upload-status');
    const clear = card.querySelector('[data-image-clear]');
    let objectUrl;
    const revoke = () => { if (objectUrl) { URL.revokeObjectURL(objectUrl); objectUrl = null; } };
    const message = (text, error = false) => { status.textContent = text; status.toggleAttribute('data-error', error); };
    image.addEventListener('error', () => { preview.setAttribute('data-empty', 'true'); card.querySelector('.cms-image-empty').textContent = 'Ảnh hiện tại không tải được'; });
    clear.addEventListener('click', () => {
      if (demo || input.disabled) return;
      revoke(); value.value = ''; image.removeAttribute('src'); preview.setAttribute('data-empty', 'true');
      card.querySelector('.cms-image-empty').textContent = 'Chưa có ảnh'; input.value = '';
      message('Ảnh sẽ được bỏ khi lưu và xuất bản.');
    });
    input.addEventListener('change', async () => {
      const file = input.files[0];
      if (!file || demo) return;
      if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 2097152 || !file.size) {
        message('Chọn ảnh PNG/JPG có dung lượng tối đa 2 MB.', true); input.value = ''; return;
      }
      pending++; input.disabled = clear.disabled = true; updateSave(); message('Đang tải ảnh…');
      const controller = new AbortController(); const timer = setTimeout(() => controller.abort(), 30000);
      try {
        const body = new FormData(); body.append('image', file); body.append('csrf_token', form.elements.csrf_token.value);
        const response = await fetch(form.dataset.context + '/admin/media', { method: 'POST', headers: { Accept: 'application/json' }, credentials: 'same-origin', body, signal: controller.signal });
        if (!response.ok || !(response.headers.get('content-type') || '').includes('application/json')) throw new Error('upload');
        const result = await response.json();
        if (typeof result.url !== 'string' || !/^\/media\?id=[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/.test(result.url)) throw new Error('format');
        revoke(); objectUrl = URL.createObjectURL(file); image.src = objectUrl; preview.removeAttribute('data-empty'); value.value = result.url;
        message('Đã tải ảnh. Bấm “Lưu và xuất bản” để sử dụng.');
      } catch (error) {
        message('Tải ảnh chưa thành công. Kiểm tra kết nối, ảnh hoặc đăng nhập lại rồi thử lại. Ảnh cũ được giữ nguyên.', true);
      } finally {
        clearTimeout(timer); input.value = ''; pending--; input.disabled = clear.disabled = demo; updateSave();
      }
    });
    window.addEventListener('pagehide', revoke);
  });
})();
