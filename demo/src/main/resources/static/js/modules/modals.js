(function () {
    'use strict';

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

    function initModals() {
        document.addEventListener('click', handleModalOverlayClick);
        document.addEventListener('keydown', handleEscape);
    }

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