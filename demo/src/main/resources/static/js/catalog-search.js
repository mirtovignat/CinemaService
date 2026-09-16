(function () {
    'use strict';

    function normalize(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .trim()
            .replace(/ё/g, 'е')
            .replace(/[^a-zа-я0-9]/gi, '');
    }

    function getTitleElement(item) {
        const selectors = [
            '.history-item-title',
            '.library-card-title a',
            '.library-card-title',
            '.favourites-card-title a',
            '.favourites-card-title',
            '.cart-card-title a',
            '.cart-card-title',
            '.movie-card-title a',
            '.movie-card-title'
        ];

        for (let i = 0; i < selectors.length; i++) {
            const el = item.querySelector(selectors[i]);
            if (el) return el;
        }

        return null;
    }

    function getTitleFromItem(item) {
        const el = getTitleElement(item);
        if (!el) return '';
        return (el.getAttribute('data-title') || el.textContent || '').trim();
    }

    function getMovieIdFromItem(item) {
        return item.getAttribute('data-movie-id') || '';
    }

    function getContainers() {
        const containers = [];

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
        const containers = getContainers();
        const result = [];

        containers.forEach(function (config) {
            const items = config.root.querySelectorAll(config.itemSelector);

            items.forEach(function (item) {
                const title = getTitleFromItem(item);

                if (!title) return;

                result.push({
                    element: item,
                    title: title,
                    normalized: normalize(title),
                    id: getMovieIdFromItem(item)
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

    function applyFilterToItems(visibleItems) {
        const visibleSet = new Set(visibleItems.map(function (x) { return x.element; }));

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
            const anyVisible = Array.from(
                group.querySelectorAll('.history-item')
            ).some(function (item) {
                return item.style.display !== 'none';
            });

            group.style.display = anyVisible ? '' : 'none';
        });
    }

    function updateEmptyState(showEmpty) {
        const emptyEl = document.querySelector('[data-catalog-search-empty]');
        if (emptyEl) {
            emptyEl.classList.toggle('show', !!showEmpty);
        }
    }

    function applyFilterByText(query) {
        const normalized = normalize(query);

        if (!normalized) {
            showAllItems();
            return;
        }

        const matched = collectAllItems().filter(function (entry) {
            return entry.normalized.indexOf(normalized) === 0;
        });

        applyFilterToItems(matched);
    }

    function applyFilterById(id) {
        if (!id) {
            showAllItems();
            return;
        }

        const matched = collectAllItems().filter(function (entry) {
            return entry.id === id;
        });

        applyFilterToItems(matched);
    }

    function renderSuggestions(items, onPick) {
        const box = document.querySelector('[data-catalog-search-suggestions]');

        if (!box) return;

        if (items.length === 0) {
            box.innerHTML = '';
            box.classList.remove('show');
            return;
        }

        const shown = items.slice(0, 8);

        box.innerHTML = shown.map(function (item) {
            return '<div class="catalog-search-suggestion" ' +
                   'data-suggestion-id="' + item.id + '">' +
                   '<span class="catalog-search-suggestion-title">' +
                   item.title.replace(/</g, '&lt;').replace(/>/g, '&gt;') +
                   '</span></div>';
        }).join('');

        box.classList.add('show');

        box.querySelectorAll('.catalog-search-suggestion').forEach(function (el) {
            el.addEventListener('mousedown', function (e) {
                e.preventDefault();
                onPick(el.getAttribute('data-suggestion-id'));
            });
        });
    }

    function initCatalogSearch() {
        const input = document.querySelector('[data-catalog-search-input]');
        const clearBtn = document.querySelector('[data-catalog-search-clear]');
        const wrapper = document.querySelector('[data-catalog-search-wrapper]');
        const box = document.querySelector('[data-catalog-search-suggestions]');

        if (!input || !wrapper) return;

        function updateClearVisibility() {
            wrapper.classList.toggle('has-value', input.value.trim().length > 0);
        }

        function hideSuggestions() {
            if (box) {
                box.classList.remove('show');
                box.innerHTML = '';
            }
        }

        input.addEventListener('input', function () {
            updateClearVisibility();

            const value = input.value.trim();

            if (!value) {
                hideSuggestions();
                return;
            }

            const normalized = normalize(value);

            const matches = collectAllItems().filter(function (entry) {
                return entry.normalized.indexOf(normalized) === 0;
            });

            renderSuggestions(matches, function (id) {
                applyFilterById(id);
                hideSuggestions();
                input.blur();
            });
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                hideSuggestions();
                applyFilterByText(input.value);
            } else if (e.key === 'Escape') {
                hideSuggestions();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(hideSuggestions, 150);
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', function () {
                input.value = '';
                updateClearVisibility();
                hideSuggestions();
                showAllItems();
                input.focus();
            });
        }

        updateClearVisibility();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCatalogSearch, { once: true });
    } else {
        initCatalogSearch();
    }
})();