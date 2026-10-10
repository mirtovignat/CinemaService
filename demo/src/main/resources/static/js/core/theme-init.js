(function () {
    'use strict';

    try {
        var theme = localStorage.getItem('cinema_theme') || 'dark';
        document.documentElement.classList.add('theme-' + theme);
        var lang = localStorage.getItem('cinema_lang') || 'ru';
        document.documentElement.setAttribute('lang', lang);
    } catch (e) {}
})();