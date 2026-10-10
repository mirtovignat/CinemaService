(function () {
    'use strict';

    function schedule(options) {
        var flag = options.flag;
        var guardName = options.guardName;
        var clear = options.clear;

        if (!flag || !guardName || typeof clear !== 'function') return;

        if (window[flag]) return;
        if (window.FormGuard && window.FormGuard.hasUnsavedState(guardName)) return;

        window[flag] = true;

        clear();
        setTimeout(clear, 50);
        setTimeout(clear, 250);

        setTimeout(function () { window[flag] = false; }, 400);
    }

    function install(options) {
        var guardName = options.guardName;
        var clear = options.clear;
        var scheduleFn = options.schedule || function () {
            schedule({ flag: options.flag, guardName: guardName, clear: clear });
        };

        window.addEventListener('pageshow', scheduleFn);
        window.addEventListener('popstate', scheduleFn);
        window.addEventListener('beforeunload', function () {
            if (window.FormGuard && window.FormGuard.hasUnsavedState(guardName)) return;
            clear();
        });

        if (performance && performance.getEntriesByType) {
            var nav = performance.getEntriesByType('navigation')[0];
            if (nav && (nav.type === 'back_forward' || nav.type === 'reload' || nav.type === 'navigate')) {
                scheduleFn();
            }
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', scheduleFn, { once: true });
        } else {
            scheduleFn();
        }
    }

    window.HardClear = {
        schedule: schedule,
        install: install
    };
})();