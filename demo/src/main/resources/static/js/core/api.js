(function () {
    'use strict';

    function getCsrfToken() {
        var csrfMeta = document.querySelector('meta[name="_csrf"]');
        if (csrfMeta && csrfMeta.content) return csrfMeta.content;

        var csrfInput = document.querySelector('input[name="_csrf"]');
        if (csrfInput && csrfInput.value) return csrfInput.value;

        var cookieMatch = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        if (cookieMatch) return decodeURIComponent(cookieMatch[1]);

        return null;
    }

    function getCsrfHeaderName() {
        var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        if (csrfHeaderMeta && csrfHeaderMeta.content) return csrfHeaderMeta.content;
        return 'X-CSRF-TOKEN';
    }

    function request(url, options) {
        options = options || {};

        var method = options.method || 'GET';
        var headers = Object.assign(
            { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
            options.headers || {}
        );

        if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
            var csrfToken = getCsrfToken();
            if (csrfToken) {
                headers[getCsrfHeaderName()] = csrfToken;
            }
        }

        var fetchOptions = {
            method: method,
            headers: headers,
            redirect: 'manual'
        };

        if (options.body !== undefined && options.body !== null) {
            fetchOptions.body = options.body;
        }

        return fetch(url, fetchOptions).then(handleResponse);
    }

    function post(url, body) {
        return request(url, { method: 'POST', body: body });
    }

    function get(url) {
        return request(url, { method: 'GET' });
    }

    function del(url, body) {
        return request(url, { method: 'DELETE', body: body });
    }

    function encode(value) {
        return encodeURIComponent(value);
    }

    var Cart = {
        add: function (movieId) { return post('/cart/add/' + encode(movieId)); },
        remove: function (movieId) { return post('/cart/remove/' + encode(movieId)); },
        clear: function () { return post('/cart/clear'); },
        count: function () { return get('/cart/count'); },
        summary: function () { return get('/cart/summary'); }
    };

    var Favourites = {
        add: function (movieId) { return post('/favourites/add/' + encode(movieId)); },
        remove: function (movieId) { return post('/favourites/remove/' + encode(movieId)); },
        clear: function () { return post('/favourites/clear'); },
        count: function () { return get('/favourites/count'); }
    };

    var Orders = {
        add: function (movieId) { return post('/orders/add/' + encode(movieId)); },
        addBulk: function () { return post('/orders/add/bulk'); }
    };

    var Rated = {
        add: function (payload) {
            var urlParams = new URLSearchParams();
            urlParams.append('id', payload.movieId);
            urlParams.append('rating', payload.rating);
            urlParams.append('review', payload.review || '');
            return post('/rated/add', urlParams);
        },
        edit: function (payload) {
            var urlParams = new URLSearchParams();
            urlParams.append('id', payload.movieId);
            urlParams.append('rating', payload.rating);
            urlParams.append('review', payload.review || '');
            return post('/rated/edit', urlParams);
        },
        remove: function (movieId) {
            return post('/rated/remove/' + encode(movieId));
        },
        count: function (movieId) {
            return get('/rated/count/' + encode(movieId));
        }
    };

    var Watched = {
        add: function (movieId) { return post('/watched/add/' + encode(movieId)); },
        remove: function (historyId) { return post('/watched/remove/' + encode(historyId)); },
        clear: function () { return post('/watched/clear'); }
    };

    var Movies = {
        rating: function (movieId) {
            return get('/movies/' + encode(movieId) + '/rating?_=' + Date.now());
        },
        suggestions: function (query) {
            return get('/movies/suggestions?query=' + encode(query));
        }
    };

    window.API = {
        request: request,
        post: post,
        get: get,
        del: del,
        cart: Cart,
        favourites: Favourites,
        orders: Orders,
        rated: Rated,
        watched: Watched,
        movies: Movies
    };
})();