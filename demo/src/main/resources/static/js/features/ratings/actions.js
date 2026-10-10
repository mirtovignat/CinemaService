(function () {
    'use strict';

    function isMeaningful(v) {
        return v !== null && v !== undefined && v !== '' && v !== 'null' && v !== 'undefined';
    }

    function getMovieIdSafe(el) {
        if (!el || !el.getAttribute) return '';
        var direct = el.getAttribute('data-movie-id');
        if (isMeaningful(direct)) return direct;
        var card = el.closest && el.closest('[data-movie-id]');
        if (card) {
            var v = card.getAttribute('data-movie-id');
            if (isMeaningful(v)) return v;
        }
        return '';
    }

    function getMovieTitleSafe(el) {
        if (!el || !el.getAttribute) return '';
        var direct = el.getAttribute('data-movie-title');
        if (isMeaningful(direct)) return direct;
        var card = el.closest && el.closest('[data-movie-title]');
        if (card) {
            var v = card.getAttribute('data-movie-title');
            if (isMeaningful(v)) return v;
        }
        return '';
    }

    function onShowRating(el) {
        var movieId = getMovieIdSafe(el);
        var movieTitle = getMovieTitleSafe(el);

        if (!movieId) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Не удалось определить фильм', 'error');
            }
            return false;
        }

        if (typeof window.showRatingModal !== 'function') {
            if (window.console) {
                console.warn('[Ratings] showRatingModal недоступен');
            }
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Окно оценки ещё загружается, попробуйте снова', 'warning');
            }
            return false;
        }

        window.showRatingModal(movieId, movieTitle, 'add', 5.0, '');
        return true;
    }

    function registerActions() {
        if (!window.Actions) {
            if (window.console) console.error('[Ratings] Actions core not loaded');
            return;
        }
        if (window.__ratingShowBound) return;
        window.__ratingShowBound = true;
        window.Actions.register('show-rating', onShowRating);
    }

    function fetchOwn(movieId) {
        return fetch('/rated/form/' + encodeURIComponent(movieId), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(function (r) { return r.ok ? r.json() : null; })
        .catch(function () { return null; });
    }

    function fetchAvg(movieId) {
        return fetch('/rated/rating/' + encodeURIComponent(movieId) + '?_=' + Date.now(), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (dto) {
            if (!dto || dto.rating == null || dto.rating === '-' || dto.rating === '—') return null;
            var n = parseFloat(dto.rating);
            return isNaN(n) ? null : n;
        })
        .catch(function () { return null; });
    }

    function fetchCount(movieId) {
        return fetch('/rated/count/' + encodeURIComponent(movieId), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(function (r) { return r.ok ? r.text() : null; })
        .then(function (raw) {
            if (raw == null) return null;
            var n = parseInt(raw, 10);
            return isNaN(n) ? null : n;
        })
        .catch(function () { return null; });
    }

    function ensurePredictState() {
        if (!window.__ratingPredictState) window.__ratingPredictState = {};
        return window.__ratingPredictState;
    }

    if (!window.ratingContext || !window.ratingContext.__owned) {
        window.ratingContext = {
            __owned: true,

            getCurrent: function (movieId) {
                if (typeof isAuthenticated === 'function' && !isAuthenticated()) {
                    return Promise.resolve(null);
                }
                return Promise.all([
                    fetchOwn(movieId),
                    fetchAvg(movieId),
                    fetchCount(movieId)
                ]).then(function (res) {
                    var dto = res[0];
                    var avgRating = res[1];
                    var totalReviews = res[2];

                    var own = (dto && dto.rating != null)
                        ? { rating: Number(dto.rating), review: dto.review || '' }
                        : null;

                    ensurePredictState()[movieId] = {
                        own: own ? { rating: Number(own.rating) } : null,
                        avgRating: avgRating,
                        totalReviews: totalReviews
                    };

                    return {
                        own: own,
                        avgRating: avgRating,
                        totalReviews: totalReviews
                    };
                });
            },

            onOptimistic: function (movieId, review) {
                if (!review || review.ratingValue == null) return;
                var state = ensurePredictState()[movieId];
                if (!state || state.avgRating == null || state.totalReviews == null) return;

                var mine = Number(review.ratingValue);
                var oldAvg = state.avgRating;
                var oldCount = state.totalReviews;
                var myOld = state.own ? Number(state.own.rating) : null;

                var predictedAvg;
                var predictedCount;

                if (myOld == null) {
                    predictedCount = oldCount + 1;
                    predictedAvg = ((oldAvg * oldCount) + mine) / predictedCount;
                } else {
                    predictedCount = oldCount;
                    predictedAvg = ((oldAvg * oldCount) - myOld + mine) / predictedCount;
                }

                state.avgRating = predictedAvg;
                state.totalReviews = predictedCount;
                state.own = { rating: mine };

                if (typeof updateRatingBadge === 'function') {
                    updateRatingBadge(movieId, predictedAvg.toFixed(1));
                }
                if (typeof applyReviewsCount === 'function') {
                    applyReviewsCount(movieId, predictedCount);
                }
            },

            onRollback: function (movieId) {
                delete ensurePredictState()[movieId];
                if (typeof updateMovieRating === 'function') updateMovieRating(movieId);
                if (typeof updateReviewsCount === 'function') updateReviewsCount(movieId);
            },

            onOptimisticDelete: function (movieId) {
                var state = ensurePredictState()[movieId];
                if (!state || state.avgRating == null || state.totalReviews == null || !state.own) {
                    if (typeof updateRatingBadge === 'function') updateRatingBadge(movieId, null);
                    return;
                }

                var myOld = Number(state.own.rating);
                var newCount = state.totalReviews - 1;
                var predictedAvg = newCount > 0
                    ? ((state.avgRating * state.totalReviews) - myOld) / newCount
                    : null;

                state.avgRating = predictedAvg;
                state.totalReviews = newCount;
                state.own = null;

                if (typeof updateRatingBadge === 'function') {
                    updateRatingBadge(movieId, predictedAvg != null ? predictedAvg.toFixed(1) : null);
                }
                if (typeof applyReviewsCount === 'function') {
                    applyReviewsCount(movieId, newCount);
                }
            },

            onRollbackDelete: function (movieId) {
                delete ensurePredictState()[movieId];
                if (typeof updateMovieRating === 'function') updateMovieRating(movieId);
                if (typeof updateReviewsCount === 'function') updateReviewsCount(movieId);
            },

            onCountUpdate: function (movieId, total) {
                if (typeof applyReviewsCount === 'function') {
                    applyReviewsCount(movieId, total);
                }
            }
        };
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerActions);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerActions, { once: true });
    } else {
        registerActions();
    }
})();