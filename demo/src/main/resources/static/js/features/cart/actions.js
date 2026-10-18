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
                try { sessionStorage.setItem('topUpAmount', shortage.toFixed(2)); } catch (e) {}
            }
            return { text: 'Перейти к пополнению', url: '/wallet/top-up' };
        }
        return null;
    }

    function handleError(err) {
        showFlashMessage(errorMessage(err), 'error', undefined, buildActionForError(err));
    }

    function setModalTitle(modal, title) {
        if (!modal) return;
        var el = modal.querySelector('[data-modal-movie-title]');
        if (el) el.textContent = title || '';
    }

    function setModalPrice(modal, price) {
        if (!modal) return;
        var el = modal.querySelector('[data-modal-price]');
        if (el) el.textContent = price ? UI.formatPrice(price) + ' ₽' : '';
    }

    function setModalPoster(modal, url) {
        if (!modal) return;
        var img = modal.querySelector('[data-modal-poster]');
        if (!img) return;
        if (url && url !== 'null' && url !== 'undefined' && url.trim() !== '') {
            img.src = url;
            img.style.display = 'block';
            img.onerror = function() {
                img.style.display = 'none';
            };
        } else {
            img.removeAttribute('src');
            img.style.display = 'none';
        }
    }

    function setModalSummary(modal, html) {
        if (!modal) return;
        var el = modal.querySelector('[data-modal-summary]');
        if (el) el.innerHTML = html || '';
    }

    function getConfirmButton(modal) {
        return modal ? modal.querySelector('[data-modal-confirm]') : null;
    }

    function bindOneTimeConfirm(modal, handler) {
        if (!modal) return;
        var btn = getConfirmButton(modal);
        if (!btn) return;
        if (btn.__cartBound) {
            btn.removeEventListener('click', btn.__cartBound);
        }
        btn.__cartBound = handler;
        btn.addEventListener('click', handler);
    }

    function openModal(id) {
        return Modal.openById(id);
    }

    function closeModal(id) {
        Modal.closeById(id);
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
        if (!movieId) return;

        var modal = openModal('cartRemoveModal');
        if (!modal) return;

        setModalTitle(modal, getMovieTitle(el));

        bindOneTimeConfirm(modal, function () {
            closeModal('cartRemoveModal');
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
        });
    }

    function onClearCart() {
        var modal = openModal('cartClearModal');
        if (!modal) return;

        bindOneTimeConfirm(modal, function () {
            closeModal('cartClearModal');
            CartFeature.clear()
                .then(function (data) {
                    saveFlashAndRedirect(
                        (data && data.message) || 'Корзина очищена',
                        'success',
                        '/cart'
                    );
                })
                .catch(handleError);
        });
    }

    function onSingleBuy(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;

        var modal = openModal('cartBuyOneModal');
        if (!modal) return;

        setModalTitle(modal, getMovieTitle(el));
        setModalPrice(modal, getMoviePrice(el));
        setModalPoster(modal, el.getAttribute('data-poster-url') || '');

        bindOneTimeConfirm(modal, function () {
            closeModal('cartBuyOneModal');

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
                .catch(handleError);
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
        cards.forEach(function (card) {
            var id = card.getAttribute('data-movie-id');
            if (id) movieIds.push(id);
            var titleEl = card.querySelector('.media-card-title a');
            var title = titleEl ? titleEl.textContent : 'Фильм';
            var price = UI.getCardPrice(card);
            total += price;
            html +=
                '<div class="item"><span>' + escapeHtml(title) +
                '</span><span>' + UI.formatPrice(price) + ' ₽</span></div>';
        });
        html +=
            '<div class="item total"><span>Итого:</span><span>' +
            UI.formatPrice(total) + ' ₽</span></div>';

        var modal = openModal('cartBuyBulkModal');
        if (!modal) return;

        setModalSummary(modal, html);

        bindOneTimeConfirm(modal, function () {
            closeModal('cartBuyBulkModal');
            CartFeature.buyBulk(movieIds)
                .then(function (data) {
                    saveFlashAndRedirect(
                        (data && data.message) || 'Покупка прошла успешно',
                        'success',
                        '/'
                    );
                })
                .catch(handleError);
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