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

    function parseLocalDateTime(s) {
        var m = String(s).match(
            /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?$/
        );
        if (!m) return null;
        var d = new Date(
            +m[1], +m[2] - 1, +m[3],
            +m[4], +m[5], m[6] ? +m[6] : 0
        );
        return isNaN(d.getTime()) ? null : d;
    }

    function extractDayKey(raw) {
        if (!raw) return '';
        var s = String(raw).trim();

        if (/Z$/.test(s) || /[+-]\d{2}:\d{2}$/.test(s)) {
            var abs = new Date(s);
            if (!isNaN(abs.getTime())) {
                return abs.getFullYear() + '-' + pad2(abs.getMonth() + 1) + '-' + pad2(abs.getDate());
            }
        }

        var local = parseLocalDateTime(s);
        if (local) {
            return local.getFullYear() + '-' + pad2(local.getMonth() + 1) + '-' + pad2(local.getDate());
        }

        var m2 = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (m2) return m2[1] + '-' + m2[2] + '-' + m2[3];

        var fallback = new Date(s);
        if (isNaN(fallback.getTime())) return '';
        return fallback.getFullYear() + '-' + pad2(fallback.getMonth() + 1) + '-' + pad2(fallback.getDate());
    }

    function keyToDate(key) {
        var p = key.split('-');
        if (p.length !== 3) return null;
        var d = new Date(+p[0], +p[1] - 1, +p[2]);
        return isNaN(d.getTime()) ? null : d;
    }

    function findTail(container) {
        var sentinel = container.querySelector('[data-cursor-sentinel]');
        if (sentinel && sentinel.parentNode === container) return sentinel;
        var loader = container.querySelector('[data-cursor-loader]');
        if (loader && loader.parentNode === container) return loader;
        return null;
    }

    function makeGroupElement(key) {
        var date = keyToDate(key);
        var group = document.createElement('div');
        group.className = 'history-day-group';
        group.setAttribute('data-date', key);

        var label = document.createElement('div');
        label.className = 'history-day-label';
        label.innerHTML = ICON_CALENDAR + '<span class="day-label"></span>';
        label.querySelector('.day-label').textContent = date ? humanize(date) : key;
        group.appendChild(label);

        return group;
    }

    function getGroupKey(group) {
        return group.getAttribute('data-date');
    }

    function collectItemsInOrder(container) {
        var items = Array.prototype.slice.call(
            container.querySelectorAll('.history-item')
        );

        var sort = container.getAttribute('data-cursor-sort') || 'desc';

        items.sort(function (a, b) {
            var ta = a.getAttribute('data-added-at') || '';
            var tb = b.getAttribute('data-added-at') || '';
            if (ta === tb) {
                var ia = a.getAttribute('data-history-id') || '';
                var ib = b.getAttribute('data-history-id') || '';
                if (ia === ib) {
                    return 0;
                }
                return sort === 'asc' ? ia.localeCompare(ib) : ib.localeCompare(ia);
            }
            return sort === 'asc' ? ta.localeCompare(tb) : tb.localeCompare(ta);
        });

        return items;
    }

    function applyTo(container) {
        if (!container) return;
        if (!container.classList) return;
        if (applying) return;

        var isGroupable =
            container.classList.contains('history-container') ||
            container.classList.contains('watched-container') ||
            container.hasAttribute('data-watched-container') ||
            container.hasAttribute('data-group-history');

        if (!isGroupable) return;

        applying = true;
        try {
            var tail = findTail(container);

            var items = collectItemsInOrder(container);

            if (items.length === 0) {
                container.querySelectorAll('.history-day-group').forEach(function (g) {
                    g.remove();
                });
                return;
            }

            var itemsByKey = new Map();
            var orderedKeys = [];

            items.forEach(function (item) {
                var key = extractDayKey(item.getAttribute('data-added-at'));
                if (!key) {
                    key = extractDayKey(new Date().toISOString());
                }
                if (!itemsByKey.has(key)) {
                    itemsByKey.set(key, []);
                    orderedKeys.push(key);
                }
                itemsByKey.get(key).push(item);
            });

            if (orderedKeys.length === 0) {
                return;
            }

            var existingGroups = new Map();
            container.querySelectorAll('.history-day-group').forEach(function (g) {
                var key = getGroupKey(g);
                if (key) {
                    if (existingGroups.has(key)) {
                        g.remove();
                    } else {
                        existingGroups.set(key, g);
                    }
                } else {
                    g.remove();
                }
            });

            var targetGroups = orderedKeys.map(function (key) {
                var group = existingGroups.get(key);
                if (!group) {
                    group = makeGroupElement(key);
                } else {
                    var label = group.querySelector('.day-label');
                    var date = keyToDate(key);
                    if (label) label.textContent = date ? humanize(date) : key;
                }
                return { key: key, element: group, items: itemsByKey.get(key) };
            });

            var neededKeys = new Set(orderedKeys);

            existingGroups.forEach(function (g, key) {
                if (!neededKeys.has(key)) {
                    g.remove();
                }
            });

            var frag = document.createDocumentFragment();
            targetGroups.forEach(function (t) {
                frag.appendChild(t.element);
            });

            if (tail && tail.parentNode === container) {
                container.insertBefore(frag, tail);
            } else {
                container.appendChild(frag);
            }

            targetGroups.forEach(function (t) {
                t.items.forEach(function (item) {
                    if (item.parentNode !== t.element) {
                        t.element.appendChild(item);
                    }
                });
            });
        } finally {
            applying = false;
        }
    }

    window.applyHistoryGrouping = applyTo;
    window.ungroupHistory = function (container) {
        if (!container) return;
        container.querySelectorAll('.history-day-group').forEach(function (g) {
            var children = Array.prototype.slice.call(g.children);
            var tail = findTail(container);
            children.forEach(function (child) {
                if (child.classList && child.classList.contains('history-item')) {
                    if (tail && tail.parentNode === container) {
                        container.insertBefore(child, tail);
                    } else {
                        container.appendChild(child);
                    }
                }
            });
            g.remove();
        });
    };

    function init() {
        document.querySelectorAll('.history-container, .watched-container, [data-watched-container], [data-group-history]').forEach(applyTo);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('load', init);
})();