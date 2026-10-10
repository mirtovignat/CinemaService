(function () {
    'use strict';

    function getMaxYear(input) {
        var attr = input.getAttribute('data-year-max');
        if (attr) {
            var n = parseInt(attr, 10);
            if (!isNaN(n)) return n;
        }
        return new Date().getFullYear();
    }

    function getMinYear(input) {
        var attr = input.getAttribute('data-year-min');
        if (attr) {
            var n = parseInt(attr, 10);
            if (!isNaN(n)) return n;
        }
        return 1895;
    }

    function isPrefixAllowed(prefix, maxYearStr) {
        if (prefix === '') return true;
        var len = prefix.length;
        if (len > maxYearStr.length) return false;
        var maxPrefix = maxYearStr.substring(0, len);
        var prefixNum = parseInt(prefix, 10);
        var maxPrefixNum = parseInt(maxPrefix, 10);
        if (isNaN(prefixNum) || isNaN(maxPrefixNum)) return false;
        return prefixNum <= maxPrefixNum;
    }

    function clampYearStr(yearStr, minYear, maxYear) {
        if (!yearStr || yearStr.length !== 4) return yearStr;
        var y = parseInt(yearStr, 10);
        if (isNaN(y)) return yearStr;
        if (y > maxYear) return String(maxYear);
        if (y < minYear) return String(minYear);
        return yearStr;
    }

    function initDateField(textInput, nativeInput) {
        if (!textInput) return;
        if (textInput.dataset.dateMaskInited === '1') return;
        textInput.dataset.dateMaskInited = '1';

        var maxYear = getMaxYear(textInput);
        var minYear = getMinYear(textInput);
        var maxYearStr = String(maxYear);

        if (textInput.value) {
            var digits0 = String(textInput.value).replace(/\D/g, '');
            textInput.value = digits0.substring(0, 4);
        }

        textInput.addEventListener('beforeinput', function (e) {
            if (e.inputType && e.inputType.indexOf('delete') === 0) return;
            var data = e.data;
            if (data === null || data === undefined) return;
            if (!/^\d+$/.test(data)) {
                e.preventDefault();
                return;
            }
            var current = this.value.replace(/\D/g, '');
            var selStart = this.selectionStart;
            var selEnd = this.selectionEnd;
            var next = current.slice(0, selStart) + data + current.slice(selEnd);
            if (next.length > 4) {
                e.preventDefault();
                return;
            }
            if (!isPrefixAllowed(next, maxYearStr)) {
                e.preventDefault();
            }
        });

        textInput.addEventListener('keydown', function (e) {
            var allowed = [
                'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
                'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
                'Home', 'End'
            ];
            if (allowed.indexOf(e.key) !== -1) return;
            if (e.ctrlKey || e.metaKey) return;
            if (!/^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });

        textInput.addEventListener('paste', function (e) {
            e.preventDefault();
            var pasted = (e.clipboardData || window.clipboardData).getData('text') || '';
            var digits = pasted.replace(/\D/g, '').substring(0, 4);
            if (!digits) return;
            if (!isPrefixAllowed(digits, maxYearStr)) return;
            this.value = digits;
            this.dispatchEvent(new Event('input', { bubbles: true }));
            this.dispatchEvent(new Event('change', { bubbles: true }));
        });

        textInput.addEventListener('input', function () {
            var digits = this.value.replace(/\D/g, '').substring(0, 4);
            if (digits !== this.value) this.value = digits;
            if (digits.length === 4) {
                var clamped = clampYearStr(digits, minYear, maxYear);
                if (clamped !== digits) this.value = clamped;
                syncToNative();
            }
        });

        textInput.addEventListener('blur', function () {
            var digits = this.value.replace(/\D/g, '').substring(0, 4);
            if (digits.length === 0) {
                this.value = '';
                syncToNative();
                return;
            }
            if (digits.length === 4) {
                var clamped = clampYearStr(digits, minYear, maxYear);
                this.value = clamped;
            } else {
                this.value = digits;
            }
            syncToNative();
        });

        function syncToNative() {
            if (!nativeInput) return;
            var digits = textInput.value.replace(/\D/g, '');
            if (digits.length !== 4) {
                nativeInput.value = '';
                return;
            }
            nativeInput.value = digits + '-01-01';
        }

        if (nativeInput) {
            nativeInput.addEventListener('change', function () {
                var val = nativeInput.value;
                if (!val) {
                    textInput.value = '';
                    textInput.dispatchEvent(new Event('input', { bubbles: true }));
                    textInput.dispatchEvent(new Event('change', { bubbles: true }));
                    return;
                }
                var y = val.split('-')[0];
                if (y && y.length === 4) {
                    var clamped = clampYearStr(y, minYear, maxYear);
                    textInput.value = clamped;
                    if (clamped !== y) {
                        nativeInput.value = clamped + '-01-01';
                    }
                }
                textInput.dispatchEvent(new Event('input', { bubbles: true }));
                textInput.dispatchEvent(new Event('change', { bubbles: true }));
            });
        }
    }

    function autoInitDateMasks(root) {
        (root || document).querySelectorAll('.date-mask-wrapper').forEach(function (wrapper) {
            var textInput = wrapper.querySelector('input[data-year-mask]');
            var nativeInput = wrapper.querySelector('input[type="date"]');
            if (textInput) {
                initDateField(textInput, nativeInput);
            }
        });
    }

    function initAllDateMasks() {
        autoInitDateMasks(document);
    }

    window.initDateField = initDateField;
    window.autoInitDateMasks = autoInitDateMasks;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAllDateMasks, { once: true });
    } else {
        initAllDateMasks();
    }
})();