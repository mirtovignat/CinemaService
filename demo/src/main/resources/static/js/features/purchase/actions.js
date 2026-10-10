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
                var card = document.querySelector(
                    '.movie-card[data-movie-id="' + CSS.escape(movieId) + '"]'
                );
                var posterUrl = '';
                if (card) {
                    var posterImg = card.querySelector('.movie-poster-img');
                    if (posterImg) posterUrl = posterImg.getAttribute('src') || '';
                } else {
                    var details = document.querySelector('.movie-details-container');
                    if (details && String(details.getAttribute('data-movie-id')) === String(movieId)) {
                        var pimg = details.querySelector('.movie-poster-img');
                        if (pimg) posterUrl = pimg.getAttribute('src') || '';
                    }
                }

                var rollbackWatch = addWatchButtonOptimistic(movieId, movieTitle, posterUrl);

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
                    .catch(function (err) {
                        if (typeof rollbackWatch === 'function') rollbackWatch();
                        handleError(err);
                    });
            }
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