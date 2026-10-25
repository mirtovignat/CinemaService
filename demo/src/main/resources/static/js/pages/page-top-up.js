(function () {
    'use strict';

    var TOP_UP_AMOUNT_KEY = 'topUpAmount';

    function formatAmount(amountValue) {
        if (amountValue === '' || amountValue === null || amountValue === undefined) return '';
        var numericAmount = parseFloat(amountValue.toString().replace(',', '.'));
        if (isNaN(numericAmount)) return amountValue;
        var roundedAmount = Math.round(numericAmount * 100) / 100;
        var amountParts = roundedAmount.toFixed(2).split('.');
        var integerPart = amountParts[0];
        var fractionalPart = amountParts[1] || '00';
        var formattedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
        return formattedInteger + ',' + fractionalPart + ' ₽';
    }

    function readTopUpAmount() {
        try { return sessionStorage.getItem(TOP_UP_AMOUNT_KEY); } catch (error) { return null; }
    }

    function clearTopUpAmount() {
        try { sessionStorage.removeItem(TOP_UP_AMOUNT_KEY); } catch (error) {}
    }

    function applyAmountFromStorage() {
        var amountInput = document.getElementById('amount');
        if (!amountInput) return;
        if (amountInput.value && amountInput.value.trim() !== '') return;

        var storedAmount = readTopUpAmount();
        if (!storedAmount) return;

        var numericAmount = parseFloat(String(storedAmount).replace(',', '.'));
        if (isNaN(numericAmount) || numericAmount < 0.01) {
            clearTopUpAmount();
            return;
        }

        var userDataElement = document.getElementById('userData');
        if (userDataElement) {
            var currentBalance = parseFloat(userDataElement.getAttribute('data-balance')) || 0;
            var maxBalance = parseFloat(userDataElement.getAttribute('data-maxbalance')) || 100000;
            var maxAllowedAmount = Math.min(100000, maxBalance - currentBalance);
            if (numericAmount > maxAllowedAmount) numericAmount = maxAllowedAmount;
        }

        if (numericAmount >= 0.01) {
            amountInput.value = numericAmount.toFixed(2).replace('.', ',');
            amountInput._lastValidValue = numericAmount.toFixed(2);
        }

        setTimeout(clearTopUpAmount, 600);
    }

    function setAmount(amountValue) {
        var amountInput = document.getElementById('amount');
        if (!amountInput) return;
        var userDataElement = document.getElementById('userData');
        if (!userDataElement) return;

        var currentBalance = parseFloat(userDataElement.getAttribute('data-balance'));
        var maxBalance = parseFloat(userDataElement.getAttribute('data-maxbalance'));
        var maxAllowedAmount = Math.min(100000, maxBalance - currentBalance);

        if (maxAllowedAmount < 0.01) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Достигнут максимальный баланс', 'error');
            }
            return;
        }

        var numericAmount = Math.min(Math.max(amountValue, 0.01), maxAllowedAmount);
        amountInput.value = numericAmount.toFixed(2).replace('.', ',');
        amountInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    var amountStepper = null;

    function stepAmount(delta) {
        var amountInput = document.getElementById('amount');
        if (!amountInput) return;
        var currentAmount = parseFloat(amountInput.value.replace(',', '.'));
        if (isNaN(currentAmount)) currentAmount = 0;
        var newAmount = currentAmount + delta;
        newAmount = Math.round(newAmount * 100) / 100;
        if (newAmount < 0) newAmount = 0;
        if (newAmount > 1000000) newAmount = 1000000;
        amountInput.value = newAmount.toFixed(2);
        amountInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function startSteppingAmount(delta, buttonElement) {
        if (!amountStepper) {
            amountStepper = createStepper({
                onStep: function (stepDelta) { stepAmount(stepDelta); },
                startDelay: 250,
                repeatDelay: 80
            });
        }
        amountStepper.start(delta, buttonElement);
    }

    function stopSteppingAmount(buttonElement) {
        if (amountStepper) amountStepper.stop(buttonElement);
    }

    function maskPhoneElement(phoneElement) {
        if (!phoneElement) return;
        var rawPhone = (phoneElement.getAttribute('data-raw-phone') || phoneElement.textContent || '').trim();
        if (!rawPhone) return;
        if (typeof window.maskPhone !== 'function') return;
        phoneElement.textContent = window.maskPhone(rawPhone);
    }

    function refreshBalanceFromServer() {
        fetch('/wallet/balance?_=' + Date.now(), {
            method: 'GET',
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
            redirect: 'manual',
            credentials: 'same-origin',
            cache: 'no-store'
        })
        .then(function (response) {
            if (response.type === 'opaqueredirect' || response.status === 0) {
                window.location.href = '/login';
                return null;
            }
            if (response.redirected) {
                window.location.href = response.url;
                return null;
            }
            if (response.status !== 200) return null;
            return response.json();
        })
        .then(function (walletDto) {
            if (!walletDto) return;

            var userDataElement = document.getElementById('userData');
            if (userDataElement) {
                if (walletDto.balance != null) userDataElement.setAttribute('data-balance', walletDto.balance);
                if (walletDto.phone != null) userDataElement.setAttribute('data-phone', walletDto.phone);
                if (walletDto.fullName != null) userDataElement.setAttribute('data-fullname', walletDto.fullName);
            }

            var balanceValueElement = document.getElementById('balanceValue');
            if (balanceValueElement && walletDto.balance != null) {
                var numericBalance = parseFloat(String(walletDto.balance).replace(',', '.'));
                if (!isNaN(numericBalance)) balanceValueElement.textContent = formatAmount(numericBalance.toFixed(2));
            }

            var userPhoneElement = document.getElementById('userPhone');
            if (userPhoneElement && walletDto.phone != null) {
                userPhoneElement.setAttribute('data-raw-phone', walletDto.phone);
                userPhoneElement.textContent = (typeof window.maskPhone === 'function')
                    ? window.maskPhone(walletDto.phone)
                    : walletDto.phone;
            }

            var userFullnameElement = document.getElementById('userFullname');
            if (userFullnameElement && walletDto.fullName != null) {
                userFullnameElement.textContent = walletDto.fullName;
            }
        })
        .catch(function () {});
    }

    function installBalanceRefreshOnBfcache() {
        window.addEventListener('pageshow', function (event) {
            if (event.persisted) refreshBalanceFromServer();
        });
    }

    function bindWheel(amountInput, maxAllowedAmount) {
        var lastWheelTime = 0;

        amountInput.addEventListener('wheel', function (event) {
            event.preventDefault();
            var now = Date.now();
            var timeDelta = lastWheelTime > 0 ? (now - lastWheelTime) / 1000 : 0.01;
            lastWheelTime = now;
            var direction = event.deltaY < 0 ? 1 : -1;
            var delta = Math.min(Math.max(timeDelta * 1.0, 0.01), 1.0);
            delta = direction * Math.round(delta * 100) / 100;
            var currentAmount = parseFloat(amountInput.value.replace(',', '.'));
            if (isNaN(currentAmount)) currentAmount = 0;
            var newAmount = Math.round((currentAmount + delta) * 100) / 100;
            if (newAmount < 0) newAmount = 0;
            if (newAmount > maxAllowedAmount) newAmount = maxAllowedAmount;
            if (newAmount > 100000) newAmount = 100000;
            amountInput.value = newAmount.toFixed(2).replace('.', ',');
            amountInput.dispatchEvent(new Event('input', { bubbles: true }));
            document.querySelectorAll('.preset-btn').forEach(function (presetButton) {
                presetButton.classList.remove('active');
            });
        });
    }

    function bindBeforeInput(amountInput) {
        amountInput.addEventListener('beforeinput', function (event) {
            var currentValue = this.value.replace(',', '.');
            if (event.inputType === 'insertText' && event.data) {
                var potentialValue = parseFloat(currentValue + event.data);
                if (potentialValue > 100000) event.preventDefault();
            }
        });
    }

    function bindInputMask(amountInput, amountErrorElement, clientErrorElement) {
        amountInput.addEventListener('input', function () {
            var inputValue = this.value;
            if (inputValue === '') return;
            inputValue = inputValue.replace(',', '.');
            var valueParts = inputValue.split('.');
            var integerPart = valueParts[0].replace(/[^0-9]/g, '');
            var fractionalPart = valueParts.length > 1 ? valueParts[1].replace(/[^0-9]/g, '') : '';
            if (integerPart.length > 1 && integerPart.startsWith('0')) {
                integerPart = integerPart.replace(/^0+/, '');
                if (integerPart === '') integerPart = '0';
            }
            if (fractionalPart.length > 2) fractionalPart = fractionalPart.slice(0, 2);
            var newValue = integerPart;
            if (fractionalPart.length > 0 || (inputValue.includes('.') && integerPart !== '')) {
                newValue = integerPart + '.' + fractionalPart;
            }
            if (inputValue === '.' || inputValue === ',') newValue = '0.';
            if (inputValue === '0.') newValue = '0.';
            var numericValue = parseFloat(newValue);
            if (!isNaN(numericValue) && numericValue > 100000) {
                this.value = this._lastValidValue || '0.00';
                return;
            }
            this._lastValidValue = newValue;
            if (this.value !== newValue) this.value = newValue;
            if (amountErrorElement) { amountErrorElement.textContent = ''; amountErrorElement.style.display = 'none'; }
            if (clientErrorElement) clientErrorElement.style.display = 'none';
        });
    }

    function bindBlur(amountInput) {
        amountInput.addEventListener('blur', function () {
            var currentValue = this.value.trim();
            if (currentValue === '' || currentValue === '.' || currentValue === '0.') {
                this.value = '0,01';
                this._lastValidValue = '0.01';
                return;
            }
            var numericValue = parseFloat(currentValue.replace(',', '.'));
            if (isNaN(numericValue) || numericValue < 0.01) {
                this.value = '0,01';
            } else if (numericValue > 100000) {
                this.value = '100000,00';
            } else {
                this.value = numericValue.toFixed(2).replace('.', ',');
            }
            this._lastValidValue = this.value.replace(',', '.');
        });
    }

    function initAmountPersistence(amountInput) {
        if (!amountInput) return;

        var DRAFT_KEY = 'topUpAmountDraft';

        function saveDraft() {
            try { sessionStorage.setItem(DRAFT_KEY, amountInput.value || ''); } catch (error) {}
        }

        function clearDraft() {
            try { sessionStorage.removeItem(DRAFT_KEY); } catch (error) {}
        }

        function restoreDraft() {
            try {
                var savedValue = sessionStorage.getItem(DRAFT_KEY);
                if (savedValue != null && savedValue !== '' && amountInput.value === '') {
                    amountInput.value = savedValue;
                    amountInput._lastValidValue = savedValue.replace(',', '.');
                    amountInput.dispatchEvent(new Event('input', { bubbles: true }));
                }
            } catch (error) {}
        }

        var navigationType = 'navigate';
        try {
            if (performance && performance.getEntriesByType) {
                var navigationEntry = performance.getEntriesByType('navigation')[0];
                if (navigationEntry && navigationEntry.type) navigationType = navigationEntry.type;
            }
        } catch (error) {}

        if (navigationType === 'reload') {
            restoreDraft();
        } else {
            clearDraft();
        }

        amountInput.addEventListener('input', saveDraft);
        amountInput.addEventListener('change', saveDraft);
        window.addEventListener('beforeunload', saveDraft);
    }

    function bindSubmit(topUpForm, userDataElement, clientErrorElement) {
        topUpForm.addEventListener('submit', function (event) {
            var amountInput = document.getElementById('amount');
            if (!amountInput) return;
            var rawValue = amountInput.value.replace(/[^0-9,.]/g, '').replace(',', '.');
            var numericAmount = parseFloat(rawValue);

            if (amountInput.value.trim() === '' || isNaN(numericAmount)) {
                event.preventDefault();
                if (clientErrorElement) {
                    clientErrorElement.textContent = 'Сумма не может быть пустой';
                    clientErrorElement.style.display = 'block';
                }
                amountInput.focus();
                return false;
            }

            if (numericAmount < 0.01) {
                event.preventDefault();
                if (clientErrorElement) {
                    clientErrorElement.textContent = 'Сумма должна быть не меньше 0,01 ₽';
                    clientErrorElement.style.display = 'block';
                }
                amountInput.focus();
                return false;
            }

            if (numericAmount > 100000) {
                event.preventDefault();
                if (clientErrorElement) {
                    clientErrorElement.textContent = 'Сумма не может превышать 100 000,00 ₽';
                    clientErrorElement.style.display = 'block';
                }
                amountInput.focus();
                return false;
            }

            var currentBalance = parseFloat(userDataElement.getAttribute('data-balance'));
            var maxBalance = parseFloat(userDataElement.getAttribute('data-maxbalance'));
            if (currentBalance + numericAmount > maxBalance) {
                event.preventDefault();
                var availableAmount = maxBalance - currentBalance;
                if (clientErrorElement) {
                    clientErrorElement.textContent = 'Сумма пополнения не может превышать ' + availableAmount.toFixed(2) + ' ₽ (максимальный баланс 100 000 ₽)';
                    clientErrorElement.style.display = 'block';
                }
                amountInput.focus();
                return false;
            }

            try { sessionStorage.removeItem('topUpAmountDraft'); } catch (error) {}

            amountInput.disabled = true;
            var hiddenAmountInput = document.createElement('input');
            hiddenAmountInput.type = 'hidden';
            hiddenAmountInput.name = 'amount';
            hiddenAmountInput.value = numericAmount.toFixed(2);
            topUpForm.appendChild(hiddenAmountInput);
        });
    }

    function initTopUp() {
        var amountInput = document.getElementById('amount');
        var userDataElement = document.getElementById('userData');
        if (!userDataElement) return;

        var amountErrorElement = document.getElementById('amountError');
        var clientErrorElement = document.getElementById('clientError');

        var currentBalance = parseFloat(userDataElement.getAttribute('data-balance'));
        var maxBalance = parseFloat(userDataElement.getAttribute('data-maxbalance'));
        var maxAllowedAmount = Math.min(100000, maxBalance - currentBalance);

        var topUpForm = document.getElementById('topUpForm');

        applyAmountFromStorage();

        if (topUpForm && window.FormGuard) {
            window.FormGuard.register('topUp', function () { return topUpForm; });
        }

        installBalanceRefreshOnBfcache();

        if (amountInput) {
            bindWheel(amountInput, maxAllowedAmount);
            bindBeforeInput(amountInput);
            bindInputMask(amountInput, amountErrorElement, clientErrorElement);
            bindBlur(amountInput);

            if (amountInput.value) {
                var numericValue = parseFloat(amountInput.value.replace(',', '.'));
                if (!isNaN(numericValue)) {
                    amountInput.value = numericValue.toFixed(2).replace('.', ',');
                    amountInput._lastValidValue = numericValue.toFixed(2);
                }
            }
        }

        var userPhoneElement = document.getElementById('userPhone');
        if (userPhoneElement) {
            var rawPhone = userDataElement.getAttribute('data-phone') || '';
            if (rawPhone) userPhoneElement.setAttribute('data-raw-phone', rawPhone);
            maskPhoneElement(userPhoneElement);
        }

        var balanceValueElement = document.getElementById('balanceValue');
        if (balanceValueElement && balanceValueElement.textContent) {
            var numericBalance = parseFloat(balanceValueElement.textContent.replace(/\s/g, '').replace(',', '.'));
            if (!isNaN(numericBalance)) balanceValueElement.textContent = formatAmount(numericBalance.toFixed(2));
        }

        var currencySymbolElement = document.getElementById('currencySymbol');
        if (currencySymbolElement) currencySymbolElement.style.display = 'none';

        if (topUpForm) {
            bindSubmit(topUpForm, userDataElement, clientErrorElement);
        }

        if (amountInput && clientErrorElement) {
            amountInput.addEventListener('input', function () {
                clientErrorElement.style.display = 'none';
            });
        }

        initAmountPersistence(amountInput);
    }

    function registerTopUpActions() {
        if (!window.Actions) return;
        if (window.__topUpActionsBound) return;
        window.__topUpActionsBound = true;

        window.Actions.register('set-amount', function (element) {
            var amountValue = parseFloat(element.getAttribute('data-amount'));
            if (isNaN(amountValue)) return false;
            setAmount(amountValue);
            document.querySelectorAll('.preset-btn').forEach(function (presetButton) {
                presetButton.classList.toggle('active', presetButton === element);
            });
            return true;
        });

        window.Actions.register('start-stepping-amount', function (element) {
            var delta = parseFloat(element.getAttribute('data-stepper-delta')) || 0;
            startSteppingAmount(delta, element);
            return true;
        });

        window.Actions.register('stop-stepping-amount', function (element) {
            stopSteppingAmount(element);
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