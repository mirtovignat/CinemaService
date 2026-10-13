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

    function getCsrfHeaderName() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        if (meta && meta.content) return meta.content;
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
            headers: headers
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
        count: function () { return get('/cart/count'); }
    };

    var Favourites = {
        add: function (movieId) { return post('/favourites/add/' + encode(movieId)); },
        remove: function (movieId) { return post('/favourites/remove/' + encode(movieId)); },
        clear: function () { return post('/favourites/clear'); },
        count: function () { return get('/favourites/count'); }
    };

    var Orders = {
        add: function (movieId) { return post('/orders/add/' + encode(movieId)); },
        addBulk: function (movieIds) {
            var params = new URLSearchParams();
            movieIds.forEach(function (id) { params.append('movieIds', id); });
            return post('/orders/add/bulk', params);
        }
    };

    var Rated = {
        add: function (payload) {
            var params = new URLSearchParams();
            params.append('id', payload.movieId);
            params.append('rating', payload.rating);
            params.append('review', payload.review || '');
            return post('/rated/add', params);
        },
        edit: function (payload) {
            var params = new URLSearchParams();
            params.append('id', payload.movieId);
            params.append('rating', payload.rating);
            params.append('review', payload.review || '');
            return post('/rated/edit', params);
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