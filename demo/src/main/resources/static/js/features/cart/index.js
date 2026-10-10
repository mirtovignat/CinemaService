(function () {
    'use strict';

    var Cart = {
        async add(movieId) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            var data = await API.cart.add(movieId);
            Store.cart.set(movieId, true);
            UI.updateCartBadge();
            return data;
        },

        async remove(movieId) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            var data = await API.cart.remove(movieId);
            Store.cart.set(movieId, false);
            UI.updateCartBadge();
            return data;
        },

        async clear() {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            return API.cart.clear();
        },

        async buyOne(movieId) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieId) {
                throw { status: 400, message: 'Не удалось определить фильм' };
            }
            var data = await API.orders.add(movieId);
            Store.cart.set(movieId, false);
            UI.updateCartBadge();
            return data;
        },

        async buyBulk(movieIds) {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            if (!movieIds || !movieIds.length) {
                throw { status: 400, message: 'Корзина пуста' };
            }
            var data = await API.orders.addBulk(movieIds);
            movieIds.forEach(function (id) { Store.cart.set(id, false); });
            UI.updateCartBadge();
            return data;
        }
    };

    window.CartFeature = Cart;
})();