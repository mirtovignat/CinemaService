(function () {
    'use strict';

    function moveDeleteModalToBody() {
        var modal = document.getElementById('deleteAccountModal');
        if (modal && modal.parentNode !== document.body) {
            document.body.appendChild(modal);
        }
    }

    function toggleSidebar() {
        var grid = document.querySelector('.profile-grid');
        if (!grid) return;
        grid.classList.toggle('sidebar-collapsed');
    }

    function toggleProfilePanel() {
        var mainContent = document.getElementById('mainContent');
        var profilePanel = document.getElementById('profilePanel');
        if (!mainContent || !profilePanel) return;

        var isHidden = mainContent.classList.contains('hidden');
        if (isHidden) {
            mainContent.classList.remove('hidden');
            profilePanel.classList.remove('active');
        } else {
            mainContent.classList.add('hidden');
            profilePanel.classList.add('active');
        }
    }

    function openDeleteAccountModal() {
        moveDeleteModalToBody();
        var modal = document.getElementById('deleteAccountModal');
        if (modal) modal.classList.add('show');
    }

    function closeDeleteAccountModal() {
        var modal = document.getElementById('deleteAccountModal');
        if (modal) modal.classList.remove('show');
    }

    function confirmDeleteAccount() {
        var form = document.getElementById('deleteAccountForm');
        if (!form) {
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Ошибка: форма удаления не найдена', 'error');
            }
            return;
        }

        if (window.AccountSwitcher && typeof window.AccountSwitcher.removeCurrent === 'function') {
            window.AccountSwitcher.removeCurrent();
        }

        var confirmButton = document.querySelector('#deleteAccountModal .modal-delete');
        if (confirmButton) {
            confirmButton.disabled = true;
            confirmButton.textContent = 'Удаляем...';
        }

        form.submit();
    }

    function registerSidebarActions() {
        if (!window.Actions) return;
        if (window.__sidebarActionsBound) return;
        window.__sidebarActionsBound = true;

        window.Actions.register('toggle-sidebar', function () {
            toggleSidebar();
            return true;
        });

        window.Actions.register('open-delete-account-modal', function () {
            openDeleteAccountModal();
            return true;
        });

        window.Actions.register('close-delete-account-modal', function () {
            closeDeleteAccountModal();
            return true;
        });

        window.Actions.register('confirm-delete-account', function () {
            confirmDeleteAccount();
            return true;
        });
    }

    function init() {
        moveDeleteModalToBody();
    }

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerSidebarActions);
    } else if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerSidebarActions, { once: true });
    } else {
        registerSidebarActions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (event) {
        if (event.persisted) moveDeleteModalToBody();
    });

    window.toggleSidebar = toggleSidebar;
    window.toggleProfilePanel = toggleProfilePanel;
    window.openDeleteAccountModal = openDeleteAccountModal;
    window.closeDeleteAccountModal = closeDeleteAccountModal;
    window.confirmDeleteAccount = confirmDeleteAccount;
})();