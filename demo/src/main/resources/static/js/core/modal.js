(function () {
    'use strict';

    var current = null;

    function el(name) {
        return document.querySelector('[data-modal-' + name + ']');
    }

    function open(options) {
        var root = document.getElementById('appModal');
        if (!root) {
            if (window.console) console.warn('appModal not found');
            return;
        }

        current = {
            onConfirm: options.onConfirm || null,
            onCancel: options.onCancel || null
        };

        var titleEl = el('title');
        var msgEl = el('message');
        var summaryEl = el('summary');
        var cancelTextEl = el('cancel-text');
        var confirmTextEl = el('confirm-text');
        var submitBtn = root.querySelector('[data-modal-action="confirm"]');

        if (titleEl) titleEl.textContent = options.title || '';
        if (msgEl) msgEl.textContent = options.message || '';
        if (cancelTextEl) cancelTextEl.textContent = options.cancelText || 'Отмена';
        if (confirmTextEl) confirmTextEl.textContent = options.confirmText || 'Подтвердить';

        if (summaryEl) {
            if (options.summaryHtml) {
                summaryEl.innerHTML = options.summaryHtml;
                summaryEl.classList.remove('hidden');
            } else {
                summaryEl.innerHTML = '';
                summaryEl.classList.add('hidden');
            }
        }

        if (submitBtn) {
            submitBtn.classList.toggle('danger', !!options.danger);
            submitBtn.disabled = false;
        }

        root.classList.add('show');
    }

    function close() {
        var root = document.getElementById('appModal');
        if (root) root.classList.remove('show');
        current = null;
    }

    function confirm() {
        var fn = current ? current.onConfirm : null;
        close();
        if (typeof fn === 'function') fn();
    }

    function cancel() {
        var fn = current ? current.onCancel : null;
        close();
        if (typeof fn === 'function') fn();
    }

    function findActionEl(target) {
        if (!target || !target.closest) return null;
        return target.closest('[data-modal-action]');
    }

    function init() {
        var root = document.getElementById('appModal');
        if (!root) return;

        root.addEventListener('click', function (e) {
            var actionEl = findActionEl(e.target);
            if (actionEl) {
                var action = actionEl.getAttribute('data-modal-action');
                if (action === 'cancel') {
                    e.preventDefault();
                    cancel();
                    return;
                }
                if (action === 'confirm') {
                    e.preventDefault();
                    confirm();
                    return;
                }
            }
            if (e.target === root) {
                cancel();
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key !== 'Escape') return;
            if (root.classList.contains('show')) cancel();
        });
    }

    window.Modal = {
        open: open,
        close: close,
        confirm: confirm,
        cancel: cancel
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();