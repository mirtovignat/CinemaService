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

    function handleClick(e) {
        var el = e.target.closest && e.target.closest('[data-action]');
        if (!el) return;
        var action = el.getAttribute('data-action');
        if (!action) return;

        var isSubmit = el.tagName === 'BUTTON' && el.getAttribute('type') === 'submit';
        var handled = run(action, el, e);

        if (handled && el.tagName === 'BUTTON' && !isSubmit) {
            e.preventDefault();
        } else if (!handled && el.tagName === 'A' && el.getAttribute('href') === '#') {
            e.preventDefault();
        }
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

    function init() {
        if (initialized) return;
        initialized = true;

        document.addEventListener('click', handleClick);
        document.addEventListener('keydown', handleKeydown);

        register('navigate', handleNavigate);

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