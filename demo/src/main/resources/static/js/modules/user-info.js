(function () {
    'use strict';

    var EMPTY_PHONE_TEXT = 'Телефон не указан';

    function getFirstLetter(name) {
        if (!name) return '?';
        return name.trim().charAt(0).toUpperCase() || '?';
    }

    function maskValue(raw) {
        if (!raw) return '';
        if (typeof window.maskPhone === 'function') {
            return window.maskPhone(raw);
        }
        return raw;
    }

    function updateHeader(user) {
        if (!user) return;
        document.querySelectorAll('.user-avatar-letter').forEach(function (el) {
            if (user.username !== undefined) {
                el.textContent = getFirstLetter(user.username);
            }
        });
        var dropName = document.querySelector('.user-dropdown-name > span');
        if (dropName && user.username !== undefined) {
            dropName.textContent = user.username;
        }
    }

    function updateSidebar(user) {
        if (!user) return;

        var avatarEl = document.getElementById('sidebarAvatar');
        if (avatarEl && user.username !== undefined) {
            avatarEl.textContent = getFirstLetter(user.username);
        }

        var nameEl = document.getElementById('sidebarName');
        if (nameEl) {
            var display = null;
            if (user.name) {
                display = user.name + (user.surname ? ' ' + user.surname.charAt(0) + '.' : '');
            } else if (user.username !== undefined) {
                display = user.username;
            }
            if (display !== null) nameEl.textContent = display;
        }

        var phoneEl = document.getElementById('sidebarPhone');
        if (phoneEl && user.phone !== undefined) {
            var rawPhone = user.phone || '';
            phoneEl.dataset.rawPhone = rawPhone;
            if (!rawPhone) {
                phoneEl.textContent = EMPTY_PHONE_TEXT;
                phoneEl.classList.add('phone-empty');
            } else {
                phoneEl.textContent = maskValue(rawPhone);
                phoneEl.classList.remove('phone-empty');
            }
        }

        var emailEl = document.getElementById('sidebarEmail');
        if (emailEl && user.email !== undefined) {
            emailEl.textContent = user.email || '';
        }
    }

    function updateAll(user) {
        if (!user) return;
        updateHeader(user);
        updateSidebar(user);
    }

    function maskSidebarPhoneOnLoad() {
        var phoneEl = document.getElementById('sidebarPhone');
        if (!phoneEl) return;
        var raw = (phoneEl.dataset.rawPhone || '').trim();
        if (!raw) {
            phoneEl.textContent = EMPTY_PHONE_TEXT;
            phoneEl.classList.add('phone-empty');
            return;
        }
        phoneEl.textContent = maskValue(raw);
        phoneEl.classList.remove('phone-empty');
    }

    function maskProfilePhoneOnLoad() {
        var phoneEl = document.getElementById('panelPhone');
        if (!phoneEl) return;

        var raw = (phoneEl.dataset.rawPhone || '').trim();
        var textHost = phoneEl.querySelector('.js-phone-text') || phoneEl;

        if (!raw) {
            phoneEl.classList.add('has-no-phone');
            if (textHost !== phoneEl) textHost.textContent = '';
            return;
        }

        phoneEl.classList.remove('has-no-phone');
        textHost.textContent = maskValue(raw);
    }

    function init() {
        maskSidebarPhoneOnLoad();
        maskProfilePhoneOnLoad();
    }

    window.updateSidebarAndHeader = updateAll;
    window.maskSidebarPhoneOnLoad = maskSidebarPhoneOnLoad;
    window.maskProfilePhoneOnLoad = maskProfilePhoneOnLoad;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }
})();