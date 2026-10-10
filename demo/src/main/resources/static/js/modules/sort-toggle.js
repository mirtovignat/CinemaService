(function () {
    'use strict';

    var BASE_KEY = 'cinemaHistorySortOrder';
    var currentOrder = 'desc';

    function getStorageKey() {
        return BASE_KEY + '_' + window.location.pathname;
    }

    function getSortableRoots() {
        var roots = [];

        var history = document.querySelector('.history-container');
        if (history) {
            roots.push({
                root: history,
                groupSelector: ':scope > .history-day-group',
                itemSelector: ':scope > .history-item'
            });
        }

        var reviews = document.getElementById('reviewsList');
        if (reviews) {
            roots.push({
                root: reviews,
                groupSelector: null,
                itemSelector: ':scope > .review-card'
            });
        }

        var library = document.querySelector('.library-container');
        if (library) {
            roots.push({
                root: library,
                groupSelector: null,
                itemSelector: ':scope > .library-card'
            });
        }

        return roots;
    }

    function sortByAttr(items, attr, order) {
        var hasData = items.some(function (el) {
            return (el.getAttribute(attr) || '').trim() !== '';
        });
        if (!hasData) return;

        items.sort(function (a, b) {
            var da = (a.getAttribute(attr) || '').trim();
            var db = (b.getAttribute(attr) || '').trim();
            if (da === db) return 0;
            if (da === '') return 1;
            if (db === '') return -1;
            return order === 'asc' ? da.localeCompare(db) : db.localeCompare(da);
        });
    }

    function getItemTime(item) {
        var el = item.querySelector('.history-item-time span:last-child');
        return el ? (el.textContent || '').trim() : '';
    }

    function getItemId(item) {
        var id = item.getAttribute('data-movie-id') || item.getAttribute('data-history-id') || '';
        return parseInt(id, 10) || 0;
    }

    function compareItems(a, b, order) {
        var ta = getItemTime(a);
        var tb = getItemTime(b);
        if (ta !== tb) {
            return order === 'asc' ? ta.localeCompare(tb) : tb.localeCompare(ta);
        }
        var ia = getItemId(a);
        var ib = getItemId(b);
        if (ia !== ib) {
            return order === 'asc' ? ia - ib : ib - ia;
        }
        return 0;
    }

    function applyOrder(order) {
        getSortableRoots().forEach(function (config) {
            var root = config.root;

            if (config.groupSelector) {
                var groups = Array.prototype.slice.call(
                    root.querySelectorAll(config.groupSelector)
                );
                sortByAttr(groups, 'data-date', order);
                groups.forEach(function (group) { root.appendChild(group); });

                root.querySelectorAll('.history-day-group').forEach(function (group) {
                    var items = Array.prototype.slice.call(
                        group.querySelectorAll(':scope > .history-item')
                    );
                    items.sort(function (a, b) { return compareItems(a, b, order); });
                    items.forEach(function (item) { group.appendChild(item); });
                });
            } else {
                var items = Array.prototype.slice.call(
                    root.querySelectorAll(config.itemSelector)
                );
                sortByAttr(items, 'data-date', order);
                items.forEach(function (item) { root.appendChild(item); });
            }
        });
    }

    function updateActiveButton(order) {
        var group = document.getElementById('sortToggleGroup');
        if (!group) return;
        group.querySelectorAll('.sort-option').forEach(function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-order') === order);
        });
    }

    function setHistorySortOrder(order) {
        if (order !== 'asc' && order !== 'desc') return;
        currentOrder = order;
        try { localStorage.setItem(getStorageKey(), order); } catch (e) {}
        applyOrder(order);
        updateActiveButton(order);
    }

    function initSortToggle() {
        if (!document.getElementById('sortToggleGroup')) return;
        var saved = 'desc';
        try { saved = localStorage.getItem(getStorageKey()) || 'desc'; } catch (e) {}
        currentOrder = saved;
        applyOrder(saved);
        updateActiveButton(saved);
    }

    window.setHistorySortOrder = setHistorySortOrder;
    window.__initSortToggle = initSortToggle;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSortToggle, { once: true });
    } else {
        initSortToggle();
    }
})();