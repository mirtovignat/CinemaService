(function () {
    'use strict';

    function normalize(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .trim()
            .replace(/ё/g, 'е')
            .replace(/[^a-zа-я0-9]/gi, '');
    }

    function hasMeaningfulChars(value) {
        return /[a-zа-я0-9ё]/i.test(String(value || ''));
    }

    function capitalizeFirst(value) {
        if (!value) return '';
        if (value.length === 1) return value.toLocaleUpperCase('ru-RU');
        return value.charAt(0).toLocaleUpperCase('ru-RU') +
               value.slice(1).toLocaleLowerCase('ru-RU');
    }

    function formatAddedDate(raw) {
        if (!raw) return '';
        var d = new Date(raw);
        if (isNaN(d.getTime())) return '';
        var pad = function (n) { return String(n).padStart(2, '0'); };
        return pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear() +
               ', ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function getTitleElement(item) {
        var selectors = [
            '.history-item-title',
            '.media-card-title a',
            '.media-card-title',
            '.movie-card-title a',
            '.movie-card-title',
            '.library-card-title a',
            '.library-card-title',
            '.favourites-card-title a',
            '.favourites-card-title',
            '.cart-card-title a',
            '.cart-card-title'
        ];
        for (var i = 0; i < selectors.length; i++) {
            var el = item.querySelector(selectors[i]);
            if (el) return el;
        }
        return null;
    }

    function getTitleFromItem(item) {
        var el = getTitleElement(item);
        if (!el) return '';
        return (el.getAttribute('data-title') || el.textContent || '').trim();
    }

    function getMovieIdFromItem(item) {
        return item.getAttribute('data-movie-id') || '';
    }

    function getHistoryIdFromItem(item) {
        return item.getAttribute('data-history-id') || '';
    }

    function getMetaFromItem(item) {
        var raw = item.getAttribute('data-added-at') || item.getAttribute('data-date') || '';
        return formatAddedDate(raw);
    }

    function getItemUniqueKey(item) {
        var historyId = getHistoryIdFromItem(item);
        if (historyId) return 'h_' + historyId;
        var movieId = getMovieIdFromItem(item);
        if (movieId) return 'm_' + movieId;
        return '';
    }

    function getContainers() {
        var containers = [];
        document.querySelectorAll('.history-container').forEach(function (el) {
            containers.push({ root: el, itemSelector: '.history-item' });
        });
        document.querySelectorAll('.library-container').forEach(function (el) {
            containers.push({ root: el, itemSelector: '.library-card' });
        });
        document.querySelectorAll('.favourites-container').forEach(function (el) {
            containers.push({ root: el, itemSelector: '.favourites-card' });
        });
        document.querySelectorAll('.cart-container').forEach(function (el) {
            containers.push({ root: el, itemSelector: '.cart-card' });
        });
        document.querySelectorAll('.movies-grid').forEach(function (el) {
            containers.push({ root: el, itemSelector: '.movie-card' });
        });
        return containers;
    }

    function collectAllItems() {
        var containers = getContainers();
        var result = [];
        containers.forEach(function (config) {
            config.root.querySelectorAll(config.itemSelector).forEach(function (item) {
                var title = getTitleFromItem(item);
                if (!title) return;
                result.push({
                    element: item,
                    title: title,
                    normalized: normalize(title),
                    id: getMovieIdFromItem(item),
                    historyId: getHistoryIdFromItem(item),
                    uniqueKey: getItemUniqueKey(item),
                    meta: getMetaFromItem(item)
                });
            });
        });
        return result;
    }

    function showAllItems() {
        getContainers().forEach(function (config) {
            config.root.querySelectorAll(config.itemSelector).forEach(function (item) {
                item.style.display = '';
            });
        });
        updateGroupVisibility();
        updateEmptyState(false);
    }

    function hideAllItems() {
        getContainers().forEach(function (config) {
            config.root.querySelectorAll(config.itemSelector).forEach(function (item) {
                item.style.display = 'none';
            });
        });
        updateGroupVisibility();
        updateEmptyState(true);
    }

    function applyFilterToItems(visibleItems) {
        var visibleSet = new Set(visibleItems.map(function (x) { return x.element; }));
        getContainers().forEach(function (config) {
            config.root.querySelectorAll(config.itemSelector).forEach(function (item) {
                item.style.display = visibleSet.has(item) ? '' : 'none';
            });
        });
        updateGroupVisibility();
        updateEmptyState(visibleItems.length === 0);
    }

    function updateGroupVisibility() {
        document.querySelectorAll('.history-day-group').forEach(function (group) {
            var anyVisible = Array.from(
                group.querySelectorAll('.history-item')
            ).some(function (item) { return item.style.display !== 'none'; });
            group.style.display = anyVisible ? '' : 'none';
        });
    }

    function updateEmptyState(showEmpty) {
        var emptyEl = document.querySelector('[data-catalog-search-empty]');
        if (emptyEl) emptyEl.classList.toggle('show', !!showEmpty);
    }

    function applyFilterByText(query) {
        var raw = String(query || '').trim();

        if (!raw) { showAllItems(); return; }

        if (!hasMeaningfulChars(raw)) {
            hideAllItems();
            return;
        }

        var normalized = normalize(raw);
        if (!normalized) {
            hideAllItems();
            return;
        }

        var matched = collectAllItems().filter(function (entry) {
            return entry.normalized.indexOf(normalized) === 0;
        });
        applyFilterToItems(matched);
    }

    function applyFilterByUniqueKey(uniqueKey) {
        if (!uniqueKey) { showAllItems(); return; }
        var matched = collectAllItems().filter(function (entry) {
            return entry.uniqueKey === uniqueKey;
        });
        applyFilterToItems(matched);
    }

    function longestCommonPrefix(titles) {
        if (!titles || titles.length === 0) return '';
        if (titles.length === 1) return titles[0];

        var first = titles[0];
        var last = titles[0];
        for (var i = 1; i < titles.length; i++) {
            if (titles[i] < first) first = titles[i];
            if (titles[i] > last) last = titles[i];
        }

        var len = 0;
        var max = Math.min(first.length, last.length);
        while (len < max && first.charAt(len) === last.charAt(len)) {
            len++;
        }
        return first.substring(0, len);
    }

    var LAST_SEARCH_KEY = 'lastSearchQuery';
    var LAST_SEARCH_AT_KEY = 'lastSearchAt';
    var PENDING_KEY = 'pendingSearchRedirect';

    function saveLastSearch(query) {
        if (!query) return;
        try {
            sessionStorage.setItem(LAST_SEARCH_KEY, query);
            sessionStorage.setItem(LAST_SEARCH_AT_KEY, String(Date.now()));
        } catch (e) {}
    }

    function readLastSearch() {
        try {
            var value = sessionStorage.getItem(LAST_SEARCH_KEY);
            if (!value) return null;
            return value;
        } catch (e) {
            return null;
        }
    }

    function clearLastSearch() {
        try {
            sessionStorage.removeItem(LAST_SEARCH_KEY);
            sessionStorage.removeItem(LAST_SEARCH_AT_KEY);
        } catch (e) {}
    }

    function markPending() {
        try {
            sessionStorage.setItem(PENDING_KEY, '1');
        } catch (e) {}
    }

    function consumePending() {
        try {
            var v = sessionStorage.getItem(PENDING_KEY);
            if (v === '1') {
                sessionStorage.removeItem(PENDING_KEY);
                return true;
            }
        } catch (e) {}
        return false;
    }

    function clearPending() {
        try {
            sessionStorage.removeItem(PENDING_KEY);
        } catch (e) {}
    }

    function isMovieDetailsPage() {
        return !!document.querySelector('.movie-details-container');
    }

    (function clearStaleSearchStateOnNonShowPages() {
        if (isMovieDetailsPage()) return;
        clearPending();
        clearLastSearch();
    })();

    function initCatalogSearch() {
        var input = document.querySelector('[data-catalog-search-input]');
        var clearBtn = document.querySelector('[data-catalog-search-clear]');
        var wrapper = document.querySelector('[data-catalog-search-wrapper]');
        var box = document.querySelector('[data-catalog-search-suggestions]');
        var typedEl = document.querySelector('[data-catalog-search-typed]');
        var hintEl = document.querySelector('[data-catalog-search-hint]');
        if (!input || !wrapper) return;

        var ELLIPSIS = '…';
        var currentTopMatch = null;
        var lockedUniqueKey = null;

        function isTruncated(value) {
            return typeof value === 'string'
                && value.length > 0
                && value.charAt(value.length - 1) === ELLIPSIS;
        }

        function stripEllipsis(value) {
            if (isTruncated(value)) return value.substring(0, value.length - 1);
            return value;
        }

        function updateClearVisibility() {
            wrapper.classList.toggle('has-value', input.value.trim().length > 0);
        }

        function hideSuggestions() {
            if (box) { box.classList.remove('show'); box.textContent = ''; }
        }

        function hideAutocomplete() {
            if (!typedEl || !hintEl) return;
            currentTopMatch = null;
            wrapper.classList.remove('has-autocomplete');
            typedEl.textContent = '';
            hintEl.textContent = '';
        }

        function normalizeForMatch(value) {
            return String(value || '')
                .toLocaleLowerCase('ru-RU')
                .replace(/ё/g, 'е');
        }

        function getMatches(value) {
            if (!hasMeaningfulChars(value)) return [];
            var normalized = normalize(value);
            if (!normalized) return [];
            return collectAllItems().filter(function (entry) {
                return entry.normalized.indexOf(normalized) === 0;
            });
        }

        function renderAutocomplete(typed, titles) {
            if (!typed || !titles || titles.length !== 1 || !typedEl || !hintEl) {
                hideAutocomplete();
                return;
            }

            var fullTitle = titles[0];
            var typedLower = normalizeForMatch(typed);
            var fullLower = normalizeForMatch(fullTitle);

            if (fullLower.indexOf(typedLower) !== 0) {
                hideAutocomplete();
                return;
            }

            var rest = fullTitle.substring(typed.length);
            if (!rest) {
                hideAutocomplete();
                return;
            }

            typedEl.textContent = typed;
            hintEl.textContent = rest;
            wrapper.classList.add('has-autocomplete');
        }

        function buildTruncatedQuery(clean) {
            var matches = getMatches(clean);
            if (matches.length === 0) return clean;
            if (matches.length === 1) return matches[0].title;

            var titles = matches.map(function (m) { return m.title; });
            var prefix = longestCommonPrefix(titles);

            if (!prefix || prefix.length <= clean.length) {
                return clean + ELLIPSIS;
            }
            return prefix + ELLIPSIS;
        }

        function applyEllipsisIfNeeded() {
            if (lockedUniqueKey) return;

            var rawValue = String(input.value || '').trim();
            if (!rawValue) return;

            if (!hasMeaningfulChars(rawValue)) {
                return;
            }

            var clean = stripEllipsis(rawValue);
            var matches = getMatches(clean);
            if (matches.length === 1) {
                var title = matches[0].title;
                input.value = title;
                input.setSelectionRange(title.length, title.length);
            } else if (matches.length > 1) {
                input.value = buildTruncatedQuery(clean);
            } else {
                input.value = clean;
            }
        }

        function lockByUniqueKey(uniqueKey) {
            lockedUniqueKey = uniqueKey || null;
        }

        function unlock() {
            lockedUniqueKey = null;
        }

        function acceptAutocomplete() {
            if (!currentTopMatch) return false;
            var matches = getMatches(currentTopMatch);
            if (matches.length === 0) return false;

            var entry = matches[0];
            input.value = entry.title;
            input.setSelectionRange(entry.title.length, entry.title.length);

            hideAutocomplete();
            hideSuggestions();

            lockByUniqueKey(entry.uniqueKey);
            applyFilterByUniqueKey(entry.uniqueKey);
            return true;
        }

        function applySuggestion(entry) {
            if (!entry) return;

            input.value = entry.title;
            input.setSelectionRange(entry.title.length, entry.title.length);
            updateClearVisibility();
            hideSuggestions();
            hideAutocomplete();

            lockByUniqueKey(entry.uniqueKey);
            applyFilterByUniqueKey(entry.uniqueKey);
        }

        function prepareForSubmit() {
            if (currentTopMatch) {
                var matches = getMatches(currentTopMatch);
                if (matches.length === 1) {
                    input.value = matches[0].title;
                } else {
                    input.value = currentTopMatch;
                }
                input.setSelectionRange(input.value.length, input.value.length);
                hideAutocomplete();
                return;
            }

            var raw = String(input.value || '').trim();
            if (!raw) return;
            if (!hasMeaningfulChars(raw)) return;

            var normalized = capitalizeFirst(raw);
            input.value = normalized;
            input.setSelectionRange(normalized.length, normalized.length);
        }

        var activeSuggestionIndex = -1;

        function getSuggestionEls() {
            if (!box) return [];
            return Array.prototype.slice.call(box.querySelectorAll('.catalog-search-suggestion'));
        }

        function setActiveSuggestion(index) {
            var els = getSuggestionEls();
            els.forEach(function (el) { el.classList.remove('is-active'); });
            activeSuggestionIndex = index;
            if (index >= 0 && index < els.length) {
                els[index].classList.add('is-active');
                try {
                    els[index].scrollIntoView({ block: 'nearest' });
                } catch (e) {}
            }
        }

        function resetActiveSuggestion() {
            activeSuggestionIndex = -1;
            getSuggestionEls().forEach(function (el) { el.classList.remove('is-active'); });
        }

        function handleArrow(direction) {
            var els = getSuggestionEls();
            if (!box || els.length === 0 || !box.classList.contains('show')) {
                return false;
            }
            var next;
            if (direction === 'down') {
                if (activeSuggestionIndex < 0) {
                    next = 0;
                } else {
                    next = (activeSuggestionIndex + 1) % els.length;
                }
            } else {
                if (activeSuggestionIndex < 0) {
                    next = els.length - 1;
                } else {
                    next = (activeSuggestionIndex - 1 + els.length) % els.length;
                }
            }
            setActiveSuggestion(next);
            return true;
        }

        input.addEventListener('input', function () {
            unlock();
            resetActiveSuggestion();

            if (isTruncated(input.value)) {
                input.value = stripEllipsis(input.value);
            }
            updateClearVisibility();
            hideAutocomplete();

            var value = input.value;
            if (!value) {
                hideSuggestions();
                showAllItems();
                return;
            }

            if (!hasMeaningfulChars(value)) {
                hideSuggestions();
                hideAllItems();
                return;
            }

            var matches = getMatches(value);

            if (matches.length === 0) {
                hideAllItems();
            }

            if (matches.length > 0) {
                currentTopMatch = matches[0].title;
                if (matches.length === 1) {
                    renderAutocomplete(value, [matches[0].title]);
                }
            }

            if (box) {
                box.textContent = '';
                if (matches.length === 0) {
                    box.classList.remove('show');
                } else {
                    matches.slice(0, 8).forEach(function (entry) {
                        var el = document.createElement('div');
                        el.className = 'catalog-search-suggestion';
                        el.setAttribute('data-suggestion-id', entry.id);
                        el.setAttribute('data-suggestion-key', entry.uniqueKey);

                        var t = document.createElement('span');
                        t.className = 'catalog-search-suggestion-title';
                        t.textContent = entry.title;
                        el.appendChild(t);

                        if (entry.meta) {
                            var m = document.createElement('span');
                            m.className = 'catalog-search-suggestion-meta';
                            m.textContent = entry.meta;
                            el.appendChild(m);
                        }

                        el.addEventListener('pointerdown', function (e) {
                            e.preventDefault();
                            applySuggestion(entry);
                            input.blur();
                        });

                        el.addEventListener('mouseenter', function () {
                            var idx = getSuggestionEls().indexOf(el);
                            if (idx >= 0) setActiveSuggestion(idx);
                        });

                        box.appendChild(el);
                    });
                    box.classList.add('show');
                }
            }
        });

        input.addEventListener('focus', function () {
            if (isTruncated(input.value)) {
                input.value = stripEllipsis(input.value);
                updateClearVisibility();
            }
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') {
                if (handleArrow('down')) {
                    e.preventDefault();
                    return;
                }
            }

            if (e.key === 'ArrowUp') {
                if (handleArrow('up')) {
                    e.preventDefault();
                    return;
                }
            }

            if (e.key === 'Enter') {
                e.preventDefault();

                if (activeSuggestionIndex >= 0) {
                    var activeEl = getSuggestionEls()[activeSuggestionIndex];
                    if (activeEl) {
                        var key = activeEl.getAttribute('data-suggestion-key');
                        var title = activeEl.querySelector('.catalog-search-suggestion-title');
                        var text = title ? title.textContent.trim() : '';
                        if (text) {
                            input.value = text;
                            input.setSelectionRange(text.length, text.length);
                            updateClearVisibility();
                            hideSuggestions();
                            hideAutocomplete();
                            lockByUniqueKey(key);
                            applyFilterByUniqueKey(key);
                            resetActiveSuggestion();
                            return;
                        }
                    }
                }

                var raw = String(input.value || '').trim();

                if (!hasMeaningfulChars(raw)) {
                    hideSuggestions();
                    hideAutocomplete();
                    hideAllItems();
                    return;
                }

                prepareForSubmit();
                var clean = stripEllipsis(input.value);
                applyFilterByText(clean);
                applyEllipsisIfNeeded();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
                return;
            }

            if (e.key === 'Tab' || e.key === 'ArrowRight') {
                if (wrapper.classList.contains('has-autocomplete')) {
                    var caretAtEnd = input.selectionStart === input.value.length
                        && input.selectionEnd === input.value.length;
                    if (caretAtEnd) {
                        e.preventDefault();
                        acceptAutocomplete();
                        updateClearVisibility();
                        return;
                    }
                }
            }

            if (e.key === 'Escape') {
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () {
                if (!input.value) { hideSuggestions(); return; }
                if (!hasMeaningfulChars(input.value)) {
                    hideSuggestions();
                    hideAutocomplete();
                    return;
                }
                applyEllipsisIfNeeded();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
            }, 150);
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', function () {
                input.value = '';
                unlock();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
                showAllItems();
                input.focus();
            });
        }

        updateClearVisibility();
    }

    function initHeaderSearch() {
        var input      = document.getElementById('headerSearchInput');
        var box        = document.getElementById('headerSuggestions');
        var form       = document.getElementById('headerSearchForm');
        var wrapper    = document.getElementById('headerSearchWrapper');
        var typedEl    = document.getElementById('headerAutocompleteTyped');
        var hintEl     = document.getElementById('headerAutocompleteHint');
        var filtersBtn = document.getElementById('headerFiltersBtn');
        var submitBtn  = document.getElementById('headerSearchSubmitBtn');
        var clearBtn   = document.getElementById('headerSearchClear');

        if (!input || !box || !form || !wrapper || !typedEl || !hintEl) return;

        var ELLIPSIS = '…';

        function getUrlTitle() {
            try {
                var params = new URLSearchParams(window.location.search);
                return params.get('title') || '';
            } catch (e) {
                return '';
            }
        }

        function isTruncated(value) {
            return typeof value === 'string'
                && value.length > 0
                && value.charAt(value.length - 1) === ELLIPSIS;
        }

        function getCardTitles() {
            return Array.from(document.querySelectorAll('.movies-grid .movie-card'))
                .map(function (card) {
                    var t = card.querySelector('.movie-card-title a, .media-card-title a');
                    return t ? t.textContent.trim() : '';
                })
                .filter(function (t) { return t.length > 0; });
        }

        function findSingleCardMatch(query) {
            var cards = getCardTitles();
            if (cards.length === 0) return null;
            var q = normalize(query);
            if (!q) return null;

            var matches = cards.filter(function (t) {
                return normalize(t).indexOf(q) === 0;
            });

            if (matches.length === 1) return matches[0];
            return null;
        }

        function updateClearVisibility() {
            wrapper.classList.toggle('has-value', input.value.trim().length > 0);
        }

        function buildTruncatedFromCards(source) {
            var cards = getCardTitles();
            if (cards.length <= 1) return source;

            var prefix = longestCommonPrefix(cards);
            if (!prefix || prefix.length <= source.length) {
                return source + ELLIPSIS;
            }
            return prefix + ELLIPSIS;
        }

        function hideAutocomplete() {
            currentTopMatch = null;
            wrapper.classList.remove('has-autocomplete');
            typedEl.textContent = '';
            hintEl.textContent  = '';
        }

        function hideDropdown() {
            box.style.display = 'none';
            box.textContent = '';
        }

        function resetSearchState() {
            input.value = '';
            updateClearVisibility();
            hideDropdown();
            hideAutocomplete();
        }

        function restoreQuery() {
            var urlTitle = getUrlTitle();
            if (urlTitle) {
                input.value = buildTruncatedFromCards(urlTitle);
                return;
            }

            if (isMovieDetailsPage()) {
                var saved = readLastSearch();
                if (saved) {
                    input.value = saved;
                    updateClearVisibility();
                    return;
                }
                resetSearchState();
                return;
            }

            resetSearchState();
        }

        restoreQuery();
        updateClearVisibility();

        var debounceTimer = null;
        var currentTopMatch = null;
        var pointerDownInsideBox = false;
        var activeSuggestionIndex = -1;
        var pendingMoveToList = false;

        function normalizeForMatch(value) {
            return String(value || '')
                .toLocaleLowerCase('ru-RU')
                .replace(/ё/g, 'е');
        }

        function renderAutocomplete(typed, titles) {
            if (!typed || !titles || titles.length !== 1 || !typedEl || !hintEl) {
                hideAutocomplete();
                return;
            }

            var fullTitle = titles[0];
            var typedLower = normalizeForMatch(typed);
            var fullLower = normalizeForMatch(fullTitle);

            if (fullLower.indexOf(typedLower) !== 0) {
                hideAutocomplete();
                return;
            }

            var rest = fullTitle.substring(typed.length);
            if (!rest) {
                hideAutocomplete();
                return;
            }

            typedEl.textContent = typed;
            hintEl.textContent = rest;
            wrapper.classList.add('has-autocomplete');
        }

        function acceptAutocomplete() {
            if (!currentTopMatch) return false;
            input.value = currentTopMatch;
            input.setSelectionRange(input.value.length, input.value.length);
            hideAutocomplete();
            updateClearVisibility();
            return true;
        }

        function renderDropdownItems(list) {
            box.textContent = '';
            list.forEach(function (item) {
                var row = document.createElement('div');
                row.className = 'suggestion-item';
                row.setAttribute('data-id', item.id || '');

                var t = document.createElement('span');
                t.className = 'suggestion-title';
                t.textContent = item.title || '';
                row.appendChild(t);

                row.addEventListener('mouseenter', function () {
                    var idx = getSuggestionEls().indexOf(row);
                    if (idx >= 0) setActiveSuggestion(idx);
                });

                box.appendChild(row);
            });
        }

        function getSuggestionEls() {
            return Array.prototype.slice.call(box.querySelectorAll('.suggestion-item'));
        }

        function setActiveSuggestion(index) {
            var els = getSuggestionEls();
            els.forEach(function (el) { el.classList.remove('is-active'); });
            activeSuggestionIndex = index;
            if (index >= 0 && index < els.length) {
                els[index].classList.add('is-active');
                try {
                    els[index].scrollIntoView({ block: 'nearest' });
                } catch (e) {}
            }
        }

        function resetActiveSuggestion() {
            activeSuggestionIndex = -1;
            getSuggestionEls().forEach(function (el) { el.classList.remove('is-active'); });
        }

        function handleArrow(direction) {
            var els = getSuggestionEls();
            if (els.length === 0) return false;
            if (box.style.display !== 'flex') return false;
            var next;
            if (direction === 'down') {
                if (activeSuggestionIndex < 0) {
                    next = 0;
                } else {
                    next = (activeSuggestionIndex + 1) % els.length;
                }
            } else {
                if (activeSuggestionIndex < 0) {
                    next = els.length - 1;
                } else {
                    next = (activeSuggestionIndex - 1 + els.length) % els.length;
                }
            }
            setActiveSuggestion(next);
            return true;
        }

        function applyActiveSuggestion() {
            if (activeSuggestionIndex < 0) return false;
            var row = getSuggestionEls()[activeSuggestionIndex];
            if (!row) return false;

            var titleEl = row.querySelector('.suggestion-title');
            var text = titleEl ? titleEl.textContent.trim() : '';
            if (!text) return false;

            input.value = text;
            input.setSelectionRange(text.length, text.length);
            hideAutocomplete();
            hideDropdown();
            updateClearVisibility();
            resetActiveSuggestion();
            input.focus();

            if (typeof validateSearchQuery === 'function' && validateSearchQuery()) {
                commitAndSubmit();
            }
            return true;
        }

        function commitAndSubmit() {
            var raw = String(input.value || '').trim();
            if (!hasMeaningfulChars(raw)) {
                hideAutocomplete();
                hideDropdown();
                return;
            }

            var clean = raw;
            if (isTruncated(clean)) {
                clean = clean.substring(0, clean.length - 1).trim();
            }

            if (currentTopMatch) {
                var typedLower = normalizeForMatch(clean);
                var topLower = normalizeForMatch(currentTopMatch);
                if (topLower.indexOf(typedLower) === 0) {
                    clean = currentTopMatch;
                }
            } else {
                var cardMatch = findSingleCardMatch(clean);
                if (cardMatch) {
                    clean = cardMatch;
                } else {
                    clean = capitalizeFirst(clean);
                }
            }

            input.value = clean;
            input.setSelectionRange(clean.length, clean.length);
            updateClearVisibility();
            hideDropdown();

            var truncated = buildTruncatedFromCards(clean);

            if (truncated && truncated.length > clean.length) {
                typedEl.textContent = clean;
                hintEl.textContent = truncated.substring(clean.length);
                wrapper.classList.add('has-autocomplete');
            } else {
                hideAutocomplete();
            }

            saveLastSearch(clean);
            markPending();

            window.setTimeout(function () {
                form.submit();
            }, 120);
        }

        function applySuggestion(row) {
            if (!row) return;
            var titleEl = row.querySelector('.suggestion-title');
            var text = titleEl ? titleEl.textContent.trim() : '';
            if (!text) return;

            input.value = text;
            input.setSelectionRange(text.length, text.length);
            hideAutocomplete();
            hideDropdown();
            updateClearVisibility();
            resetActiveSuggestion();
            input.focus();

            if (typeof validateSearchQuery === 'function' && validateSearchQuery()) {
                commitAndSubmit();
            }
        }

        box.addEventListener('pointerdown', function (e) {
            var row = e.target.closest('.suggestion-item');
            if (!row) return;
            e.preventDefault();
            e.stopPropagation();
            pointerDownInsideBox = true;
            applySuggestion(row);
        }, true);

        function fetchSuggestions(query) {
            if (!query || !hasMeaningfulChars(query)) {
                hideDropdown();
                hideAutocomplete();
                resetActiveSuggestion();
                return;
            }

            fetch('/movies/suggestions?query=' + encodeURIComponent(query))
                .then(function (resp) { return resp.json(); })
                .then(function (data) {
                    if (!data || data.length === 0) {
                        hideDropdown();
                        hideAutocomplete();
                        resetActiveSuggestion();
                        pendingMoveToList = false;
                        return;
                    }
                    renderDropdownItems(data);
                    box.style.display = 'flex';
                    resetActiveSuggestion();

                    var typedVal = input.value;
                    var q = normalizeForMatch(typedVal);
                    var titles = data
                        .map(function (it) { return it && it.title ? it.title : ''; })
                        .filter(function (t) { return t.length > 0; });

                    var matching = titles.filter(function (t) {
                        return normalizeForMatch(t).indexOf(q) === 0;
                    });

                    if (matching.length === 1) {
                        currentTopMatch = matching[0];
                        renderAutocomplete(typedVal, [matching[0]]);
                    } else {
                        hideAutocomplete();
                    }

                    if (pendingMoveToList) {
                        pendingMoveToList = false;
                        setActiveSuggestion(0);
                    }
                })
                .catch(function () {
                    hideDropdown();
                    hideAutocomplete();
                    resetActiveSuggestion();
                    pendingMoveToList = false;
                });
        }

        input.addEventListener('beforeinput', function (e) {
            if (!currentTopMatch) return;
            if (e.inputType !== 'insertText' || !e.data) return;
            var typedLen = input.value.length;
            var expected = currentTopMatch.charAt(typedLen);
            if (e.data.toLowerCase() !== expected.toLowerCase()) {
                hideAutocomplete();
            }
        });

        input.addEventListener('input', function () {
            var val = this.value;

            if (isTruncated(val)) {
                val = val.substring(0, val.length - 1);
                this.value = val;
            }

            hideAutocomplete();
            updateClearVisibility();
            resetActiveSuggestion();
            pendingMoveToList = false;

            if (val === '') {
                hideDropdown();
                return;
            }

            if (!hasMeaningfulChars(val)) {
                hideDropdown();
                return;
            }

            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(function () { fetchSuggestions(val.trim()); }, 300);
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') {
                if (handleArrow('down')) {
                    e.preventDefault();
                    return;
                }
                if (hasMeaningfulChars(input.value)) {
                    e.preventDefault();
                    pendingMoveToList = true;
                    clearTimeout(debounceTimer);
                    fetchSuggestions(input.value.trim());
                    return;
                }
            }

            if (e.key === 'ArrowUp') {
                if (handleArrow('up')) {
                    e.preventDefault();
                    return;
                }
            }

            if (currentTopMatch && (e.key === 'ArrowRight' || e.key === 'Tab')) {
                var caretAtEnd = input.selectionStart === input.value.length
                    && input.selectionEnd === input.value.length;
                if (caretAtEnd) {
                    e.preventDefault();
                    acceptAutocomplete();
                    return;
                }
            }
            if (e.key === 'Escape') {
                hideAutocomplete();
                hideDropdown();
                resetActiveSuggestion();
                pendingMoveToList = false;
                return;
            }
            if (e.key === 'Enter') {
                e.preventDefault();
                if (activeSuggestionIndex >= 0) {
                    if (applyActiveSuggestion()) return;
                }
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                commitAndSubmit();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () {
                if (pointerDownInsideBox) {
                    pointerDownInsideBox = false;
                    return;
                }
                hideDropdown();
                resetActiveSuggestion();
                pendingMoveToList = false;
            }, 200);
        });

        if (filtersBtn && !filtersBtn.dataset.bound) {
            filtersBtn.dataset.bound = '1';
            filtersBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (typeof toggleFiltersModal === 'function') toggleFiltersModal(true);
            });
        }

        if (submitBtn && !submitBtn.dataset.bound) {
            submitBtn.dataset.bound = '1';
            submitBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                commitAndSubmit();
            });
        }

        if (!form.dataset.bound) {
            form.dataset.bound = '1';
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                commitAndSubmit();
            });
        }

        if (clearBtn && !clearBtn.dataset.bound) {
            clearBtn.dataset.bound = '1';
            clearBtn.addEventListener('click', function (e) {
                e.preventDefault();
                input.value = '';
                clearPending();
                hideAutocomplete();
                hideDropdown();
                resetActiveSuggestion();
                pendingMoveToList = false;
                updateClearVisibility();
                input.focus();
            });
        }

        window.addEventListener('pageshow', function (e) {
            if (e.persisted) restoreQuery();
        });
    }

    window.catalogSearchNormalize = normalize;
    window.catalogSearchCollectAll = collectAllItems;
    window.catalogSearchShowAll = showAllItems;
    window.catalogSearchHideAll = hideAllItems;
    window.catalogSearchApplyByText = applyFilterByText;
    window.catalogSearchApplyByUniqueKey = applyFilterByUniqueKey;
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