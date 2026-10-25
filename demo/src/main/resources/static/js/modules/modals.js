(function () {
    'use strict';

    var current = null;

    function handleModalOverlayClick(e) {
        if (e.target.classList && e.target.classList.contains('modal')) {
            e.target.classList.remove('show');
            if (e.target.id === 'confirmDeleteModal') {
                window._deleteMovieId = null;
            }
        }
        if (e.target.classList && e.target.classList.contains('modal-overlay')) {
            e.target.classList.remove('active');
            if (!document.querySelector('.modal-overlay.active')) {
                document.body.style.overflow = '';
            }
        }
    }

    function handleEscape(e) {
        if (e.key !== 'Escape') return;

        document.querySelectorAll('.modal.show').forEach(function (m) {
            m.classList.remove('show');
        });

        document.querySelectorAll('.modal-overlay.active').forEach(function (m) {
            m.classList.remove('active');
        });
        document.body.style.overflow = '';

        window._deleteMovieId = null;

        if (typeof window.hideRatingModal === 'function') window.hideRatingModal();
        if (typeof window.hideConfirmDeleteModal === 'function') window.hideConfirmDeleteModal();
        if (typeof window.closeDeleteAccountModal === 'function') window.closeDeleteAccountModal();
        if (typeof window.closeDeletePhoneModal === 'function') window.closeDeletePhoneModal();
        if (typeof window.closeClearHistoryModal === 'function') window.closeClearHistoryModal();
        if (typeof window.closeDeleteItemModal === 'function') window.closeDeleteItemModal();
    }

    function open(options) {
        var root = document.getElementById('appModal');
        if (!root) {
            if (window.console) console.warn('[Modal] appModal not found');
            return;
        }

        options = options || {};
        current = {
            onConfirm: options.onConfirm || null,
            onCancel: options.onCancel || null
        };

        var titleEl = root.querySelector('[data-modal-title]');
        var msgEl = root.querySelector('[data-modal-message]');
        var summaryEl = root.querySelector('[data-modal-summary]');
        var cancelTextEl = root.querySelector('[data-modal-cancel-text]');
        var confirmTextEl = root.querySelector('[data-modal-confirm-text]');
        var confirmBtn = root.querySelector('[data-modal-action="confirm"]');

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

        if (confirmBtn) {
            confirmBtn.classList.toggle('danger', !!options.danger);
            confirmBtn.disabled = false;
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

    function bindAppModal() {
        var root = document.getElementById('appModal');
        if (!root) return;
        if (root.dataset.modalBound === '1') return;
        root.dataset.modalBound = '1';

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
            if (e.target === root) cancel();
        });
    }

    function handleNamedModalClose(e) {
        var closeEl = findActionEl(e.target, 'data-modal-close');
        if (!closeEl) return;
        e.preventDefault();
        var modal = closeEl.closest('.modal');
        if (modal) modal.classList.remove('show');
    }

    function initModals() {
        document.addEventListener('click', handleModalOverlayClick);
        document.addEventListener('click', handleNamedModalClose);
        document.addEventListener('keydown', handleEscape);
        bindAppModal();
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

    window.getDeleteMovieId = function () { return window._deleteMovieId; };
    window.setDeleteMovieId = function (id) { window._deleteMovieId = id; };
    window.getRatingModalMode = function () { return window._ratingModalMode; };
    window.setRatingModalMode = function (mode) { window._ratingModalMode = mode || 'add'; };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initModals, { once: true });
    } else {
        initModals();
    }
})();