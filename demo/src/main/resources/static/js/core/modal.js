(function () {
    'use strict';

    var current = null;

    function el(name) {
        return document.querySelector('[data-modal-' + name + ']');
    }

    function isOverlayTarget(target) {
        if (!target || !target.classList) return false;
        return target.classList.contains('modal') || target.classList.contains('modal-overlay');
    }

    function hasActiveSelection() {
        try {
            var sel = window.getSelection();
            return !!(sel && sel.toString().length > 0);
        } catch (e) {
            return false;
        }
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
        var confirmTextEl = el('confirm-text');
        var submitBtn = root.querySelector('[data-modal-action="confirm"]');

        if (titleEl) titleEl.textContent = options.title || '';
        if (msgEl) msgEl.textContent = options.message || '';
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

    function openById(id) {
        var modal = document.getElementById(id);
        if (!modal) {
            if (window.console) console.warn('[Modal] not found:', id);
            return null;
        }
        modal.classList.add('show');
        modal.classList.remove('active');
        return modal;
    }

    function closeById(id) {
        var modal = document.getElementById(id);
        if (!modal) return;
        modal.classList.remove('show');
        modal.classList.remove('active');
    }

    function setText(id, text) {
        var el = document.getElementById(id);
        if (el) el.textContent = text || '';
    }

    function setHtml(selector, html) {
        var el = document.querySelector(selector);
        if (el) el.innerHTML = html || '';
    }

    function findActionEl(target, attr) {
        if (!target || !target.closest) return null;
        return target.closest('[' + attr + ']');
    }

    function handleNamedModalClick(e) {
        var closeEl = findActionEl(e.target, 'data-modal-close');
        if (closeEl) {
            e.preventDefault();
            var modal = closeEl.closest('.modal');
            if (modal) modal.classList.remove('show');
            return;
        }
    }

    function handleEscapeNamed(e) {
        if (e.key !== 'Escape') return;
        document.querySelectorAll('.modal.show').forEach(function (m) {
            if (m.id !== 'appModal' && m.id !== 'ratingModal' && m.id !== 'confirmDeleteModal') {
                m.classList.remove('show');
            }
        });
        document.querySelectorAll('.modal-overlay.active').forEach(function (m) {
            m.classList.remove('active');
        });
    }

    var overlayMouseDownTarget = null;

    function handleOverlayMouseDown(e) {
        overlayMouseDownTarget = isOverlayTarget(e.target) ? e.target : null;
    }

    function handleOverlayMouseUp(e) {
        if (!overlayMouseDownTarget) return;
        if (!isOverlayTarget(e.target) || e.target !== overlayMouseDownTarget) {
            overlayMouseDownTarget = null;
            return;
        }
        if (hasActiveSelection()) {
            overlayMouseDownTarget = null;
            return;
        }

        var target = overlayMouseDownTarget;
        overlayMouseDownTarget = null;

        if (target.classList.contains('modal-overlay')) {
            target.classList.remove('active');
            if (!document.querySelector('.modal-overlay.active')) {
                document.body.style.overflow = '';
            }
        } else if (target.classList.contains('modal') && target.id !== 'appModal') {
            target.classList.remove('show');
        }
    }

    function init() {
        var root = document.getElementById('appModal');
        if (root) {
            var appOverlayMouseDown = false;

            root.addEventListener('mousedown', function (e) {
                appOverlayMouseDown = (e.target === root);
            });

            root.addEventListener('mouseup', function (e) {
                var wasOverlay = appOverlayMouseDown;
                appOverlayMouseDown = false;
                if (!wasOverlay) return;
                if (e.target !== root) return;
                if (hasActiveSelection()) return;
                cancel();
            });

            root.addEventListener('click', function (e) {
                var actionEl = findActionEl(e.target, 'data-modal-action');
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
            });
        }

        document.addEventListener('mousedown', handleOverlayMouseDown, true);
        document.addEventListener('mouseup', handleOverlayMouseUp, true);
        document.addEventListener('click', handleNamedModalClick);
        document.addEventListener('keydown', handleEscapeNamed);
    }

    window.Modal = {
        open: open,
        close: close,
        confirm: confirm,
        cancel: cancel,
        openById: openById,
        closeById: closeById,
        setText: setText,
        setHtml: setHtml
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();