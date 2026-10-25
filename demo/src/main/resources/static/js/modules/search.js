(function () {
    'use strict';

    var SUGGESTIONS_DEBOUNCE_MS = 200;
    var SUGGESTIONS_LIMIT = 10;

    var DEFAULT_STORAGE_KEY = 'lastSearchQuery';
    var SEARCH_CLEARED_KEY = 'searchClearedFlag';
    var KNOWN_TITLES_KEY = 'knownTitles';

    function hasMeaningfulChars(value) {
        return /[a-zа-я0-9ё]/i.test(String(value || ''));
    }

    function capitalizeFirst(value) {
        if (!value) return '';
        if (value.length === 1) return value.toLocaleUpperCase('ru-RU');
        return value.charAt(0).toLocaleUpperCase('ru-RU') + value.slice(1).toLocaleLowerCase('ru-RU');
    }

    function normalizeForMatch(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .replace(/ё/g, 'е');
    }

    function readKnownTitles() {
        try {
            var raw = sessionStorage.getItem(KNOWN_TITLES_KEY);
            if (!raw) return [];
            var parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (e) { return []; }
    }

    function rememberKnownTitles(titles) {
        if (!titles || titles.length === 0) return;
        try {
            var existing = readKnownTitles();
            var seen = new Set(existing.map(normalizeForMatch));
            var added = false;
            titles.forEach(function (t) {
                var n = normalizeForMatch(t);
                if (n && !seen.has(n)) {
                    existing.push(t);
                    seen.add(n);
                    added = true;
                }
            });
            if (added) {
                if (existing.length > 500) existing = existing.slice(-500);
                sessionStorage.setItem(KNOWN_TITLES_KEY, JSON.stringify(existing));
            }
        } catch (e) {}
    }

    function isKnownFullTitle(value) {
        var n = normalizeForMatch(value);
        if (!n) return false;
        return readKnownTitles().some(function (t) {
            return normalizeForMatch(t) === n;
        });
    }

    function isMovieDetailsPage() {
        return /^\/movies\/\d+\/?$/.test(window.location.pathname || '');
    }

    function isCatalogPage() {
        var path = (window.location.pathname || '').replace(/\/$/, '');
        return path === '' || path === '/movies';
    }

    function readLastSearch(key) {
        try { return sessionStorage.getItem(key || DEFAULT_STORAGE_KEY) || ''; } catch (e) { return ''; }
    }

    function writeLastSearch(key, value) {
        try {
            var storageKey = key || DEFAULT_STORAGE_KEY;
            var trimmed = String(value || '').trim();
            if (trimmed) sessionStorage.setItem(storageKey, trimmed);
            else sessionStorage.removeItem(storageKey);
        } catch (e) {}
    }

    function setSearchCleared() {
        try { sessionStorage.setItem(SEARCH_CLEARED_KEY, '1'); } catch (e) {}
    }

    function clearSearchCleared() {
        try { sessionStorage.removeItem(SEARCH_CLEARED_KEY); } catch (e) {}
    }

    function isSearchCleared() {
        try { return sessionStorage.getItem(SEARCH_CLEARED_KEY) === '1'; } catch (e) { return false; }
    }

    function hasAnyFilterParam() {
        try {
            var params = new URLSearchParams(window.location.search);
            var keys = ['priceFrom', 'priceTo', 'ratingFrom', 'ratingTo', 'durationFrom', 'durationTo', 'releaseDateFrom', 'releaseDateTo', 'genres', 'directors', 'sortBy', 'sortDirection', 'page'];
            for (var i = 0; i < keys.length; i++) {
                if (params.has(keys[i])) return true;
            }
            return false;
        } catch (e) { return false; }
    }

    function normalizeSuggestionsPayload(payload) {
        var items = Array.isArray(payload) ? payload : (payload && Array.isArray(payload.items) ? payload.items : []);
        var result = [];
        for (var i = 0; i < items.length; i++) {
            var entry = items[i];
            if (entry == null) continue;
            if (typeof entry === 'string') {
                var s = entry.trim();
                if (s) result.push({ title: s });
            } else if (typeof entry === 'object') {
                var raw = entry.title != null ? entry.title : entry.movieTitle;
                if (raw != null) {
                    var t = String(raw).trim();
                    if (t) {
                        var item = { title: t };
                        if (entry.watchCount != null) {
                            var wc = Number(entry.watchCount);
                            if (!isNaN(wc) && wc > 0) item.watchCount = wc;
                        }
                        if (entry.movieId != null) item.movieId = entry.movieId;
                        result.push(item);
                    }
                }
            }
        }
        return result;
    }

    function fetchSuggestionsPayload(url) {
        return fetch(url, {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin', cache: 'no-store'
        })
        .then(function (response) {
            if (response.type === 'opaqueredirect' || response.status === 0) return [];
            if (response.redirected) return [];
            if (!response.ok) return [];
            var ct = response.headers.get('content-type') || '';
            if (ct.indexOf('text/html') !== -1) return [];
            return response.json();
        })
        .then(normalizeSuggestionsPayload)
        .catch(function () { return []; });
    }

    function detectCatalogSuggestionsEndpoint() {
        if (window.__collectionSuggestionsEndpoint) return window.__collectionSuggestionsEndpoint;
        var path = (window.location.pathname || '').replace(/\/$/, '');
        if (path === '/cart') return '/cart/suggestions';
        if (path === '/favourites') return '/favourites/suggestions';
        if (path === '/library') return '/library/suggestions';
        if (path === '/history') return '/history/suggestions';
        if (path === '/rated/history') return '/rated/history/suggestions';
        if (path === '/watched') return '/watched/suggestions';
        return null;
    }

    function commonPrefixOriginal(titles) {
        if (!titles || titles.length === 0) return '';
        if (titles.length === 1) return titles[0];
        var first = titles[0];
        var max = first.length;
        for (var i = 1; i < titles.length; i++) {
            var other = titles[i];
            var limit = Math.min(max, other.length);
            var j = 0;
            while (j < limit) {
                if (normalizeForMatch(first.charAt(j)) !== normalizeForMatch(other.charAt(j))) break;
                j++;
            }
            max = j;
            if (max === 0) return '';
        }
        return first.substring(0, max);
    }

    function findExactTitle(titles, value) {
        if (!titles || titles.length === 0) return null;
        var normalized = normalizeForMatch(value);
        if (!normalized) return null;
        for (var i = 0; i < titles.length; i++) {
            if (normalizeForMatch(titles[i]) === normalized) return titles[i];
        }
        return null;
    }

    function createSearchEngine(config) {
        var form = config.form;
        var input = config.input;
        var wrapper = config.wrapper;
        var clear = config.clear || null;
        var suggestionsBox = config.suggestionsBox || null;
        var typedEl = config.typedEl || null;
        var hintEl = config.hintEl || null;
        var getSuggestionsUrl = config.getSuggestionsUrl;
        var onBeforeSubmit = config.onBeforeSubmit || null;
        var submitForm = config.submitForm || null;
        var onEmptyAfterClear = config.onEmptyAfterClear || null;
        var onRememberQuery = config.onRememberQuery || null;
        var suggestionRowClass = config.suggestionRowClass || 'suggestion-item';
        var suggestionTitleClass = config.suggestionTitleClass || 'suggestion-title';
        var queryParamName = config.queryParamName || 'title';
        var activeClass = 'is-active';

        if (!form || !input || !wrapper || typeof getSuggestionsUrl !== 'function') return null;

        var debounceTimer = null;
        var currentTopMatch = null;
        var activeIndex = -1;
        var lastFetchedTitles = null;
        var lastFetchedQuery = '';

        function syncClear() { wrapper.classList.toggle('has-value', input.value.trim().length > 0); }
        function hideAutocomplete() {
            currentTopMatch = null;
            wrapper.classList.remove('has-autocomplete');
            if (typedEl) typedEl.textContent = '';
            if (hintEl) hintEl.textContent = '';
        }
        function hideSuggestions() {
            if (!suggestionsBox) return;
            suggestionsBox.innerHTML = '';
            suggestionsBox.classList.remove('show');
            activeIndex = -1;
        }
        function getSuggestionElements() {
            if (!suggestionsBox) return [];
            return Array.prototype.slice.call(suggestionsBox.querySelectorAll('.' + suggestionRowClass));
        }
        function setActiveSuggestion(index) {
            var items = getSuggestionElements();
            items.forEach(function (el) { el.classList.remove(activeClass); });
            activeIndex = index;
            if (index >= 0 && index < items.length) {
                items[index].classList.add(activeClass);
                items[index].scrollIntoView({ block: 'nearest' });
            }
        }

        function buildVisualQuery(typedValue, allTitles) {
            if (!typedValue) return '';
            if (!allTitles || allTitles.length === 0) return typedValue;
            var prefix = commonPrefixOriginal(allTitles);
            var visual = prefix.length >= typedValue.length ? prefix : typedValue;
            if (findExactTitle(allTitles, visual)) return visual;
            return visual + '…';
        }

        function ensureHiddenField(name, value) {
            var existing = form.querySelector('input[type="hidden"][name="' + name + '"]');
            if (!existing) {
                existing = document.createElement('input');
                existing.type = 'hidden';
                existing.name = name;
                form.appendChild(existing);
            }
            existing.value = value;
        }

        function renderAutocomplete(typedValue, commonPrefix) {
            if (!typedEl || !hintEl) { hideAutocomplete(); return; }
            if (!typedValue || !commonPrefix) { hideAutocomplete(); return; }

            var typedLower = normalizeForMatch(typedValue);
            var prefixLower = normalizeForMatch(commonPrefix);
            if (prefixLower.indexOf(typedLower) !== 0) { hideAutocomplete(); return; }

            var remainingPart = commonPrefix.substring(typedValue.length);
            if (!remainingPart) { hideAutocomplete(); return; }

            typedEl.textContent = typedValue;
            hintEl.textContent = remainingPart;
            wrapper.classList.add('has-autocomplete');
            currentTopMatch = commonPrefix;
        }

        function renderSuggestions(entries) {
            if (!suggestionsBox) return;
            suggestionsBox.innerHTML = '';
            if (!entries || entries.length === 0) {
                suggestionsBox.classList.remove('show');
                return;
            }
            entries.slice(0, SUGGESTIONS_LIMIT).forEach(function (entry) {
                var row = document.createElement('div');
                row.className = suggestionRowClass;
                row.setAttribute('data-suggestion-title', entry.title);

                var titleSpan = document.createElement('span');
                titleSpan.className = suggestionTitleClass;
                titleSpan.textContent = entry.title;
                titleSpan.title = entry.title;
                row.appendChild(titleSpan);

                if (entry.watchCount != null && entry.watchCount > 1) {
                    var metaSpan = document.createElement('span');
                    metaSpan.className = 'catalog-suggestion-meta';
                    metaSpan.textContent = '(' + entry.watchCount + ')';
                    row.appendChild(metaSpan);
                }

                row.addEventListener('mouseenter', function () {
                    var idx = getSuggestionElements().indexOf(row);
                    if (idx >= 0) setActiveSuggestion(idx);
                });
                suggestionsBox.appendChild(row);
            });
            suggestionsBox.classList.add('show');
        }

        function runFetchSuggestions(query) {
            var url = getSuggestionsUrl(query);
            if (!url) return;

            fetchSuggestionsPayload(url).then(function (entries) {
                lastFetchedQuery = query;
                lastFetchedTitles = (entries || []).map(function (e) { return e.title; });
                rememberKnownTitles(lastFetchedTitles);

                if (!entries || entries.length === 0) { hideSuggestions(); hideAutocomplete(); return; }
                renderSuggestions(entries);

                var typedValue = input.value;
                var typedLower = normalizeForMatch(typedValue);
                var matching = entries.filter(function (e) { return normalizeForMatch(e.title).indexOf(typedLower) === 0; });

                if (matching.length > 0) {
                    var prefix = commonPrefixOriginal(matching.map(function (e) { return e.title; }));
                    if (prefix && normalizeForMatch(prefix).length > typedLower.length) {
                        renderAutocomplete(typedValue, prefix);
                    } else {
                        hideAutocomplete();
                    }
                } else {
                    hideAutocomplete();
                }
            });
        }

        function submitRaw(value, visualValue) {
            if (!value) return;
            ensureHiddenField(queryParamName, value);
            input.removeAttribute('name');
            if (visualValue != null) input.value = visualValue;
            if (typeof onRememberQuery === 'function' && visualValue != null) onRememberQuery(visualValue);
            if (typeof onBeforeSubmit === 'function') onBeforeSubmit();
            hideSuggestions();
            hideAutocomplete();
            if (typeof submitForm === 'function') submitForm();
            else form.submit();
        }

        function submitFromSuggestion(title) {
            if (!title) return;
            submitRaw(title, title);
        }

        function doSubmit() {
            var raw = String(input.value || '').trim();
            if (!hasMeaningfulChars(raw)) return;

            var cleanValue = capitalizeFirst(raw.replace(/…+$/, '').trim());

            var exactFromFetched = findExactTitle(lastFetchedTitles, cleanValue);
            if (exactFromFetched) {
                submitRaw(exactFromFetched, exactFromFetched);
                return;
            }

            var urlParam = new URLSearchParams(window.location.search).get(queryParamName);
            if (urlParam && normalizeForMatch(urlParam) === normalizeForMatch(cleanValue)
                && isKnownFullTitle(cleanValue)) {
                submitRaw(cleanValue, cleanValue);
                return;
            }

            var visualValue;
            var nClean = normalizeForMatch(cleanValue);
            var nLast = normalizeForMatch(lastFetchedQuery);
            var useLastFetched = lastFetchedTitles && lastFetchedTitles.length > 0
                && (nLast.indexOf(nClean) === 0 || nClean.indexOf(nLast) === 0);

            if (useLastFetched) {
                visualValue = buildVisualQuery(cleanValue, lastFetchedTitles);
            } else if (currentTopMatch && normalizeForMatch(currentTopMatch).length > nClean.length) {
                visualValue = currentTopMatch + '…';
            } else if (isKnownFullTitle(cleanValue)) {
                visualValue = cleanValue;
            } else {
                visualValue = cleanValue + '…';
            }

            submitRaw(cleanValue, visualValue);
        }

        input.addEventListener('input', function () {
            var val = input.value.trim();
            syncClear();
            hideAutocomplete();
            clearTimeout(debounceTimer);

            if (!val || !hasMeaningfulChars(val)) { hideSuggestions(); return; }
            debounceTimer = setTimeout(function () { runFetchSuggestions(val); }, SUGGESTIONS_DEBOUNCE_MS);
        });

        input.addEventListener('keydown', function (e) {
            var items = getSuggestionElements();

            if (e.key === 'ArrowDown') {
                if (items.length > 0) {
                    e.preventDefault();
                    setActiveSuggestion(activeIndex < items.length - 1 ? activeIndex + 1 : 0);
                }
                return;
            }
            if (e.key === 'ArrowUp') {
                if (items.length > 0) {
                    e.preventDefault();
                    setActiveSuggestion(activeIndex > 0 ? activeIndex - 1 : items.length - 1);
                }
                return;
            }
            if ((e.key === 'ArrowRight' || e.key === 'Tab') && currentTopMatch) {
                var caretAtEnd = input.selectionStart === input.value.length && input.selectionEnd === input.value.length;
                if (caretAtEnd) {
                    e.preventDefault();
                    input.value = currentTopMatch;
                    hideAutocomplete();
                    hideSuggestions();
                    syncClear();
                    return;
                }
            }
            if (e.key === 'Enter') {
                e.preventDefault();
                if (activeIndex >= 0 && items[activeIndex]) {
                    var chosen = items[activeIndex].getAttribute('data-suggestion-title');
                    hideSuggestions();
                    hideAutocomplete();
                    submitFromSuggestion(chosen);
                    return;
                }
                doSubmit();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () { hideSuggestions(); hideAutocomplete(); }, 150);
        });

        if (suggestionsBox) {
            suggestionsBox.addEventListener('pointerdown', function (e) {
                var row = e.target.closest('.' + suggestionRowClass);
                if (!row) return;
                e.preventDefault();
                var chosen = row.getAttribute('data-suggestion-title');
                hideSuggestions();
                hideAutocomplete();
                submitFromSuggestion(chosen);
            }, true);
        }

        if (clear) {
            clear.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                var hiddenField = form.querySelector('input[type="hidden"][name="' + queryParamName + '"]');
                if (hiddenField) hiddenField.value = '';
                syncClear();
                hideSuggestions();
                hideAutocomplete();
                if (typeof onEmptyAfterClear === 'function') onEmptyAfterClear();
            });
        }

        form.addEventListener('submit', function (e) { e.preventDefault(); doSubmit(); });

        syncClear();

        return { submit: doSubmit, setValue: function(v) { input.value = v || ''; syncClear(); } };
    }

    function initHeaderSearch() {
        var headerSearchInput = document.getElementById('headerSearchInput');
        var headerSearchForm = document.getElementById('headerSearchForm');
        var headerSearchWrapper = document.getElementById('headerSearchWrapper');
        if (!headerSearchInput || !headerSearchForm) return;

        function redirectToCleanCatalogIfNeeded() {
            if (!isCatalogPage()) return false;
            if (hasAnyFilterParam()) return false;
            if (!window.location.search.includes('title=')) return false;
            if (!isSearchCleared()) return false;
            clearSearchCleared();
            window.location.replace('/');
            return true;
        }

        if (redirectToCleanCatalogIfNeeded()) return;

        var engine = createSearchEngine({
            form: headerSearchForm,
            input: headerSearchInput,
            wrapper: headerSearchWrapper,
            clear: document.getElementById('headerSearchClear'),
            suggestionsBox: document.getElementById('headerSuggestions'),
            typedEl: document.getElementById('headerAutocompleteTyped'),
            hintEl: document.getElementById('headerAutocompleteHint'),
            suggestionRowClass: 'catalog-suggestion-item',
            suggestionTitleClass: 'catalog-suggestion-title',
            queryParamName: 'title',
            getSuggestionsUrl: function (q) { return '/movies/suggestions?query=' + encodeURIComponent(q); },
            submitForm: function () {
                if (typeof window.submitCombinedSearch === 'function') window.submitCombinedSearch();
                else headerSearchForm.submit();
            },
            onBeforeSubmit: function () {
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) throw new Error('validation');
            },
            onRememberQuery: function (v) {
                clearSearchCleared();
                writeLastSearch(DEFAULT_STORAGE_KEY, v);
            },
            onEmptyAfterClear: function () {
                setSearchCleared();
                writeLastSearch(DEFAULT_STORAGE_KEY, '');
                window.location.replace('/');
            }
        });

        if (!engine) return;

        var submitBtn = document.getElementById('headerSearchSubmitBtn');
        if (submitBtn) {
            submitBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                engine.submit();
            });
        }

        var urlParams = new URLSearchParams(window.location.search);
        var urlTitle = urlParams.get('title');
        var savedTitle = readLastSearch(DEFAULT_STORAGE_KEY);

        if (urlTitle) {
            engine.setValue(savedTitle || urlTitle);
        } else if (isMovieDetailsPage() && savedTitle) {
            engine.setValue(savedTitle);
        }
    }

    function initCatalogSearch() {
        var form = document.querySelector('[data-catalog-search-form]');
        var input = document.querySelector('[data-catalog-search-input]');
        var wrapper = document.querySelector('[data-catalog-search-wrapper]');
        if (!form || !input || !wrapper) return;

        var path = (window.location.pathname || '').replace(/\/$/, '');
        var storageKey = 'lastSearchQuery:' + path;

        var engine = createSearchEngine({
            form: form,
            input: input,
            wrapper: wrapper,
            clear: document.querySelector('[data-catalog-search-clear]'),
            suggestionsBox: document.querySelector('[data-catalog-search-suggestions]'),
            queryParamName: 'query',
            getSuggestionsUrl: function (q) {
                var endpoint = detectCatalogSuggestionsEndpoint();
                if (!endpoint) return null;
                return endpoint + '?query=' + encodeURIComponent(q);
            },
            submitForm: function () { form.submit(); },
            onRememberQuery: function (v) {
                writeLastSearch(storageKey, v);
            },
            onEmptyAfterClear: function () {
                writeLastSearch(storageKey, '');
                window.location.replace(window.location.pathname);
            }
        });

        if (!engine) return;

        var urlParams = new URLSearchParams(window.location.search);
        var urlQuery = urlParams.get('query');
        var savedQuery = readLastSearch(storageKey);

        if (urlQuery) {
            engine.setValue(savedQuery || urlQuery);
        }
    }

    function initAll() {
        initHeaderSearch();
        initCatalogSearch();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }
})();