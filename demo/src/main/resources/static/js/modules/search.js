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
        var dateValue = new Date(raw);
        if (isNaN(dateValue.getTime())) return '';
        var padTwo = function (n) { return String(n).padStart(2, '0'); };
        return padTwo(dateValue.getDate()) + '.' + padTwo(dateValue.getMonth() + 1) + '.' + dateValue.getFullYear() +
               ', ' + padTwo(dateValue.getHours()) + ':' + padTwo(dateValue.getMinutes());
    }

    function getTitleElement(itemElement) {
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
        for (var selectorIndex = 0; selectorIndex < selectors.length; selectorIndex++) {
            var element = itemElement.querySelector(selectors[selectorIndex]);
            if (element) return element;
        }
        return null;
    }

    function getTitleFromItem(itemElement) {
        var titleElement = getTitleElement(itemElement);
        if (!titleElement) return '';
        return (titleElement.getAttribute('data-title') || titleElement.textContent || '').trim();
    }

    function getMovieIdFromItem(itemElement) {
        return itemElement.getAttribute('data-movie-id') || '';
    }

    function getHistoryIdFromItem(itemElement) {
        return itemElement.getAttribute('data-history-id') || '';
    }

    function getMetaFromItem(itemElement) {
        var rawDateValue = itemElement.getAttribute('data-added-at') || itemElement.getAttribute('data-date') || '';
        return formatAddedDate(rawDateValue);
    }

    function getItemUniqueKey(itemElement) {
        var historyId = getHistoryIdFromItem(itemElement);
        if (historyId) return 'h_' + historyId;
        var movieId = getMovieIdFromItem(itemElement);
        if (movieId) return 'm_' + movieId;
        return '';
    }

    function getContainers() {
        var containersList = [];
        document.querySelectorAll('.history-container').forEach(function (containerElement) {
            containersList.push({ root: containerElement, itemSelector: '.history-item' });
        });
        document.querySelectorAll('.library-container').forEach(function (containerElement) {
            containersList.push({ root: containerElement, itemSelector: '.library-card' });
        });
        document.querySelectorAll('.favourites-container').forEach(function (containerElement) {
            containersList.push({ root: containerElement, itemSelector: '.favourites-card' });
        });
        document.querySelectorAll('.cart-container').forEach(function (containerElement) {
            containersList.push({ root: containerElement, itemSelector: '.cart-card' });
        });
        document.querySelectorAll('.movies-grid').forEach(function (containerElement) {
            containersList.push({ root: containerElement, itemSelector: '.movie-card' });
        });
        return containersList;
    }

    function collectAllItems() {
        var containersList = getContainers();
        var resultItems = [];
        containersList.forEach(function (containerConfig) {
            containerConfig.root.querySelectorAll(containerConfig.itemSelector).forEach(function (itemElement) {
                var itemTitle = getTitleFromItem(itemElement);
                if (!itemTitle) return;
                resultItems.push({
                    element: itemElement,
                    title: itemTitle,
                    normalized: normalize(itemTitle),
                    id: getMovieIdFromItem(itemElement),
                    historyId: getHistoryIdFromItem(itemElement),
                    uniqueKey: getItemUniqueKey(itemElement),
                    meta: getMetaFromItem(itemElement)
                });
            });
        });
        return resultItems;
    }

    function showAllItems() {
        getContainers().forEach(function (containerConfig) {
            containerConfig.root.querySelectorAll(containerConfig.itemSelector).forEach(function (itemElement) {
                itemElement.style.display = '';
            });
        });
        updateGroupVisibility();
        updateEmptyState(false);
    }

    function hideAllItems() {
        getContainers().forEach(function (containerConfig) {
            containerConfig.root.querySelectorAll(containerConfig.itemSelector).forEach(function (itemElement) {
                itemElement.style.display = 'none';
            });
        });
        updateGroupVisibility();
        updateEmptyState(true);
    }

    function applyFilterToItems(visibleItems) {
        var visibleSet = new Set(visibleItems.map(function (entry) { return entry.element; }));
        getContainers().forEach(function (containerConfig) {
            containerConfig.root.querySelectorAll(containerConfig.itemSelector).forEach(function (itemElement) {
                itemElement.style.display = visibleSet.has(itemElement) ? '' : 'none';
            });
        });
        updateGroupVisibility();
        updateEmptyState(visibleItems.length === 0);
    }

    function updateGroupVisibility() {
        document.querySelectorAll('.history-day-group').forEach(function (groupElement) {
            var anyVisible = Array.from(
                groupElement.querySelectorAll('.history-item')
            ).some(function (itemElement) {
                return itemElement.style.display !== 'none';
            });
            groupElement.style.display = anyVisible ? '' : 'none';
        });
    }

    function updateEmptyState(showEmpty) {
        var emptyElement = document.querySelector('[data-catalog-search-empty]');
        if (emptyElement) emptyElement.classList.toggle('show', !!showEmpty);
    }

    function applyFilterByText(query) {
        var rawQuery = String(query || '').trim();

        if (!rawQuery) { showAllItems(); return; }

        if (!hasMeaningfulChars(rawQuery)) {
            hideAllItems();
            return;
        }

        var normalizedQuery = normalize(rawQuery);
        if (!normalizedQuery) {
            hideAllItems();
            return;
        }

        var matchedItems = collectAllItems().filter(function (entry) {
            return entry.normalized.indexOf(normalizedQuery) === 0;
        });
        applyFilterToItems(matchedItems);
    }

    function applyFilterByUniqueKey(uniqueKey) {
        if (!uniqueKey) { showAllItems(); return; }
        var matchedItems = collectAllItems().filter(function (entry) {
            return entry.uniqueKey === uniqueKey;
        });
        applyFilterToItems(matchedItems);
    }

    function longestCommonPrefix(titles) {
        if (!titles || titles.length === 0) return '';
        if (titles.length === 1) return titles[0];

        var firstTitle = titles[0];
        var lastTitle = titles[0];
        for (var titleIndex = 1; titleIndex < titles.length; titleIndex++) {
            if (titles[titleIndex] < firstTitle) firstTitle = titles[titleIndex];
            if (titles[titleIndex] > lastTitle) lastTitle = titles[titleIndex];
        }

        var commonLength = 0;
        var maxLength = Math.min(firstTitle.length, lastTitle.length);
        while (commonLength < maxLength && firstTitle.charAt(commonLength) === lastTitle.charAt(commonLength)) {
            commonLength++;
        }
        return firstTitle.substring(0, commonLength);
    }

    var LAST_SEARCH_KEY = 'lastSearchQuery';
    var LAST_SEARCH_AT_KEY = 'lastSearchAt';
    var PENDING_KEY = 'pendingSearchRedirect';

    function saveLastSearch(query) {
        if (!query) return;
        try {
            sessionStorage.setItem(LAST_SEARCH_KEY, query);
            sessionStorage.setItem(LAST_SEARCH_AT_KEY, String(Date.now()));
        } catch (storageError) {}
    }

    function readLastSearch() {
        try {
            var storedValue = sessionStorage.getItem(LAST_SEARCH_KEY);
            if (!storedValue) return null;
            return storedValue;
        } catch (storageError) {
            return null;
        }
    }

    function clearLastSearch() {
        try {
            sessionStorage.removeItem(LAST_SEARCH_KEY);
            sessionStorage.removeItem(LAST_SEARCH_AT_KEY);
        } catch (storageError) {}
    }

    function markPending() {
        try { sessionStorage.setItem(PENDING_KEY, '1'); } catch (storageError) {}
    }

    function clearPending() {
        try { sessionStorage.removeItem(PENDING_KEY); } catch (storageError) {}
    }

    function isMovieDetailsPage() {
        return !!document.querySelector('.movie-details-container');
    }

    (function clearStaleSearchStateOnNonShowPages() {
        if (isMovieDetailsPage()) return;
        clearPending();
        clearLastSearch();
    })();

    var catalogSearchInputRef = null;

    function initCatalogSearch() {
        var catalogSearchInput = document.querySelector('[data-catalog-search-input]');
        var catalogSearchClearButton = document.querySelector('[data-catalog-search-clear]');
        var catalogSearchWrapper = document.querySelector('[data-catalog-search-wrapper]');
        var catalogSearchBox = document.querySelector('[data-catalog-search-suggestions]');
        var catalogSearchTypedElement = document.querySelector('[data-catalog-search-typed]');
        var catalogSearchHintElement = document.querySelector('[data-catalog-search-hint]');
        if (!catalogSearchInput || !catalogSearchWrapper) return;

        catalogSearchInputRef = catalogSearchInput;

        var ELLIPSIS = '…';
        var currentTopMatch = null;
        var lockedUniqueKey = null;
        var pointerDownInsideBox = false;

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
            catalogSearchWrapper.classList.toggle('has-value', catalogSearchInput.value.trim().length > 0);
        }

        function hideSuggestions() {
            if (catalogSearchBox) {
                catalogSearchBox.classList.remove('show');
                catalogSearchBox.textContent = '';
            }
        }

        function hideAutocomplete() {
            if (!catalogSearchTypedElement || !catalogSearchHintElement) return;
            currentTopMatch = null;
            catalogSearchWrapper.classList.remove('has-autocomplete');
            catalogSearchTypedElement.textContent = '';
            catalogSearchHintElement.textContent = '';
        }

        function normalizeForMatch(value) {
            return String(value || '')
                .toLocaleLowerCase('ru-RU')
                .replace(/ё/g, 'е');
        }

        function getMatches(value) {
            if (!hasMeaningfulChars(value)) return [];
            var normalizedValue = normalize(value);
            if (!normalizedValue) return [];
            return collectAllItems().filter(function (entry) {
                return entry.normalized.indexOf(normalizedValue) === 0;
            });
        }

        function renderAutocomplete(typedValue, titles) {
            if (!typedValue || !titles || titles.length !== 1 || !catalogSearchTypedElement || !catalogSearchHintElement) {
                hideAutocomplete();
                return;
            }

            var fullTitle = titles[0];
            var typedLower = normalizeForMatch(typedValue);
            var fullLower = normalizeForMatch(fullTitle);

            if (fullLower.indexOf(typedLower) !== 0) {
                hideAutocomplete();
                return;
            }

            var remainingPart = fullTitle.substring(typedValue.length);
            if (!remainingPart) {
                hideAutocomplete();
                return;
            }

            catalogSearchTypedElement.textContent = typedValue;
            catalogSearchHintElement.textContent = remainingPart;
            catalogSearchWrapper.classList.add('has-autocomplete');
        }

        function buildTruncatedQuery(cleanQuery) {
            var matches = getMatches(cleanQuery);
            if (matches.length === 0) return cleanQuery;
            if (matches.length === 1) return matches[0].title;

            var titles = matches.map(function (matchEntry) { return matchEntry.title; });
            var commonPrefix = longestCommonPrefix(titles);

            if (!commonPrefix || commonPrefix.length <= cleanQuery.length) {
                return cleanQuery + ELLIPSIS;
            }
            return commonPrefix + ELLIPSIS;
        }

        function applyEllipsisIfNeeded() {
            if (lockedUniqueKey) return;

            var rawValue = String(catalogSearchInput.value || '').trim();
            if (!rawValue) return;

            if (!hasMeaningfulChars(rawValue)) {
                return;
            }

            var cleanQuery = stripEllipsis(rawValue);
            var matches = getMatches(cleanQuery);
            if (matches.length === 1) {
                var singleTitle = matches[0].title;
                catalogSearchInput.value = singleTitle;
                catalogSearchInput.setSelectionRange(singleTitle.length, singleTitle.length);
            } else if (matches.length > 1) {
                catalogSearchInput.value = buildTruncatedQuery(cleanQuery);
            } else {
                catalogSearchInput.value = cleanQuery;
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

            var firstMatch = matches[0];
            catalogSearchInput.value = firstMatch.title;
            catalogSearchInput.setSelectionRange(firstMatch.title.length, firstMatch.title.length);

            hideAutocomplete();
            hideSuggestions();

            lockByUniqueKey(firstMatch.uniqueKey);
            applyFilterByUniqueKey(firstMatch.uniqueKey);
            return true;
        }

        function applySuggestion(entry) {
            if (!entry) return;

            catalogSearchInput.value = entry.title;
            catalogSearchInput.setSelectionRange(entry.title.length, entry.title.length);
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
                    catalogSearchInput.value = matches[0].title;
                } else {
                    catalogSearchInput.value = currentTopMatch;
                }
                catalogSearchInput.setSelectionRange(catalogSearchInput.value.length, catalogSearchInput.value.length);
                hideAutocomplete();
                return;
            }

            var rawValue = String(catalogSearchInput.value || '').trim();
            if (!rawValue) return;
            if (!hasMeaningfulChars(rawValue)) return;

            var normalizedValue = capitalizeFirst(rawValue);
            catalogSearchInput.value = normalizedValue;
            catalogSearchInput.setSelectionRange(normalizedValue.length, normalizedValue.length);
        }

        var activeSuggestionIndex = -1;

        function getSuggestionElements() {
            if (!catalogSearchBox) return [];
            return Array.prototype.slice.call(catalogSearchBox.querySelectorAll('.catalog-search-suggestion'));
        }

        function setActiveSuggestion(index) {
            var suggestionElements = getSuggestionElements();
            suggestionElements.forEach(function (element) { element.classList.remove('is-active'); });
            activeSuggestionIndex = index;
            if (index >= 0 && index < suggestionElements.length) {
                suggestionElements[index].classList.add('is-active');
                try {
                    suggestionElements[index].scrollIntoView({ block: 'nearest' });
                } catch (scrollError) {}
            }
        }

        function resetActiveSuggestion() {
            activeSuggestionIndex = -1;
            getSuggestionElements().forEach(function (element) { element.classList.remove('is-active'); });
        }

        function handleArrow(direction) {
            var suggestionElements = getSuggestionElements();
            if (!catalogSearchBox || suggestionElements.length === 0 || !catalogSearchBox.classList.contains('show')) {
                return false;
            }
            var nextIndex;
            if (direction === 'down') {
                if (activeSuggestionIndex < 0) {
                    nextIndex = 0;
                } else {
                    nextIndex = (activeSuggestionIndex + 1) % suggestionElements.length;
                }
            } else {
                if (activeSuggestionIndex < 0) {
                    nextIndex = suggestionElements.length - 1;
                } else {
                    nextIndex = (activeSuggestionIndex - 1 + suggestionElements.length) % suggestionElements.length;
                }
            }
            setActiveSuggestion(nextIndex);
            return true;
        }

        catalogSearchInput.addEventListener('input', function () {
            unlock();
            resetActiveSuggestion();

            if (isTruncated(catalogSearchInput.value)) {
                catalogSearchInput.value = stripEllipsis(catalogSearchInput.value);
            }
            updateClearVisibility();
            hideAutocomplete();

            var currentValue = catalogSearchInput.value;
            if (!currentValue) {
                hideSuggestions();
                showAllItems();
                return;
            }

            if (!hasMeaningfulChars(currentValue)) {
                hideSuggestions();
                hideAllItems();
                return;
            }

            var matches = getMatches(currentValue);

            if (matches.length === 0) {
                hideAllItems();
            }

            if (matches.length === 1) {
                currentTopMatch = matches[0].title;
                renderAutocomplete(currentValue, [matches[0].title]);
            }

            if (catalogSearchBox) {
                catalogSearchBox.textContent = '';
                if (matches.length === 0) {
                    catalogSearchBox.classList.remove('show');
                } else {
                    matches.slice(0, 8).forEach(function (entry) {
                        var suggestionElement = document.createElement('div');
                        suggestionElement.className = 'catalog-search-suggestion';
                        suggestionElement.setAttribute('data-suggestion-id', entry.id);
                        suggestionElement.setAttribute('data-suggestion-key', entry.uniqueKey);

                        var titleSpanElement = document.createElement('span');
                        titleSpanElement.className = 'catalog-search-suggestion-title';
                        titleSpanElement.textContent = entry.title;
                        suggestionElement.appendChild(titleSpanElement);

                        if (entry.meta) {
                            var metaSpanElement = document.createElement('span');
                            metaSpanElement.className = 'catalog-search-suggestion-meta';
                            metaSpanElement.textContent = entry.meta;
                            suggestionElement.appendChild(metaSpanElement);
                        }

                        suggestionElement.addEventListener('pointerdown', function (pointerEvent) {
                            pointerEvent.preventDefault();
                            pointerDownInsideBox = true;
                            applySuggestion(entry);
                            catalogSearchInput.blur();
                        });

                        suggestionElement.addEventListener('mouseenter', function () {
                            var hoveredIndex = getSuggestionElements().indexOf(suggestionElement);
                            if (hoveredIndex >= 0) setActiveSuggestion(hoveredIndex);
                        });

                        catalogSearchBox.appendChild(suggestionElement);
                    });
                    catalogSearchBox.classList.add('show');
                }
            }
        });

        catalogSearchInput.addEventListener('focus', function () {
            if (isTruncated(catalogSearchInput.value)) {
                catalogSearchInput.value = stripEllipsis(catalogSearchInput.value);
                updateClearVisibility();
            }
        });

        catalogSearchInput.addEventListener('keydown', function (keyboardEvent) {
            if (keyboardEvent.key === 'ArrowDown') {
                if (handleArrow('down')) {
                    keyboardEvent.preventDefault();
                    return;
                }
            }

            if (keyboardEvent.key === 'ArrowUp') {
                if (handleArrow('up')) {
                    keyboardEvent.preventDefault();
                    return;
                }
            }

            if (keyboardEvent.key === 'Enter') {
                keyboardEvent.preventDefault();

                if (activeSuggestionIndex >= 0) {
                    var activeElement = getSuggestionElements()[activeSuggestionIndex];
                    if (activeElement) {
                        var activeKey = activeElement.getAttribute('data-suggestion-key');
                        var activeTitleElement = activeElement.querySelector('.catalog-search-suggestion-title');
                        var activeText = activeTitleElement ? activeTitleElement.textContent.trim() : '';
                        if (activeText) {
                            catalogSearchInput.value = activeText;
                            catalogSearchInput.setSelectionRange(activeText.length, activeText.length);
                            updateClearVisibility();
                            hideSuggestions();
                            hideAutocomplete();
                            lockByUniqueKey(activeKey);
                            applyFilterByUniqueKey(activeKey);
                            resetActiveSuggestion();
                            return;
                        }
                    }
                }

                var rawValue = String(catalogSearchInput.value || '').trim();

                if (!hasMeaningfulChars(rawValue)) {
                    hideSuggestions();
                    hideAutocomplete();
                    hideAllItems();
                    return;
                }

                prepareForSubmit();
                var cleanValue = stripEllipsis(catalogSearchInput.value);
                applyFilterByText(cleanValue);
                applyEllipsisIfNeeded();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
                return;
            }

            if (keyboardEvent.key === 'Tab' || keyboardEvent.key === 'ArrowRight') {
                if (catalogSearchWrapper.classList.contains('has-autocomplete')) {
                    var caretAtEnd = catalogSearchInput.selectionStart === catalogSearchInput.value.length
                        && catalogSearchInput.selectionEnd === catalogSearchInput.value.length;
                    if (caretAtEnd) {
                        keyboardEvent.preventDefault();
                        acceptAutocomplete();
                        updateClearVisibility();
                        return;
                    }
                }
            }

            if (keyboardEvent.key === 'Escape') {
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
            }
        });

        catalogSearchInput.addEventListener('blur', function () {
            setTimeout(function () {
                if (pointerDownInsideBox) {
                    pointerDownInsideBox = false;
                    return;
                }
                if (!catalogSearchInput.value) {
                    hideSuggestions();
                    hideAutocomplete();
                    return;
                }
                if (!hasMeaningfulChars(catalogSearchInput.value)) {
                    hideSuggestions();
                    hideAutocomplete();
                    return;
                }
                applyEllipsisIfNeeded();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
                resetActiveSuggestion();
            }, 150);
        });

        if (catalogSearchClearButton) {
            catalogSearchClearButton.addEventListener('click', function () {
                catalogSearchInput.value = '';
                unlock();
                updateClearVisibility();
                hideSuggestions();
                hideAutocomplete();
                showAllItems();
                catalogSearchInput.focus();
            });
        }

        updateClearVisibility();

        window.reapplyCatalogSearchFilter = function () {
            if (!catalogSearchInputRef) return;
            if (lockedUniqueKey) {
                applyFilterByUniqueKey(lockedUniqueKey);
                return;
            }
            var currentValue = String(catalogSearchInputRef.value || '').trim();
            if (!currentValue) {
                showAllItems();
                return;
            }
            if (!hasMeaningfulChars(currentValue)) {
                hideAllItems();
                return;
            }
            applyFilterByText(stripEllipsis(currentValue));
        };
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

        var ELLIPSIS = '…';

        function getUrlTitle() {
            try {
                var urlParameters = new URLSearchParams(window.location.search);
                return urlParameters.get('title') || '';
            } catch (urlError) {
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
                .map(function (cardElement) {
                    var titleElement = cardElement.querySelector('.movie-card-title a, .media-card-title a');
                    return titleElement ? titleElement.textContent.trim() : '';
                })
                .filter(function (title) { return title.length > 0; });
        }

        function updateClearVisibility() {
            headerSearchWrapper.classList.toggle('has-value', headerSearchInput.value.trim().length > 0);
        }

        function buildTruncatedFromCards(sourceValue) {
            var cardTitles = getCardTitles();
            if (cardTitles.length <= 1) return sourceValue;

            var commonPrefix = longestCommonPrefix(cardTitles);
            if (!commonPrefix || commonPrefix.length <= sourceValue.length) {
                return sourceValue + ELLIPSIS;
            }
            return commonPrefix + ELLIPSIS;
        }

        function hideAutocomplete() {
            currentTopMatch = null;
            headerSearchWrapper.classList.remove('has-autocomplete');
            headerAutocompleteTypedElement.textContent = '';
            headerAutocompleteHintElement.textContent  = '';
        }

        function hideDropdown() {
            headerSuggestionsBox.style.display = 'none';
            headerSuggestionsBox.textContent = '';
        }

        function resetSearchState() {
            headerSearchInput.value = '';
            updateClearVisibility();
            hideDropdown();
            hideAutocomplete();
        }

        function restoreQuery() {
            var urlTitle = getUrlTitle();
            if (urlTitle) {
                headerSearchInput.value = buildTruncatedFromCards(urlTitle);
                return;
            }

            if (isMovieDetailsPage()) {
                var savedSearch = readLastSearch();
                if (savedSearch) {
                    headerSearchInput.value = savedSearch;
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

        function renderAutocomplete(typedValue, titles) {
            if (!typedValue || !titles || titles.length !== 1 || !headerAutocompleteTypedElement || !headerAutocompleteHintElement) {
                hideAutocomplete();
                return;
            }

            var fullTitle = titles[0];
            var typedLower = normalizeForMatch(typedValue);
            var fullLower = normalizeForMatch(fullTitle);

            if (fullLower.indexOf(typedLower) !== 0) {
                hideAutocomplete();
                return;
            }

            var remainingPart = fullTitle.substring(typedValue.length);
            if (!remainingPart) {
                hideAutocomplete();
                return;
            }

            headerAutocompleteTypedElement.textContent = typedValue;
            headerAutocompleteHintElement.textContent = remainingPart;
            headerSearchWrapper.classList.add('has-autocomplete');
        }

        function acceptAutocomplete() {
            if (!currentTopMatch) return false;
            headerSearchInput.value = currentTopMatch;
            headerSearchInput.setSelectionRange(headerSearchInput.value.length, headerSearchInput.value.length);
            hideAutocomplete();
            updateClearVisibility();
            return true;
        }

        function renderDropdownItems(itemsList) {
            headerSuggestionsBox.textContent = '';
            itemsList.forEach(function (item) {
                var rowElement = document.createElement('div');
                rowElement.className = 'suggestion-item';
                rowElement.setAttribute('data-id', item.id || '');

                var titleElement = document.createElement('span');
                titleElement.className = 'suggestion-title';
                titleElement.textContent = item.title || '';
                rowElement.appendChild(titleElement);

                rowElement.addEventListener('mouseenter', function () {
                    var hoveredIndex = getSuggestionElements().indexOf(rowElement);
                    if (hoveredIndex >= 0) setActiveSuggestion(hoveredIndex);
                });

                headerSuggestionsBox.appendChild(rowElement);
            });
        }

        function getSuggestionElements() {
            return Array.prototype.slice.call(headerSuggestionsBox.querySelectorAll('.suggestion-item'));
        }

        function setActiveSuggestion(index) {
            var suggestionElements = getSuggestionElements();
            suggestionElements.forEach(function (element) { element.classList.remove('is-active'); });
            activeSuggestionIndex = index;
            if (index >= 0 && index < suggestionElements.length) {
                suggestionElements[index].classList.add('is-active');
                try {
                    suggestionElements[index].scrollIntoView({ block: 'nearest' });
                } catch (scrollError) {}
            }
        }

        function resetActiveSuggestion() {
            activeSuggestionIndex = -1;
            getSuggestionElements().forEach(function (element) { element.classList.remove('is-active'); });
        }

        function handleArrow(direction) {
            var suggestionElements = getSuggestionElements();
            if (suggestionElements.length === 0) return false;
            if (headerSuggestionsBox.style.display !== 'flex') return false;
            var nextIndex;
            if (direction === 'down') {
                if (activeSuggestionIndex < 0) {
                    nextIndex = 0;
                } else {
                    nextIndex = (activeSuggestionIndex + 1) % suggestionElements.length;
                }
            } else {
                if (activeSuggestionIndex < 0) {
                    nextIndex = suggestionElements.length - 1;
                } else {
                    nextIndex = (activeSuggestionIndex - 1 + suggestionElements.length) % suggestionElements.length;
                }
            }
            setActiveSuggestion(nextIndex);
            return true;
        }

        function applyActiveSuggestion() {
            if (activeSuggestionIndex < 0) return false;
            var activeRowElement = getSuggestionElements()[activeSuggestionIndex];
            if (!activeRowElement) return false;

            var activeTitleElement = activeRowElement.querySelector('.suggestion-title');
            var activeText = activeTitleElement ? activeTitleElement.textContent.trim() : '';
            if (!activeText) return false;

            headerSearchInput.value = activeText;
            headerSearchInput.setSelectionRange(activeText.length, activeText.length);
            hideAutocomplete();
            hideDropdown();
            updateClearVisibility();
            resetActiveSuggestion();
            headerSearchInput.focus();

            if (typeof validateSearchQuery === 'function' && validateSearchQuery()) {
                commitAndSubmit();
            }
            return true;
        }

        function commitAndSubmit() {
            var rawValue = String(headerSearchInput.value || '').trim();
            if (!hasMeaningfulChars(rawValue)) {
                hideAutocomplete();
                hideDropdown();
                return;
            }

            var cleanValue = rawValue;
            if (isTruncated(cleanValue)) {
                cleanValue = cleanValue.substring(0, cleanValue.length - 1).trim();
            }

            if (currentTopMatch) {
                var typedLower = normalizeForMatch(cleanValue);
                var topLower = normalizeForMatch(currentTopMatch);
                if (topLower.indexOf(typedLower) === 0) {
                    cleanValue = currentTopMatch;
                }
            } else {
                cleanValue = capitalizeFirst(cleanValue);
            }

            headerSearchInput.value = cleanValue;
            headerSearchInput.setSelectionRange(cleanValue.length, cleanValue.length);
            updateClearVisibility();
            hideDropdown();

            var truncatedValue = buildTruncatedFromCards(cleanValue);

            if (truncatedValue && truncatedValue.length > cleanValue.length) {
                headerAutocompleteTypedElement.textContent = cleanValue;
                headerAutocompleteHintElement.textContent = truncatedValue.substring(cleanValue.length);
                headerSearchWrapper.classList.add('has-autocomplete');
            } else {
                hideAutocomplete();
            }

            saveLastSearch(cleanValue);
            markPending();

            window.setTimeout(function () {
                headerSearchForm.submit();
            }, 120);
        }

        function applySuggestion(rowElement) {
            if (!rowElement) return;
            var titleElement = rowElement.querySelector('.suggestion-title');
            var suggestionText = titleElement ? titleElement.textContent.trim() : '';
            if (!suggestionText) return;

            headerSearchInput.value = suggestionText;
            headerSearchInput.setSelectionRange(suggestionText.length, suggestionText.length);
            hideAutocomplete();
            hideDropdown();
            updateClearVisibility();
            resetActiveSuggestion();
            headerSearchInput.focus();

            if (typeof validateSearchQuery === 'function' && validateSearchQuery()) {
                commitAndSubmit();
            }
        }

        headerSuggestionsBox.addEventListener('pointerdown', function (pointerEvent) {
            var rowElement = pointerEvent.target.closest('.suggestion-item');
            if (!rowElement) return;
            pointerEvent.preventDefault();
            pointerEvent.stopPropagation();
            pointerDownInsideBox = true;
            applySuggestion(rowElement);
        }, true);

        function fetchSuggestions(query) {
            if (!query || !hasMeaningfulChars(query)) {
                hideDropdown();
                hideAutocomplete();
                resetActiveSuggestion();
                pendingMoveToList = false;
                return;
            }

            fetch('/movies/suggestions?query=' + encodeURIComponent(query))
                .then(function (response) { return response.json(); })
                .then(function (suggestionsList) {
                    if (!suggestionsList || suggestionsList.length === 0) {
                        hideDropdown();
                        hideAutocomplete();
                        resetActiveSuggestion();
                        pendingMoveToList = false;
                        return;
                    }
                    renderDropdownItems(suggestionsList);
                    headerSuggestionsBox.style.display = 'flex';
                    resetActiveSuggestion();

                    var typedValue = headerSearchInput.value;
                    var normalizedTyped = normalizeForMatch(typedValue);
                    var titles = suggestionsList
                        .map(function (item) { return item && item.title ? item.title : ''; })
                        .filter(function (title) { return title.length > 0; });

                    var matchingTitles = titles.filter(function (title) {
                        return normalizeForMatch(title).indexOf(normalizedTyped) === 0;
                    });

                    if (matchingTitles.length === 1) {
                        currentTopMatch = matchingTitles[0];
                        renderAutocomplete(typedValue, [matchingTitles[0]]);
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

        headerSearchInput.addEventListener('beforeinput', function (beforeInputEvent) {
            if (!currentTopMatch) return;
            if (beforeInputEvent.inputType !== 'insertText' || !beforeInputEvent.data) return;
            var typedLength = headerSearchInput.value.length;
            var expectedCharacter = currentTopMatch.charAt(typedLength);
            if (beforeInputEvent.data.toLowerCase() !== expectedCharacter.toLowerCase()) {
                hideAutocomplete();
            }
        });

        headerSearchInput.addEventListener('input', function () {
            var currentValue = headerSearchInput.value;

            if (isTruncated(currentValue)) {
                currentValue = currentValue.substring(0, currentValue.length - 1);
                headerSearchInput.value = currentValue;
            }

            hideAutocomplete();
            updateClearVisibility();
            resetActiveSuggestion();
            pendingMoveToList = false;

            if (currentValue === '') {
                hideDropdown();
                return;
            }

            if (!hasMeaningfulChars(currentValue)) {
                hideDropdown();
                return;
            }

            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(function () {
                fetchSuggestions(currentValue.trim());
            }, 300);
        });

        headerSearchInput.addEventListener('keydown', function (keyboardEvent) {
            if (keyboardEvent.key === 'ArrowDown') {
                if (handleArrow('down')) {
                    keyboardEvent.preventDefault();
                    return;
                }
                if (hasMeaningfulChars(headerSearchInput.value)) {
                    keyboardEvent.preventDefault();
                    pendingMoveToList = true;
                    clearTimeout(debounceTimer);
                    fetchSuggestions(headerSearchInput.value.trim());
                    return;
                }
            }

            if (keyboardEvent.key === 'ArrowUp') {
                if (handleArrow('up')) {
                    keyboardEvent.preventDefault();
                    return;
                }
            }

            if (currentTopMatch && (keyboardEvent.key === 'ArrowRight' || keyboardEvent.key === 'Tab')) {
                var caretAtEnd = headerSearchInput.selectionStart === headerSearchInput.value.length
                    && headerSearchInput.selectionEnd === headerSearchInput.value.length;
                if (caretAtEnd) {
                    keyboardEvent.preventDefault();
                    acceptAutocomplete();
                    return;
                }
            }
            if (keyboardEvent.key === 'Escape') {
                hideAutocomplete();
                hideDropdown();
                resetActiveSuggestion();
                pendingMoveToList = false;
                return;
            }
            if (keyboardEvent.key === 'Enter') {
                keyboardEvent.preventDefault();
                if (activeSuggestionIndex >= 0) {
                    if (applyActiveSuggestion()) return;
                }
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                commitAndSubmit();
            }
        });

        headerSearchInput.addEventListener('blur', function () {
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
                commitAndSubmit();
            });
        }

        if (!headerSearchForm.dataset.bound) {
            headerSearchForm.dataset.bound = '1';
            headerSearchForm.addEventListener('submit', function (submitEvent) {
                submitEvent.preventDefault();
                if (typeof validateSearchQuery === 'function' && !validateSearchQuery()) return;
                commitAndSubmit();
            });
        }

        if (headerSearchClearButton && !headerSearchClearButton.dataset.bound) {
            headerSearchClearButton.dataset.bound = '1';
            headerSearchClearButton.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                headerSearchInput.value = '';
                clearPending();
                hideAutocomplete();
                hideDropdown();
                resetActiveSuggestion();
                pendingMoveToList = false;
                updateClearVisibility();
                headerSearchInput.focus();
            });
        }

        window.addEventListener('pageshow', function (pageShowEvent) {
            if (pageShowEvent.persisted) restoreQuery();
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