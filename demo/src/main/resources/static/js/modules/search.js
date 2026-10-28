(function () {
    'use strict';

    var SUGGESTIONS_DEBOUNCE_MS = 250;
    var SUGGESTIONS_LIMIT = 10;

    var DEFAULT_STORAGE_KEY = 'lastSearchQuery';
    var SEARCH_CLEARED_KEY = 'searchClearedFlag';
    var KNOWN_TITLES_KEY = 'knownTitles';
    var CAME_FROM_SEARCH_KEY = 'cameFromSearch';

    var suggestionsCache = {};

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
            titles.forEach(function (title) {
                var normalizedTitle = normalizeForMatch(title);
                if (normalizedTitle && !seen.has(normalizedTitle)) {
                    existing.push(title);
                    seen.add(normalizedTitle);
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
        var normalizedValue = normalizeForMatch(value);
        if (!normalizedValue) return false;
        return readKnownTitles().some(function (title) {
            return normalizeForMatch(title) === normalizedValue;
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
                var stringTitle = entry.trim();
                if (stringTitle) result.push({ title: stringTitle, movieId: null });
            } else if (typeof entry === 'object') {
                var rawTitle = entry.title != null ? entry.title : entry.movieTitle;
                if (rawTitle != null) {
                    var trimmedTitle = String(rawTitle).trim();
                    if (trimmedTitle) {
                        var item = { title: trimmedTitle, movieId: null };
                        if (entry.id != null) {
                            item.movieId = entry.id;
                        } else if (entry.movieId != null) {
                            item.movieId = entry.movieId;
                        }
                        if (entry.watchCount != null) {
                            var watchCount = Number(entry.watchCount);
                            if (!isNaN(watchCount) && watchCount > 0) item.watchCount = watchCount;
                        }
                        result.push(item);
                    }
                }
            }
        }
        return result;
    }

    function fetchSuggestionsPayload(url) {
        if (suggestionsCache[url]) {
            return Promise.resolve(suggestionsCache[url]);
        }

        return fetch(url, {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin', cache: 'no-store'
        })
        .then(function (response) {
            if (response.type === 'opaqueredirect' || response.status === 0) return [];
            if (response.redirected) return [];
            if (!response.ok) return [];
            var contentType = response.headers.get('content-type') || '';
            if (contentType.indexOf('text/html') !== -1) return [];
            return response.json();
        })
        .then(function (json) {
            var normalized = normalizeSuggestionsPayload(json);
            suggestionsCache[url] = normalized;
            return normalized;
        })
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
        var normalizedValue = normalizeForMatch(value);
        if (!normalizedValue) return null;
        for (var i = 0; i < titles.length; i++) {
            if (normalizeForMatch(titles[i]) === normalizedValue) return titles[i];
        }
        return null;
    }

    function createSearchEngine(config) {
        var form = config.form;
        var input = config.input;
        var wrapper = config.wrapper;
        var clear = config.clear || null;
        var suggestionsBox = config.suggestionsBox || null;
        var typedElement = config.typedEl || null;
        var hintElement = config.hintEl || null;
        var getSuggestionsUrl = config.getSuggestionsUrl;
        var submitForm = config.submitForm || null;
        var onEmptyAfterClear = config.onEmptyAfterClear || null;
        var onRememberQuery = config.onRememberQuery || null;
        var suggestionRowClass = config.suggestionRowClass || 'suggestion-item';
        var suggestionTitleClass = config.suggestionTitleClass || 'suggestion-title';
        var queryParamName = config.queryParamName || 'title';
        var activeClass = 'is-active';
        var isMovieSearch = config.isMovieSearch || false;

        if (!form || !input || !wrapper || typeof getSuggestionsUrl !== 'function') return null;

        var debounceTimer = null;
        var currentTopMatch = null;
        var activeIndex = -1;
        var lastFetchedTitles = null;
        var lastFetchedQuery = '';
        var lastFetchedItems = [];

        function syncClear() { wrapper.classList.toggle('has-value', input.value.trim().length > 0); }
        function hideAutocomplete() {
            currentTopMatch = null;
            wrapper.classList.remove('has-autocomplete');
            if (typedElement) typedElement.textContent = '';
            if (hintElement) hintElement.textContent = '';
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
            items.forEach(function (element) { element.classList.remove(activeClass); });
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
            if (!typedElement || !hintElement) { hideAutocomplete(); return; }
            if (!typedValue || !commonPrefix) { hideAutocomplete(); return; }

            var typedLower = normalizeForMatch(typedValue);
            var prefixLower = normalizeForMatch(commonPrefix);
            if (prefixLower.indexOf(typedLower) !== 0) { hideAutocomplete(); return; }

            var remainingPart = commonPrefix.substring(typedValue.length);
            if (!remainingPart) { hideAutocomplete(); return; }

            typedElement.textContent = typedValue;
            hintElement.textContent = remainingPart;
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
                if (entry.movieId) {
                    row.setAttribute('data-movie-id', entry.movieId);
                }

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
                lastFetchedItems = entries || [];
                lastFetchedTitles = lastFetchedItems.map(function (entry) { return entry.title; });
                rememberKnownTitles(lastFetchedTitles);

                if (!entries || entries.length === 0) { hideSuggestions(); hideAutocomplete(); return; }
                renderSuggestions(entries);

                var typedValue = input.value;
                var typedLower = normalizeForMatch(typedValue);
                var matching = entries.filter(function (entry) { return normalizeForMatch(entry.title).indexOf(typedLower) === 0; });

                if (matching.length > 0) {
                    var prefix = commonPrefixOriginal(matching.map(function (entry) { return entry.title; }));
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
            hideSuggestions();
            hideAutocomplete();

            try { sessionStorage.setItem(CAME_FROM_SEARCH_KEY, '1'); } catch (e) {}

            if (typeof submitForm === 'function') submitForm(value);
            else form.submit();
        }

        function submitFromSuggestion(title, movieId) {
            if (!title) return;

            if (isMovieSearch && movieId) {
                if (typeof onRememberQuery === 'function') onRememberQuery(title);
                try { sessionStorage.setItem(CAME_FROM_SEARCH_KEY, '1'); } catch (e) {}
                window.location.href = '/movies/' + encodeURIComponent(movieId);
                return;
            }

            submitRaw(title, title);
        }

        function doSubmit() {
            var raw = String(input.value || '').trim();
            if (!hasMeaningfulChars(raw)) return;

            var cleanValue = capitalizeFirst(raw.replace(/…+$/, '').trim());

            var exactFromFetched = findExactTitle(lastFetchedTitles, cleanValue);
            if (exactFromFetched && lastFetchedItems.length === 1) {
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
            var normalizedClean = normalizeForMatch(cleanValue);
            var normalizedLast = normalizeForMatch(lastFetchedQuery);
            var useLastFetched = lastFetchedTitles && lastFetchedTitles.length > 0
                && (normalizedLast.indexOf(normalizedClean) === 0 || normalizedClean.indexOf(normalizedLast) === 0);

            if (useLastFetched) {
                visualValue = buildVisualQuery(cleanValue, lastFetchedTitles);
            } else if (currentTopMatch && normalizeForMatch(currentTopMatch).length > normalizedClean.length) {
                visualValue = currentTopMatch + '…';
            } else if (isKnownFullTitle(cleanValue) && lastFetchedItems.length === 1) {
                visualValue = cleanValue;
            } else {
                visualValue = cleanValue + '…';
            }

            submitRaw(cleanValue, visualValue);
        }

        input.addEventListener('input', function () {
            var value = input.value.trim();
            syncClear();
            hideAutocomplete();
            clearTimeout(debounceTimer);

            if (!value || !hasMeaningfulChars(value)) { hideSuggestions(); return; }

            debounceTimer = setTimeout(function () {
                runFetchSuggestions(value);
            }, SUGGESTIONS_DEBOUNCE_MS);
        });

        input.addEventListener('keydown', function (event) {
            var items = getSuggestionElements();

            if (event.key === 'ArrowDown') {
                if (items.length > 0) {
                    event.preventDefault();
                    setActiveSuggestion(activeIndex < items.length - 1 ? activeIndex + 1 : 0);
                }
                return;
            }
            if (event.key === 'ArrowUp') {
                if (items.length > 0) {
                    event.preventDefault();
                    setActiveSuggestion(activeIndex > 0 ? activeIndex - 1 : items.length - 1);
                }
                return;
            }
            if ((event.key === 'ArrowRight' || event.key === 'Tab') && currentTopMatch) {
                var caretAtEnd = input.selectionStart === input.value.length && input.selectionEnd === input.value.length;
                if (caretAtEnd) {
                    event.preventDefault();
                    input.value = currentTopMatch;
                    hideAutocomplete();
                    hideSuggestions();
                    syncClear();
                    return;
                }
            }
            if (event.key === 'Enter') {
                event.preventDefault();
                if (activeIndex >= 0 && items[activeIndex]) {
                    var chosenTitle = items[activeIndex].getAttribute('data-suggestion-title');
                    var chosenId = items[activeIndex].getAttribute('data-movie-id');
                    hideSuggestions();
                    hideAutocomplete();
                    submitFromSuggestion(chosenTitle, chosenId);
                    return;
                }
                doSubmit();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () { hideSuggestions(); hideAutocomplete(); }, 150);
        });

        if (suggestionsBox) {
            suggestionsBox.addEventListener('pointerdown', function (event) {
                var row = event.target.closest('.' + suggestionRowClass);
                if (!row) return;
                event.preventDefault();
                var chosenTitle = row.getAttribute('data-suggestion-title');
                var chosenId = row.getAttribute('data-movie-id');
                hideSuggestions();
                hideAutocomplete();
                submitFromSuggestion(chosenTitle, chosenId);
            }, true);
        }

        if (clear) {
            clear.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                input.value = '';
                var hiddenField = form.querySelector('input[type="hidden"][name="' + queryParamName + '"]');
                if (hiddenField) hiddenField.value = '';
                syncClear();
                hideSuggestions();
                hideAutocomplete();
                if (typeof onEmptyAfterClear === 'function') onEmptyAfterClear();
            });
        }

        form.addEventListener('submit', function (event) { event.preventDefault(); doSubmit(); });

        syncClear();

        return { submit: doSubmit, setValue: function(value) { input.value = value || ''; syncClear(); } };
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
            isMovieSearch: true,
            getSuggestionsUrl: function (query) { return '/movies/suggestions?query=' + encodeURIComponent(query); },
            submitForm: function (cleanValue) {
                if (isCatalogPage() && typeof window.submitCombinedSearch === 'function') {
                    window.submitCombinedSearch();
                    return;
                }

                var value = cleanValue;
                if (!value) {
                    var hidden = headerSearchForm.querySelector('input[type="hidden"][name="title"]');
                    value = hidden ? hidden.value.trim() : '';
                }
                if (!value) {
                    value = headerSearchInput.value.trim().replace(/…+$/, '').trim();
                }

                if (value) {
                    window.location.href = '/movies?title=' + encodeURIComponent(value);
                } else {
                    window.location.href = '/movies';
                }
            },
            onRememberQuery: function (value) {
                clearSearchCleared();
                writeLastSearch(DEFAULT_STORAGE_KEY, value);
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
            submitBtn.addEventListener('click', function (event) {
                event.preventDefault();
                engine.submit();
            });
        }

        var cameFromSearch = false;
        try {
            cameFromSearch = sessionStorage.getItem(CAME_FROM_SEARCH_KEY) === '1';
            sessionStorage.removeItem(CAME_FROM_SEARCH_KEY);
        } catch (e) {}

        var urlParams = new URLSearchParams(window.location.search);
        var urlTitle = urlParams.get('title');
        var savedTitle = readLastSearch(DEFAULT_STORAGE_KEY);

        var isMainPage = window.location.pathname === '/' || window.location.pathname === '';

        if (isMovieDetailsPage()) {
            if (cameFromSearch && savedTitle) {
                engine.setValue(savedTitle);
            } else {
                headerSearchInput.value = '';
                headerSearchWrapper.classList.remove('has-value');
                writeLastSearch(DEFAULT_STORAGE_KEY, '');
            }
        } else if (!isCatalogPage() && window.location.pathname !== '/') {
            headerSearchInput.value = '';
            headerSearchWrapper.classList.remove('has-value');
            writeLastSearch(DEFAULT_STORAGE_KEY, '');
        } else if (isMainPage) {
            if (urlTitle) {
                engine.setValue(savedTitle || urlTitle);
            } else {
                headerSearchInput.value = '';
                headerSearchWrapper.classList.remove('has-value');
                writeLastSearch(DEFAULT_STORAGE_KEY, '');
            }
        } else if (urlTitle) {
            var totalMatches = 0;
            var totalMatchesEl = document.getElementById('catalogTotalMatches');
            if (totalMatchesEl) {
                totalMatches = parseInt(totalMatchesEl.value, 10) || 0;
            }

            var cleanTitle = urlTitle.replace(/…+$/, '').trim();
            if (totalMatches > 1) {
                engine.setValue(cleanTitle + '…');
            } else {
                engine.setValue(cleanTitle);
            }
        } else if (savedTitle) {
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
            isMovieSearch: false,
            getSuggestionsUrl: function (query) {
                var endpoint = detectCatalogSuggestionsEndpoint();
                if (!endpoint) return null;
                return endpoint + '?query=' + encodeURIComponent(query);
            },
            submitForm: function () { form.submit(); },
            onRememberQuery: function (value) {
                writeLastSearch(storageKey, value);
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