(function () {
    'use strict';

    function initElement(el) {
        if (el.dataset.titleTooltipBound === '1') return;

        var fullText = (el.textContent || '').trim();
        if (!fullText) return;

        el.dataset.titleTooltipBound = '1';
        el.setAttribute('title', fullText);
    }

    function initAll() {
        var selectors = [
            '.movie-title',
            '.movie-card-title a',
            '.media-card-title a',
            '.history-item-title',
            '.fav-title'
        ];

        document.querySelectorAll(selectors.join(', ')).forEach(initElement);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }

    window.addEventListener('load', initAll);

    if (window.MutationObserver) {
        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                m.addedNodes.forEach(function (node) {
                    if (node.nodeType === 1) {
                        initAll();
                    }
                });
            });
        });
        if (document.body) {
            observer.observe(document.body, { childList: true, subtree: true });
        }
    }
})();