(function () {
    'use strict';

    function sameOrigin(url) {
        if (!url) return false;
        try {
            var parsed = new URL(url, window.location.origin);
            return parsed.origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    function goBack(fallbackUrl) {
        var fallback = fallbackUrl || '/';
        window.location.href = '/back?fallback=' + encodeURIComponent(fallback);
    }

    function bind(root) {
        var scope = root || document;
        scope.querySelectorAll('[data-action="go-back"]').forEach(function (el) {
            if (el.dataset.backBound === '1') return;
            el.dataset.backBound = '1';
            el.addEventListener('click', function (e) {
                e.preventDefault();
                goBack(el.getAttribute('data-back-fallback'));
            });
        });
    }

    function registerAction() {
        if (!window.Actions) return;
        window.Actions.register('go-back', function (el) {
            goBack(el.getAttribute('data-back-fallback'));
            return true;
        });
    }

    window.BackButton = {
        goBack: goBack,
        bind: bind
    };

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerAction);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerAction, { once: true });
    } else {
        registerAction();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { bind(); }, { once: true });
    } else {
        bind();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) bind();
    });
})();