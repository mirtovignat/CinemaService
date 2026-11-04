(function () {
    'use strict';

    var FILTER_LIMITS = {
        priceMin: 0.01,
        priceMax: 100000,
        durationMin: 1,
        durationMax: 16 * 3600 + 59 * 60 + 59,
        releaseDateMin: null,
        releaseDateMax: null
    };

    var AJAX_HEADERS = {
        'Accept': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    };

    function formatPriceValue(value) {
        var numberValue = Number(value);
        if (isNaN(numberValue)) return '';
        return numberValue.toFixed(2);
    }

    function splitDuration(totalSecondsValue) {
        var safeTotalSeconds = Math.max(0, Math.floor(Number(totalSecondsValue) || 0));
        return {
            h: Math.floor(safeTotalSeconds / 3600),
            m: Math.floor((safeTotalSeconds % 3600) / 60),
            s: safeTotalSeconds % 60
        };
    }

    function formatDurationHuman(totalSecondsValue) {
        var parts = splitDuration(totalSecondsValue);
        return parts.h + ' ч. ' + parts.m + ' мин. ' + parts.s + ' сек.';
    }

    function applyPriceLimitsToUi() {
        var priceFromInput = document.getElementById('priceFrom');
        var priceToInput = document.getElementById('priceTo');
        if (priceFromInput) {
            priceFromInput.setAttribute('placeholder', 'Мин. на сайте: ' + formatPriceValue(FILTER_LIMITS.priceMin) + ' ₽');
        }
        if (priceToInput) {
            priceToInput.setAttribute('placeholder', 'Макс. на сайте: ' + formatPriceValue(FILTER_LIMITS.priceMax) + ' ₽');
        }
    }

    function applyDurationLimitsToUi() {
        var maxDuration = splitDuration(FILTER_LIMITS.durationMax);
        var maxValuesById = {
            durationFromHours: maxDuration.h,
            durationFromMinutes: 59,
            durationFromSeconds: 59,
            durationToHours: maxDuration.h,
            durationToMinutes: 59,
            durationToSeconds: 59
        };
        Object.keys(maxValuesById).forEach(function (elementId) {
            var element = document.getElementById(elementId);
            if (element) element.setAttribute('data-max', String(maxValuesById[elementId]));
        });
    }

    function applyReleaseDateLimitsToUi() {
        var releaseDateFromInput = document.getElementById('releaseDateFrom');
        var releaseDateToInput = document.getElementById('releaseDateTo');
        if (releaseDateFromInput) {
            if (FILTER_LIMITS.releaseDateMin) releaseDateFromInput.setAttribute('min', FILTER_LIMITS.releaseDateMin);
            if (FILTER_LIMITS.releaseDateMax) releaseDateFromInput.setAttribute('max', FILTER_LIMITS.releaseDateMax);
        }
        if (releaseDateToInput) {
            if (FILTER_LIMITS.releaseDateMin) releaseDateToInput.setAttribute('min', FILTER_LIMITS.releaseDateMin);
            if (FILTER_LIMITS.releaseDateMax) releaseDateToInput.setAttribute('max', FILTER_LIMITS.releaseDateMax);
        }
    }

    function validateSearchQuery() {
        var headerSearchInput = document.getElementById('headerSearchInput');
        if (!headerSearchInput) return true;
        var trimmedQuery = headerSearchInput.value.trim();
        if (trimmedQuery.length > 100) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Слишком длинный запрос (максимум 100 символов)', 'warning');
            }
            headerSearchInput.focus();
            return false;
        }
        return true;
    }

    function closeAllAccordions() {
        document.querySelectorAll('.accordion-group.active').forEach(function (el) {
            el.classList.remove('active');
        });
        document.querySelectorAll('.accordion-dropdown.active').forEach(function (el) {
            el.classList.remove('active');
        });
    }

    function toggleFiltersModal(shouldShow) {
        var overlay = document.getElementById('filtersModalOverlay');
        if (!overlay) return;

        if (typeof shouldShow === 'boolean') {
            if (shouldShow) {
                overlay.classList.add('show');
                document.body.style.overflow = 'hidden';
            } else {
                overlay.classList.remove('show');
                document.body.style.overflow = '';
                closeAllAccordions();
            }
        } else {
            overlay.classList.toggle('show');
            if (overlay.classList.contains('show')) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = '';
                closeAllAccordions();
            }
        }
    }

    function initFilterAccordions() {
        document.querySelectorAll('.accordion-trigger').forEach(function (trigger) {
            if (trigger.dataset.accordionBound === '1') return;
            trigger.dataset.accordionBound = '1';
            trigger.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                var targetId = trigger.getAttribute('data-target');
                if (!targetId) return;
                var dropdown = document.getElementById(targetId);
                if (!dropdown) return;
                var group = trigger.closest('.accordion-group');
                if (!group) return;
                var willOpen = !group.classList.contains('active');
                group.classList.toggle('active', willOpen);
                dropdown.classList.toggle('active', willOpen);
            });
        });
    }

    function initFilterClearButtons() {
        document.querySelectorAll('.filter-clear-btn').forEach(function (clearBtn) {
            if (clearBtn.dataset.clearBound === '1') return;
            clearBtn.dataset.clearBound = '1';
            var targetId = clearBtn.getAttribute('data-clear-target');
            if (!targetId) return;
            var targetInput = document.getElementById(targetId);
            if (!targetInput) return;
            var wrapper = clearBtn.closest('.filter-input-with-clear');

            function syncHasValueClass() {
                if (!wrapper) return;
                wrapper.classList.toggle('has-value', targetInput.value !== '');
            }

            clearBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                targetInput.value = '';
                targetInput.dispatchEvent(new Event('input', { bubbles: true }));
                targetInput.dispatchEvent(new Event('change', { bubbles: true }));
                syncHasValueClass();
                targetInput.focus();
            });

            targetInput.addEventListener('input', syncHasValueClass);
            targetInput.addEventListener('change', syncHasValueClass);
            syncHasValueClass();
        });
    }

    function extractSurname(fullNameValue) {
        var trimmed = String(fullNameValue || '').trim();
        if (!trimmed) return '';
        var idx = trimmed.indexOf(' ');
        if (idx === -1) return trimmed;
        var rest = trimmed.substring(idx + 1).trim();
        return rest || trimmed;
    }

    function normalizeForSort(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .replace(/ё/g, 'е');
    }

    function displayDirectorName(fullNameValue) {
        var trimmed = String(fullNameValue || '').trim();
        if (!trimmed) return '';
        var idx = trimmed.indexOf(' ');
        if (idx === -1) return trimmed;
        var firstPart = trimmed.substring(0, idx).trim();
        var rest = trimmed.substring(idx + 1).trim();
        if (!rest) return trimmed;
        return rest + ' ' + firstPart;
    }

    function sortDirectorsBySurname(list) {
        return list.slice().sort(function (a, b) {
            var sa = normalizeForSort(extractSurname(a));
            var sb = normalizeForSort(extractSurname(b));
            if (sa !== sb) return sa.localeCompare(sb, 'ru-RU');
            return normalizeForSort(a).localeCompare(normalizeForSort(b), 'ru-RU');
        });
    }

    function sortGenresAlphabetically(list) {
        return list.slice().sort(function (a, b) {
            return normalizeForSort(a).localeCompare(normalizeForSort(b), 'ru-RU');
        });
    }

    function buildAccordionCheckbox(checkboxName, checkboxValue, displayText) {
        var label = document.createElement('label');
        label.className = 'accordion-item';
        var input = document.createElement('input');
        input.type = 'checkbox';
        input.name = checkboxName;
        input.className = 'accordion-checkbox';
        input.value = checkboxValue;
        var span = document.createElement('span');
        span.textContent = displayText != null ? displayText : checkboxValue;
        label.appendChild(input);
        label.appendChild(span);
        return label;
    }

    function appendCheckboxesToAccordion(dropdown, checkboxName, items, displayFn) {
        var list = dropdown.querySelector('[data-accordion-list]');
        if (!list) return;

        var existing = new Set();
        list.querySelectorAll('input[name="' + checkboxName + '"]').forEach(function (chk) {
            existing.add(chk.value);
        });

        (items || []).forEach(function (itemValue) {
            if (existing.has(itemValue)) return;
            var displayValue = displayFn ? displayFn(itemValue) : itemValue;
            list.appendChild(buildAccordionCheckbox(checkboxName, itemValue, displayValue));
            existing.add(itemValue);
        });

        var selectedAttr = dropdown.getAttribute('data-selected');
        if (selectedAttr && selectedAttr.trim() !== '') {
            var selectedValues = selectedAttr.split(',').map(function (s) { return s.trim(); });
            list.querySelectorAll('input[name="' + checkboxName + '"]').forEach(function (chk) {
                if (selectedValues.indexOf(chk.value) !== -1) chk.checked = true;
            });
        }
    }

    function setupInfiniteList(dropdownId, checkboxName, endpoint, displayFn) {
        var dropdown = document.getElementById(dropdownId);
        if (!dropdown) return;

        var list = dropdown.querySelector('[data-accordion-list]');
        var emptyEl = dropdown.querySelector('[data-accordion-empty]');
        if (!list) return;
        if (list.querySelector('.infinite-scroll-sentinel')) return;

        var isLoading = false;
        var hasMore = true;
        var nextCursor = null;
        var currentQuery = '';

        var sentinel = document.createElement('div');
        sentinel.style.height = '20px';
        sentinel.style.width = '100%';
        sentinel.style.pointerEvents = 'none';
        sentinel.className = 'infinite-scroll-sentinel';
        list.after(sentinel);

        var observer = new IntersectionObserver(function (entries) {
            if (entries[0].isIntersecting) loadData();
        }, { root: dropdown, rootMargin: '50px' });
        observer.observe(sentinel);

        function loadData() {
            if (isLoading || !hasMore) return;
            isLoading = true;
            sentinel.textContent = 'Загрузка...';
            sentinel.style.color = '#a78bfa';
            sentinel.style.fontSize = '0.8rem';
            sentinel.style.textAlign = 'center';

            var url = endpoint + '?query=' + encodeURIComponent(currentQuery);
            if (nextCursor) url += '&cursor=' + encodeURIComponent(nextCursor);

            fetch(url, { headers: AJAX_HEADERS })
                .then(function (r) { return r.ok ? r.json() : { items: [], hasMore: false }; })
                .then(function (data) {
                    var items = data.items || [];
                    hasMore = !!data.hasMore;
                    nextCursor = data.nextCursor || null;

                    var sorted = checkboxName === 'directors'
                        ? sortDirectorsBySurname(items)
                        : sortGenresAlphabetically(items);

                    appendCheckboxesToAccordion(dropdown, checkboxName, sorted, displayFn);
                    sentinel.textContent = '';

                    if (!hasMore) observer.unobserve(sentinel);

                    if (emptyEl) {
                        var hasItems = list.querySelectorAll('.accordion-item').length > 0;
                        emptyEl.classList.toggle('show', !hasItems && currentQuery.trim() !== '');
                    }
                })
                .catch(function () {
                    sentinel.textContent = 'Ошибка загрузки';
                })
                .finally(function () {
                    isLoading = false;
                });
        }

        var searchInput = dropdown.querySelector('[data-accordion-search-input]');
        if (searchInput) {
            var debounceTimer = null;
            searchInput.addEventListener('input', function () {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(function () {
                    currentQuery = searchInput.value;
                    nextCursor = null;
                    hasMore = true;
                    list.innerHTML = '';
                    list.after(sentinel);
                    observer.observe(sentinel);
                    loadData();
                }, 300);
            });
        }

        loadData();
    }

    function loadFilterMeta() {
        return fetch('/filters/meta', { headers: AJAX_HEADERS })
            .then(function (response) { return response.ok ? response.json() : null; })
            .then(function (meta) {
                if (!meta) return;

                if (meta.movieRangesSnapshot) {
                    var ranges = meta.movieRangesSnapshot;
                    if (ranges.price) {
                        if (ranges.price.min != null) FILTER_LIMITS.priceMin = Number(ranges.price.min);
                        if (ranges.price.max != null) FILTER_LIMITS.priceMax = Number(ranges.price.max);
                    }
                    if (ranges.duration) {
                        if (ranges.duration.min != null) FILTER_LIMITS.durationMin = Number(ranges.duration.min);
                        if (ranges.duration.max != null) FILTER_LIMITS.durationMax = Number(ranges.duration.max);
                    }
                    if (ranges.releaseDate) {
                        if (ranges.releaseDate.min != null) FILTER_LIMITS.releaseDateMin = String(ranges.releaseDate.min);
                        if (ranges.releaseDate.max != null) FILTER_LIMITS.releaseDateMax = String(ranges.releaseDate.max);
                    }
                }

                applyPriceLimitsToUi();
                applyDurationLimitsToUi();
                applyReleaseDateLimitsToUi();
            })
            .catch(function () {});
    }

    function calculateRangeMinutes() {
        function readInt(id) {
            var el = document.getElementById(id);
            return el ? (parseInt(el.value, 10) || 0) : 0;
        }

        var fh = readInt('durationFromHours');
        var fm = readInt('durationFromMinutes');
        var fs = readInt('durationFromSeconds');
        var th = readInt('durationToHours');
        var tm = readInt('durationToMinutes');
        var ts = readInt('durationToSeconds');

        var fromSeconds = fh * 3600 + fm * 60 + fs;
        var toSeconds = th * 3600 + tm * 60 + ts;

        var minEl = document.getElementById('minDuration');
        var maxEl = document.getElementById('maxDuration');
        if (minEl) minEl.value = fromSeconds > 0 ? fromSeconds : '';
        if (maxEl) maxEl.value = toSeconds > 0 ? toSeconds : '';
    }

    function clearFilterFieldErrors() {
        document.querySelectorAll('.filter-group.has-error').forEach(function (el) {
            el.classList.remove('has-error');
        });
        document.querySelectorAll('.filter-field-error').forEach(function (el) {
            el.remove();
        });
        document.querySelectorAll('.filter-input, .accordion-trigger').forEach(function (el) {
            el.style.borderColor = '';
            el.style.boxShadow = '';
        });
        document.querySelectorAll('.filter-input-with-clear.has-error, .amount-input-with-stepper.has-error').forEach(function (el) {
            el.classList.remove('has-error');
        });
        var ratingError = document.getElementById('ratingFilterError');
        if (ratingError) {
            ratingError.style.display = 'none';
            ratingError.innerText = '';
        }
    }

    function showFilterFieldError(inputElement, errorMessage) {
        if (!inputElement) return;

        var wrapper = inputElement.closest('.amount-input-with-stepper, .filter-input-with-clear')
                      || inputElement.parentNode;
        if (!wrapper) return;

        var group = inputElement.closest('.filter-group');
        if (group) group.classList.add('has-error');

        var inputId = inputElement.id || ('auto-' + Math.random().toString(36).slice(2));
        var errorEl = wrapper.querySelector('.filter-field-error[data-error-for="' + inputId + '"]');
        if (!errorEl) {
            errorEl = document.createElement('div');
            errorEl.className = 'filter-field-error';
            errorEl.setAttribute('data-error-for', inputId);
            wrapper.appendChild(errorEl);
        }

        errorEl.textContent = errorMessage;
        errorEl.title = errorMessage;
        errorEl.style.display = '';

        if (wrapper.classList) wrapper.classList.add('has-error');
    }

    function validateDateRange(fromValue, toValue) {
        if (!fromValue || !toValue) return { ok: true };
        if (fromValue > toValue) {
            return { ok: false, field: 'to', error: 'Дата «до» не может быть раньше даты «от»' };
        }
        return { ok: true };
    }

    function validateFilter() {
        clearFilterFieldErrors();
        calculateRangeMinutes();

        var hasError = false;

        var priceFromInput = document.getElementById('priceFrom');
        var priceToInput = document.getElementById('priceTo');
        var ratingFromInput = document.getElementById('ratingFrom');
        var ratingToInput = document.getElementById('ratingTo');
        var releaseDateFromInput = document.getElementById('releaseDateFrom');
        var releaseDateToInput = document.getElementById('releaseDateTo');

        var priceErrorText =
            'Минимальная цена — ' + formatPriceValue(FILTER_LIMITS.priceMin) +
            ' ₽, максимальная — ' + formatPriceValue(FILTER_LIMITS.priceMax) + ' ₽';

        var parsedPriceFrom = null;
        var parsedPriceTo = null;

        if (priceFromInput && priceFromInput.value.trim() !== '') {
            parsedPriceFrom = parseFloat(priceFromInput.value.replace(',', '.'));
            if (isNaN(parsedPriceFrom) || parsedPriceFrom < FILTER_LIMITS.priceMin || parsedPriceFrom > FILTER_LIMITS.priceMax) {
                showFilterFieldError(priceFromInput, priceErrorText);
                hasError = true;
            } else {
                priceFromInput.value = parsedPriceFrom.toFixed(2);
            }
        }

        if (priceToInput && priceToInput.value.trim() !== '') {
            parsedPriceTo = parseFloat(priceToInput.value.replace(',', '.'));
            if (isNaN(parsedPriceTo) || parsedPriceTo < FILTER_LIMITS.priceMin || parsedPriceTo > FILTER_LIMITS.priceMax) {
                showFilterFieldError(priceToInput, priceErrorText);
                hasError = true;
            } else {
                priceToInput.value = parsedPriceTo.toFixed(2);
            }
        }

        if (parsedPriceFrom !== null && parsedPriceTo !== null && parsedPriceFrom > parsedPriceTo) {
            showFilterFieldError(priceFromInput, 'Цена «от» не может быть больше цены «до»');
            hasError = true;
        }

        var parsedRatingFrom = null;
        var parsedRatingTo = null;

        if (ratingFromInput && ratingFromInput.value.trim() !== '') {
            parsedRatingFrom = parseFloat(ratingFromInput.value.replace(',', '.'));
            if (isNaN(parsedRatingFrom) || parsedRatingFrom < 0.1 || parsedRatingFrom > 10) {
                showFilterFieldError(ratingFromInput, 'Рейтинг «от» должен быть от 0.1 до 10');
                hasError = true;
            } else {
                ratingFromInput.value = parsedRatingFrom.toFixed(1);
            }
        }

        if (ratingToInput && ratingToInput.value.trim() !== '') {
            parsedRatingTo = parseFloat(ratingToInput.value.replace(',', '.'));
            if (isNaN(parsedRatingTo) || parsedRatingTo < 0.1 || parsedRatingTo > 10) {
                showFilterFieldError(ratingToInput, 'Рейтинг «до» должен быть от 0.1 до 10');
                hasError = true;
            } else {
                ratingToInput.value = parsedRatingTo.toFixed(1);
            }
        }

        if (parsedRatingFrom !== null && parsedRatingTo !== null && parsedRatingFrom > parsedRatingTo) {
            showFilterFieldError(ratingFromInput, 'Рейтинг «от» не может быть больше рейтинга «до»');
            hasError = true;
        }

        var releaseDateFromValue = releaseDateFromInput ? releaseDateFromInput.value : '';
        var releaseDateToValue = releaseDateToInput ? releaseDateToInput.value : '';

        var dateRange = validateDateRange(releaseDateFromValue, releaseDateToValue);
        if (!dateRange.ok) {
            var target = dateRange.field === 'to' ? releaseDateToInput : releaseDateFromInput;
            showFilterFieldError(target, dateRange.error);
            hasError = true;
        } else {
            var fromDateError = '';
            if (releaseDateFromValue && FILTER_LIMITS.releaseDateMin && releaseDateFromValue < FILTER_LIMITS.releaseDateMin) {
                fromDateError = 'Дата «от» не может быть раньше ' + FILTER_LIMITS.releaseDateMin;
            }
            if (!fromDateError && releaseDateFromValue && FILTER_LIMITS.releaseDateMax && releaseDateFromValue > FILTER_LIMITS.releaseDateMax) {
                fromDateError = 'Дата «от» не может быть позже ' + FILTER_LIMITS.releaseDateMax;
            }
            if (fromDateError) {
                showFilterFieldError(releaseDateFromInput, fromDateError);
                hasError = true;
            }

            var toDateError = '';
            if (releaseDateToValue && FILTER_LIMITS.releaseDateMin && releaseDateToValue < FILTER_LIMITS.releaseDateMin) {
                toDateError = 'Дата «до» не может быть раньше ' + FILTER_LIMITS.releaseDateMin;
            }
            if (!toDateError && releaseDateToValue && FILTER_LIMITS.releaseDateMax && releaseDateToValue > FILTER_LIMITS.releaseDateMax) {
                toDateError = 'Дата «до» не может быть позже ' + FILTER_LIMITS.releaseDateMax;
            }
            if (toDateError) {
                showFilterFieldError(releaseDateToInput, toDateError);
                hasError = true;
            }
        }

        var fromHours = parseInt((document.getElementById('durationFromHours') || {}).value || '0', 10);
        var fromMinutes = parseInt((document.getElementById('durationFromMinutes') || {}).value || '0', 10);
        var fromSeconds = parseInt((document.getElementById('durationFromSeconds') || {}).value || '0', 10);
        var toHours = parseInt((document.getElementById('durationToHours') || {}).value || '0', 10);
        var toMinutes = parseInt((document.getElementById('durationToMinutes') || {}).value || '0', 10);
        var toSeconds = parseInt((document.getElementById('durationToSeconds') || {}).value || '0', 10);
        var totalFrom = fromHours * 3600 + fromMinutes * 60 + fromSeconds;
        var totalTo = toHours * 3600 + toMinutes * 60 + toSeconds;

        var durationFromHoursEl = document.getElementById('durationFromHours');
        var durationFromMinutesEl = document.getElementById('durationFromMinutes');
        var durationFromSecondsEl = document.getElementById('durationFromSeconds');
        var durationToHoursEl = document.getElementById('durationToHours');
        var durationToMinutesEl = document.getElementById('durationToMinutes');
        var durationToSecondsEl = document.getElementById('durationToSeconds');

        var maxDur = splitDuration(FILTER_LIMITS.durationMax);
        var durationErrorText =
            'Минимальная длительность — ' + formatDurationHuman(FILTER_LIMITS.durationMin) + ', ' +
            'максимальная — ' + formatDurationHuman(FILTER_LIMITS.durationMax) + '.';

        var fromAllZero = fromHours === 0 && fromMinutes === 0 && fromSeconds === 0 &&
            (durationFromHoursEl.value !== '' || durationFromMinutesEl.value !== '' || durationFromSecondsEl.value !== '');
        var toAllZero = toHours === 0 && toMinutes === 0 && toSeconds === 0 &&
            (durationToHoursEl.value !== '' || durationToMinutesEl.value !== '' || durationToSecondsEl.value !== '');

        if (fromAllZero || fromHours > maxDur.h || fromMinutes > 59 || fromSeconds > 59) {
            showFilterFieldError(durationFromHoursEl, durationErrorText);
            hasError = true;
        }

        if (toAllZero || toHours > maxDur.h || toMinutes > 59 || toSeconds > 59) {
            showFilterFieldError(durationToHoursEl, durationErrorText);
            hasError = true;
        }

        if (totalTo > 0 && totalTo < FILTER_LIMITS.durationMin) {
            showFilterFieldError(durationToHoursEl,
                'Длительность «до» должна быть не менее ' + formatDurationHuman(FILTER_LIMITS.durationMin));
            hasError = true;
        }

        if (totalFrom > 0 && totalTo > 0 && totalFrom > totalTo) {
            showFilterFieldError(durationFromHoursEl,
                'Длительность «от» не может быть больше длительности «до»');
            hasError = true;
        }

        return !hasError;
    }

    function stepFilterValue(inputId, delta, minValue, maxValue, decimals) {
        var input = document.getElementById(inputId);
        if (!input) return;
        var current = parseFloat(input.value.replace(',', '.'));
        if (isNaN(current)) current = 0;
        var next = current + delta;
        var factor = Math.pow(10, decimals);
        next = Math.round(next * factor) / factor;
        if (minValue !== null && next < minValue) next = minValue;
        if (maxValue !== null && next > maxValue) next = maxValue;
        input.value = next.toFixed(decimals);
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var filterSteppers = {};

    function startSteppingFilter(inputId, delta, minValue, maxValue, decimals, buttonElement) {
        if (!filterSteppers[inputId]) {
            filterSteppers[inputId] = createStepper({
                onStep: function (stepDelta) {
                    stepFilterValue(inputId, stepDelta, minValue, maxValue, decimals);
                }
            });
        }
        filterSteppers[inputId].start(delta, buttonElement);
    }

    function stopSteppingFilter(buttonElement) {
        Object.keys(filterSteppers).forEach(function (inputId) {
            filterSteppers[inputId].stop(buttonElement);
        });
        if (buttonElement) buttonElement.classList.remove('holding');
    }

    function setupSelectAllCheckbox(dropdownId, checkboxName) {
        var dropdown = document.getElementById(dropdownId);
        if (!dropdown) return;

        var existing = dropdown.querySelector('.accordion-select-all');
        if (existing) existing.remove();

        var wrapper = document.createElement('label');
        wrapper.className = 'accordion-select-all';

        var selectAllCheckbox = document.createElement('input');
        selectAllCheckbox.type = 'checkbox';
        selectAllCheckbox.className = 'accordion-checkbox';

        var labelText = document.createElement('span');
        labelText.textContent = 'Выбрать всё';

        wrapper.appendChild(selectAllCheckbox);
        wrapper.appendChild(labelText);

        function getVisibleItems() {
            return Array.prototype.slice.call(
                dropdown.querySelectorAll('.accordion-item:not(.is-hidden)')
            );
        }

        function getVisibleCheckboxes() {
            return getVisibleItems()
                .map(function (item) {
                    return item.querySelector('input[name="' + checkboxName + '"]');
                })
                .filter(Boolean);
        }

        function updateState() {
            var visible = getVisibleCheckboxes();
            if (visible.length === 0) {
                selectAllCheckbox.checked = false;
                selectAllCheckbox.indeterminate = false;
                return;
            }
            var checkedCount = visible.filter(function (cb) { return cb.checked; }).length;
            selectAllCheckbox.checked = checkedCount === visible.length;

            selectAllCheckbox.indeterminate = false;
        }

        wrapper.addEventListener('click', function (e) {
            e.preventDefault();
            var visible = getVisibleCheckboxes();
            if (visible.length === 0) return;

            var checkedCount = visible.filter(function (cb) { return cb.checked; }).length;
            var allChecked = checkedCount === visible.length;

            visible.forEach(function (cb) { cb.checked = !allChecked; });
            updateState();
        });

        dropdown.addEventListener('change', function (e) {
            if (e.target && e.target.name === checkboxName) {
                updateState();
            }
        });

        var list = dropdown.querySelector('[data-accordion-list]');
        if (list) {
            list.insertBefore(wrapper, list.firstChild);
        } else {
            dropdown.insertBefore(wrapper, dropdown.firstChild);
        }

        updateState();

        window.__selectAllState = window.__selectAllState || {};
        window.__selectAllState[dropdownId] = updateState;
    }

    function resetFiltersForm() {
        var form = document.getElementById('filterForm');
        if (!form) return;

        form.querySelectorAll('input[type="text"]').forEach(function (el) {
            el.value = '';
            el.style.borderColor = '';
            el.style.boxShadow = '';
        });

        form.querySelectorAll('input[type="date"]').forEach(function (el) {
            el.value = '';
            el.style.borderColor = '';
            el.style.boxShadow = '';
        });

        form.querySelectorAll('input[type="checkbox"]').forEach(function (cb) {
            cb.checked = false;
            cb.indeterminate = false;
            cb.disabled = false;
        });

        var minEl = document.getElementById('minDuration');
        var maxEl = document.getElementById('maxDuration');
        if (minEl) minEl.value = '';
        if (maxEl) maxEl.value = '';

        clearFilterFieldErrors();
        closeAllAccordions();

        document.querySelectorAll('.filter-input-with-clear.has-value').forEach(function (el) {
            el.classList.remove('has-value');
        });

        document.querySelectorAll('.accordion-search.has-value').forEach(function (el) {
            el.classList.remove('has-value');
        });

        document.querySelectorAll('[data-accordion-search-input]').forEach(function (el) {
            el.value = '';
        });

        document.querySelectorAll('[data-accordion-list] .accordion-item.is-hidden').forEach(function (el) {
            el.classList.remove('is-hidden');
        });

        document.querySelectorAll('[data-accordion-empty]').forEach(function (el) {
            el.classList.remove('show');
        });

        if (window.__selectAllState) {
            Object.keys(window.__selectAllState).forEach(function (dropdownId) {
                try { window.__selectAllState[dropdownId](); } catch (e) {}
            });
        }

        updateSortButtons(null);
        syncHiddenSortFields(null);
    }

    function initFilterMasks() {
        if (typeof window.setupFilterRatingInput === 'function') {
            window.setupFilterRatingInput('ratingFrom');
            window.setupFilterRatingInput('ratingTo');
        }
        if (typeof window.setupFilterPriceInput === 'function') {
            window.setupFilterPriceInput('priceFrom');
            window.setupFilterPriceInput('priceTo');
        }
        if (typeof window.setupDurationStrictFilter === 'function') {
            window.setupDurationStrictFilter('durationFromHours');
            window.setupDurationStrictFilter('durationToHours');
            window.setupDurationStrictFilter('durationFromMinutes');
            window.setupDurationStrictFilter('durationToMinutes');
            window.setupDurationStrictFilter('durationFromSeconds');
            window.setupDurationStrictFilter('durationToSeconds');
        }

        loadFilterMeta().then(function () {
            setupInfiniteList('directorsDropdown', 'directors', '/filters/directors/search', displayDirectorName);
            setupInfiniteList('genresDropdown', 'genres', '/filters/genres/search', null);
            setupSelectAllCheckbox('directorsDropdown', 'directors');
            setupSelectAllCheckbox('genresDropdown', 'genres');
        });
    }

    function getCheckedValues(name) {
        var seen = new Set();
        var result = [];
        document.querySelectorAll('#filterForm input[type="checkbox"][name="' + name + '"]:checked')
            .forEach(function (cb) {
                if (seen.has(cb.value)) return;
                seen.add(cb.value);
                result.push(cb.value);
            });
        return result;
    }

    function getAllCheckboxValues(name) {
        var seen = new Set();
        var result = [];
        document.querySelectorAll('#filterForm input[type="checkbox"][name="' + name + '"]')
            .forEach(function (cb) {
                if (seen.has(cb.value)) return;
                seen.add(cb.value);
                result.push(cb.value);
            });
        return result;
    }

    function pickFilterList(name) {
        var checked = getCheckedValues(name);
        if (checked.length === 0) return null;

        var all = getAllCheckboxValues(name);
        if (all.length === 0) return null;

        // Если выбрано больше половины — отправляем EXCLUDE
        if (checked.length > all.length / 2) {
            var checkedSet = new Set(checked);
            var excluded = all.filter(function (v) { return !checkedSet.has(v); });
            if (excluded.length === 0) {
                // Выбраны абсолютно все — фильтр не нужен, показываем всё
                return null;
            }
            return { mode: 'exclude', values: excluded };
        }

        // Иначе — INCLUDE
        return { mode: 'include', values: checked };
    }

    function collectMeaningfulFilters() {
        var result = {};

        function num(id) {
            var el = document.getElementById(id);
            if (!el || el.value.trim() === '') return null;
            var n = parseFloat(el.value.replace(',', '.'));
            return isNaN(n) ? null : n;
        }

        function str(id) {
            var el = document.getElementById(id);
            return el ? el.value.trim() : '';
        }

        var pFrom = num('priceFrom');
        var pTo = num('priceTo');
        if (pFrom != null && FILTER_LIMITS.priceMin != null && pFrom > FILTER_LIMITS.priceMin) {
            result.priceFrom = pFrom.toFixed(2);
        }
        if (pTo != null && FILTER_LIMITS.priceMax != null && pTo < FILTER_LIMITS.priceMax) {
            result.priceTo = pTo.toFixed(2);
        }

        var rFrom = num('ratingFrom');
        var rTo = num('ratingTo');
        if (rFrom != null && rFrom > 0.1) result.ratingFrom = rFrom.toFixed(1);
        if (rTo != null && rTo < 10) result.ratingTo = rTo.toFixed(1);

        var relFrom = str('releaseDateFrom');
        var relTo = str('releaseDateTo');
        if (relFrom && FILTER_LIMITS.releaseDateMin && relFrom > FILTER_LIMITS.releaseDateMin) {
            result.releaseDateFrom = relFrom;
        }
        if (relTo && FILTER_LIMITS.releaseDateMax && relTo < FILTER_LIMITS.releaseDateMax) {
            result.releaseDateTo = relTo;
        }

        calculateRangeMinutes();
        var durFrom = str('minDuration');
        var durTo = str('maxDuration');
        if (durFrom && FILTER_LIMITS.durationMin != null && Number(durFrom) > FILTER_LIMITS.durationMin) {
            result.durationFrom = durFrom;
        }
        if (durTo && FILTER_LIMITS.durationMax != null && Number(durTo) < FILTER_LIMITS.durationMax) {
            result.durationTo = durTo;
        }

        var genrePick = pickFilterList('genres');
        if (genrePick) {
            if (genrePick.mode === 'include') {
                result.genres = genrePick.values;
            } else if (genrePick.mode === 'exclude') {
                result.genresExclude = genrePick.values;
            }
        }

        var directorPick = pickFilterList('directors');
        if (directorPick) {
            if (directorPick.mode === 'include') {
                result.directors = directorPick.values;
            } else if (directorPick.mode === 'exclude') {
                result.directorsExclude = directorPick.values;
            }
        }

        var actorsInput = document.getElementById('actorsInput');
        if (actorsInput && actorsInput.value.trim() !== '') {
            var actorsList = actorsInput.value.split(/[,;]+/).map(function (s) { return s.trim(); }).filter(function (s) { return s.length > 0; });
            if (actorsList.length > 0) {
                result.actors = actorsList;
            }
        }

        var sort = getCurrentSortSelection();
        if (sort) {
            result.sortBy = sort.key;
            result.sortDirection = sort.dir;
        }

        return result;
    }

    function buildQueryString(title, filters) {
        var params = new URLSearchParams();
        if (title) params.set('title', title);
        Object.keys(filters).forEach(function (key) {
            var v = filters[key];
            if (Array.isArray(v)) {
                var seen = new Set();
                v.forEach(function (item) {
                    if (seen.has(item)) return;
                    seen.add(item);
                    params.append(key, item);
                });
            } else {
                params.set(key, v);
            }
        });
        return params.toString();
    }

    function submitCombinedSearch() {
        if (!validateFilter()) return;

        var headerInput = document.getElementById('headerSearchInput');
        var title = headerInput ? headerInput.value.trim().replace(/…+$/, '').trim() : '';
        var filters = collectMeaningfulFilters();

        if (!title && Object.keys(filters).length === 0) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Введите запрос или задайте значимые фильтры', 'warning');
            }
            return;
        }

        var query = buildQueryString(title, filters);
        window.location.href = '/movies' + (query ? '?' + query : '');
    }

    function applyFiltersFromModal() {
        if (!validateFilter()) return;

        var headerInput = document.getElementById('headerSearchInput');
        var title = headerInput ? headerInput.value.trim().replace(/…+$/, '').trim() : '';
        var filters = collectMeaningfulFilters();

        if (!title && Object.keys(filters).length === 0) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Ничего не выбрано: фильтр не изменит список.', 'warning');
            }
            toggleFiltersModal(false);
            return;
        }

        toggleFiltersModal(false);
        var query = buildQueryString(title, filters);
        window.location.href = '/movies' + (query ? '?' + query : '');
    }

    function getFocusableFilterFields() {
        var form = document.getElementById('filterForm');
        if (!form) return [];
        return Array.prototype.slice.call(
            form.querySelectorAll(
                'input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled]), .accordion-trigger, .sort-filter-btn'
            )
        ).filter(function (el) {
            if (el.offsetParent === null) return false;
            if (el.classList && el.classList.contains('accordion-item')) return false;
            return true;
        });
    }

    function focusNextFilterField(currentEl) {
        var fields = getFocusableFilterFields();
        if (fields.length === 0) return false;
        var idx = fields.indexOf(currentEl);
        var nextIdx = idx === -1 ? 0 : (idx + 1) % fields.length;
        var nextEl = fields[nextIdx];
        if (nextEl) {
            try { nextEl.focus(); } catch (e) {}
            if (nextEl.tagName === 'INPUT' && (nextEl.type === 'text' || nextEl.type === 'date')) {
                try { nextEl.select(); } catch (e) {}
            }
            return true;
        }
        return false;
    }

    function isFilterInputElement(el) {
        if (!el) return false;
        if (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA' && el.tagName !== 'SELECT') return false;
        if (el.type === 'hidden') return false;
        return true;
    }

    function initFilterEnterKeyHandler() {
        var overlay = document.getElementById('filtersModalOverlay');
        if (!overlay) return;
        if (overlay.dataset.enterBound === '1') return;
        overlay.dataset.enterBound = '1';

        overlay.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') return;
            if (e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) return;

            var active = document.activeElement;
            var insideModal = overlay.contains(active);
            if (!insideModal) return;

            if (isFilterInputElement(active)) {
                e.preventDefault();
                e.stopPropagation();

                var fields = getFocusableFilterFields();
                var isLastField = fields.length > 0 && fields[fields.length - 1] === active;

                if (isLastField) {
                    applyFiltersFromModal();
                } else {
                    focusNextFilterField(active);
                }
                return;
            }

            if (active && active.classList && active.classList.contains('accordion-trigger')) {
                return;
            }

            if (active && active.classList && active.classList.contains('sort-filter-btn')) {
                return;
            }

            e.preventDefault();
            e.stopPropagation();
            applyFiltersFromModal();
        });
    }

    function readSortFromUrl() {
        var searchString = window.location.search || '';
        if (searchString.charAt(0) === '?') searchString = searchString.substring(1);
        if (!searchString) return null;

        var parts = searchString.split('&');
        var sortByValue = null;
        var sortDirectionValue = null;

        for (var i = 0; i < parts.length; i++) {
            var kv = parts[i].split('=');
            var name = decodeURIComponent(kv[0] || '');
            var value = decodeURIComponent(kv[1] || '');
            if (name === 'sortBy') sortByValue = value;
            if (name === 'sortDirection') sortDirectionValue = value.toUpperCase();
        }

        if (!sortByValue || !sortDirectionValue) return null;
        if (sortDirectionValue !== 'ASC' && sortDirectionValue !== 'DESC') return null;
        return { key: sortByValue, dir: sortDirectionValue };
    }

    function getHiddenSortField(fieldName) {
        var allFields = document.querySelectorAll('input[name="' + fieldName + '"]');
        return allFields.length ? allFields[0] : null;
    }

    function updateSortButtons(sortState) {
        var buttons = document.querySelectorAll('.sort-filter-btn');
        for (var i = 0; i < buttons.length; i++) {
            var btn = buttons[i];
            var sortKey = btn.getAttribute('data-sort-key');
            var sortDirection = btn.getAttribute('data-sort-dir').toUpperCase();
            if (sortState && sortState.key === sortKey && sortState.dir.toUpperCase() === sortDirection) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        }
    }

    function syncHiddenSortFields(sortState) {
        var sortByField = getHiddenSortField('sortBy');
        var sortDirectionField = getHiddenSortField('sortDirection');
        if (sortByField) sortByField.value = sortState ? sortState.key : '';
        if (sortDirectionField) sortDirectionField.value = sortState ? sortState.dir.toUpperCase() : '';
    }

    function getCurrentSortSelection() {
        var sortByField = getHiddenSortField('sortBy');
        var sortDirectionField = getHiddenSortField('sortDirection');
        if (!sortByField || !sortDirectionField) return null;
        var sortKey = (sortByField.value || '').trim();
        var sortDirection = (sortDirectionField.value || '').trim().toUpperCase();
        if (!sortKey || !sortDirection) return null;
        if (sortDirection !== 'ASC' && sortDirection !== 'DESC') return null;
        return { key: sortKey, dir: sortDirection };
    }

    function initFilterSort() {
        if (window.__filterSortInitDone) return;
        window.__filterSortInitDone = true;

        var urlSortState = readSortFromUrl();
        updateSortButtons(urlSortState);
        syncHiddenSortFields(urlSortState);

        document.addEventListener('click', function (e) {
            var target = e.target;
            while (target && target !== document) {
                if (target.classList && target.classList.contains('sort-filter-btn')) break;
                target = target.parentNode;
            }
            if (!target || target === document) return;

            e.preventDefault();

            var sortKey = target.getAttribute('data-sort-key');
            var sortDirection = target.getAttribute('data-sort-dir').toUpperCase();
            if (!sortKey || !sortDirection) return;

            var current = getCurrentSortSelection();
            var isSame = current && current.key === sortKey && current.dir === sortDirection;

            if (isSame) {
                updateSortButtons(null);
                syncHiddenSortFields(null);
            } else {
                updateSortButtons({ key: sortKey, dir: sortDirection });
                syncHiddenSortFields({ key: sortKey, dir: sortDirection });
            }
        });
    }

    function initFiltersApplyButton() {
        var applyBtn = document.getElementById('filtersApplyBtn');
        if (!applyBtn) return;
        if (applyBtn.dataset.applyBound === '1') return;
        applyBtn.dataset.applyBound = '1';
        applyBtn.addEventListener('click', function (e) {
            e.preventDefault();
            applyFiltersFromModal();
        });
    }

    function initOverlayAndClose() {
        var overlay = document.getElementById('filtersModalOverlay');
        if (overlay && overlay.dataset.overlayBound !== '1') {
            overlay.dataset.overlayBound = '1';
            overlay.addEventListener('click', function (e) {
                if (e.target === overlay) toggleFiltersModal(false);
            });
        }
        var closeBtn = document.getElementById('filtersModalCloseBtn');
        if (closeBtn && closeBtn.dataset.closeBound !== '1') {
            closeBtn.dataset.closeBound = '1';
            closeBtn.addEventListener('click', function () {
                toggleFiltersModal(false);
            });
        }
        var resetBtn = document.getElementById('filtersResetBtn');
        if (resetBtn && resetBtn.dataset.resetBound !== '1') {
            resetBtn.dataset.resetBound = '1';
            resetBtn.addEventListener('click', function () {
                resetFiltersForm();
            });
        }
    }

    function initStepperButtons() {
        document.querySelectorAll('.stepper-btn[data-stepper-target]').forEach(function (btn) {
            if (btn.dataset.stepperBound === '1') return;
            btn.dataset.stepperBound = '1';
            var target = btn.getAttribute('data-stepper-target');
            var delta = parseFloat(btn.getAttribute('data-stepper-delta')) || 0;
            var dec = parseInt(btn.getAttribute('data-stepper-decimals'), 10) || 0;

            function start(e) {
                e.preventDefault();
                var minAttr = btn.getAttribute('data-stepper-min');
                var maxAttr = btn.getAttribute('data-stepper-max');
                var minV = (minAttr === '' || minAttr === null) ? null : parseFloat(minAttr);
                var maxV = (maxAttr === '' || maxAttr === null) ? null : parseFloat(maxAttr);
                if (typeof startSteppingFilter === 'function') {
                    startSteppingFilter(target, delta, minV, maxV, dec, btn);
                }
            }
            function stop() {
                if (typeof stopSteppingFilter === 'function') {
                    stopSteppingFilter(btn);
                }
            }

            btn.addEventListener('mousedown', start);
            btn.addEventListener('mouseup', stop);
            btn.addEventListener('mouseleave', stop);
            btn.addEventListener('touchstart', start, { passive: false });
            btn.addEventListener('touchend', stop);
            btn.addEventListener('touchcancel', stop);
        });
    }

    function bindWheelPassthrough() {
        var outer = document.getElementById('filterForm');
        if (!outer) return;
        if (outer.dataset.wheelPassBound === '1') return;
        outer.dataset.wheelPassBound = '1';

        outer.querySelectorAll('[data-accordion-dropdown]').forEach(function (inner) {
            inner.addEventListener('wheel', function (e) {
                var atTop = inner.scrollTop <= 0;
                var atBottom = Math.ceil(inner.scrollTop + inner.clientHeight) >= inner.scrollHeight - 1;
                var goingUp = e.deltaY < 0;
                var goingDown = e.deltaY > 0;

                if ((goingDown && atBottom) || (goingUp && atTop) || inner.scrollHeight <= inner.clientHeight) {
                    e.preventDefault();
                    var step = e.deltaY;
                    if (e.deltaMode === 1) step *= 16;
                    else if (e.deltaMode === 2) step *= outer.clientHeight;
                    outer.scrollTop += step;
                }
            }, { passive: false });
        });
    }

    function initAll() {
        try { initFilterSort(); } catch (e) { console.error('[filters] sort init failed', e); }
        try { initFilterMasks(); } catch (e) { console.error('[filters] masks init failed', e); }
        try { initFilterAccordions(); } catch (e) { console.error('[filters] accordions init failed', e); }
        try { initFilterClearButtons(); } catch (e) { console.error('[filters] clear buttons init failed', e); }
        try { initFiltersApplyButton(); } catch (e) { console.error('[filters] apply init failed', e); }
        try { initOverlayAndClose(); } catch (e) { console.error('[filters] overlay init failed', e); }
        try { initStepperButtons(); } catch (e) { console.error('[filters] stepper init failed', e); }
        try { initFilterEnterKeyHandler(); } catch (e) { console.error('[filters] enter init failed', e); }
        try { bindWheelPassthrough(); } catch (e) { console.error('[filters] wheel init failed', e); }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll, { once: true });
    } else {
        initAll();
    }

    window.validateSearchQuery = validateSearchQuery;
    window.toggleFiltersModal = toggleFiltersModal;
    window.calculateRangeMinutes = calculateRangeMinutes;
    window.normalizeDurationFilters = calculateRangeMinutes;
    window.validateFilter = validateFilter;
    window.stepFilterValue = stepFilterValue;
    window.startSteppingFilter = startSteppingFilter;
    window.stopSteppingFilter = stopSteppingFilter;
    window.setupSelectAllCheckbox = setupSelectAllCheckbox;
    window.resetFiltersForm = resetFiltersForm;
    window.applyFiltersFromModal = applyFiltersFromModal;
    window.submitCombinedSearch = submitCombinedSearch;
    window.focusNextFilterField = focusNextFilterField;
    window.initFilterMasks = initFilterMasks;
})();