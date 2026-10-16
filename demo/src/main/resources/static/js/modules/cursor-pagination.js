(function () {
    'use strict';

    var LOAD_THRESHOLD_ROOT_MARGIN = '200px';
    var instances = [];

    function findScrollParent(el) {
        var node = el && el.parentElement;
        while (node && node !== document.body && node !== document.documentElement) {
            var style = window.getComputedStyle(node);
            if (/(auto|scroll|overlay)/.test(style.overflowY) &&
                node.scrollHeight > node.clientHeight + 1) {
                return node;
            }
            node = node.parentElement;
        }
        return null;
    }

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

    function interpolateTemplate(tpl, item, isUrl) {
        return String(tpl).replace(/\{(\w+)\}/g, function (_, key) {
            var v = item[key];
            if (v === undefined || v === null) return '';
            var s = String(v);
            return isUrl ? encodeURIComponent(s) : s;
        });
    }

    function applyDataAttrs(root, item) {
        root.querySelectorAll('[data-attr]').forEach(function (el) {
            var spec = el.getAttribute('data-attr') || '';
            spec.split(';').forEach(function (pair) {
                var idx = pair.indexOf(':');
                if (idx === -1) return;
                var attr = pair.slice(0, idx).trim();
                var key  = pair.slice(idx + 1).trim();
                if (!attr || !key) return;

                var value;
                if (key.indexOf('{') !== -1) {
                    value = interpolateTemplate(key, item, !!URL_ATTRS[attr]);
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

        root.querySelectorAll('[data-field]').forEach(function (el) {
            var key = el.getAttribute('data-field');
            var value = item[key];
            if (value === undefined || value === null) return;

            if (key === 'price' || key === 'priceSnapshot') {
                var n = parseFloat(value);
                if (!isNaN(n) && window.UI && typeof window.UI.formatPrice === 'function') {
                    value = window.UI.formatPrice(n) + ' ₽';
                }
            }

            if (key === 'addedAt' || key === 'watchedAt') {
                if (typeof window.formatDateTime === 'function') {
                    var formatted = window.formatDateTime(value);
                    if (formatted) value = formatted;
                } else {
                    value = normalizeAddedAt(value);
                }
            }

            if (el.tagName === 'IMG') {
                el.setAttribute('src', String(value));
                el.setAttribute('data-poster-img', '');
            } else {
                el.textContent = String(value);
            }
        });

        var posterImgs = root.querySelectorAll('img[data-poster-img]');
        posterImgs.forEach(function (img) {
            var url = img.getAttribute('src');
            var container = img.closest(
                '.media-card-poster-link, .movie-poster-link, .poster-wrapper, ' +
                '.history-item-poster, .fav-poster, .rec-poster'
            ) || img.parentNode;
            var placeholder = container
                ? container.querySelector('[data-poster-placeholder]')
                : null;

            var posterUrl = url;
            if (!posterUrl) {
                posterUrl = item.posterUrl || item.poster_url || '';
            }

            var empty = !posterUrl || posterUrl === 'null';
            if (!empty) {
                img.setAttribute('src', String(posterUrl));
                img.style.display = '';
                if (placeholder) placeholder.classList.add('hidden');
            } else {
                img.style.display = 'none';
                if (placeholder) placeholder.classList.remove('hidden');
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

    function reorderTail(container, sentinel, loader) {
        if (sentinel && sentinel.parentNode === container) {
            container.appendChild(sentinel);
        }
        if (loader && loader.parentNode === container) {
            container.appendChild(loader);
        }
    }

    function mount(container) {
        if (!container || container.dataset.cursorMounted === '1') return null;
        container.dataset.cursorMounted = '1';

        var endpoint   = container.getAttribute('data-cursor-endpoint');
        var templateId = container.getAttribute('data-cursor-template');
        var sort       = container.getAttribute('data-cursor-sort') || 'desc';
        var nextCursor = container.getAttribute('data-cursor-next') || null;
        var hasMore    = container.getAttribute('data-cursor-has-more') === 'true';
        var loader     = container.querySelector('[data-cursor-loader]');

        if (!endpoint || !templateId) return null;

        var state = {
            container: container,
            endpoint: endpoint,
            templateId: templateId,
            sort: sort,
            nextCursor: nextCursor,
            hasMore: hasMore,
            loading: false,
            fetching: false,
            loader: loader,
            observer: null,
            resizeObserver: null,
            sentinel: null,
            scrollRoot: null,
            scrollHandler: null,
            autoFillTimer: null
        };

        var sentinel = document.createElement('div');
        sentinel.setAttribute('aria-hidden', 'true');
        sentinel.style.height = '1px';
        sentinel.style.width = '100%';
        sentinel.style.pointerEvents = 'none';
        container.appendChild(sentinel);
        state.sentinel = sentinel;

        if (loader && loader.parentNode === container) {
            container.appendChild(loader);
        }

        state.rebindScrollRoot = function () {
            var newRoot = findScrollParent(container);
            if (newRoot === state.scrollRoot) return;

            if (state.scrollRoot && state.scrollHandler) {
                state.scrollRoot.removeEventListener('scroll', state.scrollHandler);
            } else if (state.scrollHandler) {
                window.removeEventListener('scroll', state.scrollHandler);
            }

            state.scrollRoot = newRoot;

            if (state.scrollRoot) {
                state.scrollRoot.addEventListener('scroll', state.scrollHandler, { passive: true });
            } else {
                window.addEventListener('scroll', state.scrollHandler, { passive: true });
            }

            if (state.observer) {
                state.observer.disconnect();
                state.observer = new IntersectionObserver(function (entries) {
                    for (var i = 0; i < entries.length; i++) {
                        if (entries[i].isIntersecting) {
                            state.fetchNext();
                            return;
                        }
                    }
                }, {
                    root: state.scrollRoot,
                    rootMargin: '0px 0px ' + LOAD_THRESHOLD_ROOT_MARGIN + ' 0px',
                    threshold: 0
                });
                state.observer.observe(sentinel);
            }
        };

        state.fetchNext = function () {
            if (state.fetching || state.loading || !state.hasMore) return Promise.resolve(false);
            state.fetching = true;
            state.loading = true;

            reorderTail(container, sentinel, loader);

            var params = new URLSearchParams();
            params.set('sort', state.sort);
            if (state.nextCursor) params.set('cursor', state.nextCursor);

            return fetch(state.endpoint + '?' + params.toString(), {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                cache: 'no-store'
            })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (response) {
                var page = (response && response.page) ? response.page : response;
                var content = Array.isArray(page && page.content) ? page.content : [];

                var frag = document.createDocumentFragment();
                content.forEach(function (item) {
                    var card = createCardFromTemplate(state.templateId, item);
                    if (card) frag.appendChild(card);
                });

                container.insertBefore(frag, sentinel);

                state.nextCursor = page.nextCursor || null;
                state.hasMore = !!page.hasMore;
                container.setAttribute('data-cursor-next', state.nextCursor || '');
                container.setAttribute('data-cursor-has-more', state.hasMore ? 'true' : 'false');

                if (response && response.userSidebar &&
                    typeof window.updateSidebarAndHeader === 'function') {
                    window.updateSidebarAndHeader(response.userSidebar);
                }

                if (typeof window.applyHistoryGrouping === 'function') {
                    window.applyHistoryGrouping(container);
                }
                if (window.PosterFallback && typeof window.PosterFallback.scan === 'function') {
                    window.PosterFallback.scan(container);
                }
                if (typeof window.applyStatusesFromSession === 'function') {
                    window.applyStatusesFromSession();
                }

                reorderTail(container, sentinel, loader);
                state.rebindScrollRoot();

                scheduleAutoFill();

                return content.length > 0;
            })
            .catch(function (e) {
                console.warn('[cursor-pagination] error', e);
                state.hasMore = false;
                return false;
            })
            .finally(function () {
                state.loading = false;
                state.fetching = false;
            });
        };

        state.checkEdge = function () {
            if (state.loading || !state.hasMore) return;
            var rect = sentinel.getBoundingClientRect();
            var rootRect = state.scrollRoot
                ? state.scrollRoot.getBoundingClientRect()
                : { top: 0, bottom: window.innerHeight || document.documentElement.clientHeight };
            var margin = 200;
            if (rect.top - rootRect.bottom < margin) {
                state.fetchNext();
            }
        };

        function scheduleAutoFill() {
            if (state.autoFillTimer) clearTimeout(state.autoFillTimer);
            var attempts = 0;
            function tryFill() {
                if (state.loading || !state.hasMore) return;
                if (attempts >= 8) return;
                attempts++;
                var rect = sentinel.getBoundingClientRect();
                var rootRect = state.scrollRoot
                    ? state.scrollRoot.getBoundingClientRect()
                    : { top: 0, bottom: window.innerHeight || document.documentElement.clientHeight };
                if (rect.top - rootRect.bottom < 200) {
                    state.fetchNext().then(function (got) {
                        if (got) {
                            state.autoFillTimer = setTimeout(tryFill, 100);
                        }
                    });
                }
            }
            state.autoFillTimer = setTimeout(tryFill, 150);
        }

        state.scrollRoot = findScrollParent(container);

        if ('IntersectionObserver' in window) {
            state.observer = new IntersectionObserver(function (entries) {
                for (var i = 0; i < entries.length; i++) {
                    if (entries[i].isIntersecting) {
                        state.fetchNext();
                        return;
                    }
                }
            }, {
                root: state.scrollRoot,
                rootMargin: '0px 0px ' + LOAD_THRESHOLD_ROOT_MARGIN + ' 0px',
                threshold: 0
            });
            state.observer.observe(sentinel);
        }

        state.scrollHandler = function () { state.checkEdge(); };
        if (state.scrollRoot) {
            state.scrollRoot.addEventListener('scroll', state.scrollHandler, { passive: true });
        } else {
            window.addEventListener('scroll', state.scrollHandler, { passive: true });
        }

        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                state.rebindScrollRoot();
                state.checkEdge();
                scheduleAutoFill();
            });
        });

        if ('ResizeObserver' in window) {
            state.resizeObserver = new ResizeObserver(function () {
                state.rebindScrollRoot();
                state.checkEdge();
            });
            state.resizeObserver.observe(container);
        }

        state.destroy = function () {
            if (state.observer) state.observer.disconnect();
            if (state.resizeObserver) state.resizeObserver.disconnect();
            if (state.autoFillTimer) clearTimeout(state.autoFillTimer);
            if (state.scrollRoot && state.scrollHandler) {
                state.scrollRoot.removeEventListener('scroll', state.scrollHandler);
            } else if (state.scrollHandler) {
                window.removeEventListener('scroll', state.scrollHandler);
            }
            state.observer = null;
            state.resizeObserver = null;
            state.scrollHandler = null;
            state.scrollRoot = null;
            if (state.sentinel && state.sentinel.parentNode) state.sentinel.remove();
            container.dataset.cursorMounted = '';
        };

        instances.push(state);
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
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { mountAll(); }, { once: true });
    } else {
        mountAll();
    }
    window.addEventListener('load', function () { mountAll(); });
})();