(function() {
    'use strict';

    function formatDateLabel(dateText) {
        if (!dateText) return dateText;

        const parts = dateText.trim().split(' ');
        if (parts.length < 3) return dateText;

        const day = parseInt(parts[0]);
        const monthNames = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
                           'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
        const monthIndex = monthNames.indexOf(parts[1]);
        const year = parseInt(parts[2]);

        if (isNaN(day) || monthIndex === -1 || isNaN(year)) return dateText;

        const date = new Date(year, monthIndex, day);
        date.setHours(0, 0, 0, 0);

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const diffDays = Math.floor((today - date) / (1000 * 60 * 60 * 24));
        const daysOfWeek = ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
        const currentYear = new Date().getFullYear();

        let displayText = '';

        if (diffDays === 0) {
            displayText = 'Сегодня';
        } else if (diffDays === 1) {
            displayText = 'Вчера';
        } else if (diffDays === 2) {
            displayText = 'Позавчера';
        } else if (diffDays < 7 && diffDays > 0) {
            displayText = daysOfWeek[date.getDay()];
        } else if (diffDays >= 7 && diffDays < 14) {
            displayText = 'Неделю назад';
        } else if (year === currentYear) {
            displayText = day + ' ' + monthNames[monthIndex];
        } else {
            displayText = day + ' ' + monthNames[monthIndex] + ' ' + year;
        }

        return displayText;
    }

    function formatAllDates(selector) {
        selector = selector || '.history-day-label .day-label';
        document.querySelectorAll(selector).forEach(function(el) {
            const originalText = el.textContent.trim();
            const formatted = formatDateLabel(originalText);
            if (formatted !== originalText) {
                el.textContent = formatted;
            }
        });
    }

    if (typeof window !== 'undefined') {
        window.formatDateLabel = formatDateLabel;
        window.formatAllDates = formatAllDates;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            formatAllDates();
        });
    } else {
        formatAllDates();
    }

    if (typeof document !== 'undefined') {
        document.addEventListener('DOMContentLoaded', function() {
            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                        setTimeout(function() {
                            formatAllDates();
                        }, 100);
                    }
                });
            });

            const targetNode = document.querySelector('.history-container');
            if (targetNode) {
                observer.observe(targetNode, { childList: true, subtree: true });
            }
        });
    }
})();