(function () {
    'use strict';

    var REGISTER_DRAFT_KEY = 'registerFormDraft';

    function getNavType() {
        try {
            if (performance && performance.getEntriesByType) {
                var nav = performance.getEntriesByType('navigation')[0];
                if (nav && nav.type) return nav.type;
            }
        } catch (e) {}
        return 'navigate';
    }

    function showError(inputId, errorId, message) {
        var input = document.getElementById(inputId);
        var error = document.getElementById(errorId);
        if (input) input.classList.add('has-error');
        if (error) {
            error.textContent = message;
            error.classList.add('show');
        }
    }

    function clearError(inputId, errorId) {
        var input = document.getElementById(inputId);
        var error = document.getElementById(errorId);
        if (input) input.classList.remove('has-error');
        if (error) {
            error.textContent = '';
            error.classList.remove('show');
        }
    }

    function clearAllErrors() {
        [
            ['surname', 'surnameError'],
            ['name', 'nameError'],
            ['patronymic', 'patronymicError'],
            ['sex', 'sexError'],
            ['birthDate', 'birthDateError'],
            ['username', 'usernameError'],
            ['email', 'emailError'],
            ['phone', 'phoneError'],
            ['rawPassword', 'rawPasswordError'],
            ['repeatRawPassword', 'repeatRawPasswordError']
        ].forEach(function (pair) {
            clearError(pair[0], pair[1]);
        });
    }

    function validateForm() {
        clearAllErrors();
        var hasErrors = false;

        var surnameEl = document.getElementById('surname');
        var surname = surnameEl ? surnameEl.value.trim() : '';
        var rSurname = Validators.name(surname, 'фамилию');
        if (!rSurname.ok) { showError('surname', 'surnameError', rSurname.error); hasErrors = true; }

        var nameEl = document.getElementById('name');
        var name = nameEl ? nameEl.value.trim() : '';
        var rName = Validators.name(name, 'имя');
        if (!rName.ok) { showError('name', 'nameError', rName.error); hasErrors = true; }

        var patronymicEl = document.getElementById('patronymic');
        var patronymic = patronymicEl ? patronymicEl.value.trim() : '';
        if (patronymic && !/^[А-Яа-яЁёA-Za-z\-]+$/.test(patronymic)) {
            showError('patronymic', 'patronymicError', 'Только буквы и дефис');
            hasErrors = true;
        }

        var sexEl = document.getElementById('sex');
        var sex = sexEl ? sexEl.value : '';
        if (sex !== 'true' && sex !== 'false') {
            showError('sex', 'sexError', 'Выберите пол');
            hasErrors = true;
        }

        var birthEl = document.getElementById('birthDate');
        var birthDate = birthEl ? birthEl.value : '';
        var rBirth = Validators.birthDate(birthDate, 14, 120);
        if (!rBirth.ok) { showError('birthDate', 'birthDateError', rBirth.error); hasErrors = true; }

        var usernameEl = document.getElementById('username');
        var username = usernameEl ? usernameEl.value.trim() : '';
        var rUsername = Validators.username(username);
        if (!rUsername.ok) { showError('username', 'usernameError', rUsername.error); hasErrors = true; }

        var emailEl = document.getElementById('email');
        var email = emailEl ? emailEl.value.trim() : '';
        var rEmail = Validators.email(email);
        if (!rEmail.ok) { showError('email', 'emailError', rEmail.error); hasErrors = true; }

        var phoneEl = document.getElementById('phone');
        var phone = phoneEl ? phoneEl.value.trim() : '';
        var rPhone = Validators.phone(phone);
        if (!rPhone.ok) { showError('phone', 'phoneError', rPhone.error); hasErrors = true; }

        var rawPwdEl = document.getElementById('rawPassword');
        var rawPassword = rawPwdEl ? rawPwdEl.value : '';
        var rPwd = Validators.password(rawPassword, 8);
        if (!rPwd.ok) { showError('rawPassword', 'rawPasswordError', rPwd.error); hasErrors = true; }

        var repeatEl = document.getElementById('repeatRawPassword');
        var repeat = repeatEl ? repeatEl.value : '';
        if (!repeat) {
            showError('repeatRawPassword', 'repeatRawPasswordError', 'Повторите пароль');
            hasErrors = true;
        } else if (repeat !== rawPassword) {
            showError('repeatRawPassword', 'repeatRawPasswordError', 'Пароли не совпадают');
            hasErrors = true;
        }

        return !hasErrors;
    }

    function bindLiveValidation() {
        var fields = [
            { id: 'surname', errorId: 'surnameError' },
            { id: 'name', errorId: 'nameError' },
            { id: 'patronymic', errorId: 'patronymicError' },
            { id: 'sex', errorId: 'sexError' },
            { id: 'birthDate', errorId: 'birthDateError' },
            { id: 'username', errorId: 'usernameError' },
            { id: 'email', errorId: 'emailError' },
            { id: 'phone', errorId: 'phoneError' },
            { id: 'rawPassword', errorId: 'rawPasswordError' },
            { id: 'repeatRawPassword', errorId: 'repeatRawPasswordError' }
        ];

        fields.forEach(function (f) {
            var el = document.getElementById(f.id);
            if (!el) return;
            el.addEventListener('input', function () {
                clearError(f.id, f.errorId);
            });
        });

        var repeat = document.getElementById('repeatRawPassword');
        var raw = document.getElementById('rawPassword');
        if (repeat && raw) {
            repeat.addEventListener('input', function () {
                clearError('repeatRawPassword', 'repeatRawPasswordError');
            });
            repeat.addEventListener('blur', function () {
                if (this.value && this.value !== raw.value) {
                    showError('repeatRawPassword', 'repeatRawPasswordError', 'Пароли не совпадают');
                } else {
                    clearError('repeatRawPassword', 'repeatRawPasswordError');
                }
            });
            raw.addEventListener('input', function () {
                if (repeat.value && repeat.value !== raw.value) {
                    showError('repeatRawPassword', 'repeatRawPasswordError', 'Пароли не совпадают');
                } else if (repeat.value === raw.value && repeat.value) {
                    clearError('repeatRawPassword', 'repeatRawPasswordError');
                }
            });
        }
    }

    function initClearButtons() {
        document.querySelectorAll('.input-wrapper').forEach(function (wrapper) {
            var input = wrapper.querySelector('input');
            var clearBtn = wrapper.querySelector('.clear-input-btn');
            if (!input || !clearBtn) return;
            var sync = function () {
                wrapper.classList.toggle('has-value', input.value.length > 0);
            };
            clearBtn.addEventListener('click', function () {
                input.value = '';
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                input.focus();
                sync();
                saveRegisterDraft();
            });
            input.addEventListener('input', sync);
            input.addEventListener('change', sync);
            sync();
        });
    }

    function clearRegisterFields() {
        var form = document.getElementById('registerForm');
        if (!form) return;

        try { form.reset(); } catch (e) {}

        form.querySelectorAll('.form-control').forEach(function (field) {
            if (field.type === 'hidden') return;
            if (field.tagName === 'SELECT') {
                field.selectedIndex = 0;
                field.dispatchEvent(new Event('change', { bubbles: true }));
                return;
            }
            field.value = '';
            field.removeAttribute('value');
            field.dispatchEvent(new Event('input', { bubbles: true }));
            field.dispatchEvent(new Event('change', { bubbles: true }));
            field.classList.remove('has-error');
        });

        form.querySelectorAll('.input-wrapper').forEach(function (wrapper) {
            wrapper.classList.remove('has-value');
        });

        clearAllErrors();

        form.querySelectorAll('input[type="password"]').forEach(function (p) {
            p.type = 'password';
        });

        form.querySelectorAll('.toggle-password i').forEach(function (icon) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        });
    }

    function saveRegisterDraft() {
        var form = document.getElementById('registerForm');
        if (!form) return;
        try {
            var data = {};
            form.querySelectorAll('input[name]:not([type="hidden"]):not([type="submit"])').forEach(function (el) {
                if (el.type === 'password') return;
                if (el.name === '_csrf') return;
                if (el.type === 'checkbox' || el.type === 'radio') {
                    data[el.name + '::' + el.value] = el.checked;
                } else {
                    data[el.name] = el.value;
                }
            });
            form.querySelectorAll('select[name]').forEach(function (el) {
                data[el.name] = el.value;
            });
            sessionStorage.setItem(REGISTER_DRAFT_KEY, JSON.stringify(data));
        } catch (e) {}
    }

    function restoreRegisterDraft() {
        var form = document.getElementById('registerForm');
        if (!form) return;
        try {
            var raw = sessionStorage.getItem(REGISTER_DRAFT_KEY);
            if (!raw) return;
            var data = JSON.parse(raw);
            if (!data || typeof data !== 'object') return;

            form.querySelectorAll('input[name]:not([type="hidden"]):not([type="submit"])').forEach(function (el) {
                if (el.type === 'password') return;
                if (el.type === 'checkbox' || el.type === 'radio') {
                    var key = el.name + '::' + el.value;
                    if (key in data) el.checked = !!data[key];
                } else if (el.name in data) {
                    el.value = data[el.name];
                }
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                var wrapper = el.closest('.input-wrapper');
                if (wrapper) wrapper.classList.toggle('has-value', el.value.length > 0);
            });

            form.querySelectorAll('select[name]').forEach(function (el) {
                if (el.name in data) {
                    el.value = data[el.name];
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });
        } catch (e) {}
    }

    function clearRegisterDraft() {
        try { sessionStorage.removeItem(REGISTER_DRAFT_KEY); } catch (e) {}
    }

    function installClearOnEveryEntry() {
        var navType = getNavType();

        if (navType === 'reload') {
            restoreRegisterDraft();
        } else {
            clearRegisterDraft();
            clearRegisterFields();
        }

        var form = document.getElementById('registerForm');
        if (form) {
            form.addEventListener('input', saveRegisterDraft);
            form.addEventListener('change', saveRegisterDraft);
            form.addEventListener('submit', clearRegisterDraft);
        }

        window.addEventListener('pageshow', function (e) {
            if (!e.persisted) return;
            restoreRegisterDraft();
        });
    }

    function autoHideFlash() {
        setTimeout(function () {
            document.querySelectorAll('.flash-message').forEach(function (msg) {
                msg.style.animation = 'fadeOut 0.3s ease forwards';
                setTimeout(function () { msg.remove(); }, 300);
            });
        }, 4000);
    }

    function initRegisterPage() {
        var form = document.getElementById('registerForm');
        if (!form) return;

        installClearOnEveryEntry();
        initClearButtons();
        if (typeof window.autoInitPhoneMasks === 'function') {
            window.autoInitPhoneMasks(document);
        }
        bindLiveValidation();
        bindSubmit();
        autoHideFlash();
    }

    function bindSubmit() {
        var form = document.getElementById('registerForm');
        if (!form) return;
        if (form.dataset.validateBound === '1') return;
        form.dataset.validateBound = '1';

        form.addEventListener('submit', function (e) {
            if (typeof window.Validators === 'undefined') return;
            if (!validateForm()) {
                e.preventDefault();
                var firstError = form.querySelector('.has-error');
                if (firstError) {
                    try { firstError.focus(); } catch (err) {}
                    try {
                        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } catch (err) {}
                }
            }
        });
    }

    window.clearRegisterFields = clearRegisterFields;
    window.validateRegisterForm = validateForm;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initRegisterPage, { once: true });
    } else {
        initRegisterPage();
    }
})();