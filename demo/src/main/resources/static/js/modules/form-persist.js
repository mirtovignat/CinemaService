(function () {
    'use strict';

    var STORAGE_PREFIX = 'formPersist:';
    var SUBMITTED_SUFFIX = ':submitted';

    function storage() {
        try { return window.localStorage; } catch (e) { return null; }
    }

    function buildKey(form) {
        var explicit = form.getAttribute('data-persist');
        if (explicit && explicit !== 'true') {
            return STORAGE_PREFIX + explicit;
        }
        var action = form.getAttribute('action') || '';
        var id = form.id || '';
        var method = (form.getAttribute('method') || 'GET').toUpperCase();
        return STORAGE_PREFIX + method + ':' + action + ':' + id;
    }

    function isPersistable(el) {
        if (!el || !el.name) return false;
        var tag = el.tagName;
        if (tag !== 'INPUT' && tag !== 'TEXTAREA' && tag !== 'SELECT') return false;
        var type = (el.type || '').toLowerCase();
        if (type === 'password' || type === 'hidden' || type === 'file' ||
            type === 'submit' || type === 'button' || type === 'reset' ||
            type === 'image') {
            return false;
        }
        if (el.hasAttribute('data-no-persist')) return false;
        return true;
    }

    function serialize(form) {
        var data = {};
        form.querySelectorAll('input, textarea, select').forEach(function (el) {
            if (!isPersistable(el)) return;
            var type = (el.type || '').toLowerCase();
            if (type === 'checkbox' || type === 'radio') {
                data[el.name] = data[el.name] || {};
                data[el.name][el.value] = el.checked;
            } else {
                data[el.name] = el.value;
            }
        });
        return data;
    }

    function restore(form) {
        var s = storage();
        if (!s) return false;
        var raw = s.getItem(buildKey(form));
        if (!raw) return false;

        var data;
        try { data = JSON.parse(raw); } catch (e) { return false; }
        if (!data || typeof data !== 'object') return false;

        form.querySelectorAll('input, textarea, select').forEach(function (el) {
            if (!isPersistable(el)) return;
            if (!(el.name in data)) return;

            var type = (el.type || '').toLowerCase();
            var stored = data[el.name];

            if (type === 'checkbox' || type === 'radio') {
                if (stored && typeof stored === 'object' && el.value in stored) {
                    el.checked = !!stored[el.value];
                }
            } else if (typeof stored === 'string') {
                el.value = stored;
                if (el._lastValidValue !== undefined) {
                    el._lastValidValue = stored.replace(',', '.');
                }
            }

            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        });
        return true;
    }

    function save(form) {
        var s = storage();
        if (!s) return;
        try { s.setItem(buildKey(form), JSON.stringify(serialize(form))); } catch (e) {}
    }

    function clear(form) {
        var s = storage();
        if (!s) return;
        try { s.removeItem(buildKey(form)); } catch (e) {}
    }

    function has(form) {
        var s = storage();
        if (!s) return false;
        try { return !!s.getItem(buildKey(form)); } catch (e) { return false; }
    }

    function bind(form) {
        if (form.dataset.formPersistBound === '1') return;
        form.dataset.formPersistBound = '1';

        restore(form);

        form.addEventListener('input', function () { save(form); });
        form.addEventListener('change', function () { save(form); });

        form.addEventListener('submit', function () {
            clear(form);
            var s = storage();
            if (s) {
                try { s.setItem(buildKey(form) + SUBMITTED_SUFFIX, '1'); } catch (e) {}
            }
        });
    }

    function init(root) {
        var scope = root || document;
        scope.querySelectorAll('form[data-persist]').forEach(bind);
    }

    window.FormPersist = {
        init: init,
        bind: bind,
        save: save,
        restore: restore,
        clear: clear,
        has: has
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { init(); }, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) init();
    });
})();