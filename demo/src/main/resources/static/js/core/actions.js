(function () {
    'use strict';

    var handlers = {};
    var queue = [];
    var initialized = false;

    function register(action, fn) {
        if (!action || typeof fn !== 'function') return;
        if (handlers[action] === fn) return;
        if (handlers[action] && window.console) {
            console.warn('[Actions] overwriting handler:', action);
        }
        handlers[action] = fn;
    }

    function run(action, el, e) {
        var fn = handlers[action];
        if (!fn) {
            if (window.console) console.warn('[Actions] no handler for action:', action);
            return false;
        }
        try {
            var result = fn(el, e);
            return result !== false;
        } catch (err) {
            if (window.console) console.error('[Actions] handler error:', action, err);
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Ошибка выполнения действия', 'error');
            }
            return false;
        }
    }

    function getPosterUrl(el) {
        if (!el) return '';
        var url = el.getAttribute('data-poster-url') || '';
        if (url === 'null' || url === 'undefined') return '';
        return url.trim();
    }

    function redirectToMovie(movieId) {
        if (!movieId) return;
        window.location.href = '/movies/' + encodeURIComponent(movieId);
    }

    function findWatchTarget(e) {
        return e.target.closest(
            '.watch-btn-neon, ' +
            '[data-action="watch-movie"], ' +
            '.poster-link[data-action="watch-movie"], ' +
            '.history-item-poster[data-action="watch-movie"]'
        );
    }

    function handleClick(e) {
        var watchEl = findWatchTarget(e);
        if (watchEl) {
            e.preventDefault();
            e.stopPropagation();
            run('watch-movie', watchEl, e);
            return;
        }

        var actionEl = e.target.closest && e.target.closest('[data-action]');
        if (actionEl) {
            var actionName = actionEl.getAttribute('data-action');
            if (!actionName) return;

            e.preventDefault();
            e.stopPropagation();

            var isSubmit = actionEl.tagName === 'BUTTON' && actionEl.getAttribute('type') === 'submit';
            var handled = run(actionName, actionEl, e);

            if (!handled && actionEl.tagName === 'A' && actionEl.getAttribute('href') === '#') {
                e.preventDefault();
            }
            return;
        }

        var card = e.target.closest('.movie-card, .library-card, .history-item, .cart-card, .favourites-card');
        if (!card) return;

        var movieId = card.getAttribute('data-movie-id');
        var posterUrl = getPosterUrl(card);
        if (!movieId || posterUrl) return;

        if (e.target.closest('a[href]')) return;

        e.preventDefault();
        e.stopPropagation();
        redirectToMovie(movieId);
    }

    function handleKeydown(e) {
        if (e.key !== 'Enter' && e.key !== ' ') return;
        var el = e.target.closest && e.target.closest('[data-action]');
        if (!el) return;
        if (el.tagName === 'BUTTON' || el.tagName === 'A') return;
        e.preventDefault();
        run(el.getAttribute('data-action'), el, e);
    }

    function handleNavigate(el) {
        var href = el.getAttribute('data-href');
        if (href) window.location.href = href;
        return true;
    }

    function initCardClickHandler() {
        document.addEventListener('click', function (e) {
            if (e.target.closest('button, a, input, textarea, select, [data-action]')) {
                return;
            }
            var card = e.target.closest('.movie-card, .library-card, .favourites-card, .cart-card, .history-item');
            if (!card) return;
            var movieId = card.getAttribute('data-movie-id');
            if (!movieId) return;
            redirectToMovie(movieId);
        });
    }

    function init() {
        if (initialized) return;
        initialized = true;

        document.addEventListener('click', handleClick);
        document.addEventListener('keydown', handleKeydown);

        register('navigate', handleNavigate);

        initCardClickHandler();

        while (queue.length) {
            var task = queue.shift();
            try { task(); } catch (err) {
                if (window.console) console.error('[Actions] queued task error', err);
            }
        }
    }

    function ready(fn) {
        if (typeof fn !== 'function') return;
        if (initialized) fn();
        else queue.push(fn);
    }

    window.Actions = {
        register: register,
        run: run,
        ready: ready,
        isReady: function () { return initialized; }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();