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

        // Обработчик кнопки фильтров
        var filtersBtn = document.getElementById('headerFiltersBtn');
        if (filtersBtn && filtersBtn.dataset.headerBound !== '1') {
            filtersBtn.dataset.headerBound = '1';
            filtersBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.toggleFiltersModal === 'function') {
                    window.toggleFiltersModal();
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

        var userMenuToggle = document.getElementById('userMenuToggle');
        if (userMenuToggle && userMenuToggle.dataset.headerBound !== '1') {
            userMenuToggle.dataset.headerBound = '1';
            userMenuToggle.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.toggleUserMenu === 'function') window.toggleUserMenu();
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