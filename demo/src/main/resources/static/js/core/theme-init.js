(function () {
    'use strict';

    if ('scrollRestoration' in history) {
        history.scrollRestoration = 'manual';
    }

    try {
        var savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
        } else if (savedTheme === 'light') {
            document.documentElement.setAttribute('data-theme', 'light');
        }
    } catch (e) {}
})();