(function () {
    'use strict';

    var MONTHS_GENITIVE = [
        'января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
        'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'
    ];

    var WEEKDAYS_PREPOSITIONAL = [
        'В воскресенье',
        'В понедельник',
        'Во вторник',
        'В среду',
        'В четверг',
        'В пятницу',
        'В субботу'
    ];

    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function parseServerDate(raw) {
        if (raw === null || raw === undefined) return null;
        var s = String(raw).trim();
        if (!s) return null;

        var hasZone = /[zZ]$/.test(s) || /[+-]\d{2}:?\d{2}$/.test(s);
        var candidate = hasZone ? s : (s.replace(' ', 'T') + 'Z');

        var d = new Date(candidate);
        return isNaN(d.getTime()) ? null : d;
    }

    function startOfDay(d) {
        var x = new Date(d);
        x.setHours(0, 0, 0, 0);
        return x;
    }

    function dayDiff(a, b) {
        return Math.round(
            (startOfDay(a).getTime() - startOfDay(b).getTime()) / 86400000
        );
    }

    function buildTimeSuffix(d) {
        return 'в ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
    }

    function buildAbsoluteDate(d, now) {
        var day = d.getDate();
        var month = MONTHS_GENITIVE[d.getMonth()];

        if (d.getFullYear() !== now.getFullYear()) {
            return day + ' ' + month + ' ' + d.getFullYear() + ' г. ' + buildTimeSuffix(d);
        }
        return day + ' ' + month + ' ' + buildTimeSuffix(d);
    }

    function formatDateTime(raw) {
        var d = parseServerDate(raw);
        if (!d) return '';

        var now = new Date();
        var diffMs = now.getTime() - d.getTime();
        if (diffMs < 0) diffMs = 0;

        var diffSeconds = Math.floor(diffMs / 1000);
        var diffMinutes = Math.floor(diffSeconds / 60);
        var days = dayDiff(now, d);

        if (diffSeconds < 1) return 'Только что';
        if (diffSeconds < 60) return diffSeconds + ' сек. назад';
        if (diffMinutes < 60) return diffMinutes + ' мин. назад';
        if (days === 0) return Math.floor(diffMinutes / 60) + ' ч. назад';
        if (days === 1) return 'Вчера ' + buildTimeSuffix(d);
        if (days === 2) return 'Позавчера ' + buildTimeSuffix(d);
        if (days >= 3 && days <= 6) {
            return WEEKDAYS_PREPOSITIONAL[d.getDay()] + ' ' + buildTimeSuffix(d);
        }
        if (days === 7) return 'Неделю назад ' + buildTimeSuffix(d);

        return buildAbsoluteDate(d, now);
    }

    function applyStatusesFromSession() {
        document.querySelectorAll('[data-movie-id]').forEach(function (card) {
            var movieId = card.getAttribute('data-movie-id');
            if (!movieId) return;

            var cartStatus = Store.cart.get(movieId);
            if (cartStatus !== null) {
                var cartBtn = card.querySelector('.cart-button, .in-cart-btn-details, .to-cart-btn-details');
                if (cartBtn) {
                    if (cartStatus) {
                        cartBtn.textContent = 'В корзине → Перейти';
                        cartBtn.className = 'cart-button in-cart-btn-details';
                        cartBtn.removeAttribute('data-action');
                        cartBtn.onclick = function () { window.location.href = '/cart'; };
                    } else {
                        cartBtn.textContent = 'В корзину';
                        cartBtn.className = 'cart-button to-cart-btn-details';
                        cartBtn.onclick = null;
                        cartBtn.setAttribute('data-action', 'add-to-cart');
                    }
                }
            }

            var favStatus = Store.favourite.get(movieId);
            if (favStatus !== null) {
                var favBtn = card.querySelector('.favourite-btn');
                if (favBtn) {
                    var span = favBtn.querySelector('span');
                    if (span) span.textContent = favStatus ? '♥' : '♡';
                    favBtn.classList.toggle('active', favStatus);
                    favBtn.setAttribute('data-state', favStatus ? 'remove' : 'add');
                }
            }
        });
        Store.clearAll();
    }

    window.formatDateTime = formatDateTime;
    window.parseServerDate = parseServerDate;
    window.applyStatusesFromSession = applyStatusesFromSession;
})();