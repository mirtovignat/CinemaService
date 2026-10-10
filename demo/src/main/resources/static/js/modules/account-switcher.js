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
        var el = document.getElementById('currentUserData');
        return el && el.dataset.username ? el.dataset.username : '';
    }

    function read() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return [];
            var parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (e) {
            return [];
        }
    }

    function write(list) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(list.slice(0, MAX_ACCOUNTS)));
        } catch (e) {}
    }

    function firstLetter(value) {
        if (!value) return '?';
        return value.trim().charAt(0).toUpperCase() || '?';
    }

    function upsert(username) {
        if (!username) return;
        var list = read();
        var lower = username.toLowerCase();
        var index = list.findIndex(function (a) {
            return a.username.toLowerCase() === lower;
        });
        list.forEach(function (a) { a.active = false; });
        var entry = {
            username: username,
            avatarLetter: firstLetter(username),
            active: true
        };
        if (index !== -1) list[index] = entry;
        else list.unshift(entry);
        write(list);
    }

    function remove(username) {
        if (!username) return;
        if (username.toLowerCase() === activeUsername().toLowerCase()) return;
        var lower = username.toLowerCase();
        var list = read().filter(function (a) {
            return a.username.toLowerCase() !== lower;
        });
        write(list);
    }

    function clearAllExceptActive() {
        var current = activeUsername();
        if (!current) {
            write([]);
            return;
        }
        var list = read().filter(function (a) {
            return a.username.toLowerCase() === current.toLowerCase();
        });
        if (list.length === 0) {
            list = [{
                username: current,
                avatarLetter: firstLetter(current),
                active: true
            }];
        } else {
            list[0].active = true;
        }
        write(list);
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
            var original = read().find(function (a) {
                return a.username.toLowerCase() === username.toLowerCase();
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
        container.querySelectorAll('.account-switcher-item').forEach(function (el) {
            map.set(el, el.getBoundingClientRect());
        });
        return map;
    }

    function playFlip(container, firstRects, excludeEl) {
        container.querySelectorAll('.account-switcher-item').forEach(function (el) {
            if (el === excludeEl) return;
            var first = firstRects.get(el);
            if (!first) return;

            var last = el.getBoundingClientRect();
            var dx = first.left - last.left;
            var dy = first.top - last.top;

            if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) return;

            el.classList.add('is-flying');

            var anim = el.animate(
                [
                    { transform: 'translate(' + dx + 'px, ' + dy + 'px)' },
                    { transform: 'translate(0, 0)' }
                ],
                {
                    duration: 320,
                    easing: SPRING,
                    fill: 'none'
                }
            );

            anim.onfinish = function () {
                el.classList.remove('is-flying');
            };
        });
    }

    function findInsertTarget(container, cursorY) {
        var rows = Array.prototype.slice.call(
            container.querySelectorAll('.account-switcher-item:not(.is-dragging)')
        );

        var from = dragState.row;

        var candidates = rows.filter(function (r) {
            return !r.classList.contains('active');
        });

        if (!candidates.length) return null;

        for (var i = 0; i < candidates.length; i++) {
            var row = candidates[i];
            var rect = row.getBoundingClientRect();

            if (cursorY < rect.top + rect.height / 2) {
                if (from.nextElementSibling === row) {
                    return { row: row, before: true, noop: true };
                }
                return { row: row, before: true };
            }
        }

        var last = candidates[candidates.length - 1];
        if (from.previousElementSibling === last) {
            return { row: last, before: false, noop: true };
        }
        return { row: last, before: false };
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

        var from = dragState.row;
        var firstRects = captureRects(container);

        if (before) {
            container.insertBefore(from, row);
        } else {
            container.insertBefore(from, row.nextSibling);
        }

        playFlip(container, firstRects, from);
    }

    function buildRow(account, index) {
        var row = document.createElement('a');
        row.className = 'account-switcher-item' + (account.active ? ' active' : '');
        row.href = account.active
            ? '/personal-account'
            : '/login?switch=' + encodeURIComponent(account.username);
        row.setAttribute('data-index', String(index));
        row.setAttribute('data-username', account.username);

        var avatar = document.createElement('span');
        avatar.className = 'account-switcher-avatar';
        avatar.textContent = account.avatarLetter;

        var name = document.createElement('span');
        name.className = 'account-switcher-name';
        name.textContent = account.username;

        row.appendChild(avatar);
        row.appendChild(name);

        if (account.active) {
            return row;
        }

        var removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'account-switcher-remove';
        removeBtn.title = 'Удалить из списка';
        removeBtn.setAttribute('aria-label', 'Удалить из списка');
        removeBtn.innerHTML = REMOVE_ICON;
        removeBtn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            remove(account.username);
            render();
        });
        row.appendChild(removeBtn);

        row.setAttribute('draggable', 'true');

        row.addEventListener('dragstart', function (e) {
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
                e.dataTransfer.effectAllowed = 'move';
                e.dataTransfer.setData('text/plain', account.username);
            } catch (err) {}
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

        container.addEventListener('dragover', function (e) {
            if (!dragState) return;
            e.preventDefault();

            try { e.dataTransfer.dropEffect = 'move'; } catch (err) {}

            handleDragMove(container, e.clientY);
        });

        container.addEventListener('drop', function (e) {
            if (!dragState) return;
            e.preventDefault();
            dragState = null;
            persistDomOrder();
        });

        container.addEventListener('dragleave', function (e) {
            if (container.contains(e.relatedTarget)) return;
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

        function block(e) {
            e.preventDefault();
            e.stopPropagation();
            if (e.stopImmediatePropagation) e.stopImmediatePropagation();
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

        clearRow.addEventListener('pointerdown', function (e) {
            block(e);
            pressed = true;
        }, true);

        clearRow.addEventListener('mousedown', function (e) {
            block(e);
            pressed = true;
        }, true);

        clearRow.addEventListener('pointerup', function (e) {
            block(e);
            if (pressed) {
                pressed = false;
                runClear();
            }
        }, true);

        clearRow.addEventListener('mouseup', function (e) {
            block(e);
            if (pressed) {
                pressed = false;
                runClear();
            }
        }, true);

        clearRow.addEventListener('click', function (e) {
            block(e);
            if (!handled) runClear();
        }, true);

        clearRow.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                block(e);
                runClear();
            }
        });

        clearRow.addEventListener('dragstart', function (e) {
            e.preventDefault();
        });

        return clearRow;
    }

    function render() {
        var container = document.getElementById('accountSwitcher');
        if (!container) return;

        var list = read();

        list.sort(function (a, b) {
            if (a.active && !b.active) return -1;
            if (!a.active && b.active) return 1;
            return 0;
        });

        container.textContent = '';

        if (list.length === 0) {
            container.style.display = 'none';
            return;
        }

        container.style.display = '';

        list.forEach(function (account, index) {
            container.appendChild(buildRow(account, index));
        });

        if (list.length > 1) {
            container.appendChild(buildClearRow());
        }

        bindContainerDrag(container);
    }

    function init() {
        var current = activeUsername();
        if (current) upsert(current);
        render();
    }

    window.AccountSwitcher = {
        init: init,
        upsert: upsert,
        remove: remove,
        clearAllExceptActive: clearAllExceptActive,
        render: render,
        read: read
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, { once: true });
    } else {
        init();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) init();
    });
})();