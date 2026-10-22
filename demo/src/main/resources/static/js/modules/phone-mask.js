(function () {
    'use strict';

    var MAX_DIGITS = 13;

    function digitsOnly(value) {
        return String(value == null ? '' : value).replace(/\D/g, '');
    }

    function splitCountry(digits) {
        var len = digits.length;
        if (len === 0) return { country: '', rest: '' };
        var countryLen = len - 10;
        if (countryLen < 1) countryLen = 1;
        if (countryLen > 3) countryLen = 3;
        return {
            country: digits.substring(0, countryLen),
            rest: digits.substring(countryLen)
        };
    }

    function formatPhoneRaw(raw) {
        var digits = digitsOnly(raw);
        if (digits.length === 0) return '';
        if (digits.length > MAX_DIGITS) digits = digits.substring(0, MAX_DIGITS);

        var parts = splitCountry(digits);
        var formatted = '+' + parts.country;
        var rest = parts.rest;

        if (rest.length > 0) {
            formatted += ' (' + rest.substring(0, 3);
            if (rest.length >= 3) formatted += ')';
            if (rest.length > 3) formatted += ' ' + rest.substring(3, 6);
            if (rest.length > 6) formatted += '-' + rest.substring(6, 8);
            if (rest.length > 8) formatted += '-' + rest.substring(8, 10);
        }
        return formatted;
    }

    function countDigitsBefore(text, pos) {
        var count = 0;
        for (var i = 0; i < pos && i < text.length; i++) {
            if (text.charCodeAt(i) >= 48 && text.charCodeAt(i) <= 57) count++;
        }
        return count;
    }

    function positionFromDigitCount(text, digitCount) {
        if (digitCount <= 0) return text.charAt(0) === '+' ? 1 : 0;
        var seen = 0;
        for (var i = 0; i < text.length; i++) {
            var code = text.charCodeAt(i);
            if (code >= 48 && code <= 57) {
                seen++;
                if (seen === digitCount) return i + 1;
            }
        }
        return text.length;
    }

    function initPhoneField(input) {
        if (!input) return;
        if (input.dataset.phoneMaskInited === '1') return;
        input.dataset.phoneMaskInited = '1';

        var skipFormat = false;

        input.addEventListener('beforeinput', function (e) {
            if (!e.inputType) return;
            if (e.inputType === 'deleteContentBackward' ||
                e.inputType === 'deleteContentForward' ||
                e.inputType === 'deleteByCut') {
                e.preventDefault();

                var start = this.selectionStart;
                var end = this.selectionEnd;
                var val = this.value;
                var newPos = start;

                if (start === end) {
                    if (e.inputType === 'deleteContentBackward') {
                        if (start === 0) return;
                        val = val.slice(0, start - 1) + val.slice(start);
                        newPos = start - 1;
                    } else {
                        if (start >= val.length) return;
                        val = val.slice(0, start) + val.slice(start + 1);
                    }
                } else {
                    val = val.slice(0, start) + val.slice(end);
                }

                this.value = val;
                this.setSelectionRange(newPos, newPos);

                skipFormat = true;
                this.dispatchEvent(new Event('input', { bubbles: true }));
                skipFormat = false;
            }
        });

        input.addEventListener('keydown', function (e) {
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (e.key.length === 1 && !/[0-9]/.test(e.key)) {
                e.preventDefault();
            }
        });

        input.addEventListener('input', function (e) {
            if (skipFormat) return;
            if (e.inputType && e.inputType.indexOf('delete') === 0) {
                return;
            }

            var cursorPos = this.selectionStart;
            var digitsBefore = countDigitsBefore(this.value, cursorPos);
            var formatted = formatPhoneRaw(this.value);

            if (this.value !== formatted) {
                this.value = formatted;
            }

            var newPos = positionFromDigitCount(this.value, digitsBefore);
            this.setSelectionRange(newPos, newPos);
        });

        input.addEventListener('paste', function (event) {
            event.preventDefault();
            var pasted = (event.clipboardData || window.clipboardData).getData('text');
            if (!pasted) return;

            var digits = digitsOnly(pasted);
            if (!digits) return;

            var currentVal = this.value;
            var start = this.selectionStart;
            var end = this.selectionEnd;
            var newVal = currentVal.slice(0, start) + digits + currentVal.slice(end);

            var digitsBefore = countDigitsBefore(newVal, start + digits.length);

            this.value = formatPhoneRaw(newVal);

            var newPos = positionFromDigitCount(this.value, digitsBefore);
            this.setSelectionRange(newPos, newPos);
        });

        if (input.value) {
            input.value = formatPhoneRaw(input.value);
        }
    }

    function autoInitPhoneMasks(root) {
        (root || document).querySelectorAll('input[data-phone-mask]').forEach(function (input) {
            initPhoneField(input);
        });
    }

    window.PhoneMask = {
        format: formatPhoneRaw,
        init: initPhoneField,
        autoInit: autoInitPhoneMasks
    };

    window.formatPhoneRaw = formatPhoneRaw;
    window.initPhoneField = initPhoneField;
    window.autoInitPhoneMasks = autoInitPhoneMasks;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            autoInitPhoneMasks(document);
        }, { once: true });
    } else {
        autoInitPhoneMasks(document);
    }
})();