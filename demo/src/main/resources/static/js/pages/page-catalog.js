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

    function syncRecommendationsVisibility(doc) {
        var main = document.querySelector('#catalogMain');
        if (!main) return;

        var newRec = doc.querySelector('.recommendations-section');
        var oldRec = main.querySelector('.recommendations-section');

        if (newRec && !oldRec) {
            var content = main.querySelector('.catalog-content');
            if (content) content.appendChild(newRec);
        } else if (!newRec && oldRec) {
            oldRec.remove();
        }
    }

    function mergeCurrentQueryIntoUrl(href) {
        try {
            var currentUrl = new URL(window.location.href);
            var targetUrl = new URL(href, window.location.origin);
            [
                'title',
                'genre',
                'priceFrom',
                'priceTo',
                'ratingFrom',
                'ratingTo',
                'durationFrom',
                'durationTo',
                'releaseDateFrom',
                'releaseDateTo',
                'sortBy',
                'sortDirection',
                'genres',
                'directors'
            ].forEach(function (key) {
                if (targetUrl.searchParams.has(key)) return;
                if (!currentUrl.searchParams.has(key)) return;
                currentUrl.searchParams.getAll(key).forEach(function (value) {
                    targetUrl.searchParams.append(key, value);
                });
            });
            return targetUrl.toString();
        } catch (e) {
            return href;
        }
    }

    function reinitFiltersAfterContentSwap() {
        if (window.FiltersSearch && typeof window.FiltersSearch.init === 'function') {
            window.FiltersSearch.init(document);
        }
        if (window.initFilterMasks) {
            try { window.initFilterMasks(); } catch (e) {}
        }
    }

    function initCatalogPage() {
        refreshCatalogBadges();
        refreshCatalogStatuses();

        document.addEventListener('click', function (event) {
            var link = event.target.closest('.page-nav');
            if (!link) return;

            if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            if (event.button !== 0) return;

            if (link.classList.contains('active')) {
                event.preventDefault();
                return;
            }

            var href = link.getAttribute('href');
            if (!href || href === '#') return;

            event.preventDefault();

            var targetHref = mergeCurrentQueryIntoUrl(href);

            var pagination = document.querySelector('.pagination');
            if (pagination) pagination.classList.add('pagination-loading');

            fetch(targetHref, {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'text/html'
                },
                redirect: 'manual',
                credentials: 'same-origin'
            })
            .then(function (response) {
                if (response.type === 'opaqueredirect' || response.status === 0) {
                    window.location.href = '/login';
                    return null;
                }
                if (response.redirected) {
                    window.location.href = response.url;
                    return null;
                }
                if (!response.ok) throw new Error('Network response was not ok');
                return response.text();
            })
            .then(function (html) {
                if (html == null) return;

                var parser = new DOMParser();
                var doc = parser.parseFromString(html, 'text/html');
                var newContent = doc.querySelector('#catalogContent');
                var newPagination = doc.querySelector('.pagination-wrapper');

                if (newContent) {
                    var oldContent = document.querySelector('#catalogContent');
                    if (oldContent) oldContent.innerHTML = newContent.innerHTML;
                }
                if (newPagination) {
                    var oldPagination = document.querySelector('.pagination-wrapper');
                    if (oldPagination) oldPagination.innerHTML = newPagination.innerHTML;
                }

                syncRecommendationsVisibility(doc);

                history.pushState({}, '', targetHref);
                refreshCatalogBadges();
                refreshCatalogStatuses();
                reinitFiltersAfterContentSwap();

                document.querySelectorAll('.js-rating-badge[data-movie-id]').forEach(function (el) {
                    var movieId = el.getAttribute('data-movie-id');
                    if (movieId) updateMovieRating(movieId);
                });

                if (pagination) pagination.classList.remove('pagination-loading');
            })
            .catch(function (error) {
                console.error('Error loading page:', error);
                if (pagination) pagination.classList.remove('pagination-loading');
                window.location.href = targetHref;
            });
        });

        window.addEventListener('pageshow', function (event) {
            if (event.persisted) {
                refreshCatalogBadges();
                refreshCatalogStatuses();
                reinitFiltersAfterContentSwap();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCatalogPage, { once: true });
    } else {
        initCatalogPage();
    }
})();