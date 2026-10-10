(function () {
    'use strict';

    function getStoredSetting(key, fallback) {
        try {
            var v = localStorage.getItem(key);
            return v === null ? fallback : v;
        } catch (e) {
            return fallback;
        }
    }

    function setStoredSetting(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch (e) {}
    }

    function setLanguage(lang) {
        if (!lang) return;
        setStoredSetting('cinema_lang', lang);
        document.documentElement.setAttribute('lang', lang);
        if (typeof showFlashMessage === 'function') {
            var labels = {
                ru: 'Русский',
                en: 'English',
                ru_beta: 'Русский (бета)'
            };
            showFlashMessage('Язык изменён: ' + (labels[lang] || lang), 'success');
        }
    }

    function applyTheme(theme) {
        var html = document.documentElement;
        html.classList.remove('theme-dark', 'theme-light', 'theme-system', 'theme-neon');

        if (theme === 'system') {
            var prefersDark = window.matchMedia &&
                window.matchMedia('(prefers-color-scheme: dark)').matches;
            html.classList.add(prefersDark ? 'theme-dark' : 'theme-light');
        } else {
            html.classList.add('theme-' + theme);
        }
    }

    function setTheme(theme, element) {
        if (!theme) return;
        setStoredSetting('cinema_theme', theme);

        document.querySelectorAll('.theme-option').forEach(function (el) {
            el.classList.remove('active');
        });
        if (element) element.classList.add('active');

        applyTheme(theme);

        if (typeof showFlashMessage === 'function') {
            var labels = {
                dark: 'Тёмная',
                light: 'Светлая',
                system: 'Системная',
                neon: 'Неон'
            };
            showFlashMessage('Тема: ' + (labels[theme] || theme), 'success');
        }
    }

    function initSettingsPage() {
        var select = document.getElementById('languageSelect');
        if (select) {
            var savedLang = getStoredSetting('cinema_lang', 'ru');
            select.value = savedLang;
        }

        var savedTheme = getStoredSetting('cinema_theme', 'dark');
        document.querySelectorAll('.theme-option').forEach(function (el) {
            el.classList.remove('active');
        });
        var activeThemeEl = document.querySelector('.theme-option[onclick*="' + savedTheme + '"]');
        if (activeThemeEl) {
            activeThemeEl.classList.add('active');
        } else {
            var first = document.querySelector('.theme-option');
            if (first) first.classList.add('active');
        }

        applyTheme(savedTheme);

        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
                if (getStoredSetting('cinema_theme', 'dark') === 'system') {
                    applyTheme('system');
                }
            });
        }
    }

    window.setLanguage = setLanguage;
    window.setTheme = setTheme;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSettingsPage, { once: true });
    } else {
        initSettingsPage();
    }
})();