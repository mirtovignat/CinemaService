(function () {
    'use strict';

    var SUGGESTIONS_DEBOUNCE_MS = 250;
    var SUGGESTIONS_LIMIT = 10;

    var LAST_SEARCH_KEY = 'lastSearchQuery';
    var LAST_SEARCH_AT_KEY = 'lastSearchAt';
    var PENDING_KEY = 'pendingSearchRedirect';

    function hasMeaningfulChars(value) {
        return /[a-zа-я0-9ё]/i.test(String(value || ''));
    }

    function capitalizeFirst(value) {
        if (!value) return '';
        if (value.length === 1) return value.toLocaleUpperCase('ru-RU');
        return value.charAt(0).toLocaleUpperCase('ru-RU') +
               value.slice(1).toLocaleLowerCase('ru-RU');
    }

    function normalizeForMatch(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .replace(/ё/g, 'е');
    }

    function pad2(n) { return String(n).padStart(2, '0'); }

    function formatSuggestionDate(raw) {
        if (raw == null || raw === '') return '';

        var d = null;

        if (Array.isArray(raw) && raw.length >= 3) {
            d = new Date(raw[0], raw[1] - 1, raw[2],
                         raw[3] || 0, raw[4] || 0, raw[5] || 0);
        } else if (typeof raw === 'string') {
            var m = raw.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
            if (m) {
                d = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], m[6] ? +m[6] : 0);
            } else {
                d = new Date(raw);
            }
        } else if (typeof raw === 'number') {
            d = new Date(raw);
        }

        if (!d || isNaN(d.getTime())) return '';

        var now = new Date();
        var isToday = d.getFullYear() === now.getFullYear()
                   && d.getMonth() === now.getMonth()
                   && d.getDate() === now.getDate();

        var datePart = pad2(d.getDate()) + '.' + pad2(d.getMonth() + 1) + '.' + d.getFullYear();
        var timePart = pad2(d.getHours()) + ':' + pad2(d.getMinutes());

        return isToday ? timePart : datePart + ', ' + timePart;
    }

    function formatSuggestionMeta(entry) {
        var parts = [];

        if (entry.watchCount != null && Number(entry.watchCount) > 1) {
            parts.push('(' + Number(entry.watchCount) + ')');
        }

        var dateText = formatSuggestionDate(entry.lastWatchedAt);
        if (dateText) parts.push(dateText);

        return parts.join(' · ');
    }

    function normalizeSuggestionsPayload(payload) {
        var items = [];

        if (Array.isArray(payload)) {
            items = payload;
        } else if (payload && typeof payload === 'object' && Array.isArray(payload.items)) {
            items = payload.items;
        }

        var result = [];

        for (var i = 0; i < items.length; i++) {
            var entry = items[i];
            if (entry == null) continue;

            if (typeof entry === 'string') {
                var s = entry.trim();
                if (s) {
                    result.push({
                        title: s,
                        meta: '',
                        movieId: null,
                        historyId: null,
                        addedAt: null
                    });
                }
                continue;
            }

            if (typeof entry === 'object' && entry.title) {
                var t = String(entry.title).trim();
                if (!t) continue;

                result.push({
                    title: t,
                    meta: formatSuggestionMeta(entry),
                    movieId: entry.movieId != null ? String(entry.movieId) : null,
                    historyId: entry.historyId != null ? String(entry.historyId) : null,
                    addedAt: entry.lastWatchedAt || entry.addedAt || entry.watchedAt || null
                });
            }
        }

        return result;
    }

    function fetchSuggestionsPayload(url) {
        return fetch(url, {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin',
            cache: 'no-store'
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
        if (window.__collectionSuggestionsEndpoint) {
            return window.__collectionSuggestionsEndpoint;
        }

        var container = document.querySelector('[data-cursor-container]');
        if (container) {
            var endpoint = container.getAttribute('data-cursor-endpoint');
            if (endpoint === '/cart/more') return '/cart/suggestions';
            if (endpoint === '/favourites/more') return '/favourites/suggestions';
            if (endpoint === '/library/more') return '/library/suggestions';
            if (endpoint === '/history/more') return '/history/suggestions';
            if (endpoint === '/rated/history/more') return '/rated/history/suggestions';
            if (endpoint === '/watched/more') return '/watched/suggestions';
        }

        var path = (window.location.pathname || '').replace(/\/$/, '');
        if (path === '/cart') return '/cart/suggestions';
        if (path === '/favourites') return '/favourites/suggestions';
        if (path === '/library') return '/library/suggestions';
        if (path === '/history') return '/history/suggestions';
        if (path === '/rated/history') return '/rated/history/suggestions';
        if (path === '/watched') return '/watched/suggestions';
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
        var suggestionRowClass = config.suggestionRowClass || 'suggestion-item';
        var suggestionTitleClass = config.suggestionTitleClass || 'suggestion-title';
        var suggestionMetaClass = config.suggestionMetaClass || 'suggestion-meta';
        var activeClass = 'is-active';

        if (!form || !input || !wrapper || typeof getSuggestionsUrl !== 'function') return null;

        var debounceTimer = null;
        var currentTopMatch = null;
        var pointerDownInsideBox = false;
        var activeIndex = -1;
        var pendingMoveToList = false;
        var pendingSubmitTarget = null;

        function syncClear() {
            wrapper.classList.toggle('has-value', input.value.trim().length > 0);
        }

        function hideAutocomplete() {
            currentTopMatch = null;
            wrapper.classList.remove('has-autocomplete');
            if (typedEl) typedEl.textContent = '';
            if (hintEl) hintEl.textContent = '';
        }

        function hideSuggestions() {
            if (!suggestionsBox) return;
            suggestionsBox.textContent = '';
            suggestionsBox.classList.remove('show');
            if (suggestionsBox.style && suggestionsBox.style.display === 'flex') {
                suggestionsBox.style.display = 'none';
            }
            activeIndex = -1;
        }

        function getSuggestionElements() {
            if (!suggestionsBox) return [];
            return Array.prototype.slice.call(
                suggestionsBox.querySelectorAll('.' + suggestionRowClass)
            );
        }

        function setActiveSuggestion(index) {
            var items = getSuggestionElements();
            items.forEach(function (el) { el.classList.remove(activeClass); });
            activeIndex = index;
            if (index >= 0 && index < items.length) {
                items[index].classList.add(activeClass);
                try { items[index].scrollIntoView({ block: 'nearest' }); } catch (e) {}
            }
        }

        function resetActiveSuggestion() {
            activeIndex = -1;
            getSuggestionElements().forEach(function (el) { el.classList.remove(activeClass); });
        }

        function renderAutocomplete(typedValue, entries) {
            if (!typedEl || !hintEl) { hideAutocomplete(); return; }
            if (!typedValue || !entries || entries.length !== 1) { hideAutocomplete(); return; }
            var fullTitle = entries[0].title;
            var typedLower = normalizeForMatch(typedValue);
            var fullLower = normalizeForMatch(fullTitle);
            if (fullLower.indexOf(typedLower) !== 0) { hideAutocomplete(); return; }
            var remainingPart = fullTitle.substring(typedValue.length);
            if (!remainingPart) { hideAutocomplete(); return; }
            typedEl.textContent = typedValue;
            hintEl.textContent = remainingPart;
            wrapper.classList.add('has-autocomplete');
        }

        function acceptAutocomplete() {
            if (!currentTopMatch) return false;
            input.value = currentTopMatch;
            try { input.setSelectionRange(input.value.length, input.value.length); } catch (e) {}
            hideAutocomplete();
            hideSuggestions();
            syncClear();
            return true;
        }

        function applySuggestion(title) {
            if (!title) return;
            input.value = title;
            try { input.setSelectionRange(title.length, title.length); } catch (e) {}
            syncClear();
            hideSuggestions();
            hideAutocomplete();
        }

        function renderSuggestions(entries) {
            if (!suggestionsBox) return;
            suggestionsBox.textContent = '';

            if (!entries || entries.length === 0) {
                suggestionsBox.classList.remove('show');
                if (suggestionsBox.style) suggestionsBox.style.display = 'none';
                activeIndex = -1;
                return;
            }

            entries.slice(0, SUGGESTIONS_LIMIT).forEach(function (entry) {
                var row = document.createElement('div');
                row.className = suggestionRowClass;
                row.setAttribute('role', 'option');
                row.setAttribute('tabindex', '-1');
                row.setAttribute('data-suggestion-title', entry.title);
                if (entry.movieId) row.setAttribute('data-suggestion-movie-id', entry.movieId);
                if (entry.historyId) row.setAttribute('data-suggestion-history-id', entry.historyId);
                if (entry.addedAt) row.setAttribute('data-suggestion-added-at', entry.addedAt);

                var titleSpan = document.createElement('span');
                titleSpan.className = suggestionTitleClass;
                titleSpan.textContent = entry.title;
                row.appendChild(titleSpan);

                if (entry.meta) {
                    var metaSpan = document.createElement('span');
                    metaSpan.className = suggestionMetaClass;
                    metaSpan.textContent = entry.meta;
                    row.appendChild(metaSpan);
                }

                row.addEventListener('mouseenter', function () {
                    var idx = getSuggestionElements().indexOf(row);
                    if (idx >= 0) setActiveSuggestion(idx);
                });

                suggestionsBox.appendChild(row);
            });

            suggestionsBox.classList.add('show');
            if (suggestionsBox.style) suggestionsBox.style.display = 'flex';
            activeIndex = -1;
        }

        function runFetchSuggestions(query) {
            var url = getSuggestionsUrl(query);
            if (!url) { hideSuggestions(); hideAutocomplete(); pendingMoveToList = false; return; }

            fetchSuggestionsPayload(url).then(function (entries) {
                if (!entries || entries.length === 0) {
                    hideSuggestions();
                    hideAutocomplete();
                    pendingMoveToList = false;
                    return;
                }

                renderSuggestions(entries);

                var typedValue = input.value;
                var normalizedTyped = normalizeForMatch(typedValue);
                var matching = entries.filter(function (e) {
                    return normalizeForMatch(e.title).indexOf(normalizedTyped) === 0;
                });

                if (matching.length === 1) {
                    currentTopMatch = matching[0].title;
                    renderAutocomplete(typedValue, [matching[0]]);
                } else {
                    hideAutocomplete();
                }

                if (pendingMoveToList) {
                    pendingMoveToList = false;
                    if (getSuggestionElements().length > 0) setActiveSuggestion(0);
                }
            });
        }

        function removePendingHighlightField() {
            var existing = form.querySelector('input[data-catalog-search-highlight]');
            if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
        }

        function attachPendingHighlightField(row) {
            removePendingHighlightField();
            if (!row) return;

            var historyId = row.getAttribute('data-suggestion-history-id') || '';
            var addedAt = row.getAttribute('data-suggestion-added-at') || '';

            if (!historyId && !addedAt) return;

            if (historyId) {
                var hiddenId = document.createElement('input');
                hiddenId.type = 'hidden';
                hiddenId.name = 'highlight';
                hiddenId.value = historyId;
                hiddenId.setAttribute('data-catalog-search-highlight', '1');
                form.appendChild(hiddenId);
            }

            if (addedAt) {
                var hiddenAt = document.createElement('input');
                hiddenAt.type = 'hidden';
                hiddenAt.name = 'watchedAt';
                hiddenAt.value = addedAt;
                hiddenAt.setAttribute('data-catalog-search-highlight', '1');
                form.appendChild(hiddenAt);
            }
        }

        function doSubmit(activeRowEl) {
            var raw = String(input.value || '').trim();

            if (hasMeaningfulChars(raw)) {
                var cleanValue = raw;
                if (currentTopMatch && normalizeForMatch(currentTopMatch).indexOf(normalizeForMatch(cleanValue)) === 0) {
                    cleanValue = currentTopMatch;
                } else {
                    cleanValue = capitalizeFirst(cleanValue);
                }
                input.value = cleanValue;
                try { input.setSelectionRange(cleanValue.length, cleanValue.length); } catch (e) {}
            } else {
                input.value = '';
            }

            syncClear();
            hideSuggestions();
            hideAutocomplete();
            resetActiveSuggestion();

            attachPendingHighlightField(activeRowEl || pendingSubmitTarget);
            pendingSubmitTarget = null;

            if (typeof onBeforeSubmit === 'function') onBeforeSubmit();

            if (typeof submitForm === 'function') {
                submitForm();
            } else {
                form.removeAttribute('onsubmit');
                form.submit();
            }
        }

        input.addEventListener('beforeinput', function (e) {
            if (!currentTopMatch) return;
            if (e.inputType !== 'insertText' || !e.data) return;
            var typedLength = input.value.length;
            var expectedChar = currentTopMatch.charAt(typedLength);
            if (e.data.toLowerCase() !== expectedChar.toLowerCase()) {
                hideAutocomplete();
            }
        });

        input.addEventListener('input', function () {
            syncClear();
            resetActiveSuggestion();
            hideAutocomplete();
            pendingMoveToList = false;
            clearTimeout(debounceTimer);

            var value = input.value.trim();
            if (!value || !hasMeaningfulChars(value)) {
                hideSuggestions();
                return;
            }

            debounceTimer = setTimeout(function () {
                runFetchSuggestions(value);
            }, SUGGESTIONS_DEBOUNCE_MS);
        });

        input.addEventListener('keydown', function (e) {
            var items = getSuggestionElements();

            if (e.key === 'ArrowDown') {
                if (items.length > 0) {
                    e.preventDefault();
                    setActiveSuggestion(activeIndex < items.length - 1 ? activeIndex + 1 : 0);
                    return;
                }
                if (hasMeaningfulChars(input.value)) {
                    e.preventDefault();
                    pendingMoveToList = true;
                    clearTimeout(debounceTimer);
                    runFetchSuggestions(input.value.trim());
                    return;
                }
            }

            if (e.key === 'ArrowUp') {
                if (items.length > 0) {
                    e.preventDefault();
                    setActiveSuggestion(activeIndex > 0 ? activeIndex - 1 : items.length - 1);
                    return;
                }
            }

            if (e.key === 'Escape') {
                hideSuggestions();
                hideAutocomplete();
                pendingMoveToList = false;
                return;
            }

            if ((e.key === 'ArrowRight' || e.key === 'Tab') && currentTopMatch) {
                var caretAtEnd = input.selectionStart === input.value.length
                    && input.selectionEnd === input.value.length;
                if (caretAtEnd) {
                    e.preventDefault();
                    acceptAutocomplete();
                    return;
                }
            }

            if (e.key === 'Enter') {
                if (activeIndex >= 0 && items[activeIndex]) {
                    e.preventDefault();
                    var activeRow = items[activeIndex];
                    applySuggestion(activeRow.getAttribute('data-suggestion-title') || '');
                    doSubmit(activeRow);
                    return;
                }
                e.preventDefault();
                doSubmit(null);
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () {
                if (pointerDownInsideBox) { pointerDownInsideBox = false; return; }
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
                pendingMoveToList = false;
            }, 150);
        });

        if (suggestionsBox) {
            suggestionsBox.addEventListener('pointerdown', function (e) {
                var row = e.target.closest('.' + suggestionRowClass);
                if (!row) return;
                e.preventDefault();
                e.stopPropagation();
                pointerDownInsideBox = true;
                pendingSubmitTarget = row;
                var title = row.getAttribute('data-suggestion-title') || '';
                if (title) {
                    applySuggestion(title);
                    doSubmit(row);
                }
            }, true);
        }

        if (clear) {
            clear.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                syncClear();
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
                pendingSubmitTarget = null;
                removePendingHighlightField();
                clearTimeout(debounceTimer);
                if (typeof submitForm === 'function') submitForm();
                else { form.removeAttribute('onsubmit'); form.submit(); }
            });
        }

        form.addEventListener('submit', function (e) { e.preventDefault(); doSubmit(null); });

        syncClear();
        hideSuggestions();
        hideAutocomplete();

        return {
            submit: function () { doSubmit(null); },
            setValue: function (v) { input.value = v || ''; syncClear(); },
            focus: function () { try { input.focus(); } catch (e) {} }
        };
    }

    function initCatalogSearch() {
        var form = document.querySelector('[data-catalog-search-form]');
        var input = document.querySelector('[data-catalog-search-input]');
        var clear = document.querySelector('[data-catalog-search-clear]');
        var wrapper = document.querySelector('[data-catalog-search-wrapper]');
        var suggestionsBox = document.querySelector('[data-catalog-search-suggestions]');
        var typedEl = document.querySelector('[data-catalog-search-typed]');
        var hintEl = document.querySelector('[data-catalog-search-hint]');

        if (!form || !input || !wrapper) return;

        var endpoint = detectCatalogSuggestionsEndpoint();

        createSearchEngine({
            form: form,
            input: input,
            wrapper: wrapper,
            clear: clear,
            suggestionsBox: suggestionsBox,
            typedEl: typedEl,
            hintEl: hintEl,
            suggestionRowClass: 'catalog-search-suggestion',
            suggestionTitleClass: 'catalog-search-suggestion-title',
            suggestionMetaClass: 'catalog-search-suggestion-meta',
            getSuggestionsUrl: function (query) {
                if (!endpoint) return null;
                return endpoint + '?query=' + encodeURIComponent(query);
            },
            submitForm: function () { form.submit(); }
        });
    }

    function initHeaderSearch() {
        var headerSearchInput = document.getElementById('headerSearchInput');
        var headerSuggestionsBox = document.getElementById('headerSuggestions');
        var headerSearchForm = document.getElementById('headerSearchForm');
        var headerSearchWrapper = document.getElementById('headerSearchWrapper');
        var headerAutocompleteTypedElement = document.getElementById('headerAutocompleteTyped');
        var headerAutocompleteHintElement = document.getElementById('headerAutocompleteHint');
        var headerFiltersButton = document.getElementById('headerFiltersBtn');
        var headerSearchSubmitButton = document.getElementById('headerSearchSubmitBtn');
        var headerSearchClearButton = document.getElementById('headerSearchClear');

        if (!headerSearchInput || !headerSuggestionsBox || !headerSearchForm || !headerSearchWrapper
                || !headerAutocompleteTypedElement || !headerAutocompleteHintElement) return;

        function getUrlTitle() {
            try {
                var params = new URLSearchParams(window.location.search);
                return params.get('title') || '';
            } catch (e) { return ''; }
        }

        function restoreQuery(engine) {
            var urlTitle = getUrlTitle();
            if (urlTitle) { engine.setValue(urlTitle); return; }
            if (isMovieDetailsPage()) {
                var saved = readLastSearch();
                engine.setValue(saved || '');
                return;
            }
            engine.setValue('');
        }

        var engine = createSearchEngine({
            form: headerSearchForm,
            input: headerSearchInput,
            wrapper: headerSearchWrapper,
            clear: headerSearchClearButton,
            suggestionsBox: headerSuggestionsBox,
            typedEl: headerAutocompleteTypedElement,
            hintEl: headerAutocompleteHintElement,
            suggestionRowClass: 'suggestion-item',
            suggestionTitleClass: 'suggestion-title',
            suggestionMetaClass: 'suggestion-meta',
            getSuggestionsUrl: function (query) {
                return '/movies/suggestions?query=' + encodeURIComponent(query);
            },
            submitForm: function () {
                if (typeof window.submitCombinedSearch === 'function') {
                    window.submitCombinedSearch();
                } else {
                    headerSearchForm.removeAttribute('onsubmit');
                    headerSearchForm.submit();
                }
            },
            onBeforeSubmit: function () {
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) {
                    throw new Error('validateSearchQuery rejected');
                }
            }
        });

        if (!engine) return;
        restoreQuery(engine);

        if (headerFiltersButton && !headerFiltersButton.dataset.bound) {
            headerFiltersButton.dataset.bound = '1';
            headerFiltersButton.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                if (typeof toggleFiltersModal === 'function') toggleFiltersModal(true);
            });
        }

        if (headerSearchSubmitButton && !headerSearchSubmitButton.dataset.bound) {
            headerSearchSubmitButton.dataset.bound = '1';
            headerSearchSubmitButton.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                engine.submit();
            });
        }

        if (headerSearchClearButton && !headerSearchClearButton.dataset.bound) {
            headerSearchClearButton.dataset.bound = '1';
            headerSearchClearButton.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                headerSearchInput.value = '';
                if (typeof clearPending === 'function') clearPending();
                engine.setValue('');
                headerSearchInput.focus();
            });
        }

        window.addEventListener('pageshow', function (pageShowEvent) {
            if (pageShowEvent.persisted) restoreQuery(engine);
        });
    }

    window.initCatalogSearch = initCatalogSearch;
    window.initHeaderSearch = initHeaderSearch;

    function initAll() {
        initCatalogSearch();
        initHeaderSearch();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll, { once: true });
    } else {
        initAll();
    }
})();