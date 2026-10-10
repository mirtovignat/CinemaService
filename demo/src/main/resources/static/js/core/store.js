(function () {
    'use strict';

    var KEYS = {
        cart: 'cart_',
        favourite: 'fav_'
    };

    function read(key) {
        try { return sessionStorage.getItem(key); } catch (e) { return null; }
    }
    function write(key, value) {
        try { sessionStorage.setItem(key, value); } catch (e) {}
    }
    function setStatus(prefix, movieId, value) {
        write(prefix + movieId, value ? 'true' : 'false');
    }
    function getStatus(prefix, movieId) {
        var val = read(prefix + movieId);
        if (val === 'true') return true;
        if (val === 'false') return false;
        return null;
    }
    function clearPrefixes() {
        try {
            Object.keys(sessionStorage).forEach(function (key) {
                if (key.indexOf(KEYS.cart) === 0 || key.indexOf(KEYS.favourite) === 0) {
                    sessionStorage.removeItem(key);
                }
            });
        } catch (e) {}
    }

    window.Store = {
        cart: {
            set: function (movieId, value) { setStatus(KEYS.cart, movieId, value); },
            get: function (movieId) { return getStatus(KEYS.cart, movieId); }
        },
        favourite: {
            set: function (movieId, value) { setStatus(KEYS.favourite, movieId, value); },
            get: function (movieId) { return getStatus(KEYS.favourite, movieId); }
        },
        clearAll: clearPrefixes
    };
})();