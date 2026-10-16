(function () {
    'use strict';

    const IMG_SELECTOR = 'img[data-poster-img]';

    function handle(img) {
        if (typeof window.showPlaceholderFor === 'function') {
            window.showPlaceholderFor(img);
            return;
        }
        img.style.display = 'none';
        img.setAttribute('aria-hidden', 'true');
    }

    function bindImage(img) {
        if (!img || img.dataset.posterFallbackBound === '1') return;
        img.dataset.posterFallbackBound = '1';

        if (img.complete && img.naturalWidth === 0 && img.src) {
            handle(img);
            return;
        }

        img.addEventListener('error', function () {
            handle(img);
        }, { once: true });
    }

    function scan(root) {
        (root || document).querySelectorAll(IMG_SELECTOR).forEach(bindImage);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { scan(); }, { once: true });
    } else {
        scan();
    }

    const observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (m) {
            m.addedNodes.forEach(function (node) {
                if (node.nodeType !== 1) return;
                if (node.matches && node.matches(IMG_SELECTOR)) bindImage(node);
                if (node.querySelectorAll) scan(node);
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

    window.PosterFallback = { scan: scan };
})();