(function () {
    'use strict';

    var MONTHS_GENITIVE = [
        'января','февраля','марта','апреля','мая','июня',
        'июля','августа','сентября','октября','ноября','декабря'
    ];
    var WEEKDAYS = ['Воскресенье','Понедельник','Вторник','Среда','Четверг','Пятница','Суббота'];

    var ICON_CALENDAR =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.5" stroke="currentColor"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>' +
        '<line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/>' +
        '<line x1="3" y1="10" x2="21" y2="10"/></svg>';

    var applying = false;

    function startOfDay(d) {
        var x = new Date(d);
        x.setHours(0, 0, 0, 0);
        return x;
    }

    function dayDiff(a, b) {
        return Math.round((startOfDay(a).getTime() - startOfDay(b).getTime()) / 86400000);
    }

    function parseDate(raw) {
        if (!raw) return null;
        var d = new Date(raw);
        return isNaN(d.getTime()) ? null : d;
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

    function dateKey(date) {
        return date.getFullYear() + '-' +
            String(date.getMonth() + 1).padStart(2, '0') + '-' +
            String(date.getDate()).padStart(2, '0');
    }

    function ungroupAll() {
        var groups = document.querySelectorAll('.history-day-group');
        groups.forEach(function (g) {
            var parent = g.parentNode;
            if (!parent) return;
            while (g.firstChild) {
                parent.insertBefore(g.firstChild, g);
            }
            g.remove();
        });
    }

    function applyTo(container) {
        if (!container) return;
        if (!container.classList || !container.classList.contains('history-container')) return;
        if (applying) return;

        applying = true;
        try {
            ungroupAll();

            var items = Array.prototype.slice.call(container.querySelectorAll('[data-added-at]'));
            if (items.length === 0) return;

            var groups = new Map();
            items.forEach(function (item) {
                var d = parseDate(item.getAttribute('data-added-at'));
                if (!d) return;
                var key = dateKey(d);
                if (!groups.has(key)) groups.set(key, { date: d, items: [] });
                groups.get(key).items.push(item);
            });

            var sort = container.getAttribute('data-cursor-sort') || 'desc';
            var sortedKeys = Array.from(groups.keys()).sort(function (a, b) {
                return sort === 'asc' ? a.localeCompare(b) : b.localeCompare(a);
            });

            var frag = document.createDocumentFragment();
            sortedKeys.forEach(function (key) {
                var g = groups.get(key);
                var group = document.createElement('div');
                group.className = 'history-day-group';
                group.setAttribute('data-date', key);

                var label = document.createElement('div');
                label.className = 'history-day-label';
                label.innerHTML = ICON_CALENDAR + '<span class="day-label"></span>';
                label.querySelector('.day-label').textContent = humanize(g.date);
                group.appendChild(label);

                g.items.forEach(function (item) { group.appendChild(item); });
                frag.appendChild(group);
            });

            var loader = container.querySelector('[data-cursor-loader]');
            if (loader && loader.parentNode === container) {
                container.insertBefore(frag, loader);
            } else {
                container.appendChild(frag);
            }
        } finally {
            applying = false;
        }
    }

    window.applyHistoryGrouping = applyTo;
    window.ungroupHistory = ungroupAll;

    function init() {
        document.querySelectorAll('.history-container').forEach(applyTo);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();