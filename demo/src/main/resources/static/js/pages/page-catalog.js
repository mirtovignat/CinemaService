(function () {
    'use strict';

    function refreshCatalogBadges() {
        document.querySelectorAll('.reviews-badge[data-movie-id]').forEach(function (el) {
            var id = el.getAttribute('data-movie-id');
            if (id) updateReviewsCount(id);
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

        document.addEventListener('click', function (e) {
            var link = e.target.closest('.page-nav');
            if (!link) return;

            if (link.classList.contains('active')) {
                e.preventDefault();
                return;
            }

            var href = link.getAttribute('href');
            if (!href || href === '#') return;

            e.preventDefault();

            var pagination = document.querySelector('.pagination');
            if (pagination) pagination.classList.add('pagination-loading');

            fetch(href, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(function (response) {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.text();
            })
            .then(function (html) {
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

                history.pushState({}, '', href);
                refreshCatalogBadges();
                refreshCatalogStatuses();

                document.querySelectorAll('.js-rating-badge[data-movie-id]').forEach(function (el) {
                    var id = el.getAttribute('data-movie-id');
                    if (id) updateMovieRating(id);
                });

                if (pagination) pagination.classList.remove('pagination-loading');
            })
            .catch(function (error) {
                console.error('Error loading page:', error);
                if (pagination) pagination.classList.remove('pagination-loading');
                window.location.href = href;
            });
        });

        window.addEventListener('pageshow', function (event) {
            if (event.persisted) {
                refreshCatalogBadges();
                refreshCatalogStatuses();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCatalogPage, { once: true });
    } else {
        initCatalogPage();
    }
})();