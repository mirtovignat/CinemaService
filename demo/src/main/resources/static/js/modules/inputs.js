(function () {
    'use strict';

    function setupNumericRatingInput(inputId) {
        var input = document.getElementById(inputId);
        if (!input) return;

        input.addEventListener('input', function () {
            var value = this.value;
            if (value === '') return;
            value = value.replace(',', '.');
            var parts = value.split('.');
            var intPart = parts[0].replace(/[^0-9]/g, '');
            var decPart = parts.length > 1 ? parts[1].replace(/[^0-9]/g, '') : '';
            if (intPart === '' && decPart !== '') intPart = '0';
            if (intPart.length > 1 && intPart.startsWith('0')) {
                intPart = intPart.replace(/^0+/, '');
                if (intPart === '') intPart = '0';
            }
            if (intPart.length > 2) intPart = intPart.slice(0, 2);
            var intNum = parseInt(intPart);
            if (intNum > 10) intPart = '10';
            if (decPart.length > 1) decPart = decPart.slice(0, 1);
            var newValue = intPart;
            if (decPart.length > 0 || (value.includes('.') && intPart !== '')) {
                newValue = intPart + '.' + decPart;
            }
            var num = parseFloat(newValue);
            if (!isNaN(num)) {
                if (num > 10) newValue = '10.0';
                if (num > 0 && num < 0.1) newValue = '0.1';
            }
            if (value === '.' || value === ',') newValue = '0.';
            if (value === '0.') newValue = '0.';
            if (this.value !== newValue) this.value = newValue;
        });

        input.addEventListener('keydown', function (e) {
            var allowedKeys = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete', 'Enter', 'Escape', '.', ','];
            if (allowedKeys.includes(e.key)) return;
            if (e.key >= '0' && e.key <= '9') return;
            e.preventDefault();
        });

        input.addEventListener('paste', function (e) {
            e.preventDefault();
            var pasted = (e.clipboardData || window.clipboardData).getData('text');
            var cleaned = pasted.replace(/[^0-9.,]/g, '').replace(',', '.');
            var num = parseFloat(cleaned);
            if (!isNaN(num)) {
                var val = Math.min(Math.max(num, 0.1), 10);
                this.value = val.toFixed(1);
            } else if (cleaned === '.') {
                this.value = '0.';
            } else {
                this.value = '';
            }
        });

        input.addEventListener('blur', function () {
            if (this.value.trim() === '') {
                if (input.dataset.allowEmptyOnBlur === '1') return;
                this.value = '5.0';
                return;
            }
            if (this.value === '.' || this.value === '0.') {
                if (input.dataset.allowEmptyOnBlur === '1') {
                    this.value = '';
                    return;
                }
                this.value = '0.1';
                return;
            }
            var num = parseFloat(this.value.replace(',', '.'));
            if (isNaN(num)) {
                if (input.dataset.allowEmptyOnBlur === '1') {
                    this.value = '';
                    return;
                }
                this.value = '0.1';
                return;
            }
            if (num < 0.1) {
                if (input.dataset.allowEmptyOnBlur === '1') {
                    this.value = '';
                    return;
                }
                this.value = '0.1';
                return;
            }
            if (num > 10) {
                this.value = '10.0';
                return;
            }
            this.value = (Math.round(num * 10) / 10).toFixed(1);
        });
    }

    function setupFilterPriceInput(inputId) {
        var input = document.getElementById(inputId);
        if (!input) return;
        input.addEventListener('input', function () {
            var value = this.value;
            if (value === '') return;
            value = value.replace(',', '.');
            var parts = value.split('.');
            var intPart = parts[0].replace(/[^0-9]/g, '');
            var decPart = parts.length > 1 ? parts[1].replace(/[^0-9]/g, '') : '';
            if (intPart.length > 1 && intPart.startsWith('0')) {
                intPart = intPart.replace(/^0+/, '');
                if (intPart === '') intPart = '0';
            }
            if (decPart.length > 2) decPart = decPart.slice(0, 2);
            var newValue = intPart;
            if (decPart.length > 0 || (value.includes('.') && intPart !== '')) {
                newValue = intPart + '.' + decPart;
            }
            if (value === '.' || value === ',') newValue = '0.';
            if (this.value !== newValue) this.value = newValue;
        });
        input.addEventListener('keydown', function (e) {
            var allowedKeys = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete', 'Enter', 'Escape', '.', ','];
            if (allowedKeys.includes(e.key)) return;
            if (e.key >= '0' && e.key <= '9') return;
            e.preventDefault();
        });
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            var pasted = (e.clipboardData || window.clipboardData).getData('text');
            var cleaned = pasted.replace(/[^0-9.,]/g, '').replace(',', '.');
            var parts = cleaned.split('.');
            var intPart = parts[0].replace(/[^0-9]/g, '');
            var decPart = parts.length > 1 ? parts[1].replace(/[^0-9]/g, '').slice(0, 2) : '';
            if (intPart !== '' || decPart !== '') {
                this.value = decPart.length > 0 ? intPart + '.' + decPart : intPart;
            } else if (cleaned === '.') {
                this.value = '0.';
            } else {
                this.value = '';
            }
        });
        input.addEventListener('blur', function () {
            if (this.value.trim() === '') return;
            if (this.value === '.' || this.value === '0.') this.value = '0';
        });
    }

    function setupDurationStrictFilter(id) {
        var input = document.getElementById(id);
        if (!input) return;
        if (input.dataset.durationStrictBound === '1') return;
        input.dataset.durationStrictBound = '1';

        function getMax() {
            var attr = input.getAttribute('data-max');
            var n = parseInt(attr, 10);
            return isNaN(n) ? Infinity : n;
        }

        input.addEventListener('beforeinput', function (e) {
            if (e.data && !/^\d+$/.test(e.data)) { e.preventDefault(); return; }
            if (e.data) {
                var selStart = input.selectionStart;
                var selEnd = input.selectionEnd;
                var current = input.value;
                var nextStr = current.slice(0, selStart) + e.data + current.slice(selEnd);
                var nextNum = parseInt(nextStr, 10);
                if (nextNum > getMax()) e.preventDefault();
            }
        });
        input.addEventListener('keydown', function (e) {
            var allowed = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete', 'Enter', 'Escape'];
            if (allowed.includes(e.key) || e.ctrlKey || e.metaKey) return;
            if (!/^\d$/.test(e.key)) e.preventDefault();
        });
        input.addEventListener('input', function () {
            var maxVal = getMax();
            var valStr = this.value.replace(/\D/g, '');
            if (valStr !== '') {
                var valNum = parseInt(valStr, 10);
                if (valNum > maxVal) valNum = maxVal;
                valStr = valNum.toString();
            }
            if (this.value !== valStr) this.value = valStr;
            if (typeof calculateRangeMinutes === 'function') calculateRangeMinutes();
        });
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            var pasted = (e.clipboardData || window.clipboardData).getData('text');
            var digits = pasted.replace(/\D/g, '');
            if (!digits) return;
            var selStart = input.selectionStart;
            var selEnd = input.selectionEnd;
            var current = input.value;
            var nextStr = current.slice(0, selStart) + digits + current.slice(selEnd);
            var nextNum = parseInt(nextStr, 10);
            if (!isNaN(nextNum)) {
                var maxVal = getMax();
                if (nextNum > maxVal) nextNum = maxVal;
                input.value = nextNum.toString();
                if (typeof calculateRangeMinutes === 'function') calculateRangeMinutes();
            }
        });
    }

    function setupRatingInput() {
        var input = document.getElementById('ratingValue');
        if (input) input.dataset.allowEmptyOnBlur = '0';
        setupNumericRatingInput('ratingValue');
    }

    function setupFilterRatingInput(inputId) {
        var input = document.getElementById(inputId);
        if (input) input.dataset.allowEmptyOnBlur = '1';
        setupNumericRatingInput(inputId);
    }

    function autoInitInputs() {
        setupRatingInput();
    }

    window.setupRatingInput = setupRatingInput;
    window.setupFilterRatingInput = setupFilterRatingInput;
    window.setupFilterPriceInput = setupFilterPriceInput;
    window.setupDurationStrictFilter = setupDurationStrictFilter;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoInitInputs, { once: true });
    } else {
        autoInitInputs();
    }
})();