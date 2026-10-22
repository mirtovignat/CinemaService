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

        async buyBulk() {
            if (!window.requireAuth()) {
                throw { status: 401, code: 'NOT_AUTHORIZED', message: 'Авторизуйтесь!' };
            }
            var data = await API.orders.addBulk();
            UI.updateCartBadge();
            return data;
        }
    };

    window.CartFeature = Cart;
})();