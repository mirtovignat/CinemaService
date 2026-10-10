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

    function formatPriceValue(v) {
        var n = Number(v);
        if (isNaN(n)) return '';
        return n.toFixed(2);
    }

    function splitDuration(totalSeconds) {
        totalSeconds = Math.max(0, Math.floor(Number(totalSeconds) || 0));
        return {
            h: Math.floor(totalSeconds / 3600),
            m: Math.floor((totalSeconds % 3600) / 60),
            s: totalSeconds % 60
        };
    }

    function applyPriceLimitsToUi() {
        var priceFrom = document.getElementById('priceFrom');
        var priceTo = document.getElementById('priceTo');

        if (priceFrom) {
            priceFrom.setAttribute('placeholder', 'От ' + formatPriceValue(FILTER_LIMITS.priceMin) + ' ₽');
        }
        if (priceTo) {
            priceTo.setAttribute('placeholder', 'До ' + formatPriceValue(FILTER_LIMITS.priceMax) + ' ₽');
        }

        document.querySelectorAll(
            '.stepper-btn[data-stepper-target="priceFrom"], .stepper-btn[data-stepper-target="priceTo"]'
        ).forEach(function (btn) {
            btn.setAttribute('data-stepper-min', String(FILTER_LIMITS.priceMin));
            btn.setAttribute('data-stepper-max', String(FILTER_LIMITS.priceMax));
        });
    }

    function applyDurationLimitsToUi() {
        var max = splitDuration(FILTER_LIMITS.durationMax);
        var map = {
            durationFromHours: max.h,
            durationFromMinutes: 59,
            durationFromSeconds: 59,
            durationToHours: max.h,
            durationToMinutes: 59,
            durationToSeconds: 59
        };
        Object.keys(map).forEach(function (id) {
            var el = document.getElementById(id);
            if (el) el.setAttribute('data-max', String(map[id]));
        });
    }

    function applyReleaseDateLimitsToUi() {
        var dateFrom = document.getElementById('releaseDateFrom');
        var dateTo = document.getElementById('releaseDateTo');

        if (dateFrom) {
            if (FILTER_LIMITS.releaseDateMin) dateFrom.setAttribute('min', FILTER_LIMITS.releaseDateMin);
            if (FILTER_LIMITS.releaseDateMax) dateFrom.setAttribute('max', FILTER_LIMITS.releaseDateMax);
        }
        if (dateTo) {
            if (FILTER_LIMITS.releaseDateMin) dateTo.setAttribute('min', FILTER_LIMITS.releaseDateMin);
            if (FILTER_LIMITS.releaseDateMax) dateTo.setAttribute('max', FILTER_LIMITS.releaseDateMax);
        }
    }

    function stripEmptyFields(form) {
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.type === 'checkbox' || el.type === 'radio') {
                el.disabled = !el.checked;
                return;
            }
            var value = (el.value || '').trim();
            el.disabled = value === '';
        });
    }

    function restoreFields(form) {
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
            el.disabled = false;
        });
    }

    function enforceCheckboxLimit(dropdownId, maxSelected, label) {
        var dropdown = document.getElementById(dropdownId);
        if (!dropdown) return;

        dropdown.addEventListener('change', function (e) {
            var target = e.target;
            if (!target || target.type !== 'checkbox') return;
            if (target.name !== 'directors' && target.name !== 'genres') return;

            var checked = dropdown.querySelectorAll(
                'input[name="' + target.name + '"]:checked'
            );
            if (checked.length > maxSelected) {
                target.checked = false;
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage(
                        'Можно выбрать не более ' + maxSelected + ' значений в «' + label + '»',
                        'warning'
                    );
                }
            }
        });
    }

    function validateSearchQuery() {
        var input = document.getElementById('headerSearchInput');
        if (!input) return true;
        var query = input.value.trim();
        if (query.length === 0) {
            input.focus();
            return false;
        }
        if (query.length > 100) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Слишком длинный запрос (максимум 100 символов)', 'warning');
            }
            input.focus();
            return false;
        }
        return true;
    }

    function closeAllAccordions() {
        document.querySelectorAll('.accordion-group.active').forEach(function (g) {
            g.classList.remove('active');
        });
        document.querySelectorAll('.accordion-dropdown.active').forEach(function (dd) {
            dd.classList.remove('active');
        });
    }

    function toggleFiltersModal(show) {
        var overlay = document.getElementById('filtersModalOverlay');
        if (!overlay) return;

        if (typeof show === 'boolean') {
            if (show) {
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

    function handleOverlayClick(e) {
        if (e.target && e.target.id === 'filtersModalOverlay') {
            toggleFiltersModal(false);
        }
    }

    function initFilterAccordions() {
        document.querySelectorAll('.accordion-trigger').forEach(function (trigger) {
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
        document.querySelectorAll('.filter-clear-btn').forEach(function (btn) {
            var targetId = btn.getAttribute('data-clear-target');
            if (!targetId) return;
            var input = document.getElementById(targetId);
            if (!input) return;
            var wrapper = btn.closest('.filter-input-with-clear');

            function sync() {
                if (!wrapper) return;
                wrapper.classList.toggle('has-value', input.value !== '');
            }

            btn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                sync();
                input.focus();
            });

            input.addEventListener('input', sync);
            input.addEventListener('change', sync);
            sync();
        });
    }

    function loadFilterOptions() {
        var directorsDropdown = document.getElementById('directorsDropdown');
        var genresDropdown = document.getElementById('genresDropdown');

        function buildCheckbox(name, value) {
            var label = document.createElement('label');
            label.className = 'accordion-item';
            var input = document.createElement('input');
            input.type = 'checkbox';
            input.name = name;
            input.className = 'accordion-checkbox';
            input.value = value;
            var span = document.createElement('span');
            span.textContent = value;
            label.appendChild(input);
            label.appendChild(span);
            return label;
        }

        function fill(dropdown, name, items) {
            if (!dropdown) return;
            dropdown.innerHTML = '';
            items.forEach(function (item) {
                dropdown.appendChild(buildCheckbox(name, item));
            });

            var selectedAttr = dropdown.getAttribute('data-selected');
            if (selectedAttr && selectedAttr.trim() !== '') {
                var selected = selectedAttr.split(',').map(function (s) { return s.trim(); });
                dropdown.querySelectorAll('input[name="' + name + '"]').forEach(function (cb) {
                    cb.checked = selected.indexOf(cb.value) !== -1;
                });
            }
        }

        var requests = [];

        if (directorsDropdown) {
            requests.push(
                fetch('/filters/directors', { headers: { 'Accept': 'application/json' } })
                    .then(function (r) { return r.ok ? r.json() : []; })
                    .then(function (list) { fill(directorsDropdown, 'directors', list || []); })
                    .catch(function () { fill(directorsDropdown, 'directors', []); })
            );
        }

        if (genresDropdown) {
            requests.push(
                fetch('/filters/genres', { headers: { 'Accept': 'application/json' } })
                    .then(function (r) { return r.ok ? r.json() : []; })
                    .then(function (list) { fill(genresDropdown, 'genres', list || []); })
                    .catch(function () { fill(genresDropdown, 'genres', []); })
            );
        }

        requests.push(
            fetch('/filters/min-max/price', { headers: { 'Accept': 'application/json' } })
                .then(function (r) { return r.ok ? r.json() : null; })
                .then(function (dto) {
                    if (!dto) return;
                    if (dto.min != null) FILTER_LIMITS.priceMin = Number(dto.min);
                    if (dto.max != null) FILTER_LIMITS.priceMax = Number(dto.max);
                    applyPriceLimitsToUi();
                })
                .catch(function () {})
        );

        requests.push(
            fetch('/filters/min-max/duration', { headers: { 'Accept': 'application/json' } })
                .then(function (r) { return r.ok ? r.json() : null; })
                .then(function (dto) {
                    if (!dto) return;
                    if (dto.min != null) FILTER_LIMITS.durationMin = Number(dto.min);
                    if (dto.max != null) FILTER_LIMITS.durationMax = Number(dto.max);
                    applyDurationLimitsToUi();
                })
                .catch(function () {})
        );

        requests.push(
            fetch('/filters/min-max/release-date', { headers: { 'Accept': 'application/json' } })
                .then(function (r) { return r.ok ? r.json() : null; })
                .then(function (dto) {
                    if (!dto) return;
                    if (dto.min != null) FILTER_LIMITS.releaseDateMin = String(dto.min);
                    if (dto.max != null) FILTER_LIMITS.releaseDateMax = String(dto.max);
                    applyReleaseDateLimitsToUi();
                })
                .catch(function () {})
        );

        return Promise.all(requests);
    }

    function calculateRangeMinutes() {
        var getVal = function (id) {
            var el = document.getElementById(id);
            return el ? (parseInt(el.value, 10) || 0) : 0;
        };

        var fromH = getVal('durationFromHours');
        var fromM = getVal('durationFromMinutes');
        var fromS = getVal('durationFromSeconds');
        var toH = getVal('durationToHours');
        var toM = getVal('durationToMinutes');
        var toS = getVal('durationToSeconds');

        var totalFromSeconds = (fromH * 3600) + (fromM * 60) + fromS;
        var totalToSeconds = (toH * 3600) + (toM * 60) + toS;

        var minEl = document.getElementById('minDuration');
        var maxEl = document.getElementById('maxDuration');

        if (minEl) minEl.value = totalFromSeconds > 0 ? totalFromSeconds : '';
        if (maxEl) maxEl.value = totalToSeconds > 0 ? totalToSeconds : '';
    }

    function normalizeDurationFilters() {
        calculateRangeMinutes();
    }

    function clearFilterFieldErrors() {
        document.querySelectorAll('.filter-field-error').forEach(function (el) {
            el.remove();
        });
        document.querySelectorAll('.filter-input, .accordion-trigger').forEach(function (el) {
            el.style.borderColor = '';
            el.style.boxShadow = '';
        });
        var globalErr = document.getElementById('ratingFilterError');
        if (globalErr) {
            globalErr.style.display = 'none';
            globalErr.innerText = '';
        }
    }

    function showFilterFieldError(input, message) {
        if (!input) return;
        var group = input.closest('.filter-group') || input.parentNode;
        if (group) {
            var err = document.createElement('div');
            err.className = 'filter-field-error';
            err.textContent = message;
            err.style.color = '#f87171';
            err.style.fontSize = '0.78rem';
            err.style.marginTop = '4px';
            err.style.lineHeight = '1.3';
            group.appendChild(err);
        }
        input.style.borderColor = '#f87171';
        input.style.boxShadow = '0 0 0 3px rgba(248, 113, 113, 0.3)';
        try { input.focus(); } catch (e) {}
    }

    function getCheckboxState(name) {
        var all = document.querySelectorAll('input[name="' + name + '"]');
        var checked = document.querySelectorAll('input[name="' + name + '"]:checked');
        return { total: all.length, checked: checked.length };
    }

    function validateFilter() {
        clearFilterFieldErrors();
        normalizeDurationFilters();

        var hasError = false;

        var priceFrom = document.getElementById('priceFrom');
        var priceTo = document.getElementById('priceTo');
        var ratingFrom = document.getElementById('ratingFrom');
        var ratingTo = document.getElementById('ratingTo');
        var dateFrom = document.getElementById('releaseDateFrom');
        var dateTo = document.getElementById('releaseDateTo');
        var titleInput = document.getElementById('headerSearchInput');

        var fromH = parseInt((document.getElementById('durationFromHours') || {}).value || '0', 10);
        var fromM = parseInt((document.getElementById('durationFromMinutes') || {}).value || '0', 10);
        var fromS = parseInt((document.getElementById('durationFromSeconds') || {}).value || '0', 10);
        var toH = parseInt((document.getElementById('durationToHours') || {}).value || '0', 10);
        var toM = parseInt((document.getElementById('durationToMinutes') || {}).value || '0', 10);
        var toS = parseInt((document.getElementById('durationToSeconds') || {}).value || '0', 10);
        var fromTotal = fromH * 3600 + fromM * 60 + fromS;
        var toTotal = toH * 3600 + toM * 60 + toS;

        var priceErrorText =
            'Минимальная цена — ' + formatPriceValue(FILTER_LIMITS.priceMin) +
            ' ₽, максимальная — ' + formatPriceValue(FILTER_LIMITS.priceMax) + ' ₽';

        var pfVal = null;
        var ptVal = null;

        if (priceFrom && priceFrom.value.trim() !== '') {
            pfVal = parseFloat(priceFrom.value.replace(',', '.'));
            if (isNaN(pfVal) || pfVal < FILTER_LIMITS.priceMin || pfVal > FILTER_LIMITS.priceMax) {
                showFilterFieldError(priceFrom, priceErrorText);
                hasError = true;
            } else {
                priceFrom.value = pfVal.toFixed(2);
            }
        }

        if (!hasError && priceTo && priceTo.value.trim() !== '') {
            ptVal = parseFloat(priceTo.value.replace(',', '.'));
            if (isNaN(ptVal) || ptVal < FILTER_LIMITS.priceMin || ptVal > FILTER_LIMITS.priceMax) {
                showFilterFieldError(priceTo, priceErrorText);
                hasError = true;
            } else {
                priceTo.value = ptVal.toFixed(2);
            }
        }

        if (!hasError && pfVal !== null && ptVal !== null && pfVal > ptVal) {
            showFilterFieldError(priceFrom, 'Цена «от» не может быть больше цены «до»');
            hasError = true;
        }

        var rfVal = null;
        var rtVal = null;

        if (ratingFrom && ratingFrom.value.trim() !== '') {
            rfVal = parseFloat(ratingFrom.value.replace(',', '.'));
            if (isNaN(rfVal) || rfVal < 0.1 || rfVal > 10) {
                showFilterFieldError(ratingFrom, 'Рейтинг «от» должен быть от 0.1 до 10');
                hasError = true;
            } else {
                ratingFrom.value = rfVal.toFixed(1);
            }
        }

        if (!hasError && ratingTo && ratingTo.value.trim() !== '') {
            rtVal = parseFloat(ratingTo.value.replace(',', '.'));
            if (isNaN(rtVal) || rtVal < 0.1 || rtVal > 10) {
                showFilterFieldError(ratingTo, 'Рейтинг «до» должен быть от 0.1 до 10');
                hasError = true;
            } else {
                ratingTo.value = rtVal.toFixed(1);
            }
        }

        if (!hasError && rfVal !== null && rtVal !== null && rfVal > rtVal) {
            showFilterFieldError(ratingFrom, 'Рейтинг «от» не может быть больше рейтинга «до»');
            hasError = true;
        }

        var fromValue = dateFrom ? dateFrom.value : '';
        var toValue = dateTo ? dateTo.value : '';

        var dateRangeResult = Validators.dateRange(fromValue, toValue);
        if (!dateRangeResult.ok) {
            var targetInput = dateRangeResult.field === 'to' ? dateTo : dateFrom;
            showFilterFieldError(targetInput, dateRangeResult.error);
            hasError = true;
        } else {
            if (fromValue && FILTER_LIMITS.releaseDateMin && fromValue < FILTER_LIMITS.releaseDateMin) {
                showFilterFieldError(dateFrom, 'Дата «от» не может быть раньше ' + FILTER_LIMITS.releaseDateMin);
                hasError = true;
            }
            if (!hasError && fromValue && FILTER_LIMITS.releaseDateMax && fromValue > FILTER_LIMITS.releaseDateMax) {
                showFilterFieldError(dateFrom, 'Дата «от» не может быть позже ' + FILTER_LIMITS.releaseDateMax);
                hasError = true;
            }
            if (!hasError && toValue && FILTER_LIMITS.releaseDateMin && toValue < FILTER_LIMITS.releaseDateMin) {
                showFilterFieldError(dateTo, 'Дата «до» не может быть раньше ' + FILTER_LIMITS.releaseDateMin);
                hasError = true;
            }
            if (!hasError && toValue && FILTER_LIMITS.releaseDateMax && toValue > FILTER_LIMITS.releaseDateMax) {
                showFilterFieldError(dateTo, 'Дата «до» не может быть позже ' + FILTER_LIMITS.releaseDateMax);
                hasError = true;
            }
        }

        var fromHoursEl = document.getElementById('durationFromHours');
        var fromMinsEl = document.getElementById('durationFromMinutes');
        var fromSecsEl = document.getElementById('durationFromSeconds');
        var toHoursEl = document.getElementById('durationToHours');
        var toMinsEl = document.getElementById('durationToMinutes');
        var toSecsEl = document.getElementById('durationToSeconds');

        var maxDur = splitDuration(FILTER_LIMITS.durationMax);
        var durationErrorText =
            'Минимальная длительность — ' + FILTER_LIMITS.durationMin + ' сек., ' +
            'максимальная — ' + maxDur.h + ' ч. ' + maxDur.m + ' мин. ' + maxDur.s + ' сек.';

        var fromAllZero = fromH === 0 && fromM === 0 && fromS === 0 &&
            (fromHoursEl.value !== '' || fromMinsEl.value !== '' || fromSecsEl.value !== '');

        var toAllZero = toH === 0 && toM === 0 && toS === 0 &&
            (toHoursEl.value !== '' || toMinsEl.value !== '' || toSecsEl.value !== '');

        if (fromAllZero || fromH > maxDur.h || fromM > 59 || fromS > 59) {
            showFilterFieldError(fromHoursEl, durationErrorText);
            hasError = true;
        }

        if (!hasError && (toAllZero || toH > maxDur.h || toM > 59 || toS > 59)) {
            showFilterFieldError(toHoursEl, durationErrorText);
            hasError = true;
        }

        if (!hasError && toTotal > 0 && toTotal < FILTER_LIMITS.durationMin) {
            showFilterFieldError(toHoursEl, 'Длительность «до» должна быть не менее ' + FILTER_LIMITS.durationMin + ' сек.');
            hasError = true;
        }

        if (!hasError && fromTotal > 0 && toTotal > 0 && fromTotal > toTotal) {
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

        if (pfVal !== null && pfVal > FILTER_LIMITS.priceMin) isWideFilter = false;
        if (ptVal !== null && ptVal < FILTER_LIMITS.priceMax) isWideFilter = false;
        if (rfVal !== null && rfVal > 0.1) isWideFilter = false;
        if (rtVal !== null && rtVal < 10) isWideFilter = false;

        if (fromTotal >= FILTER_LIMITS.durationMin) isWideFilter = false;
        if (toTotal > 0) isWideFilter = false;
        if (genresMeaningful) isWideFilter = false;
        if (directorsMeaningful) isWideFilter = false;
        if (titleInput && titleInput.value.trim() !== '') isWideFilter = false;
        if (fromValue) isWideFilter = false;
        if (toValue) isWideFilter = false;

        if (isWideFilter) {
            toggleFiltersModal(false);
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Укажите хотя бы один фильтр', 'warning');
            }
            return false;
        }

        return true;
    }

    function stepFilterValue(inputId, delta, min, max, decimals) {
        var input = document.getElementById(inputId);
        if (!input) return;
        var current = parseFloat(input.value.replace(',', '.'));
        if (isNaN(current)) current = 0;
        var newValue = current + delta;
        var factor = Math.pow(10, decimals);
        newValue = Math.round(newValue * factor) / factor;
        if (min !== null && newValue < min) newValue = min;
        if (max !== null && newValue > max) newValue = max;
        input.value = newValue.toFixed(decimals);
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var filterSteppers = {};

    function startSteppingFilter(inputId, delta, min, max, decimals, button) {
        if (!filterSteppers[inputId]) {
            filterSteppers[inputId] = createStepper({
                onStep: function (d) {
                    stepFilterValue(inputId, d, min, max, decimals);
                }
            });
        }
        filterSteppers[inputId].start(delta, button);
    }

    function stopSteppingFilter(button) {
        Object.keys(filterSteppers).forEach(function (key) {
            filterSteppers[key].stop(button);
        });
        if (button) button.classList.remove('holding');
    }

    function setupSelectAllCheckbox(dropdownId, name) {
        var dropdown = document.getElementById(dropdownId);
        if (!dropdown) return;
        if (!dropdown.querySelector('input[name="' + name + '"]')) return;

        var existing = dropdown.querySelector('.accordion-select-all');
        if (existing) existing.remove();

        var wrap = document.createElement('label');
        wrap.className = 'accordion-select-all';

        var cb = document.createElement('input');
        cb.type = 'checkbox';
        cb.className = 'accordion-checkbox';

        var span = document.createElement('span');
        span.textContent = 'Выбрать всё';

        wrap.appendChild(cb);
        wrap.appendChild(span);

        function updateState() {
            var all = dropdown.querySelectorAll('input[name="' + name + '"]');
            var checked = dropdown.querySelectorAll('input[name="' + name + '"]:checked');
            var allChecked = all.length > 0 && checked.length === all.length;
            var noneChecked = checked.length === 0;
            cb.checked = allChecked;
            cb.indeterminate = !allChecked && !noneChecked;
        }

        wrap.addEventListener('click', function (e) {
            e.preventDefault();
            var all = dropdown.querySelectorAll('input[name="' + name + '"]');
            var checked = dropdown.querySelectorAll('input[name="' + name + '"]:checked');
            var allChecked = all.length > 0 && checked.length === all.length;

            if (!allChecked && all.length > MAX_SELECTED_PER_DROPDOWN) {
                if (typeof showFlashMessage === 'function') {
                    showFlashMessage(
                        'Можно выбрать не более ' + MAX_SELECTED_PER_DROPDOWN + ' значений',
                        'warning'
                    );
                }
                all.forEach(function (item, index) {
                    item.checked = index < MAX_SELECTED_PER_DROPDOWN;
                });
            } else {
                all.forEach(function (item) { item.checked = !allChecked; });
            }
            updateState();
        });

        dropdown.addEventListener('change', function (e) {
            if (e.target && e.target.name === name) {
                updateState();
            }
        });

        dropdown.insertBefore(wrap, dropdown.firstChild);
        updateState();
    }

    function resetFiltersForm() {
        var form = document.getElementById('filterForm');
        if (!form) return;

        form.querySelectorAll('input[type="text"]').forEach(function (input) {
            input.value = '';
            input.style.borderColor = '';
            input.style.boxShadow = '';
        });

        form.querySelectorAll('input[type="date"]').forEach(function (input) {
            input.value = '';
            input.style.borderColor = '';
            input.style.boxShadow = '';
        });

        form.querySelectorAll('input[type="checkbox"]').forEach(function (cb) {
            cb.checked = false;
            cb.indeterminate = false;
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
        var form = document.getElementById('filterForm');
        if (!form) return;

        form.addEventListener('submit', function (e) {
            if (typeof validateFilter === 'function' && !validateFilter()) {
                e.preventDefault();
                return;
            }
            stripEmptyFields(form);
        });

        window.addEventListener('pageshow', function (e) {
            if (e.persisted) restoreFields(form);
        });
    }

    function initAll() {
        initFilterMasks();
        initFilterAccordions();
        initFilterClearButtons();
        initFormSubmit();
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