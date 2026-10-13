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
    var _clearHistoryHasItems = false;

    var ICON_CALENDAR =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.8" stroke="currentColor">' +
        '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>' +
        '<line x1="16" y1="2" x2="16" y2="6"/>' +
        '<line x1="8" y1="2" x2="8" y2="6"/>' +
        '<line x1="3" y1="10" x2="21" y2="10"/>' +
        '</svg>';

    var ICON_TAG =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.8" stroke="currentColor">' +
        '<path stroke-linecap="round" stroke-linejoin="round" ' +
        'd="M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z"/>' +
        '<path stroke-linecap="round" stroke-linejoin="round" d="M6 6h.008v.008H6V6Z"/>' +
        '</svg>';

    var ICON_VIDEO =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.8" stroke="currentColor">' +
        '<path stroke-linecap="round" stroke-linejoin="round" ' +
        'd="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z"/>' +
        '</svg>';

    var ICON_STAR =
        '<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" ' +
        'stroke-width="1.8" stroke="currentColor">' +
        '<path stroke-linecap="round" stroke-linejoin="round" ' +
        'd="M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z"/>' +
        '</svg>';

    function watchMovie(button) {
        if (!isAuthenticated()) {
            showFlashMessage('Авторизуйтесь!', 'error');
            return Promise.resolve();
        }
        var movieId = button.getAttribute('data-movie-id');
        var posterUrl = button.getAttribute('data-poster-url') || '';
        var movieTitle = button.getAttribute('data-movie-title') || '';
        if (!movieId) return Promise.resolve();
        var rollback = applyOptimisticWatchUI(button, movieId, movieTitle, posterUrl);
        if (posterUrl && posterUrl.trim() !== '') {
            window.open(posterUrl, '_blank');
        } else {
            window.open('/movies/' + movieId, '_blank');
        }
        button.disabled = true;
        return fetch('/watched/add/' + encodeURIComponent(movieId), {
            method: 'POST',
            headers: csrfHeaders()
        })
        .then(handleResponse)
        .then(function (data) {
            button.disabled = false;
            var saved = data && data.saved === true;
            if (saved) return data;
            if (typeof rollback === 'function') rollback();
            return data;
        })
        .catch(function (error) {
            button.disabled = false;
            if (typeof rollback === 'function') rollback();
            if (error.status === 401) {
                showFlashMessage('Авторизуйтесь!', 'error');
            } else {
                showFlashMessage(error.message || 'Ошибка', 'error');
            }
            throw error;
        });
    }

    function applyOptimisticWatchUI(button, movieId, movieTitle, posterUrl) {
        var libraryCard = button.closest('.library-card');
        if (libraryCard) return applyOptimisticInLibrary(libraryCard);
        var historyItem = button.closest('.history-item');
        if (historyItem) return applyOptimisticInHistory(historyItem, movieId, movieTitle, posterUrl);
        return function () {};
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
                if (prevWatched !== null) {
                    btn.setAttribute('data-watched', prevWatched);
                } else {
                    btn.removeAttribute('data-watched');
                }
            }
        };
    }

    function getCurrentSortOrder() {
        var activeBtn = document.querySelector('#sortToggleGroup .sort-option.active');
        if (activeBtn) return activeBtn.getAttribute('data-order') || 'desc';
        return 'desc';
    }

    function extractRating(sourceItem) {
        if (!sourceItem) return null;

        var fromNew = sourceItem.querySelector('.history-item-meta-row--rating .history-item-meta-value');
        if (fromNew) {
            var t0 = (fromNew.textContent || '').trim();
            if (t0) return t0.replace(/\s*\/\s*10\s*$/, '');
        }

        var historyRating = sourceItem.querySelector('.history-item-rating span:last-child');
        if (historyRating) {
            var t = (historyRating.textContent || '').trim();
            if (t) return t;
        }

        var candidates = sourceItem.querySelectorAll(
            '.media-card-rating-badge .rating-value, ' +
            '.movie-card .js-rating-badge, ' +
            '.js-rating-badge, ' +
            '.js-rating-badge-max, ' +
            '.rating-value, ' +
            '.rating-value-bottom'
        );
        for (var i = 0; i < candidates.length; i++) {
            var raw = (candidates[i].textContent || '').replace(/[^\d.,]/g, '').trim();
            if (raw) return raw;
        }

        return null;
    }

    function extractGenre(sourceItem) {
        if (!sourceItem) return '';

        var fromNew = sourceItem.querySelector('.history-item-meta-row--genre .history-item-meta-value');
        if (fromNew) {
            var t0 = (fromNew.textContent || '').trim();
            if (t0) return t0;
        }

        var fromHistory = sourceItem.querySelector('.history-item-meta .genre');
        if (fromHistory) {
            var t = (fromHistory.textContent || '').trim();
            if (t) return t.replace(/^Жанр:\s*/i, '');
        }

        var fromGrid = sourceItem.querySelector('.badge.genre span:last-child, .badge.genre');
        if (fromGrid) {
            var g = (fromGrid.textContent || '').trim();
            if (g) return g.replace(/^Жанр:\s*/i, '');
        }

        var fromMedia = sourceItem.querySelector('.media-card-details span[title="Жанр"]');
        if (fromMedia) {
            var m = (fromMedia.textContent || '').trim();
            if (m) return m;
        }

        var infoItems = sourceItem.querySelectorAll('.info-item');
        for (var i = 0; i < infoItems.length; i++) {
            var label = infoItems[i].querySelector('.info-label');
            if (!label) continue;
            var labelText = (label.textContent || '').trim().toLowerCase();
            if (labelText.indexOf('жанр') !== -1) {
                var val = infoItems[i].querySelector('.info-value');
                if (val) return (val.textContent || '').trim();
            }
        }

        return '';
    }

    function extractYear(sourceItem) {
        if (!sourceItem) return '';

        var fromNew = sourceItem.querySelector('.history-item-meta-row--year .history-item-meta-value');
        if (fromNew) {
            var t0 = (fromNew.textContent || '').trim();
            if (t0) {
                var m0 = t0.match(/\d{4}/);
                if (m0) return m0[0];
            }
        }

        var metaSpans = sourceItem.querySelectorAll('.history-item-meta > span:not(.genre):not(.dot)');
        if (metaSpans.length > 0) {
            var t = (metaSpans[0].textContent || '').trim();
            if (t) {
                var tYear = t.replace(/^Год:\s*/i, '');
                var mY = tYear.match(/\d{4}/);
                if (mY) return mY[0];
            }
        }

        var fromGrid = sourceItem.querySelector('.badge.year span:last-child, .badge.year');
        if (fromGrid) {
            var g = (fromGrid.textContent || '').trim();
            if (g) {
                var gYear = g.replace(/^Год:\s*/i, '');
                var mG = gYear.match(/\d{4}/);
                if (mG) return mG[0];
            }
        }

        var fromMedia = sourceItem.querySelector('.media-card-details span[title="Год выпуска"]');
        if (fromMedia) {
            var m = (fromMedia.textContent || '').trim();
            if (m) return m;
        }

        var infoItems = sourceItem.querySelectorAll('.info-item');
        for (var i = 0; i < infoItems.length; i++) {
            var label = infoItems[i].querySelector('.info-label');
            if (!label) continue;
            var labelText = (label.textContent || '').trim().toLowerCase();
            if (labelText.indexOf('выпуск') !== -1 || labelText.indexOf('год') !== -1) {
                var val = infoItems[i].querySelector('.info-value');
                if (val) {
                    var year = (val.textContent || '').trim();
                    var m2 = year.match(/\d{4}/);
                    if (m2) return m2[0];
                }
            }
        }

        return '';
    }

    function extractDirector(sourceItem) {
        if (!sourceItem) return '';

        var fromNew = sourceItem.querySelector('.history-item-meta-row--director .history-item-meta-value');
        if (fromNew) {
            var t0 = (fromNew.textContent || '').trim();
            if (t0) return t0;
        }

        var metaSpans = sourceItem.querySelectorAll('.history-item-meta > span:not(.genre):not(.dot)');
        if (metaSpans.length > 1) {
            var t = (metaSpans[1].textContent || '').trim();
            if (t) return t.replace(/^Режиссёр:\s*/i, '');
        }

        var fromGrid = sourceItem.querySelector('.badge.director span:last-child, .badge.director');
        if (fromGrid) {
            var g = (fromGrid.textContent || '').trim();
            if (g) return g.replace(/^Режиссёр:\s*/i, '');
        }

        var libraryRow = sourceItem.querySelector('.library-info-row .library-info-value');
        if (libraryRow) {
            var lr = (libraryRow.textContent || '').trim();
            if (lr) return lr;
        }

        var infoItems = sourceItem.querySelectorAll('.info-item');
        for (var i = 0; i < infoItems.length; i++) {
            var label = infoItems[i].querySelector('.info-label');
            if (!label) continue;
            var labelText = (label.textContent || '').trim().toLowerCase();
            if (labelText.indexOf('режисс') !== -1) {
                var val = infoItems[i].querySelector('.info-value');
                if (val) return (val.textContent || '').trim();
            }
        }

        return '';
    }

    function buildMetaRow(cls, iconSvg, labelText, valueText) {
        var row = document.createElement('div');
        row.className = 'history-item-meta-row ' + cls;

        var iconWrap = document.createElement('span');
        iconWrap.className = 'history-item-meta-icon';
        iconWrap.innerHTML = iconSvg;
        row.appendChild(iconWrap);

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

    function applyOptimisticInHistory(sourceItem, movieId, movieTitle, posterUrl) {
        var container = document.querySelector('.history-container');
        var emptyState = document.querySelector('.empty-state');
        if (!container && emptyState) {
            emptyState.classList.remove('empty-state');
            emptyState.classList.add('history-container');
            emptyState.innerHTML = '';
            container = emptyState;
        }
        if (!container) return function () {};

        var genre = extractGenre(sourceItem);
        var year = extractYear(sourceItem);
        var director = extractDirector(sourceItem);
        var rating = extractRating(sourceItem);

        var now = new Date();
        var dateKey =
            now.getFullYear() + '-' +
            String(now.getMonth() + 1).padStart(2, '0') + '-' +
            String(now.getDate()).padStart(2, '0');

        var createdGroup = !container.querySelector('.history-day-group[data-date="' + dateKey + '"]');
        var group = getOrCreateHistoryGroup(container, dateKey, now);
        var newItem = buildHistoryItem(movieId, movieTitle, posterUrl, genre, year, director, rating, now);

        var currentOrder = getCurrentSortOrder();
        if (currentOrder === 'asc') {
            group.appendChild(newItem);
        } else {
            var firstItem = group.querySelector('.history-item');
            if (firstItem) group.insertBefore(newItem, firstItem);
            else group.appendChild(newItem);
        }

        sortHistoryGroups(container);

        return function rollback() {
            newItem.remove();
            if (createdGroup && !group.querySelector('.history-item')) group.remove();
            sortHistoryGroups(container);
            if (!container.querySelector('.history-item') && !container.querySelector('.history-day-group')) {
                container.classList.remove('history-container');
                container.classList.add('empty-state');
                container.innerHTML =
                    '<p>У вас пока нет истории просмотров</p>' +
                    '<a href="/" class="watch-btn-empty">Перейти в главное меню</a>';
            }
        };
    }

    function getOrCreateHistoryGroup(container, dateKey, date) {
        var group = container.querySelector('.history-day-group[data-date="' + dateKey + '"]');
        if (group) return group;
        group = document.createElement('div');
        group.className = 'history-day-group';
        group.setAttribute('data-date', dateKey);
        var label = document.createElement('div');
        label.className = 'history-day-label';
        label.innerHTML =
            ICON_CALENDAR +
            '<span class="day-label"></span>';
        var months = ['января','февраля','марта','апреля','мая','июня',
                      'июля','августа','сентября','октября','ноября','декабря'];
        label.querySelector('.day-label').textContent =
            String(date.getDate()).padStart(2, '0') + ' ' +
            months[date.getMonth()] + ' ' +
            date.getFullYear();
        group.appendChild(label);
        container.insertBefore(group, container.firstChild);
        return group;
    }

    function buildHistoryItem(movieId, movieTitle, posterUrl, genre, year, director, rating, date) {
        var template = document.getElementById('historyItemTemplate');
        if (!template) {
            return document.createElement('div');
        }

        var item = template.content.firstElementChild.cloneNode(true);
        var hasPoster = posterUrl && posterUrl !== 'null' && posterUrl !== '';

        item.setAttribute('data-movie-id', movieId);
        item.setAttribute('data-movie-title', movieTitle || '');
        item.setAttribute('data-poster-url', posterUrl || '');

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
                posterImg.src = posterUrl;
                posterImg.onclick = function () {
                    window.location.href = '/movies/' + movieId;
                };
            }
            if (noPoster) noPoster.style.display = 'none';
        } else {
            if (posterImg) posterImg.style.display = 'none';
            if (noPoster) {
                noPoster.style.display = 'flex';
                noPoster.onclick = function () {
                    window.location.href = '/movies/' + movieId;
                };
            }
        }

        var watchBtn = item.querySelector('.watch-btn-neon');
        if (watchBtn) {
            watchBtn.setAttribute('data-movie-id', movieId);
            watchBtn.setAttribute('data-movie-title', movieTitle || '');
            watchBtn.setAttribute('data-poster-url', posterUrl || '');
        }

        var infoBlock = item.querySelector('.history-item-info');
        if (infoBlock) {
            infoBlock.onclick = function () {
                window.location.href = '/movies/' + movieId;
            };
        }

        var titleLink = item.querySelector('.history-item-title');
        if (titleLink) {
            titleLink.href = '/movies/' + movieId;
            titleLink.textContent = movieTitle || '';
        }

        var metaBlock = item.querySelector('.history-item-meta');
        if (metaBlock) {
            metaBlock.innerHTML = '';

            if (director) {
                metaBlock.appendChild(
                    buildMetaRow(
                        'history-item-meta-row--director',
                        ICON_VIDEO,
                        'Режиссёр:',
                        director
                    )
                );
            }
            if (year) {
                metaBlock.appendChild(
                    buildMetaRow(
                        'history-item-meta-row--year',
                        ICON_CALENDAR,
                        'Год создания:',
                        String(year)
                    )
                );
            }
            if (genre) {
                metaBlock.appendChild(
                    buildMetaRow(
                        'history-item-meta-row--genre',
                        ICON_TAG,
                        'Жанр:',
                        genre
                    )
                );
            }
            if (rating) {
                metaBlock.appendChild(
                    buildMetaRow(
                        'history-item-meta-row--rating',
                        ICON_STAR,
                        'Рейтинг:',
                        String(rating) + ' / 10'
                    )
                );
            }
        }

        var timeSpan = item.querySelector('.history-item-time span');
        if (timeSpan) {
            timeSpan.textContent =
                'Просмотрено в ' +
                String(date.getHours()).padStart(2, '0') + ':' +
                String(date.getMinutes()).padStart(2, '0');
        }

        item.style.opacity = '0';
        item.style.transform = 'translateY(-8px)';
        item.style.transition = 'opacity .35s ease, transform .35s ease';
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                item.style.opacity = '1';
                item.style.transform = 'translateY(0)';
            });
        });

        return item;
    }

    function sortHistoryGroups(container) {
        if (!container) return;
        try {
            var currentOrder = getCurrentSortOrder();
            var groups = Array.prototype.slice.call(container.querySelectorAll(':scope > .history-day-group'));
            groups.sort(function (a, b) {
                var da = a.getAttribute('data-date') || '';
                var db = b.getAttribute('data-date') || '';
                return currentOrder === 'asc' ? da.localeCompare(db) : db.localeCompare(da);
            });
            groups.forEach(function (g) { container.appendChild(g); });
        } catch (e) {}
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
            headers: Object.assign(csrfHeaders(), {
                'Content-Type': 'application/x-www-form-urlencoded'
            }),
            body: 'save=' + encodeURIComponent(newValue)
        })
        .then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        })
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
            if (typeof showFlashMessage === 'function') {
                showFlashMessage('Ошибка изменения настройки', 'error');
            }
        });
    }

    function initSaveHistoryToggle() {
        var toggle = document.getElementById('saveHistoryToggle');
        if (!toggle) return;
        if (toggle.dataset.saveHistoryBound === '1') return;
        toggle.dataset.saveHistoryBound = '1';

        window.saveHistoryEnabled = toggle.checked;

        toggle.addEventListener('change', function () {
            toggleSaveHistory();
        });
    }

    function toggleDropdown(button) {
        var menu = button.nextElementSibling;
        if (!menu) return;
        var isOpen = menu.classList.contains('show');
        document.querySelectorAll('.dropdown-menu.show').forEach(function (m) {
            m.classList.remove('show');
        });
        if (!isOpen) menu.classList.add('show');
    }

    function closeAllDropdowns() {
        document.querySelectorAll('.dropdown-menu.show').forEach(function (m) {
            m.classList.remove('show');
        });
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
        if (window.Modal && typeof Modal.openById === 'function') {
            Modal.openById('watchedRemoveModal');
        }
        closeAllDropdowns();
    }

    function closeDeleteItemModal() {
        if (window.Modal && typeof Modal.closeById === 'function') {
            Modal.closeById('watchedRemoveModal');
        }
        _pendingHistoryId = null;
    }

    function confirmDeleteHistoryItem() {
        if (!_pendingHistoryId) return;
        var historyId = _pendingHistoryId;
        var btn = document.getElementById('confirmDeleteItemBtn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Удаляем...';
        }
        fetch('/watched/remove/' + encodeURIComponent(historyId), {
            method: 'POST',
            headers: csrfHeaders()
        })
        .then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        })
        .then(function (data) {
            var item = document.querySelector('.history-item[data-history-id="' + CSS.escape(historyId) + '"]');
            if (item) {
                item.style.transition = 'all .3s ease';
                item.style.opacity = '0';
                item.style.transform = 'translateX(-20px)';
                setTimeout(function () {
                    var group = item.closest('.history-day-group');
                    item.remove();
                    if (group && !group.querySelector('.history-item')) {
                        group.remove();
                    }
                    var container = document.querySelector('.history-container');
                    if (container && !container.querySelector('.history-item')) {
                        var empty = document.querySelector('.empty-state');
                        if (empty) empty.style.display = '';
                    }
                }, 300);
            }
            closeDeleteItemModal();
            if (typeof showFlashMessage === 'function') {
                showFlashMessage(data.message || 'Запись удалена', 'success');
            }
        })
        .catch(function (err) {
            closeDeleteItemModal();
            if (typeof showFlashMessage === 'function') {
                showFlashMessage(err.message || 'Ошибка удаления', 'error');
            }
        })
        .finally(function () {
            if (btn) {
                btn.disabled = false;
                btn.textContent = 'Удалить';
            }
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
        if (document.querySelector('.history-container')) return true;
        return false;
    }

    function openClearHistoryModal() {
        _clearHistoryHasItems = hasHistoryNow();
        if (window.Modal && typeof Modal.openById === 'function') {
            Modal.openById('watchedClearModal');
        }
    }

    function closeClearHistoryModal() {
        if (window.Modal && typeof Modal.closeById === 'function') {
            Modal.closeById('watchedClearModal');
        }
    }

    function confirmClearHistory() {
        if (!hasHistoryNow()) {
            closeClearHistoryModal();
            return;
        }

        var btn = document.querySelector('#watchedClearModal [data-action="confirm-clear-history"]');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Удаляем...';
        }
        fetch('/watched/clear', {
            method: 'POST',
            headers: csrfHeaders()
        })
        .then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        })
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
            if (typeof showFlashMessage === 'function') {
                showFlashMessage(err.message || 'Ошибка очистки', 'error');
            }
        })
        .finally(function () {
            if (btn) {
                btn.disabled = false;
                btn.textContent = 'Удалить всё';
            }
        });
    }

    function registerWatchActions() {
        if (!window.Actions) {
            if (window.console) console.error('[Watch] Actions core not loaded');
            return;
        }

        window.Actions.register('watch-movie', function (el) {
            watchMovie(el);
            return true;
        });

        window.Actions.register('toggle-history-dropdown', function (el) {
            toggleDropdown(el);
            return true;
        });

        window.Actions.register('delete-history-item', function (el) {
            deleteHistoryItem(el);
            return true;
        });

        window.Actions.register('open-clear-history-modal', function () {
            openClearHistoryModal();
            return true;
        });

        window.Actions.register('close-clear-history-modal', function () {
            closeClearHistoryModal();
            return true;
        });

        window.Actions.register('confirm-clear-history', function () {
            confirmClearHistory();
            return true;
        });

        window.Actions.register('close-delete-item-modal', function () {
            closeDeleteItemModal();
            return true;
        });
    }

    function initWatchHistory() {
        initSaveHistoryToggle();

        var confirmBtn = document.getElementById('confirmDeleteItemBtn');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', confirmDeleteHistoryItem);
        }

        document.addEventListener('click', function (e) {
            if (!e.target.closest('.history-item-actions')) {
                closeAllDropdowns();
            }
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