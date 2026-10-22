(function () {
    'use strict';

    const ICON_CHECK = `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5"/></svg>`;
    const ICON_CROSS = `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/></svg>`;
    const ICON_CLEAR = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;

    function readRawPhone(group) {
        if (!group) return '';
        var source = group.querySelector('[data-raw-phone]');
        if (source) {
            var raw = source.getAttribute('data-raw-phone');
            if (raw != null) return String(raw).trim();
        }
        return '';
    }

    function setupProfileFieldEdit(groupId, fieldName) {
        const group = document.getElementById(groupId);
        if (!group) return;
        const editBtn = group.querySelector('.edit-trigger');
        if (!editBtn) return;

        const valueContainer = group.querySelector('.contact-value');
        const oldHtml = valueContainer.innerHTML;

        const activateEdit = () => {
            if (group.querySelector('.inline-edit-wrapper')) return;

            const currentTextSpan = group.querySelector('.editable-text');
            const currentErrorSpan = group.querySelector('.field-error');

            let currentText = '';
            if (fieldName === 'PHONE') {
                currentText = readRawPhone(group);
            } else if (currentTextSpan) {
                currentText = currentTextSpan.textContent.trim();
            }

            if (currentText === 'Телефон не указан' || currentText === 'Добавить номер') {
                currentText = '';
            }

            const originalValue = currentText;
            const isPhone = fieldName === 'PHONE';

            const valueForInput = (isPhone && currentText)
                ? (typeof window.PhoneMask !== 'undefined'
                    ? window.PhoneMask.format(currentText)
                    : currentText)
                : currentText;

            valueContainer.innerHTML = `
                <div class="inline-edit-wrapper">
                    <div class="inline-edit-controls">
                        <div class="inline-edit-input-wrap">
                            <input type="text"
                                   class="inline-edit-input"
                                   placeholder="Введите значение..."
                                   value="${valueForInput}"
                                   ${isPhone ? 'data-phone-mask' : ''}>
                            <button type="button" class="inline-edit-clear">${ICON_CLEAR}</button>
                        </div>
                        <div class="inline-edit-actions">
                            <button type="button" class="inline-edit-done-btn">${ICON_CHECK}</button>
                            <button type="button" class="inline-edit-cancel-btn">${ICON_CROSS}</button>
                        </div>
                    </div>
                    <div class="inline-error-text" style="display: none;"></div>
                </div>
            `;

            const input = valueContainer.querySelector('.inline-edit-input');

            if (isPhone) {
                if (typeof window.initPhoneField === 'function') {
                    window.initPhoneField(input);
                }
                if (currentText) {
                    input.value = (typeof window.PhoneMask !== 'undefined')
                        ? window.PhoneMask.format(currentText)
                        : currentText;
                }
            }

            const btnDone = valueContainer.querySelector('.inline-edit-done-btn');
            const btnCancel = valueContainer.querySelector('.inline-edit-cancel-btn');
            const btnClear = valueContainer.querySelector('.inline-edit-clear');
            const errorTextEl = valueContainer.querySelector('.inline-error-text');

            input.focus();
            input.setSelectionRange(input.value.length, input.value.length);

            const showError = (msg) => {
                input.classList.add('has-error');
                errorTextEl.textContent = msg;
                errorTextEl.style.display = 'block';
            };

            const clearError = () => {
                input.classList.remove('has-error');
                errorTextEl.style.display = 'none';
                errorTextEl.textContent = '';
            };

            input.addEventListener('input', clearError);

            btnClear.addEventListener('click', () => {
                input.value = '';
                clearError();
                input.focus();
            });

            const cancelEdit = () => {
                valueContainer.innerHTML = oldHtml;
                if (currentErrorSpan) currentErrorSpan.textContent = '';
                setupProfileFieldEdit(groupId, fieldName);
                if (fieldName === 'PHONE' && typeof window.maskProfilePhoneOnLoad === 'function') {
                    window.maskProfilePhoneOnLoad();
                }
            };

            btnCancel.addEventListener('click', cancelEdit);

            const saveEdit = () => {
                let val = input.value.trim();

                if (!val && fieldName !== 'PHONE') {
                    showError('Поле не может быть пустым');
                    return;
                }

                if (isPhone && val) {
                    if (typeof window.isValidPhone === 'function' && !window.isValidPhone(val)) {
                        showError('Некорректный номер телефона');
                        return;
                    }
                }

                clearError();
                btnDone.disabled = true;

                const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
                const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
                const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : 'X-CSRF-TOKEN';
                const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : '';

                const headers = {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                };
                headers[csrfHeader] = csrfToken;

                fetch('/settings/change/field', {
                    method: 'PATCH',
                    headers: headers,
                    redirect: 'manual',
                    credentials: 'same-origin',
                    body: JSON.stringify({ identifier: fieldName, field: val })
                })
                .then(async (r) => {
                    if (r.type === 'opaqueredirect' || r.status === 0) {
                        throw new Error('Сессия истекла. Обновите страницу.');
                    }
                    if (r.redirected) {
                        throw new Error('Сессия истекла. Обновите страницу.');
                    }

                    const text = await r.text();
                    let data = {};
                    try {
                        data = JSON.parse(text);
                    } catch (e) {
                        if (!r.ok) throw new Error('Ошибка сервера (' + r.status + ')');
                        throw new Error('Некорректный ответ сервера');
                    }

                    if (!r.ok) {
                        const error = new Error(data.message || 'Ошибка сохранения');
                        error.code = data.code;
                        throw error;
                    }
                    return data;
                })
                .then((data) => {
                    if (fieldName === 'USERNAME' && window.AccountSwitcher) {
                        window.AccountSwitcher.rename(originalValue, val);
                    }

                    if (window.saveFlashAndRedirect) {
                        window.saveFlashAndRedirect(
                            (data && data.message) || 'Успешно обновлено',
                            'success',
                            window.location.href
                        );
                    } else {
                        window.location.reload();
                    }
                })
                .catch((err) => {
                    if (err.code === 'DATA_COINCIDENCE') {
                        cancelEdit();
                        return;
                    }

                    showError(err.message);
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
        };

        editBtn.addEventListener('click', activateEdit);

        const bindTextClick = () => {
            const textSpanEl = group.querySelector('.editable-text');
            const emptySpanEl = group.querySelector('.detail-value-empty');
            if (textSpanEl) {
                textSpanEl.addEventListener('click', activateEdit);
            }
            if (emptySpanEl) {
                emptySpanEl.addEventListener('click', activateEdit);
            }
        };

        bindTextClick();
    }

    function syncPhoneEmptyState() {
        var group = document.getElementById('phoneGroup');
        if (!group) return;
        var phoneEl = document.getElementById('panelPhone');
        var emptyEl = document.getElementById('panelPhoneEmpty');
        var editBtn = document.getElementById('phoneEditBtn');
        if (!phoneEl) return;

        var raw = (phoneEl.dataset.rawPhone || '').trim();
        var hasPhone = !!raw;

        if (emptyEl) emptyEl.style.display = hasPhone ? 'none' : 'inline-flex';
        if (editBtn) editBtn.style.display = hasPhone ? 'inline-flex' : 'none';
    }

    function init() {
        if (typeof window.maskProfilePhoneOnLoad === 'function') {
            window.maskProfilePhoneOnLoad();
        }
        syncPhoneEmptyState();

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