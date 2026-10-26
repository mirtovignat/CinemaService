(function () {
    'use strict';

    var reviewsBody = document.getElementById('reviewsBody');
    var reviewsList = document.getElementById('reviewsList');
    var reviewsSpinner = document.getElementById('reviewsSpinnerWrapper');
    var reviewsCountElement = document.getElementById('reviewsCount');
    var loadMoreWrapper = document.getElementById('loadMoreWrapper');

    if (!reviewsBody || !reviewsList) return;

    var movieId = (loadMoreWrapper && loadMoreWrapper.dataset.movieId)
        ? loadMoreWrapper.dataset.movieId
        : reviewsBody.dataset.movieId;

    function normalizeRating(ratingValue) {
        if (ratingValue == null) return null;
        var parsedRating = parseFloat(String(ratingValue).replace(',', '.'));
        if (isNaN(parsedRating)) return null;
        parsedRating = Math.round(parsedRating * 10) / 10;
        if (parsedRating < 0.1) parsedRating = 0.1;
        if (parsedRating > 10) parsedRating = 10;
        return parsedRating;
    }

    function formatRating(ratingValue) {
        var normalizedRating = normalizeRating(ratingValue);
        if (normalizedRating == null) return '—';
        return normalizedRating.toFixed(1);
    }

    function getReviewsCount() {
        if (!reviewsCountElement) return null;
        var match = reviewsCountElement.textContent.match(/\d+/);
        return match ? parseInt(match[0], 10) : null;
    }

    function setReviewsCountText(totalReviews) {
        if (!reviewsCountElement || totalReviews == null) return;
        var safeTotalReviews = Math.max(0, Number(totalReviews) || 0);
        reviewsCountElement.textContent = safeTotalReviews + ' ' + window.getDeclension(safeTotalReviews);
    }

    function bumpReviewsCount(delta) {
        var currentReviewsCount = getReviewsCount();
        if (currentReviewsCount == null) return;
        setReviewsCountText(Math.max(0, currentReviewsCount + delta));
    }

    function ensureEndMarkerRecheck() {
        var reviewCards = reviewsList.querySelectorAll('.review-card');
        var endMarker = reviewsList.parentNode && reviewsList.parentNode.querySelector('.reviews-end');
        if (reviewCards.length > 0 && endMarker) endMarker.remove();
    }

    function bindOwnCardActions(reviewCard, movieTitle) {
        if (!reviewCard) return;

        var editButton = reviewCard.querySelector('.btn-edit');
        if (editButton) {
            if (movieTitle != null) editButton.dataset.movieTitle = movieTitle;
            if (movieId != null && !editButton.dataset.movieId) {
                editButton.dataset.movieId = String(movieId);
            }
            editButton.onclick = function () {
                if (typeof window.showRatingModal !== 'function') return;
                var rawRating = (this.dataset.reviewRating || '').replace(',', '.');
                var parsedRating = parseFloat(rawRating);
                if (isNaN(parsedRating)) parsedRating = 5.0;
                parsedRating = Math.round(parsedRating * 10) / 10;
                if (parsedRating < 0.1) parsedRating = 0.1;
                if (parsedRating > 10) parsedRating = 10;
                window.showRatingModal(
                    this.dataset.movieId,
                    this.dataset.movieTitle || '',
                    'edit',
                    parsedRating.toFixed(1),
                    this.dataset.reviewText || ''
                );
            };
        }

        var deleteButton = reviewCard.querySelector('.btn-delete');
        if (deleteButton) {
            if (movieId != null && !deleteButton.dataset.movieId) {
                deleteButton.dataset.movieId = String(movieId);
            }
            deleteButton.onclick = function () {
                if (typeof window.deleteRating !== 'function') return;
                window.deleteRating(this.dataset.movieId);
            };
        }
    }

    function fillCardFromReview(reviewCard, review, isNew, isOwn) {
        var username = review.username || 'Пользователь';
        var isDeleted = review.deleted === true;
        var avatarLetter = username.trim().charAt(0).toUpperCase() || 'U';
        var ratingText = formatRating(review.ratingValue);
        var dateText = isNew
            ? 'Только что'
            : (review.addedAt ? window.formatDateTime(review.addedAt) : '');

        var avatarElement = reviewCard.querySelector('.avatar');
        if (avatarElement) {
            if (isDeleted) {
                avatarElement.classList.add('avatar--deleted');
                avatarElement.innerHTML = '<span class="avatar-svg"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:22px;height:22px;"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7" r="4"></circle><line x1="17" y1="11" x2="23" y2="11"></line></svg></span>';
            } else {
                avatarElement.classList.remove('avatar--deleted');
                avatarElement.textContent = avatarLetter;
            }
        }

        var usernameElement = reviewCard.querySelector('.review-username');
        if (usernameElement) {
            var usernameText = usernameElement.querySelector('.username-text');
            var deletedBadge = usernameElement.querySelector('.deleted-badge');

            if (usernameText) {
                usernameText.textContent = username;
            } else {
                usernameElement.textContent = username;
            }

            if (deletedBadge) {
                deletedBadge.style.display = isDeleted ? '' : 'none';
            }
        }

        var ratingElement = reviewCard.querySelector('.review-rating-value');
        if (ratingElement) ratingElement.textContent = ratingText;

        var dateElement = reviewCard.querySelector('.review-date');
        if (dateElement) {
            dateElement.textContent = dateText;
            dateElement.style.display = dateText ? '' : 'none';
        }

        var textElement = reviewCard.querySelector('.review-text, .is-muted');
        if (textElement) {
            if (review.reviewText && review.reviewText.trim() !== '') {
                textElement.textContent = review.reviewText;
                textElement.style.display = '';
                textElement.className = 'review-text';
            } else {
                textElement.textContent = isOwn
                    ? '— Вы не оставили текстового отзыва —'
                    : '— Пользователь не оставил текстового отзыва —';
                textElement.style.display = '';
                textElement.className = 'is-muted';
            }
        }

        if (isOwn) {
            var editButton = reviewCard.querySelector('.btn-edit');
            if (editButton) {
                editButton.dataset.movieId = String(movieId);
                editButton.dataset.reviewRating = ratingText;
                editButton.dataset.reviewText = review.reviewText || '';
            }
            var deleteButton = reviewCard.querySelector('.btn-delete');
            if (deleteButton) {
                deleteButton.dataset.movieId = String(movieId);
            }
        }
    }

    function cloneTemplate(templateId) {
        var template = document.getElementById(templateId);
        if (!template) return null;
        return template.content.firstElementChild.cloneNode(true);
    }

    function buildCard(review, isOwn, isNew) {
        var templateId = isOwn ? 'ownReviewCardTemplate' : 'reviewCardTemplate';
        var reviewCard = cloneTemplate(templateId);
        if (!reviewCard) return null;

        reviewCard.dataset.movieId = movieId;
        if (isNew) reviewCard.dataset.justAdded = 'true';

        fillCardFromReview(reviewCard, review, isNew, isOwn);

        if (isOwn) {
            bindOwnCardActions(reviewCard, review.movieTitle || '');
        }

        return reviewCard;
    }

    function updateCardFields(reviewCard, review) {
        if (!reviewCard || !review) return;

        var ratingElement = reviewCard.querySelector('.review-rating-value');
        if (ratingElement && review.ratingValue != null) {
            ratingElement.textContent = formatRating(review.ratingValue);
        }

        var textElement = reviewCard.querySelector('.review-text, .is-muted');
        if (textElement && review.reviewText != null) {
            if (review.reviewText === '') {
                textElement.textContent = '— Вы не оставили текстового отзыва —';
                textElement.style.display = '';
                textElement.className = 'is-muted';
            } else {
                textElement.textContent = review.reviewText;
                textElement.style.display = '';
                textElement.className = 'review-text';
            }
        }

        var editButton = reviewCard.querySelector('.btn-edit');
        if (editButton) {
            if (review.ratingValue != null) {
                editButton.dataset.reviewRating = formatRating(review.ratingValue);
            }
            if (review.reviewText != null) {
                editButton.dataset.reviewText = review.reviewText;
            }
        }

        var dateElement = reviewCard.querySelector('.review-date');
        if (dateElement) {
            dateElement.textContent = 'Только что';
            dateElement.style.display = '';
        }
    }

    function setTopAddReviewButtonVisible(isVisible) {
        var topAddReviewButton = document.getElementById('topAddReviewBtn');
        if (topAddReviewButton) topAddReviewButton.style.display = isVisible ? '' : 'none';
    }

    function setNoReviewsBlockVisible(isVisible) {
        var noReviewsBlock = document.getElementById('noReviewsBlock');
        if (noReviewsBlock) noReviewsBlock.style.display = isVisible ? '' : 'none';
    }

    function refreshEmptyState() {
        var ownReviewCard = reviewsList.querySelector('.review-card[data-own="true"]');
        var otherReviewCards = reviewsList.querySelectorAll('.review-card:not([data-own="true"])');
        var hasAnyReview = !!ownReviewCard || otherReviewCards.length > 0;
        setNoReviewsBlockVisible(!hasAnyReview);
    }

    function initTopAddReviewButton() {
        var topAddReviewButton = document.getElementById('topAddReviewBtn');
        if (!topAddReviewButton) return;
        if (topAddReviewButton.dataset.bound === '1') return;
        topAddReviewButton.dataset.bound = '1';
        topAddReviewButton.addEventListener('click', function () {
            if (typeof window.showRatingModal !== 'function') {
                console.warn('[reviews] showRatingModal не готов');
                return;
            }
            window.showRatingModal(
                this.dataset.movieId,
                this.dataset.movieTitle || '',
                'add',
                5.0,
                ''
            );
        });
    }

    function formatReviewDates() {
        document.querySelectorAll('.js-review-date[data-date]').forEach(function (dateElement) {
            var rawDate = dateElement.getAttribute('data-date');
            if (rawDate && window.formatDateTime) {
                dateElement.textContent = window.formatDateTime(rawDate);
            }
        });
    }

    window.reviewsUI = {
        upsertOwn(targetMovieId, review, isNew, movieTitle) {
            if (String(targetMovieId) !== String(movieId)) return;
            if (!review) return;

            var existingOwnCard = this.findOwnCard(targetMovieId);

            if (existingOwnCard) {
                updateCardFields(existingOwnCard, review);
                if (movieTitle) {
                    var editButton = existingOwnCard.querySelector('.btn-edit');
                    if (editButton) editButton.dataset.movieTitle = movieTitle;
                }
            } else {
                var ownReviewCard = buildCard(
                    Object.assign({}, review, {
                        movieTitle: movieTitle || window._ratingModalMovieTitle || ''
                    }),
                    true,
                    !!isNew
                );
                if (!ownReviewCard) return;

                ownReviewCard.style.opacity = '0';
                ownReviewCard.style.transform = 'translateY(-12px) scale(0.98)';
                ownReviewCard.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

                reviewsList.insertBefore(ownReviewCard, reviewsList.firstChild);

                requestAnimationFrame(function () {
                    requestAnimationFrame(function () {
                        ownReviewCard.style.opacity = '1';
                        ownReviewCard.style.transform = 'translateY(0) scale(1)';
                    });
                });

                setTimeout(function () {
                    ownReviewCard.style.transition = '';
                    ownReviewCard.style.opacity = '';
                    ownReviewCard.style.transform = '';
                }, 350);

                bumpReviewsCount(1);
            }

            setTopAddReviewButtonVisible(false);
            setNoReviewsBlockVisible(false);
            ensureEndMarkerRecheck();
            formatReviewDates();
        },

        removeOwn(targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            var ownReviewCard = this.findOwnCard(targetMovieId);
            if (ownReviewCard) {
                ownReviewCard.style.transition = 'opacity 0.28s ease, transform 0.28s ease';
                ownReviewCard.style.opacity = '0';
                ownReviewCard.style.transform = 'scale(0.94)';
                setTimeout(function () {
                    ownReviewCard.remove();
                    refreshEmptyState();
                    ensureEndMarkerRecheck();
                }, 300);
                bumpReviewsCount(-1);
            } else {
                refreshEmptyState();
                ensureEndMarkerRecheck();
            }

            setTopAddReviewButtonVisible(true);
        },

        setCount(targetMovieId, totalReviews) {
            if (String(targetMovieId) !== String(movieId)) return;
            setReviewsCountText(totalReviews);
        },

        findOwnCard(targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return null;
            return reviewsList.querySelector('.review-card[data-own="true"]');
        },

        getCount() {
            return getReviewsCount();
        },

        bindOwnCardActions(reviewCard, movieTitle) {
            bindOwnCardActions(reviewCard, movieTitle);
        },

        formatDates() {
            formatReviewDates();
        }
    };

    (function installReviewsRatingContext() {
        var ratingContext = window.ratingContext || {};
        ratingContext.__owned = true;
        ratingContext.__fromReviews = true;

        ratingContext.getCurrent = function (targetMovieId) {
            var ownReview = null;
            if (String(targetMovieId) === String(movieId)) {
                var ownReviewCard = window.reviewsUI.findOwnCard(targetMovieId);
                if (ownReviewCard) {
                    var ratingElement = ownReviewCard.querySelector('.review-rating-value');
                    var textElement = ownReviewCard.querySelector('.review-text');
                    var parsedRating = ratingElement
                        ? parseFloat((ratingElement.textContent || '').replace(',', '.'))
                        : NaN;
                    ownReview = {
                        rating: isNaN(parsedRating) ? null : parsedRating,
                        review: textElement ? textElement.textContent.trim() : ''
                    };
                }
            }

            return Promise.all([
                fetch('/rated/rating/' + encodeURIComponent(targetMovieId) + '?_=' + Date.now(), {
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
                    redirect: 'manual',
                    credentials: 'same-origin'
                })
                .then(function (response) {
                    if (response.type === 'opaqueredirect' || response.status === 0) return null;
                    if (response.redirected) return null;
                    return response.ok ? response.json() : null;
                })
                .catch(function () { return null; }),

                fetch('/rated/count/' + encodeURIComponent(targetMovieId), {
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
                    redirect: 'manual',
                    credentials: 'same-origin'
                })
                .then(function (response) {
                    if (response.type === 'opaqueredirect' || response.status === 0) return null;
                    if (response.redirected) return null;
                    return response.ok ? response.text() : null;
                })
                .catch(function () { return null; })
            ]).then(function (results) {
                var averageRatingDto = results[0];
                var averageRating = null;
                if (averageRatingDto && averageRatingDto.rating != null && averageRatingDto.rating !== '-' && averageRatingDto.rating !== '—') {
                    var parsedAverageRating = parseFloat(averageRatingDto.rating);
                    if (!isNaN(parsedAverageRating)) averageRating = Math.round(parsedAverageRating * 10) / 10;
                }

                var totalReviews = null;
                if (results[1] != null) {
                    var parsedTotalReviews = parseInt(results[1], 10);
                    if (!isNaN(parsedTotalReviews)) totalReviews = parsedTotalReviews;
                }

                if (!window.__ratingPredictState) window.__ratingPredictState = {};
                window.__ratingPredictState[String(targetMovieId)] = {
                    own: ownReview ? { rating: Number(ownReview.rating) } : null,
                    avgRating: averageRating,
                    totalReviews: totalReviews
                };

                return { own: ownReview, avgRating: averageRating, totalReviews: totalReviews };
            });
        };

        ratingContext.onOptimistic = function (targetMovieId, review, isNew) {
            if (String(targetMovieId) !== String(movieId)) return;

            window.reviewsUI.upsertOwn(targetMovieId, review, isNew, review.movieTitle || '');

            if (!review || review.ratingValue == null) return;
            var predictState = (window.__ratingPredictState || {})[String(targetMovieId)];
            if (!predictState || predictState.avgRating == null || predictState.totalReviews == null) return;

            var newRating = Number(review.ratingValue);
            var oldAverageRating = predictState.avgRating;
            var oldTotalReviews = predictState.totalReviews;
            var oldOwnRating = predictState.own ? Number(predictState.own.rating) : null;

            var predictedAverageRating;
            var predictedTotalReviews;

            if (oldOwnRating == null) {
                predictedTotalReviews = oldTotalReviews + 1;
                predictedAverageRating = ((oldAverageRating * oldTotalReviews) + newRating) / predictedTotalReviews;
            } else {
                predictedTotalReviews = oldTotalReviews;
                predictedAverageRating = ((oldAverageRating * oldTotalReviews) - oldOwnRating + newRating) / predictedTotalReviews;
            }

            predictState.avgRating = predictedAverageRating;
            predictState.totalReviews = predictedTotalReviews;
            predictState.own = { rating: newRating };

            if (typeof window.updateRatingBadge === 'function') {
                window.updateRatingBadge(targetMovieId, predictedAverageRating.toFixed(1));
            }
            if (typeof window.applyReviewsCount === 'function') {
                window.applyReviewsCount(targetMovieId, predictedTotalReviews);
            }
        };

        ratingContext.onRollback = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            delete (window.__ratingPredictState || {})[String(targetMovieId)];
            if (typeof window.updateMovieRating === 'function') window.updateMovieRating(targetMovieId);
            if (typeof window.updateReviewsCount === 'function') window.updateReviewsCount(targetMovieId);
        };

        ratingContext.onOptimisticDelete = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;

            window.reviewsUI.removeOwn(targetMovieId);

            var predictState = (window.__ratingPredictState || {})[String(targetMovieId)];
            if (!predictState || predictState.avgRating == null || predictState.totalReviews == null || !predictState.own) {
                if (typeof window.updateRatingBadge === 'function') {
                    window.updateRatingBadge(targetMovieId, null);
                }
                return;
            }

            var oldOwnRating = Number(predictState.own.rating);
            var newTotalReviews = predictState.totalReviews - 1;
            var predictedAverageRating = newTotalReviews > 0
                ? ((predictState.avgRating * predictState.totalReviews) - oldOwnRating) / newTotalReviews
                : null;

            predictState.avgRating = predictedAverageRating;
            predictState.totalReviews = newTotalReviews;
            predictState.own = null;

            if (typeof window.updateRatingBadge === 'function') {
                window.updateRatingBadge(
                    targetMovieId,
                    predictedAverageRating != null ? predictedAverageRating.toFixed(1) : null
                );
            }
            if (typeof window.applyReviewsCount === 'function') {
                window.applyReviewsCount(targetMovieId, newTotalReviews);
            }
        };

        ratingContext.onRollbackDelete = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            delete (window.__ratingPredictState || {})[String(targetMovieId)];
            if (typeof window.updateMovieRating === 'function') window.updateMovieRating(targetMovieId);
            if (typeof window.updateReviewsCount === 'function') window.updateReviewsCount(targetMovieId);
        };

        ratingContext.onCountUpdate = function (targetMovieId, totalReviews) {
            if (String(targetMovieId) !== String(movieId)) return;
            window.reviewsUI.setCount(targetMovieId, totalReviews);
        };

        window.ratingContext = ratingContext;
    })();

    function bindExistingCards() {
        var ownReviewCard = reviewsList.querySelector('.review-card[data-own="true"]');
        if (ownReviewCard) bindOwnCardActions(ownReviewCard);
    }

    bindExistingCards();
    initTopAddReviewButton();
    formatReviewDates();

    function appendEndMarker() {
        if (reviewsList.parentNode && reviewsList.parentNode.querySelector('.reviews-end')) return;
        var endMarker = document.createElement('div');
        endMarker.className = 'reviews-end';
        endMarker.textContent = '— Это все отзывы —';
        reviewsList.insertAdjacentElement('afterend', endMarker);
    }

    if (!loadMoreWrapper || loadMoreWrapper.dataset.hasMore !== 'true') {
        if (loadMoreWrapper) loadMoreWrapper.remove();
        appendEndMarker();
        return;
    }

    var currentSort = loadMoreWrapper.dataset.sort || 'DESC';
    var currentCursor = loadMoreWrapper.dataset.cursor || null;
    var hasMore = true;
    var isLoading = false;

    var MAX_RETRIES = 3;
    var RETRY_BASE_DELAY_MS = 1000;
    var MAX_BACKOFF_MS = 30000;
    var FETCH_TIMEOUT_MS = 10000;

    var retryCount = 0;
    var disabledUntil = 0;
    var retryTimer = null;

    reviewsBody.addEventListener('scroll', function() {
        if (isLoading || !hasMore) return;

        if (reviewsBody.scrollHeight - reviewsBody.scrollTop - reviewsBody.clientHeight < 300) {
            requestLoad();
        }
    }, { passive: true });

    maybeFillOnce();

    function maybeFillOnce() {
        if (!hasMore) return;
        if (reviewsBody.scrollHeight > reviewsBody.clientHeight + 20) return;
        requestLoad();
    }

    function requestLoad() {
        if (isLoading || !hasMore) return;
        if (Date.now() < disabledUntil) {
            if (!retryTimer) {
                retryTimer = setTimeout(function () {
                    retryTimer = null;
                    requestLoad();
                }, disabledUntil - Date.now());
            }
            return;
        }
        loadNext();
    }

    function disconnectObserver() {
        if (retryTimer) {
            clearTimeout(retryTimer);
            retryTimer = null;
        }
    }

    async function loadNext() {
        if (isLoading || !hasMore) return;

        isLoading = true;
        loadMoreWrapper.classList.add('is-loading');
        if (reviewsSpinner) reviewsSpinner.style.display = 'flex';

        var abortController = new AbortController();
        var timeoutId = setTimeout(() => abortController.abort(), FETCH_TIMEOUT_MS);

        try {
            var params = new URLSearchParams();
            if (currentCursor) params.set('cursor', currentCursor);
            params.set('sortDirection', currentSort);

            var response = await fetch(`/rated/${movieId}/reviews/more?${params}`, {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'application/json'
                },
                redirect: 'manual',
                credentials: 'same-origin',
                signal: abortController.signal
            });

            if (response.type === 'opaqueredirect' || response.status === 0) {
                window.location.href = '/login';
                return;
            }
            if (response.redirected) {
                window.location.href = response.url;
                return;
            }
            if (!response.ok) throw new Error('HTTP ' + response.status);

            var responseData = await response.json();

            retryCount = 0;
            disabledUntil = 0;

            var reviews = Array.isArray(responseData.items) ? responseData.items : [];

            if (reviews.length > 0) {
                var fragment = document.createDocumentFragment();
                reviews.forEach(review => {
                    var reviewCard = buildCard(review, false, false);
                    if (reviewCard) fragment.appendChild(reviewCard);
                });
                reviewsList.appendChild(fragment);
                formatReviewDates();
            } else {
                hasMore = false;
            }

            currentCursor = responseData.nextCursor || null;
            if (responseData.hasMore === false) hasMore = false;

            if (!hasMore) {
                disconnectObserver();
                loadMoreWrapper.remove();
                appendEndMarker();
            } else {
                setTimeout(maybeFillOnce, 100);
            }
        } catch (error) {
            var isAbort = error && error.name === 'AbortError';
            console.warn('[reviews] Ошибка загрузки:', isAbort ? 'timeout' : error);

            retryCount++;

            if (retryCount >= MAX_RETRIES) {
                var delay = Math.min(
                    RETRY_BASE_DELAY_MS * Math.pow(2, retryCount),
                    MAX_BACKOFF_MS
                );
                disabledUntil = Date.now() + delay;
                retryCount = 0;
            } else {
                var retryDelay = RETRY_BASE_DELAY_MS * Math.pow(2, retryCount - 1);
                disabledUntil = Date.now() + retryDelay;
            }
        } finally {
            clearTimeout(timeoutId);
            isLoading = false;
            loadMoreWrapper.classList.remove('is-loading');
            if (reviewsSpinner) reviewsSpinner.style.display = 'none';
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', formatReviewDates);
    } else {
        formatReviewDates();
    }
})();