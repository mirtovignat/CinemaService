(function () {
    'use strict';

    if ('scrollRestoration' in history) {
        history.scrollRestoration = 'manual';
    }

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

        document.addEventListener('click', function (e) {
            var link = e.target.closest('a');
            if (!link) return;

            var href = link.getAttribute('href');
            if (href === '/' || href === window.location.origin + '/') {
                sessionStorage.removeItem('catalogScrollPos');
                sessionStorage.setItem('forceScrollTop', 'true');
            }
        });

        var forceScroll = sessionStorage.getItem('forceScrollTop');
        var savedPos = sessionStorage.getItem('catalogScrollPos');

        if (window.location.pathname === '/') {
            if (forceScroll === 'true') {
                window.scrollTo(0, 0);
                sessionStorage.removeItem('forceScrollTop');
                sessionStorage.removeItem('catalogScrollPos');
            } else if (savedPos) {
                window.scrollTo(0, parseInt(savedPos, 10));
                sessionStorage.removeItem('catalogScrollPos');
            } else {
                window.scrollTo(0, 0);
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCatalogPage, { once: true });
    } else {
        initCatalogPage();
    }
})();