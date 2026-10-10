(function () {
    'use strict';

    var MIN_DIGITS = 11;
    var MAX_DIGITS = 13;
    var MIN_COUNTRY = 1;
    var MAX_COUNTRY = 3;

    function digitsOnly(value) {
        return String(value == null ? '' : value).replace(/\D/g, '');
    }

    function splitCountry(digits) {
        var len = digits.length;
        if (len === 0) return { country: '', rest: '' };
        var countryLen = len - 10;
        if (countryLen < MIN_COUNTRY) countryLen = MIN_COUNTRY;
        if (countryLen > MAX_COUNTRY) countryLen = MAX_COUNTRY;
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

    function parsePhone(value) {
        var digits = digitsOnly(value);
        if (digits.length < MIN_DIGITS || digits.length > MAX_DIGITS) return null;

        var parts = splitCountry(digits);
        if (parts.rest.length !== 10) return null;

        return {
            country: parts.country,
            area: parts.rest.substring(0, 3),
            part1: parts.rest.substring(3, 6),
            part2: parts.rest.substring(6, 8),
            part3: parts.rest.substring(8, 10),
            digits: digits,
            formatted: formatPhoneRaw(digits)
        };
    }

    function isValidPhone(value) {
        if (!value) return true;
        return parsePhone(value) != null;
    }

    function maskPhone(value) {
        if (!value) return '';
        var raw = String(value).trim();
        if (raw.indexOf('***') !== -1) return raw;

        var parsed = parsePhone(raw);
        if (!parsed) return raw;

        return '+' + parsed.country +
            ' (' + parsed.area + ') ***-**-' + parsed.part3;
    }

    function countDigitsBefore(text, pos) {
        var count = 0;
        for (var i = 0; i < pos && i < text.length; i++) {
            if (text.charCodeAt(i) >= 48 && text.charCodeAt(i) <= 57) count++;
        }
        return count;
    }

    function positionFromDigitCount(text, digitCount) {
        if (digitCount <= 0) {
            return text.charAt(0) === '+' ? Math.min(1, text.length) : 0;
        }
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

    function placeCursorByDigits(input, digitsBeforeCursor) {
        var value = input.value;
        var pos = positionFromDigitCount(value, digitsBeforeCursor);
        try { input.setSelectionRange(pos, pos); } catch (e) {}
    }

    function initPhoneField(input) {
        if (!input) return;
        if (input.dataset.phoneMaskInited === '1') return;
        input.dataset.phoneMaskInited = '1';

        input.addEventListener('beforeinput', function (e) {
            if (!e.inputType) return;
            if (e.inputType.indexOf('delete') !== 0) return;
            this.dataset.digitsBeforeDelete = String(
                countDigitsBefore(this.value, this.selectionStart)
            );
            this.dataset.digitsAfterDelete = String(
                countDigitsBefore(this.value, this.selectionEnd)
            );
        });

        input.addEventListener('input', function () {
            var isDelete = this.dataset.digitsBeforeDelete !== undefined;
            var beforeDigits = isDelete ? parseInt(this.dataset.digitsBeforeDelete, 10) || 0 : null;
            var afterDigits = isDelete ? parseInt(this.dataset.digitsAfterDelete, 10) || 0 : null;

            if (!isDelete) {
                var cursorBefore = this.selectionStart;
                beforeDigits = countDigitsBefore(this.value, cursorBefore);
            }

            var formatted = formatPhoneRaw(this.value);
            if (this.value !== formatted) this.value = formatted;

            if (isDelete) {
                var targetDigits = Math.max(0, beforeDigits - 1);
                if (this.selectionStart === this.selectionEnd) {
                    placeCursorByDigits(this, targetDigits);
                } else {
                    placeCursorByDigits(this, afterDigits);
                }
            } else {
                placeCursorByDigits(this, beforeDigits);
            }

            delete this.dataset.digitsBeforeDelete;
            delete this.dataset.digitsAfterDelete;
        });

        input.addEventListener('keydown', function (event) {
            var allowedKeys = [
                'Backspace', 'Tab', 'ArrowLeft', 'ArrowRight',
                'Delete', 'Enter', 'Escape', 'Home', 'End'
            ];
            if (allowedKeys.indexOf(event.key) !== -1) return;
            if (event.ctrlKey || event.metaKey) return;
            if (event.key >= '0' && event.key <= '9') return;
            if (event.key === '+' || event.key === '(' || event.key === ')' ||
                event.key === '-' || event.key === ' ') return;
            event.preventDefault();
        });

        input.addEventListener('paste', function (event) {
            event.preventDefault();
            var pasted = (event.clipboardData || window.clipboardData).getData('text');
            if (!pasted) return;
            var digits = digitsOnly(pasted);
            if (!digits) return;
            var merged = digitsOnly(this.value) + digits;
            this.value = formatPhoneRaw(merged);
            placeCursorByDigits(this, digitsOnly(this.value).length);
            this.dispatchEvent(new Event('input', { bubbles: true }));
        });

        input.addEventListener('blur', function () {
            var digits = digitsOnly(this.value);
            if (digits.length === 0) { this.value = ''; return; }
            this.value = formatPhoneRaw(this.value);
        });

        if (input.value) input.value = formatPhoneRaw(input.value);
    }

    function autoInitPhoneMasks(root) {
        (root || document).querySelectorAll('input[data-phone-mask]').forEach(function (input) {
            initPhoneField(input);
        });
    }

    window.PhoneMask = {
        format: formatPhoneRaw,
        parse: parsePhone,
        isValid: isValidPhone,
        mask: maskPhone,
        init: initPhoneField,
        autoInit: autoInitPhoneMasks,
        MAX_DIGITS: MAX_DIGITS,
        MIN_DIGITS: MIN_DIGITS
    };

    window.formatPhoneRaw = formatPhoneRaw;
    window.maskPhone = maskPhone;
    window.isValidPhone = isValidPhone;
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