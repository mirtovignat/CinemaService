(function () {
    'use strict';

    var instances = [];
    var POLL_INTERVAL_MS = 300;
    var EDGE_MARGIN_PX = 600;

    var URL_ATTRS = {
        href: true,
        src: true,
        action: true,
        'data-href': true,
        'data-poster-url': true
    };

    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function normalizeAddedAt(value) {
        if (!value) return '';
        var s = String(value).trim();
        var m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/);
        if (m) return m[1] + 'T' + m[2];
        var d = new Date(s);
        if (isNaN(d.getTime())) return '';
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) +
               'T' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
    }

    function formatTimeHHmm(value) {
        if (value === null || value === undefined || value === '') return '';
        var s = String(value).trim();
        var m = s.match(/[T ](\d{2}):(\d{2})/);
        if (m) return m[1] + ':' + m[2];
        var d = new Date(s);
        if (isNaN(d.getTime())) return '';
        return pad2(d.getHours()) + ':' + pad2(d.getMinutes());
    }

    function formatTimeHHmmss(value) {
        if (value === null || value === undefined || value === '') return '';
        var s = String(value).trim();
        var m = s.match(/[T ](\d{2}):(\d{2}):(\d{2})/);
        if (m) return m[1] + ':' + m[2] + ':' + m[3];
        var d = new Date(s);
        if (isNaN(d.getTime())) return '';
        return pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
    }

    function interpolateTemplate(tpl, item, isUrl) {
        return String(tpl).replace(/\{(\w+)\}/g, function (_, key) {
            var v = item[key];
            if (v === undefined || v === null) return '';
            var s = String(v);
            return isUrl ? encodeURIComponent(s) : s;
        });
    }

    function collectTargets(root, selector) {
        var list = [];
        if (root.matches && root.matches(selector)) list.push(root);
        root.querySelectorAll(selector).forEach(function (el) {
            list.push(el);
        });
        return list;
    }

    function extractYear(value) {
        var s = String(value === null || value === undefined ? '' : value);
        var m = s.match(/\d{4}/);
        return m ? m[0] : '';
    }

    function formatRatingValue(value) {
        if (value === null || value === undefined || value === '') return '';
        var n = parseFloat(String(value).replace(',', '.'));
        if (isNaN(n)) return String(value);
        return n.toFixed(1).replace('.', ',');
    }

    function formatPriceWithCurrency(value) {
        var n = parseFloat(String(value).replace(',', '.'));
        if (isNaN(n)) return String(value);
        if (window.UI && typeof window.UI.formatPrice === 'function') {
            return window.UI.formatPrice(n) + ' ₽';
        }
        return n.toFixed(2).replace('.', ',') + ' ₽';
    }

    function formatPricePlain(value) {
        var n = parseFloat(String(value).replace(',', '.'));
        if (isNaN(n)) return String(value);
        if (window.UI && typeof window.UI.formatPrice === 'function') {
            return window.UI.formatPrice(n);
        }
        return n.toFixed(2).replace('.', ',');
    }

    function resolveAddedAt(item) {
        return item.addedAt || item.watchedAt || item.added_at || item.watched_at || '';
    }

    function normalizePoster(value) {
        if (value === null || value === undefined) return '';
        var s = String(value).trim();
        if (s === 'null' || s === 'undefined') return '';
        return s;
    }

    function applyDataAttrs(root, item) {
        var itemPosterUrl = normalizePoster(item.posterUrl != null ? item.posterUrl : item.poster_url);

        collectTargets(root, '[data-attr]').forEach(function (el) {
            var spec = el.getAttribute('data-attr') || '';
            spec.split(';').forEach(function (pair) {
                var idx = pair.indexOf(':');
                if (idx === -1) return;
                var attr = pair.slice(0, idx).trim();
                var key  = pair.slice(idx + 1).trim();
                if (!attr || !key) return;

                if (attr === 'data-poster-url') {
                    el.setAttribute(attr, itemPosterUrl);
                    return;
                }

                var value;
                if (key.indexOf('{') !== -1) {
                    value = interpolateTemplate(key, item, !!URL_ATTRS[attr]);
                } else if (key === 'addedAt' || key === 'watchedAt') {
                    value = resolveAddedAt(item);
                } else {
                    value = item[key];
                }
                if (value === undefined || value === null) return;

                if (attr === 'data-added-at') {
                    value = normalizeAddedAt(value);
                }

                el.setAttribute(attr, String(value));
            });
        });

        collectTargets(root, '[data-field]').forEach(function (el) {
            var key = el.getAttribute('data-field');
            var value = item[key];

            if (key === 'releaseYear') {
                value = extractYear(item.releaseDate || item.release_year || item.year || '');
            }

            if (key === 'addedAtTime') {
                value = formatTimeHHmm(resolveAddedAt(item));
            }

            if (key === 'addedAt' || key === 'watchedAt') {
                value = formatTimeHHmmss(resolveAddedAt(item));
            }

            if (key === 'price') {
                if (value !== undefined && value !== null) {
                    value = formatPriceWithCurrency(value);
                }
            } else if (key === 'priceSnapshot') {
                if (value !== undefined && value !== null) {
                    value = formatPricePlain(value);
                }
            }

            if (key === 'rating' || key === 'ratingValue') {
                if (value !== undefined && value !== null) {
                    value = formatRatingValue(value);
                }
            }

            if (key === 'releaseDate') {
                value = extractYear(value);
            }

            if (key === 'review') {
                var text = (value === null || value === undefined) ? '' : String(value).trim();
                var empty = text === '';
                value = empty ? 'Пользователь не оставил отзыва' : text;
                el.classList.toggle('history-item-meta-value--muted', empty);
            }

            if (value === undefined || value === null) return;

            if (el.tagName === 'IMG') {
                el.setAttribute('src', String(value));
                el.setAttribute('data-poster-img', '');
            } else {
                el.textContent = String(value);
            }
        });

        var addedRaw = resolveAddedAt(item);
        if (addedRaw && !root.getAttribute('data-added-at')) {
            var normalized = normalizeAddedAt(addedRaw);
            if (normalized) root.setAttribute('data-added-at', normalized);
        }

        if (item.id !== undefined && item.id !== null && !root.getAttribute('data-movie-id')) {
            root.setAttribute('data-movie-id', String(item.id));
        }

        if (!root.hasAttribute('data-poster-url')) {
            root.setAttribute('data-poster-url', itemPosterUrl);
        }

        var posterContainer = root.querySelector('.history-item-poster');
        if (posterContainer && !posterContainer.hasAttribute('data-poster-url')) {
            posterContainer.setAttribute('data-poster-url', itemPosterUrl);
        }

        root.querySelectorAll('img[data-poster-img]').forEach(function (img) {
            var container = img.closest(
                '.media-card-poster-link, .movie-poster-link, .poster-wrapper, ' +
                '.history-item-poster, .fav-poster, .rec-poster'
            ) || img.parentNode;

            var placeholder = container ? container.querySelector('[data-poster-placeholder]') : null;
            var wrapper = container ? container.querySelector('[data-poster-placeholder-wrapper]') : null;
            var link = img.closest('a');

            var posterUrl = img.getAttribute('src') || itemPosterUrl;
            var empty = !posterUrl || posterUrl === 'null';

            if (!empty) {
                img.setAttribute('src', String(posterUrl));
                img.style.display = '';
                if (placeholder) placeholder.classList.add('hidden');
                if (wrapper) wrapper.style.display = 'none';
                if (link) {
                    link.style.display = '';
                    link.style.pointerEvents = '';
                }
            } else {
                img.style.display = 'none';
                if (placeholder) {
                    placeholder.classList.remove('hidden');
                    placeholder.style.display = '';
                }
                if (wrapper) {
                    wrapper.style.display = 'block';
                }
                if (link) {
                    link.style.display = '';
                    link.style.opacity = '0';
                    link.style.pointerEvents = 'none';
                }
            }
        });
    }

    function createCardFromTemplate(templateId, item) {
        var tpl = document.getElementById(templateId);
        if (!tpl) return null;
        var node = tpl.content.firstElementChild.cloneNode(true);
        applyDataAttrs(node, item);
        return node;
    }

    function findScrollParents(el) {
        var result = [];
        var node = el && el.parentElement;
        while (node && node !== document.documentElement) {
            var style = window.getComputedStyle(node);
            if (/(auto|scroll|overlay)/.test(style.overflowY)) {
                result.push(node);
            }
            node = node.parentElement;
        }
        result.push(window);
        return result;
    }

    function isSentinelVisible(state) {
        var s = state.sentinel;
        if (!s || !s.isConnected) return false;

        var rect = s.getBoundingClientRect();

        var rootRect;
        if (state.scrollRoot && state.scrollRoot.isConnected) {
            rootRect = state.scrollRoot.getBoundingClientRect();
        } else {
            rootRect = { top: 0, bottom: window.innerHeight || document.documentElement.clientHeight };
        }

        if (rect.top - rootRect.bottom < EDGE_MARGIN_PX) return true;

        var doc = document.documentElement;
        var winBottom = window.innerHeight || doc.clientHeight;
        if (rect.top - winBottom < EDGE_MARGIN_PX) return true;

        return false;
    }

    function mount(container) {
        if (!container || container.dataset.cursorMounted === '1') return null;
        container.dataset.cursorMounted = '1';

        var endpoint   = container.getAttribute('data-cursor-endpoint');
        var templateId = container.getAttribute('data-cursor-template');
        var sortAttr   = container.getAttribute('data-cursor-sort') || 'DESC';
        var sort       = sortAttr.toUpperCase() === 'ASC' ? 'ASC' : 'DESC';
        var nextCursor = container.getAttribute('data-cursor-next') || null;
        var hasMore    = container.getAttribute('data-cursor-has-more') === 'true';

        if (!endpoint || !templateId) return null;

        var state = {
            container: container,
            endpoint: endpoint,
            templateId: templateId,
            sort: sort,
            nextCursor: nextCursor,
            hasMore: hasMore,
            fetching: false,
            sentinel: null,
            loader: null,
            scrollRoot: null,
            scrollParents: [],
            pollTimer: null,
            scrollListeners: []
        };

        function isGroupableHistoryContainer() {
            if (!container.classList) return false;
            if (container.classList.contains('watched-container')) return true;
            if (container.hasAttribute('data-watched-container')) return true;
            if (container.classList.contains('history-container')) return true;
            return false;
        }

        function ensureSentinel() {
            var s = container.querySelector('[data-cursor-sentinel]');
            if (!s) {
                s = document.createElement('div');
                s.setAttribute('aria-hidden', 'true');
                s.setAttribute('data-cursor-sentinel', '');
                s.style.height = '1px';
                s.style.width = '100%';
                s.style.pointerEvents = 'none';
            }
            state.sentinel = s;
            return s;
        }

        function ensureTail() {
            var s = ensureSentinel();
            var loader = container.querySelector('[data-cursor-loader]');

            if (s.parentNode !== container) {
                container.appendChild(s);
            }
            if (loader) {
                if (loader.parentNode !== container) {
                    container.appendChild(loader);
                }
                if (s.nextElementSibling !== loader) {
                    container.appendChild(s);
                    container.appendChild(loader);
                }
                state.loader = loader;
            } else {
                if (container.lastElementChild !== s) {
                    container.appendChild(s);
                }
            }
        }

        function detachScrollListeners() {
            state.scrollListeners.forEach(function (pair) {
                if (pair.node === window) {
                    window.removeEventListener('scroll', pair.handler);
                } else {
                    pair.node.removeEventListener('scroll', pair.handler);
                }
            });
            state.scrollListeners = [];
        }

        function rebindScrollParents() {
            detachScrollListeners();
            state.scrollParents = findScrollParents(container);

            state.scrollParents.forEach(function (node) {
                var handler = function () { state.checkEdge(); };
                if (node === window) {
                    window.addEventListener('scroll', handler, { passive: true });
                } else {
                    node.addEventListener('scroll', handler, { passive: true });
                }
                state.scrollListeners.push({ node: node, handler: handler });
            });

            state.scrollRoot = state.scrollParents.length > 1
                ? state.scrollParents[state.scrollParents.length - 2]
                : null;
        }

        state.fetchNext = function () {
            if (state.fetching || !state.hasMore) return Promise.resolve(false);
            state.fetching = true;

            ensureTail();

            var params = new URLSearchParams();
            params.set('sort', state.sort);
            if (state.nextCursor) params.set('cursor', state.nextCursor);

            return fetch(state.endpoint + '?' + params.toString(), {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                redirect: 'manual',
                credentials: 'same-origin',
                cache: 'no-store'
            })
            .then(function (r) {
                if (r.type === 'opaqueredirect' || r.status === 0) {
                    window.location.href = '/login';
                    return Promise.reject(new Error('unauth'));
                }
                if (r.redirected) {
                    window.location.href = r.url;
                    return Promise.reject(new Error('redirect'));
                }
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (response) {
                var cursorPageResponse = (response && response.cursorPageResponse)
                    ? response.cursorPageResponse
                    : response;
                var items = Array.isArray(cursorPageResponse && cursorPageResponse.items)
                    ? cursorPageResponse.items
                    : [];

                var sentinel = ensureSentinel();
                var frag = document.createDocumentFragment();
                items.forEach(function (item) {
                    var card = createCardFromTemplate(state.templateId, item);
                    if (card) frag.appendChild(card);
                });
                container.insertBefore(frag, sentinel);

                state.nextCursor = cursorPageResponse.nextCursor || null;
                state.hasMore = !!cursorPageResponse.hasMore;
                container.setAttribute('data-cursor-next', state.nextCursor || '');
                container.setAttribute('data-cursor-has-more', state.hasMore ? 'true' : 'false');

                var sidebar = (response && response.userSidebarViewDTO)
                    ? response.userSidebarViewDTO
                    : null;
                if (sidebar && typeof window.updateSidebarAndHeader === 'function') {
                    window.updateSidebarAndHeader(sidebar);
                }

                ensureTail();

                if (isGroupableHistoryContainer() &&
                    typeof window.applyHistoryGrouping === 'function') {
                    window.applyHistoryGrouping(container);
                    ensureTail();
                }

                if (window.PosterFallback && typeof window.PosterFallback.scan === 'function') {
                    window.PosterFallback.scan(container);
                }

                if (typeof window.applyStatusesFromSession === 'function') {
                    window.applyStatusesFromSession();
                }

                if (typeof window.reapplyCatalogSearchFilter === 'function') {
                    window.reapplyCatalogSearchFilter();
                }

                ensureTail();
                rebindScrollParents();

                return items.length > 0;
            })
            .catch(function (e) {
                console.warn('[cursor-pagination] error', e);
                state.hasMore = false;
                return false;
            })
            .finally(function () {
                state.fetching = false;
            });
        };

        state.checkEdge = function () {
            if (state.fetching || !state.hasMore) return;
            ensureTail();
            if (isSentinelVisible(state)) {
                state.fetchNext();
            }
        };

        ensureTail();
        rebindScrollParents();

        state.pollTimer = setInterval(function () {
            state.checkEdge();
        }, POLL_INTERVAL_MS);

        state.destroy = function () {
            if (state.pollTimer) clearInterval(state.pollTimer);
            detachScrollListeners();
            if (state.sentinel && state.sentinel.parentNode) {
                state.sentinel.remove();
            }
            container.dataset.cursorMounted = '';
        };

        instances.push(state);
        window.__cursorStates = instances;

        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                state.checkEdge();
                setTimeout(function () { state.checkEdge(); }, 300);
                setTimeout(function () { state.checkEdge(); }, 1000);
            });
        });

        return state;
    }

    function mountAll(root) {
        (root || document).querySelectorAll('[data-cursor-container]').forEach(mount);
    }

    window.CursorPagination = {
        mount: mount,
        mountAll: mountAll,
        destroyAll: function () {
            instances.forEach(function (s) { if (s.destroy) s.destroy(); });
            instances = [];
            window.__cursorStates = instances;
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { mountAll(); }, { once: true });
    } else {
        mountAll();
    }
    window.addEventListener('load', function () { mountAll(); });
    window.addEventListener('pageshow', function (e) {
        if (e.persisted) mountAll();
    });
})();