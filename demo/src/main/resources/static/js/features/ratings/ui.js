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
        var submitButton = modal.querySelector('.modal-confirm-btn');
        var cancelButton = modal.querySelector('.modal-cancel-btn');
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = 'Отправить';
        }
        if (cancelButton) {
            cancelButton.disabled = false;
        }
    }

    function parseRating(value, fallback) {
        if (value == null) return fallback;
        var numericRating = parseFloat(String(value).replace(',', '.'));
        if (isNaN(numericRating)) return fallback;
        return numericRating;
    }

    function normalizeRatingString(value, fallback) {
        var numericRating = parseRating(value, fallback != null ? fallback : 5.0);
        var roundedRating = Math.round(numericRating * 10) / 10;
        if (roundedRating < 0.1) roundedRating = 0.1;
        if (roundedRating > 10) roundedRating = 10;
        return roundedRating.toFixed(1);
    }

    function updateCommentCounter(wrapper, textarea, counter) {
        if (!wrapper || !textarea || !counter) return;
        var textLength = textarea.value.length;
        counter.textContent = textLength + ' / ' + REVIEW_MAX_LENGTH;
        counter.classList.toggle('warn', textLength >= Math.floor(REVIEW_MAX_LENGTH * 0.8) && textLength < REVIEW_MAX_LENGTH);
        counter.classList.toggle('limit', textLength >= REVIEW_MAX_LENGTH);
    }

    function openModalInternal(movieId, movieTitle, mode, existingRating, existingReview) {
        var modal = ensureModalInDom();
        if (!modal) return;

        var titleInput = document.getElementById('ratingFormTitle');
        var titleElement = document.getElementById('ratingModalMovieTitle');
        var ratingInput = document.getElementById('ratingValue');
        var reviewInput = document.getElementById('ratingComment');
        var heading = modal.querySelector('h2');
        var errorDiv = document.getElementById('ratingModalError');

        resetModalButtons(modal);

        if (errorDiv) {
            errorDiv.innerText = '';
            errorDiv.style.display = 'none';
        }
        if (titleInput) titleInput.value = movieId;
        if (titleElement) {
            titleElement.innerText = movieTitle || 'Рейтинг фильма';
            titleElement.dataset.movieId = movieId;
        }

        if (ratingInput) {
            ratingInput.value = normalizeRatingString(existingRating, 5.0);
        }

        if (reviewInput) {
            var reviewValue = existingReview || '';
            if (reviewValue.length > REVIEW_MAX_LENGTH) {
                reviewValue = reviewValue.substring(0, REVIEW_MAX_LENGTH);
            }
            reviewInput.value = reviewValue;
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

        var ratingContext = window.ratingContext;
        var loadPromise = (ratingContext && typeof ratingContext.getCurrent === 'function')
            ? ratingContext.getCurrent(movieId)
            : null;

        Promise.resolve(loadPromise).then(function (currentRatingData) {
            var hasOwnRating = !!(currentRatingData && currentRatingData.own && currentRatingData.own.rating != null);

            var realMode;
            var realRating;
            var realReview;

            if (hasOwnRating) {
                realMode = 'edit';
                realRating = parseRating(currentRatingData.own.rating, 5.0);
                realReview = currentRatingData.own.review || '';
            } else {
                realMode = 'add';
                realReview = '';
                if (currentRatingData && currentRatingData.avgRating != null) {
                    realRating = parseRating(currentRatingData.avgRating, 5.0);
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
        var userDataElement = document.getElementById('currentUserData');
        var username = userDataElement ? (userDataElement.dataset.username || 'Пользователь') : 'Пользователь';
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

        var ratingValidation = Validators.rating(rating);
        if (!ratingValidation.ok) {
            if (errorDiv) {
                errorDiv.innerText = ratingValidation.error;
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
        var submitButton = modal.querySelector('.modal-confirm-btn');
        var cancelButton = modal.querySelector('.modal-cancel-btn');
        var originalText = submitButton ? submitButton.textContent : 'Отправить';

        var mode = window._ratingModalMode || 'add';
        var trimmedReview = review ? review.trim() : '';
        var originalRating = window._ratingModalOriginalRating;
        var originalReview = (window._ratingModalOriginalReview || '').trim();

        if (mode === 'edit' && originalRating != null) {
            var oldRatingString = parseFloat(originalRating).toFixed(1);
            var newRatingString = ratingValue.toFixed(1);
            if (oldRatingString === newRatingString && originalReview === trimmedReview) {
                hideRatingModal();
                return;
            }
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = 'Отправка...';
        }
        if (cancelButton) cancelButton.disabled = true;

        var ratingContext = window.ratingContext;
        var movieTitle = window._ratingModalMovieTitle || '';
        var hadOwnRating = mode === 'edit';

        if (ratingContext && typeof ratingContext.onOptimistic === 'function') {
            var optimisticReview = buildOptimisticReview(ratingValue, trimmedReview);
            optimisticReview.movieTitle = movieTitle;
            optimisticReview.isNew = !hadOwnRating;
            optimisticReview.ratingValue = ratingValue;
            ratingContext.onOptimistic(movieId, optimisticReview);
        }

        hideRatingModal();

        var responseData;
        try {
            responseData = mode === 'add'
                ? await RatingsFeature.add(movieId, ratingValue.toFixed(1), trimmedReview)
                : await RatingsFeature.edit(movieId, ratingValue.toFixed(1), trimmedReview);
        } catch (err) {
            rollbackRecommendation();
            if (ratingContext && typeof ratingContext.onRollback === 'function') ratingContext.onRollback(movieId);

            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = originalText;
            }
            if (cancelButton) cancelButton.disabled = false;

            showFlashMessage(errorMessage(err), 'error');
            return;
        }

        if (!responseData) {
            rollbackRecommendation();
            if (ratingContext && typeof ratingContext.onRollback === 'function') ratingContext.onRollback(movieId);
            showFlashMessage('Не удалось сохранить оценку', 'error');
            return;
        }

        showFlashMessage(responseData.message, 'success');

        if (typeof applyRatingResponse === 'function') applyRatingResponse(responseData, movieId);

        if (typeof window.updateMovieRating === 'function') {
            window.updateMovieRating(movieId);
        }

        var payload = (responseData && responseData.data) || responseData || {};
        if (ratingContext && typeof ratingContext.onCountUpdate === 'function' && payload.totalReviews != null) {
            ratingContext.onCountUpdate(movieId, payload.totalReviews);
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

        var confirmButton = document.getElementById('confirmDeleteBtn');
        if (confirmButton) {
            setTimeout(function () { confirmButton.focus(); }, 50);
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

        var ratingContext = window.ratingContext;

        hideConfirmDeleteModal();

        if (ratingContext && typeof ratingContext.onOptimisticDelete === 'function') {
            ratingContext.onOptimisticDelete(movieId);
        }

        var responseData;
        try {
            responseData = await RatingsFeature.remove(movieId);
        } catch (err) {
            if (ratingContext && typeof ratingContext.onRollbackDelete === 'function') {
                ratingContext.onRollbackDelete(movieId);
            }
            showFlashMessage(errorMessage(err), 'error');
            return;
        }

        if (!responseData) {
            if (ratingContext && typeof ratingContext.onRollbackDelete === 'function') {
                ratingContext.onRollbackDelete(movieId);
            }
            showFlashMessage('Не удалось удалить отзыв', 'error');
            return;
        }

        showFlashMessage(responseData.message, 'success');

        if (typeof updateRatingBadge === 'function') updateRatingBadge(movieId, null);
        if (typeof updateMovieRating === 'function') updateMovieRating(movieId);

        var payload = (responseData && responseData.data) || responseData || {};
        if (ratingContext && typeof ratingContext.onCountUpdate === 'function' && payload.totalReviews != null) {
            ratingContext.onCountUpdate(movieId, payload.totalReviews);
        } else if (typeof updateReviewsCount === 'function') {
            updateReviewsCount(movieId);
        }
    }

    function stepRating(delta) {
        var ratingInput = document.getElementById('ratingValue');
        if (!ratingInput) return;
        var currentRating = parseFloat(ratingInput.value.replace(',', '.'));
        if (isNaN(currentRating)) currentRating = 0;
        var newRating = currentRating + delta;
        newRating = Math.round(newRating * 10) / 10;
        if (newRating < 0.1) newRating = 0.1;
        if (newRating > 10.0) newRating = 10.0;
        ratingInput.value = newRating.toFixed(1);
        ratingInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var ratingStepper = null;

    function startStepping(delta, button) {
        if (!ratingStepper && typeof createStepper === 'function') {
            ratingStepper = createStepper({
                onStep: function (stepDelta) { stepRating(stepDelta); },
                startDelay: 250,
                repeatDelay: 80
            });
        }
        if (ratingStepper) ratingStepper.start(delta, button);
    }

    function stopStepping(button) {
        if (ratingStepper) ratingStepper.stop(button);
    }

    function onRatingModalKeydown(event) {
        if (event.key !== 'Enter') return;
        if (event.shiftKey) return;

        var modal = document.getElementById('ratingModal');
        if (!modal || !modal.classList.contains('show')) return;

        var target = event.target;
        if (target && (target.tagName === 'BUTTON' || target.tagName === 'A')) return;

        if (target && target.id === 'ratingComment') {
            if (!(event.ctrlKey || event.metaKey)) return;
        }

        event.preventDefault();
        event.stopPropagation();

        var ratingInput = document.getElementById('ratingValue');
        if (ratingInput && ratingInput === target) {
            ratingInput.blur();
        }

        submitRating();
    }

    function onDeleteModalKeydown(event) {
        if (event.key !== 'Enter') return;

        var modal = document.getElementById('confirmDeleteModal');
        if (!modal || !modal.classList.contains('show')) return;

        var target = event.target;
        if (target && (target.tagName === 'BUTTON' || target.tagName === 'A')) return;

        event.preventDefault();
        event.stopPropagation();
        executeDelete();
    }

    function initCommentTools() {
        var wrapper = document.querySelector('[data-comment-wrapper]');
        if (!wrapper) return;
        if (wrapper.dataset.commentToolsBound === '1') return;
        wrapper.dataset.commentToolsBound = '1';

        var textarea = wrapper.querySelector('textarea');
        var clearButton = wrapper.querySelector('[data-comment-clear]');
        var counter = wrapper.querySelector('[data-comment-counter]');
        if (!textarea) return;

        if (textarea.getAttribute('maxlength') == null) {
            textarea.setAttribute('maxlength', String(REVIEW_MAX_LENGTH));
        }

        function syncCommentTools() {
            wrapper.classList.toggle('has-value', textarea.value.length > 0);
            updateCommentCounter(wrapper, textarea, counter);
        }

        if (clearButton) {
            clearButton.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                textarea.value = '';
                textarea.dispatchEvent(new Event('input', { bubbles: true }));
                textarea.dispatchEvent(new Event('change', { bubbles: true }));
                textarea.focus();
                syncCommentTools();
            });
        }

        textarea.addEventListener('input', function () {
            if (textarea.value.length > REVIEW_MAX_LENGTH) {
                textarea.value = textarea.value.substring(0, REVIEW_MAX_LENGTH);
            }
            syncCommentTools();
        });

        textarea.addEventListener('change', syncCommentTools);
        syncCommentTools();
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