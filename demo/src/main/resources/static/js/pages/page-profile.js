(function () {
    'use strict';

    var ICON_CHECK =
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" ' +
        'fill="none" stroke="currentColor" stroke-width="2.2" ' +
        'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
        '<path d="M4.5 12.75l6 6 9-13.5"/></svg>';

    var ICON_X =
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" ' +
        'fill="none" stroke="currentColor" stroke-width="2.2" ' +
        'stroke-linecap="round" aria-hidden="true">' +
        '<path d="M7 7l10 10M17 7L7 17"/></svg>';

    function pushUserInfo(user) {
        if (typeof window.updateSidebarAndHeader === 'function') {
            window.updateSidebarAndHeader(user);
        }
    }

    function closeDeletePhoneModal() {
        var e = document.getElementById('deletePhoneModal');
        if (e) e.classList.remove('show');
    }

    function normalizeForCompare(fieldType, value) {
        if (value == null) return '';
        var raw = String(value).trim();
        if (fieldType === 'PHONE') {
            if (raw === '') return '';
            if (typeof window.PhoneMask !== 'undefined' && typeof window.PhoneMask.parse === 'function') {
                var parsed = window.PhoneMask.parse(raw);
                if (parsed && parsed.digits) return parsed.digits;
            }
            return raw.replace(/\D/g, '');
        }
        if (fieldType === 'EMAIL') {
            return raw.toLowerCase();
        }
        return raw;
    }

    function getEditableValue(textElement, fieldType) {
        if (fieldType === 'PHONE') {
            var rawAttr = textElement.getAttribute('data-raw-phone');
            if (rawAttr && rawAttr.trim() !== '') {
                return rawAttr.trim();
            }
        }
        return textElement.textContent.trim();
    }

    function setEditableValue(textElement, fieldType, value) {
        var raw = value || '';
        if (fieldType === 'PHONE') {
            textElement.setAttribute('data-raw-phone', raw);
            if (raw === '') {
                textElement.textContent = '';
                return;
            }
            var display = raw;
            if (typeof window.maskPhone === 'function') {
                display = window.maskPhone(raw);
            }
            textElement.textContent = display;
            return;
        }
        textElement.textContent = raw;
    }

    function startInlineEdit(groupId, fieldType) {
        var group = document.getElementById(groupId);
        if (!group) return;
        var textElement = group.querySelector('.editable-text');
        if (!textElement) return;
        if (group.querySelector('.inline-edit-wrapper')) return;

        var oldValueRaw = getEditableValue(textElement, fieldType);
        var oldValueNormalized = normalizeForCompare(fieldType, oldValueRaw);
        var editTrigger = group.querySelector('.edit-trigger');
        var emptyElement = group.querySelector('.detail-value-empty');

        var wrapper = document.createElement('div');
        wrapper.className = 'inline-edit-wrapper';
        var controls = document.createElement('div');
        controls.className = 'inline-edit-controls';
        var inputWrap = document.createElement('div');
        inputWrap.className = 'inline-edit-input-wrap';

        var input = document.createElement('input');
        input.type = 'text';
        input.className = 'inline-edit-input';
        input.dataset.fieldType = fieldType;

        if (fieldType === 'PHONE') {
            input.type = 'tel';
            var formatted = oldValueRaw;
            if (formatted !== '' && typeof window.formatPhoneRaw === 'function') {
                formatted = window.formatPhoneRaw(formatted);
            }
            input.value = formatted;
            if (typeof window.initPhoneField === 'function') {
                window.initPhoneField(input);
            }
        } else if (fieldType === 'EMAIL') {
            input.type = 'email';
            input.value = oldValueRaw;
        } else {
            input.value = oldValueRaw;
        }

        var clearButton = document.createElement('button');
        clearButton.type = 'button';
        clearButton.className = 'inline-edit-clear';
        clearButton.setAttribute('aria-label', 'Очистить');
        clearButton.innerHTML = ICON_X;
        clearButton.onclick = function () { input.value = ''; input.focus(); };

        inputWrap.appendChild(input);
        inputWrap.appendChild(clearButton);

        var actions = document.createElement('div');
        actions.className = 'inline-edit-actions';

        var doneButton = document.createElement('button');
        doneButton.type = 'button';
        doneButton.className = 'inline-edit-done-btn';
        doneButton.setAttribute('aria-label', 'Сохранить');
        doneButton.innerHTML = ICON_CHECK;

        var cancelButton = document.createElement('button');
        cancelButton.type = 'button';
        cancelButton.className = 'inline-edit-cancel-btn';
        cancelButton.setAttribute('aria-label', 'Отмена');
        cancelButton.innerHTML = ICON_X;

        var errorElement = document.createElement('div');
        errorElement.className = 'inline-edit-error';

        function closeWithoutChange() {
            wrapper.replaceWith(textElement);
            var isEmpty = oldValueRaw === '';
            if (editTrigger) editTrigger.style.display = isEmpty ? 'none' : 'inline-flex';
            if (emptyElement) emptyElement.style.display = isEmpty ? 'inline-flex' : 'none';
        }

        function validateAndSave() {
            var value = input.value.trim();
            errorElement.classList.remove('show');
            input.classList.remove('has-error');

            var result;
            if (fieldType === 'PHONE') {
                result = Validators.phone(value);
            } else if (fieldType === 'EMAIL') {
                result = Validators.email(value);
            } else {
                result = Validators.username(value);
            }

            if (!result.ok) {
                errorElement.textContent = result.error;
                errorElement.classList.add('show');
                input.classList.add('has-error');
                return;
            }

            var newValueNormalized = normalizeForCompare(fieldType, value);

            if (newValueNormalized === oldValueNormalized) {
                closeWithoutChange();
                return;
            }

            API.request('/settings/change/field', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ identifier: fieldType, field: value })
            })
            .then(function (data) {
                var maybeUser = (data && data.data && data.data.user) || (data && data.user) || null;

                var storedValue = value;
                if (fieldType === 'PHONE' && maybeUser && maybeUser.phone != null) {
                    storedValue = maybeUser.phone;
                }

                setEditableValue(textElement, fieldType, storedValue);
                wrapper.replaceWith(textElement);

                var isEmpty = !storedValue;
                if (editTrigger) editTrigger.style.display = isEmpty ? 'none' : 'inline-flex';
                if (emptyElement) emptyElement.style.display = isEmpty ? 'inline-flex' : 'none';

                var patchedUser = {};
                if (fieldType === 'USERNAME') patchedUser.username = value;
                if (fieldType === 'EMAIL') patchedUser.email = value;
                if (fieldType === 'PHONE') patchedUser.phone = storedValue;

                if (maybeUser) {
                    for (var k in maybeUser) {
                        if (Object.prototype.hasOwnProperty.call(maybeUser, k)) {
                            patchedUser[k] = maybeUser[k];
                        }
                    }
                }

                pushUserInfo(patchedUser);

                if (typeof showFlashMessage === 'function') {
                    showFlashMessage('Данные изменены', 'success');
                }
            })
            .catch(function (error) {
                errorElement.textContent = (error && error.message) || 'Ошибка обновления';
                errorElement.classList.add('show');
                input.classList.add('has-error');
            });
        }

        function cancelEdit() {
            closeWithoutChange();
        }

        doneButton.onclick = validateAndSave;
        cancelButton.onclick = cancelEdit;

        input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') { event.preventDefault(); validateAndSave(); }
            if (event.key === 'Escape') { event.preventDefault(); cancelEdit(); }
        });

        if (editTrigger) editTrigger.style.display = 'none';
        if (emptyElement) emptyElement.style.display = 'none';

        actions.appendChild(doneButton);
        actions.appendChild(cancelButton);
        controls.appendChild(inputWrap);
        controls.appendChild(actions);
        controls.appendChild(errorElement);
        wrapper.appendChild(controls);
        textElement.replaceWith(wrapper);

        input.focus();
        input.select();

        var fieldError = group.querySelector('.field-error');
        if (fieldError) fieldError.classList.remove('show');
    }

    function bindInlineEditTriggers() {
        document.querySelectorAll('[data-edit-group][data-edit-field]').forEach(function (btn) {
            if (btn.dataset.inlineEditBound === '1') return;
            btn.dataset.inlineEditBound = '1';
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                var groupId = btn.getAttribute('data-edit-group');
                var fieldType = btn.getAttribute('data-edit-field');
                if (groupId && fieldType) startInlineEdit(groupId, fieldType);
            });
        });
    }

    function bindCloseDeletePhoneModal() {
        document.querySelectorAll('[data-close-delete-phone-modal]').forEach(function (btn) {
            if (btn.dataset.closePhoneBound === '1') return;
            btn.dataset.closePhoneBound = '1';
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                closeDeletePhoneModal();
            });
        });
    }

    function bindConfirmDeletePhone() {
        var btn = document.getElementById('confirmDeletePhoneBtn');
        if (!btn) return;
        if (btn.dataset.confirmPhoneBound === '1') return;
        btn.dataset.confirmPhoneBound = '1';

        btn.addEventListener('click', function () {
            API.del('/settings/delete/phone')
            .then(function (data) {
                closeDeletePhoneModal();

                var phone = document.getElementById('panelPhone');
                var empty = document.getElementById('panelPhoneEmpty');
                var edit = document.getElementById('phoneEditBtn');

                if (phone) {
                    phone.textContent = '';
                    phone.setAttribute('data-raw-phone', '');
                }
                if (empty) empty.style.display = 'inline-flex';
                if (edit) edit.style.display = 'none';

                var patchedUser = { phone: '' };
                var maybeUser = (data && data.data && data.data.user) || (data && data.user) || null;
                if (maybeUser) {
                    for (var k in maybeUser) {
                        if (Object.prototype.hasOwnProperty.call(maybeUser, k)) {
                            patchedUser[k] = maybeUser[k];
                        }
                    }
                }
                pushUserInfo(patchedUser);

                if (typeof showFlashMessage === 'function') {
                    showFlashMessage('Номер телефона удален', 'success');
                }
            })
            .catch(function () {
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage('Ошибка удаления', 'error');
                }
            });
        });
    }

    function maskInitialPhones() {
        var phone = document.getElementById('panelPhone');
        if (!phone) return;
        var raw = phone.getAttribute('data-raw-phone') || phone.textContent.trim();
        if (!raw) return;
        if (raw.indexOf('***') !== -1) return;
        phone.setAttribute('data-raw-phone', raw);
        if (typeof window.maskPhone === 'function') {
            phone.textContent = window.maskPhone(raw);
        }
    }

    function init() {
        maskInitialPhones();
        bindInlineEditTriggers();
        bindCloseDeletePhoneModal();
        bindConfirmDeletePhone();

        document.addEventListener('keydown', function (event) {
            if (event.key !== 'Escape') return;
            if (typeof window.closeDeleteAccountModal === 'function') {
                window.closeDeleteAccountModal();
            }
            closeDeletePhoneModal();
        });
    }

    window.startInlineEdit = startInlineEdit;
    window.closeDeletePhoneModal = closeDeletePhoneModal;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();