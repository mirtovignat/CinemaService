(function () {
    'use strict';

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

        var surname = document.getElementById('surname').value.trim();
        var rSurname = Validators.name(surname, 'фамилию');
        if (!rSurname.ok) { showError('surname', 'surnameError', rSurname.error); hasErrors = true; }

        var name = document.getElementById('name').value.trim();
        var rName = Validators.name(name, 'имя');
        if (!rName.ok) { showError('name', 'nameError', rName.error); hasErrors = true; }

        var patronymic = document.getElementById('patronymic').value.trim();
        if (patronymic && !/^[А-Яа-яЁёA-Za-z\-]+$/.test(patronymic)) {
            showError('patronymic', 'patronymicError', 'Только буквы и дефис');
            hasErrors = true;
        }

        var sex = document.getElementById('sex').value;
        if (sex !== 'true' && sex !== 'false') {
            showError('sex', 'sexError', 'Выберите пол');
            hasErrors = true;
        }

        var birthDate = document.getElementById('birthDate').value;
        var rBirth = Validators.birthDate(birthDate, 14, 120);
        if (!rBirth.ok) { showError('birthDate', 'birthDateError', rBirth.error); hasErrors = true; }

        var username = document.getElementById('username').value.trim();
        var rUsername = Validators.username(username);
        if (!rUsername.ok) { showError('username', 'usernameError', rUsername.error); hasErrors = true; }

        var email = document.getElementById('email').value.trim();
        var rEmail = Validators.email(email);
        if (!rEmail.ok) { showError('email', 'emailError', rEmail.error); hasErrors = true; }

        var phone = document.getElementById('phone').value.trim();
        var rPhone = Validators.phone(phone);
        if (!rPhone.ok) { showError('phone', 'phoneError', rPhone.error); hasErrors = true; }

        var rawPassword = document.getElementById('rawPassword').value;
        var rPwd = Validators.password(rawPassword, 8);
        if (!rPwd.ok) { showError('rawPassword', 'rawPasswordError', rPwd.error); hasErrors = true; }

        var repeat = document.getElementById('repeatRawPassword').value;
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

    function scheduleHardClear() {
        if (window.__registerHardClearScheduled) return;
        window.__registerHardClearScheduled = true;

        clearRegisterFields();

        setTimeout(clearRegisterFields, 50);
        setTimeout(clearRegisterFields, 250);

        setTimeout(function () {
            window.__registerHardClearScheduled = false;
        }, 400);
    }

    function installClearOnEveryEntry() {
        window.addEventListener('pageshow', scheduleHardClear);
        window.addEventListener('popstate', scheduleHardClear);
        window.addEventListener('beforeunload', clearRegisterFields);

        if (performance && performance.getEntriesByType) {
            var nav = performance.getEntriesByType('navigation')[0];
            if (nav && (nav.type === 'back_forward' || nav.type === 'reload' || nav.type === 'navigate')) {
                scheduleHardClear();
            }
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', scheduleHardClear, { once: true });
        } else {
            scheduleHardClear();
        }
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
        autoInitPhoneMasks();
        bindLiveValidation();
        autoHideFlash();
    }

    window.clearRegisterFields = clearRegisterFields;
    window.validateRegisterForm = validateForm;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initRegisterPage, { once: true });
    } else {
        initRegisterPage();
    }
})();