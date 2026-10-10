(function () {
    'use strict';

    function isPasswordField(el) {
        return el && el.tagName === 'INPUT' && (el.type || '').toLowerCase() === 'password';
    }

    function bindNoClipboard(el) {
        if (!isPasswordField(el)) return;
        if (el.dataset.noClipboardBound === '1') return;
        el.dataset.noClipboardBound = '1';

        el.addEventListener('copy', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('cut', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('paste', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('drop', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('dragstart', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('dragover', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('contextmenu', function (e) {
            e.preventDefault();
            e.stopPropagation();
        });

        el.addEventListener('beforeinput', function (e) {
            if (!e.inputType) return;
            var forbidden = [
                'insertFromPaste',
                'insertFromDrop',
                'insertFromYank',
                'insertReplacementText',
                'insertFromPasteAsQuotation'
            ];
            if (forbidden.indexOf(e.inputType) !== -1) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    function bindAllNoClipboard(root) {
        (root || document).querySelectorAll('input[type="password"]').forEach(bindNoClipboard);
    }

    function togglePasswordById(inputId, iconEl) {
        var pwd = document.getElementById(inputId);
        if (!pwd) return;

        if (pwd.type === 'password') {
            pwd.type = 'text';
            if (iconEl) {
                iconEl.classList.remove('fa-eye-slash');
                iconEl.classList.add('fa-eye');
            }
        } else {
            pwd.type = 'password';
            if (iconEl) {
                iconEl.classList.remove('fa-eye');
                iconEl.classList.add('fa-eye-slash');
            }
        }
    }

    function bindEye(btn) {
        if (!btn) return;
        if (btn.dataset.eyeBound === '1') return;
        btn.dataset.eyeBound = '1';

        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            var targetId = btn.getAttribute('data-target');
            if (!targetId) return;

            var icon = btn.querySelector('i');
            togglePasswordById(targetId, icon);
        });
    }

    function bindAllEyes(root) {
        (root || document).querySelectorAll('.toggle-password[data-target]').forEach(bindEye);
    }

    function bindAll(root) {
        bindAllNoClipboard(root);
        bindAllEyes(root);
    }

    function blockGlobalClipboardInPassword() {
        document.addEventListener('copy', function (e) {
            if (isPasswordField(document.activeElement)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);

        document.addEventListener('cut', function (e) {
            if (isPasswordField(document.activeElement)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);

        document.addEventListener('paste', function (e) {
            if (isPasswordField(document.activeElement)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);
    }

    function init() {
        bindAll(document);
    }

    blockGlobalClipboardInPassword();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) init();
    });

    window.PasswordTools = {
        bindAll: bindAll,
        bindAllNoClipboard: bindAllNoClipboard,
        bindAllEyes: bindAllEyes,
        toggleById: togglePasswordById
    };
})();