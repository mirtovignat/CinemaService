(function () {
    'use strict';

    function isMeaningful(v) {
        return v !== null && v !== undefined && v !== '' && v !== 'null' && v !== 'undefined';
    }

    function escapeAttr(v) {
        if (v === null || v === undefined) return '';
        return String(v)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function buildWatchOverlay(movieId, movieTitle, posterUrl, variant) {
        var cls = variant ? ' watch-btn-neon--' + variant : '';
        return '<div class="poster-overlay">' +
            '<button type="button" class="watch-btn-neon' + cls + '" data-action="watch-movie"' +
            ' data-movie-id="' + escapeAttr(movieId) + '"' +
            ' data-movie-title="' + escapeAttr(movieTitle) + '"' +
            ' data-poster-url="' + escapeAttr(posterUrl) + '">' +
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">' +
            '<polygon points="5 3 19 12 5 21 5 3"/></svg>' +
            'Смотреть' +
            '</button></div>';
    }

    function upgradeCatalogCard(card, movieId, movieTitle, posterUrl) {
        var posterLink = card.querySelector('.movie-poster-link');
        if (posterLink && posterLink.getAttribute('data-action') !== 'watch-movie') {
            posterLink.setAttribute('data-action', 'watch-movie');
            posterLink.setAttribute('data-movie-id', movieId);
            posterLink.setAttribute('data-movie-title', movieTitle);
            posterLink.setAttribute('data-poster-url', posterUrl);
            posterLink.setAttribute('href', 'javascript:void(0);');
            if (!posterLink.querySelector('.poster-overlay')) {
                var holder = document.createElement('div');
                holder.innerHTML = buildWatchOverlay(movieId, movieTitle, posterUrl, 'md');
                posterLink.appendChild(holder.firstChild);
            }
        }

        var purchaseBlock = card.querySelector('.purchase-buttons');
        if (purchaseBlock) {
            purchaseBlock.innerHTML = '';
            var boughtLink = document.createElement('a');
            boughtLink.className = 'bought-btn';
            boughtLink.href = '/library?highlight=' + encodeURIComponent(movieId);
            boughtLink.textContent = 'Куплено → К коллекции';
            purchaseBlock.appendChild(boughtLink);
        }
    }

    function upgradeShowPage(movieId, movieTitle, posterUrl) {
        var container = document.querySelector('.movie-details-container');
        if (!container) return;

        var posterWrapper = container.querySelector('.poster-wrapper');
        if (posterWrapper) {
            posterWrapper.setAttribute('data-action', 'watch-movie');
            posterWrapper.setAttribute('data-movie-id', movieId);
            posterWrapper.setAttribute('data-movie-title', movieTitle);
            posterWrapper.setAttribute('data-poster-url', posterUrl);
            if (!posterWrapper.querySelector('.poster-overlay')) {
                var holder = document.createElement('div');
                holder.innerHTML = buildWatchOverlay(movieId, movieTitle, posterUrl, '');
                posterWrapper.appendChild(holder.firstChild);
            }
        }

        var actionButtons = container.querySelector('.action-buttons');
        if (actionButtons) {
            actionButtons.querySelectorAll('.cart-button, .buy-now-btn, .in-cart-btn-details, .to-cart-btn-details, .bought-btn')
                .forEach(function (btn) { btn.remove(); });
            var boughtLink = document.createElement('a');
            boughtLink.className = 'bought-btn';
            boughtLink.href = '/library?highlight=' + encodeURIComponent(movieId);
            boughtLink.textContent = 'Куплено → В коллекцию';
            actionButtons.appendChild(boughtLink);
        }
    }

    function markAsBought(movieId) {
        if (!movieId) return;
        var escaped = CSS.escape(String(movieId));

        document.querySelectorAll(
            '.movie-card[data-movie-id="' + escaped + '"]'
        ).forEach(function (card) {
            upgradeCatalogCard(
                card,
                card.getAttribute('data-movie-id') || movieId,
                card.getAttribute('data-movie-title') || '',
                card.getAttribute('data-poster-url') || ''
            );
        });

        document.querySelectorAll(
            '[data-action="buy-now"][data-movie-id="' + escaped + '"]'
        ).forEach(function (buyBtn) {
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

        if (document.querySelector('.movie-details-container')) {
            var container = document.querySelector('.movie-details-container');
            upgradeShowPage(
                movieId,
                container.getAttribute('data-movie-title') || '',
                ''
            );
        }
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

        var img = modal.querySelector('[data-modal-poster]');
        var posterUrl = el.getAttribute('data-poster-url') || '';
        if (img) {
            if (posterUrl && posterUrl !== 'null' && posterUrl !== 'undefined' && posterUrl.trim() !== '') {
                img.src = posterUrl;
                img.style.display = 'block';
                img.onerror = function () { img.style.display = 'none'; };
            } else {
                img.removeAttribute('src');
                img.style.display = 'none';
            }
        }

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

    window.markAsBought = markAsBought;

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(register);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', register, { once: true });
    } else {
        register();
    }
})();