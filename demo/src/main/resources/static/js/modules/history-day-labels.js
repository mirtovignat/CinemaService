(function () {
    'use strict';

    var MONTHS_GENITIVE = [
        'января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
        'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'
    ];

    var WEEKDAYS = [
        'Воскресенье', 'Понедельник', 'Вторник', 'Среда',
        'Четверг', 'Пятница', 'Суббота'
    ];

    function startOfDay(d) {
        var x = new Date(d);
        x.setHours(0, 0, 0, 0);
        return x;
    }

    function dayDiff(a, b) {
        return Math.round((startOfDay(a).getTime() - startOfDay(b).getTime()) / 86400000);
    }

    function formatDayMonth(date) {
        return date.getDate() + ' ' + MONTHS_GENITIVE[date.getMonth()];
    }

    function formatFull(date) {
        return formatDayMonth(date) + ' ' + date.getFullYear();
    }

    function weekday(date) {
        return WEEKDAYS[date.getDay()];
    }

    function humanize(dateStr) {
        if (!dateStr) return '';
        var parts = String(dateStr).split('-');
        if (parts.length !== 3) return '';
        var d = new Date(
            parseInt(parts[0], 10),
            parseInt(parts[1], 10) - 1,
            parseInt(parts[2], 10)
        );
        if (isNaN(d.getTime())) return '';

        var today = new Date();
        var diff = dayDiff(today, d);

        if (diff === 0) return 'Сегодня';
        if (diff === 1) return 'Вчера';
        if (diff === 2) return 'Позавчера';
        if (diff >= 3 && diff <= 6) return weekday(d);
        if (diff === 7) return 'Неделю назад';

        if (d.getFullYear() === today.getFullYear()) {
            return formatDayMonth(d);
        }
        return formatFull(d);
    }

    function applyTo(root) {
        var scope = root || document;
        scope.querySelectorAll('.history-day-group[data-date]').forEach(function (group) {
            var raw = group.getAttribute('data-date');
            if (!raw) return;
            var label = group.querySelector('.day-label');
            if (!label) return;
            var text = humanize(raw);
            if (!text) return;
            if (label.textContent === text) return;
            label.textContent = text;
        });
    }

    var observer = null;

    function observe() {
        if (observer || typeof MutationObserver === 'undefined') return;
        observer = new MutationObserver(function (mutations) {
            for (var i = 0; i < mutations.length; i++) {
                var added = mutations[i].addedNodes;
                for (var j = 0; j < added.length; j++) {
                    var node = added[j];
                    if (node.nodeType !== 1) continue;
                    if (node.matches && node.matches('.history-day-group[data-date]')) {
                        applyTo(node.parentNode);
                        return;
                    }
                    if (node.querySelector && node.querySelector('.history-day-group[data-date]')) {
                        applyTo(node);
                        return;
                    }
                }
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    function init() {
        applyTo(document);
        observe();
    }

    window.applyHistoryDayLabels = applyTo;
    window.humanizeHistoryDate = humanize;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();