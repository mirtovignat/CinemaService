(function () {
    'use strict';

    var SERVER_FLASH_ATTR = 'data-server-flash';
    var JS_FLASH_ATTR = 'data-js-flash';

    var ICONS = {
        success:
            '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
            'stroke-width="1.5" stroke="currentColor" aria-hidden="true">' +
            '<path stroke-linecap="round" stroke-linejoin="round" ' +
            'd="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>',
        error:
            '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
            'stroke-width="1.5" stroke="currentColor" aria-hidden="true">' +
            '<path stroke-linecap="round" stroke-linejoin="round" ' +
            'd="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>',
        warning:
            '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
            'stroke-width="1.5" stroke="currentColor" aria-hidden="true">' +
            '<path stroke-linecap="round" stroke-linejoin="round" ' +
            'd="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"/></svg>',
        info:
            '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
            'stroke-width="1.5" stroke="currentColor" aria-hidden="true">' +
            '<path stroke-linecap="round" stroke-linejoin="round" ' +
            'd="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z"/></svg>'
    };

    function dismiss(el, delay) {
        if (!el || !el.parentNode) return;
        setTimeout(function () {
            if (!el.parentNode) return;
            el.setAttribute('data-leaving', 'true');
            el.style.animation = 'flashVanish 0.3s ease forwards';
            setTimeout(function () { if (el.parentNode) el.remove(); }, 320);
        }, delay);
    }

    function appendAction(body, action) {
        if (!action || !action.url || !action.text) return;
        var link = document.createElement('a');
        link.href = action.url;
        link.className = 'flash-action-link';
        link.textContent = action.text;
        body.appendChild(link);
    }

    function showFlashMessage(message, type, autoHide, action) {
        if (!message || message === 'undefined') return;

        var container = document.getElementById('flash-messages-container');
        if (!container) {
            console.log('[' + (type || 'info').toUpperCase() + ']: ' + message);
            return;
        }

        container.querySelectorAll('[' + JS_FLASH_ATTR + ']:not([data-leaving])').forEach(function (old) {
            old.setAttribute('data-leaving', 'true');
            old.style.pointerEvents = 'none';
            old.style.animation = 'flashVanish 0.3s ease forwards';
            setTimeout(function () { if (old.parentNode) old.remove(); }, 320);
        });

        var div = document.createElement('div');
        div.className = 'flash-message ' + type;
        div.setAttribute(JS_FLASH_ATTR, 'true');

        var iconSvg = ICONS[type] || null;
        if (iconSvg) {
            var iconSpan = document.createElement('span');
            iconSpan.className = 'flash-icon';
            iconSpan.innerHTML = iconSvg;
            div.appendChild(iconSpan);
        }

        var body = document.createElement('div');
        body.className = 'flash-message-body';

        var msgSpan = document.createElement('span');
        msgSpan.className = 'flash-text';
        msgSpan.textContent = message;
        body.appendChild(msgSpan);

        appendAction(body, action);
        div.appendChild(body);

        container.appendChild(div);

        var hideDelay = (autoHide === false)
            ? null
            : (typeof autoHide === 'number' ? autoHide : (type === 'subtle' ? 1600 : 2000));

        if (hideDelay !== null) {
            dismiss(div, hideDelay);
        }
    }

    function saveFlashAndRedirect(message, type, redirectUrl, action) {
        type = type || 'success';
        redirectUrl = redirectUrl || '/';
        if (message) {
            sessionStorage.setItem('flashMessage', JSON.stringify({
                message: message,
                type: type,
                action: action || null
            }));
        }
        window.location.href = redirectUrl;
    }

    function initServerFlash() {
        var container = document.getElementById('flash-messages-container');
        if (!container) return;

        var allServerMessages = Array.from(
            container.querySelectorAll('.flash-message:not([' + JS_FLASH_ATTR + '])')
        );
        if (allServerMessages.length === 0) return;

        allServerMessages.forEach(function (m) {
            m.setAttribute(SERVER_FLASH_ATTR, 'true');
        });

        var queue = allServerMessages.slice();
        var index = 0;

        queue.forEach(function (m, i) {
            if (i > 0) m.style.display = 'none';
        });

        function showNext() {
            var current = queue[index];
            if (!current) return;

            if (!current.parentNode || current.hasAttribute('data-leaving')) {
                index++;
                showNext();
                return;
            }

            current.style.display = '';

            setTimeout(function () {
                if (!current.parentNode || current.hasAttribute('data-leaving')) {
                    index++;
                    showNext();
                    return;
                }
                current.setAttribute('data-leaving', 'true');
                current.style.animation = 'flashVanish 0.3s ease forwards';
                setTimeout(function () {
                    if (current.parentNode) current.remove();
                    index++;
                    showNext();
                }, 320);
            }, 2000);
        }

        showNext();
    }

    window.showFlashMessage = showFlashMessage;
    window.saveFlashAndRedirect = saveFlashAndRedirect;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initServerFlash, { once: true });
    } else {
        initServerFlash();
    }
})();