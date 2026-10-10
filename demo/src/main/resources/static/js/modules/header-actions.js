(function () {
    'use strict';

    function resolveActiveUsername() {
        var el = document.getElementById('currentUserData');
        return el && el.dataset.username ? el.dataset.username : '';
    }

    function bindHeaderActions() {
        var favBtn = document.getElementById('headerFavouritesBtn');
        if (favBtn) {
            favBtn.addEventListener('click', function () {
                if (typeof navigateProtected === 'function') navigateProtected('/favourites');
            });
        }

        var cartBtn = document.getElementById('headerCartBtn');
        if (cartBtn) {
            cartBtn.addEventListener('click', function () {
                if (typeof navigateProtected === 'function') navigateProtected('/cart');
            });
        }

        var userMenuAddAccount = document.getElementById('userMenuAddAccount');
        if (userMenuAddAccount) {
            userMenuAddAccount.addEventListener('click', function () {
                var active = resolveActiveUsername();
                window.location.href = active
                    ? '/login?switch=' + encodeURIComponent(active)
                    : '/login';
            });
        }

        var overlay = document.getElementById('filtersModalOverlay');
        if (overlay) {
            overlay.addEventListener('click', function (e) {
                if (e.target === overlay && typeof toggleFiltersModal === 'function') {
                    toggleFiltersModal(false);
                }
            });
        }

        var closeBtn = document.getElementById('filtersModalCloseBtn');
        if (closeBtn) {
            closeBtn.addEventListener('click', function () {
                if (typeof toggleFiltersModal === 'function') toggleFiltersModal(false);
            });
        }

        var resetBtn = document.getElementById('filtersResetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', function () {
                if (typeof resetFiltersForm === 'function') resetFiltersForm();
            });
        }

        var filterForm = document.getElementById('filterForm');
        if (filterForm) {
            filterForm.addEventListener('submit', function (e) {
                if (typeof validateFilter === 'function' && !validateFilter()) {
                    e.preventDefault();
                }
            });
        }

        document.querySelectorAll('.stepper-btn[data-stepper-target]').forEach(function (btn) {
            var target = btn.getAttribute('data-stepper-target');
            var delta = parseFloat(btn.getAttribute('data-stepper-delta')) || 0;
            var dec = parseInt(btn.getAttribute('data-stepper-decimals'), 10) || 0;

            function start(e) {
                e.preventDefault();
                var minAttr = btn.getAttribute('data-stepper-min');
                var maxAttr = btn.getAttribute('data-stepper-max');
                var minV = (minAttr === '' || minAttr === null) ? null : parseFloat(minAttr);
                var maxV = (maxAttr === '' || maxAttr === null) ? null : parseFloat(maxAttr);
                if (typeof startSteppingFilter === 'function') {
                    startSteppingFilter(target, delta, minV, maxV, dec, btn);
                }
            }
            function stop() {
                if (typeof stopSteppingFilter === 'function') {
                    stopSteppingFilter(btn);
                }
            }

            btn.addEventListener('mousedown', start);
            btn.addEventListener('mouseup', stop);
            btn.addEventListener('mouseleave', stop);
            btn.addEventListener('touchstart', start, { passive: false });
            btn.addEventListener('touchend', stop);
            btn.addEventListener('touchcancel', stop);
        });

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