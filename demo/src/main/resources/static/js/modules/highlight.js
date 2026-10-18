(function () {
    'use strict';

    var HIGHLIGHT_KEY = 'libraryHighlightId';
    var SCROLL_SPEED = 45;
    var MAX_WAIT_TIME = 12000;

    var userInterrupted = false;
    var userListenersBound = false;

    function bindUserInterrupts() {
        if (userListenersBound) return;
        userListenersBound = true;

        function onInterrupt() {
            userInterrupted = true;
        }

        window.addEventListener('wheel', onInterrupt, { passive: true });
        window.addEventListener('touchmove', onInterrupt, { passive: true });
        window.addEventListener('keydown', function (e) {
            var keys = ['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '];
            if (keys.indexOf(e.key) !== -1) onInterrupt();
        }, { passive: true });
    }

    function isReloadNavigation() {
        try {
            if (performance && performance.getEntriesByType) {
                var nav = performance.getEntriesByType('navigation')[0];
                if (nav && nav.type) return nav.type === 'reload';
            }
        } catch (e) {}
        try {
            if (performance && performance.navigation) {
                return performance.navigation.type === 1;
            }
        } catch (e) {}
        return false;
    }

    function getUrlId() {
        try {
            var params = new URLSearchParams(window.location.search);
            var id = params.get('highlight');
            return id ? String(id).trim() : null;
        } catch (e) { return null; }
    }

    function getStoredId() {
        try { return sessionStorage.getItem(HIGHLIGHT_KEY); } catch (e) { return null; }
    }

    function saveId(id) {
        try { if (id) sessionStorage.setItem(HIGHLIGHT_KEY, id); } catch (e) {}
    }

    function clearId() {
        try { sessionStorage.removeItem(HIGHLIGHT_KEY); } catch (e) {}
    }

    function stripUrl() {
        try {
            var url = new URL(window.location.href);
            if (url.searchParams.has('highlight')) {
                url.searchParams.delete('highlight');
                var newUrl = url.pathname + (url.searchParams.toString() ? '?' + url.searchParams.toString() : '') + url.hash;
                window.history.replaceState({}, '', newUrl);
            }
        } catch (e) {}
    }

    function findCard(id) {
        if (!id) return null;
        var escaped = CSS.escape(id);
        return document.querySelector(
            '.library-card[data-movie-id="' + escaped + '"], ' +
            '.cart-card[data-movie-id="' + escaped + '"], ' +
            '.favourites-card[data-movie-id="' + escaped + '"]'
        );
    }

    function findScrollParent(el) {
        var node = el && el.parentElement;
        while (node && node !== document.documentElement) {
            var style = window.getComputedStyle(node);
            if (/(auto|scroll|overlay)/.test(style.overflowY)) return node;
            node = node.parentElement;
        }
        return window;
    }

    function getScrollTop(scroller) {
        return scroller === window ? window.scrollY : scroller.scrollTop;
    }

    function setScrollTop(scroller, value) {
        if (scroller === window) {
            window.scrollTo(0, value);
        } else {
            scroller.scrollTop = value;
        }
    }

    function getMaxScroll(scroller) {
        if (scroller === window) {
            return document.documentElement.scrollHeight - window.innerHeight;
        }
        return scroller.scrollHeight - scroller.clientHeight;
    }

    function getTargetScrollPosition(card, scroller) {
        var scrollerRect = scroller === window ? { top: 0 } : scroller.getBoundingClientRect();
        var cardRect = card.getBoundingClientRect();
        var headerEl = scroller === window ? null : scroller.querySelector('.page-header-row');
        var headerHeight = headerEl ? headerEl.offsetHeight : 0;
        var scrollerHeight = scroller === window ? window.innerHeight : scroller.clientHeight;

        var cardTopInScroller = cardRect.top - scrollerRect.top;
        var cardHeight = cardRect.height;
        var visibleHeight = scrollerHeight - headerHeight;

        var target = getScrollTop(scroller) + cardTopInScroller - headerHeight - (visibleHeight - cardHeight) / 2;
        return Math.max(0, Math.min(target, getMaxScroll(scroller)));
    }

    function getCursorState() {
        var container = document.querySelector('[data-cursor-container]');
        if (!container) return null;
        var states = window.__cursorStates || [];
        for (var i = 0; i < states.length; i++) {
            if (states[i].container === container) return states[i];
        }
        return null;
    }

    function performHighlight(card) {
        if (!card || !card.isConnected) return;
        card.classList.remove('highlight');
        void card.offsetWidth;
        card.classList.add('highlight');
        setTimeout(function () {
            card.classList.remove('highlight');
        }, 4000);
    }

    function triggerScroll(scroller) {
        if (scroller === window) {
            window.dispatchEvent(new Event('scroll'));
            document.dispatchEvent(new Event('scroll'));
        } else {
            scroller.dispatchEvent(new Event('scroll'));
            window.dispatchEvent(new Event('scroll'));
        }
    }

    function smoothScrollAndSearch(id) {
        var startTime = Date.now();
        var container = document.querySelector('[data-cursor-container]');
        if (!container) return;

        var scroller = findScrollParent(container);
        var currentSpeed = 0;
        var targetSpeed = SCROLL_SPEED;
        var foundCard = null;
        var lastExpectedPos = getScrollTop(scroller);
        var stuckAtBottomTime = 0;

        function step() {
            if (userInterrupted) return;

            if (Date.now() - startTime > MAX_WAIT_TIME) {
                clearId();
                return;
            }

            var realPos = getScrollTop(scroller);
            if (Math.abs(realPos - lastExpectedPos) > 3) {
                userInterrupted = true;
                return;
            }

            if (!foundCard) {
                foundCard = findCard(id);
                if (foundCard) {
                    stuckAtBottomTime = 0;
                }
            }

            if (foundCard) {
                var targetPos = getTargetScrollPosition(foundCard, scroller);
                var distance = targetPos - realPos;

                if (Math.abs(distance) < 2) {
                    setScrollTop(scroller, targetPos);
                    requestAnimationFrame(function () {
                        requestAnimationFrame(function () {
                            performHighlight(foundCard);
                            clearId();
                        });
                    });
                    return;
                }

                var stepDistance = distance * 0.12;
                if (Math.abs(stepDistance) < 1) {
                    stepDistance = distance > 0 ? 1 : -1;
                }
                var newPos = realPos + stepDistance;
                setScrollTop(scroller, newPos);
                lastExpectedPos = newPos;
                requestAnimationFrame(step);
                return;
            }

            var maxPos = getMaxScroll(scroller);
            var atBottom = realPos >= maxPos - 5;

            if (atBottom) {
                if (stuckAtBottomTime === 0) {
                    stuckAtBottomTime = Date.now();
                }

                triggerScroll(scroller);

                var hasMore = container.getAttribute('data-cursor-has-more') === 'true';

                if (hasMore) {
                    var state = getCursorState();
                    if (state && typeof state.fetchNext === 'function' && !state.fetching) {
                        state.fetchNext();
                    }
                    requestAnimationFrame(step);
                    return;
                }

                if (Date.now() - stuckAtBottomTime > 1500) {
                    clearId();
                    return;
                }
                requestAnimationFrame(step);
                return;
            }

            stuckAtBottomTime = 0;
            currentSpeed += (targetSpeed - currentSpeed) * 0.1;
            var newPos2 = Math.min(realPos + currentSpeed, maxPos);
            setScrollTop(scroller, newPos2);
            lastExpectedPos = newPos2;
            triggerScroll(scroller);
            requestAnimationFrame(step);
        }

        requestAnimationFrame(step);
    }

    function runHighlight(id) {
        var card = findCard(id);
        if (card) {
            setTimeout(function () {
                var scroller = findScrollParent(card);
                var target = getTargetScrollPosition(card, scroller);
                var startPos = getScrollTop(scroller);
                var startTime = performance.now();
                var duration = 500;
                var lastExpectedPos = startPos;

                function easeOut(t) {
                    return 1 - Math.pow(1 - t, 3);
                }

                function stepFade() {
                    if (userInterrupted) {
                        performHighlight(card);
                        clearId();
                        return;
                    }

                    var realPos = getScrollTop(scroller);
                    if (Math.abs(realPos - lastExpectedPos) > 3) {
                        userInterrupted = true;
                        performHighlight(card);
                        clearId();
                        return;
                    }

                    var elapsed = performance.now() - startTime;
                    var progress = Math.min(elapsed / duration, 1);
                    var eased = easeOut(progress);
                    var newPos = startPos + (target - startPos) * eased;
                    setScrollTop(scroller, newPos);
                    lastExpectedPos = newPos;

                    if (progress < 1) {
                        requestAnimationFrame(stepFade);
                    } else {
                        requestAnimationFrame(function () {
                            requestAnimationFrame(function () {
                                performHighlight(card);
                                clearId();
                            });
                        });
                    }
                }

                requestAnimationFrame(stepFade);
            }, 300);
            return;
        }

        setTimeout(function () {
            smoothScrollAndSearch(id);
        }, 300);
    }

    function init() {
        bindUserInterrupts();

        var urlId = getUrlId();
        if (urlId) saveId(urlId);
        stripUrl();

        if (isReloadNavigation()) {
            clearId();
            return;
        }

        var targetId = urlId || getStoredId();
        if (!targetId) return;

        runHighlight(targetId);
    }

    window.addEventListener('pageshow', function (e) {
        if (!e.persisted) return;
        var storedId = getStoredId();
        if (!storedId) return;
        userInterrupted = false;
        runHighlight(storedId);
    });

    init();
})();