(function () {
    'use strict';

    var MAX_DIGITS = 13;
    var MIN_DIGITS_FOR_MASK = 5;

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

    function formatFromParts(country, rest) {
        if (country === '') return '';

        if (rest.length > 10) rest = rest.substring(0, 10);

        var result = '+' + country;
        if (rest.length === 0) return result;

        result += ' (' + rest.substring(0, 3);
        if (rest.length >= 3) result += ')';
        if (rest.length > 3) result += ' ' + rest.substring(3, 6);
        if (rest.length > 6) result += '-' + rest.substring(6, 8);
        if (rest.length > 8) result += '-' + rest.substring(8, 10);

        return result;
    }

    function formatPhoneRaw(raw) {
        var digits = digitsOnly(raw);
        if (digits.length === 0) return '';
        if (digits.length > MAX_DIGITS) digits = digits.substring(0, MAX_DIGITS);

        var parts = splitCountry(digits);
        return formatFromParts(parts.country, parts.rest);
    }

    function maskPhone(raw) {
        if (raw == null) return '';
        var str = String(raw).trim();
        if (!str) return '';

        var digits = digitsOnly(str);
        if (digits.length < MIN_DIGITS_FOR_MASK) return str;
        if (digits.length > MAX_DIGITS) digits = digits.substring(0, MAX_DIGITS);

        var parts = splitCountry(digits);
        var rest = parts.rest;

        var head3 = rest.substring(0, 3);
        var tail2 = rest.length >= 2 ? rest.substring(rest.length - 2) : rest;

        var result = '+' + parts.country;
        if (head3) {
            result += ' (' + head3;
            if (rest.length >= 3) result += ')';
        }
        result += ' ***-**-' + tail2;

        return result.replace(/\s+/g, ' ').trim();
    }

    function isValidPhone(raw) {
        if (raw == null || raw === '') return true;
        var digits = digitsOnly(raw);
        return digits.length >= 11 && digits.length <= MAX_DIGITS;
    }

    function countDigitsBefore(text, pos) {
        var count = 0;
        for (var i = 0; i < pos && i < text.length; i++) {
            var code = text.charCodeAt(i);
            if (code >= 48 && code <= 57) count++;
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

    function isDeletion(inputType) {
        if (!inputType) return false;
        return inputType.indexOf('delete') === 0;
    }

    function isCaretAtEnd(input) {
        return input.selectionStart === input.value.length &&
               input.selectionEnd === input.value.length;
    }

    function initPhoneField(input) {
        if (!input) return;
        if (input.dataset.phoneMaskInited === '1') return;
        input.dataset.phoneMaskInited = '1';

        var skipFormat = false;

        input.addEventListener('beforeinput', function (event) {
            if (!event.inputType) return;

            if (event.inputType === 'deleteContentBackward') {
                if (this.value === '') {
                    event.preventDefault();
                    return;
                }

                if (!isCaretAtEnd(this)) {
                    return;
                }

                var digits = digitsOnly(this.value);
                var countryLen = digits.length > 10 ? digits.length - 10 : digits.length;

                if (countryLen > 1) {
                    event.preventDefault();

                    var nextDigits = digits.substring(0, countryLen - 1) + digits.substring(countryLen);
                    var formatted = formatPhoneRaw(nextDigits);
                    this.value = formatted;

                    try { this.setSelectionRange(formatted.length, formatted.length); } catch (err) {}

                    skipFormat = true;
                    this.dispatchEvent(new Event('input', { bubbles: true }));
                    skipFormat = false;
                    return;
                }

                return;
            }

            if (event.inputType === 'deleteContentForward' ||
                event.inputType === 'deleteByCut') {
                return;
            }

            if (event.inputType === 'insertText' && event.data) {
                if (!/^[0-9]+$/.test(event.data)) {
                    event.preventDefault();
                    return;
                }
                var currentDigits = digitsOnly(this.value);
                if (currentDigits.length + event.data.length > MAX_DIGITS) {
                    event.preventDefault();
                }
            }
        });

        input.addEventListener('keydown', function (event) {
            if (event.ctrlKey || event.metaKey || event.altKey) return;
            if (event.key.length !== 1) return;
            if (!/[0-9]/.test(event.key)) {
                event.preventDefault();
            }
        });

        input.addEventListener('input', function (event) {
            if (skipFormat) return;

            if (isDeletion(event.inputType)) {
                return;
            }

            var cursorPos = this.selectionStart;
            var digitsBefore = countDigitsBefore(this.value, cursorPos);
            var formatted = formatPhoneRaw(this.value);

            if (this.value !== formatted) {
                this.value = formatted;
                var newPos = positionFromDigitCount(this.value, digitsBefore);
                try { this.setSelectionRange(newPos, newPos); } catch (err) {}
            }
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
            try { this.setSelectionRange(newPos, newPos); } catch (err) {}
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
        mask: maskPhone,
        isValid: isValidPhone,
        init: initPhoneField,
        autoInit: autoInitPhoneMasks
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