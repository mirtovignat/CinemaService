(function () {
    'use strict';

    function markAsBought(movieId) {
        var buyButtons = document.querySelectorAll(
            '[data-action="buy-now"][data-movie-id="' + CSS.escape(movieId) + '"]'
        );
        buyButtons.forEach(function (buyBtn) {
            var container = buyBtn.parentNode;
            if (!container) return;

            container.querySelectorAll(
                '.cart-button, .buy-now-btn, .in-cart-btn-details, .to-cart-btn-details'
            ).forEach(function (btn) { btn.remove(); });

            var boughtBtn = document.createElement('a');
            boughtBtn.className = 'bought-btn';
            boughtBtn.href = '/library?highlight=' + encodeURIComponent(movieId);
            boughtBtn.textContent = 'Куплено → В коллекцию';
            container.appendChild(boughtBtn);
        });
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
        if (btn.__purchaseBound) btn.removeEventListener('click', btn.__purchaseBound);
        btn.__purchaseBound = handler;
        btn.addEventListener('click', handler);
    }

    function onBuyNow(el) {
        var movieId = getMovieId(el);
        if (!movieId) return;

        if (typeof window.requireAuth === 'function' && !window.requireAuth()) {
            showFlashMessage('Авторизуйтесь!', 'error', undefined, {
                text: 'Перейти к авторизации',
                url: '/login'
            });
            return;
        }

        var modal = openModal('cartBuyOneModal');
        if (!modal) return;

        var titleEl = modal.querySelector('[data-modal-movie-title]');
        if (titleEl) titleEl.textContent = getMovieTitle(el) || '';

        var priceEl = modal.querySelector('[data-modal-price]');
        if (priceEl) priceEl.textContent = UI.formatPrice(getMoviePrice(el)) + ' ₽';

        bindOneTimeConfirm(modal, function () {
            closeModal('cartBuyOneModal');
            PurchaseFeature.buyOne(movieId)
                .then(function (data) {
                    showFlashMessage((data && data.message) || 'Фильм куплен', 'success');
                    markAsBought(movieId);

                    var cartCard = document.querySelector(
                        '.cart-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                    );
                    if (cartCard) {
                        UI.animateRemoveCard(cartCard, function () {
                            if (document.querySelectorAll('.cart-card').length === 0) {
                                window.location.reload();
                            }
                        });
                    }
                })
                .catch(handleError);
        });
    }

    function register() {
        if (!window.Actions) {
            if (window.console) console.error('[Purchase] Actions core not loaded');
            return;
        }
        Actions.register('buy-now', onBuyNow);
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(register);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', register, { once: true });
    } else {
        register();
    }
})();