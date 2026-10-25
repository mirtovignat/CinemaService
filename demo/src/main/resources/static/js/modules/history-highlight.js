(function () {
    'use strict';

    function getScrollTop(scroller) {
        return scroller === window ? window.scrollY : scroller.scrollTop;
    }

    function setScrollTop(scroller, value) {
        if (scroller === window) {
            window.scrollTo({ top: value, behavior: 'smooth' });
        } else {
            scroller.scrollTo({ top: value, behavior: 'smooth' });
        }
    }

    function findScroller(el) {
        var node = el && el.parentElement;
        while (node && node !== document.documentElement) {
            var style = window.getComputedStyle(node);
            if (/(auto|scroll|overlay)/.test(style.overflowY)) return node;
            node = node.parentElement;
        }
        return window;
    }

    function getTargetScrollPosition(el, scroller) {
        var scrollerRect = scroller === window ? { top: 0 } : scroller.getBoundingClientRect();
        var elRect = el.getBoundingClientRect();
        var headerEl = scroller === window ? null : scroller.querySelector('.page-header-row');
        var headerHeight = headerEl ? headerEl.offsetHeight : 0;
        var scrollerHeight = scroller === window ? window.innerHeight : scroller.clientHeight;
        var topInScroller = elRect.top - scrollerRect.top;
        var elHeight = elRect.height;
        var visibleHeight = scrollerHeight - headerHeight;
        var target = getScrollTop(scroller) + topInScroller - headerHeight - (visibleHeight - elHeight) / 2;
        var maxScroll = scroller === window
            ? document.documentElement.scrollHeight - window.innerHeight
            : scroller.scrollHeight - scroller.clientHeight;
        return Math.max(0, Math.min(target, maxScroll));
    }

    function highlightItem(item) {
        if (!item) return;
        item.classList.remove('highlight');
        void item.offsetWidth;
        item.classList.add('highlight');
        setTimeout(function () { item.classList.remove('highlight'); }, 4000);
    }

    function stripHighlightFromUrl() {
        try {
            var url = new URL(window.location.href);
            if (url.searchParams.has('highlight') || url.searchParams.has('watchedAt')) {
                url.searchParams.delete('highlight');
                url.searchParams.delete('watchedAt');
                window.history.replaceState({}, '',
                    url.pathname
                    + (url.searchParams.toString() ? '?' + url.searchParams.toString() : '')
                    + url.hash);
            }
        } catch (e) {}
    }

    function run() {
        var container = document.querySelector('[data-highlight-history-id]');
        if (!container) return;

        var id = container.getAttribute('data-highlight-history-id');
        if (!id) return;

        var selector = '.history-item[data-history-id="' + CSS.escape(id) + '"]';
        var tries = 0;
        var timer = setInterval(function () {
            var item = container.querySelector(selector);
            if (item) {
                clearInterval(timer);
                var scroller = findScroller(item);
                var target = getTargetScrollPosition(item, scroller);
                setScrollTop(scroller, target);
                setTimeout(function () { highlightItem(item); }, 350);
                stripHighlightFromUrl();
                return;
            }
            if (++tries > 40) clearInterval(timer);
        }, 150);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', run, { once: true });
    } else {
        run();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) run();
    });
})();