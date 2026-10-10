(function () {
    'use strict';

    function togglePassword() {
        var pwd = document.getElementById('password');
        var icon = document.getElementById('loginPasswordToggleIcon');
        if (!pwd) return;

        if (pwd.type === 'password') {
            pwd.type = 'text';
            if (icon) {
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        } else {
            pwd.type = 'password';
            if (icon) {
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            }
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

        if (identifierValue.value) {
            identifierType.value = detectIdentifierType(identifierValue.value);
        }

        identifierValue.addEventListener('input', function () {
            identifierType.value = detectIdentifierType(this.value);
        });
    }

    function isSwitchMode() {
        var params = new URLSearchParams(window.location.search);
        return params.has('switch');
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

        form.querySelectorAll('input[type="password"]').forEach(function (p) {
            p.type = 'password';
        });

        var icon = document.getElementById('loginPasswordToggleIcon');
        if (icon) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }

        document.querySelectorAll('.input-container.has-value').forEach(function (c) {
            c.classList.remove('has-value');
        });
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
        if (icon) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    }

    function performClear() {
        if (isSwitchMode()) {
            clearPasswordOnly();
        } else {
            clearForm();
        }
    }

    function scheduleHardClear() {
        if (window.__loginHardClearScheduled) return;
        window.__loginHardClearScheduled = true;

        performClear();
        setTimeout(performClear, 50);
        setTimeout(performClear, 250);

        setTimeout(function () {
            window.__loginHardClearScheduled = false;
        }, 400);
    }

    function installClearFormOnEveryEntry() {
        window.addEventListener('pageshow', scheduleHardClear);
        window.addEventListener('popstate', scheduleHardClear);
        window.addEventListener('beforeunload', performClear);

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

    function initClearButtons() {
        document.querySelectorAll('.input-container').forEach(function (container) {
            var input = container.querySelector('input');
            var clearBtn = container.querySelector('.clear-input-btn');
            if (!input || !clearBtn) return;

            var sync = function () {
                container.classList.toggle('has-value', input.value.length > 0);
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

    function submitWithAuthCheck(form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            if (isSwitchMode()) {
                HTMLFormElement.prototype.submit.call(form);
                return;
            }

            fetch('/auth/status?_=' + Date.now(), {
                method: 'GET',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                cache: 'no-store'
            })
            .then(function (r) {
                if (r.status === 200) {
                    window.location.replace('/');
                } else {
                    HTMLFormElement.prototype.submit.call(form);
                }
            })
            .catch(function () {
                HTMLFormElement.prototype.submit.call(form);
            });
        });
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
        submitWithAuthCheck(form);
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