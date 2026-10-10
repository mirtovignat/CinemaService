(function () {
    'use strict';

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (typeof window.closeDeleteAccountModal === 'function') {
                window.closeDeleteAccountModal();
            }
        }
    });

    (function () {
        var wrapper = document.getElementById('recScrollWrapper');
        var grid = document.getElementById('recGrid');
        var section = document.getElementById('recommendationsSection');
        var userIdHolder = document.getElementById('userIdHolder');

        if (!userIdHolder) return;

        var userId = Number(userIdHolder.dataset.userId);

        var currentPage = 0;
        var isLoading = false;
        var hasMore = true;

        var loadedIds = new Set();
        var excludedRecommendationIds = new Set();

        function checkEdge() {
            if (!wrapper) return;

            var remaining =
                wrapper.scrollWidth -
                wrapper.scrollLeft -
                wrapper.clientWidth;

            if (remaining < 200) {
                loadNextPage();
            }
        }

        function forceRecommendationsLayout() {
            if (!wrapper || !grid) return;

            requestAnimationFrame(function () {
                requestAnimationFrame(function () {
                    void wrapper.offsetWidth;
                    void wrapper.scrollWidth;
                    void grid.offsetWidth;
                    void grid.scrollWidth;

                    checkEdge();
                });
            });
        }

        if (wrapper && grid) {
            forceRecommendationsLayout();

            window.addEventListener('load', forceRecommendationsLayout);

            window.addEventListener('resize', function () {
                requestAnimationFrame(forceRecommendationsLayout);
            });

            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(forceRecommendationsLayout);
            }

            if (window.ResizeObserver) {
                var recommendationsResizeObserver =
                    new ResizeObserver(function () {
                        checkEdge();
                    });

                recommendationsResizeObserver.observe(wrapper);
            }
        }

        if (grid) {
            grid.querySelectorAll('.rec-card').forEach(function (card) {
                var id = card.dataset.movieId;
                if (id) {
                    loadedIds.add(String(id));
                }
            });
        }

        function updateSectionVisibility() {
            if (!section || !grid) return;

            var cards = grid.querySelectorAll('.rec-card');
            var hasCards = cards.length > 0;

            section.style.display = hasCards ? '' : 'none';
        }

        function updateRecommendationsEmptyState() {
            if (!grid) return;

            var cards = grid.querySelectorAll('.rec-card');
            var empty = grid.querySelector('.recommendations-empty');

            if (cards.length === 0) {
                if (!empty) {
                    empty = document.createElement('div');
                    empty.className = 'recommendations-empty';

                    empty.innerHTML =
                        '<div class="empty-title">' +
                            'Нет рекомендаций' +
                        '</div>' +
                        '<div class="empty-description">' +
                            'Вы уже оценили или купили все фильмы' +
                        '</div>';

                    grid.appendChild(empty);
                }
            } else {
                if (empty) {
                    empty.remove();
                }
            }

            updateSectionVisibility();
        }

        function createCard(movie) {
            var id = movie && movie.id;
            if (!id) return null;

            var idStr = String(id);

            if (excludedRecommendationIds.has(idStr)) return null;
            if (loadedIds.has(idStr)) return null;

            var tpl = document.getElementById('recCardTemplate');
            if (!tpl) return null;

            var card = tpl.content.firstElementChild.cloneNode(true);
            card.dataset.movieId = idStr;

            var link = card.querySelector('.rec-card-link');
            if (link) link.href = '/movies/' + encodeURIComponent(id);

            var img = card.querySelector('.rec-poster-img');
            var empty = card.querySelector('.rec-poster-empty');
            var poster = movie.posterUrl || movie.poster_url || '';

            if (poster) {
                if (img) {
                    img.src = poster;
                    img.alt = movie.title || '';
                    img.style.display = '';
                }
                if (empty) empty.style.display = 'none';
            } else {
                if (img) img.style.display = 'none';
                if (empty) empty.style.display = 'flex';
            }

            var titleEl = card.querySelector('.rec-title');
            if (titleEl) titleEl.textContent = movie.title || 'Без названия';

            var yearEl = card.querySelector('.rec-year');
            if (yearEl) yearEl.textContent = movie.releaseYear || movie.release_year || '';

            return card;
        }

        window.removeRecommendationOptimistic = function (movieId) {
            if (!movieId) {
                return function () {};
            }

            var id = String(movieId);

            excludedRecommendationIds.add(id);
            loadedIds.add(id);

            if (!grid) {
                return function () {};
            }

            var cards = Array.from(
                grid.querySelectorAll('.rec-card[data-movie-id]')
            ).filter(function (card) {
                return String(card.dataset.movieId) === id;
            });

            if (cards.length === 0) {
                updateRecommendationsEmptyState();

                return function rollback() {
                    excludedRecommendationIds.delete(id);
                    loadedIds.delete(id);
                };
            }

            var snapshots = cards.map(function (card) {
                return {
                    html: card.outerHTML,
                    parent: card.parentNode,
                    nextSibling: card.nextSibling
                };
            });

            cards.forEach(function (card) {
                card.style.transition = 'opacity .2s ease, transform .2s ease';
                card.style.opacity = '0';
                card.style.transform = 'scale(.92)';
            });

            setTimeout(function () {
                cards.forEach(function (card) { card.remove(); });

                updateRecommendationsEmptyState();

                checkAndLoadMoreRecommendations();
            }, 200);

            var rolledBack = false;

            return function rollback() {
                if (rolledBack) return;

                rolledBack = true;

                excludedRecommendationIds.delete(id);
                loadedIds.delete(id);

                snapshots.forEach(function (snapshot) {
                    var parser = document.createElement('div');
                    parser.innerHTML = snapshot.html;

                    var restoredCard = parser.firstElementChild;

                    if (!restoredCard || !snapshot.parent) {
                        return;
                    }

                    restoredCard.style.opacity = '0';
                    restoredCard.style.transform = 'scale(.92)';
                    restoredCard.style.transition = 'none';

                    if (
                        snapshot.nextSibling &&
                        snapshot.nextSibling.parentNode === snapshot.parent
                    ) {
                        snapshot.parent.insertBefore(
                            restoredCard,
                            snapshot.nextSibling
                        );
                    } else {
                        snapshot.parent.appendChild(restoredCard);
                    }

                    requestAnimationFrame(function () {
                        requestAnimationFrame(function () {
                            restoredCard.style.transition =
                                'opacity .2s ease, transform .2s ease';

                            restoredCard.style.opacity = '1';
                            restoredCard.style.transform = 'scale(1)';
                        });
                    });
                });

                updateRecommendationsEmptyState();
            };
        };

        window.__realRemoveRecommendationOptimistic =
            window.removeRecommendationOptimistic;

        async function loadNextPage() {
            if (isLoading || !hasMore || !grid) return;

            isLoading = true;

            var nextPage = currentPage + 1;

            try {
                var response = await fetch(
                    '/recommendations?userId=' + userId + '&page=' + nextPage,
                    {
                        headers: {
                            'Accept': 'application/json'
                        }
                    }
                );

                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }

                var slice = await response.json();

                var movies = Array.isArray(slice.content)
                    ? slice.content
                    : [];

                currentPage = nextPage;

                hasMore =
                    slice.hasNext === true ||
                    (
                        slice.hasNext === undefined &&
                        slice.last === false
                    );

                for (var i = 0; i < movies.length; i++) {
                    var movie = movies[i];
                    var id = movie && movie.id;

                    if (!id) continue;

                    var idStr = String(id);

                    if (excludedRecommendationIds.has(idStr)) {
                        continue;
                    }

                    if (loadedIds.has(idStr)) {
                        continue;
                    }

                    var card = createCard(movie);

                    if (!card) continue;

                    loadedIds.add(idStr);

                    card.style.opacity = '0';
                    card.style.transform = 'translateY(6px)';
                    card.style.transition = 'opacity .2s ease, transform .2s ease';

                    grid.appendChild(card);

                    (function (c) {
                        requestAnimationFrame(function () {
                            requestAnimationFrame(function () {
                                c.style.opacity = '1';
                                c.style.transform = 'translateY(0)';
                            });
                        });
                    })(card);
                }

                updateRecommendationsEmptyState();

            } catch (err) {
                console.error('Ошибка загрузки рекомендаций:', err);
            } finally {
                isLoading = false;
            }
        }

        function checkAndLoadMoreRecommendations() {
            if (isLoading || !hasMore || !grid || !wrapper) {
                updateRecommendationsEmptyState();
                return;
            }

            var cards = grid.querySelectorAll('.rec-card');

            if (cards.length === 0) {
                loadNextPage();
                return;
            }

            var remaining =
                wrapper.scrollWidth -
                wrapper.scrollLeft -
                wrapper.clientWidth;

            if (remaining < 200) {
                loadNextPage();
            }
        }

        window.refreshRecommendationsAfterMutation = function () {
            updateRecommendationsEmptyState();
            checkAndLoadMoreRecommendations();
        };

        async function reloadRecommendations() {
            if (isLoading || !grid) return;

            isLoading = true;

            try {
                var response = await fetch(
                    '/recommendations?userId=' + encodeURIComponent(userId) + '&page=1',
                    {
                        headers: {
                            'Accept': 'application/json',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        cache: 'no-store'
                    }
                );

                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }

                var slice = await response.json();
                var movies = Array.isArray(slice.content)
                    ? slice.content
                    : [];

                currentPage = 1;
                hasMore =
                    slice.hasNext === true ||
                    (slice.hasNext === undefined && slice.last === false);

                loadedIds.clear();
                grid.innerHTML = '';

                for (var i = 0; i < movies.length; i++) {
                    var movie = movies[i];
                    var id = movie && movie.id;
                    if (!id) continue;

                    var idStr = String(id);

                    if (excludedRecommendationIds.has(idStr)) {
                        continue;
                    }

                    if (loadedIds.has(idStr)) {
                        continue;
                    }

                    var card = createCard(movie);
                    if (!card) continue;

                    loadedIds.add(idStr);
                    grid.appendChild(card);
                }

                updateRecommendationsEmptyState();

            } catch (error) {
                console.error('Ошибка обновления рекомендаций:', error);
            } finally {
                isLoading = false;
            }
        }

        window.reloadRecommendations = reloadRecommendations;

        if (wrapper) {
            wrapper.addEventListener('scroll', checkEdge, { passive: true });

            wrapper.addEventListener(
                'wheel',
                function (e) {
                    if (Math.abs(e.deltaX) >= Math.abs(e.deltaY)) {
                        return;
                    }

                    if (wrapper.scrollWidth <= wrapper.clientWidth) {
                        return;
                    }

                    e.preventDefault();

                    wrapper.scrollLeft += e.deltaY;
                },
                { passive: false }
            );
        }

        updateRecommendationsEmptyState();
    })();
})();