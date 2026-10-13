(function () {
    'use strict';

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (typeof window.closeDeleteAccountModal === 'function') {
                window.closeDeleteAccountModal();
            }
        }
    });
})();