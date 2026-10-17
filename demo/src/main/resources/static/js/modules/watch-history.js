(function () {
    'use strict';

    function getCsrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;
        var input = document.querySelector('input[name="_csrf"]');
        if (input && input.value) return input.value;
        var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        if (match) return decodeURIComponent(match[1]);
        return null;
    }

    function csrfHeaders() {
        var token = getCsrfToken();
        var headers = { 'X-Requested-With': 'XMLHttpRequest' };
        if (token) headers['X-CSRF-TOKEN'] = token;
        return headers;
    }

    var _pendingHistoryId = null;
    var _watchInFlight = 0;

    var ICON_CALENDAR = '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>';
    var ICON_TAG = '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z"/><path stroke-linecap="round" stroke-linejoin="round" d="M6 6h.008v.008H6V6Z"/></svg>';
    var ICON_VIDEO = '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>';
    var ICON_STAR = '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z"/></svg>';

    function pad2(n) { return String(n).padStart(2, '0'); }
    function toIso(date) { return date.toISOString(); }

    function formatRatingText(value) {
        if (value === null || value === undefined || value === '') return '';
        var n = parseFloat(String(value).replace(',', '.'));
        if (isNaN(n)) return String(value);
        return n.toFixed(1).replace('.', ',');
    }

    function findWatchedContainer() {
        return document.querySelector('[data-watched-container]') ||
               document.querySelector('.watched-container') ||
               document.querySelector('.history-container');
    }

    function ensureWatchedContainer() {
        var container = findWatchedContainer();
        if (container) return container;
        var page = document.querySelector('.watched-page') || document.body;
        var emptyState = page.querySelector('[data-watched-empty]');
        var host = emptyState && emptyState.parentNode ? emptyState.parentNode : page;
        container = document.createElement('div');
        container.className = 'history-container watched-container';
        container.setAttribute('data-watched-container', '');
        container.setAttribute('data-group-history', '');
        container.setAttribute('data-cursor-container', '');
        container.setAttribute('data-cursor-endpoint', '/watched/more');
        container.setAttribute('data-cursor-template', 'historyItemTemplate');
        container.setAttribute('data-cursor-sort', 'desc');
        container.setAttribute('data-cursor-has-more', 'false');
        var loader = document.createElement('div');
        loader.className = 'cursor-loader';
        loader.setAttribute('data-cursor-loader', '');
        container.appendChild(loader);
        if (emptyState && emptyState.parentNode) host.insertBefore(container, emptyState);
        else host.appendChild(container);
        return container;
    }

    function hideWatchedEmptyState() {
        var empty = document.querySelector('[data-watched-empty]');
        if (empty) empty.style.display = 'none';
    }

    function getContainerSort(container) {
        var s = (container.getAttribute('data-cursor-sort') || 'desc').toLowerCase();
        return s === 'asc' ? 'asc' : 'desc';
    }

    function extractRating(sourceItem) {
        if (!sourceItem) return null;
        var el = sourceItem.querySelector('.history-item-meta-row--rating .history-item-meta-value');
        if (el) {
            var t = (el.textContent || '').trim();
            if (t) return t.replace(/\s*\/\s*10\s*$/, '');
        }
        return null;
    }

    function extractGenre(sourceItem) {
        if (!sourceItem) return '';
        var el = sourceItem.querySelector('.history-item-meta-row--genre .history-item-meta-value');
        if (el) return (el.textContent || '').trim();
        return '';
    }

    function extractYear(sourceItem) {
        if (!sourceItem) return '';
        var el = sourceItem.querySelector('.history-item-meta-row--year .history-item-meta-value');
        if (el) {
            var t = (el.textContent || '').trim();
            var m = t.match(/\d{4}/);
            if (m) return m[0];
        }
        return '';
    }

    function extractDirector(sourceItem) {
        if (!sourceItem) return '';
        var el = sourceItem.querySelector('.history-item-meta-row--director .history-item-meta-value');
        if (el) return (el.textContent || '').trim();
        return '';
    }

    function extractAgeRating(sourceItem) {
        if (!sourceItem) return '';
        var el = sourceItem.querySelector('.history-item-meta-row--age .history-item-meta-value');
        if (el) return (el.textContent || '').replace(/\+/g, '').trim();
        return '';
    }

    function buildMetaRow(cls, iconSvg, labelText, valueText) {
        var row = document.createElement('div');
        row.className = 'history-item-meta-row ' + cls;
        var icon = document.createElement('span');
        icon.className = 'history-item-meta-icon';
        icon.innerHTML = iconSvg;
        row.appendChild(icon);
        var label = document.createElement('span');
        label.className = 'history-item-meta-label';
        label.textContent = labelText;
        row.appendChild(label);
        var value = document.createElement('span');
        value.className = 'history-item-meta-value';
        value.textContent = valueText;
        value.setAttribute('title', valueText);
        row.appendChild(value);
        return row;
    }

    function buildHistoryItem(movieId, movieTitle, posterUrl, genre, year, director, ageRating, rating, date) {
        var template = document.getElementById('historyItemTemplate');
        if (!template) return document.createElement('div');

        var item = template.content.firstElementChild.cloneNode(true);
        var hasPoster = posterUrl && posterUrl !== 'null' && posterUrl !== '';

        item.setAttribute('data-movie-id', movieId);
        item.setAttribute('data-movie-title', movieTitle || '');
        item.setAttribute('data-poster-url', posterUrl || '');
        item.setAttribute('data-added-at', toIso(date));
        item.setAttribute('data-history-id', '');

        var posterContainer = item.querySelector('.history-item-poster');
        if (posterContainer) {
            posterContainer.setAttribute('data-movie-id', movieId);
            posterContainer.setAttribute('data-movie-title', movieTitle || '');
            posterContainer.setAttribute('data-poster-url', posterUrl || '');
        }

        var posterImg = item.querySelector('.history-item-poster img');
        var noPoster = item.querySelector('.history-item-poster .no-poster');
        if (hasPoster) {
            if (posterImg) {
                posterImg.setAttribute('data-poster-img', '');
                posterImg.src = posterUrl;
                posterImg.onclick = function () { window.location.href = '/movies/' + movieId; };
                posterImg.addEventListener('error', function () {
                    posterImg.style.display = 'none';
                    if (noPoster) noPoster.style.display = 'flex';
                    var overlayEl = item.querySelector('.poster-overlay');
                    if (overlayEl) overlayEl.style.display = 'none';
                }, { once: true });
            }
            if (noPoster) noPoster.style.display = 'none';
        } else {
            if (posterImg) posterImg.style.display = 'none';
            if (noPoster) {
                noPoster.style.display = 'flex';
                noPoster.onclick = function () { window.location.href = '/movies/' + movieId; };
            }
        }

        var overlay = item.querySelector('.poster-overlay');
        if (overlay) overlay.style.display = hasPoster ? '' : 'none';

        var watchBtn = item.querySelector('.watch-btn-neon');
        if (watchBtn) {
            watchBtn.setAttribute('data-movie-id', movieId);
            watchBtn.setAttribute('data-movie-title', movieTitle || '');
            watchBtn.setAttribute('data-poster-url', posterUrl || '');
            watchBtn.setAttribute('data-watched', 'true');
        }

        var infoBlock = item.querySelector('.history-item-info');
        if (infoBlock) infoBlock.onclick = function () { window.location.href = '/movies/' + movieId; };

        var titleLink = item.querySelector('.history-item-title');
        if (titleLink) {
            titleLink.href = '/movies/' + movieId;
            titleLink.textContent = movieTitle || '';
        }

        var metaBlock = item.querySelector('.history-item-meta');
        if (metaBlock) {
            metaBlock.innerHTML = '';
            if (director) metaBlock.appendChild(buildMetaRow('history-item-meta-row--director', ICON_VIDEO, 'Режиссёр:', director));
            if (year) metaBlock.appendChild(buildMetaRow('history-item-meta-row--year', ICON_CALENDAR, 'Год создания:', String(year)));
            if (genre) metaBlock.appendChild(buildMetaRow('history-item-meta-row--genre', ICON_TAG, 'Жанр:', genre));
            if (ageRating) metaBlock.appendChild(buildMetaRow('history-item-meta-row--age', ICON_CALENDAR, 'Возрастной рейтинг:', String(ageRating) + '+'));
            if (rating) metaBlock.appendChild(buildMetaRow('history-item-meta-row--rating', ICON_STAR, 'Рейтинг:', formatRatingText(rating) + ' / 10'));
        }

        var timeSpan = item.querySelector('.history-item-time span');
        if (timeSpan) {
            timeSpan.textContent = 'Просмотрено в ' + pad2(date.getHours()) + ':' + pad2(date.getMinutes());
        }

        return item;
    }

    function applyOptimisticInLibrary(card) {
        var wasWatched = card.classList.contains('watched');
        card.classList.add('watched');
        var btn = card.querySelector('.watch-btn-neon');
        var prevWatched = btn ? btn.getAttribute('data-watched') : null;
        if (btn) btn.setAttribute('data-watched', 'true');
        return function rollback() {
            if (!wasWatched) card.classList.remove('watched');
            if (btn && !wasWatched) {
                if (prevWatched !== null) btn.setAttribute('data-watched', prevWatched);
                else btn.removeAttribute('data-watched');
            }
        };
    }

    function insertIntoTodayGroup(container, node) {
        var sort = getContainerSort(container);
        if (sort === 'asc') {
            var loader = container.querySelector('[data-cursor-loader]');
            var sentinel = container.querySelector('[data-cursor-sentinel]');
            if (loader && loader.parentNode === container) container.insertBefore(node, loader);
            else if (sentinel && sentinel.parentNode === container) container.insertBefore(node, sentinel);
            else container.appendChild(node);
            return;
        }

        var todayGroup = null;
        var groups = container.querySelectorAll('.history-day-group');
        for (var i = 0; i < groups.length; i++) {
            var label = groups[i].querySelector('.day-label');
            if (label && label.textContent.trim() === 'Сегодня') {
                todayGroup = groups[i];
                break;
            }
        }

        if (todayGroup) {
            var firstItem = todayGroup.querySelector('.history-item');
            if (firstItem) {
                todayGroup.insertBefore(node, firstItem);
            } else {
                todayGroup.appendChild(node);
            }
        } else {
            var newGroup = document.createElement('div');
            newGroup.className = 'history-day-group';
            var now = new Date();
            var dateKey = now.getFullYear() + '-' + pad2(now.getMonth() + 1) + '-' + pad2(now.getDate());
            newGroup.setAttribute('data-date', dateKey);

            var groupLabel = document.createElement('div');
            groupLabel.className = 'history-day-label';
            groupLabel.innerHTML = ICON_CALENDAR + '<span class="day-label">Сегодня</span>';
            newGroup.appendChild(groupLabel);
            newGroup.appendChild(node);

            var firstGroup = container.querySelector('.history-day-group');
            if (firstGroup) {
                container.insertBefore(newGroup, firstGroup);
            } else {
                var topLoader = container.querySelector('[data-cursor-loader]');
                var topSentinel = container.querySelector('[data-cursor-sentinel]');
                if (topLoader && topLoader.parentNode === container) container.insertBefore(newGroup, topLoader);
                else if (topSentinel && topSentinel.parentNode === container) container.insertBefore(newGroup, topSentinel);
                else container.appendChild(newGroup);
            }
        }
    }

    function collectMovieContext(button) {
        var movieId = button && button.getAttribute('data-movie-id');
        var card = null;
        if (button) {
            card = button.closest('.history-item') || button.closest('.library-card');
        }
        if (!card && movieId) {
            card = document.querySelector('.history-item[data-movie-id="' + CSS.escape(movieId) + '"]') ||
                   document.querySelector('.library-card[data-movie-id="' + CSS.escape(movieId) + '"]');
        }
        return {
            genre: extractGenre(card),
            year: extractYear(card),
            director: extractDirector(card),
            ageRating: extractAgeRating(card),
            rating: extractRating(card)
        };
    }

    function watchMovie(button) {
        if (!isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error');
            return Promise.resolve();
        }

        var movieId = button.getAttribute('data-movie-id');
        var posterUrl = button.getAttribute('data-poster-url') || '';
        var movieTitle = button.getAttribute('data-movie-title') || '';
        if (!movieId) return Promise.resolve();

        var libraryCard = button.closest('.library-card');
        var rollbackLibrary = null;
        var newItem = null;
        var container = null;

        if (libraryCard) {
            rollbackLibrary = applyOptimisticInLibrary(libraryCard);
        } else {
            var context = collectMovieContext(button);
            container = findWatchedContainer() || ensureWatchedContainer();
            var now = new Date();

            newItem = buildHistoryItem(
                String(movieId),
                movieTitle,
                posterUrl,
                context.genre,
                context.year,
                context.director,
                context.ageRating,
                context.rating,
                now
            );

            insertIntoTodayGroup(container, newItem);
            hideWatchedEmptyState();
        }

        button.disabled = true;
        _watchInFlight++;

        return fetch('/watched/add/' + encodeURIComponent(movieId), {
            method: 'POST',
            headers: csrfHeaders(),
            keepalive: true
        })
        .then(handleResponse)
        .then(function (data) {
            button.disabled = false;

            if (posterUrl && posterUrl.trim() !== '') {
                window.open(posterUrl, '_blank');
            } else {
                window.open('/movies/' + movieId, '_blank');
            }

            if (newItem && data && data.historyItemId) {
                newItem.setAttribute('data-history-id', String(data.historyItemId));
            }

            return data;
        })
        .catch(function (error) {
            button.disabled = false;

            if (rollbackLibrary) {
                rollbackLibrary();
            }

            if (newItem && newItem.parentNode) {
                newItem.parentNode.removeChild(newItem);
            }

            if (error && error.status === 401) {
                showFlashMessage('Авторизуйтесь!', 'error');
            } else {
                showFlashMessage((error && error.message) || 'Ошибка', 'error');
            }
            throw error;
        })
        .finally(function () {
            _watchInFlight--;
        });
    }

    function handlePosterClick(el) {
        var container = el.closest('.library-card') || el.closest('.history-item');
        if (!container) return;
        var watchBtn = container.querySelector('.watch-btn-neon');
        if (!watchBtn) return;
        watchMovie(watchBtn);
    }

    function toggleSaveHistory() {
        var toggle = document.getElementById('saveHistoryToggle');
        if (!toggle) return;
        var previousValue = window.saveHistoryEnabled;
        var newValue = toggle.checked;
        window.saveHistoryEnabled = newValue;

        fetch('/watched', {
            method: 'POST',
            headers: Object.assign(csrfHeaders(), { 'Content-Type': 'application/x-www-form-urlencoded' }),
            body: 'save=' + encodeURIComponent(newValue)
        })
        .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function (data) {
            sessionStorage.setItem('flashMessage', JSON.stringify({
                message: data.message || (newValue ? 'История включена' : 'История отключена'),
                type: 'success'
            }));
            window.location.reload();
        })
        .catch(function () {
            toggle.checked = previousValue;
            window.saveHistoryEnabled = previousValue;
            if (typeof showFlashMessage === 'function') showFlashMessage('Ошибка изменения настройки', 'error');
        });
    }

    function initSaveHistoryToggle() {
        var toggle = document.getElementById('saveHistoryToggle');
        if (!toggle) return;
        if (toggle.dataset.saveHistoryBound === '1') return;
        toggle.dataset.saveHistoryBound = '1';
        window.saveHistoryEnabled = toggle.checked;
        toggle.addEventListener('change', function () { toggleSaveHistory(); });
    }

    function toggleDropdown(button) {
        var menu = button.nextElementSibling;
        if (!menu) return;
        var isOpen = menu.classList.contains('show');
        document.querySelectorAll('.dropdown-menu.show').forEach(function (m) { m.classList.remove('show'); });
        if (!isOpen) menu.classList.add('show');
    }

    function closeAllDropdowns() {
        document.querySelectorAll('.dropdown-menu.show').forEach(function (m) { m.classList.remove('show'); });
    }

    function deleteHistoryItem(button) {
        var item = button.closest('.history-item');
        if (!item) return;
        var historyId = item.getAttribute('data-history-id');
        var title = item.getAttribute('data-movie-title') || '';
        if (!historyId) return;
        _pendingHistoryId = historyId;
        var titleEl = document.getElementById('deleteItemTitle');
        if (titleEl) titleEl.textContent = title;
        if (window.Modal && typeof Modal.openById === 'function') Modal.openById('watchedRemoveModal');
        closeAllDropdowns();
    }

    function closeDeleteItemModal() {
        if (window.Modal && typeof Modal.closeById === 'function') Modal.closeById('watchedRemoveModal');
        _pendingHistoryId = null;
    }

    function confirmDeleteHistoryItem() {
        if (!_pendingHistoryId) return;
        var historyId = _pendingHistoryId;
        var btn = document.getElementById('confirmDeleteItemBtn');
        if (btn) { btn.disabled = true; btn.textContent = 'Удаляем...'; }

        fetch('/watched/remove/' + encodeURIComponent(historyId), {
            method: 'POST',
            headers: csrfHeaders()
        })
        .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function (data) {
            var item = document.querySelector('.history-item[data-history-id="' + CSS.escape(historyId) + '"]');
            if (item) {
                var group = item.parentNode;
                item.style.transition = 'all .3s ease';
                item.style.opacity = '0';
                item.style.transform = 'translateX(-20px)';
                setTimeout(function () {
                    if (item.parentNode) item.parentNode.removeChild(item);
                    if (group && group.classList.contains('history-day-group')) {
                        if (!group.querySelector('.history-item')) group.remove();
                    }
                }, 300);
            }
            closeDeleteItemModal();
            if (typeof showFlashMessage === 'function') showFlashMessage(data.message || 'Запись удалена', 'success');
        })
        .catch(function (err) {
            closeDeleteItemModal();
            if (typeof showFlashMessage === 'function') showFlashMessage(err.message || 'Ошибка удаления', 'error');
        })
        .finally(function () {
            if (btn) { btn.disabled = false; btn.textContent = 'Удалить'; }
        });
    }

    function hasHistoryNow() {
        var btn = document.querySelector('[data-action="open-clear-history-modal"]');
        if (btn) {
            var attr = btn.getAttribute('data-has-history');
            if (attr === 'true') return true;
            if (attr === 'false') return false;
        }
        if (document.querySelector('.history-item')) return true;
        if (document.querySelector('.history-day-group')) return true;
        if (findWatchedContainer()) return true;
        return false;
    }

    function openClearHistoryModal() {
        if (window.Modal && typeof Modal.openById === 'function') Modal.openById('watchedClearModal');
    }

    function closeClearHistoryModal() {
        if (window.Modal && typeof Modal.closeById === 'function') Modal.closeById('watchedClearModal');
    }

    function confirmClearHistory() {
        if (!hasHistoryNow()) { closeClearHistoryModal(); return; }
        var btn = document.querySelector('#watchedClearModal [data-action="confirm-clear-history"]');
        if (btn) { btn.disabled = true; btn.textContent = 'Удаляем...'; }
        fetch('/watched/clear', { method: 'POST', headers: csrfHeaders() })
        .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function (data) {
            closeClearHistoryModal();
            sessionStorage.setItem('flashMessage', JSON.stringify({
                message: data.message || 'История просмотров очищена',
                type: 'success'
            }));
            window.location.reload();
        })
        .catch(function (err) {
            closeClearHistoryModal();
            if (typeof showFlashMessage === 'function') showFlashMessage(err.message || 'Ошибка очистки', 'error');
        })
        .finally(function () {
            if (btn) { btn.disabled = false; btn.textContent = 'Удалить всё'; }
        });
    }

    function registerWatchActions() {
        if (!window.Actions) return;
        window.Actions.register('watch-movie', function (el) { watchMovie(el); return true; });
        window.Actions.register('toggle-history-dropdown', function (el) { toggleDropdown(el); return true; });
        window.Actions.register('delete-history-item', function (el) { deleteHistoryItem(el); return true; });
        window.Actions.register('open-clear-history-modal', function () { openClearHistoryModal(); return true; });
        window.Actions.register('close-clear-history-modal', function () { closeClearHistoryModal(); return true; });
        window.Actions.register('confirm-clear-history', function () { confirmClearHistory(); return true; });
        window.Actions.register('close-delete-item-modal', function () { closeDeleteItemModal(); return true; });
    }

    function initWatchHistory() {
        initSaveHistoryToggle();

        var confirmBtn = document.getElementById('confirmDeleteItemBtn');
        if (confirmBtn) confirmBtn.addEventListener('click', confirmDeleteHistoryItem);

        document.addEventListener('click', function (e) {
            if (!e.target.closest('.history-item-actions')) closeAllDropdowns();
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeAllDropdowns();
                closeDeleteItemModal();
                closeClearHistoryModal();
            }
        });
    }

    window.watchMovie = watchMovie;
    window.handlePosterClick = handlePosterClick;
    window.toggleSaveHistory = toggleSaveHistory;
    window.toggleDropdown = toggleDropdown;
    window.deleteHistoryItem = deleteHistoryItem;
    window.closeDeleteItemModal = closeDeleteItemModal;
    window.confirmDeleteHistoryItem = confirmDeleteHistoryItem;
    window.openClearHistoryModal = openClearHistoryModal;
    window.closeClearHistoryModal = closeClearHistoryModal;
    window.confirmClearHistory = confirmClearHistory;
    window.refreshHistoryFromServer = function () {};

    if (window.Actions && typeof window.Actions.ready === 'function') {
        window.Actions.ready(registerWatchActions);
    } else {
        document.addEventListener('DOMContentLoaded', registerWatchActions, { once: true });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initWatchHistory, { once: true });
    } else {
        initWatchHistory();
    }
})();