(function () {
    'use strict';

    var REGISTER_DRAFT_KEY = 'registerFormDraft';

    function getNavType() {
        try {
            if (performance && performance.getEntriesByType) {
                var navigationEntry = performance.getEntriesByType('navigation')[0];
                if (navigationEntry && navigationEntry.type) return navigationEntry.type;
            }
        } catch (error) {}
        return 'navigate';
    }

    function showError(inputId, errorId, errorMessage) {
        var inputElement = document.getElementById(inputId);
        var errorElement = document.getElementById(errorId);
        if (inputElement) inputElement.classList.add('has-error');
        if (errorElement) {
            errorElement.textContent = errorMessage;
            errorElement.classList.add('show');
        }
    }

    function clearError(inputId, errorId) {
        var inputElement = document.getElementById(inputId);
        var errorElement = document.getElementById(errorId);
        if (inputElement) inputElement.classList.remove('has-error');
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.classList.remove('show');
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
        ].forEach(function (fieldPair) {
            clearError(fieldPair[0], fieldPair[1]);
        });
    }

    function validateForm() {
        clearAllErrors();
        var hasErrors = false;

        var surnameElement = document.getElementById('surname');
        var surname = surnameElement ? surnameElement.value.trim() : '';
        var surnameValidation = Validators.name(surname, 'фамилию');
        if (!surnameValidation.ok) { showError('surname', 'surnameError', surnameValidation.error); hasErrors = true; }

        var nameElement = document.getElementById('name');
        var name = nameElement ? nameElement.value.trim() : '';
        var nameValidation = Validators.name(name, 'имя');
        if (!nameValidation.ok) { showError('name', 'nameError', nameValidation.error); hasErrors = true; }

        var patronymicElement = document.getElementById('patronymic');
        var patronymic = patronymicElement ? patronymicElement.value.trim() : '';
        if (patronymic && !/^[А-Яа-яЁёA-Za-z\-]+$/.test(patronymic)) {
            showError('patronymic', 'patronymicError', 'Только буквы и дефис');
            hasErrors = true;
        }

        var sexElement = document.getElementById('sex');
        var sex = sexElement ? sexElement.value : '';
        if (sex !== 'true' && sex !== 'false') {
            showError('sex', 'sexError', 'Выберите пол');
            hasErrors = true;
        }

        var birthDateElement = document.getElementById('birthDate');
        var birthDate = birthDateElement ? birthDateElement.value : '';
        var birthDateValidation = Validators.birthDate(birthDate, 14, 120);
        if (!birthDateValidation.ok) { showError('birthDate', 'birthDateError', birthDateValidation.error); hasErrors = true; }

        var usernameElement = document.getElementById('username');
        var username = usernameElement ? usernameElement.value.trim() : '';
        var usernameValidation = Validators.username(username);
        if (!usernameValidation.ok) { showError('username', 'usernameError', usernameValidation.error); hasErrors = true; }

        var emailElement = document.getElementById('email');
        var email = emailElement ? emailElement.value.trim() : '';
        var emailValidation = Validators.email(email);
        if (!emailValidation.ok) { showError('email', 'emailError', emailValidation.error); hasErrors = true; }

        var phoneElement = document.getElementById('phone');
        var phone = phoneElement ? phoneElement.value.trim() : '';
        var phoneValidation = Validators.phone(phone);
        if (!phoneValidation.ok) { showError('phone', 'phoneError', phoneValidation.error); hasErrors = true; }

        var rawPasswordElement = document.getElementById('rawPassword');
        var rawPassword = rawPasswordElement ? rawPasswordElement.value : '';
        var rawPasswordValidation = Validators.password(rawPassword, 8);
        if (!rawPasswordValidation.ok) { showError('rawPassword', 'rawPasswordError', rawPasswordValidation.error); hasErrors = true; }

        var repeatPasswordElement = document.getElementById('repeatRawPassword');
        var repeatPassword = repeatPasswordElement ? repeatPasswordElement.value : '';
        if (!repeatPassword) {
            showError('repeatRawPassword', 'repeatRawPasswordError', 'Повторите пароль');
            hasErrors = true;
        } else if (repeatPassword !== rawPassword) {
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

        fields.forEach(function (field) {
            var element = document.getElementById(field.id);
            if (!element) return;
            element.addEventListener('input', function () {
                clearError(field.id, field.errorId);
            });
        });

        var repeatPasswordElement = document.getElementById('repeatRawPassword');
        var rawPasswordElement = document.getElementById('rawPassword');
        if (repeatPasswordElement && rawPasswordElement) {
            repeatPasswordElement.addEventListener('input', function () {
                clearError('repeatRawPassword', 'repeatRawPasswordError');
            });
            repeatPasswordElement.addEventListener('blur', function () {
                if (this.value && this.value !== rawPasswordElement.value) {
                    showError('repeatRawPassword', 'repeatRawPasswordError', 'Пароли не совпадают');
                } else {
                    clearError('repeatRawPassword', 'repeatRawPasswordError');
                }
            });
            rawPasswordElement.addEventListener('input', function () {
                if (repeatPasswordElement.value && repeatPasswordElement.value !== rawPasswordElement.value) {
                    showError('repeatRawPassword', 'repeatRawPasswordError', 'Пароли не совпадают');
                } else if (repeatPasswordElement.value === rawPasswordElement.value && repeatPasswordElement.value) {
                    clearError('repeatRawPassword', 'repeatRawPasswordError');
                }
            });
        }
    }

    function initClearButtons() {
        document.querySelectorAll('.input-wrapper').forEach(function (inputWrapper) {
            var inputElement = inputWrapper.querySelector('input');
            var clearButton = inputWrapper.querySelector('.clear-input-btn');
            if (!inputElement || !clearButton) return;
            var syncHasValue = function () {
                inputWrapper.classList.toggle('has-value', inputElement.value.length > 0);
            };
            clearButton.addEventListener('click', function () {
                inputElement.value = '';
                inputElement.dispatchEvent(new Event('input', { bubbles: true }));
                inputElement.dispatchEvent(new Event('change', { bubbles: true }));
                inputElement.focus();
                syncHasValue();
            });
            inputElement.addEventListener('input', syncHasValue);
            inputElement.addEventListener('change', syncHasValue);
            syncHasValue();
        });
    }

    function clearRegisterFields() {
        var registerForm = document.getElementById('registerForm');
        if (!registerForm) return;

        try { registerForm.reset(); } catch (error) {}

        registerForm.querySelectorAll('.form-control').forEach(function (formField) {
            if (formField.type === 'hidden') return;
            if (formField.tagName === 'SELECT') {
                formField.selectedIndex = 0;
                formField.dispatchEvent(new Event('change', { bubbles: true }));
                return;
            }
            formField.value = '';
            formField.removeAttribute('value');
            formField.dispatchEvent(new Event('input', { bubbles: true }));
            formField.dispatchEvent(new Event('change', { bubbles: true }));
            formField.classList.remove('has-error');
        });

        registerForm.querySelectorAll('.input-wrapper').forEach(function (inputWrapper) {
            inputWrapper.classList.remove('has-value');
        });

        clearAllErrors();

        registerForm.querySelectorAll('input[type="password"]').forEach(function (passwordInput) {
            passwordInput.type = 'password';
        });

        registerForm.querySelectorAll('.toggle-password i').forEach(function (iconElement) {
            iconElement.classList.remove('fa-eye');
            iconElement.classList.add('fa-eye-slash');
        });
    }

    function autoHideFlash() {
        setTimeout(function () {
            document.querySelectorAll('.flash-message').forEach(function (flashMessageElement) {
                flashMessageElement.style.animation = 'fadeOut 0.3s ease forwards';
                setTimeout(function () { flashMessageElement.remove(); }, 300);
            });
        }, 4000);
    }

    function initRegisterPage() {
        var registerForm = document.getElementById('registerForm');
        if (!registerForm) return;

        clearRegisterFields();
        initClearButtons();
        if (typeof window.autoInitPhoneMasks === 'function') {
            window.autoInitPhoneMasks(document);
        }
        bindLiveValidation();
        bindSubmit();
        autoHideFlash();
    }

    function bindSubmit() {
        var registerForm = document.getElementById('registerForm');
        if (!registerForm) return;
        if (registerForm.dataset.validateBound === '1') return;
        registerForm.dataset.validateBound = '1';

        registerForm.addEventListener('submit', function (event) {
            if (typeof window.Validators === 'undefined') return;
            if (!validateForm()) {
                event.preventDefault();
                var firstErrorElement = registerForm.querySelector('.has-error');
                if (firstErrorElement) {
                    try { firstErrorElement.focus(); } catch (error) {}
                    try {
                        firstErrorElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } catch (error) {}
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