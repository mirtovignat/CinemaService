(function () {
    'use strict';

    function getHighlightId() {
        var params = new URLSearchParams(window.location.search);
        var id = params.get('highlight');
        if (!id) return null;
        return String(id).trim();
    }

    function stripHighlightFromUrl() {
        try {
            var url = new URL(window.location.href);
            if (url.searchParams.has('highlight')) {
                url.searchParams.delete('highlight');
                var newUrl = url.pathname + (url.searchParams.toString() ? '?' + url.searchParams.toString() : '') + url.hash;
                window.history.replaceState({}, '', newUrl);
            }
        } catch (e) {}
    }

    function scrollToHighlightedCard() {
        var id = getHighlightId();
        if (!id) return;

        var card = document.querySelector(
            '[data-movie-id="' + CSS.escape(id) + '"]'
        );
        if (!card) {
            stripHighlightFromUrl();
            return;
        }

        card.classList.add('highlight');

        function doScroll() {
            var scroller = card.closest('.profile-right')
                || card.closest('main.catalog-wrapper')
                || card.closest('.profile-wrapper');
            if (!scroller) {
                card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return;
            }

            var scrollerRect = scroller.getBoundingClientRect();
            var cardRect = card.getBoundingClientRect();

            var headerEl = scroller.querySelector('.page-header-row');
            var headerHeight = headerEl ? headerEl.getBoundingClientRect().height : 0;

            var visibleHeight = scroller.clientHeight - headerHeight;
            var visibleCenter = headerHeight + (visibleHeight / 2);

            var cardCenterInScroller =
                (cardRect.top - scrollerRect.top) + (cardRect.height / 2);

            var targetScroll = scroller.scrollTop + cardCenterInScroller - visibleCenter;

            var maxScroll = scroller.scrollHeight - scroller.clientHeight;
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));

            scroller.scrollTo({
                top: targetScroll,
                behavior: 'smooth'
            });
        }

        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                doScroll();
            });
        });

        stripHighlightFromUrl();

        setTimeout(function () {
            card.classList.remove('highlight');
        }, 4000);
    }

    function init() {
        if (document.readyState === 'complete') {
            setTimeout(scrollToHighlightedCard, 80);
            return;
        }
        window.addEventListener('load', function () {
            setTimeout(scrollToHighlightedCard, 80);
        });
    }

    init();
})();