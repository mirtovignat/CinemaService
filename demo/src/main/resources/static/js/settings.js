function toggleWatchHistory(el) {
    el.classList.toggle('active');
    var save = el.classList.contains('active');

    fetch('/watched', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: 'save=' + save
    })
    .then(function(response) {
        if (!response.ok) {
            return response.json().then(function(err) {
                throw new Error(err.message || 'Ошибка');
            });
        }
        return response.json();
    })
    .then(function(data) {
        alert(data.message || (save ? 'История включена' : 'История отключена'));
    })
    .catch(function(err) {
        alert(err.message || 'Ошибка изменения настройки');
    });
}

function clearWatchHistory() {
    if (!confirm('Вы уверены, что хотите очистить всю историю просмотров?')) {
        return;
    }

    fetch('/watched/clear', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(function(response) {
        if (!response.ok) {
            return response.json().then(function(err) {
                throw new Error(err.message || 'Ошибка');
            });
        }
        return response.json();
    })
    .then(function(data) {
        alert(data.message || 'История очищена');
    })
    .catch(function(err) {
        alert(err.message || 'Ошибка очистки истории');
    });
}