(function () {
    'use strict';

    function assertAuthOrThrow() {
        if (!window.requireAuth()) {
            throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
        }
    }

    var RatingsFeature = {
        async add(movieId, rating, review) {
            assertAuthOrThrow();
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            return API.rated.add({
                movieId: movieId,
                rating: rating,
                review: review || ''
            });
        },

        async edit(movieId, rating, review) {
            assertAuthOrThrow();
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            return API.rated.edit({
                movieId: movieId,
                rating: rating,
                review: review || ''
            });
        },

        async remove(movieId) {
            assertAuthOrThrow();
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            return API.rated.remove(movieId);
        },

        async count(movieId) {
            if (!movieId) return 0;
            var raw = await API.rated.count(movieId);
            return parseInt(raw, 10) || 0;
        }
    };

    window.RatingsFeature = RatingsFeature;
})();