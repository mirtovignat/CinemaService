(function () {
    'use strict';

    var STORAGE_PREFIX = 'cinemaSortOrder';
    var REVIEWS_STORAGE_PREFIX = 'cinemaReviewsSortOrder';

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

    function getReviewsStorageKey() {
        return REVIEWS_STORAGE_PREFIX + ':' + getPageKey();
    }

    function getUrlSort() {
        var params = new URLSearchParams(window.location.search);
        var s = (params.get('sort') || '').toLowerCase();
        return (s === 'asc' || s === 'desc') ? s : null;
    }

    function getStoredSort() {
        try {
            var saved = localStorage.getItem(getStorageKey());
            if (saved === 'asc' || saved === 'desc') return saved;
        } catch (e) {}
        try {
            var saved2 = localStorage.getItem(getReviewsStorageKey());
            if (saved2 === 'ASC') return 'asc';
            if (saved2 === 'DESC') return 'desc';
        } catch (e) {}
        return null;
    }

    function setStoredSort(value) {
        try { localStorage.setItem(getStorageKey(), value); } catch (e) {}
        try {
            localStorage.setItem(getReviewsStorageKey(), value === 'asc' ? 'ASC' : 'DESC');
        } catch (e) {}
    }

    function isCursorPage() {
        return !!document.querySelector('[data-cursor-container]');
    }

    function isReviewsPage() {
        return !!document.getElementById('reviewsBody');
    }

    function applyActive(sort) {
        document.querySelectorAll('#sortToggleGroup .sort-option').forEach(function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-order') === sort);
        });
    }

    function getCurrentContainerSort() {
        var container = document.querySelector('[data-cursor-container]');
        if (!container) return null;
        var s = (container.getAttribute('data-cursor-sort') || '').toLowerCase();
        return (s === 'asc' || s === 'desc') ? s : null;
    }

    function buildUrl(order) {
        var url = new URL(window.location.href);
        if (isReviewsPage()) {
            url.searchParams.set('sort', order === 'asc' ? 'ASC' : 'DESC');
        } else {
            url.searchParams.set('sort', order);
        }
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
        if (order !== 'asc' && order !== 'desc') return;
        setStoredSort(order);
        navigateToOrder(order);
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
            effective = containerSort || urlSort || storedSort || 'desc';
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
            effective = urlSort || storedSort || 'desc';
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