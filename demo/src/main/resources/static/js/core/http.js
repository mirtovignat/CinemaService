(function () {
    'use strict';

    var LOGIN_PATH = '/login';

    function isLoginUrl(url) {
        try {
            var parsed = new URL(url, window.location.origin);
            return parsed.pathname === LOGIN_PATH;
        } catch (e) {
            return false;
        }
    }

    function handleResponse(response) {
        if (response.redirected && isLoginUrl(response.url)) {
            return Promise.reject({
                status: 401,
                code: 'NOT_AUTHORIZED',
                message: 'Авторизуйтесь!'
            });
        }
        if (response.status === 401) {
            return Promise.reject({
                status: 401,
                code: 'NOT_AUTHORIZED',
                message: 'Авторизуйтесь!'
            });
        }
        return response.text().then(function (text) {
            var contentType = response.headers.get('content-type') || '';
            if (contentType.indexOf('text/html') !== -1) {
                if (isLoginUrl(response.url)) {
                    return Promise.reject({
                        status: 401,
                        code: 'NOT_AUTHORIZED',
                        message: 'Авторизуйтесь!'
                    });
                }
                return Promise.reject({
                    status: response.status,
                    code: null,
                    message: 'Ошибка сервера'
                });
            }
            var data = {};
            if (text) {
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    if (!response.ok) {
                        return Promise.reject({
                            status: response.status,
                            code: null,
                            message: 'Ошибка сервера'
                        });
                    }
                    return Promise.reject({
                        status: response.status,
                        code: null,
                        message: 'Некорректный ответ сервера'
                    });
                }
            }
            if (!response.ok) {
                return Promise.reject({
                    status: response.status,
                    code: data.code || null,
                    message: data.message || 'Произошла ошибка'
                });
            }
            if (data && data.success === false) {
                return Promise.reject({
                    status: response.status,
                    code: data.code || null,
                    message: data.message || 'Произошла ошибка'
                });
            }
            return data;
        });
    }

    window.handleResponse = handleResponse;
})();