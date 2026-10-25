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

    var MAX_SELECTED_PER_DROPDOWN = 200;

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
            priceFromInput.setAttribute('placeholder', 'От ' + formatPriceValue(FILTER_LIMITS.priceMin) + ' ₽');
        }
        if (priceToInput) {
            priceToInput.setAttribute('placeholder', 'До ' + formatPriceValue(FILTER_LIMITS.priceMax) + ' ₽');
        }

        document.querySelectorAll(
            '.stepper-btn[data-stepper-target="priceFrom"], .stepper-btn[data-stepper-target="priceTo"]'
        ).forEach(function (button) {
            button.setAttribute('data-stepper-min', String(FILTER_LIMITS.priceMin));
            button.setAttribute('data-stepper-max', String(FILTER_LIMITS.priceMax));
        });
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

    function enforceCheckboxLimit(dropdownElementId, maxSelectedCount, labelText) {
        var dropdownElement = document.getElementById(dropdownElementId);
        if (!dropdownElement) return;

        dropdownElement.addEventListener('change', function (changeEvent) {
            var targetElement = changeEvent.target;
            if (!targetElement || targetElement.type !== 'checkbox') return;
            if (targetElement.name !== 'directors' && targetElement.name !== 'genres') return;

            var allCheckboxes = dropdownElement.querySelectorAll('input[name="' + targetElement.name + '"]');
            var checkedCheckboxes = dropdownElement.querySelectorAll('input[name="' + targetElement.name + '"]:checked');

            var allChecked = allCheckboxes.length > 0 && checkedCheckboxes.length === allCheckboxes.length;
            if (allChecked) return;

            if (checkedCheckboxes.length > maxSelectedCount) {
                targetElement.checked = false;
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage('Можно выбрать не более ' + maxSelectedCount + ' значений в «' + labelText + '»', 'warning');
                }
            }
        });
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
        document.querySelectorAll('.accordion-group.active').forEach(function (accordionGroupElement) {
            accordionGroupElement.classList.remove('active');
        });
        document.querySelectorAll('.accordion-dropdown.active').forEach(function (accordionDropdownElement) {
            accordionDropdownElement.classList.remove('active');
        });
    }

    function toggleFiltersModal(shouldShow) {
        var filtersModalOverlay = document.getElementById('filtersModalOverlay');
        if (!filtersModalOverlay) return;

        if (typeof shouldShow === 'boolean') {
            if (shouldShow) {
                filtersModalOverlay.classList.add('show');
                document.body.style.overflow = 'hidden';
            } else {
                filtersModalOverlay.classList.remove('show');
                document.body.style.overflow = '';
                closeAllAccordions();
            }
        } else {
            filtersModalOverlay.classList.toggle('show');
            if (filtersModalOverlay.classList.contains('show')) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = '';
                closeAllAccordions();
            }
        }
    }

    function handleOverlayClick(clickEvent) {
        if (clickEvent.target && clickEvent.target.id === 'filtersModalOverlay') {
            toggleFiltersModal(false);
        }
    }

    function initFilterAccordions() {
        document.querySelectorAll('.accordion-trigger').forEach(function (accordionTriggerElement) {
            if (accordionTriggerElement.dataset.accordionBound === '1') return;
            accordionTriggerElement.dataset.accordionBound = '1';
            accordionTriggerElement.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                clickEvent.stopPropagation();
                var targetElementId = accordionTriggerElement.getAttribute('data-target');
                if (!targetElementId) return;
                var accordionDropdownElement = document.getElementById(targetElementId);
                if (!accordionDropdownElement) return;
                var accordionGroupElement = accordionTriggerElement.closest('.accordion-group');
                if (!accordionGroupElement) return;
                var willOpen = !accordionGroupElement.classList.contains('active');
                accordionGroupElement.classList.toggle('active', willOpen);
                accordionDropdownElement.classList.toggle('active', willOpen);
            });
        });
    }

    function initFilterClearButtons() {
        document.querySelectorAll('.filter-clear-btn').forEach(function (clearButtonElement) {
            if (clearButtonElement.dataset.clearBound === '1') return;
            clearButtonElement.dataset.clearBound = '1';
            var targetElementId = clearButtonElement.getAttribute('data-clear-target');
            if (!targetElementId) return;
            var targetInputElement = document.getElementById(targetElementId);
            if (!targetInputElement) return;
            var wrapperElement = clearButtonElement.closest('.filter-input-with-clear');

            function syncHasValueClass() {
                if (!wrapperElement) return;
                wrapperElement.classList.toggle('has-value', targetInputElement.value !== '');
            }

            clearButtonElement.addEventListener('click', function (clickEvent) {
                clickEvent.preventDefault();
                clickEvent.stopPropagation();
                targetInputElement.value = '';
                targetInputElement.dispatchEvent(new Event('input', { bubbles: true }));
                targetInputElement.dispatchEvent(new Event('change', { bubbles: true }));
                syncHasValueClass();
                targetInputElement.focus();
            });

            targetInputElement.addEventListener('input', syncHasValueClass);
            targetInputElement.addEventListener('change', syncHasValueClass);
            syncHasValueClass();
        });
    }

    function extractSurname(fullNameValue) {
        var trimmedFullName = String(fullNameValue || '').trim();
        if (!trimmedFullName) return '';
        var firstSpaceIndex = trimmedFullName.indexOf(' ');
        if (firstSpaceIndex === -1) return trimmedFullName;
        var remainingPart = trimmedFullName.substring(firstSpaceIndex + 1).trim();
        return remainingPart || trimmedFullName;
    }

    function normalizeForSort(value) {
        return String(value || '')
            .toLocaleLowerCase('ru-RU')
            .replace(/ё/g, 'е');
    }

    function displayDirectorName(fullNameValue) {
        var trimmedFullName = String(fullNameValue || '').trim();
        if (!trimmedFullName) return '';
        var firstSpaceIndex = trimmedFullName.indexOf(' ');
        if (firstSpaceIndex === -1) return trimmedFullName;
        var firstNamePart = trimmedFullName.substring(0, firstSpaceIndex).trim();
        var remainingPart = trimmedFullName.substring(firstSpaceIndex + 1).trim();
        if (!remainingPart) return trimmedFullName;
        return remainingPart + ' ' + firstNamePart;
    }

    function sortDirectorsBySurname(directorList) {
        return directorList.slice().sort(function (leftDirector, rightDirector) {
            var leftSurname = normalizeForSort(extractSurname(leftDirector));
            var rightSurname = normalizeForSort(extractSurname(rightDirector));
            if (leftSurname !== rightSurname) return leftSurname.localeCompare(rightSurname, 'ru-RU');
            return normalizeForSort(leftDirector).localeCompare(normalizeForSort(rightDirector), 'ru-RU');
        });
    }

    function sortGenresAlphabetically(genreList) {
        return genreList.slice().sort(function (leftGenre, rightGenre) {
            return normalizeForSort(leftGenre).localeCompare(normalizeForSort(rightGenre), 'ru-RU');
        });
    }

    function buildAccordionCheckbox(checkboxName, checkboxValue, displayText) {
        var labelElement = document.createElement('label');
        labelElement.className = 'accordion-item';
        var inputElement = document.createElement('input');
        inputElement.type = 'checkbox';
        inputElement.name = checkboxName;
        inputElement.className = 'accordion-checkbox';
        inputElement.value = checkboxValue;
        var spanElement = document.createElement('span');
        spanElement.textContent = displayText != null ? displayText : checkboxValue;
        labelElement.appendChild(inputElement);
        labelElement.appendChild(spanElement);
        return labelElement;
    }

    function appendCheckboxesToAccordion(dropdownElement, checkboxName, itemsList, displayFunction) {
        var accordionListElement = dropdownElement.querySelector('[data-accordion-list]');
        if (!accordionListElement) return;

        (itemsList || []).forEach(function (itemValue) {
            var displayValue = displayFunction ? displayFunction(itemValue) : itemValue;
            accordionListElement.appendChild(buildAccordionCheckbox(checkboxName, itemValue, displayValue));
        });

        var selectedAttributeValue = dropdownElement.getAttribute('data-selected');
        if (selectedAttributeValue && selectedAttributeValue.trim() !== '') {
            var selectedValues = selectedAttributeValue.split(',').map(function (s) { return s.trim(); });
            accordionListElement.querySelectorAll('input[name="' + checkboxName + '"]').forEach(function (chk) {
                if (selectedValues.indexOf(chk.value) !== -1) chk.checked = true;
            });
        }
    }

    function setupInfiniteList(dropdownId, checkboxName, endpoint, displayFunction) {
        var dropdownElement = document.getElementById(dropdownId);
        if (!dropdownElement) return;

        var accordionListElement = dropdownElement.querySelector('[data-accordion-list]');
        var emptyElement = dropdownElement.querySelector('[data-accordion-empty]');
        if (!accordionListElement) return;

        var isLoading = false;
        var hasMore = true;
        var nextCursor = null;
        var currentQuery = '';

        var sentinel = document.createElement('div');
        sentinel.style.height = '20px';
        sentinel.style.width = '100%';
        sentinel.style.pointerEvents = 'none';
        sentinel.className = 'infinite-scroll-sentinel';
        accordionListElement.after(sentinel);

        var observer = new IntersectionObserver(function (entries) {
            if (entries[0].isIntersecting) loadData();
        }, { root: dropdownElement, rootMargin: '50px' });
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

                    var displayData = checkboxName === 'directors'
                        ? sortDirectorsBySurname(items)
                        : sortGenresAlphabetically(items);

                    appendCheckboxesToAccordion(dropdownElement, checkboxName, displayData, displayFunction);
                    sentinel.textContent = '';

                    if (!hasMore) observer.unobserve(sentinel);

                    if (emptyElement) {
                        var hasVisibleItems = accordionListElement.querySelectorAll('.accordion-item').length > 0;
                        emptyElement.classList.toggle('show', !hasVisibleItems && currentQuery.trim() !== '');
                    }
                })
                .catch(function () {
                    sentinel.textContent = 'Ошибка загрузки';
                })
                .finally(function () {
                    isLoading = false;
                });
        }

        var searchInput = dropdownElement.querySelector('[data-accordion-search-input]');
        if (searchInput) {
            var debounceTimer = null;
            searchInput.addEventListener('input', function () {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(function () {
                    currentQuery = searchInput.value;
                    nextCursor = null;
                    hasMore = true;
                    accordionListElement.innerHTML = '';
                    observer.observe(sentinel);

                    if (window.__selectAllState && window.__selectAllState[dropdownId]) {
                        window.__selectAllState[dropdownId]();
                    }
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
                if (meta.ranges) {
                    if (meta.ranges.price) {
                        if (meta.ranges.price.min != null) FILTER_LIMITS.priceMin = Number(meta.ranges.price.min);
                        if (meta.ranges.price.max != null) FILTER_LIMITS.priceMax = Number(meta.ranges.price.max);
                    }
                    if (meta.ranges.duration) {
                        if (meta.ranges.duration.min != null) FILTER_LIMITS.durationMin = Number(meta.ranges.duration.min);
                        if (meta.ranges.duration.max != null) FILTER_LIMITS.durationMax = Number(meta.ranges.duration.max);
                    }
                    if (meta.ranges.releaseDate) {
                        if (meta.ranges.releaseDate.min != null) FILTER_LIMITS.releaseDateMin = String(meta.ranges.releaseDate.min);
                        if (meta.ranges.releaseDate.max != null) FILTER_LIMITS.releaseDateMax = String(meta.ranges.releaseDate.max);
                    }
                }
                applyPriceLimitsToUi();
                applyDurationLimitsToUi();
                applyReleaseDateLimitsToUi();
            })
            .catch(function () {});
    }

    function calculateRangeMinutes() {
        function readIntValue(elementId) {
            var element = document.getElementById(elementId);
            return element ? (parseInt(element.value, 10) || 0) : 0;
        }

        var fromHoursValue = readIntValue('durationFromHours');
        var fromMinutesValue = readIntValue('durationFromMinutes');
        var fromSecondsValue = readIntValue('durationFromSeconds');
        var toHoursValue = readIntValue('durationToHours');
        var toMinutesValue = readIntValue('durationToMinutes');
        var toSecondsValue = readIntValue('durationToSeconds');

        var totalFromSeconds = (fromHoursValue * 3600) + (fromMinutesValue * 60) + fromSecondsValue;
        var totalToSeconds = (toHoursValue * 3600) + (toMinutesValue * 60) + toSecondsValue;

        var minDurationElement = document.getElementById('minDuration');
        var maxDurationElement = document.getElementById('maxDuration');

        if (minDurationElement) minDurationElement.value = totalFromSeconds > 0 ? totalFromSeconds : '';
        if (maxDurationElement) maxDurationElement.value = totalToSeconds > 0 ? totalToSeconds : '';
    }

    function normalizeDurationFilters() {
        calculateRangeMinutes();
    }

    function clearFilterFieldErrors() {
        document.querySelectorAll('.filter-group.has-error').forEach(function (groupEl) {
            groupEl.classList.remove('has-error');
        });
        document.querySelectorAll('.filter-field-error').forEach(function (errEl) {
            errEl.textContent = '';
            errEl.style.display = 'none';
        });
        document.querySelectorAll('.filter-input, .accordion-trigger').forEach(function (inputElement) {
            inputElement.style.borderColor = '';
            inputElement.style.boxShadow = '';
        });
        document.querySelectorAll('.filter-input-with-clear.has-error, .amount-input-with-stepper.has-error').forEach(function (el) {
            el.classList.remove('has-error');
        });
        var ratingFilterErrorElement = document.getElementById('ratingFilterError');
        if (ratingFilterErrorElement) {
            ratingFilterErrorElement.style.display = 'none';
            ratingFilterErrorElement.innerText = '';
        }
    }

    function showFilterFieldError(inputElement, errorMessage) {
        if (!inputElement) return;

        var groupElement = inputElement.closest('.filter-group') || inputElement.parentNode;
        if (!groupElement) return;

        groupElement.classList.add('has-error');

        var errorElement = groupElement.querySelector('.filter-field-error');
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'filter-field-error';
            groupElement.appendChild(errorElement);
        }

        errorElement.textContent = errorMessage;
        errorElement.title = errorMessage;
        errorElement.style.display = '';

        var wrapper = inputElement.closest('.filter-input-with-clear, .amount-input-with-stepper');
        if (wrapper) wrapper.classList.add('has-error');

        try { inputElement.focus(); } catch (focusError) {}
    }

    function getCheckboxState(checkboxName) {
        var allCheckboxes = document.querySelectorAll('input[name="' + checkboxName + '"]');
        var checkedCheckboxes = document.querySelectorAll('input[name="' + checkboxName + '"]:checked');
        return { total: allCheckboxes.length, checked: checkedCheckboxes.length };
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
        normalizeDurationFilters();

        var hasError = false;

        var priceFromInput = document.getElementById('priceFrom');
        var priceToInput = document.getElementById('priceTo');
        var ratingFromInput = document.getElementById('ratingFrom');
        var ratingToInput = document.getElementById('ratingTo');
        var releaseDateFromInput = document.getElementById('releaseDateFrom');
        var releaseDateToInput = document.getElementById('releaseDateTo');

        var fromHoursValue = parseInt((document.getElementById('durationFromHours') || {}).value || '0', 10);
        var fromMinutesValue = parseInt((document.getElementById('durationFromMinutes') || {}).value || '0', 10);
        var fromSecondsValue = parseInt((document.getElementById('durationFromSeconds') || {}).value || '0', 10);
        var toHoursValue = parseInt((document.getElementById('durationToHours') || {}).value || '0', 10);
        var toMinutesValue = parseInt((document.getElementById('durationToMinutes') || {}).value || '0', 10);
        var toSecondsValue = parseInt((document.getElementById('durationToSeconds') || {}).value || '0', 10);
        var totalFromSeconds = fromHoursValue * 3600 + fromMinutesValue * 60 + fromSecondsValue;
        var totalToSeconds = toHoursValue * 3600 + toMinutesValue * 60 + toSecondsValue;

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

        if (!hasError && priceToInput && priceToInput.value.trim() !== '') {
            parsedPriceTo = parseFloat(priceToInput.value.replace(',', '.'));
            if (isNaN(parsedPriceTo) || parsedPriceTo < FILTER_LIMITS.priceMin || parsedPriceTo > FILTER_LIMITS.priceMax) {
                showFilterFieldError(priceToInput, priceErrorText);
                hasError = true;
            } else {
                priceToInput.value = parsedPriceTo.toFixed(2);
            }
        }

        if (!hasError && parsedPriceFrom !== null && parsedPriceTo !== null && parsedPriceFrom > parsedPriceTo) {
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

        if (!hasError && ratingToInput && ratingToInput.value.trim() !== '') {
            parsedRatingTo = parseFloat(ratingToInput.value.replace(',', '.'));
            if (isNaN(parsedRatingTo) || parsedRatingTo < 0.1 || parsedRatingTo > 10) {
                showFilterFieldError(ratingToInput, 'Рейтинг «до» должен быть от 0.1 до 10');
                hasError = true;
            } else {
                ratingToInput.value = parsedRatingTo.toFixed(1);
            }
        }

        if (!hasError && parsedRatingFrom !== null && parsedRatingTo !== null && parsedRatingFrom > parsedRatingTo) {
            showFilterFieldError(ratingFromInput, 'Рейтинг «от» не может быть больше рейтинга «до»');
            hasError = true;
        }

        var releaseDateFromValue = releaseDateFromInput ? releaseDateFromInput.value : '';
        var releaseDateToValue = releaseDateToInput ? releaseDateToInput.value : '';

        var dateRangeValidationResult = validateDateRange(releaseDateFromValue, releaseDateToValue);
        if (!dateRangeValidationResult.ok) {
            var targetDateInput = dateRangeValidationResult.field === 'to' ? releaseDateToInput : releaseDateFromInput;
            showFilterFieldError(targetDateInput, dateRangeValidationResult.error);
            hasError = true;
        } else {
            if (releaseDateFromValue && FILTER_LIMITS.releaseDateMin && releaseDateFromValue < FILTER_LIMITS.releaseDateMin) {
                showFilterFieldError(releaseDateFromInput, 'Дата «от» не может быть раньше ' + FILTER_LIMITS.releaseDateMin);
                hasError = true;
            }
            if (!hasError && releaseDateFromValue && FILTER_LIMITS.releaseDateMax && releaseDateFromValue > FILTER_LIMITS.releaseDateMax) {
                showFilterFieldError(releaseDateFromInput, 'Дата «от» не может быть позже ' + FILTER_LIMITS.releaseDateMax);
                hasError = true;
            }
            if (!hasError && releaseDateToValue && FILTER_LIMITS.releaseDateMin && releaseDateToValue < FILTER_LIMITS.releaseDateMin) {
                showFilterFieldError(releaseDateToInput, 'Дата «до» не может быть раньше ' + FILTER_LIMITS.releaseDateMin);
                hasError = true;
            }
            if (!hasError && releaseDateToValue && FILTER_LIMITS.releaseDateMax && releaseDateToValue > FILTER_LIMITS.releaseDateMax) {
                showFilterFieldError(releaseDateToInput, 'Дата «до» не может быть позже ' + FILTER_LIMITS.releaseDateMax);
                hasError = true;
            }
        }

        var durationFromHoursElement = document.getElementById('durationFromHours');
        var durationFromMinutesElement = document.getElementById('durationFromMinutes');
        var durationFromSecondsElement = document.getElementById('durationFromSeconds');
        var durationToHoursElement = document.getElementById('durationToHours');
        var durationToMinutesElement = document.getElementById('durationToMinutes');
        var durationToSecondsElement = document.getElementById('durationToSeconds');

        var maxDurationValues = splitDuration(FILTER_LIMITS.durationMax);
        var durationErrorText =
            'Минимальная длительность — ' + formatDurationHuman(FILTER_LIMITS.durationMin) + ', ' +
            'максимальная — ' + formatDurationHuman(FILTER_LIMITS.durationMax) + '.';

        var fromAllZero = fromHoursValue === 0 && fromMinutesValue === 0 && fromSecondsValue === 0 &&
            (durationFromHoursElement.value !== '' || durationFromMinutesElement.value !== '' || durationFromSecondsElement.value !== '');

        var toAllZero = toHoursValue === 0 && toMinutesValue === 0 && toSecondsValue === 0 &&
            (durationToHoursElement.value !== '' || durationToMinutesElement.value !== '' || durationToSecondsElement.value !== '');

        if (fromAllZero || fromHoursValue > maxDurationValues.h || fromMinutesValue > 59 || fromSecondsValue > 59) {
            showFilterFieldError(durationFromHoursElement, durationErrorText);
            hasError = true;
        }

        if (!hasError && (toAllZero || toHoursValue > maxDurationValues.h || toMinutesValue > 59 || toSecondsValue > 59)) {
            showFilterFieldError(durationToHoursElement, durationErrorText);
            hasError = true;
        }

        if (!hasError && totalToSeconds > 0 && totalToSeconds < FILTER_LIMITS.durationMin) {
            showFilterFieldError(
                durationToHoursElement,
                'Длительность «до» должна быть не менее ' + formatDurationHuman(FILTER_LIMITS.durationMin)
            );
            hasError = true;
        }

        if (!hasError && totalFromSeconds > 0 && totalToSeconds > 0 && totalFromSeconds > totalToSeconds) {
            showFilterFieldError(
                document.getElementById('durationFromHours'),
                'Длительность «от» не может быть больше длительности «до»'
            );
            hasError = true;
        }

        if (hasError) return false;

        return true;
    }

    function stepFilterValue(inputElementId, deltaValue, minValue, maxValue, decimalPlaces) {
        var inputElement = document.getElementById(inputElementId);
        if (!inputElement) return;
        var currentValue = parseFloat(inputElement.value.replace(',', '.'));
        if (isNaN(currentValue)) currentValue = 0;
        var nextValue = currentValue + deltaValue;
        var decimalFactor = Math.pow(10, decimalPlaces);
        nextValue = Math.round(nextValue * decimalFactor) / decimalFactor;
        if (minValue !== null && nextValue < minValue) nextValue = minValue;
        if (maxValue !== null && nextValue > maxValue) nextValue = maxValue;
        inputElement.value = nextValue.toFixed(decimalPlaces);
        inputElement.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var filterSteppers = {};

    function startSteppingFilter(inputElementId, deltaValue, minValue, maxValue, decimalPlaces, buttonElement) {
        if (!filterSteppers[inputElementId]) {
            filterSteppers[inputElementId] = createStepper({
                onStep: function (stepDelta) {
                    stepFilterValue(inputElementId, stepDelta, minValue, maxValue, decimalPlaces);
                }
            });
        }
        filterSteppers[inputElementId].start(deltaValue, buttonElement);
    }

    function stopSteppingFilter(buttonElement) {
        Object.keys(filterSteppers).forEach(function (inputElementId) {
            filterSteppers[inputElementId].stop(buttonElement);
        });
        if (buttonElement) buttonElement.classList.remove('holding');
    }

    function setupSelectAllCheckbox(dropdownElementId, checkboxName) {
        var dropdownElement = document.getElementById(dropdownElementId);
        if (!dropdownElement) return;
        if (!dropdownElement.querySelector('input[name="' + checkboxName + '"]')) return;

        var existingSelectAllElement = dropdownElement.querySelector('.accordion-select-all');
        if (existingSelectAllElement) existingSelectAllElement.remove();

        var wrapperElement = document.createElement('label');
        wrapperElement.className = 'accordion-select-all';

        var selectAllCheckbox = document.createElement('input');
        selectAllCheckbox.type = 'checkbox';
        selectAllCheckbox.className = 'accordion-checkbox';

        var labelElement = document.createElement('span');
        labelElement.textContent = 'Выбрать всё';

        wrapperElement.appendChild(selectAllCheckbox);
        wrapperElement.appendChild(labelElement);

        function getVisibleItems() {
            return Array.prototype.slice.call(
                dropdownElement.querySelectorAll('.accordion-item:not(.is-hidden)')
            );
        }

        function getVisibleCheckboxes() {
            return getVisibleItems()
                .map(function (itemElement) {
                    return itemElement.querySelector('input[name="' + checkboxName + '"]');
                })
                .filter(Boolean);
        }

        function updateState() {
            var visibleCheckboxes = getVisibleCheckboxes();
            if (visibleCheckboxes.length === 0) {
                selectAllCheckbox.checked = false;
                selectAllCheckbox.indeterminate = false;
                return;
            }
            var checkedCount = visibleCheckboxes.filter(function (checkboxElement) {
                return checkboxElement.checked;
            }).length;
            var allChecked = checkedCount === visibleCheckboxes.length;
            selectAllCheckbox.checked = allChecked;
            selectAllCheckbox.indeterminate = false;
        }

        wrapperElement.addEventListener('click', function (clickEvent) {
            clickEvent.preventDefault();

            var visibleCheckboxes = getVisibleCheckboxes();
            if (visibleCheckboxes.length === 0) return;

            var checkedCount = visibleCheckboxes.filter(function (checkboxElement) {
                return checkboxElement.checked;
            }).length;
            var allChecked = checkedCount === visibleCheckboxes.length;

            visibleCheckboxes.forEach(function (checkboxElement) {
                checkboxElement.checked = !allChecked;
            });

            updateState();
        });

        dropdownElement.addEventListener('change', function (changeEvent) {
            if (changeEvent.target && changeEvent.target.name === checkboxName) {
                updateState();
            }
        });

        var accordionListElement = dropdownElement.querySelector('[data-accordion-list]');
        if (accordionListElement) {
            accordionListElement.insertBefore(wrapperElement, accordionListElement.firstChild);
        } else {
            dropdownElement.insertBefore(wrapperElement, dropdownElement.firstChild);
        }

        updateState();

        window.__selectAllState = window.__selectAllState || {};
        window.__selectAllState[dropdownElementId] = updateState;
    }

    function resetFiltersForm() {
        var filterFormElement = document.getElementById('filterForm');
        if (!filterFormElement) return;

        filterFormElement.querySelectorAll('input[type="text"]').forEach(function (inputElement) {
            inputElement.value = '';
            inputElement.style.borderColor = '';
            inputElement.style.boxShadow = '';
        });

        filterFormElement.querySelectorAll('input[type="date"]').forEach(function (inputElement) {
            inputElement.value = '';
            inputElement.style.borderColor = '';
            inputElement.style.boxShadow = '';
        });

        filterFormElement.querySelectorAll('input[type="checkbox"]').forEach(function (checkboxElement) {
            checkboxElement.checked = false;
            checkboxElement.indeterminate = false;
            checkboxElement.disabled = false;
        });

        var minDurationElement = document.getElementById('minDuration');
        var maxDurationElement = document.getElementById('maxDuration');
        if (minDurationElement) minDurationElement.value = '';
        if (maxDurationElement) maxDurationElement.value = '';

        clearFilterFieldErrors();
        closeAllAccordions();

        document.querySelectorAll('.filter-input-with-clear.has-value').forEach(function (element) {
            element.classList.remove('has-value');
        });

        document.querySelectorAll('.accordion-search.has-value').forEach(function (element) {
            element.classList.remove('has-value');
        });

        document.querySelectorAll('[data-accordion-search-input]').forEach(function (inputElement) {
            inputElement.value = '';
        });

        document.querySelectorAll('[data-accordion-list] .accordion-item.is-hidden').forEach(function (itemElement) {
            itemElement.classList.remove('is-hidden');
        });

        document.querySelectorAll('[data-accordion-empty]').forEach(function (emptyElement) {
            emptyElement.classList.remove('show');
        });

        if (window.__selectAllState) {
            Object.keys(window.__selectAllState).forEach(function (dropdownElementId) {
                try { window.__selectAllState[dropdownElementId](); } catch (stateError) {}
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
            enforceCheckboxLimit('directorsDropdown', MAX_SELECTED_PER_DROPDOWN, 'Режиссёры');
            enforceCheckboxLimit('genresDropdown', MAX_SELECTED_PER_DROPDOWN, 'Жанры');
        });
    }

    function attachFiltersAsHiddenInputsTo(targetForm) {
        if (!targetForm) return;
        targetForm.querySelectorAll('[data-from-filters="1"]').forEach(function (el) { el.remove(); });

        var filterFormElement = document.getElementById('filterForm');
        if (!filterFormElement) return;

        filterFormElement.querySelectorAll('input, select, textarea').forEach(function (sourceEl) {
            if (!sourceEl.name) return;

            if (sourceEl.type === 'checkbox' || sourceEl.type === 'radio') {
                if (!sourceEl.checked) return;
                var hiddenCheckbox = document.createElement('input');
                hiddenCheckbox.type = 'hidden';
                hiddenCheckbox.name = sourceEl.name;
                hiddenCheckbox.value = sourceEl.value;
                hiddenCheckbox.setAttribute('data-from-filters', '1');
                targetForm.appendChild(hiddenCheckbox);
                return;
            }

            var value = (sourceEl.value || '').trim();
            if (value === '') return;

            var hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = sourceEl.name;
            hiddenInput.value = value;
            hiddenInput.setAttribute('data-from-filters', '1');
            targetForm.appendChild(hiddenInput);
        });
    }

    function removeEmptyFormFields(targetForm) {
        if (!targetForm) return;
        targetForm.querySelectorAll('input, select, textarea').forEach(function (field) {
            if (!field.name) return;
            if (field.type === 'submit' || field.type === 'button' || field.type === 'reset') return;
            if (field.type === 'checkbox' || field.type === 'radio') {
                if (!field.checked) field.remove();
                return;
            }
            if ((field.value || '').trim() === '') {
                field.remove();
            }
        });
    }

    function isFilterMeaningful() {
        var filterFormElement = document.getElementById('filterForm');
        if (!filterFormElement) return false;

        var currentSortSelection = getCurrentSortSelection();
        if (currentSortSelection) return true;

        var priceFromInput = document.getElementById('priceFrom');
        var priceToInput = document.getElementById('priceTo');
        var ratingFromInput = document.getElementById('ratingFrom');
        var ratingToInput = document.getElementById('ratingTo');
        var releaseDateFromInput = document.getElementById('releaseDateFrom');
        var releaseDateToInput = document.getElementById('releaseDateTo');

        function parseNum(el) {
            if (!el) return null;
            var raw = (el.value || '').trim();
            if (raw === '') return null;
            var n = parseFloat(raw.replace(',', '.'));
            return isNaN(n) ? null : n;
        }

        function parseDate(el) {
            if (!el) return null;
            var raw = (el.value || '').trim();
            return raw === '' ? null : raw;
        }

        var priceFrom = parseNum(priceFromInput);
        var priceTo = parseNum(priceToInput);
        var ratingFrom = parseNum(ratingFromInput);
        var ratingTo = parseNum(ratingToInput);
        var releaseDateFrom = parseDate(releaseDateFromInput);
        var releaseDateTo = parseDate(releaseDateToInput);

        if (priceFrom !== null && priceFrom > FILTER_LIMITS.priceMin) return true;
        if (priceTo !== null && priceTo < FILTER_LIMITS.priceMax) return true;

        if (ratingFrom !== null && ratingFrom > 0.1) return true;
        if (ratingTo !== null && ratingTo < 10) return true;

        var fromHours = parseInt((document.getElementById('durationFromHours') || {}).value || '0', 10) || 0;
        var fromMinutes = parseInt((document.getElementById('durationFromMinutes') || {}).value || '0', 10) || 0;
        var fromSeconds = parseInt((document.getElementById('durationFromSeconds') || {}).value || '0', 10) || 0;
        var toHours = parseInt((document.getElementById('durationToHours') || {}).value || '0', 10) || 0;
        var toMinutes = parseInt((document.getElementById('durationToMinutes') || {}).value || '0', 10) || 0;
        var toSeconds = parseInt((document.getElementById('durationToSeconds') || {}).value || '0', 10) || 0;

        var fromTotalSeconds = fromHours * 3600 + fromMinutes * 60 + fromSeconds;
        var toTotalSeconds = toHours * 3600 + toMinutes * 60 + toSeconds;

        if (fromTotalSeconds > FILTER_LIMITS.durationMin) return true;
        if (toTotalSeconds > 0 && toTotalSeconds < FILTER_LIMITS.durationMax) return true;

        if (releaseDateFrom !== null && !FILTER_LIMITS.releaseDateMin) return true;
        if (releaseDateFrom !== null && FILTER_LIMITS.releaseDateMin && releaseDateFrom > FILTER_LIMITS.releaseDateMin) return true;
        if (releaseDateTo !== null && !FILTER_LIMITS.releaseDateMax) return true;
        if (releaseDateTo !== null && FILTER_LIMITS.releaseDateMax && releaseDateTo < FILTER_LIMITS.releaseDateMax) return true;

        var genresState = getCheckboxState('genres');
        if (genresState.checked > 0) return true;

        var directorsState = getCheckboxState('directors');
        if (directorsState.checked > 0) return true;

        return false;
    }

    function submitCombinedSearch() {
        var headerForm = document.getElementById('headerSearchForm');
        if (!headerForm) return;

        if (typeof validateFilter === 'function' && !validateFilter()) return;

        var headerInput = document.getElementById('headerSearchInput');
        var trimmedTitle = headerInput ? headerInput.value.trim() : '';
        var cleanTitle = trimmedTitle.replace(/…+$/, '').trim();

        var hasTitle = cleanTitle !== '';
        var hasMeaningfulFilters = isFilterMeaningful();

        if (!hasTitle && !hasMeaningfulFilters) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Введите запрос или задайте значимые фильтры', 'warning');
            }
            return;
        }

        if (headerInput) {
            headerInput.removeAttribute('name');
            var hiddenTitle = headerForm.querySelector('input[type="hidden"][name="title"]');

            if (hasTitle) {
                if (!hiddenTitle) {
                    hiddenTitle = document.createElement('input');
                    hiddenTitle.type = 'hidden';
                    hiddenTitle.name = 'title';
                    headerForm.appendChild(hiddenTitle);
                }
                hiddenTitle.value = cleanTitle;
            } else if (hiddenTitle) {
                hiddenTitle.remove();
            }
        }

        attachFiltersAsHiddenInputsTo(headerForm);
        removeEmptyFormFields(headerForm);

        headerForm.removeAttribute('onsubmit');
        headerForm.submit();
    }

    function applyFiltersFromModal() {
        if (typeof validateFilter === 'function' && !validateFilter()) return;

        if (!isFilterMeaningful()) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Выбранные фильтры не сужают выборку. Задайте значимые параметры.', 'warning');
            }
            return;
        }

        toggleFiltersModal(false);

        var headerForm = document.getElementById('headerSearchForm');
        if (headerForm) {
            var headerInput = document.getElementById('headerSearchInput');
            if (headerInput) {
                var trimmedTitle = headerInput.value.trim();
                var cleanTitle = trimmedTitle.replace(/…+$/, '').trim();

                headerInput.removeAttribute('name');

                var hiddenTitle = headerForm.querySelector('input[type="hidden"][name="title"]');

                if (cleanTitle !== '') {
                    if (!hiddenTitle) {
                        hiddenTitle = document.createElement('input');
                        hiddenTitle.type = 'hidden';
                        hiddenTitle.name = 'title';
                        headerForm.appendChild(hiddenTitle);
                    }
                    hiddenTitle.value = cleanTitle;
                } else if (hiddenTitle) {
                    hiddenTitle.remove();
                }
            }

            attachFiltersAsHiddenInputsTo(headerForm);
            removeEmptyFormFields(headerForm);
            headerForm.removeAttribute('onsubmit');
            headerForm.submit();
            return;
        }

        var filterForm = document.getElementById('filterForm');
        if (filterForm) {
            filterForm.removeAttribute('onsubmit');
            filterForm.submit();
        }
    }

    function getFocusableFilterFields() {
        var filterFormElement = document.getElementById('filterForm');
        if (!filterFormElement) return [];
        return Array.prototype.slice.call(
            filterFormElement.querySelectorAll(
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
        var currentIndex = fields.indexOf(currentEl);
        var nextIndex = currentIndex === -1 ? 0 : (currentIndex + 1) % fields.length;
        var nextEl = fields[nextIndex];
        if (nextEl) {
            try { nextEl.focus(); } catch (focusError) {}
            if (nextEl.tagName === 'INPUT' && (nextEl.type === 'text' || nextEl.type === 'date')) {
                try { nextEl.select(); } catch (selectError) {}
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
        var filtersModalOverlay = document.getElementById('filtersModalOverlay');
        if (!filtersModalOverlay) return;
        if (filtersModalOverlay.dataset.enterBound === '1') return;
        filtersModalOverlay.dataset.enterBound = '1';

        filtersModalOverlay.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') return;
            if (e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) return;

            var active = document.activeElement;
            var insideModal = filtersModalOverlay.contains(active);

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

        var searchParts = searchString.split('&');
        var sortByValue = null;
        var sortDirectionValue = null;

        for (var partIndex = 0; partIndex < searchParts.length; partIndex++) {
            var keyValuePair = searchParts[partIndex].split('=');
            var parameterName = decodeURIComponent(keyValuePair[0] || '');
            var parameterValue = decodeURIComponent(keyValuePair[1] || '');
            if (parameterName === 'sortBy') sortByValue = parameterValue;
            if (parameterName === 'sortDirection') sortDirectionValue = parameterValue.toUpperCase();
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
        var sortButtons = document.querySelectorAll('.sort-filter-btn');
        for (var buttonIndex = 0; buttonIndex < sortButtons.length; buttonIndex++) {
            var sortButton = sortButtons[buttonIndex];
            var sortKey = sortButton.getAttribute('data-sort-key');
            var sortDirection = sortButton.getAttribute('data-sort-dir').toUpperCase();
            if (sortState && sortState.key === sortKey && sortState.dir.toUpperCase() === sortDirection) {
                sortButton.classList.add('active');
            } else {
                sortButton.classList.remove('active');
            }
        }
    }

    function syncHiddenSortFields(sortState) {
        var sortByHiddenField = getHiddenSortField('sortBy');
        var sortDirectionHiddenField = getHiddenSortField('sortDirection');
        if (sortByHiddenField) sortByHiddenField.value = sortState ? sortState.key : '';
        if (sortDirectionHiddenField) sortDirectionHiddenField.value = sortState ? sortState.dir.toUpperCase() : '';
    }

    function getCurrentSortSelection() {
        var sortByHiddenField = getHiddenSortField('sortBy');
        var sortDirectionHiddenField = getHiddenSortField('sortDirection');
        if (!sortByHiddenField || !sortDirectionHiddenField) return null;
        var sortKey = (sortByHiddenField.value || '').trim();
        var sortDirection = (sortDirectionHiddenField.value || '').trim().toUpperCase();
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

        document.addEventListener('click', function (clickEvent) {
            var targetElement = clickEvent.target;
            while (targetElement && targetElement !== document) {
                if (targetElement.classList && targetElement.classList.contains('sort-filter-btn')) break;
                targetElement = targetElement.parentNode;
            }
            if (!targetElement || targetElement === document) return;

            clickEvent.preventDefault();

            var sortKey = targetElement.getAttribute('data-sort-key');
            var sortDirection = targetElement.getAttribute('data-sort-dir').toUpperCase();
            if (!sortKey || !sortDirection) return;

            var currentSortSelection = getCurrentSortSelection();
            var isSameSelection = currentSortSelection
                && currentSortSelection.key === sortKey
                && currentSortSelection.dir === sortDirection;

            if (isSameSelection) {
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

    function initFiltersOpenButton() {
        var filtersBtn = document.getElementById('headerFiltersBtn');
        if (!filtersBtn) return;
        if (filtersBtn.dataset.filtersOpenBound === '1') return;
        filtersBtn.dataset.filtersOpenBound = '1';
        filtersBtn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            toggleFiltersModal();
        });
    }

    function initAll() {
        try { initFiltersOpenButton(); } catch (openInitError) { console.error('[filters] open button init failed', openInitError); }
        try { initFilterSort(); } catch (sortInitError) { console.error('[filters] sort init failed', sortInitError); }
        try { initFilterMasks(); } catch (masksInitError) { console.error('[filters] masks init failed', masksInitError); }
        try { initFilterAccordions(); } catch (accordionsInitError) { console.error('[filters] accordions init failed', accordionsInitError); }
        try { initFilterClearButtons(); } catch (clearButtonsInitError) { console.error('[filters] clear buttons init failed', clearButtonsInitError); }
        try { initFiltersApplyButton(); } catch (applyInitError) { console.error('[filters] apply init failed', applyInitError); }
        try { initOverlayAndClose(); } catch (overlayInitError) { console.error('[filters] overlay init failed', overlayInitError); }
        try { initStepperButtons(); } catch (stepperInitError) { console.error('[filters] stepper init failed', stepperInitError); }
        try { initFilterEnterKeyHandler(); } catch (enterInitError) { console.error('[filters] enter init failed', enterInitError); }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll, { once: true });
    } else {
        initAll();
    }

    window.validateSearchQuery = validateSearchQuery;
    window.toggleFiltersModal = toggleFiltersModal;
    window.handleOverlayClick = handleOverlayClick;
    window.calculateRangeMinutes = calculateRangeMinutes;
    window.normalizeDurationFilters = normalizeDurationFilters;
    window.validateFilter = validateFilter;
    window.stepFilterValue = stepFilterValue;
    window.startSteppingFilter = startSteppingFilter;
    window.stopSteppingFilter = stopSteppingFilter;
    window.setupSelectAllCheckbox = setupSelectAllCheckbox;
    window.resetFiltersForm = resetFiltersForm;
    window.applyFiltersFromModal = applyFiltersFromModal;
    window.attachFiltersAsHiddenInputsTo = attachFiltersAsHiddenInputsTo;
    window.removeEmptyFormFields = removeEmptyFormFields;
    window.isFilterMeaningful = isFilterMeaningful;
    window.submitCombinedSearch = submitCombinedSearch;
    window.focusNextFilterField = focusNextFilterField;
    window.initFilterMasks = initFilterMasks;
})();