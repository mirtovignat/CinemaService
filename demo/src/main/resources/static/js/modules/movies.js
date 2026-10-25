(function () {
    'use strict';

    function formatRating(ratingValue) {
        return ratingValue == null ? '' : String(ratingValue).replace('.', ',');
    }

    var RATING_TARGETS = [
        {
            root: '.js-rating-badge',
            value: null,
            format: function (ratingValue) { return ratingValue == null ? '★ —' : '★ ' + formatRating(ratingValue); }
        },
        {
            root: '.js-rating-badge-max',
            value: null,
            format: function (ratingValue) { return ratingValue == null ? '★ — / 10' : '★ ' + formatRating(ratingValue) + ' / 10'; }
        },
        {
            root: '.rating-val',
            value: null,
            format: function (ratingValue) { return ratingValue == null ? '★ —' : '★ ' + formatRating(ratingValue); }
        },
        {
            root: '.js-media-rating-badge',
            value: '.rating-value',
            format: function (ratingValue) { return formatRating(ratingValue); },
            hideWhenNull: true
        },
        {
            root: '.favourites-rating-badge',
            value: '.rating-value',
            format: function (ratingValue) { return formatRating(ratingValue); },
            hideWhenNull: true
        }
    ];

    function normalizeRating(rating) {
        if (rating === null || rating === undefined) return null;
        if (rating === '-' || rating === '—' || rating === 'undefined') return null;
        var numericRating = parseFloat(rating);
        if (isNaN(numericRating)) return null;
        return numericRating.toFixed(1);
    }

    function updateRatingBadge(movieId, rating) {
        if (!movieId) return;
        var normalizedRating = normalizeRating(rating);
        var escapedMovieId = CSS.escape(String(movieId));

        RATING_TARGETS.forEach(function (target) {
            document.querySelectorAll(target.root + '[data-movie-id="' + escapedMovieId + '"]').forEach(function (rootElement) {
                if (!target.value) {
                    var newText = target.format(normalizedRating);
                    if (rootElement.textContent !== newText) {
                        rootElement.textContent = newText;
                    }
                    return;
                }
                if (target.hideWhenNull) {
                    rootElement.style.display = normalizedRating == null ? 'none' : '';
                }
                var valueElement = rootElement.querySelector(target.value);
                if (valueElement) {
                    var newValue = target.format(normalizedRating);
                    if (valueElement.textContent !== newValue) {
                        valueElement.textContent = newValue;
                    }
                }
            });
        });

        var pageMovieId = (window.location.pathname.match(/\/movies\/(\d+)/) || [])[1];
        if (pageMovieId && String(pageMovieId) === String(movieId)) {
            var singleRatingElement = document.querySelector('.rating-value:not([data-movie-id])');
            if (singleRatingElement) {
                singleRatingElement.textContent = normalizedRating == null ? '—' : formatRating(normalizedRating) + ' / 10';
            }
        }
    }

    function updateMovieRating(movieId) {
        if (!movieId) return Promise.resolve();
        return fetch('/movies/' + encodeURIComponent(movieId) + '/rating?_=' + Date.now(), {
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) throw new Error('Ошибка получения рейтинга');
                return response.json();
            })
            .then(function (data) {
                updateRatingBadge(movieId, data && data.rating);
                return data;
            })
            .catch(function (error) { console.warn(error); });
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
            .then(function (response) {
                if (!response.ok) throw new Error('Ошибка получения количества отзывов');
                return response.text();
            })
            .then(function (countText) {
                var parsedCount = parseInt(countText, 10) || 0;
                applyReviewsCount(movieId, parsedCount);
            })
            .catch(console.warn);
    }

    function applyReviewsCount(movieId, count) {
        if (!movieId) return;
        var numericCount = Number(count) || 0;
        var movieIdString = String(movieId);
        var escapedMovieId = window.CSS && CSS.escape ? CSS.escape(movieIdString) : movieIdString;

        document.querySelectorAll(
            '.js-reviews-count[data-movie-id="' + escapedMovieId + '"], ' +
            '.reviews-badge[data-movie-id="' + escapedMovieId + '"]'
        ).forEach(function (badge) {
            badge.textContent = numericCount;
            badge.style.display = numericCount > 0 ? '' : 'none';
        });

        document.querySelectorAll(
            '.reviews-count-text[data-movie-id="' + escapedMovieId + '"]'
        ).forEach(function (textElement) {
            textElement.textContent = 'Отзывы: ' + numericCount + ' шт.';
        });

        var reviewsCounter = document.getElementById('reviewsCount');
        if (reviewsCounter) {
            reviewsCounter.textContent = numericCount + ' ' + getDeclension(numericCount);
        }
    }

    function autoInitReviewsCount() {
        var reviewsCountElement = document.getElementById('reviewsCount');
        if (reviewsCountElement) {
            var initialText = reviewsCountElement.textContent.trim();
            var initialMatch = initialText.match(/^(\d+)/);
            if (initialMatch) {
                var initialCount = parseInt(initialMatch[1], 10);
                reviewsCountElement.textContent = initialCount + ' ' + getDeclension(initialCount);
            }
        }

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