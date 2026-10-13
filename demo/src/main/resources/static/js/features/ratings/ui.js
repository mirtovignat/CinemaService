(function () {
    'use strict';

    var REVIEW_MAX_LENGTH = 350;

    function errorMessage(err) {
        if (!err) return 'Ошибка';
        if (err.status === 401) return 'Авторизуйтесь!';
        if (typeof err === 'string') return err;
        return err.message || 'Ошибка';
    }

    var AUTH_ACTION = { text: 'Перейти к авторизации', url: '/login' };

    function ensureModalInDom() {
        return document.getElementById('ratingModal');
    }

    function resetModalButtons(modal) {
        if (!modal) return;
        var submitBtn = modal.querySelector('.modal-confirm-btn');
        var cancelBtn = modal.querySelector('.modal-cancel-btn');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Отправить';
        }
        if (cancelBtn) {
            cancelBtn.disabled = false;
        }
    }

    function parseRating(value, fallback) {
        if (value == null) return fallback;
        var num = parseFloat(String(value).replace(',', '.'));
        if (isNaN(num)) return fallback;
        return num;
    }

    function normalizeRatingString(value, fallback) {
        var num = parseRating(value, fallback != null ? fallback : 5.0);
        var rounded = Math.round(num * 10) / 10;
        if (rounded < 0.1) rounded = 0.1;
        if (rounded > 10) rounded = 10;
        return rounded.toFixed(1);
    }

    function updateCommentCounter(wrapper, textarea, counter) {
        if (!wrapper || !textarea || !counter) return;
        var len = textarea.value.length;
        counter.textContent = len + ' / ' + REVIEW_MAX_LENGTH;
        counter.classList.toggle('warn', len >= Math.floor(REVIEW_MAX_LENGTH * 0.8) && len < REVIEW_MAX_LENGTH);
        counter.classList.toggle('limit', len >= REVIEW_MAX_LENGTH);
    }

    function openModalInternal(movieId, movieTitle, mode, existingRating, existingReview) {
        var modal = ensureModalInDom();
        if (!modal) return;

        var keyInput = document.getElementById('ratingFormTitle');
        var titleEl = document.getElementById('ratingModalMovieTitle');
        var ratingInput = document.getElementById('ratingValue');
        var reviewInput = document.getElementById('ratingComment');
        var heading = modal.querySelector('h2');
        var errorDiv = document.getElementById('ratingModalError');

        resetModalButtons(modal);

        if (errorDiv) {
            errorDiv.innerText = '';
            errorDiv.style.display = 'none';
        }
        if (keyInput) keyInput.value = movieId;
        if (titleEl) {
            titleEl.innerText = movieTitle || 'Рейтинг фильма';
            titleEl.dataset.movieId = movieId;
        }

        if (ratingInput) {
            ratingInput.value = normalizeRatingString(existingRating, 5.0);
        }

        if (reviewInput) {
            var rv = existingReview || '';
            if (rv.length > REVIEW_MAX_LENGTH) {
                rv = rv.substring(0, REVIEW_MAX_LENGTH);
            }
            reviewInput.value = rv;
        }

        if (heading) {
            heading.innerText = (mode === 'edit') ? 'Редактировать отзыв' : 'Оценить фильм';
        }

        var commentWrapper = modal.querySelector('[data-comment-wrapper]');
        if (commentWrapper && reviewInput) {
            commentWrapper.classList.toggle('has-value', reviewInput.value.length > 0);
            updateCommentCounter(
                commentWrapper,
                reviewInput,
                commentWrapper.querySelector('[data-comment-counter]')
            );
        }

        modal.classList.add('show');

        if (ratingInput) {
            setTimeout(function () {
                ratingInput.focus();
                ratingInput.select();
            }, 50);
        }
    }

    function showRatingModal(movieId, movieTitle, mode, existingRating, existingReview) {
        if (typeof isAuthenticated === 'function' && !isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error', undefined, AUTH_ACTION);
            return;
        }
        if (!movieId || movieId === 'null' || movieId === 'undefined' || movieId === '') {
            showFlashMessage('Не удалось определить фильм', 'error');
            return;
        }

        var modal = ensureModalInDom();
        if (!modal) {
            showFlashMessage('Не удалось открыть окно оценки', 'error');
            return;
        }

        if (window.__ratingModalOpening) return;
        window.__ratingModalOpening = true;

        window._ratingModalMovieTitle = movieTitle || '';

        var ctx = window.ratingContext;
        var load = (ctx && typeof ctx.getCurrent === 'function')
            ? ctx.getCurrent(movieId)
            : null;

        Promise.resolve(load).then(function (current) {
            var hasOwn = !!(current && current.own && current.own.rating != null);

            var realMode;
            var realRating;
            var realReview;

            if (hasOwn) {
                realMode = 'edit';
                realRating = parseRating(current.own.rating, 5.0);
                realReview = current.own.review || '';
            } else {
                realMode = 'add';
                realReview = '';
                if (current && current.avgRating != null) {
                    realRating = parseRating(current.avgRating, 5.0);
                } else if (existingRating != null) {
                    realRating = parseRating(existingRating, 5.0);
                } else {
                    realRating = 5.0;
                }
            }

            realRating = Math.round(realRating * 10) / 10;
            if (realRating < 0.1) realRating = 0.1;
            if (realRating > 10) realRating = 10;

            window._ratingModalMode = realMode;
            window._ratingModalOriginalRating = realRating;
            window._ratingModalOriginalReview = realReview;

            openModalInternal(movieId, movieTitle, realMode, realRating.toFixed(1), realReview);
            window.__ratingModalOpening = false;
        }).catch(function () {
            var fallbackRating = parseRating(existingRating, 5.0);
            fallbackRating = Math.round(fallbackRating * 10) / 10;
            if (fallbackRating < 0.1) fallbackRating = 0.1;
            if (fallbackRating > 10) fallbackRating = 10;

            window._ratingModalMode = mode || 'add';
            window._ratingModalOriginalRating = fallbackRating;
            window._ratingModalOriginalReview = existingReview || '';

            openModalInternal(movieId, movieTitle, mode || 'add',
                fallbackRating.toFixed(1),
                existingReview || '');
            window.__ratingModalOpening = false;
        });
    }

    function hideRatingModal() {
        var modal = document.getElementById('ratingModal');
        if (modal) {
            modal.classList.remove('show');
            resetModalButtons(modal);
        }
        var errorDiv = document.getElementById('ratingModalError');
        if (errorDiv) {
            errorDiv.innerText = '';
            errorDiv.style.display = 'none';
        }
    }

    function buildOptimisticReview(rating, review) {
        var userDataEl = document.getElementById('currentUserData');
        var username = userDataEl ? (userDataEl.dataset.username || 'Пользователь') : 'Пользователь';
        return {
            username: username,
            ratingValue: rating,
            reviewText: review || '',
            addedAt: new Date().toISOString()
        };
    }

    async function submitRating() {
        if (typeof isAuthenticated === 'function' && !isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error', undefined, AUTH_ACTION);
            return;
        }
        var modal = document.getElementById('ratingModal');
        if (!modal) return;
        var errorDiv = document.getElementById('ratingModalError');
        if (errorDiv) {
            errorDiv.style.display = 'none';
            errorDiv.innerText = '';
        }
        var movieId = document.getElementById('ratingFormTitle').value;
        if (!movieId || movieId === 'null' || movieId === 'undefined' || movieId === '') {
            showFlashMessage('Не удалось определить фильм', 'error');
            return;
        }

        var rollbackRecommendation = window.callRemoveRecommendation
            ? window.callRemoveRecommendation(movieId)
            : function () {};

        var ratingInput = document.getElementById('ratingValue');
        var rating = ratingInput.value.replace(',', '.');
        var review = document.getElementById('ratingComment').value;

        var rRating = Validators.rating(rating);
        if (!rRating.ok) {
            if (errorDiv) {
                errorDiv.innerText = rRating.error;
                errorDiv.style.display = 'block';
            }
            return;
        }

        if (review && review.length > REVIEW_MAX_LENGTH) {
            if (errorDiv) {
                errorDiv.innerText = 'Отзыв не может превышать ' + REVIEW_MAX_LENGTH + ' символов';
                errorDiv.style.display = 'block';
            }
            return;
        }

        var ratingValue = Math.round(parseFloat(rating) * 10) / 10;
        var submitBtn = modal.querySelector('.modal-confirm-btn');
        var cancelBtn = modal.querySelector('.modal-cancel-btn');
        var originalText = submitBtn ? submitBtn.textContent : 'Отправить';

        var mode = window._ratingModalMode || 'add';
        var trimmedReview = review ? review.trim() : '';
        var originalRating = window._ratingModalOriginalRating;
        var originalReview = (window._ratingModalOriginalReview || '').trim();

        if (mode === 'edit' && originalRating != null) {
            var oldStr = parseFloat(originalRating).toFixed(1);
            var newStr = ratingValue.toFixed(1);
            if (oldStr === newStr && originalReview === trimmedReview) {
                hideRatingModal();
                return;
            }
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Отправка...';
        }
        if (cancelBtn) cancelBtn.disabled = true;

        var ctx = window.ratingContext;
        var movieTitle = window._ratingModalMovieTitle || '';
        var hadOwn = mode === 'edit';

        if (ctx && typeof ctx.onOptimistic === 'function') {
            var optimisticReview = buildOptimisticReview(ratingValue, trimmedReview);
            optimisticReview.movieTitle = movieTitle;
            optimisticReview.isNew = !hadOwn;
            optimisticReview.ratingValue = ratingValue;
            ctx.onOptimistic(movieId, optimisticReview);
        }

        hideRatingModal();

        var data;
        try {
            data = mode === 'add'
                ? await RatingsFeature.add(movieId, ratingValue.toFixed(1), trimmedReview)
                : await RatingsFeature.edit(movieId, ratingValue.toFixed(1), trimmedReview);
        } catch (err) {
            rollbackRecommendation();
            if (ctx && typeof ctx.onRollback === 'function') ctx.onRollback(movieId);

            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
            if (cancelBtn) cancelBtn.disabled = false;

            showFlashMessage(errorMessage(err), 'error');
            return;
        }

        if (!data) {
            rollbackRecommendation();
            if (ctx && typeof ctx.onRollback === 'function') ctx.onRollback(movieId);
            showFlashMessage('Не удалось сохранить оценку', 'error');
            return;
        }

        showFlashMessage(data.message, 'success');

        if (typeof applyRatingResponse === 'function') applyRatingResponse(data, movieId);

        if (typeof window.updateMovieRating === 'function') {
            window.updateMovieRating(movieId);
        }

        var payload = (data && data.data) || data || {};
        if (ctx && typeof ctx.onCountUpdate === 'function' && payload.totalReviews != null) {
            ctx.onCountUpdate(movieId, payload.totalReviews);
        } else if (typeof updateReviewsCount === 'function') {
            updateReviewsCount(movieId);
        }
    }

    function deleteRating(movieId) {
        if (typeof isAuthenticated === 'function' && !isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error', undefined, AUTH_ACTION);
            return;
        }
        if (!movieId || movieId === 'null' || movieId === 'undefined' || movieId === '') {
            showFlashMessage('Не удалось определить фильм', 'error');
            return;
        }
        window._deleteMovieId = movieId;
        var modal = document.getElementById('confirmDeleteModal');
        if (modal) modal.classList.add('show');

        var confirmBtn = document.getElementById('confirmDeleteBtn');
        if (confirmBtn) {
            setTimeout(function () { confirmBtn.focus(); }, 50);
        }
    }

    function hideConfirmDeleteModal() {
        var modal = document.getElementById('confirmDeleteModal');
        if (modal) modal.classList.remove('show');
        window._deleteMovieId = null;
    }

    async function executeDelete() {
        var movieId = window._deleteMovieId;
        if (!movieId) return;

        var ctx = window.ratingContext;

        hideConfirmDeleteModal();

        if (ctx && typeof ctx.onOptimisticDelete === 'function') {
            ctx.onOptimisticDelete(movieId);
        }

        var data;
        try {
            data = await RatingsFeature.remove(movieId);
        } catch (err) {
            if (ctx && typeof ctx.onRollbackDelete === 'function') {
                ctx.onRollbackDelete(movieId);
            }
            showFlashMessage(errorMessage(err), 'error');
            return;
        }

        if (!data) {
            if (ctx && typeof ctx.onRollbackDelete === 'function') {
                ctx.onRollbackDelete(movieId);
            }
            showFlashMessage('Не удалось удалить отзыв', 'error');
            return;
        }

        showFlashMessage(data.message, 'success');

        if (typeof updateRatingBadge === 'function') updateRatingBadge(movieId, null);
        if (typeof updateMovieRating === 'function') updateMovieRating(movieId);

        var payload = (data && data.data) || data || {};
        if (ctx && typeof ctx.onCountUpdate === 'function' && payload.totalReviews != null) {
            ctx.onCountUpdate(movieId, payload.totalReviews);
        } else if (typeof updateReviewsCount === 'function') {
            updateReviewsCount(movieId);
        }
    }

    function stepRating(delta) {
        var input = document.getElementById('ratingValue');
        if (!input) return;
        var current = parseFloat(input.value.replace(',', '.'));
        if (isNaN(current)) current = 0;
        var newValue = current + delta;
        newValue = Math.round(newValue * 10) / 10;
        if (newValue < 0.1) newValue = 0.1;
        if (newValue > 10.0) newValue = 10.0;
        input.value = newValue.toFixed(1);
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var ratingStepper = null;

    function startStepping(delta, button) {
        if (!ratingStepper && typeof createStepper === 'function') {
            ratingStepper = createStepper({
                onStep: function (d) { stepRating(d); },
                startDelay: 250,
                repeatDelay: 80
            });
        }
        if (ratingStepper) ratingStepper.start(delta, button);
    }

    function stopStepping(button) {
        if (ratingStepper) ratingStepper.stop(button);
    }

    function onRatingModalKeydown(e) {
        if (e.key !== 'Enter') return;
        if (e.shiftKey) return;

        var modal = document.getElementById('ratingModal');
        if (!modal || !modal.classList.contains('show')) return;

        var target = e.target;
        if (target && (target.tagName === 'BUTTON' || target.tagName === 'A')) return;

        if (target && target.id === 'ratingComment') {
            if (!(e.ctrlKey || e.metaKey)) return;
        }

        e.preventDefault();
        e.stopPropagation();

        var ratingInput = document.getElementById('ratingValue');
        if (ratingInput && ratingInput === target) {
            ratingInput.blur();
        }

        submitRating();
    }

    function onDeleteModalKeydown(e) {
        if (e.key !== 'Enter') return;

        var modal = document.getElementById('confirmDeleteModal');
        if (!modal || !modal.classList.contains('show')) return;

        var target = e.target;
        if (target && (target.tagName === 'BUTTON' || target.tagName === 'A')) return;

        e.preventDefault();
        e.stopPropagation();
        executeDelete();
    }

    function initCommentTools() {
        var wrapper = document.querySelector('[data-comment-wrapper]');
        if (!wrapper) return;
        if (wrapper.dataset.commentToolsBound === '1') return;
        wrapper.dataset.commentToolsBound = '1';

        var textarea = wrapper.querySelector('textarea');
        var clearBtn = wrapper.querySelector('[data-comment-clear]');
        var counter = wrapper.querySelector('[data-comment-counter]');
        if (!textarea) return;

        if (textarea.getAttribute('maxlength') == null) {
            textarea.setAttribute('maxlength', String(REVIEW_MAX_LENGTH));
        }

        function sync() {
            wrapper.classList.toggle('has-value', textarea.value.length > 0);
            updateCommentCounter(wrapper, textarea, counter);
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                textarea.value = '';
                textarea.dispatchEvent(new Event('input', { bubbles: true }));
                textarea.dispatchEvent(new Event('change', { bubbles: true }));
                textarea.focus();
                sync();
            });
        }

        textarea.addEventListener('input', function () {
            if (textarea.value.length > REVIEW_MAX_LENGTH) {
                textarea.value = textarea.value.substring(0, REVIEW_MAX_LENGTH);
            }
            sync();
        });

        textarea.addEventListener('change', sync);
        sync();
    }

    function init() {
        initCommentTools();
        document.addEventListener('keydown', onRatingModalKeydown);
        document.addEventListener('keydown', onDeleteModalKeydown);
    }

    window.showRatingModal = showRatingModal;
    window.hideRatingModal = hideRatingModal;
    window.submitRating = submitRating;
    window.deleteRating = deleteRating;
    window.hideConfirmDeleteModal = hideConfirmDeleteModal;
    window.executeDelete = executeDelete;
    window.stepRating = stepRating;
    window.startStepping = startStepping;
    window.stopStepping = stopStepping;

    function registerRatingActions() {
        if (!window.Actions) return;
        if (window.__ratingActionsBound) return;
        window.__ratingActionsBound = true;

        window.Actions.register('submit-rating', function () {
            submitRating();
            return true;
        });

        window.Actions.register('hide-rating-modal', function () {
            hideRatingModal();
            return true;
        });

        window.Actions.register('hide-confirm-delete-modal', function () {
            hideConfirmDeleteModal();
            return true;
        });

        window.Actions.register('execute-delete', function () {
            executeDelete();
            return true;
        });

        window.Actions.register('rating-step-up', function () {
            stepRating(0.1);
            return true;
        });

        window.Actions.register('rating-step-down', function () {
            stepRating(-0.1);
            return true;
        });
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerRatingActions);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerRatingActions, { once: true });
    } else {
        registerRatingActions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();