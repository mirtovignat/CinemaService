(function () {
    'use strict';

    var form = document.getElementById('pwdForm');
    if (!form) return;

    var newPwd = document.getElementById('rawPassword');
    var repeatPwd = document.getElementById('repeatRawPassword');
    var newError = document.getElementById('newPwdError');
    var repeatError = document.getElementById('repeatError');

    form.addEventListener('submit', function (e) {
        if (newError) newError.style.display = 'none';
        if (repeatError) repeatError.style.display = 'none';

        var valid = true;

        if (newPwd.value.length < 8) {
            if (newError) {
                newError.textContent = 'Пароль должен быть не менее 8 символов';
                newError.style.display = 'block';
            }
            valid = false;
        }

        if (newPwd.value !== repeatPwd.value) {
            if (repeatError) {
                repeatError.textContent = 'Пароли не совпадают';
                repeatError.style.display = 'block';
            }
            valid = false;
        }

        if (!valid) {
            e.preventDefault();
        }
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