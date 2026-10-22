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

    function getIcon(name) {
        var tpl = document.getElementById('js-icon-' + name);
        return tpl ? tpl.innerHTML : '';
    }

    function pad2(n) { return String(n).padStart(2, '0'); }
    function toIso(date) { return date.toISOString(); }

    function formatRatingText(value) {
        if (value === null || value === undefined || value === '') return '';
        var n = parseFloat(String(value).replace(',', '.'));
        if (isNaN(n)) return String(value);
        return n.toFixed(1).replace('.', ',');
    }

    function isHistoryEnabled() {
        if (typeof window.saveHistoryEnabled === 'boolean') {
            return window.saveHistoryEnabled;
        }
        var toggle = document.getElementById('saveHistoryToggle');
        if (toggle) {
            window.saveHistoryEnabled = !!toggle.checked;
            return window.saveHistoryEnabled;
        }
        return true;
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

    function buildMetaRow(cls, iconName, labelText, valueText) {
        var row = document.createElement('div');
        row.className = 'history-item-meta-row ' + cls;
        var icon = document.createElement('span');
        icon.className = 'history-item-meta-icon';
        icon.innerHTML = getIcon(iconName);
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
                posterImg.addEventListener('error', function () {
                    posterImg.style.display = 'none';
                    if (noPoster) noPoster.style.display = 'flex';
                }, { once: true });
            }
            if (noPoster) noPoster.style.display = 'none';
        } else {
            if (posterImg) posterImg.style.display = 'none';
            if (noPoster) {
                noPoster.style.display = 'flex';
            }
        }

        var watchBtn = item.querySelector('.watch-btn-neon');
        if (watchBtn) {
            watchBtn.setAttribute('data-movie-id', movieId);
            watchBtn.setAttribute('data-movie-title', movieTitle || '');
            watchBtn.setAttribute('data-poster-url', posterUrl || '');
            watchBtn.setAttribute('data-watched', 'true');
        }

        var titleLink = item.querySelector('.history-item-title');
        if (titleLink) {
            titleLink.href = '/movies/' + movieId;
            titleLink.textContent = movieTitle || '';
        }

        var metaBlock = item.querySelector('.history-item-meta');
        if (metaBlock) {
            metaBlock.innerHTML = '';
            if (director) metaBlock.appendChild(buildMetaRow('history-item-meta-row--director', 'video', 'Режиссёр:', director));
            if (year) metaBlock.appendChild(buildMetaRow('history-item-meta-row--year', 'calendar', 'Год создания:', String(year)));
            if (genre) metaBlock.appendChild(buildMetaRow('history-item-meta-row--genre', 'tag', 'Жанр:', genre));
            if (ageRating) metaBlock.appendChild(buildMetaRow('history-item-meta-row--age', 'calendar', 'Возрастной рейтинг:', String(ageRating) + '+'));
            if (rating) metaBlock.appendChild(buildMetaRow('history-item-meta-row--rating', 'star', 'Рейтинг:', formatRatingText(rating) + ' / 10'));
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
            groupLabel.innerHTML = getIcon('calendar') + '<span class="day-label">Сегодня</span>';
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

    function isPosterImageBroken(container) {
        if (!container) return false;
        var img = container.querySelector('img[data-poster-img]');
        if (!img) return false;
        if (!img.getAttribute('src')) return false;
        if (!img.complete) return false;
        return img.naturalWidth === 0;
    }

    function resolveWatchContext(button) {
        var card = button.closest('.movie-card, .library-card, .history-item, .cart-card, .favourites-card');
        var movieId = (card && card.getAttribute('data-movie-id')) ||
                      button.getAttribute('data-movie-id') ||
                      '';
        var movieTitle = (card && card.getAttribute('data-movie-title')) ||
                         button.getAttribute('data-movie-title') ||
                         '';
        var posterUrl = button.getAttribute('data-poster-url') ||
                        (card && card.getAttribute('data-poster-url')) ||
                        '';
        if (posterUrl === 'null' || posterUrl === 'undefined') posterUrl = '';
        posterUrl = String(posterUrl).trim();

        var posterContainer = button.closest('.history-item-poster, .poster-wrapper, .media-card-poster-link, .movie-poster-link') ||
                              (card && card.querySelector('.history-item-poster, .poster-wrapper, .media-card-poster-link, .movie-poster-link')) ||
                              card;

        return {
            card: card,
            movieId: movieId,
            movieTitle: movieTitle,
            posterUrl: posterUrl,
            posterContainer: posterContainer
        };
    }

    function redirectToDetails(movieId) {
        window.location.href = '/movies/' + encodeURIComponent(movieId);
    }

    function watchMovie(button) {
        if (!isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error');
            return Promise.resolve();
        }

        var ctx = resolveWatchContext(button);
        if (!ctx.movieId) return Promise.resolve();

        var hasPosterAttr = !!ctx.posterUrl;
        var broken = isPosterImageBroken(ctx.posterContainer);

        if (!hasPosterAttr || broken) {
            redirectToDetails(ctx.movieId);
            return Promise.resolve();
        }

        var historyEnabled = isHistoryEnabled();
        var libraryCard = ctx.card && ctx.card.classList && ctx.card.classList.contains('library-card')
            ? ctx.card
            : null;
        var rollbackLibrary = null;
        var newItem = null;
        var container = null;

        if (libraryCard && historyEnabled) {
            rollbackLibrary = applyOptimisticInLibrary(libraryCard);
        } else if (historyEnabled) {
            var existingContainer = findWatchedContainer();
            if (existingContainer) {
                var context = collectMovieContext(button);
                container = existingContainer;
                var now = new Date();

                newItem = buildHistoryItem(
                    String(ctx.movieId),
                    ctx.movieTitle,
                    ctx.posterUrl,
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
        }

        button.disabled = true;
        _watchInFlight++;

        return fetch('/watched/add/' + encodeURIComponent(ctx.movieId), {
            method: 'POST',
            headers: csrfHeaders(),
            keepalive: true
        })
        .then(handleResponse)
        .then(function (data) {
            button.disabled = false;

            if (newItem && data && data.historyItemId) {
                newItem.setAttribute('data-history-id', String(data.historyItemId));
            }

            window.location.href = ctx.posterUrl;

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
        if (!watchBtn) {
            var movieId = container.getAttribute('data-movie-id');
            if (movieId) redirectToDetails(movieId);
            return;
        }
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