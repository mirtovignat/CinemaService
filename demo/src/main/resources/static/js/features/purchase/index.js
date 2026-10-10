(function () {
    'use strict';

    var PurchaseFeature = {
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
        }
    };

    window.PurchaseFeature = PurchaseFeature;
})();