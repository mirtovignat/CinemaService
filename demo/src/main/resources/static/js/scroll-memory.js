(function () {
    'use strict';

    if ('scrollRestoration' in history) {
        history.scrollRestoration = 'manual';
    }

    var KEY_PREFIX = 'scrollPos:';
    var isRestoring = false;

    function getKey(url) {
        try {
            var u = new URL(url, window.location.origin);
            return KEY_PREFIX + u.pathname + u.search;
        } catch (e) {
            return null;
        }
    }

    function getScrollableElement() {
        var selectors = [
            'main.catalog-wrapper',
            '.profile-right',
            '.reviews-body',
            'main.reviews-main',
            '.modal-content'
        ];
        for (var i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el) {
                var style = window.getComputedStyle(el);
                if (style.overflowY === 'auto' || style.overflowY === 'scroll') {
                    return el;
                }
            }
        }
        return window;
    }

    function save(url, scrollPos) {
        var key = getKey(url || window.location.href);
        if (!key) return;
        try {
            sessionStorage.setItem(key, JSON.stringify({ y: scrollPos }));
        } catch (e) {}
    }

    function saveCurrent() {
        var target = getScrollableElement();
        var pos = (target === window) ? window.scrollY : target.scrollTop;
        if (pos > 0) {
            save(window.location.href, pos);
        }
    }

    function tryLoadMoreCursorContent() {
        var container = document.querySelector('[data-cursor-container]');
        if (!container) return false;
        if (container.getAttribute('data-cursor-has-more') !== 'true') return false;

        var states = window.__cursorStates || [];
        for (var i = 0; i < states.length; i++) {
            var state = states[i];
            if (state.container === container
                    && !state.fetching
                    && typeof state.fetchNext === 'function') {
                state.fetchNext();
                return true;
            }
        }
        return false;
    }

    function restore() {
        var key = getKey(window.location.href);
        if (!key) return false;

        var raw;
        try {
            raw = sessionStorage.getItem(key);
        } catch (e) {
            return false;
        }
        if (!raw) return false;

        var data;
        try {
            data = JSON.parse(raw);
        } catch (e) {
            return false;
        }

        sessionStorage.removeItem(key);

        if (!data || typeof data.y !== 'number') return false;

        isRestoring = true;
        var targetY = data.y;
        var target = getScrollableElement();
        var stableCount = 0;
        var lastHeight = 0;

        function checkAndScroll() {
            var currentHeight = target === window ? document.documentElement.scrollHeight : target.scrollHeight;

            if (target === window) {
                window.scrollTo(0, targetY);
            } else {
                target.scrollTop = targetY;
            }

            var newScroll = target === window ? window.scrollY : target.scrollTop;
            if (Math.abs(newScroll - targetY) < 5) {
                isRestoring = false;
                return;
            }

            if (currentHeight === lastHeight) {
                stableCount++;
            } else {
                stableCount = 0;
                lastHeight = currentHeight;
            }

            if (stableCount >= 3) {
                if (tryLoadMoreCursorContent()) {
                    stableCount = 0;
                    setTimeout(checkAndScroll, 150);
                    return;
                }
                isRestoring = false;
                return;
            }

            setTimeout(checkAndScroll, 100);
        }

        if (document.readyState === 'complete') {
            setTimeout(checkAndScroll, 300);
        } else {
            window.addEventListener('load', function() {
                setTimeout(checkAndScroll, 300);
            }, { once: true });
        }

        return true;
    }

    window.ScrollMemory = {
        save: save,
        restore: restore,
        saveCurrent: saveCurrent,
        isRestoring: function() { return isRestoring; }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restore, { once: true });
    } else {
        restore();
    }
})();