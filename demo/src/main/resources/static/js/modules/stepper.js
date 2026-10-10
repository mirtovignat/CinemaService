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

    window.createStepper = createStepper;
    window.bindStepperToElement = bindStepperToElement;
})();