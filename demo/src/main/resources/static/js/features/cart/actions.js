(function () {
    'use strict';

    function cardFrom(el) {
        return el.closest('.cart-card, .media-card, .movie-card');
    }

    function replaceWithAddButton(button, movieId) {
        if (!button.parentNode) return;
        var newBtn = document.createElement('button');
        newBtn.type = 'button';
        newBtn.className = 'cart-button to-cart-btn-details';
        newBtn.setAttribute('data-action', 'add-to-cart');
        newBtn.setAttribute('data-movie-id', movieId);
        newBtn.textContent = 'В корзину';
        button.parentNode.replaceChild(newBtn, button);
    }

    function errorMessage(err) {
        if (!err) return 'Ошибка';
        if (err.status === 401) return 'Авторизуйтесь!';
        if (typeof err === 'string') return err;
        return err.message || 'Ошибка';
    }

    function extractShortage(message) {
        if (!message) return null;
        var needMatch = message.match(/нужно\s*([\d\s]+(?:[.,]\d+)?)\s*₽/i);
        var haveMatch = message.match(/доступно\s*([\d\s]+(?:[.,]\d+)?)\s*₽/i);
        if (!needMatch || !haveMatch) return null;

        var need = parseFloat(needMatch[1].replace(/\s/g, '').replace(',', '.'));
        var have = parseFloat(haveMatch[1].replace(/\s/g, '').replace(',', '.'));
        if (isNaN(need) || isNaN(have)) return null;

        var shortage = need - have;
        if (shortage <= 0) return null;
        return shortage;
    }

    function buildActionForError(err) {
        if (!err) return null;
        if (err.status === 401 || err.code === 'NOT_AUTHORIZED') {
            return { text: 'Перейти к авторизации', url: '/login' };
        }
        if (err.code === 'INSUFFICIENT_FUNDS') {
            var shortage = extractShortage(err.message);
            if (shortage != null) {
                try {
                    sessionStorage.setItem('topUpAmount', shortage.toFixed(2));
                } catch (e) {}
            }
            return { text: 'Перейти к пополнению', url: '/wallet/top-up' };
        }
        return null;
    }

    function handleError(err) {
        showFlashMessage(errorMessage(err), 'error', undefined, buildActionForError(err));
    }

    function buildWatchButton(movieId, movieTitle, posterUrl) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'watch-btn-neon watch-btn-neon--md';
        btn.setAttribute('data-action', 'watch-movie');
        btn.setAttribute('data-movie-id', movieId);
        btn.setAttribute('data-movie-title', movieTitle || '');
        btn.setAttribute('data-poster-url', posterUrl || '');
        btn.setAttribute('data-watched', 'false');

        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('fill', 'currentColor');
        var poly = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
        poly.setAttribute('points', '5 3 19 12 5 21 5 3');
        svg.appendChild(poly);

        btn.appendChild(svg);
        btn.appendChild(document.createTextNode(' Смотреть'));
        return btn;
    }

    function addWatchButtonOptimistic(movieId, movieTitle, posterUrl) {
        var card = document.querySelector(
            '.movie-card[data-movie-id="' + CSS.escape(movieId) + '"]'
        );

        if (card) {
            var posterLink = card.querySelector('.movie-poster-link');
            if (!posterLink) return null;

            var existing = posterLink.querySelector('.poster-overlay');
            if (existing) {
                if (existing.querySelector('.watch-btn-neon')) return null;
                existing.appendChild(buildWatchButton(movieId, movieTitle, posterUrl));
                return null;
            }

            var overlay = document.createElement('div');
            overlay.className = 'poster-overlay';
            overlay.appendChild(buildWatchButton(movieId, movieTitle, posterUrl));
            posterLink.appendChild(overlay);

            return function rollback() {
                if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
            };
        }

        var details = document.querySelector('.movie-details-container');
        if (details && String(details.getAttribute('data-movie-id')) === String(movieId)) {
            var posterWrapper = details.querySelector('.poster-wrapper');
            if (!posterWrapper) return null;

            var existingOverlay = posterWrapper.querySelector('.poster-overlay');
            if (existingOverlay) {
                if (existingOverlay.querySelector('.watch-btn-neon')) return null;
                existingOverlay.appendChild(buildWatchButton(movieId, movieTitle, posterUrl));
                return null;
            }

            var newOverlay = document.createElement('div');
            newOverlay.className = 'poster-overlay';
            newOverlay.appendChild(buildWatchButton(movieId, movieTitle, posterUrl));
            posterWrapper.appendChild(newOverlay);

            return function rollback() {
                if (newOverlay.parentNode) newOverlay.parentNode.removeChild(newOverlay);
            };
        }

        return null;
    }

    async function onAdd(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;
        if (el.tagName === 'BUTTON') el.disabled = true;
        try {
            var data = await CartFeature.add(movieId);
            showFlashMessage((data && data.message) || 'Добавлено в корзину', 'success');
            el.textContent = 'В корзине → Перейти';
            el.className = 'cart-button in-cart-btn-details';
            el.removeAttribute('data-action');
            el.onclick = function () {
                window.location.href = '/cart?highlight=' + encodeURIComponent(movieId);
            };
        } catch (err) {
            handleError(err);
        } finally {
            if (el.tagName === 'BUTTON') el.disabled = false;
        }
    }

    async function onRemove(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;
        if (el.tagName === 'BUTTON') el.disabled = true;
        try {
            var data = await CartFeature.remove(movieId);
            showFlashMessage((data && data.message) || 'Удалено из корзины', 'success');
            var card = cardFrom(el);
            if (card && window.location.pathname.indexOf('/cart') === 0) {
                UI.animateRemoveCard(card, function () {
                    if (document.querySelectorAll('.cart-card').length === 0) {
                        window.location.reload();
                    }
                });
            } else {
                replaceWithAddButton(el, movieId);
            }
        } catch (err) {
            handleError(err);
        } finally {
            if (el.tagName === 'BUTTON') el.disabled = false;
        }
    }

    function onConfirmRemove(el) {
        var movieId = getMovieId(el);
        Modal.open({
            title: 'Удаление из корзины',
            message: 'Вы уверены, что хотите удалить фильм из корзины?',
            summaryHtml:
                '<p style="font-weight:600; margin-top:0.5rem;">' +
                    escapeHtml(getMovieTitle(el)) +
                '</p>',
            confirmText: 'Удалить',
            danger: true,
            onConfirm: function () {
                CartFeature.remove(movieId)
                    .then(function (data) {
                        showFlashMessage((data && data.message) || 'Удалено', 'success');
                        var card = document.querySelector(
                            '.cart-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                        );
                        UI.animateRemoveCard(card, function () {
                            if (document.querySelectorAll('.cart-card').length === 0) {
                                window.location.reload();
                            }
                        });
                    })
                    .catch(handleError);
            }
        });
    }

    function onClearCart() {
        Modal.open({
            title: 'Очистка корзины',
            message: 'Вы уверены, что хотите удалить все фильмы из корзины?',
            confirmText: 'Очистить корзину',
            danger: true,
            onConfirm: function () {
                CartFeature.clear()
                    .then(function (data) {
                        saveFlashAndRedirect(
                            (data && data.message) || 'Корзина очищена',
                            'success',
                            '/cart'
                        );
                    })
                    .catch(handleError);
            }
        });
    }

    function onSingleBuy(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;
        Modal.open({
            title: 'Подтверждение покупки',
            message: 'Вы уверены, что хотите купить фильм:',
            summaryHtml:
                '<p style="font-weight:600;">' +
                    escapeHtml(getMovieTitle(el)) +
                '</p><div class="modal-price">' +
                    escapeHtml(getMoviePrice(el)) + ' ₽' +
                '</div>',
            confirmText: 'Купить',
            onConfirm: function () {
                var movieTitle = getMovieTitle(el);
                var cartCard = document.querySelector(
                    '.cart-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                );
                var posterUrl = '';
                if (cartCard) {
                    var posterImg = cartCard.querySelector('.media-card-poster-img');
                    if (posterImg) posterUrl = posterImg.getAttribute('src') || '';
                }

                var rollbackWatch = addWatchButtonOptimistic(movieId, movieTitle, posterUrl);

                CartFeature.buyOne(movieId)
                    .then(function (data) {
                        showFlashMessage((data && data.message) || 'Фильм куплен', 'success');
                        var card = document.querySelector(
                            '.cart-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                        );
                        UI.animateRemoveCard(card, function () {
                            if (document.querySelectorAll('.cart-card').length === 0) {
                                window.location.reload();
                            }
                        });
                    })
                    .catch(function (err) {
                        if (typeof rollbackWatch === 'function') rollbackWatch();
                        handleError(err);
                    });
            }
        });
    }

    function onBulkBuy() {
        var cards = document.querySelectorAll('.cart-card');
        if (!cards.length) {
            showFlashMessage('Корзина пуста', 'error');
            return;
        }
        var html = '';
        var total = 0;
        var movieIds = [];
        var movieTitles = {};
        var moviePosters = {};
        cards.forEach(function (card) {
            var id = card.getAttribute('data-movie-id');
            if (id) movieIds.push(id);
            var titleEl = card.querySelector('.media-card-title a');
            var title = titleEl ? titleEl.textContent : 'Фильм';
            if (id) movieTitles[id] = title;
            if (id) {
                var pimg = card.querySelector('.media-card-poster-img');
                moviePosters[id] = pimg ? (pimg.getAttribute('src') || '') : '';
            }
            var price = UI.getCardPrice(card);
            total += price;
            html +=
                '<div class="item"><span>' + escapeHtml(title) +
                '</span><span>' + price.toFixed(0) + ' ₽</span></div>';
        });
        html +=
            '<div class="item total"><span>Итого:</span><span>' +
            total.toFixed(0) + ' ₽</span></div>';

        Modal.open({
            title: 'Подтверждение покупки',
            message: 'Вы покупаете все фильмы из корзины:',
            summaryHtml: html,
            confirmText: 'Купить всё',
            onConfirm: function () {
                var rollbacks = movieIds.map(function (id) {
                    return addWatchButtonOptimistic(
                        id,
                        movieTitles[id] || '',
                        moviePosters[id] || ''
                    );
                });

                CartFeature.buyBulk(movieIds)
                    .then(function (data) {
                        saveFlashAndRedirect(
                            (data && data.message) || 'Покупка прошла успешно',
                            'success',
                            '/'
                        );
                    })
                    .catch(function (err) {
                        rollbacks.forEach(function (r) {
                            if (typeof r === 'function') r();
                        });
                        handleError(err);
                    });
            }
        });
    }

    function register() {
        if (!window.Actions) {
            if (window.console) console.error('[Cart] Actions core not loaded');
            return;
        }
        Actions.register('add-to-cart', onAdd);
        Actions.register('remove-from-cart', onRemove);
        Actions.register('confirm-remove-from-cart', onConfirmRemove);
        Actions.register('clear-cart', onClearCart);
        Actions.register('single-buy', onSingleBuy);
        Actions.register('bulk-buy', onBulkBuy);
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(register);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', register, { once: true });
    } else {
        register();
    }
})();