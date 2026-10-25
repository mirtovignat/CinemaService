(function () {
    'use strict';

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            if (typeof window.closeDeleteAccountModal === 'function') {
                window.closeDeleteAccountModal();
            }
        }
    });

    document.addEventListener('click', function (event) {
        var confirmButton = event.target.closest('[data-action="confirm-delete-account"]');
        if (!confirmButton) return;

        if (window.AccountSwitcher && typeof window.AccountSwitcher.removeCurrent === 'function') {
            window.AccountSwitcher.removeCurrent();
        }
    });
})();