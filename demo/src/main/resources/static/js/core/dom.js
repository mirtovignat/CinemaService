(function () {
    'use strict';

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function isMeaningful(v) {
        return v !== null && v !== undefined && v !== '' && v !== 'null' && v !== 'undefined';
    }

    function getMovieId(element) {
        if (!element || !element.getAttribute) return '';

        var direct = element.getAttribute('data-movie-id');
        if (isMeaningful(direct)) return direct;

        var card = element.closest && element.closest('[data-movie-id]');
        if (card) {
            var fromCard = card.getAttribute('data-movie-id');
            if (isMeaningful(fromCard)) return fromCard;
        }

        var link = element.closest && element.closest('a[href*="/movies/"]');
        if (link) {
            var m = (link.getAttribute('href') || '').match(/\/movies\/(\d+)/);
            if (m) return m[1];
        }

        var pageMatch = (window.location.pathname || '').match(/\/movies\/(\d+)/);
        if (pageMatch) return pageMatch[1];

        return '';
    }

    function getMovieTitle(element) {
        if (!element || !element.getAttribute) return '';
        var direct = element.getAttribute('data-movie-title');
        if (isMeaningful(direct)) return direct;
        var card = element.closest && element.closest('[data-movie-title]');
        if (card) {
            var t = card.getAttribute('data-movie-title');
            if (isMeaningful(t)) return t;
        }
        return '';
    }

    function getMoviePrice(element) {
        if (!element || !element.getAttribute) return '';
        var direct = element.getAttribute('data-movie-price');
        if (isMeaningful(direct)) return direct;
        var card = element.closest && element.closest('[data-movie-price]');
        if (card) {
            var p = card.getAttribute('data-movie-price');
            if (isMeaningful(p)) return p;
        }
        return '';
    }

    function getDeclension(n) {
        n = Math.abs(n);
        var lastDigit = n % 10;
        var lastTwoDigits = n % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return 'отзывов';
        if (lastDigit === 1) return 'отзыв';
        if (lastDigit >= 2 && lastDigit <= 4) return 'отзыва';
        return 'отзывов';
    }

    function installPosterErrorHandler() {
        document.addEventListener('error', function (e) {
            var target = e.target;
            if (!target || !target.matches || !target.matches('[data-poster-img]')) return;
            target.style.display = 'none';
            var placeholder = target.parentNode
                ? target.parentNode.querySelector('[data-poster-placeholder]')
                : null;
            if (placeholder) placeholder.classList.remove('hidden');
        }, true);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', installPosterErrorHandler, { once: true });
    } else {
        installPosterErrorHandler();
    }

    window.escapeHtml = escapeHtml;
    window.getMovieId = getMovieId;
    window.getMovieTitle = getMovieTitle;
    window.getMoviePrice = getMoviePrice;
    window.getDeclension = getDeclension;
})();