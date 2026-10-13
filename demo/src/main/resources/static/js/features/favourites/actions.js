(function () {
    'use strict';

    function errorMessage(err) {
        if (!err) return 'Ошибка';
        if (err.status === 401) return 'Авторизуйтесь!';
        if (typeof err === 'string') return err;
        return err.message || 'Ошибка';
    }

    function buildActionForError(err) {
        if (!err) return null;
        if (err.status === 401 || err.code === 'NOT_AUTHORIZED') {
            return { text: 'Перейти к авторизации', url: '/login' };
        }
        return null;
    }

    function handleError(err) {
        showFlashMessage(errorMessage(err), 'error', undefined, buildActionForError(err));
    }

    function openModal(id) {
        return Modal.openById(id);
    }

    function closeModal(id) {
        Modal.closeById(id);
    }

    function bindOneTimeConfirm(modal, handler) {
        if (!modal) return;
        var btn = modal.querySelector('[data-modal-confirm]');
        if (!btn) return;
        if (btn.__favBound) btn.removeEventListener('click', btn.__favBound);
        btn.__favBound = handler;
        btn.addEventListener('click', handler);
    }

    function onToggle(el) {
        var movieId = getMovieId(el);
        var current = el.getAttribute('data-state') || 'add';
        if (!movieId) return;

        el.disabled = true;
        FavouritesFeature.toggle(movieId, current)
            .then(function (data) {
                showFlashMessage((data && data.message) || 'Обновлено', 'success');
                var newAction = current === 'add' ? 'remove' : 'add';
                el.setAttribute('data-state', newAction);
                el.classList.toggle('active', newAction === 'remove');
            })
            .catch(handleError)
            .finally(function () {
                el.disabled = false;
            });
    }

    function onRemoveFromFavourites(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;

        var modal = openModal('favouritesRemoveModal');
        if (!modal) return;

        var titleEl = modal.querySelector('[data-modal-movie-title]');
        if (titleEl) titleEl.textContent = getMovieTitle(el) || '';

        bindOneTimeConfirm(modal, function () {
            closeModal('favouritesRemoveModal');
            FavouritesFeature.remove(movieId)
                .then(function (data) {
                    showFlashMessage((data && data.message) || 'Удалено', 'success');
                    var card = document.querySelector(
                        '.favourites-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                    );
                    UI.animateRemoveCard(card, function () {
                        if (document.querySelectorAll('.favourites-card').length === 0) {
                            window.location.reload();
                        }
                    });
                })
                .catch(handleError);
        });
    }

    function onClearFavourites() {
        var modal = openModal('favouritesClearModal');
        if (!modal) return;

        bindOneTimeConfirm(modal, function () {
            closeModal('favouritesClearModal');
            FavouritesFeature.clear()
                .then(function (data) {
                    saveFlashAndRedirect(
                        (data && data.message) || 'Избранное очищено',
                        'success',
                        '/favourites'
                    );
                })
                .catch(handleError);
        });
    }

    function register() {
        if (!window.Actions) {
            if (window.console) console.error('[Favourites] Actions core not loaded');
            return;
        }
        Actions.register('toggle-favourite', onToggle);
        Actions.register('remove-from-favourites', onRemoveFromFavourites);
        Actions.register('clear-favourites', onClearFavourites);
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(register);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', register, { once: true });
    } else {
        register();
    }
})();