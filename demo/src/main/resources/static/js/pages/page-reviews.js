(function () {
    'use strict';

    const body = document.getElementById('reviewsBody');
    const list = document.getElementById('reviewsList');
    const spinner = document.getElementById('reviewsSpinnerWrapper');
    const countEl = document.getElementById('reviewsCount');
    const wrapper = document.getElementById('loadMoreWrapper');

    if (!body || !list) return;

    const movieId = (wrapper && wrapper.dataset.movieId)
        ? wrapper.dataset.movieId
        : body.dataset.movieId;

    const SORT_STORAGE_KEY = 'cinemaReviewsSortOrder';

    function normalizeRating(value) {
        if (value == null) return null;
        var num = parseFloat(String(value).replace(',', '.'));
        if (isNaN(num)) return null;
        num = Math.round(num * 10) / 10;
        if (num < 0.1) num = 0.1;
        if (num > 10) num = 10;
        return num;
    }

    function formatRating(value) {
        var num = normalizeRating(value);
        if (num == null) return '—';
        return num.toFixed(1);
    }

    (function ensureSortFromStorage() {
        var params = new URLSearchParams(window.location.search);
        if (params.has('sort')) {
            var current = params.get('sort');
            if (current === 'ASC' || current === 'DESC') {
                try { localStorage.setItem(SORT_STORAGE_KEY, current); } catch (e) {}
            }
            return;
        }
        var saved = null;
        try { saved = localStorage.getItem(SORT_STORAGE_KEY); } catch (e) {}
        if (saved === 'ASC' || saved === 'DESC') {
            var url = new URL(window.location.href);
            url.searchParams.set('sort', saved);
            url.searchParams.delete('cursor');
            window.location.replace(url.toString());
        }
    })();

    function getCount() {
        if (!countEl) return null;
        const m = countEl.textContent.match(/\d+/);
        return m ? parseInt(m[0], 10) : null;
    }

    function setCountText(total) {
        if (!countEl || total == null) return;
        const n = Math.max(0, Number(total) || 0);
        countEl.textContent = n + ' ' + window.getDeclension(n);
    }

    function bumpCount(delta) {
        const cur = getCount();
        if (cur == null) return;
        setCountText(Math.max(0, cur + delta));
    }

    function ensureEndMarkerRecheck() {
        const cards = list.querySelectorAll('.review-card');
        const end = list.parentNode && list.parentNode.querySelector('.reviews-end');
        if (cards.length > 0 && end) end.remove();
    }

    function bindOwnCardActions(card, movieTitle) {
        if (!card) return;

        const editBtn = card.querySelector('.btn-edit');
        if (editBtn) {
            if (movieTitle != null) editBtn.dataset.movieTitle = movieTitle;
            if (movieId != null && !editBtn.dataset.movieId) {
                editBtn.dataset.movieId = String(movieId);
            }
            editBtn.onclick = function () {
                if (typeof window.showRatingModal !== 'function') return;
                var raw = (this.dataset.reviewRating || '').replace(',', '.');
                var parsed = parseFloat(raw);
                if (isNaN(parsed)) parsed = 5.0;
                parsed = Math.round(parsed * 10) / 10;
                if (parsed < 0.1) parsed = 0.1;
                if (parsed > 10) parsed = 10;
                window.showRatingModal(
                    this.dataset.movieId,
                    this.dataset.movieTitle || '',
                    'edit',
                    parsed.toFixed(1),
                    this.dataset.reviewText || ''
                );
            };
        }

        const deleteBtn = card.querySelector('.btn-delete');
        if (deleteBtn) {
            if (movieId != null && !deleteBtn.dataset.movieId) {
                deleteBtn.dataset.movieId = String(movieId);
            }
            deleteBtn.onclick = function () {
                if (typeof window.deleteRating !== 'function') return;
                window.deleteRating(this.dataset.movieId);
            };
        }
    }

    function fillCardFromReview(card, review, isNew, isOwn) {
        const username = review.username || 'Пользователь';
        const letter = username.trim().charAt(0).toUpperCase() || 'U';
        const ratingText = formatRating(review.ratingValue);
        const dateText = isNew
            ? 'Только что'
            : (review.addedAt ? window.formatDateTime(review.addedAt) : '');

        const avatarEl = card.querySelector('.avatar');
        if (avatarEl) avatarEl.textContent = letter;

        const usernameEl = card.querySelector('.review-username');
        if (usernameEl) usernameEl.textContent = username;

        const ratingEl = card.querySelector('.review-rating-value');
        if (ratingEl) ratingEl.textContent = ratingText;

        const dateEl = card.querySelector('.review-date');
        if (dateEl) {
            dateEl.textContent = dateText;
            dateEl.style.display = dateText ? '' : 'none';
        }

        const textEl = card.querySelector('.review-text');
        if (textEl) {
            if (review.reviewText) {
                textEl.textContent = review.reviewText;
                textEl.style.display = '';
            } else {
                textEl.textContent = '';
                textEl.style.display = 'none';
            }
        }

        if (isOwn) {
            const editBtn = card.querySelector('.btn-edit');
            if (editBtn) {
                editBtn.dataset.movieId = String(movieId);
                editBtn.dataset.reviewRating = ratingText;
                editBtn.dataset.reviewText = review.reviewText || '';
            }
            const deleteBtn = card.querySelector('.btn-delete');
            if (deleteBtn) {
                deleteBtn.dataset.movieId = String(movieId);
            }
        }
    }

    function cloneTemplate(templateId) {
        const tpl = document.getElementById(templateId);
        if (!tpl) return null;
        return tpl.content.firstElementChild.cloneNode(true);
    }

    function buildCard(review, isOwn, isNew) {
        const templateId = isOwn ? 'ownReviewCardTemplate' : 'reviewCardTemplate';
        const card = cloneTemplate(templateId);
        if (!card) return null;

        card.dataset.movieId = movieId;
        if (isNew) card.dataset.justAdded = 'true';

        fillCardFromReview(card, review, isNew, isOwn);

        if (isOwn) {
            bindOwnCardActions(card, review.movieTitle || '');
        }

        return card;
    }

    function updateCardFields(card, review) {
        if (!card || !review) return;

        const ratingEl = card.querySelector('.review-rating-value');
        if (ratingEl && review.ratingValue != null) {
            ratingEl.textContent = formatRating(review.ratingValue);
        }

        const textEl = card.querySelector('.review-text');
        if (textEl && review.reviewText != null) {
            if (review.reviewText === '') {
                textEl.textContent = '';
                textEl.style.display = 'none';
            } else {
                textEl.textContent = review.reviewText;
                textEl.style.display = '';
            }
        }

        const editBtn = card.querySelector('.btn-edit');
        if (editBtn) {
            if (review.ratingValue != null) {
                editBtn.dataset.reviewRating = formatRating(review.ratingValue);
            }
            if (review.reviewText != null) {
                editBtn.dataset.reviewText = review.reviewText;
            }
        }

        const dateEl = card.querySelector('.review-date');
        if (dateEl) {
            dateEl.textContent = 'Только что';
            dateEl.style.display = '';
        }
    }

    function setTopAddBtnVisible(visible) {
        const btn = document.getElementById('topAddReviewBtn');
        if (btn) btn.style.display = visible ? '' : 'none';
    }

    function setNoReviewsBlockVisible(visible) {
        const block = document.getElementById('noReviewsBlock');
        if (block) block.style.display = visible ? '' : 'none';
    }

    function refreshEmptyState() {
        const own = list.querySelector('.review-card[data-own="true"]');
        const others = list.querySelectorAll('.review-card:not([data-own="true"])');
        const hasAny = !!own || others.length > 0;
        setNoReviewsBlockVisible(!hasAny);
    }

    function initTopAddBtn() {
        const btn = document.getElementById('topAddReviewBtn');
        if (!btn) return;
        if (btn.dataset.bound === '1') return;
        btn.dataset.bound = '1';
        btn.addEventListener('click', function () {
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

    window.reviewsUI = {
        upsertOwn(targetMovieId, review, isNew, movieTitle) {
            if (String(targetMovieId) !== String(movieId)) return;
            if (!review) return;

            const existing = this.findOwnCard(targetMovieId);

            if (existing) {
                updateCardFields(existing, review);
                if (movieTitle) {
                    const editBtn = existing.querySelector('.btn-edit');
                    if (editBtn) editBtn.dataset.movieTitle = movieTitle;
                }
            } else {
                const card = buildCard(
                    Object.assign({}, review, {
                        movieTitle: movieTitle || window._ratingModalMovieTitle || ''
                    }),
                    true,
                    !!isNew
                );
                if (!card) return;

                card.style.opacity = '0';
                card.style.transform = 'translateY(-12px) scale(0.98)';
                card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

                list.insertBefore(card, list.firstChild);

                requestAnimationFrame(function () {
                    requestAnimationFrame(function () {
                        card.style.opacity = '1';
                        card.style.transform = 'translateY(0) scale(1)';
                    });
                });

                setTimeout(function () {
                    card.style.transition = '';
                    card.style.opacity = '';
                    card.style.transform = '';
                }, 350);

                bumpCount(1);
            }

            setTopAddBtnVisible(false);
            setNoReviewsBlockVisible(false);
            ensureEndMarkerRecheck();
        },

        removeOwn(targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            const card = this.findOwnCard(targetMovieId);
            if (card) {
                card.style.transition = 'opacity 0.28s ease, transform 0.28s ease';
                card.style.opacity = '0';
                card.style.transform = 'scale(0.94)';
                setTimeout(function () {
                    card.remove();
                    refreshEmptyState();
                    ensureEndMarkerRecheck();
                }, 300);
                bumpCount(-1);
            } else {
                refreshEmptyState();
                ensureEndMarkerRecheck();
            }

            setTopAddBtnVisible(true);
        },

        setCount(targetMovieId, total) {
            if (String(targetMovieId) !== String(movieId)) return;
            setCountText(total);
        },

        findOwnCard(targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return null;
            return list.querySelector('.review-card[data-own="true"]');
        },

        getCount() {
            return getCount();
        },

        bindOwnCardActions(card, movieTitle) {
            bindOwnCardActions(card, movieTitle);
        }
    };

    (function installReviewsRatingContext() {
        var base = window.ratingContext || {};
        base.__owned = true;
        base.__fromReviews = true;

        base.getCurrent = function (targetMovieId) {
            var own = null;
            if (String(targetMovieId) === String(movieId)) {
                var ownCard = window.reviewsUI.findOwnCard(targetMovieId);
                if (ownCard) {
                    var ratingEl = ownCard.querySelector('.review-rating-value');
                    var textEl = ownCard.querySelector('.review-text');
                    var parsed = ratingEl ? parseFloat((ratingEl.textContent || '').replace(',', '.')) : NaN;
                    own = {
                        rating: isNaN(parsed) ? null : parsed,
                        review: textEl ? textEl.textContent.trim() : ''
                    };
                }
            }

            return Promise.all([
                fetch('/rated/rating/' + encodeURIComponent(targetMovieId) + '?_=' + Date.now(), {
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
                })
                .then(function (r) { return r.ok ? r.json() : null; })
                .catch(function () { return null; }),

                fetch('/rated/count/' + encodeURIComponent(targetMovieId), {
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
                })
                .then(function (r) { return r.ok ? r.text() : null; })
                .catch(function () { return null; })
            ]).then(function (res) {
                var avgDto = res[0];
                var avgRating = null;
                if (avgDto && avgDto.rating != null && avgDto.rating !== '-' && avgDto.rating !== '—') {
                    var n = parseFloat(avgDto.rating);
                    if (!isNaN(n)) avgRating = Math.round(n * 10) / 10;
                }

                var totalReviews = null;
                if (res[1] != null) {
                    var c = parseInt(res[1], 10);
                    if (!isNaN(c)) totalReviews = c;
                }

                if (!window.__ratingPredictState) window.__ratingPredictState = {};
                window.__ratingPredictState[String(targetMovieId)] = {
                    own: own ? { rating: Number(own.rating) } : null,
                    avgRating: avgRating,
                    totalReviews: totalReviews
                };

                return { own: own, avgRating: avgRating, totalReviews: totalReviews };
            });
        };

        base.onOptimistic = function (targetMovieId, review, isNew) {
            if (String(targetMovieId) !== String(movieId)) return;

            window.reviewsUI.upsertOwn(targetMovieId, review, isNew, review.movieTitle || '');

            if (!review || review.ratingValue == null) return;
            var state = (window.__ratingPredictState || {})[String(targetMovieId)];
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

            if (typeof window.updateRatingBadge === 'function') {
                window.updateRatingBadge(targetMovieId, predictedAvg.toFixed(1));
            }
            if (typeof window.applyReviewsCount === 'function') {
                window.applyReviewsCount(targetMovieId, predictedCount);
            }
        };

        base.onRollback = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            delete (window.__ratingPredictState || {})[String(targetMovieId)];
            if (typeof window.updateMovieRating === 'function') window.updateMovieRating(targetMovieId);
            if (typeof window.updateReviewsCount === 'function') window.updateReviewsCount(targetMovieId);
        };

        base.onOptimisticDelete = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;

            window.reviewsUI.removeOwn(targetMovieId);

            var state = (window.__ratingPredictState || {})[String(targetMovieId)];
            if (!state || state.avgRating == null || state.totalReviews == null || !state.own) {
                if (typeof window.updateRatingBadge === 'function') {
                    window.updateRatingBadge(targetMovieId, null);
                }
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

            if (typeof window.updateRatingBadge === 'function') {
                window.updateRatingBadge(
                    targetMovieId,
                    predictedAvg != null ? predictedAvg.toFixed(1) : null
                );
            }
            if (typeof window.applyReviewsCount === 'function') {
                window.applyReviewsCount(targetMovieId, newCount);
            }
        };

        base.onRollbackDelete = function (targetMovieId) {
            if (String(targetMovieId) !== String(movieId)) return;
            delete (window.__ratingPredictState || {})[String(targetMovieId)];
            if (typeof window.updateMovieRating === 'function') window.updateMovieRating(targetMovieId);
            if (typeof window.updateReviewsCount === 'function') window.updateReviewsCount(targetMovieId);
        };

        base.onCountUpdate = function (targetMovieId, total) {
            if (String(targetMovieId) !== String(movieId)) return;
            window.reviewsUI.setCount(targetMovieId, total);
        };

        window.ratingContext = base;
    })();

    function bindExistingCards() {
        const own = list.querySelector('.review-card[data-own="true"]');
        if (own) bindOwnCardActions(own);
    }

    bindExistingCards();
    initTopAddBtn();

    function appendEndMarker() {
        if (list.parentNode && list.parentNode.querySelector('.reviews-end')) return;
        const marker = document.createElement('div');
        marker.className = 'reviews-end';
        marker.textContent = '— Это все отзывы —';
        list.insertAdjacentElement('afterend', marker);
    }

    if (!wrapper || wrapper.dataset.hasMore !== 'true') {
        if (wrapper) wrapper.remove();
        appendEndMarker();
        return;
    }

    const sort = wrapper.dataset.sort || 'DESC';
    let cursor = wrapper.dataset.cursor || null;
    let hasMore = true;
    let isLoading = false;

    const MAX_RETRIES = 3;
    const RETRY_BASE_DELAY_MS = 1000;
    const MAX_BACKOFF_MS = 30000;
    const FETCH_TIMEOUT_MS = 10000;
    const OBSERVER_DEBOUNCE_MS = 150;

    let retryCount = 0;
    let disabledUntil = 0;
    let observerTimer = null;
    let retryTimer = null;

    const sentinel = document.createElement('div');
    sentinel.setAttribute('aria-hidden', 'true');
    sentinel.style.height = '1px';
    sentinel.style.width = '100%';
    sentinel.style.pointerEvents = 'none';
    wrapper.appendChild(sentinel);

    const observer = new IntersectionObserver(onIntersect, {
        root: body,
        rootMargin: '0px 0px 200px 0px',
        threshold: 0
    });
    observer.observe(sentinel);

    maybeFillOnce();

    function onIntersect(entries) {
        if (!entries[0].isIntersecting) return;
        if (observerTimer) return;
        observerTimer = setTimeout(() => {
            observerTimer = null;
            requestLoad();
        }, OBSERVER_DEBOUNCE_MS);
    }

    function maybeFillOnce() {
        if (!hasMore) return;
        if (body.scrollHeight > body.clientHeight + 20) return;
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
        if (observerTimer) {
            clearTimeout(observerTimer);
            observerTimer = null;
        }
        if (retryTimer) {
            clearTimeout(retryTimer);
            retryTimer = null;
        }
        observer.disconnect();
    }

    async function loadNext() {
        if (isLoading || !hasMore) return;

        isLoading = true;
        wrapper.classList.add('is-loading');
        if (spinner) spinner.style.display = 'flex';

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);

        try {
            const params = new URLSearchParams();
            if (cursor) params.set('cursor', cursor);
            params.set('sort', sort);

            const resp = await fetch(`/rated/${movieId}/reviews/more?${params}`, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                signal: controller.signal
            });
            if (!resp.ok) throw new Error('HTTP ' + resp.status);

            const data = await resp.json();

            retryCount = 0;
            disabledUntil = 0;

            if (Array.isArray(data.reviews) && data.reviews.length > 0) {
                const fragment = document.createDocumentFragment();
                data.reviews.forEach(review => {
                    const el = buildCard(review, false, false);
                    if (el) fragment.appendChild(el);
                });
                list.appendChild(fragment);
            } else {
                hasMore = false;
            }

            cursor = data.nextCursor || null;
            if (data.hasMore === false) hasMore = false;

            if (countEl && data.totalReviews != null) {
                setCountText(data.totalReviews);
            }

            if (!hasMore) {
                disconnectObserver();
                wrapper.remove();
                appendEndMarker();
            }
        } catch (error) {
            const isAbort = error && error.name === 'AbortError';
            console.warn('[reviews] Ошибка загрузки:', isAbort ? 'timeout' : error);

            retryCount++;

            if (retryCount >= MAX_RETRIES) {
                const delay = Math.min(
                    RETRY_BASE_DELAY_MS * Math.pow(2, retryCount),
                    MAX_BACKOFF_MS
                );
                disabledUntil = Date.now() + delay;
                retryCount = 0;
            } else {
                const delay = RETRY_BASE_DELAY_MS * Math.pow(2, retryCount - 1);
                disabledUntil = Date.now() + delay;
            }
        } finally {
            clearTimeout(timeoutId);
            isLoading = false;
            wrapper.classList.remove('is-loading');
            if (spinner) spinner.style.display = 'none';
        }
    }
})();