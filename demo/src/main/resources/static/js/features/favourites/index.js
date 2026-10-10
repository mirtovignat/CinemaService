(function () {
    'use strict';

    var FavouritesFeature = {
        async add(movieId) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            var data = await API.favourites.add(movieId);
            Store.favourite.set(movieId, true);
            UI.updateFavouritesBadge();
            return data;
        },

        async remove(movieId) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            var data = await API.favourites.remove(movieId);
            Store.favourite.set(movieId, false);
            UI.updateFavouritesBadge();
            return data;
        },

        async toggle(movieId, currentAction) {
            return currentAction === 'add'
                ? this.add(movieId)
                : this.remove(movieId);
        },

        async clear() {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            var data = await API.favourites.clear();
            UI.updateFavouritesBadge();
            return data;
        }
    };

    window.FavouritesFeature = FavouritesFeature;
})();