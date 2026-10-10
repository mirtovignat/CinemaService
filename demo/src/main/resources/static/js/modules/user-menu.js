(function () {
    'use strict';

    var MENU_SELECTOR = '#userMenu';

    function toggleUserMenu() {
        var menu = document.querySelector(MENU_SELECTOR);
        if (!menu) return;
        menu.classList.toggle('show');
    }

    function closeOnOutsideClick(e) {
        var menu = document.querySelector(MENU_SELECTOR);
        if (!menu) return;
        if (menu.contains(e.target)) return;
        if (e.target.closest('#userMenuToggle')) return;
        menu.classList.remove('show');
    }

    window.toggleUserMenu = toggleUserMenu;

    document.addEventListener('click', closeOnOutsideClick);
})();