(function () {
    'use strict';

    var ENDPOINT = '/recommendations';
    var LOAD_THRESHOLD = 200;
    var MAX_INITIAL_PAGES = 5;

    var instances = [];

    function buildPlaceholder() {
        if (typeof window.buildPosterPlaceholder === 'function') {
            return window.buildPosterPlaceholder();
        }
        var wrap = document.createElement('div');
        wrap.className = 'movie-poster-placeholder';
        wrap.setAttribute('data-poster-placeholder', '');
        wrap.innerHTML =
            '<div class="poster-placeholder-icon">' +
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" ' +
                     'fill="none" stroke="currentColor" stroke-width="1.6" ' +
                     'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                    '<rect x="3" y="3" width="18" height="18" rx="3"/>' +
                    '<circle cx="9" cy="9" r="2"/>' +
                    '<path d="m21 15-4.35-4.35a1.5 1.5 0 0 0-2.12 0L7 18"/>' +
                '</svg>' +
            '</div>' +
            '<div class="poster-placeholder-text">Нет постера</div>' +
            '<div class="poster-placeholder-sub">Изображение недоступно</div>';
        return wrap;
    }

    function createCard(movie, currentUri) {
        if (!movie || !movie.id) return null;

        var card = document.createElement('div');
        card.className = 'rec-card';
        card.dataset.movieId = String(movie.id);

        var link = document.createElement('a');
        link.className = 'rec-card-link';
        var href = '/movies/' + encodeURIComponent(movie.id);
        if (currentUri) {
            href += '?from=' + encodeURIComponent(currentUri);
        }
        link.href = href;

        var poster = document.createElement('div');
        poster.className = 'rec-poster';
        var posterUrl = movie.posterUrl || movie.poster_url || '';

        if (posterUrl) {
            var img = document.createElement('img');
            img.className = 'rec-poster-img';
            img.setAttribute('data-poster-img', '');
            img.src = posterUrl;
            img.alt = movie.title || '';
            img.addEventListener('error', function () {
                img.style.display = 'none';
                if (!poster.querySelector('[data-poster-placeholder]')) {
                    poster.appendChild(buildPlaceholder());
                }
            }, { once: true });
            poster.appendChild(img);
        } else {
            poster.appendChild(buildPlaceholder());
        }

        var info = document.createElement('div');
        info.className = 'rec-info';
        var title = document.createElement('div');
        title.className = 'rec-title';
        title.textContent = movie.title || 'Без названия';
        var year = document.createElement('div');
        year.className = 'rec-year';
        year.textContent = movie.releaseYear || movie.release_year || '';
        info.appendChild(title);
        info.appendChild(year);

        link.appendChild(poster);
        link.appendChild(info);
        card.appendChild(link);
        return card;
    }

    function mount(section) {
        if (!section || section.dataset.recMounted === '1') return null;
        section.dataset.recMounted = '1';

        var wrapper = section.querySelector('.rec-scroll-wrapper');
        var grid = section.querySelector('[data-recommendations-grid]');
        if (!wrapper || !grid) return null;

        var currentUri = section.getAttribute('data-current-uri') || '';

        var state = {
            section: section,
            wrapper: wrapper,
            grid: grid,
            currentUri: currentUri,
            nextCursor: null,
            hasMore: true,
            loading: false,
            loaded: new Set(),
            excluded: new Set()
        };

        state.appendMovies = function (movies) {
            var added = 0;
            movies.forEach(function (movie) {
                if (!movie || !movie.id) return;
                var id = String(movie.id);
                if (state.excluded.has(id)) return;
                if (state.loaded.has(id)) return;
                var card = createCard(movie, state.currentUri);
                if (!card) return;
                state.loaded.add(id);
                grid.appendChild(card);
                added++;
            });
            return added;
        };

        state.fetchPage = function () {
            if (state.loading || !state.hasMore) return Promise.resolve(false);
            state.loading = true;

            var url = ENDPOINT;
            if (state.nextCursor) {
                url += '?cursor=' + encodeURIComponent(state.nextCursor);
            }

            return fetch(url, {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                cache: 'no-store'
            })
            .then(function (response) { return response.ok ? response.json() : Promise.reject(response); })
            .then(function (page) {
                var movies = Array.isArray(page && page.items) ? page.items : [];
                state.nextCursor = page && page.nextCursor ? page.nextCursor : null;
                state.hasMore = page && page.hasMore === true;
                state.appendMovies(movies);
                if (movies.length === 0 && grid.children.length === 0) {
                    state.hasMore = false;
                    wrapper.style.display = 'none';
                }
                return movies.length > 0;
            })
            .catch(function (error) {
                console.warn('[RecommendationsCarousel]', error);
                state.hasMore = false;
                return false;
            })
            .finally(function () {
                state.loading = false;
            });
        };

        state.hasOverflow = function () {
            return wrapper.scrollWidth > wrapper.clientWidth + 1;
        };

        state.fillUntilOverflow = function () {
            var pages = 0;

            function step() {
                if (!state.hasMore) return Promise.resolve();
                if (pages >= MAX_INITIAL_PAGES) return Promise.resolve();
                if (state.hasOverflow()) return Promise.resolve();
                pages++;
                return state.fetchPage().then(function (got) {
                    if (!got) return;
                    return step();
                });
            }

            return step();
        };

        state.checkEdge = function () {
            if (state.loading || !state.hasMore) return;
            var remaining = wrapper.scrollWidth - wrapper.scrollLeft - wrapper.clientWidth;
            if (remaining < LOAD_THRESHOLD) state.fetchPage();
        };

        state.reload = function () {
            state.nextCursor = null;
            state.hasMore = true;
            state.loading = false;
            state.loaded.clear();
            state.grid.innerHTML = '';
            state.wrapper.style.display = '';
            state.fillUntilOverflow();
        };

        state.removeOptimistic = function (movieId) {
            if (!movieId) return function () {};
            var id = String(movieId);
            state.excluded.add(id);
            state.loaded.add(id);

            var cards = [];
            state.grid.querySelectorAll('.rec-card').forEach(function (card) {
                if (String(card.dataset.movieId) === id) cards.push(card);
            });

            var snapshots = cards.map(function (card) {
                return { html: card.outerHTML, parent: card.parentNode, next: card.nextSibling };
            });

            cards.forEach(function (card) { card.remove(); });

            var rolled = false;
            return function rollback() {
                if (rolled) return;
                rolled = true;
                state.excluded.delete(id);
                state.loaded.delete(id);
                snapshots.forEach(function (snapshot) {
                    var parser = document.createElement('div');
                    parser.innerHTML = snapshot.html;
                    var node = parser.firstElementChild;
                    if (!node || !snapshot.parent) return;
                    if (snapshot.next && snapshot.next.parentNode === snapshot.parent) {
                        snapshot.parent.insertBefore(node, snapshot.next);
                    } else {
                        snapshot.parent.appendChild(node);
                    }
                });
            };
        };

        wrapper.addEventListener('scroll', state.checkEdge, { passive: true });

        if (window.ResizeObserver) {
            var resizeObserver = new ResizeObserver(function () {
                state.fillUntilOverflow().then(function () {
                    state.checkEdge();
                });
            });
            resizeObserver.observe(wrapper);
        }

        state.fillUntilOverflow().then(function () {
            state.checkEdge();
        });

        instances.push(state);
        return state;
    }

    function mountAll(root) {
        (root || document).querySelectorAll('[data-recommendations-carousel]').forEach(mount);
    }

    window.RecommendationsCarousel = { mount: mount, mountAll: mountAll };

    window.removeRecommendationOptimistic = function (movieId) {
        var rollbacks = instances.map(function (state) {
            return state.removeOptimistic(movieId);
        });
        return function () {
            rollbacks.forEach(function (rollback) { rollback(); });
        };
    };

    window.reloadRecommendations = function () {
        instances.forEach(function (state) { state.reload(); });
    };

    window.refreshRecommendationsAfterMutation = function () {
        instances.forEach(function (state) { state.checkEdge(); });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { mountAll(); }, { once: true });
    } else {
        mountAll();
    }

    window.addEventListener('load', function () { mountAll(); });
})();