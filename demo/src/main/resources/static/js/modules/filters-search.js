(function () {
    'use strict';

    var ELLIPSIS = '…';

    function normalize(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .trim()
            .replace(/ё/g, 'е');
    }

    function normalizeTight(value) {
        return normalize(value).replace(/[^a-zа-я0-9]/gi, '');
    }

    function hasMeaningfulChars(value) {
        return /[a-zа-я0-9ё]/i.test(String(value || ''));
    }

    function isTruncated(value) {
        return typeof value === 'string'
            && value.length > 0
            && value.charAt(value.length - 1) === ELLIPSIS;
    }

    function stripEllipsis(value) {
        return isTruncated(value) ? value.substring(0, value.length - 1) : value;
    }

    function capitalizeFirst(value) {
        if (!value) return '';
        if (value.length === 1) return value.toLocaleUpperCase('ru-RU');
        return value.charAt(0).toLocaleUpperCase('ru-RU') + value.slice(1).toLocaleLowerCase('ru-RU');
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
        while (len < max && first.charAt(len) === last.charAt(len)) len++;
        return first.substring(0, len);
    }

    function getSearchName(accordion) {
        var dropdown = accordion.querySelector('[data-accordion-dropdown]');
        if (!dropdown) return '';
        var input = dropdown.querySelector('[data-accordion-search-input]');
        if (!input) return '';
        return input.getAttribute('data-accordion-search-name') || '';
    }

    function isDirectorAccordion(accordion) {
        return getSearchName(accordion) === 'directors'
            || accordion.getAttribute('data-accordion') === 'directors';
    }

    function getItems(accordion) {
        var list = accordion.querySelector('[data-accordion-list]');
        if (!list) return [];
        return Array.prototype.slice.call(
            list.querySelectorAll('.accordion-item')
        );
    }

    function getItemDisplay(item) {
        var span = item.querySelector('span:last-child');
        var raw = span ? span.textContent : item.textContent;
        return (raw || '').trim();
    }

    function getItemCheckbox(item) {
        return item.querySelector('input[type="checkbox"]');
    }

    function getItemValue(item) {
        var cb = getItemCheckbox(item);
        return cb ? cb.value : '';
    }

    function getDirectorWords(display) {
        return String(display || '')
            .split(/\s+/)
            .map(function (part) { return part.trim(); })
            .filter(function (part) { return part.length > 0; });
    }

    function getSearchWords(accordion, item) {
        var display = getItemDisplay(item);
        if (isDirectorAccordion(accordion)) {
            return getDirectorWords(display);
        }
        return display ? [display] : [];
    }

    function getItemSearchKey(accordion, item) {
        var display = getItemDisplay(item);
        if (isDirectorAccordion(accordion)) {
            var idx = display.indexOf(' ');
            if (idx === -1) return display;
            return display.substring(0, idx).trim();
        }
        return display;
    }

    function getItemSortKey(accordion, item) {
        return getItemSearchKey(accordion, item);
    }

    function queryWords(query) {
        var q = normalize(query);
        if (!q) return [];
        return q
            .split(/\s+/)
            .map(function (part) { return normalizeTight(part); })
            .filter(function (part) { return part.length > 0; });
    }

    function itemMatchesQuery(accordion, item, normalizedQuery) {
        var words = queryWords(normalizedQuery);
        if (words.length === 0) return true;

        var itemWords = getSearchWords(accordion, item)
            .map(normalizeTight)
            .filter(function (w) { return w.length > 0; });

        if (itemWords.length === 0) return false;

        for (var i = 0; i < words.length; i++) {
            var qw = words[i];
            var matched = false;
            for (var j = 0; j < itemWords.length; j++) {
                if (itemWords[j].indexOf(qw) === 0) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    function applyFilter(accordion, query) {
        var items = getItems(accordion);
        var empty = accordion.querySelector('[data-accordion-empty]');
        var visible = 0;

        items.forEach(function (item) {
            var match = itemMatchesQuery(accordion, item, query);
            item.classList.toggle('is-hidden', !match);
            if (match) visible++;
        });

        if (empty) {
            var hasMeaningful = hasMeaningfulChars(query);
            empty.classList.toggle('show', visible === 0 && hasMeaningful);
        }
    }

    function hideAutocomplete(ui) {
        if (!ui.typedEl || !ui.hintEl) return;
        ui.typedEl.textContent = '';
        ui.hintEl.textContent = '';
        ui.wrapper.classList.remove('has-autocomplete');
    }

    function renderAutocomplete(ui, accordion, typed) {
        if (!typed) {
            hideAutocomplete(ui);
            return;
        }
        var typedWords = queryWords(typed);
        if (typedWords.length === 0) {
            hideAutocomplete(ui);
            return;
        }

        var items = getItems(accordion);
        var matches = items.filter(function (item) {
            return itemMatchesQuery(accordion, item, typed);
        });

        if (matches.length !== 1) {
            hideAutocomplete(ui);
            return;
        }

        var display = getItemDisplay(matches[0]);
        var displayWords = getDirectorWords(display);

        var used = new Array(displayWords.length).fill(false);
        for (var i = 0; i < typedWords.length; i++) {
            var qw = typedWords[i];
            var found = false;
            for (var j = 0; j < displayWords.length; j++) {
                if (used[j]) continue;
                var dw = normalizeTight(displayWords[j]);
                if (dw.indexOf(qw) === 0) {
                    used[j] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                hideAutocomplete(ui);
                return;
            }
        }

        var rest = '';
        for (var k = 0; k < displayWords.length; k++) {
            if (!used[k]) {
                rest += (rest ? ' ' : '') + displayWords[k];
            }
        }

        if (!rest) {
            hideAutocomplete(ui);
            return;
        }

        ui.typedEl.textContent = typed;
        ui.hintEl.textContent = ' ' + rest;
        ui.wrapper.classList.add('has-autocomplete');
    }

    function updateClear(ui) {
        var v = ui.input.value;
        ui.wrapper.classList.toggle('has-value', v.trim().length > 0);
    }

    function buildTruncatedQuery(accordion, clean) {
        var cleanWords = queryWords(clean);
        if (cleanWords.length === 0) return clean;

        var items = getItems(accordion);
        var matched = [];
        items.forEach(function (item) {
            if (itemMatchesQuery(accordion, item, clean)) {
                var display = getItemDisplay(item);
                if (display) matched.push(display);
            }
        });

        if (matched.length === 0) return clean;
        if (matched.length === 1) return matched[0];

        var prefix = longestCommonPrefix(matched);
        if (!prefix || prefix.length <= clean.length) return clean + ELLIPSIS;
        return prefix + ELLIPSIS;
    }

    function selectItem(checkbox) {
        if (!checkbox) return;
        checkbox.checked = !checkbox.checked;
        checkbox.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function bindOne(accordion) {
        var dropdown = accordion.querySelector('[data-accordion-dropdown]');
        if (!dropdown) return;

        if (dropdown.querySelector('.infinite-scroll-sentinel')) {
            return;
        }

        var wrapper = dropdown.querySelector('[data-accordion-search-wrapper]');
        var input = dropdown.querySelector('[data-accordion-search-input]');
        var clear = dropdown.querySelector('[data-accordion-search-clear]');
        var typedEl = dropdown.querySelector('[data-accordion-search-typed]');
        var hintEl = dropdown.querySelector('[data-accordion-search-hint]');

        if (!wrapper || !input) return;
        if (input.dataset.accordionSearchBound === '1') return;
        input.dataset.accordionSearchBound = '1';

        var ui = {
            wrapper: wrapper,
            input: input,
            typedEl: typedEl,
            hintEl: hintEl
        };

        function matchesForQuery(value) {
            var q = normalizeTight(value);
            if (!q) return [];
            return getItems(accordion).filter(function (item) {
                return itemMatchesQuery(accordion, item, value);
            });
        }

        function bestMatch(value) {
            var matches = matchesForQuery(value);
            if (matches.length === 0) return null;
            var best = matches[0];
            var bestDisplay = getItemDisplay(best);
            matches.forEach(function (item) {
                var display = getItemDisplay(item);
                if (display.length < bestDisplay.length) {
                    best = item;
                    bestDisplay = display;
                }
            });
            return best;
        }

        function acceptAutocomplete() {
            if (!wrapper.classList.contains('has-autocomplete')) return false;
            var typedText = (typedEl ? typedEl.textContent : '').trim();
            var hintText = (hintEl ? hintEl.textContent : '').trim();
            var full = (typedText + ' ' + hintText).trim();
            if (!full) return false;
            input.value = full;
            input.setSelectionRange(full.length, full.length);
            hideAutocomplete(ui);
            updateClear(ui);
            applyFilter(accordion, full);
            return true;
        }

        input.addEventListener('input', function () {
            var val = input.value;
            if (isTruncated(val)) {
                val = stripEllipsis(val);
                input.value = val;
            }

            hideAutocomplete(ui);
            updateClear(ui);

            if (val === '') {
                applyFilter(accordion, '');
                return;
            }

            if (!hasMeaningfulChars(val)) {
                applyFilter(accordion, val);
                return;
            }

            applyFilter(accordion, val);
            renderAutocomplete(ui, accordion, val);
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();

                if (wrapper.classList.contains('has-autocomplete')) {
                    acceptAutocomplete();
                    return;
                }

                var raw = input.value.trim();
                if (!raw) return;
                if (!hasMeaningfulChars(raw)) {
                    applyFilter(accordion, '');
                    return;
                }

                var clean = stripEllipsis(raw);
                var normalizedInput = capitalizeFirst(clean);
                input.value = normalizedInput;

                var matches = matchesForQuery(normalizedInput);

                if (matches.length === 1) {
                    var best = bestMatch(normalizedInput);
                    if (best) {
                        selectItem(getItemCheckbox(best));
                        input.value = '';
                        applyFilter(accordion, '');
                        updateClear(ui);
                        hideAutocomplete(ui);
                        return;
                    }
                }

                if (matches.length > 1) {
                    var truncated = buildTruncatedQuery(accordion, normalizedInput);
                    input.value = truncated;
                    input.setSelectionRange(truncated.length, truncated.length);
                    applyFilter(accordion, normalizedInput);
                } else {
                    input.value = normalizedInput + ELLIPSIS;
                    applyFilter(accordion, normalizedInput);
                }

                hideAutocomplete(ui);
                updateClear(ui);
                return;
            }

            if ((e.key === 'ArrowRight' || e.key === 'Tab') &&
                wrapper.classList.contains('has-autocomplete')) {
                var atEnd = input.selectionStart === input.value.length
                    && input.selectionEnd === input.value.length;
                if (atEnd) {
                    e.preventDefault();
                    acceptAutocomplete();
                    return;
                }
            }

            if (e.key === 'Escape') {
                hideAutocomplete(ui);
                applyFilter(accordion, '');
                input.value = '';
                updateClear(ui);
                input.blur();
            }
        });

        input.addEventListener('blur', function () {
            setTimeout(function () {
                if (!input.value) return;
                if (!hasMeaningfulChars(input.value)) return;
                var clean = stripEllipsis(input.value.trim());
                var normalizedInput = capitalizeFirst(clean);
                var matches = matchesForQuery(normalizedInput);

                if (matches.length === 1) {
                    input.value = getItemDisplay(matches[0]) + ELLIPSIS;
                } else if (matches.length > 1) {
                    input.value = buildTruncatedQuery(accordion, normalizedInput);
                } else {
                    input.value = normalizedInput + ELLIPSIS;
                }

                updateClear(ui);
                hideAutocomplete(ui);
            }, 120);
        });

        if (clear) {
            clear.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                hideAutocomplete(ui);
                updateClear(ui);
                applyFilter(accordion, '');
                input.focus();
            });
        }

        wrapper.addEventListener('click', function (e) {
            e.stopPropagation();
        });

        applyFilter(accordion, '');
        updateClear(ui);
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

    window.FiltersSearch = {
        init: init,
        normalize: normalize
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { init(); }, { once: true });
    } else {
        init();
    }

    window.addEventListener('load', function () { init(); });
})();