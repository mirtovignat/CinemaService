(function () {
    'use strict';

    var form = document.getElementById('pwdForm');
    if (!form) return;

    var newError = document.getElementById('newPwdError');
    var repeatError = document.getElementById('repeatError');

    form.addEventListener('submit', function () {
        if (newError) newError.style.display = 'none';
        if (repeatError) repeatError.style.display = 'none';
    });

    document.querySelectorAll('.input-wrapper').forEach(function (wrapper) {
        var input = wrapper.querySelector('input');
        var clearBtn = wrapper.querySelector('.clear-input-btn');
        if (!input || !clearBtn) return;

        function syncHasValue() {
            wrapper.classList.toggle('has-value', input.value.length > 0);
        }

        clearBtn.addEventListener('click', function (e) {
            e.preventDefault();
            input.value = '';
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            input.focus();
            syncHasValue();
        });

        input.addEventListener('input', syncHasValue);
        input.addEventListener('change', syncHasValue);

        syncHasValue();
    });

    if (window.history && window.history.replaceState) {
        window.history.replaceState(null, document.title, window.location.href);
    }
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });
})();