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

    function stripEmptyFields(formElement) {
        formElement.querySelectorAll('input, select, textarea').forEach(function (fieldElement) {
            if (fieldElement.type === 'checkbox' || fieldElement.type === 'radio') {
                fieldElement.disabled = false;
                return;
            }
            var trimmedValue = (fieldElement.value || '').trim();
            fieldElement.disabled = trimmedValue === '';
        });

        disableFullyCheckedCheckboxGroups(formElement);
    }

    function disableFullyCheckedCheckboxGroups(formElement) {
        var checkboxGroups = {};

        formElement.querySelectorAll('input[type="checkbox"][name]').forEach(function (checkboxElement) {
            var checkboxName = checkboxElement.name;
            if (!checkboxGroups[checkboxName]) {
                checkboxGroups[checkboxName] = [];
            }
            checkboxGroups[checkboxName].push(checkboxElement);
        });

        Object.keys(checkboxGroups).forEach(function (checkboxName) {
            var groupCheckboxes = checkboxGroups[checkboxName];
            var checkedCount = groupCheckboxes.filter(function (checkboxElement) {
                return checkboxElement.checked;
            }).length;

            var allChecked = groupCheckboxes.length > 0 && checkedCount === groupCheckboxes.length;
            var noneChecked = checkedCount === 0;

            if (allChecked || noneChecked) {
                groupCheckboxes.forEach(function (checkboxElement) {
                    checkboxElement.disabled = true;
                });
                return;
            }

            groupCheckboxes.forEach(function (checkboxElement) {
                checkboxElement.disabled = !checkboxElement.checked;
            });
        });
    }

    function restoreFields(formElement) {
        formElement.querySelectorAll('input, select, textarea').forEach(function (fieldElement) {
            fieldElement.disabled = false;
        });
    }

    function enforceCheckboxLimit(dropdownElementId, maxSelectedCount, labelText) {
        var dropdownElement = document.getElementById(dropdownElementId);
        if (!dropdownElement) return;

        dropdownElement.addEventListener('change', function (changeEvent) {
            var targetElement = changeEvent.target;
            if (!targetElement || targetElement.type !== 'checkbox') return;
            if (targetElement.name !== 'directors' && targetElement.name !== 'genres') return;

            var allCheckboxes = dropdownElement.querySelectorAll(
                'input[name="' + targetElement.name + '"]'
            );
            var checkedCheckboxes = dropdownElement.querySelectorAll(
                'input[name="' + targetElement.name + '"]:checked'
            );

            var allChecked = allCheckboxes.length > 0 && checkedCheckboxes.length === allCheckboxes.length;
            if (allChecked) {
                return;
            }

            if (checkedCheckboxes.length > maxSelectedCount) {
                targetElement.checked = false;
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage(
                        'Можно выбрать не более ' + maxSelectedCount + ' значений в «' + labelText + '»',
                        'warning'
                    );
                }
            }
        });
    }

    function validateSearchQuery() {
        var headerSearchInput = document.getElementById('headerSearchInput');
        if (!headerSearchInput) return true;
        var trimmedQuery = headerSearchInput.value.trim();
        if (trimmedQuery.length === 0) {
            headerSearchInput.focus();
            return false;
        }
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

    function fillAccordionList(dropdownElement, checkboxName, itemsList, displayFunction) {
        if (!dropdownElement) return;
        var accordionListElement = dropdownElement.querySelector('[data-accordion-list]');
        var accordionEmptyElement = dropdownElement.querySelector('[data-accordion-empty]');
        if (!accordionListElement) return;

        accordionListElement.innerHTML = '';
        (itemsList || []).forEach(function (itemValue) {
            var displayValue = displayFunction ? displayFunction(itemValue) : itemValue;
            accordionListElement.appendChild(buildAccordionCheckbox(checkboxName, itemValue, displayValue));
        });

        if (accordionEmptyElement) accordionEmptyElement.classList.remove('show');

        var selectedAttributeValue = dropdownElement.getAttribute('data-selected');
        if (selectedAttributeValue && selectedAttributeValue.trim() !== '') {
            var selectedValues = selectedAttributeValue.split(',').map(function (selectedValue) {
                return selectedValue.trim();
            });
            accordionListElement.querySelectorAll('input[name="' + checkboxName + '"]').forEach(function (checkboxElement) {
                checkboxElement.checked = selectedValues.indexOf(checkboxElement.value) !== -1;
            });
        }

        if (window.FiltersSearch) {
            window.FiltersSearch.init(dropdownElement.closest('[data-accordion]') || dropdownElement);
        }
    }

    function loadFilterOptions() {
        var directorsDropdownElement = document.getElementById('directorsDropdown');
        var genresDropdownElement = document.getElementById('genresDropdown');

        var requestsList = [];

        if (directorsDropdownElement) {
            requestsList.push(
                fetch('/filters/directors', { headers: AJAX_HEADERS })
                    .then(function (response) { return response.ok ? response.json() : []; })
                    .then(function (directorsList) {
                        fillAccordionList(
                            directorsDropdownElement,
                            'directors',
                            sortDirectorsBySurname(directorsList || []),
                            displayDirectorName
                        );
                    })
                    .catch(function () {
                        fillAccordionList(directorsDropdownElement, 'directors', [], displayDirectorName);
                    })
            );
        }

        if (genresDropdownElement) {
            requestsList.push(
                fetch('/filters/genres', { headers: AJAX_HEADERS })
                    .then(function (response) { return response.ok ? response.json() : []; })
                    .then(function (genresList) {
                        fillAccordionList(
                            genresDropdownElement,
                            'genres',
                            sortGenresAlphabetically(genresList || [])
                        );
                    })
                    .catch(function () { fillAccordionList(genresDropdownElement, 'genres', []); })
            );
        }

        requestsList.push(
            fetch('/filters/min-max/price', { headers: AJAX_HEADERS })
                .then(function (response) { return response.ok ? response.json() : null; })
                .then(function (rangeDto) {
                    if (!rangeDto) return;
                    if (rangeDto.min != null) FILTER_LIMITS.priceMin = Number(rangeDto.min);
                    if (rangeDto.max != null) FILTER_LIMITS.priceMax = Number(rangeDto.max);
                    applyPriceLimitsToUi();
                })
                .catch(function () {})
        );

        requestsList.push(
            fetch('/filters/min-max/duration', { headers: AJAX_HEADERS })
                .then(function (response) { return response.ok ? response.json() : null; })
                .then(function (rangeDto) {
                    if (!rangeDto) return;
                    if (rangeDto.min != null) FILTER_LIMITS.durationMin = Number(rangeDto.min);
                    if (rangeDto.max != null) FILTER_LIMITS.durationMax = Number(rangeDto.max);
                    applyDurationLimitsToUi();
                })
                .catch(function () {})
        );

        requestsList.push(
            fetch('/filters/min-max/release-date', { headers: AJAX_HEADERS })
                .then(function (response) { return response.ok ? response.json() : null; })
                .then(function (rangeDto) {
                    if (!rangeDto) return;
                    if (rangeDto.min != null) FILTER_LIMITS.releaseDateMin = String(rangeDto.min);
                    if (rangeDto.max != null) FILTER_LIMITS.releaseDateMax = String(rangeDto.max);
                    applyReleaseDateLimitsToUi();
                })
                .catch(function () {})
        );

        return Promise.all(requestsList);
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
        document.querySelectorAll('.filter-field-error').forEach(function (errorElement) {
            errorElement.remove();
        });
        document.querySelectorAll('.filter-input, .accordion-trigger').forEach(function (inputElement) {
            inputElement.style.borderColor = '';
            inputElement.style.boxShadow = '';
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
        if (groupElement) {
            var errorElement = document.createElement('div');
            errorElement.className = 'filter-field-error';
            errorElement.textContent = errorMessage;
            errorElement.style.color = '#f87171';
            errorElement.style.fontSize = '0.78rem';
            errorElement.style.marginTop = '4px';
            errorElement.style.lineHeight = '1.3';
            groupElement.appendChild(errorElement);
        }
        inputElement.style.borderColor = '#f87171';
        inputElement.style.boxShadow = '0 0 0 3px rgba(248, 113, 113, 0.3)';
        try { inputElement.focus(); } catch (focusError) {}
    }

    function getCheckboxState(checkboxName) {
        var allCheckboxes = document.querySelectorAll('input[name="' + checkboxName + '"]');
        var checkedCheckboxes = document.querySelectorAll('input[name="' + checkboxName + '"]:checked');
        return { total: allCheckboxes.length, checked: checkedCheckboxes.length };
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
        var headerSearchInput = document.getElementById('headerSearchInput');

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

        var dateRangeValidationResult = Validators.dateRange(releaseDateFromValue, releaseDateToValue);
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
            'Минимальная длительность — ' + FILTER_LIMITS.durationMin + ' сек., ' +
            'максимальная — ' + maxDurationValues.h + ' ч. ' + maxDurationValues.m + ' мин. ' + maxDurationValues.s + ' сек.';

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
            showFilterFieldError(durationToHoursElement, 'Длительность «до» должна быть не менее ' + FILTER_LIMITS.durationMin + ' сек.');
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

        var genresState = getCheckboxState('genres');
        var directorsState = getCheckboxState('directors');

        var genresMeaningful = genresState.checked > 0 && genresState.checked < genresState.total;
        var directorsMeaningful = directorsState.checked > 0 && directorsState.checked < directorsState.total;

        var isWideFilter = true;

        if (parsedPriceFrom !== null && parsedPriceFrom > FILTER_LIMITS.priceMin) isWideFilter = false;
        if (parsedPriceTo !== null && parsedPriceTo < FILTER_LIMITS.priceMax) isWideFilter = false;
        if (parsedRatingFrom !== null && parsedRatingFrom > 0.1) isWideFilter = false;
        if (parsedRatingTo !== null && parsedRatingTo < 10) isWideFilter = false;

        if (totalFromSeconds >= FILTER_LIMITS.durationMin) isWideFilter = false;
        if (totalToSeconds > 0) isWideFilter = false;
        if (genresMeaningful) isWideFilter = false;
        if (directorsMeaningful) isWideFilter = false;
        if (headerSearchInput && headerSearchInput.value.trim() !== '') isWideFilter = false;
        if (releaseDateFromValue) isWideFilter = false;
        if (releaseDateToValue) isWideFilter = false;

        var sortByHiddenField = getHiddenSortField('sortBy');
        if (sortByHiddenField && sortByHiddenField.value.trim() !== '') isWideFilter = false;

        if (isWideFilter) {
            toggleFiltersModal(false);
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Укажите хотя бы один фильтр', 'warning');
            }
            return false;
        }

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

        loadFilterOptions().then(function () {
            setupSelectAllCheckbox('directorsDropdown', 'directors');
            setupSelectAllCheckbox('genresDropdown', 'genres');
            enforceCheckboxLimit('directorsDropdown', MAX_SELECTED_PER_DROPDOWN, 'Режиссёры');
            enforceCheckboxLimit('genresDropdown', MAX_SELECTED_PER_DROPDOWN, 'Жанры');
        });
    }

    function initFormSubmit() {
        var filterFormElement = document.getElementById('filterForm');
        if (!filterFormElement) return;

        filterFormElement.addEventListener('submit', function (submitEvent) {
            if (typeof validateFilter === 'function' && !validateFilter()) {
                submitEvent.preventDefault();
                return;
            }
            stripEmptyFields(filterFormElement);
        });

        window.addEventListener('pageshow', function (pageShowEvent) {
            if (pageShowEvent.persisted) restoreFields(filterFormElement);
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

    function initAll() {
        try { initFilterSort(); } catch (sortInitError) { console.error('[filters] sort init failed', sortInitError); }
        try { initFilterMasks(); } catch (masksInitError) { console.error('[filters] masks init failed', masksInitError); }
        try { initFilterAccordions(); } catch (accordionsInitError) { console.error('[filters] accordions init failed', accordionsInitError); }
        try { initFilterClearButtons(); } catch (clearButtonsInitError) { console.error('[filters] clear buttons init failed', clearButtonsInitError); }
        try { initFormSubmit(); } catch (formSubmitInitError) { console.error('[filters] form submit init failed', formSubmitInitError); }
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
    window.stripEmptyFields = stripEmptyFields;
    window.restoreFields = restoreFields;
})();