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

    function findBulkBuyButton() {
        return document.querySelector('[data-action="bulk-buy"]');
    }

    function findClearCartButton() {
        return document.querySelector('[data-action="clear-cart"]');
    }

    function findBulkPriceLabel() {
        return document.getElementById('bulkTotalPrice');
    }

    function renderBulkSummary(summary) {
        var bulkBtn = findBulkBuyButton();
        var clearBtn = findClearCartButton();
        var priceEl = findBulkPriceLabel();

        if (!summary || !summary.count) {
            if (bulkBtn) bulkBtn.style.display = 'none';
            if (clearBtn) clearBtn.style.display = 'none';
            if (priceEl) priceEl.textContent = '0 ₽';
            return;
        }

        if (bulkBtn) bulkBtn.style.display = '';
        if (clearBtn) clearBtn.style.display = '';

        if (priceEl) {
            priceEl.textContent = summary.count + ' шт. · ' + UI.formatPrice(summary.total) + ' ₽';
        }
    }

    function refreshBulkSummary() {
        if (!window.API || !API.cart || typeof API.cart.summary !== 'function') {
            return Promise.resolve();
        }
        return API.cart.summary()
            .then(function (summary) {
                renderBulkSummary(summary);
                return summary;
            })
            .catch(function () {
                return null;
            });
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
            refreshBulkSummary();
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
                    refreshBulkSummary();
                    if (document.querySelectorAll('.cart-card').length === 0) {
                        window.location.reload();
                    }
                });
            } else {
                replaceWithAddButton(el, movieId);
                refreshBulkSummary();
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
                        refreshBulkSummary();
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
                        refreshBulkSummary();
                        if (document.querySelectorAll('.cart-card').length === 0) {
                            window.location.reload();
                        }
                    });
                })
                .catch(handleError);
        });
    }

    async function onBulkBuy() {
        if (!window.requireAuth()) {
            showFlashMessage('Авторизуйтесь!', 'error', undefined, {
                text: 'Перейти к авторизации',
                url: '/login'
            });
            return;
        }

        var summary;
        try {
            summary = await API.cart.summary();
        } catch (err) {
            handleError(err);
            return;
        }

        if (!summary || !summary.count) {
            showFlashMessage('Корзина пуста', 'warning');
            return;
        }

        var html =
            '<div class="item"><span>Фильмов к покупке:</span><span>' +
                summary.count + ' шт.</span></div>' +
            '<div class="item total"><span>Итого:</span><span>' +
                UI.formatPrice(summary.total) + ' ₽</span></div>';

        var modal = openModal('cartBuyBulkModal');
        if (!modal) return;

        setModalSummary(modal, html);

        bindOneTimeConfirm(modal, function () {
            closeModal('cartBuyBulkModal');
            CartFeature.buyBulk()
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