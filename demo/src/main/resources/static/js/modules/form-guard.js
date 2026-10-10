(function () {
    'use strict';

    var REGISTRY = {};

    function register(name, formProvider) {
        if (!name || typeof formProvider !== 'function') return;
        REGISTRY[name] = formProvider;
    }

    function hasUnsavedState(name) {
        var form = null;
        try { form = REGISTRY[name] ? REGISTRY[name]() : null; } catch (e) { return false; }
        if (!form) return false;
        if (!window.FormPersist) return false;
        return window.FormPersist.has(form);
    }

    function anyUnsavedState(names) {
        if (!Array.isArray(names)) return false;
        for (var i = 0; i < names.length; i++) {
            if (hasUnsavedState(names[i])) return true;
        }
        return false;
    }

    window.FormGuard = {
        register: register,
        hasUnsavedState: hasUnsavedState,
        anyUnsavedState: anyUnsavedState
    };
})();