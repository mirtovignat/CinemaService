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

    if (window.history && window.history.replaceState) {
        window.history.replaceState(null, document.title, window.location.href);
    }
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });
})();