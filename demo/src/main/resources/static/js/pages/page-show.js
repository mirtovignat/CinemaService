(function () {
    'use strict';

    var FULL_TITLE_LIMIT = 53;

    function isTruncatedByLength(text) {
        return text.length >= FULL_TITLE_LIMIT;
    }

    function isTruncatedByWidth(el) {
        return el.scrollWidth > el.clientWidth + 1;
    }

    function buildTooltip(titleEl, fullText) {
        var tooltip = document.createElement('div');
        tooltip.className = 'movie-title-tooltip';
        tooltip.textContent = fullText;
        titleEl.parentNode.appendChild(tooltip);
        return tooltip;
    }

    function buildModal(fullText) {
        var modal = document.createElement('div');
        modal.className = 'movie-title-modal';
        modal.innerHTML =
            '<div class="movie-title-modal-content">' +
                '<div class="movie-title-modal-text"></div>' +
                '<button type="button" class="movie-title-modal-close">Закрыть</button>' +
            '</div>';
        modal.querySelector('.movie-title-modal-text').textContent = fullText;
        document.body.appendChild(modal);

        function close() {
            modal.classList.remove('show');
        }

        modal.addEventListener('click', function (e) {
            if (e.target === modal) close();
        });
        modal.querySelector('.movie-title-modal-close').addEventListener('click', close);
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') close();
        });

        return modal;
    }

    function initMovieTitleTruncation() {
        var titleEl = document.querySelector('.movie-title');
        if (!titleEl) return;

        var fullText = (titleEl.textContent || '').trim();
        if (!fullText) return;

        var shouldShowHelper =
            isTruncatedByLength(fullText) || isTruncatedByWidth(titleEl);

        if (!shouldShowHelper) return;

        titleEl.classList.add('is-truncated');
        titleEl.setAttribute('data-full-title', fullText);
        titleEl.setAttribute('title', fullText);

        var tooltip = buildTooltip(titleEl, fullText);

        titleEl.addEventListener('mouseenter', function () {
            tooltip.classList.add('show');
        });
        titleEl.addEventListener('mouseleave', function () {
            tooltip.classList.remove('show');
        });

        var modal = buildModal(fullText);

        titleEl.addEventListener('click', function () {
            modal.classList.add('show');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMovieTitleTruncation);
    } else {
        initMovieTitleTruncation();
    }

    window.addEventListener('load', initMovieTitleTruncation);
})();