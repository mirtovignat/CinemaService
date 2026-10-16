(function () {
    'use strict';

    var MONTHS_GENITIVE = [
        'января','февраля','марта','апреля','мая','июня',
        'июля','августа','сентября','октября','ноября','декабря'
    ];
    var WEEKDAYS = ['Воскресенье','Понедельник','Вторник','Среда','Четверг','Пятница','Суббота'];

    var ICON_CALENDAR =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.5" stroke="currentColor">' +
        '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>' +
        '<line x1="16" y1="2" x2="16" y2="6"/>' +
        '<line x1="8" y1="2" x2="8" y2="6"/>' +
        '<line x1="3" y1="10" x2="21" y2="10"/></svg>';

    var applying = false;

    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function startOfDay(d) {
        var x = new Date(d);
        x.setHours(0, 0, 0, 0);
        return x;
    }

    function dayDiff(a, b) {
        return Math.round((startOfDay(a).getTime() - startOfDay(b).getTime()) / 86400000);
    }

    function humanize(date) {
        var today = new Date();
        var diff = dayDiff(today, date);
        if (diff === 0) return 'Сегодня';
        if (diff === 1) return 'Вчера';
        if (diff === 2) return 'Позавчера';
        if (diff >= 3 && diff <= 6) return WEEKDAYS[date.getDay()];
        if (diff === 7) return 'Неделю назад';

        var dayMonth = date.getDate() + ' ' + MONTHS_GENITIVE[date.getMonth()];
        if (date.getFullYear() === today.getFullYear()) {
            return dayMonth;
        }
        return dayMonth + ' ' + date.getFullYear();
    }

    function extractDayKey(raw) {
        if (!raw) return '';
        var s = String(raw).trim();
        var m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (m) return m[1] + '-' + m[2] + '-' + m[3];
        var d = new Date(s);
        if (isNaN(d.getTime())) return '';
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
    }

    function keyToDate(key) {
        var p = key.split('-');
        if (p.length !== 3) return null;
        var d = new Date(+p[0], +p[1] - 1, +p[2]);
        return isNaN(d.getTime()) ? null : d;
    }

    function getSentinel(container) {
        var direct = container.querySelector(':scope > [aria-hidden="true"]');
        if (direct) return direct;
        var all = container.querySelectorAll('[aria-hidden="true"]');
        return all.length ? all[all.length - 1] : null;
    }

    function collectItems(container) {
        var items = [];
        container.querySelectorAll('.history-item').forEach(function (el) {
            items.push(el);
        });
        return items;
    }

    function removeGroups(container) {
        container.querySelectorAll(':scope > .history-day-group').forEach(function (g) {
            g.remove();
        });
    }

    function detachItems(items) {
        items.forEach(function (item) {
            if (item.parentNode) item.parentNode.removeChild(item);
        });
    }

    function buildGroups(container, items, sentinel) {
        if (!items.length) return;

        var groups = new Map();
        items.forEach(function (item) {
            var key = extractDayKey(item.getAttribute('data-added-at'));
            if (!key) return;
            if (!groups.has(key)) groups.set(key, []);
            groups.get(key).push(item);
        });

        if (groups.size === 0) return;

        var sort = container.getAttribute('data-cursor-sort') || 'desc';
        var keys = Array.from(groups.keys()).sort(function (a, b) {
            return sort === 'asc' ? a.localeCompare(b) : b.localeCompare(a);
        });

        var frag = document.createDocumentFragment();
        keys.forEach(function (key) {
            var date = keyToDate(key);
            var group = document.createElement('div');
            group.className = 'history-day-group';
            group.setAttribute('data-date', key);

            var label = document.createElement('div');
            label.className = 'history-day-label';
            label.innerHTML = ICON_CALENDAR + '<span class="day-label"></span>';
            label.querySelector('.day-label').textContent = date ? humanize(date) : key;
            group.appendChild(label);

            groups.get(key).forEach(function (item) {
                group.appendChild(item);
            });
            frag.appendChild(group);
        });

        if (sentinel && sentinel.parentNode === container) {
            container.insertBefore(frag, sentinel);
        } else {
            container.appendChild(frag);
        }
    }

    function ensureTail(container, sentinel) {
        if (sentinel && sentinel.parentNode === container) {
            container.appendChild(sentinel);
        }
        var loader = container.querySelector('[data-cursor-loader]');
        if (loader && loader.parentNode === container) {
            container.appendChild(loader);
        }
    }

    function applyTo(container) {
        if (!container) return;
        if (!container.classList || !container.classList.contains('history-container')) return;
        if (applying) return;

        applying = true;
        try {
            var sentinel = getSentinel(container);
            var items = collectItems(container);
            removeGroups(container);
            detachItems(items);
            buildGroups(container, items, sentinel);
            ensureTail(container, sentinel);
        } finally {
            applying = false;
        }
    }

    window.applyHistoryGrouping = applyTo;
    window.ungroupHistory = function (container) {
        removeGroups(container || document);
    };

    function init() {
        document.querySelectorAll('.history-container').forEach(applyTo);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();