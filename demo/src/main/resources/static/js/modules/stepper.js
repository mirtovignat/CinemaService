(function () {
    'use strict';

    function createStepper(config) {
        var onStep = config.onStep;
        var startDelay = config.startDelay || 300;
        var repeatDelay = config.repeatDelay || 80;
        var _timeout = null;
        var _interval = null;

        function stop(button) {
            if (button) button.classList.remove('holding');
            if (_timeout) { clearTimeout(_timeout); _timeout = null; }
            if (_interval) { clearInterval(_interval); _interval = null; }
        }

        function start(delta, button) {
            stop(button);
            if (button) button.classList.add('holding');
            onStep(delta);
            _timeout = setTimeout(function () {
                _interval = setInterval(function () {
                    onStep(delta);
                }, repeatDelay);
            }, startDelay);
        }

        return { start: start, stop: stop };
    }

    function bindStepperToElement(selector, delta, onStep, button) {
        var input = document.querySelector(selector);
        if (!input || !button) return;
        var stepper = createStepper({ onStep: function (d) { onStep(input, d); } });
        button.__stepper = stepper;
        button.__stepperDelta = delta;
    }

    function initWheelSupport() {
        document.addEventListener('wheel', function (e) {
            var container = e.target.closest('.rating-input-with-stepper, .amount-input-with-stepper');
            if (!container) return;

            var input = container.querySelector('input[type="number"], input[type="text"]');
            if (!input) return;

            e.preventDefault();

            var delta = e.deltaY < 0 ? 0.1 : -0.1;
            var currentValue = parseFloat(input.value.replace(',', '.'));
            if (isNaN(currentValue)) currentValue = 0;

            var newValue = Math.round((currentValue + delta) * 10) / 10;
            var min = parseFloat(input.getAttribute('min'));
            var max = parseFloat(input.getAttribute('max'));

            if (!isNaN(min) && newValue < min) newValue = min;
            if (!isNaN(max) && newValue > max) newValue = max;
            if (isNaN(min) && newValue < 0) newValue = 0;

            input.value = newValue.toFixed(1);

            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        }, { passive: false });
    }

    window.createStepper = createStepper;
    window.bindStepperToElement = bindStepperToElement;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initWheelSupport, { once: true });
    } else {
        initWheelSupport();
    }
})();