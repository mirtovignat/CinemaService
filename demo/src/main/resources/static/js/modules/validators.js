(function () {
    'use strict';

    var MIN_DATE_YEAR = 1895;
    var MIN_AGE = 14;
    var MAX_AGE = 120;

    function validateEmail(email) {
        if (!email) return { ok: false, error: 'Введите email' };
        var re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!re.test(email)) return { ok: false, error: 'Некорректный формат email' };
        return { ok: true, error: '' };
    }

    function validateUsername(username) {
        if (!username) return { ok: false, error: 'Введите username' };
        if (!/^[a-zA-Z0-9_-]{3,30}$/.test(username)) {
            return { ok: false, error: '3-30 символов: латиница, цифры, _ и -' };
        }
        return { ok: true, error: '' };
    }

    function validatePhone(phone) {
        if (!phone) return { ok: true, error: '' };
        if (typeof window.PhoneMask === 'undefined') {
            return { ok: true, error: '' };
        }
        if (window.PhoneMask.isValid(phone)) {
            return { ok: true, error: '' };
        }
        return {
            ok: false,
            error: 'Некорректный номер телефона'
        };
    }

    function validateName(value, label) {
        if (!value) return { ok: false, error: 'Введите ' + (label || 'значение') };
        if (value.length < 2) return { ok: false, error: 'Слишком короткое значение' };
        if (!/^[А-Яа-яЁёA-Za-z\-]+$/.test(value)) {
            return { ok: false, error: 'Только буквы и дефис' };
        }
        return { ok: true, error: '' };
    }

    function validatePassword(password, minLength) {
        var min = minLength || 8;
        if (!password) return { ok: false, error: 'Введите пароль' };
        if (password.length < min) return { ok: false, error: 'Минимум ' + min + ' символов' };
        if (password.length > 100) return { ok: false, error: 'Пароль слишком длинный' };
        return { ok: true, error: '' };
    }

    function validateRating(rating) {
        var s = String(rating).replace(',', '.');
        var num = parseFloat(s);
        if (isNaN(num)) {
            return { ok: false, error: 'Введите число от 0.1 до 10.0' };
        }
        if (num < 0.1 || num > 10) {
            return { ok: false, error: 'Введите число от 0.1 до 10.0' };
        }
        return { ok: true, error: '' };
    }

    function parseDateValue(value) {
        if (!value) return null;
        var d = new Date(value + 'T00:00:00');
        if (isNaN(d.getTime())) return null;
        return d;
    }

    function normalizeToday() {
        var t = new Date();
        t.setHours(0, 0, 0, 0);
        return t;
    }

    function validateBirthDate(value, minAge, maxAge) {
        if (!value) return { ok: false, error: 'Укажите дату рождения' };
        var birth = parseDateValue(value);
        if (!birth) return { ok: false, error: 'Некорректная дата' };
        var today = normalizeToday();
        var age = today.getFullYear() - birth.getFullYear();
        var m = today.getMonth() - birth.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
        var min = minAge || MIN_AGE;
        var max = maxAge || MAX_AGE;
        if (age < min) return { ok: false, error: 'Регистрация с ' + min + ' лет' };
        if (age > max) return { ok: false, error: 'Проверьте дату рождения' };
        return { ok: true, error: '' };
    }

    function validateSingleDate(value, label) {
        if (!value) return { ok: true, error: '' };
        var d = parseDateValue(value);
        if (!d) return { ok: false, error: 'Некорректная дата' };
        if (d.getFullYear() < MIN_DATE_YEAR) {
            return { ok: false, error: 'Год не может быть раньше ' + MIN_DATE_YEAR };
        }
        if (d > normalizeToday()) {
            return { ok: false, error: 'Дата «' + label + '» не может быть в будущем' };
        }
        return { ok: true, error: '' };
    }

    function validateDateRange(fromValue, toValue) {
        var fromResult = validateSingleDate(fromValue, 'от');
        if (!fromResult.ok) return { field: 'from', error: fromResult.error };

        var toResult = validateSingleDate(toValue, 'до');
        if (!toResult.ok) return { field: 'to', error: toResult.error };

        if (fromValue && toValue) {
            var fromDate = parseDateValue(fromValue);
            var toDate = parseDateValue(toValue);
            if (fromDate && toDate && fromDate > toDate) {
                return { field: 'from', error: 'Дата «от» не может быть позже даты «до»' };
            }
        }

        return { ok: true, error: '' };
    }

    window.Validators = {
        email: validateEmail,
        username: validateUsername,
        phone: validatePhone,
        name: validateName,
        password: validatePassword,
        rating: validateRating,
        birthDate: validateBirthDate,
        singleDate: validateSingleDate,
        dateRange: validateDateRange,
        constants: {
            MIN_DATE_YEAR: MIN_DATE_YEAR,
            MIN_AGE: MIN_AGE,
            MAX_AGE: MAX_AGE
        }
    };
})();