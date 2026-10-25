(function () {
    'use strict';

    function bindHeaderActions() {
        var favBtn = document.getElementById('headerFavouritesBtn');
        if (favBtn && favBtn.dataset.headerBound !== '1') {
            favBtn.dataset.headerBound = '1';
            favBtn.addEventListener('click', function () {
                if (typeof navigateProtected === 'function') {
                    navigateProtected('/favourites');
                }
            });
        }

        var cartBtn = document.getElementById('headerCartBtn');
        if (cartBtn && cartBtn.dataset.headerBound !== '1') {
            cartBtn.dataset.headerBound = '1';
            cartBtn.addEventListener('click', function () {
                if (typeof navigateProtected === 'function') {
                    navigateProtected('/cart');
                }
            });
        }

        var userMenuAddAccount = document.getElementById('userMenuAddAccount');
        if (userMenuAddAccount && userMenuAddAccount.dataset.headerBound !== '1') {
            userMenuAddAccount.dataset.headerBound = '1';
            userMenuAddAccount.addEventListener('click', function () {
                window.location.href = '/login';
            });
        }

        document.querySelectorAll('.sort-option[data-order]').forEach(function (btn) {
            if (btn.dataset.sortBound === '1') return;
            btn.dataset.sortBound = '1';
            btn.addEventListener('click', function () {
                var order = btn.getAttribute('data-order');
                if (order && typeof window.setHistorySortOrder === 'function') {
                    window.setHistorySortOrder(order);
                }
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindHeaderActions, { once: true });
    } else {
        bindHeaderActions();
    }
})();