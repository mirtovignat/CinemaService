(function () {
    'use strict';

    var LOGIN_DRAFT_KEY = 'loginFormDraft';

    function togglePassword() {
        var pwd = document.getElementById('password');
        var icon = document.getElementById('loginPasswordToggleIcon');
        if (!pwd) return;

        if (pwd.type === 'password') {
            pwd.type = 'text';
            if (icon) { icon.classList.remove('fa-eye-slash'); icon.classList.add('fa-eye'); }
        } else {
            pwd.type = 'password';
            if (icon) { icon.classList.remove('fa-eye'); icon.classList.add('fa-eye-slash'); }
        }
    }

    function detectIdentifierType(value) {
        if (!value || value.trim() === '') return 'USERNAME';
        var trimmed = value.trim();
        if (trimmed.indexOf('@') !== -1) return 'EMAIL';
        if (/^\+?[\d\s\-()]{7,15}$/.test(trimmed)) return 'PHONE';
        return 'USERNAME';
    }

    function bindIdentifierTypeSync() {
        var identifierValue = document.getElementById('identifierValue');
        var identifierType = document.getElementById('identifierType');
        if (!identifierValue || !identifierType) return;

        if (identifierValue.value) identifierType.value = detectIdentifierType(identifierValue.value);
        identifierValue.addEventListener('input', function () {
            identifierType.value = detectIdentifierType(this.value);
        });
    }

    function isSwitchMode() {
        return new URLSearchParams(window.location.search).has('switch');
    }

    function getNavType() {
        try {
            if (performance && performance.getEntriesByType) {
                var nav = performance.getEntriesByType('navigation')[0];
                if (nav && nav.type) return nav.type;
            }
        } catch (e) {}
        return 'navigate';
    }

    function clearForm() {
        var form = document.getElementById('loginForm');
        if (!form) return;
        try { form.reset(); } catch (e) {}
        form.querySelectorAll('input').forEach(function (input) {
            if (input.type === 'hidden') return;
            input.value = '';
            input.removeAttribute('value');
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        });
        form.querySelectorAll('select').forEach(function (sel) {
            sel.selectedIndex = 0;
            sel.dispatchEvent(new Event('change', { bubbles: true }));
        });
        form.querySelectorAll('input[type="password"]').forEach(function (p) { p.type = 'password'; });
        var icon = document.getElementById('loginPasswordToggleIcon');
        if (icon) { icon.classList.remove('fa-eye'); icon.classList.add('fa-eye-slash'); }
        document.querySelectorAll('.input-container.has-value').forEach(function (c) { c.classList.remove('has-value'); });
    }

    function clearPasswordOnly() {
        var form = document.getElementById('loginForm');
        if (!form) return;
        form.querySelectorAll('input[type="password"]').forEach(function (p) {
            p.value = '';
            p.removeAttribute('value');
            p.dispatchEvent(new Event('input', { bubbles: true }));
            p.dispatchEvent(new Event('change', { bubbles: true }));
            p.type = 'password';
        });
        var icon = document.getElementById('loginPasswordToggleIcon');
        if (icon) { icon.classList.remove('fa-eye'); icon.classList.add('fa-eye-slash'); }
    }

    function performClear() {
        isSwitchMode() ? clearPasswordOnly() : clearForm();
    }

    function saveLoginDraft() {
        var form = document.getElementById('loginForm');
        if (!form) return;
        try {
            var data = {};
            form.querySelectorAll('input[name]:not([type="hidden"]):not([type="submit"])').forEach(function (el) {
                if (el.type === 'password') return;
                if (el.name === '_csrf') return;
                data[el.name] = el.value;
            });
            sessionStorage.setItem(LOGIN_DRAFT_KEY, JSON.stringify(data));
        } catch (e) {}
    }

    function restoreLoginDraft() {
        var form = document.getElementById('loginForm');
        if (!form) return;
        try {
            var raw = sessionStorage.getItem(LOGIN_DRAFT_KEY);
            if (!raw) return;
            var data = JSON.parse(raw);
            if (!data || typeof data !== 'object') return;
            form.querySelectorAll('input[name]:not([type="hidden"]):not([type="submit"])').forEach(function (el) {
                if (el.type === 'password') return;
                if (!(el.name in data)) return;
                el.value = data[el.name];
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                var container = el.closest('.input-container');
                if (container) container.classList.toggle('has-value', el.value.length > 0);
            });
            var identifierValue = document.getElementById('identifierValue');
            var identifierType = document.getElementById('identifierType');
            if (identifierValue && identifierType) {
                identifierType.value = detectIdentifierType(identifierValue.value);
            }
        } catch (e) {}
    }

    function clearLoginDraft() {
        try { sessionStorage.removeItem(LOGIN_DRAFT_KEY); } catch (e) {}
    }

    function installClearFormOnEveryEntry() {
        var navType = getNavType();

        if (navType === 'reload' && !isSwitchMode()) {
            restoreLoginDraft();
        } else {
            clearLoginDraft();
            performClear();
        }

        var form = document.getElementById('loginForm');
        if (form) {
            form.addEventListener('input', saveLoginDraft);
            form.addEventListener('change', saveLoginDraft);
            form.addEventListener('submit', clearLoginDraft);
        }

        window.addEventListener('pageshow', function (e) {
            if (!e.persisted) return;
            if (!isSwitchMode()) restoreLoginDraft();
        });
    }

    function initClearButtons() {
        document.querySelectorAll('.input-container').forEach(function (container) {
            var input = container.querySelector('input');
            var clearBtn = container.querySelector('.clear-input-btn');
            if (!input || !clearBtn) return;
            var sync = function () { container.classList.toggle('has-value', input.value.length > 0); };
            clearBtn.addEventListener('click', function () {
                input.value = '';
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                input.focus();
                sync();
                saveLoginDraft();
            });
            input.addEventListener('input', sync);
            input.addEventListener('change', sync);
            sync();
        });
    }

    function autoHideServerFlash() {
        var container = document.getElementById('flash-messages-container');
        if (!container) return;
        var msgs = container.querySelectorAll('.flash-message');
        if (!msgs.length) return;
        setTimeout(function () {
            msgs.forEach(function (msg) {
                setTimeout(function () {
                    if (!msg.parentNode) return;
                    msg.style.transition = 'opacity .5s ease';
                    msg.style.opacity = '0';
                    setTimeout(function () { msg.remove(); }, 500);
                }, 4500);
            });
        }, 100);
    }

    function registerLoginActions() {
        if (!window.Actions) return;
        if (window.__loginActionsBound) return;
        window.__loginActionsBound = true;
        window.Actions.register('toggle-login-password', function () {
            togglePassword();
            return true;
        });
    }

    function initLoginPage() {
        var form = document.getElementById('loginForm');
        if (!form) return;

        installClearFormOnEveryEntry();
        initClearButtons();
        bindIdentifierTypeSync();
        autoHideServerFlash();
    }

    window.togglePassword = togglePassword;

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerLoginActions);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerLoginActions, { once: true });
    } else {
        registerLoginActions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initLoginPage, { once: true });
    } else {
        initLoginPage();
    }
})();