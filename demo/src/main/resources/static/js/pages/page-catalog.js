(function () {
    'use strict';

    function refreshCatalogBadges() {
        document.querySelectorAll('.reviews-badge[data-movie-id]').forEach(function (el) {
            var movieId = el.getAttribute('data-movie-id');
            if (movieId) updateReviewsCount(movieId);
        });
    }

    function refreshCatalogStatuses() {
        if (typeof applyStatusesFromSession === 'function') {
            applyStatusesFromSession();
        }
    }

    function initCatalogPage() {
        refreshCatalogBadges();
        refreshCatalogStatuses();

        window.addEventListener('pageshow', function (event) {
            if (event.persisted) {
                refreshCatalogBadges();
                refreshCatalogStatuses();
                if (window.FiltersSearch && typeof window.FiltersSearch.init === 'function') {
                    window.FiltersSearch.init(document);
                }
            }
        });

        window.addEventListener('beforeunload', function () {
            sessionStorage.setItem('catalogScrollPos', window.scrollY);
        });

        var savedPos = sessionStorage.getItem('catalogScrollPos');
        if (savedPos) {
            window.scrollTo(0, parseInt(savedPos, 10));
            sessionStorage.removeItem('catalogScrollPos');
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCatalogPage, { once: true });
    } else {
        initCatalogPage();
    }
})();