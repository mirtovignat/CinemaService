(function () {
    'use strict';

    function isAuthenticated() {
        var authStatus = document.getElementById('authStatus');
        if (!authStatus) {
            return false;
        }
        return authStatus.dataset.authenticated === 'true';
    }

    function requireAuth(event) {
        if (isAuthenticated()) return true;
        if (event && typeof event.preventDefault === 'function') {
            event.preventDefault();
        }
        return false;
    }

    function navigateProtected(url) {
        if (!isAuthenticated()) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Авторизуйтесь!', 'error', undefined, {
                    text: 'Перейти к авторизации',
                    url: '/login'
                });
            }
            return;
        }
        window.location.href = url;
    }

    window.isAuthenticated = isAuthenticated;
    window.requireAuth = requireAuth;
    window.navigateProtected = navigateProtected;
})();