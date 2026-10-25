(function () {
    'use strict';

    var STORAGE_KEY = 'cinema_accounts';
    var MAX_ACCOUNTS = 5;

    var SPRING = 'cubic-bezier(0.34, 1.56, 0.64, 1)';

    var REMOVE_ICON =
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" ' +
        'fill="none" stroke="currentColor" stroke-width="1.8" ' +
        'stroke-linecap="round" aria-hidden="true">' +
        '<path d="M7 7l10 10M17 7L7 17"/></svg>';

    var dragState = null;

    function activeUsername() {
        var currentUserDataElement = document.getElementById('currentUserData');
        return currentUserDataElement && currentUserDataElement.dataset.username
            ? currentUserDataElement.dataset.username
            : '';
    }

    function read() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return [];
            var parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function write(accountList) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(accountList.slice(0, MAX_ACCOUNTS)));
        } catch (error) {}
    }

    function firstLetter(username) {
        if (!username) return '?';
        return username.trim().charAt(0).toUpperCase() || '?';
    }

    function upsert(username) {
        if (!username) return;
        var accountList = read();
        var lowerUsername = username.toLowerCase();
        var existingIndex = accountList.findIndex(function (account) {
            return account.username.toLowerCase() === lowerUsername;
        });
        accountList.forEach(function (account) { account.active = false; });
        var entry = {
            username: username,
            avatarLetter: firstLetter(username),
            active: true
        };
        if (existingIndex !== -1) accountList[existingIndex] = entry;
        else accountList.unshift(entry);
        write(accountList);
    }

    function rename(oldUsername, newUsername) {
        if (!oldUsername || !newUsername) return;
        if (oldUsername === newUsername) return;

        var accountList = read();
        var oldLowerUsername = oldUsername.toLowerCase();
        var newLowerUsername = newUsername.toLowerCase();

        var entry = accountList.find(function (account) {
            return account.username.toLowerCase() === oldLowerUsername;
        });

        if (!entry) {
            entry = accountList.find(function (account) { return account.active; });
        }

        if (!entry) {
            upsert(newUsername);
            render();
            return;
        }

        var duplicate = accountList.find(function (account) {
            return account !== entry && account.username.toLowerCase() === newLowerUsername;
        });

        if (duplicate) {
            accountList = accountList.filter(function (account) { return account !== duplicate; });
        }

        entry.username = newUsername;
        entry.avatarLetter = firstLetter(newUsername);

        accountList.forEach(function (account) {
            account.active = (account === entry);
        });

        write(accountList);
        render();
    }

    function remove(username) {
        if (!username) return;
        if (username.toLowerCase() === activeUsername().toLowerCase()) return;
        var lowerUsername = username.toLowerCase();
        var accountList = read().filter(function (account) {
            return account.username.toLowerCase() !== lowerUsername;
        });
        write(accountList);
    }

    function removeCurrent() {
        var currentUsername = activeUsername();
        if (!currentUsername) return;

        var accountList = read();
        var lowerUsername = currentUsername.toLowerCase();

        var filteredList = accountList.filter(function (account) {
            return account.username.toLowerCase() !== lowerUsername;
        });

        write(filteredList);
        render();
    }

    function clearAllExceptActive() {
        var currentUsername = activeUsername();
        if (!currentUsername) {
            write([]);
            return;
        }
        var accountList = read().filter(function (account) {
            return account.username.toLowerCase() === currentUsername.toLowerCase();
        });
        if (accountList.length === 0) {
            accountList = [{
                username: currentUsername,
                avatarLetter: firstLetter(currentUsername),
                active: true
            }];
        } else {
            accountList[0].active = true;
        }
        write(accountList);
    }

    function persistDomOrder() {
        var container = document.getElementById('accountSwitcher');
        if (!container) return;

        var rows = container.querySelectorAll('.account-switcher-item');
        var order = [];
        var activeItem = null;

        rows.forEach(function (row) {
            var username = row.getAttribute('data-username');
            if (!username) return;
            var original = read().find(function (account) {
                return account.username.toLowerCase() === username.toLowerCase();
            });
            if (!original) return;
            if (original.active) activeItem = original;
            else order.push(original);
        });

        if (activeItem) order.unshift(activeItem);
        if (order.length) write(order);
    }

    function captureRects(container) {
        var map = new Map();
        container.querySelectorAll('.account-switcher-item').forEach(function (element) {
            map.set(element, element.getBoundingClientRect());
        });
        return map;
    }

    function playFlip(container, firstRects, excludeElement) {
        container.querySelectorAll('.account-switcher-item').forEach(function (element) {
            if (element === excludeElement) return;
            var first = firstRects.get(element);
            if (!first) return;

            var last = element.getBoundingClientRect();
            var deltaX = first.left - last.left;
            var deltaY = first.top - last.top;

            if (Math.abs(deltaX) < 0.5 && Math.abs(deltaY) < 0.5) return;

            element.classList.add('is-flying');

            var animation = element.animate(
                [
                    { transform: 'translate(' + deltaX + 'px, ' + deltaY + 'px)' },
                    { transform: 'translate(0, 0)' }
                ],
                {
                    duration: 320,
                    easing: SPRING,
                    fill: 'none'
                }
            );

            animation.onfinish = function () {
                element.classList.remove('is-flying');
            };
        });
    }

    function findInsertTarget(container, cursorY) {
        var rows = Array.prototype.slice.call(
            container.querySelectorAll('.account-switcher-item:not(.is-dragging)')
        );

        var fromRow = dragState.row;

        var candidates = rows.filter(function (row) {
            return !row.classList.contains('active');
        });

        if (!candidates.length) return null;

        for (var index = 0; index < candidates.length; index++) {
            var row = candidates[index];
            var rect = row.getBoundingClientRect();

            if (cursorY < rect.top + rect.height / 2) {
                if (fromRow.nextElementSibling === row) {
                    return { row: row, before: true, noop: true };
                }
                return { row: row, before: true };
            }
        }

        var lastRow = candidates[candidates.length - 1];
        if (fromRow.previousElementSibling === lastRow) {
            return { row: lastRow, before: false, noop: true };
        }
        return { row: lastRow, before: false };
    }

    function handleDragMove(container, cursorY) {
        if (!dragState) return;

        var target = findInsertTarget(container, cursorY);
        if (!target) return;

        if (target.noop) {
            dragState.lastTarget = null;
            dragState.lastBefore = null;
            return;
        }

        var row = target.row;
        var before = target.before;

        if (dragState.lastTarget === row && dragState.lastBefore === before) {
            return;
        }
        dragState.lastTarget = row;
        dragState.lastBefore = before;

        var fromRow = dragState.row;
        var firstRects = captureRects(container);

        if (before) {
            container.insertBefore(fromRow, row);
        } else {
            container.insertBefore(fromRow, row.nextSibling);
        }

        playFlip(container, firstRects, fromRow);
    }

    function buildRow(account, index) {
        var row = document.createElement('a');
        row.className = 'account-switcher-item' + (account.active ? ' active' : '');
        row.href = account.active
            ? '/personal-account'
            : '/login?switch=' + encodeURIComponent(account.username);
        row.setAttribute('data-index', String(index));
        row.setAttribute('data-username', account.username);

        var avatarElement = document.createElement('span');
        avatarElement.className = 'account-switcher-avatar';
        avatarElement.textContent = account.avatarLetter;

        var nameElement = document.createElement('span');
        nameElement.className = 'account-switcher-name';
        nameElement.textContent = account.username;

        row.appendChild(avatarElement);
        row.appendChild(nameElement);

        if (account.active) {
            return row;
        }

        var removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'account-switcher-remove';
        removeButton.title = 'Удалить из списка';
        removeButton.setAttribute('aria-label', 'Удалить из списка');
        removeButton.innerHTML = REMOVE_ICON;
        removeButton.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            event.stopImmediatePropagation();
            remove(account.username);
            render();
        });
        row.appendChild(removeButton);

        row.setAttribute('draggable', 'true');

        row.addEventListener('dragstart', function (event) {
            dragState = {
                username: account.username,
                row: row,
                lastTarget: null,
                lastBefore: null
            };

            var container = row.parentNode;
            if (container) container.classList.add('is-dragging-active');

            requestAnimationFrame(function () {
                row.classList.add('is-dragging');
            });

            try {
                event.dataTransfer.effectAllowed = 'move';
                event.dataTransfer.setData('text/plain', account.username);
            } catch (error) {}
        });

        row.addEventListener('dragend', function () {
            var container = row.parentNode;
            row.classList.remove('is-dragging');

            if (container) {
                container.classList.remove('is-dragging-active');
            }

            dragState = null;
            persistDomOrder();
        });

        return row;
    }

    function bindContainerDrag(container) {
        if (container.dataset.dragBound === '1') return;
        container.dataset.dragBound = '1';

        container.addEventListener('dragover', function (event) {
            if (!dragState) return;
            event.preventDefault();

            try { event.dataTransfer.dropEffect = 'move'; } catch (error) {}

            handleDragMove(container, event.clientY);
        });

        container.addEventListener('drop', function (event) {
            if (!dragState) return;
            event.preventDefault();
            dragState = null;
            persistDomOrder();
        });

        container.addEventListener('dragleave', function (event) {
            if (container.contains(event.relatedTarget)) return;
            if (!dragState) return;
            dragState.lastTarget = null;
            dragState.lastBefore = null;
        });
    }

    function buildClearRow() {
        var clearRow = document.createElement('div');
        clearRow.className = 'account-switcher-clear';
        clearRow.setAttribute('role', 'button');
        clearRow.setAttribute('tabindex', '0');
        clearRow.innerHTML = '<span>Оставить только активный</span>';

        var pressed = false;
        var handled = false;

        function block(event) {
            event.preventDefault();
            event.stopPropagation();
            if (event.stopImmediatePropagation) event.stopImmediatePropagation();
        }

        function runClear() {
            if (handled) return;
            handled = true;
            setTimeout(function () {
                clearAllExceptActive();
                render();
                if (typeof window.showFlashMessage === 'function') {
                    window.showFlashMessage('Список неактивных аккаунтов очищен', 'subtle');
                }
                handled = false;
            }, 0);
        }

        clearRow.addEventListener('pointerdown', function (event) {
            block(event);
            pressed = true;
        }, true);

        clearRow.addEventListener('mousedown', function (event) {
            block(event);
            pressed = true;
        }, true);

        clearRow.addEventListener('pointerup', function (event) {
            block(event);
            if (pressed) {
                pressed = false;
                runClear();
            }
        }, true);

        clearRow.addEventListener('mouseup', function (event) {
            block(event);
            if (pressed) {
                pressed = false;
                runClear();
            }
        }, true);

        clearRow.addEventListener('click', function (event) {
            block(event);
            if (!handled) runClear();
        }, true);

        clearRow.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                block(event);
                runClear();
            }
        });

        clearRow.addEventListener('dragstart', function (event) {
            event.preventDefault();
        });

        return clearRow;
    }

    function render() {
        var container = document.getElementById('accountSwitcher');
        if (!container) return;

        var accountList = read();

        accountList.sort(function (firstAccount, secondAccount) {
            if (firstAccount.active && !secondAccount.active) return -1;
            if (!firstAccount.active && secondAccount.active) return 1;
            return 0;
        });

        container.textContent = '';

        if (accountList.length === 0) {
            container.style.display = 'none';
            return;
        }

        container.style.display = '';

        accountList.forEach(function (account, index) {
            container.appendChild(buildRow(account, index));
        });

        if (accountList.length > 1) {
            container.appendChild(buildClearRow());
        }

        bindContainerDrag(container);
    }

    function init() {
        var currentUsername = activeUsername();
        if (!currentUsername) {
            render();
            return;
        }

        var accountList = read();

        var existingIndex = accountList.findIndex(function (account) {
            return account.username.toLowerCase() === currentUsername.toLowerCase();
        });

        if (existingIndex === -1) {
            accountList.unshift({
                username: currentUsername,
                avatarLetter: firstLetter(currentUsername),
                active: true
            });
        }

        accountList.forEach(function (account) {
            account.active = account.username.toLowerCase() === currentUsername.toLowerCase();
        });

        write(accountList);
        render();
    }

    window.AccountSwitcher = {
        init: init,
        upsert: upsert,
        rename: rename,
        remove: remove,
        removeCurrent: removeCurrent,
        clearAllExceptActive: clearAllExceptActive,
        render: render,
        read: read
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (event) {
        if (event.persisted) init();
    });
})();