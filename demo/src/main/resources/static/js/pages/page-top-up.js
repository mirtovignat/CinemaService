(function () {
    'use strict';

    var TOP_UP_AMOUNT_KEY = 'topUpAmount';

    function formatAmount(value) {
        if (value === '' || value === null || value === undefined) return '';
        var num = parseFloat(value.toString().replace(',', '.'));
        if (isNaN(num)) return value;
        var rounded = Math.round(num * 100) / 100;
        var parts = rounded.toFixed(2).split('.');
        var intPart = parts[0];
        var decPart = parts[1] || '00';
        var formattedInt = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
        return formattedInt + ',' + decPart + ' ₽';
    }

    function readTopUpAmount() {
        try { return sessionStorage.getItem(TOP_UP_AMOUNT_KEY); } catch (e) { return null; }
    }

    function clearTopUpAmount() {
        try { sessionStorage.removeItem(TOP_UP_AMOUNT_KEY); } catch (e) {}
    }

    function applyAmountFromStorage() {
        var input = document.getElementById('amount');
        if (!input) return;
        if (input.value && input.value.trim() !== '') return;

        var raw = readTopUpAmount();
        if (!raw) return;

        var num = parseFloat(String(raw).replace(',', '.'));
        if (isNaN(num) || num < 0.01) {
            clearTopUpAmount();
            return;
        }

        var userData = document.getElementById('userData');
        if (userData) {
            var currentBalance = parseFloat(userData.getAttribute('data-balance')) || 0;
            var maxBalance = parseFloat(userData.getAttribute('data-maxbalance')) || 100000;
            var maxAllowed = Math.min(100000, maxBalance - currentBalance);
            if (num > maxAllowed) num = maxAllowed;
        }

        if (num >= 0.01) {
            input.value = num.toFixed(2).replace('.', ',');
            input._lastValidValue = num.toFixed(2);
        }

        setTimeout(clearTopUpAmount, 600);
    }

    function setAmount(val) {
        var input = document.getElementById('amount');
        if (!input) return;
        var userData = document.getElementById('userData');
        if (!userData) return;

        var currentBalance = parseFloat(userData.getAttribute('data-balance'));
        var maxBalance = parseFloat(userData.getAttribute('data-maxbalance'));
        var maxAllowed = Math.min(100000, maxBalance - currentBalance);

        if (maxAllowed < 0.01) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Достигнут максимальный баланс', 'error');
            }
            return;
        }

        var num = Math.min(Math.max(val, 0.01), maxAllowed);
        input.value = num.toFixed(2).replace('.', ',');
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var amountStepper = null;

    function stepAmount(delta) {
        var input = document.getElementById('amount');
        if (!input) return;
        var current = parseFloat(input.value.replace(',', '.'));
        if (isNaN(current)) current = 0;
        var newValue = current + delta;
        newValue = Math.round(newValue * 100) / 100;
        if (newValue < 0) newValue = 0;
        if (newValue > 1000000) newValue = 1000000;
        input.value = newValue.toFixed(2);
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function startSteppingAmount(delta, button) {
        if (!amountStepper) {
            amountStepper = createStepper({
                onStep: function (d) { stepAmount(d); },
                startDelay: 250,
                repeatDelay: 80
            });
        }
        amountStepper.start(delta, button);
    }

    function stopSteppingAmount(button) {
        if (amountStepper) amountStepper.stop(button);
    }

    function clearTopUpForm() {
        var input = document.getElementById('amount');
        if (input) {
            try { input.form && input.form.reset(); } catch (e) {}
            input.value = '';
            input.removeAttribute('value');
            input._lastValidValue = '';
            input.disabled = false;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        }

        document.querySelectorAll('.preset-btn').forEach(function (b) {
            b.classList.remove('active');
        });

        var clientErrorEl = document.getElementById('clientError');
        if (clientErrorEl) {
            clientErrorEl.textContent = '';
            clientErrorEl.style.display = 'none';
        }

        var amountErrorEl = document.getElementById('amountError');
        if (amountErrorEl) {
            amountErrorEl.textContent = '';
            amountErrorEl.style.display = 'none';
        }

        var hidden = document.querySelectorAll('#topUpForm input[type="hidden"][name="amount"]');
        hidden.forEach(function (h) { h.remove(); });

        var flashSuccess = document.querySelector('.flash-success');
        if (flashSuccess) flashSuccess.remove();

        var flashError = document.querySelector('.flash-error:not(#clientError)');
        if (flashError) flashError.remove();
    }

    function maskPhoneElement(el) {
        if (!el) return;
        var raw = (el.textContent || '').trim();
        if (!raw) return;
        if (raw.indexOf('***') !== -1) return;
        var digits = raw.replace(/\D/g, '');
        if (digits.length < 5) return;
        if (typeof window.maskPhone !== 'function') return;
        el.textContent = window.maskPhone(raw);
    }

    function refreshBalanceFromServer() {
        fetch('/wallet/balance?_=' + Date.now(), {
            method: 'GET',
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
            cache: 'no-store'
        })
        .then(function (r) {
            if (r.status !== 200) return null;
            return r.json();
        })
        .then(function (dto) {
            if (!dto) return;

            var userData = document.getElementById('userData');
            if (userData) {
                if (dto.balance != null) userData.setAttribute('data-balance', dto.balance);
                if (dto.phone != null) userData.setAttribute('data-phone', dto.phone);
                if (dto.fullName != null) userData.setAttribute('data-fullname', dto.fullName);
            }

            var balanceEl = document.getElementById('balanceValue');
            if (balanceEl && dto.balance != null) {
                var num = parseFloat(String(dto.balance).replace(',', '.'));
                if (!isNaN(num)) balanceEl.textContent = formatAmount(num.toFixed(2));
            }

            var phoneEl = document.getElementById('userPhone');
            if (phoneEl && dto.phone != null) {
                phoneEl.textContent = (typeof window.maskPhone === 'function')
                    ? window.maskPhone(dto.phone)
                    : dto.phone;
            }

            var nameEl = document.getElementById('userFullname');
            if (nameEl && dto.fullName != null) {
                nameEl.textContent = dto.fullName;
            }
        })
        .catch(function () {});
    }

    function installBalanceRefreshOnBfcache() {
        window.addEventListener('pageshow', function (e) {
            if (e.persisted) refreshBalanceFromServer();
        });
    }

    function bindWheel(amountInput, maxAllowed) {
        var lastWheelTime = 0;

        amountInput.addEventListener('wheel', function (e) {
            e.preventDefault();
            var now = Date.now();
            var dt = lastWheelTime > 0 ? (now - lastWheelTime) / 1000 : 0.01;
            lastWheelTime = now;
            var direction = e.deltaY < 0 ? 1 : -1;
            var delta = Math.min(Math.max(dt * 1.0, 0.01), 1.0);
            delta = direction * Math.round(delta * 100) / 100;
            var current = parseFloat(amountInput.value.replace(',', '.'));
            if (isNaN(current)) current = 0;
            var newValue = Math.round((current + delta) * 100) / 100;
            if (newValue < 0) newValue = 0;
            if (newValue > maxAllowed) newValue = maxAllowed;
            if (newValue > 100000) newValue = 100000;
            amountInput.value = newValue.toFixed(2).replace('.', ',');
            amountInput.dispatchEvent(new Event('input', { bubbles: true }));
            document.querySelectorAll('.preset-btn').forEach(function (b) {
                b.classList.remove('active');
            });
        });
    }

    function bindBeforeInput(amountInput) {
        amountInput.addEventListener('beforeinput', function (e) {
            var val = this.value.replace(',', '.');
            if (e.inputType === 'insertText' && e.data) {
                var newNum = parseFloat(val + e.data);
                if (newNum > 100000) e.preventDefault();
            }
        });
    }

    function bindInputMask(amountInput, errorEl, clientErrorEl) {
        amountInput.addEventListener('input', function () {
            var val = this.value;
            if (val === '') return;
            val = val.replace(',', '.');
            var parts = val.split('.');
            var intPart = parts[0].replace(/[^0-9]/g, '');
            var decPart = parts.length > 1 ? parts[1].replace(/[^0-9]/g, '') : '';
            if (intPart.length > 1 && intPart.startsWith('0')) {
                intPart = intPart.replace(/^0+/, '');
                if (intPart === '') intPart = '0';
            }
            if (decPart.length > 2) decPart = decPart.slice(0, 2);
            var newValue = intPart;
            if (decPart.length > 0 || (val.includes('.') && intPart !== '')) {
                newValue = intPart + '.' + decPart;
            }
            if (val === '.' || val === ',') newValue = '0.';
            if (val === '0.') newValue = '0.';
            var num = parseFloat(newValue);
            if (!isNaN(num) && num > 100000) {
                this.value = this._lastValidValue || '0.00';
                return;
            }
            this._lastValidValue = newValue;
            if (this.value !== newValue) this.value = newValue;
            if (errorEl) { errorEl.textContent = ''; errorEl.style.display = 'none'; }
            if (clientErrorEl) clientErrorEl.style.display = 'none';
        });
    }

    function bindBlur(amountInput) {
        amountInput.addEventListener('blur', function () {
            var val = this.value.trim();
            if (val === '' || val === '.' || val === '0.') {
                this.value = '0,01';
                this._lastValidValue = '0.01';
                return;
            }
            var num = parseFloat(val.replace(',', '.'));
            if (isNaN(num) || num < 0.01) {
                this.value = '0,01';
            } else if (num > 100000) {
                this.value = '100000,00';
            } else {
                this.value = num.toFixed(2).replace('.', ',');
            }
            this._lastValidValue = this.value.replace(',', '.');
        });
    }

    function initAmountPersistence(amountInput) {
        if (!amountInput) return;

        var KEY = 'topUpAmountDraft';

        function save() {
            try { sessionStorage.setItem(KEY, amountInput.value || ''); } catch (e) {}
        }

        function clear() {
            try { sessionStorage.removeItem(KEY); } catch (e) {}
        }

        function restore() {
            try {
                var v = sessionStorage.getItem(KEY);
                if (v != null && v !== '' && amountInput.value === '') {
                    amountInput.value = v;
                    amountInput._lastValidValue = v.replace(',', '.');
                    amountInput.dispatchEvent(new Event('input', { bubbles: true }));
                }
            } catch (e) {}
        }

        var navType = 'navigate';
        try {
            if (performance && performance.getEntriesByType) {
                var nav = performance.getEntriesByType('navigation')[0];
                if (nav && nav.type) navType = nav.type;
            }
        } catch (e) {}

        if (navType === 'reload') {
            restore();
        } else {
            clear();
        }

        amountInput.addEventListener('input', save);
        amountInput.addEventListener('change', save);
        window.addEventListener('beforeunload', save);
    }

    function bindSubmit(form, userData, clientErrorEl) {
        form.addEventListener('submit', function (e) {
            var input = document.getElementById('amount');
            if (!input) return;
            var rawVal = input.value.replace(/[^0-9,.]/g, '').replace(',', '.');
            var num = parseFloat(rawVal);

            if (input.value.trim() === '' || isNaN(num)) {
                e.preventDefault();
                if (clientErrorEl) {
                    clientErrorEl.textContent = 'Сумма не может быть пустой';
                    clientErrorEl.style.display = 'block';
                }
                input.focus();
                return false;
            }

            if (num < 0.01) {
                e.preventDefault();
                if (clientErrorEl) {
                    clientErrorEl.textContent = 'Сумма должна быть не меньше 0,01 ₽';
                    clientErrorEl.style.display = 'block';
                }
                input.focus();
                return false;
            }

            if (num > 100000) {
                e.preventDefault();
                if (clientErrorEl) {
                    clientErrorEl.textContent = 'Сумма не может превышать 100 000,00 ₽';
                    clientErrorEl.style.display = 'block';
                }
                input.focus();
                return false;
            }

            var currentBal = parseFloat(userData.getAttribute('data-balance'));
            var maxBal = parseFloat(userData.getAttribute('data-maxbalance'));
            if (currentBal + num > maxBal) {
                e.preventDefault();
                var available = maxBal - currentBal;
                if (clientErrorEl) {
                    clientErrorEl.textContent = 'Сумма пополнения не может превышать ' + available.toFixed(2) + ' ₽ (максимальный баланс 100 000 ₽)';
                    clientErrorEl.style.display = 'block';
                }
                input.focus();
                return false;
            }

            try { sessionStorage.removeItem('topUpAmountDraft'); } catch (err) {}

            input.disabled = true;
            var hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'amount';
            hiddenInput.value = num.toFixed(2);
            form.appendChild(hiddenInput);
        });
    }

    function initTopUp() {
        var amountInput = document.getElementById('amount');
        var userData = document.getElementById('userData');
        if (!userData) return;

        var errorEl = document.getElementById('amountError');
        var clientErrorEl = document.getElementById('clientError');

        var currentBalance = parseFloat(userData.getAttribute('data-balance'));
        var maxBalance = parseFloat(userData.getAttribute('data-maxbalance'));
        var maxAllowed = Math.min(100000, maxBalance - currentBalance);

        var form = document.getElementById('topUpForm');

        applyAmountFromStorage();

        if (form && window.FormGuard) {
            window.FormGuard.register('topUp', function () { return form; });
        }

        installBalanceRefreshOnBfcache();

        if (amountInput) {
            bindWheel(amountInput, maxAllowed);
            bindBeforeInput(amountInput);
            bindInputMask(amountInput, errorEl, clientErrorEl);
            bindBlur(amountInput);

            if (amountInput.value) {
                var num = parseFloat(amountInput.value.replace(',', '.'));
                if (!isNaN(num)) {
                    amountInput.value = num.toFixed(2).replace('.', ',');
                    amountInput._lastValidValue = num.toFixed(2);
                }
            }
        }

        var phoneEl = document.getElementById('userPhone');
        maskPhoneElement(phoneEl);

        var balanceEl = document.getElementById('balanceValue');
        if (balanceEl && balanceEl.textContent) {
            var num2 = parseFloat(balanceEl.textContent.replace(/\s/g, '').replace(',', '.'));
            if (!isNaN(num2)) balanceEl.textContent = formatAmount(num2.toFixed(2));
        }

        var currEl = document.getElementById('currencySymbol');
        if (currEl) currEl.style.display = 'none';

        if (form) {
            bindSubmit(form, userData, clientErrorEl);
        }

        if (amountInput && clientErrorEl) {
            amountInput.addEventListener('input', function () {
                clientErrorEl.style.display = 'none';
            });
        }

        initAmountPersistence(amountInput);
    }

    function registerTopUpActions() {
        if (!window.Actions) return;
        if (window.__topUpActionsBound) return;
        window.__topUpActionsBound = true;

        window.Actions.register('set-amount', function (el) {
            var val = parseFloat(el.getAttribute('data-amount'));
            if (isNaN(val)) return false;
            setAmount(val);
            document.querySelectorAll('.preset-btn').forEach(function (b) {
                b.classList.toggle('active', b === el);
            });
            return true;
        });

        window.Actions.register('start-stepping-amount', function (el) {
            var delta = parseFloat(el.getAttribute('data-stepper-delta')) || 0;
            startSteppingAmount(delta, el);
            return true;
        });

        window.Actions.register('stop-stepping-amount', function (el) {
            stopSteppingAmount(el);
            return true;
        });
    }

    window.formatAmount = formatAmount;
    window.setAmount = setAmount;
    window.stepAmount = stepAmount;
    window.startSteppingAmount = startSteppingAmount;
    window.stopSteppingAmount = stopSteppingAmount;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTopUp, { once: true });
    } else {
        initTopUp();
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerTopUpActions);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerTopUpActions, { once: true });
    } else {
        registerTopUpActions();
    }
})();