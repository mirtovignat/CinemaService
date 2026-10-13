(function () {
    'use strict';

    function formatRating(v) {
        return v == null ? '' : String(v).replace('.', ',');
    }

    var RATING_TARGETS = [
        {
            root: '.js-rating-badge',
            value: null,
            format: function (v) { return v == null ? '★ —' : '★ ' + formatRating(v); }
        },
        {
            root: '.js-rating-badge-max',
            value: null,
            format: function (v) { return v == null ? '★ — / 10' : '★ ' + formatRating(v) + ' / 10'; }
        },
        {
            root: '.rating-val',
            value: null,
            format: function (v) { return v == null ? '★ —' : '★ ' + formatRating(v); }
        },
        {
            root: '.js-media-rating-badge',
            value: '.rating-value',
            format: function (v) { return formatRating(v); },
            hideWhenNull: true
        },
        {
            root: '.favourites-rating-badge',
            value: '.rating-value',
            format: function (v) { return formatRating(v); },
            hideWhenNull: true
        }
    ];

    function normalizeRating(rating) {
        if (rating === null || rating === undefined) return null;
        if (rating === '-' || rating === '—' || rating === 'undefined') return null;
        var n = parseFloat(rating);
        if (isNaN(n)) return null;
        return n.toFixed(1);
    }

    function updateRatingBadge(movieId, rating) {
        if (!movieId) return;
        var value = normalizeRating(rating);
        var id = CSS.escape(String(movieId));

        RATING_TARGETS.forEach(function (target) {
            document.querySelectorAll(target.root + '[data-movie-id="' + id + '"]').forEach(function (rootEl) {
                if (!target.value) {
                    var newText = target.format(value);
                    if (rootEl.textContent !== newText) {
                        rootEl.textContent = newText;
                    }
                    return;
                }
                if (target.hideWhenNull) {
                    rootEl.style.display = value == null ? 'none' : '';
                }
                var valueEl = rootEl.querySelector(target.value);
                if (valueEl) {
                    var newValue = target.format(value);
                    if (valueEl.textContent !== newValue) {
                        valueEl.textContent = newValue;
                    }
                }
            });
        });

        var pageMovieId = (window.location.pathname.match(/\/movies\/(\d+)/) || [])[1];
        if (pageMovieId && String(pageMovieId) === String(movieId)) {
            var single = document.querySelector('.rating-value:not([data-movie-id])');
            if (single) {
                single.textContent = value == null ? '—' : formatRating(value) + ' / 10';
            }
        }
    }

    function updateMovieRating(movieId) {
        if (!movieId) return Promise.resolve();
        return fetch('/movies/' + encodeURIComponent(movieId) + '/rating?_=' + Date.now(), {
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        })
            .then(function (r) {
                if (!r.ok) throw new Error('Ошибка получения рейтинга');
                return r.json();
            })
            .then(function (data) {
                updateRatingBadge(movieId, data && data.rating);
                return data;
            })
            .catch(function (e) { console.warn(e); });
    }

    function applyRatingResponse(data, movieId) {
        var payload = (data && data.data) || data || {};
        if (payload.rating !== undefined) {
            updateRatingBadge(movieId, payload.rating);
        }
        if (payload.reviewsCount !== undefined && typeof applyReviewsCount === 'function') {
            applyReviewsCount(movieId, Number(payload.reviewsCount) || 0);
        }
    }

    function updateReviewsCount(movieId) {
        if (!movieId) return;
        fetch('/rated/count/' + encodeURIComponent(movieId), {
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        })
            .then(r => {
                if (!r.ok) throw new Error('Ошибка получения количества отзывов');
                return r.text();
            })
            .then(count => {
                const num = parseInt(count, 10) || 0;
                applyReviewsCount(movieId, num);
            })
            .catch(console.warn);
    }

    function applyReviewsCount(movieId, num) {
        if (!movieId) return;
        const value = Number(num) || 0;
        const idStr = String(movieId);
        const escaped = window.CSS && CSS.escape ? CSS.escape(idStr) : idStr;

        document.querySelectorAll(
            '.js-reviews-count[data-movie-id="' + escaped + '"], ' +
            '.reviews-badge[data-movie-id="' + escaped + '"]'
        ).forEach(function (badge) {
            badge.textContent = value;
            badge.style.display = value > 0 ? '' : 'none';
        });

        document.querySelectorAll(
            '.reviews-count-text[data-movie-id="' + escaped + '"]'
        ).forEach(function (el) {
            el.textContent = 'Отзывы: ' + value + ' шт.';
        });

        const reviewsCounter = document.getElementById('reviewsCount');
        if (reviewsCounter) {
            reviewsCounter.textContent = value + ' ' + getDeclension(value);
        }
    }

    function autoInitReviewsCount() {
        var reviewsCountEl = document.getElementById('reviewsCount');
        if (reviewsCountEl) {
            var initialText = reviewsCountEl.textContent.trim();
            var initialMatch = initialText.match(/^(\d+)/);
            if (initialMatch) {
                var initialCount = parseInt(initialMatch[1], 10);
                reviewsCountEl.textContent = initialCount + ' ' + getDeclension(initialCount);
            }
        }

        document.querySelectorAll('.js-reviews-count[data-movie-id]').forEach(function (el) {
            var id = el.getAttribute('data-movie-id');
            if (id) updateReviewsCount(id);
        });

        document.querySelectorAll('.reviews-count-text[data-movie-id]').forEach(function (el) {
            var id = el.getAttribute('data-movie-id');
            if (id) updateReviewsCount(id);
        });

        var reviewsContainer = document.querySelector('.reviews-container');
        if (reviewsContainer) {
            var movieId = reviewsContainer.getAttribute('data-movie-id');
            if (movieId) updateReviewsCount(movieId);
        }
    }

    window.updateReviewsCount = updateReviewsCount;
    window.applyReviewsCount = applyReviewsCount;
    window.updateRatingBadge = updateRatingBadge;
    window.updateMovieRating = updateMovieRating;
    window.applyRatingResponse = applyRatingResponse;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoInitReviewsCount, { once: true });
    } else {
        autoInitReviewsCount();
    }
})();