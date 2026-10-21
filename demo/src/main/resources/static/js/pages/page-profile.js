(function () {
    'use strict';

    const ICON_CHECK = `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5"/></svg>`;
    const ICON_CROSS = `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/></svg>`;

    const ICON_CLEAR = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;

    function setupProfileFieldEdit(groupId, fieldName) {
        const group = document.getElementById(groupId);
        if (!group) return;
        const editBtn = group.querySelector('.edit-trigger');
        if (!editBtn) return;

        const valueContainer = group.querySelector('.contact-value');
        const textSpan = group.querySelector('.editable-text');
        const emptySpan = group.querySelector('.detail-value-empty');
        const errorSpan = group.querySelector('.field-error');
        const oldHtml = valueContainer.innerHTML;

        editBtn.addEventListener('click', () => {
            if (group.querySelector('.inline-edit-wrapper')) return;

            let currentText = '';
            if (textSpan) currentText = textSpan.textContent.trim();
            if (currentText === 'Телефон не указан') currentText = '';

            valueContainer.innerHTML = `
                <div class="inline-edit-wrapper">
                    <div class="inline-edit-controls">
                        <div class="inline-edit-input-wrap">
                            <input type="text" class="inline-edit-input" value="${currentText}">
                            <button type="button" class="inline-edit-clear">${ICON_CLEAR}</button>
                        </div>
                        <div class="inline-edit-actions">
                            <button type="button" class="inline-edit-done-btn">${ICON_CHECK}</button>
                            <button type="button" class="inline-edit-cancel-btn">${ICON_CROSS}</button>
                        </div>
                    </div>
                    <div class="inline-edit-error"></div>
                </div>
            `;

            const input = valueContainer.querySelector('.inline-edit-input');
            const btnDone = valueContainer.querySelector('.inline-edit-done-btn');
            const btnCancel = valueContainer.querySelector('.inline-edit-cancel-btn');
            const btnClear = valueContainer.querySelector('.inline-edit-clear');
            const errDiv = valueContainer.querySelector('.inline-edit-error');

            input.focus();
            input.setSelectionRange(input.value.length, input.value.length);

            btnClear.addEventListener('click', () => {
                input.value = '';
                input.focus();
            });

            const cancelEdit = () => {
                valueContainer.innerHTML = oldHtml;
                if (errorSpan) errorSpan.textContent = '';
                setupProfileFieldEdit(groupId, fieldName);
            };

            btnCancel.addEventListener('click', cancelEdit);

            const saveEdit = () => {
                const val = input.value.trim();

                if (!val && fieldName !== 'PHONE') {
                    errDiv.textContent = 'Поле не может быть пустым';
                    errDiv.classList.add('show');
                    input.classList.add('has-error');
                    return;
                }

                errDiv.classList.remove('show');
                input.classList.remove('has-error');
                btnDone.disabled = true;

                fetch('/settings/change/field', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        [document.querySelector('meta[name="_csrf_header"]').content]: document.querySelector('meta[name="_csrf"]').content
                    },
                    body: JSON.stringify({ identifier: fieldName, field: val })
                })
                .then(async (r) => {
                    const data = await r.json();
                    if (!r.ok) throw new Error(data.message || 'Ошибка сохранения');
                    return data;
                })
                .then((data) => {
                    if (fieldName === 'USERNAME' && data.data.sessionUser) {
                        if (window.updateSidebarAndHeader) {
                            window.updateSidebarAndHeader({ username: data.data.sessionUser.username });
                        }
                    }
                    window.location.reload();
                })
                .catch((err) => {
                    errDiv.textContent = err.message;
                    errDiv.classList.add('show');
                    input.classList.add('has-error');
                    btnDone.disabled = false;
                });
            };

            btnDone.addEventListener('click', saveEdit);

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    saveEdit();
                }
                if (e.key === 'Escape') {
                    e.preventDefault();
                    cancelEdit();
                }
            });
        });
    }

    function init() {
        setupProfileFieldEdit('usernameGroup', 'USERNAME');
        setupProfileFieldEdit('emailGroup', 'EMAIL');
        setupProfileFieldEdit('phoneGroup', 'PHONE');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();