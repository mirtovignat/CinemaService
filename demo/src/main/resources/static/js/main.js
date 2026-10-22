(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var flash = sessionStorage.getItem('flashMessage');
        if (flash) {
            try {
                var data = JSON.parse(flash);
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage(data.message, data.type, undefined, data.action || null);
                }
                sessionStorage.removeItem('flashMessage');
            } catch (e) {}
        }
    });

    function refreshCounts() {
        if (typeof UI !== 'undefined' && typeof UI.refreshBadges === 'function') {
            UI.refreshBadges();
        }
    }

    window.refreshCounts = refreshCounts;

    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) return;
        if (typeof isAuthenticated === 'function' && !isAuthenticated()) return;

        refreshCounts();

        if (typeof window.reloadRecommendations === 'function') {
            window.reloadRecommendations();
        }

        document.querySelectorAll('.js-reviews-count[data-movie-id]').forEach(function (el) {
            var id = el.getAttribute('data-movie-id');
            if (id && typeof updateReviewsCount === 'function') updateReviewsCount(id);
        });

        document.querySelectorAll('.js-rating-badge[data-movie-id], .js-rating-badge-max[data-movie-id]').forEach(function (el) {
            var id = el.getAttribute('data-movie-id');
            if (id && typeof updateMovieRating === 'function') updateMovieRating(id);
        });
    });

    document.addEventListener('visibilitychange', function () {
        if (document.visibilityState !== 'visible') return;
        if (typeof isAuthenticated === 'function' && isAuthenticated()) {
            refreshCounts();
        }
    });
})();