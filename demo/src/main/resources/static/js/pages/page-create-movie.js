(function () {
    'use strict';

    var MAX_RESULTS = 100;
    var STORAGE_KEY = 'createMovieState';

    var searchResults = [];
    var selectedTmdbIds = new Set();
    var prices = {};
    var lastError = null;

    var root = document.querySelector('.create-movie-wrapper');
    var messages = {
        emptyInput: root ? root.dataset.msgEmptyInput : '',
        failed: root ? root.dataset.msgSearchFailed : '',
        noSelection: root ? root.dataset.msgNoSelection : '',
        priceRequired: root ? root.dataset.msgPriceRequired : '',
        fillAllConfirm: root ? root.dataset.msgFillAllConfirm : '',
        fillAllEmptyPrice: root ? root.dataset.msgFillAllEmptyPrice : '',
        fillAllNothing: root ? root.dataset.msgFillAllNothing : '',
        limitReached: root ? root.dataset.msgLimitReached : '',
        confirmYes: root ? root.dataset.msgConfirmYes : 'Да',
        confirmNo: root ? root.dataset.msgConfirmNo : 'Отмена'
    };

    var tmdbIdInput = document.getElementById('tmdbIdInput');
    var titleInput = document.getElementById('titleInput');
    var directorInput = document.getElementById('directorInput');
    var actorInput = document.getElementById('actorInput');
    var searchBtn = document.getElementById('searchBtn');
    var clearBtn = document.getElementById('clearBtn');
    var resultsContainer = document.getElementById('resultsContainer');
    var createBtn = document.getElementById('createBtn');
    var errorMessage = document.getElementById('errorMessage');
    var selectAllBtn = document.getElementById('selectAllBtn');
    var bulkPriceInput = document.getElementById('bulkPriceInput');
    var fillAllBtn = document.getElementById('fillAllBtn');
    var selectionSummary = document.getElementById('selectionSummary');
    var emptyState = document.getElementById('emptyState');
    var loadingState = document.getElementById('loadingState');

    var confirmModal = document.getElementById('confirmModal');
    var confirmModalText = document.getElementById('confirmModalText');
    var confirmModalOk = document.getElementById('confirmModalOk');
    var confirmModalCancel = document.getElementById('confirmModalCancel');
    var confirmResolver = null;

    confirmModalOk.textContent = messages.confirmYes;
    confirmModalCancel.textContent = messages.confirmNo;

    function saveState() {
        try {
            var state = {
                searchResults: searchResults,
                selectedTmdbIds: Array.from(selectedTmdbIds),
                prices: prices,
                lastError: lastError
            };
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        } catch (e) {}
    }

    function restoreState() {
        try {
            var raw = sessionStorage.getItem(STORAGE_KEY);
            if (!raw) return false;
            var state = JSON.parse(raw);
            if (!state) return false;

            searchResults = state.searchResults || [];
            selectedTmdbIds = new Set(state.selectedTmdbIds || []);
            prices = state.prices || {};
            lastError = state.lastError || null;

            if (lastError) {
                if (lastError.code === 'TMDB_MOVIE_ALREADY_EXISTS') {
                    renderAlreadyExists(lastError.data);
                } else {
                    showError(lastError.message);
                }
            }

            if (searchResults.length > 0) {
                renderResults();
                return true;
            }
        } catch (e) {}
        return false;
    }

    function clearSavedState() {
        try {
            sessionStorage.removeItem(STORAGE_KEY);
        } catch (e) {}
    }

    function showLoading() {
        if (emptyState) emptyState.style.display = 'none';
        if (loadingState) loadingState.style.display = 'flex';
    }

    function hideLoading() {
        if (loadingState) loadingState.style.display = 'none';
    }

    function showEmptyState() {
        if (emptyState) emptyState.style.display = 'flex';
    }

    function askConfirm(message) {
        confirmModalText.textContent = message;
        confirmModal.hidden = false;
        return new Promise(function (resolve) {
            confirmResolver = resolve;
        });
    }

    function closeConfirm(value) {
        confirmModal.hidden = true;
        if (confirmResolver) {
            var resolver = confirmResolver;
            confirmResolver = null;
            resolver(value);
        }
    }

    confirmModalOk.addEventListener('click', function () { closeConfirm(true); });
    confirmModalCancel.addEventListener('click', function () { closeConfirm(false); });
    confirmModal.addEventListener('click', function (event) {
        if (event.target === confirmModal) closeConfirm(false);
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !confirmModal.hidden) closeConfirm(false);
    });

    function showError(message) {
        errorMessage.innerHTML = '';
        var textDiv = document.createElement('div');
        textDiv.className = 'error-text';
        textDiv.textContent = message;
        errorMessage.appendChild(textDiv);
        errorMessage.classList.add('show');
    }

    function clearError() {
        errorMessage.innerHTML = '';
        errorMessage.textContent = '';
        errorMessage.classList.remove('show');
    }

    function parsePrice(value) {
        var price = parseFloat(value);
        return !isNaN(price) && price > 0 ? price : null;
    }

    function splitLines(raw) {
        if (!raw) return [];
        return raw.split(/[\n,;]+/)
            .map(function (part) { return part.trim(); })
            .filter(function (part) { return part.length > 0; });
    }

    function parseTmdbIds(raw) {
        return splitLines(raw)
            .map(function (part) { return Number(part); })
            .filter(function (value) { return !isNaN(value) && value > 0; });
    }

    function allSelectedHavePrice() {
        if (selectedTmdbIds.size === 0) return false;
        var ok = true;
        selectedTmdbIds.forEach(function (tmdbId) {
            if (parsePrice(prices[tmdbId]) === null) ok = false;
        });
        return ok;
    }

    function updateSelectionSummary() {
        if (!selectionSummary) return;
        selectionSummary.textContent = 'Выбрано: ' + selectedTmdbIds.size + ' / ' + MAX_RESULTS;
    }

    function updateCreateButton() {
        createBtn.disabled = !allSelectedHavePrice();
        createBtn.textContent = 'Создать выбранные (' + selectedTmdbIds.size + ')';
        updateSelectionSummary();
    }

    function updateSelectAllButton() {
        if (!selectAllBtn) return;
        if (searchResults.length === 0) {
            selectAllBtn.disabled = true;
            selectAllBtn.textContent = 'Выбрать все';
            return;
        }
        selectAllBtn.disabled = false;
        var allSelected = selectedTmdbIds.size === searchResults.length;
        selectAllBtn.textContent = allSelected ? 'Снять выделение' : 'Выбрать все';
    }

    function updateFillAllButton() {
        if (!fillAllBtn) return;
        var hasBulkPrice = parsePrice(bulkPriceInput ? bulkPriceInput.value : null) !== null;
        fillAllBtn.disabled = selectedTmdbIds.size === 0 || !hasBulkPrice;
    }

    function buildPosterArea(movie, card) {
        if (movie.posterUrl) {
            var image = document.createElement('img');
            image.src = movie.posterUrl;
            image.alt = movie.title || '';
            image.addEventListener('error', function () {
                image.style.display = 'none';
                var placeholder = document.createElement('div');
                placeholder.className = 'poster-placeholder';
                placeholder.textContent = 'Нет постера';
                card.insertBefore(placeholder, image);
            }, { once: true });
            card.appendChild(image);
        } else {
            var placeholder = document.createElement('div');
            placeholder.className = 'poster-placeholder';
            placeholder.textContent = 'Нет постера';
            card.appendChild(placeholder);
        }
    }

    function buildPriceArea(movie, card) {
        var priceWrapper = document.createElement('div');
        priceWrapper.className = 'price-wrapper';
        priceWrapper.addEventListener('click', function (event) { event.stopPropagation(); });

        var priceLabel = document.createElement('label');
        priceLabel.className = 'price-label';
        priceLabel.textContent = 'Цена (₽):';
        priceLabel.setAttribute('for', 'priceInput-' + movie.tmdbId);
        priceWrapper.appendChild(priceLabel);

        var priceInput = document.createElement('input');
        priceInput.type = 'number';
        priceInput.className = 'price-input';
        priceInput.id = 'priceInput-' + movie.tmdbId;
        priceInput.step = '0.01';
        priceInput.min = '0.01';
        priceInput.placeholder = 'Обязательно';
        if (prices[movie.tmdbId] != null) {
            priceInput.value = prices[movie.tmdbId];
        }
        priceInput.addEventListener('input', function () {
            var raw = priceInput.value.trim();
            if (raw === '') delete prices[movie.tmdbId];
            else prices[movie.tmdbId] = raw;
            updateCreateButton();
            saveState();
        });
        priceWrapper.appendChild(priceInput);

        card.appendChild(priceWrapper);
    }

    function renderResultCard(movie) {
        var card = document.createElement('div');
        card.className = 'result-card' + (selectedTmdbIds.has(movie.tmdbId) ? ' selected' : '');
        card.dataset.tmdbId = movie.tmdbId;

        var checkboxIndicator = document.createElement('div');
        checkboxIndicator.className = 'checkbox-indicator';
        checkboxIndicator.textContent = '✓';
        card.appendChild(checkboxIndicator);

        buildPosterArea(movie, card);

        var titleElement = document.createElement('div');
        titleElement.className = 'title';
        titleElement.textContent = movie.title || 'Без названия';
        card.appendChild(titleElement);

        var yearElement = document.createElement('div');
        yearElement.className = 'year';
        yearElement.textContent = movie.releaseDate ? String(movie.releaseDate).substring(0, 4) : '';
        card.appendChild(yearElement);

        buildPriceArea(movie, card);

        card.addEventListener('click', function () {
            if (selectedTmdbIds.has(movie.tmdbId)) {
                selectedTmdbIds.delete(movie.tmdbId);
                card.classList.remove('selected');
            } else {
                if (selectedTmdbIds.size >= MAX_RESULTS) {
                    showError(messages.limitReached.replace('{0}', MAX_RESULTS));
                    return;
                }
                selectedTmdbIds.add(movie.tmdbId);
                card.classList.add('selected');
            }
            updateCreateButton();
            updateSelectAllButton();
            updateFillAllButton();
            saveState();
        });

        return card;
    }

    function renderResults() {
        resultsContainer.innerHTML = '';
        searchResults.forEach(function (movie) {
            resultsContainer.appendChild(renderResultCard(movie));
        });
        updateCreateButton();
        updateSelectAllButton();
        updateFillAllButton();
    }

    function buildSearchUrl() {
        var tmdbIds = parseTmdbIds(tmdbIdInput.value);
        var titles = splitLines(titleInput.value);
        var directors = splitLines(directorInput.value);
        var actors = splitLines(actorInput.value);

        if (tmdbIds.length === 0 && titles.length === 0 && directors.length === 0 && actors.length === 0) {
            return null;
        }

        var params = new URLSearchParams();
        tmdbIds.forEach(function (tmdbId) { params.append('tmdbIds', tmdbId); });
        titles.forEach(function (title) { params.append('titles', title); });
        directors.forEach(function (director) { params.append('directors', director); });
        actors.forEach(function (actor) { params.append('actors', actor); });

        return '/movies/tmdb/search?' + params.toString();
    }

    function readError(response) {
        return response.json()
            .then(function (body) {
                return {
                    code: body ? body.code : null,
                    message: body && body.message ? body.message : messages.failed,
                    data: body ? body.data : null
                };
            })
            .catch(function () {
                return { code: null, message: messages.failed, data: null };
            });
    }

    function renderAlreadyExists(list) {
        errorMessage.innerHTML = '';
        if (!list || list.length === 0) {
            showError(messages.failed);
            return;
        }

        var intro = document.createElement('div');
        intro.className = 'error-title';
        intro.textContent = list.length === 1 ? 'Этот фильм уже есть на сайте:' : 'Эти фильмы уже есть на сайте:';
        errorMessage.appendChild(intro);

        var listContainer = document.createElement('div');
        listContainer.className = 'tmdb-error-list';

        var ul = document.createElement('ul');
        ul.className = 'already-exists-list';

        list.forEach(function (movieRefer) {
            var li = document.createElement('li');
            var link = document.createElement('a');
            link.href = '/movies/' + movieRefer.id;
            link.textContent = movieRefer.title || '';
            if (movieRefer.year != null) {
                var yearSpan = document.createElement('span');
                yearSpan.className = 'year-muted';
                yearSpan.textContent = ' (' + movieRefer.year + ')';
                link.appendChild(yearSpan);
            }
            li.appendChild(link);
            ul.appendChild(li);
        });

        listContainer.appendChild(ul);
        errorMessage.appendChild(listContainer);
        errorMessage.classList.add('show');
    }

    function handleSearchClick() {
        clearError();
        lastError = null;
        var url = buildSearchUrl();
        if (!url) {
            showError(messages.emptyInput);
            return;
        }

        searchBtn.disabled = true;
        searchBtn.textContent = 'Ищем...';
        showLoading();

        fetch(url, { headers: { 'Accept': 'application/json' } })
            .then(function (response) {
                if (response.ok) {
                    return response.json().then(function (data) {
                        return { ok: true, data: data };
                    });
                }
                return readError(response).then(function (error) {
                    return { ok: false, error: error };
                });
            })
            .then(function (result) {
                hideLoading();

                if (!result.ok) {
                    searchResults = [];
                    selectedTmdbIds.clear();
                    prices = {};
                    resultsContainer.innerHTML = '';
                    updateCreateButton();
                    updateSelectAllButton();
                    updateFillAllButton();

                    lastError = result.error;
                    if (result.error.code === 'TMDB_MOVIE_ALREADY_EXISTS') {
                        renderAlreadyExists(result.error.data);
                    } else {
                        showError(result.error.message);
                    }
                    saveState();
                    return;
                }

                searchResults = result.data || [];
                selectedTmdbIds.clear();
                prices = {};

                if (searchResults.length === 0) {
                    resultsContainer.innerHTML = '';
                    updateCreateButton();
                    updateSelectAllButton();
                    updateFillAllButton();
                    showError(messages.failed);
                    saveState();
                    return;
                }

                renderResults();
                saveState();
            })
            .catch(function () {
                hideLoading();
                showError(messages.failed);
            })
            .finally(function () {
                searchBtn.disabled = false;
                searchBtn.textContent = 'Найти';
            });
    }

    function handleSelectAllClick() {
        if (searchResults.length === 0) return;
        var allSelected = selectedTmdbIds.size === searchResults.length;
        if (allSelected) {
            selectedTmdbIds.clear();
        } else {
            searchResults.forEach(function (movie) {
                if (selectedTmdbIds.size >= MAX_RESULTS) return;
                selectedTmdbIds.add(movie.tmdbId);
            });
            if (searchResults.length > MAX_RESULTS) {
                showError(messages.limitReached.replace('{0}', MAX_RESULTS));
            }
        }
        var cards = resultsContainer.querySelectorAll('.result-card');
        cards.forEach(function (card) {
            if (selectedTmdbIds.has(Number(card.dataset.tmdbId))) {
                card.classList.add('selected');
            } else {
                card.classList.remove('selected');
            }
        });
        updateCreateButton();
        updateSelectAllButton();
        updateFillAllButton();
        saveState();
    }

    function handleFillAllClick() {
        clearError();

        var bulkPrice = parsePrice(bulkPriceInput.value);
        if (bulkPrice === null) {
            showError(messages.fillAllEmptyPrice);
            return;
        }
        if (selectedTmdbIds.size === 0) {
            showError(messages.noSelection);
            return;
        }

        var targets = [];
        selectedTmdbIds.forEach(function (tmdbId) {
            if (parsePrice(prices[tmdbId]) === null) targets.push(tmdbId);
        });

        if (targets.length === 0) {
            showError(messages.fillAllNothing);
            return;
        }

        askConfirm(messages.fillAllConfirm).then(function (confirmed) {
            if (!confirmed) return;
            targets.forEach(function (tmdbId) {
                prices[tmdbId] = String(bulkPrice);
                var priceInput = document.getElementById('priceInput-' + tmdbId);
                if (priceInput) {
                    priceInput.value = String(bulkPrice);
                    priceInput.classList.remove('error');
                }
            });
            updateCreateButton();
            saveState();
        });
    }

    function handleClearClick() {
        clearError();
        searchResults = [];
        selectedTmdbIds.clear();
        prices = {};
        lastError = null;
        resultsContainer.innerHTML = '';
        tmdbIdInput.value = '';
        titleInput.value = '';
        directorInput.value = '';
        actorInput.value = '';
        updateCreateButton();
        updateSelectAllButton();
        updateFillAllButton();
        clearSavedState();

        var form = document.getElementById('createMovieSearchForm');
        if (form && window.FormPersist) {
            window.FormPersist.clear(form);
        }

        showEmptyState();
    }

    function buildCreateForm() {
        var form = document.createElement('form');
        form.method = 'POST';
        form.action = '/movies/tmdb/create';

        var csrfMeta = document.querySelector('meta[name="_csrf"]');
        if (csrfMeta) {
            var csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = csrfMeta.content;
            form.appendChild(csrfInput);
        }

        var index = 0;
        selectedTmdbIds.forEach(function (tmdbId) {
            var idInput = document.createElement('input');
            idInput.type = 'hidden';
            idInput.name = 'moviePriceItems[' + index + '].tmdbId';
            idInput.value = tmdbId;
            form.appendChild(idInput);

            var priceInput = document.createElement('input');
            priceInput.type = 'hidden';
            priceInput.name = 'moviePriceItems[' + index + '].price';
            priceInput.value = parsePrice(prices[tmdbId]);
            form.appendChild(priceInput);

            index++;
        });

        return form;
    }

    function handleCreateClick() {
        if (selectedTmdbIds.size === 0) {
            showError(messages.noSelection);
            return;
        }

        var hasInvalid = false;
        selectedTmdbIds.forEach(function (tmdbId) {
            if (parsePrice(prices[tmdbId]) === null) {
                hasInvalid = true;
                var priceInput = document.getElementById('priceInput-' + tmdbId);
                if (priceInput) priceInput.classList.add('error');
            }
        });
        if (hasInvalid) {
            showError(messages.priceRequired);
            return;
        }

        var form = buildCreateForm();
        document.body.appendChild(form);
        form.submit();

        clearSavedState();
        var searchForm = document.getElementById('createMovieSearchForm');
        if (searchForm && window.FormPersist) {
            window.FormPersist.clear(searchForm);
        }
    }

    searchBtn.addEventListener('click', handleSearchClick);
    if (clearBtn) clearBtn.addEventListener('click', handleClearClick);
    if (selectAllBtn) selectAllBtn.addEventListener('click', handleSelectAllClick);
    if (fillAllBtn) fillAllBtn.addEventListener('click', handleFillAllClick);
    if (bulkPriceInput) bulkPriceInput.addEventListener('input', updateFillAllButton);
    createBtn.addEventListener('click', handleCreateClick);

    restoreState();

    updateCreateButton();
    updateSelectAllButton();
    updateFillAllButton();

})();