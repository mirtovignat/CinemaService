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

    var PLACEHOLDER_HTML =
        '<div class="poster-placeholder-icon">' +
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" ' +
                 'fill="none" stroke="currentColor" stroke-width="1.6" ' +
                 'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                '<rect x="3" y="3" width="18" height="18" rx="3"/>' +
                '<circle cx="9" cy="9" r="2"/>' +
                '<path d="m21 15-4.35-4.35a1.5 1.5 0 0 0-2.12 0L7 18"/>' +
            '</svg>' +
        '</div>' +
        '<div class="poster-placeholder-text">Нет постера</div>' +
        '<div class="poster-placeholder-sub">Изображение недоступно</div>';

    function buildPosterPlaceholder() {
        var wrap = document.createElement('div');
        wrap.className = 'movie-poster-placeholder';
        wrap.setAttribute('data-poster-placeholder', '');
        wrap.innerHTML = PLACEHOLDER_HTML;
        return wrap;
    }

    function getPosterContainer(img) {
        if (!img) return null;
        return img.closest(
            '.media-card-poster-link, .movie-poster-link, .poster-wrapper, ' +
            '.history-item-poster, .fav-poster'
        ) || img.parentNode;
    }

    function hideOverlay(container) {
        if (!container) return;
        var overlay = container.querySelector('.poster-overlay');
        if (overlay) {
            overlay.style.display = 'none';
        }
    }

    function showPlaceholderFor(img) {
        if (!img || !img.parentNode) return;

        // Не вмешиваемся в работу рекомендаций, у них свой обработчик
        if (img.closest('.rec-card') || img.closest('.rec-poster')) return;

        var container = getPosterContainer(img);
        if (!container) return;

        img.classList.add('hidden');
        img.setAttribute('aria-hidden', 'true');
        img.style.display = 'none';

        var placeholder = container.querySelector('[data-poster-placeholder]');
        if (placeholder) {
            placeholder.classList.remove('hidden');
            placeholder.removeAttribute('style');
        } else {
            var linkEl = container.tagName === 'A'
                ? container
                : (container.closest && container.closest('a')) || container.querySelector('a');

            if (linkEl) {
                linkEl.removeAttribute('href');
                linkEl.removeAttribute('data-action');
                linkEl.removeAttribute('data-poster-url');
                linkEl.removeAttribute('title');
                linkEl.style.cursor = 'default';
                linkEl.style.pointerEvents = 'none';
                linkEl.appendChild(buildPosterPlaceholder());
            } else if (
                container.classList.contains('history-item-poster') ||
                container.classList.contains('fav-poster') ||
                container.classList.contains('poster-wrapper') ||
                container.classList.contains('media-card-poster-link') ||
                container.classList.contains('movie-poster-link')
            ) {
                container.appendChild(buildPosterPlaceholder());
            }
        }

        hideOverlay(container);
    }

    document.addEventListener('error', function (e) {
        var t = e.target;
        if (!t || t.tagName !== 'IMG' || !t.hasAttribute('data-poster-img')) return;
        showPlaceholderFor(t);
    }, true);

    function recheckBrokenImages() {
        document.querySelectorAll('img[data-poster-img]').forEach(function (img) {
            if (img.complete && img.naturalWidth === 0 && img.src) {
                showPlaceholderFor(img);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', recheckBrokenImages, { once: true });
    } else {
        recheckBrokenImages();
    }

    window.addEventListener('load', recheckBrokenImages);

    window.escapeHtml = escapeHtml;
    window.getMovieId = getMovieId;
    window.getMovieTitle = getMovieTitle;
    window.getMoviePrice = getMoviePrice;
    window.getDeclension = getDeclension;
    window.buildPosterPlaceholder = buildPosterPlaceholder;
    window.showPlaceholderFor = showPlaceholderFor;
})();