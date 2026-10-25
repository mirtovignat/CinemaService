(function () {
    'use strict';

    var LOGIN_PATH = '/login';

    function isLoginUrl(url) {
        try {
            var parsedUrl = new URL(url, window.location.origin);
            return parsedUrl.pathname === LOGIN_PATH;
        } catch (error) {
            return false;
        }
    }

    function handleResponse(response) {
        if (response.type === 'opaqueredirect' || response.status === 0) {
            return Promise.reject({
                status: 401,
                code: 'NOT_AUTHORIZED',
                message: 'Авторизуйтесь!'
            });
        }
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
        return response.text().then(function (responseText) {
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
            var responseData = {};
            if (responseText) {
                try {
                    responseData = JSON.parse(responseText);
                } catch (error) {
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
                    code: responseData.code || null,
                    message: responseData.message || 'Произошла ошибка'
                });
            }
            if (responseData && responseData.success === false) {
                return Promise.reject({
                    status: response.status,
                    code: responseData.code || null,
                    message: responseData.message || 'Произошла ошибка'
                });
            }
            return responseData;
        });
    }

    window.handleResponse = handleResponse;
})();