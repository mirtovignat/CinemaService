(function () {
    'use strict';

    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function fillLocalTimes(root) {
        (root || document).querySelectorAll('.js-local-time[data-utc]').forEach(function (el) {
            var raw = el.getAttribute('data-utc');
            if (!raw) return;
            var d = new Date(raw);
            if (isNaN(d.getTime())) return;
            el.textContent = pad2(d.getHours()) + ':' + pad2(d.getMinutes());
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { fillLocalTimes(); }, { once: true });
    } else {
        fillLocalTimes();
    }

    window.addEventListener('load', function () { fillLocalTimes(); });
    window.addEventListener('pageshow', function (e) {
        if (e.persisted) fillLocalTimes();
    });

    if (window.MutationObserver) {
        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                m.addedNodes.forEach(function (node) {
                    if (node.nodeType !== 1) return;
                    if (node.matches && node.matches('.js-local-time[data-utc]')) {
                        fillLocalTimes(node.parentNode || document);
                    } else if (node.querySelectorAll) {
                        fillLocalTimes(node);
                    }
                });
            });
        });
        if (document.body) {
            observer.observe(document.body, { childList: true, subtree: true });
        } else {
            document.addEventListener('DOMContentLoaded', function () {
                observer.observe(document.body, { childList: true, subtree: true });
            }, { once: true });
        }
    }
})();