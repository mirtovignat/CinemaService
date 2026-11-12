(function () {
    'use strict';

    var DEBOUNCE_MS = 250;
    var MAX_RESULTS = 10;

    function normalize(value) {
        return String(value || '').toLocaleLowerCase('ru-RU').trim().replace(/ё/g, 'е');
    }

    function hasMeaningfulChars(value) {
        return /[a-zа-я0-9ё]/i.test(String(value || ''));
    }

    function getAccordionName(accordion) {
        return accordion.getAttribute('data-accordion') || '';
    }

    function readSelectedFromUrl(name) {
        try {
            var params = new URLSearchParams(window.location.search);
            var out = [];
            params.getAll(name).forEach(function (v) {
                if (v.indexOf(',') !== -1) {
                    out = out.concat(v.split(','));
                } else {
                    out.push(v);
                }
            });
            return out.map(function (s) { return s.trim(); }).filter(Boolean);
        } catch (e) {
            return [];
        }
    }

    function findExactTitle(titles, value) {
        if (!titles || titles.length === 0) return null;
        var normalizedValue = normalize(value);
        if (!normalizedValue) return null;
        for (var i = 0; i < titles.length; i++) {
            if (normalize(titles[i]) === normalizedValue) return titles[i];
        }
        return null;
    }

    function bindOne(accordion) {
        var dropdown = accordion.querySelector('[data-accordion-dropdown]');
        if (!dropdown) return;

        var wrapper = dropdown.querySelector('[data-accordion-search-wrapper]');
        var input = dropdown.querySelector('[data-accordion-search-input]');
        var clear = dropdown.querySelector('[data-accordion-search-clear]');
        var list = dropdown.querySelector('[data-accordion-list]');
        var selectedBox = dropdown.querySelector('[data-accordion-selected]');
        var empty = dropdown.querySelector('[data-accordion-empty]');
        var triggerLabel = accordion.querySelector('.accordion-trigger > span:first-child');

        if (!wrapper || !input || !list) return;

        // Защита от повторного биндинга
        if (input.dataset.filtersPrefixBound === '1') return;
        input.dataset.filtersPrefixBound = '1';

        var name = getAccordionName(accordion);
        if (!name) return;

        var baseLabel = triggerLabel ? triggerLabel.textContent.trim() : name;
        var selected = new Set(readSelectedFromUrl(name));
        var lastResults = [];
        var lastFetchedTitles = null;
        var debounceTimer = null;
        var fetchToken = 0;

        list.innerHTML = '';
        if (empty) empty.classList.remove('show');

        function syncClearBtn() {
            wrapper.classList.toggle('has-value', input.value.trim().length > 0);
        }

        function updateTriggerLabel() {
            if (!triggerLabel) return;
            triggerLabel.textContent = selected.size > 0
                ? baseLabel + ' (' + selected.size + ')'
                : baseLabel;
        }

        function renderChips() {
            if (!selectedBox) return;
            selectedBox.innerHTML = '';
            selected.forEach(function (value) {
                var hidden = document.createElement('input');
                hidden.type = 'checkbox';
                hidden.name = name;
                hidden.value = value;
                hidden.checked = true;
                hidden.style.display = 'none';
                selectedBox.appendChild(hidden);

                var chip = document.createElement('span');
                chip.className = 'accordion-chip';
                chip.textContent = value;

                var rm = document.createElement('button');
                rm.type = 'button';
                rm.className = 'accordion-chip-remove';
                rm.setAttribute('aria-label', 'Убрать');
                rm.innerHTML = '&times;';
                rm.addEventListener('click', function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    selected.delete(value);
                    renderChips();
                    renderResults();
                    updateTriggerLabel();
                });
                chip.appendChild(rm);
                selectedBox.appendChild(chip);
            });
        }

        function renderResults() {
            list.innerHTML = '';
            var shown = 0;

            if (lastResults.length === 0) {
                if (empty) {
                    var hasQuery = hasMeaningfulChars(input.value);
                    empty.classList.toggle('show', hasQuery);
                }
                return;
            } else {
                if (empty) empty.classList.remove('show');
            }

            lastResults.forEach(function (value) {
                if (selected.has(value)) return;
                if (shown >= MAX_RESULTS) return;
                shown++;

                var item = document.createElement('div');
                item.className = 'accordion-item--selectable';
                item.textContent = value;

                // Предотвращаем потерю фокуса при клике
                item.addEventListener('pointerdown', function (e) {
                    e.preventDefault();
                });

                item.addEventListener('click', function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    selected.add(value);
                    renderChips();
                    updateTriggerLabel();
                    input.value = '';
                    syncClearBtn();
                    renderResults();
                });
                list.appendChild(item);
            });
        }

        function search(query, token) {
            if (!query) {
                lastResults = [];
                renderResults();
                return;
            }
            var url = '/filters/' + encodeURIComponent(name) + '/search?searchQuery=' + encodeURIComponent(query);

            fetch(url, {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                credentials: 'same-origin',
                cache: 'no-store'
            })
            .then(function (r) {
                if (!r.ok) throw new Error('Network response was not ok');
                return r.json();
            })
            .then(function (data) {
                if (token !== fetchToken) return;
                var items = (data && data.items) ? data.items : [];
                lastFetchedTitles = items.map(function (v) { return String(v); });
                lastResults = lastFetchedTitles.slice();
                renderResults();
            })
            .catch(function () {
                if (token !== fetchToken) return;
                lastResults = [];
                renderResults();
            });
        }

        input.addEventListener('input', function () {
            syncClearBtn();
            clearTimeout(debounceTimer);

            var value = input.value;

            if (!hasMeaningfulChars(value)) {
                lastResults = [];
                renderResults();
                return;
            }

            var token = ++fetchToken;
            debounceTimer = setTimeout(function () { search(value, token); }, DEBOUNCE_MS);
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                var raw = input.value.trim();
                if (!raw) return;
                if (!hasMeaningfulChars(raw)) return;

                var exact = findExactTitle(lastFetchedTitles, raw);
                if (exact && !selected.has(exact)) {
                    selected.add(exact);
                    renderChips();
                    updateTriggerLabel();
                    input.value = '';
                    syncClearBtn();
                    renderResults();
                }
                return;
            }

            if (e.key === 'Escape') {
                input.value = '';
                syncClearBtn();
                lastResults = [];
                renderResults();
                input.blur();
            }
        });

        if (clear) {
            clear.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                syncClearBtn();
                lastResults = [];
                renderResults();
                input.focus();
            });
        }

        wrapper.addEventListener('click', function (e) {
            e.stopPropagation();
        });

        accordion.__filtersSearchReset = function () {
            selected.clear();
            lastResults = [];
            lastFetchedTitles = null;
            renderChips();
            renderResults();
            updateTriggerLabel();
            input.value = '';
            syncClearBtn();
        };

        renderChips();
        renderResults();
        updateTriggerLabel();
        syncClearBtn();
    }

    function init(root) {
        var scope = root || document;
        var groups;

        if (scope.matches && scope.matches('[data-accordion]')) {
            groups = [scope];
        } else if (scope.querySelectorAll) {
            groups = Array.prototype.slice.call(scope.querySelectorAll('[data-accordion]'));
        } else {
            groups = [];
        }

        groups.forEach(bindOne);
    }

    function resetAll() {
        document.querySelectorAll('[data-accordion]').forEach(function (accordion) {
            if (typeof accordion.__filtersSearchReset === 'function') {
                accordion.__filtersSearchReset();
            }
        });
    }

    window.FiltersSearch = {
        init: init,
        reset: resetAll
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { init(); }, { once: true });
    } else {
        init();
    }

    window.addEventListener('load', function () { init(); });
})();