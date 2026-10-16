(function () {
    'use strict';

    var STORAGE_KEY = 'cinemaSortOrder';

    function getUrlSort() {
        var params = new URLSearchParams(window.location.search);
        var s = (params.get('sort') || '').toLowerCase();
        return (s === 'asc' || s === 'desc') ? s : null;
    }

    function getStoredSort() {
        try {
            var saved = localStorage.getItem(STORAGE_KEY);
            if (saved === 'asc' || saved === 'desc') return saved;
        } catch (e) {}
        return null;
    }

    function isCursorPage() {
        return !!document.querySelector('[data-cursor-container]');
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

    function setSort(order) {
        if (order !== 'asc' && order !== 'desc') return;
        try { localStorage.setItem(STORAGE_KEY, order); } catch (e) {}

        var url = new URL(window.location.href);
        url.searchParams.set('sort', order);
        url.searchParams.delete('cursor');
        window.location.href = url.toString();
    }

    function init() {
        var group = document.getElementById('sortToggleGroup');
        if (!group) return;

        var urlSort = getUrlSort();
        var storedSort = getStoredSort();

        if (urlSort) {
            try { localStorage.setItem(STORAGE_KEY, urlSort); } catch (e) {}
        }

        if (isCursorPage()) {
            var containerSort = getCurrentContainerSort() || urlSort || storedSort || 'desc';
            applyActive(containerSort);
        } else {
            if (!urlSort && storedSort) {
                var url = new URL(window.location.href);
                url.searchParams.set('sort', storedSort);
                url.searchParams.delete('cursor');
                window.location.replace(url.toString());
                return;
            }
            applyActive(urlSort || storedSort || 'desc');
        }

        group.querySelectorAll('.sort-option').forEach(function (btn) {
            if (btn.dataset.sortBound === '1') return;
            btn.dataset.sortBound = '1';
            btn.addEventListener('click', function () {
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
    window.addEventListener('pageshow', function (e) { if (e.persisted) init(); });
})();