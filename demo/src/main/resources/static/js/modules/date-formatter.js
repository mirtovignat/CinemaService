function formatDateTime(raw) {
    if (!raw) return '';
    var d = new Date(raw);
    if (isNaN(d.getTime())) return '';
    var pad = function (n) { return String(n).padStart(2, '0'); };
    return pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear() +
           ', ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

function applyStatusesFromSession() {
    document.querySelectorAll('[data-movie-id]').forEach(function (card) {
        var movieId = card.getAttribute('data-movie-id');
        if (!movieId) return;

        var cartStatus = Store.cart.get(movieId);
        if (cartStatus !== null) {
            var cartBtn = card.querySelector('.cart-button, .in-cart-btn-details, .to-cart-btn-details');
            if (cartBtn) {
                if (cartStatus) {
                    cartBtn.textContent = 'В корзине → Перейти';
                    cartBtn.className = 'cart-button in-cart-btn-details';
                    cartBtn.removeAttribute('data-action');
                    cartBtn.onclick = function () { window.location.href = '/cart'; };
                } else {
                    cartBtn.textContent = 'В корзину';
                    cartBtn.className = 'cart-button to-cart-btn-details';
                    cartBtn.onclick = null;
                    cartBtn.setAttribute('data-action', 'add-to-cart');
                }
            }
        }

        var favStatus = Store.favourite.get(movieId);
        if (favStatus !== null) {
            var favBtn = card.querySelector('.favourite-btn');
            if (favBtn) {
                var span = favBtn.querySelector('span');
                if (span) span.textContent = favStatus ? '♥' : '♡';
                favBtn.classList.toggle('active', favStatus);
                favBtn.setAttribute('data-state', favStatus ? 'remove' : 'add');
            }
        }
    });
    Store.clearAll();
}

window.formatDateTime = formatDateTime;
window.applyStatusesFromSession = applyStatusesFromSession;