(function () {
    'use strict';

    function requireAuth(event) {
        return window.requireAuth ? window.requireAuth(event) : false;
    }

    function parsePrice(text) {
        if (text === null || text === undefined) return 0;
        return parseFloat(
            String(text).replace(/[^\d.,]/g, '').replace(',', '.')
        ) || 0;
    }

    function getCardPrice(card) {
        if (!card) return 0;
        var attr = card.getAttribute('data-movie-price');
        if (attr !== null && attr !== '') {
            var n = parseFloat(attr);
            if (!isNaN(n)) return n;
        }
        var priceEl = card.querySelector('.media-card-price, .cart-price, .favourites-price');
        return parsePrice(priceEl ? priceEl.textContent : '');
    }

    function showModal(id) {
        var modal = document.getElementById(id);
        if (modal) modal.classList.add('show');
    }

    function hideModal(id) {
        var modal = document.getElementById(id);
        if (modal) modal.classList.remove('show');
    }

    function updateBadge(url, badgeId) {
        if (typeof isAuthenticated === 'function' && !isAuthenticated()) return;
        var badge = document.getElementById(badgeId);
        if (!badge) return;
        fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
            .then(function (r) {
                var ct = r.headers.get('content-type') || '';
                if (!r.ok || ct.indexOf('text/html') !== -1) throw new Error('unauth');
                return r.text();
            })
            .then(function (count) {
                badge.innerText = count;
                badge.style.display = (count && count !== '0') ? 'flex' : 'none';
            })
            .catch(function () {});
    }

    function updateCartBadge() {
        updateBadge('/cart/count', 'cart-count');
    }

    function updateFavouritesBadge() {
        updateBadge('/favourites/count', 'favourites-count');
    }

    function refreshBadges() {
        updateCartBadge();
        updateFavouritesBadge();
    }

    function animateRemoveCard(card, onDone) {
        if (!card) {
            if (typeof onDone === 'function') onDone();
            return;
        }
        card.style.transition = 'all .3s ease';
        card.style.opacity = '0';
        card.style.transform = 'scale(0.9)';
        setTimeout(function () {
            card.remove();
            if (typeof onDone === 'function') onDone();
        }, 300);
    }

    function renderEmptyState(container, config) {
        if (!container) return;

        var wrapper = document.createElement('div');
        wrapper.className = 'empty-message' + (config.danger ? ' empty-message--danger' : '');

        var iconDiv = document.createElement('div');
        iconDiv.className = 'empty-icon';
        if (config.iconSvg) iconDiv.innerHTML = config.iconSvg;
        wrapper.appendChild(iconDiv);

        var titleEl = document.createElement('h2');
        titleEl.textContent = config.title || '';
        wrapper.appendChild(titleEl);

        var textEl = document.createElement('p');
        textEl.textContent = config.text || '';
        wrapper.appendChild(textEl);

        var link = document.createElement('a');
        link.className = 'browse-movies-btn';
        link.href = config.linkUrl || '/';
        link.textContent = config.linkText || 'Перейти в главное меню';
        wrapper.appendChild(link);

        container.innerHTML = '';
        container.appendChild(wrapper);
    }

    function confirmAction(options) {
        var btn = options.button;
        var originalText = btn ? btn.textContent : '';
        if (btn) {
            btn.disabled = true;
            if (options.loadingText) btn.textContent = options.loadingText;
        }
        return fetch(options.url, {
            method: options.method || 'POST',
            headers: options.headers || { 'X-Requested-With': 'XMLHttpRequest' },
            body: options.body || null
        })
        .then(handleResponse)
        .then(function (data) {
            if (typeof options.onSuccess === 'function') options.onSuccess(data);
            return data;
        })
        .catch(function (err) {
            if (typeof options.onError === 'function') options.onError(err);
            throw err;
        })
        .finally(function () {
            if (btn) {
                btn.disabled = false;
                if (originalText) btn.textContent = originalText;
            }
        });
    }

    window.UI = {
        requireAuth: requireAuth,
        parsePrice: parsePrice,
        getCardPrice: getCardPrice,
        showModal: showModal,
        hideModal: hideModal,
        updateBadge: updateBadge,
        updateCartBadge: updateCartBadge,
        updateFavouritesBadge: updateFavouritesBadge,
        refreshBadges: refreshBadges,
        animateRemoveCard: animateRemoveCard,
        renderEmptyState: renderEmptyState,
        confirmAction: confirmAction
    };
})();