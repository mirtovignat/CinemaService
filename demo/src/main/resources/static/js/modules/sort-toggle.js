(function () {
    'use strict';

    var STORAGE_PREFIX = 'cinemaSortOrder';

    function getPageKey() {
        var path = window.location.pathname.replace(/\/$/, '');
        if (path === '/rated/history') return '/rated';
        var match = path.match(/^\/rated\/\d+\/reviews/);
        if (match) return '/reviews';
        return path;
    }

    function getStorageKey() {
        return STORAGE_PREFIX + ':' + getPageKey();
    }

    function getUrlSort() {
        var params = new URLSearchParams(window.location.search);
        var s = (params.get('sort') || '').toUpperCase();
        return (s === 'ASC' || s === 'DESC') ? s : null;
    }

    function getStoredSort() {
        try {
            var saved = localStorage.getItem(getStorageKey());
            if (saved) {
                var s = saved.toUpperCase();
                if (s === 'ASC' || s === 'DESC') return s;
            }
        } catch (e) {}
        return null;
    }

    function setStoredSort(value) {
        try { localStorage.setItem(getStorageKey(), value.toUpperCase()); } catch (e) {}
    }

    function isCursorPage() {
        return !!document.querySelector('[data-cursor-container]');
    }

    function applyActive(sort) {
        document.querySelectorAll('#sortToggleGroup .sort-option').forEach(function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-order').toUpperCase() === sort.toUpperCase());
        });
    }

    function getCurrentContainerSort() {
        var container = document.querySelector('[data-cursor-container]');
        if (!container) return null;
        var s = (container.getAttribute('data-cursor-sort') || '').toUpperCase();
        return (s === 'ASC' || s === 'DESC') ? s : null;
    }

    function buildUrl(order) {
        var url = new URL(window.location.href);
        url.searchParams.set('sort', order.toUpperCase()); // <--- Тут именно sort, так как бэкенд ждет sort
        url.searchParams.delete('cursor');
        return url.toString();
    }

    function navigateToOrder(order) {
        var target = buildUrl(order);
        var current = window.location.href;
        if (target === current) return;
        window.location.replace(target);
    }

    function setSort(order) {
        if (!order) return;
        var upperOrder = order.toUpperCase();
        if (upperOrder !== 'ASC' && upperOrder !== 'DESC') return;
        setStoredSort(upperOrder);
        navigateToOrder(upperOrder);
    }

    function init() {
        var group = document.getElementById('sortToggleGroup');
        if (!group) return;

        var urlSort = getUrlSort();
        var storedSort = getStoredSort();

        if (urlSort) {
            setStoredSort(urlSort);
        }

        var effective;

        if (isCursorPage()) {
            var containerSort = getCurrentContainerSort();
            effective = containerSort || urlSort || storedSort || 'DESC';
            setStoredSort(effective);

            if (!urlSort && storedSort && containerSort && storedSort !== containerSort) {
                var target = buildUrl(storedSort);
                if (target !== window.location.href) {
                    window.location.replace(target);
                    return;
                }
            }

            applyActive(effective);
        } else {
            effective = urlSort || storedSort || 'DESC';
            setStoredSort(effective);

            if (!urlSort && storedSort) {
                var target2 = buildUrl(storedSort);
                if (target2 !== window.location.href) {
                    window.location.replace(target2);
                    return;
                }
            }

            applyActive(effective);
        }

        group.querySelectorAll('.sort-option').forEach(function (btn) {
            if (btn.dataset.sortBound === '1') return;
            btn.dataset.sortBound = '1';
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                setSort(btn.getAttribute('data-order'));
            });
        });
    }

    window.setHistorySortOrder = setSort;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) init();
    });
})();