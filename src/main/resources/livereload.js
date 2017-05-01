(function connectToLiveReloadServer() {
  var port = window.LIVE_RELOAD_PORT || 27492;
  var socket = new WebSocket('ws://localhost:' + port);

  socket.onmessage = function (message) {
    if (event.data === 'reload_stylesheets') {
      var linkElements = document.getElementsByTagName('link');

      [].slice.call(linkElements).forEach(function (element) {
        if (element.getAttribute('rel') !== 'stylesheet') {
          return;
        }

        var href = element.getAttribute('href') || '';
        var newHref = updateHref(href);

        if (newHref) {
          element.setAttribute('href', newHref);
        }
      });

    } else if (event.data === 'reload_page') {
      location.reload();
    }
  };

  socket.onclose = function () {
    console.log('Disconnected from live-reload server. Will reconnect in 5 seconds.');
    setTimeout(connectToLiveReloadServer, 5000);
  };

  function updateHref(href) {
    var newHref;

    var timestampQuery = 'live_reload_timestamp=' + new Date().getTime();
    var timestampRegex = /([?&])live_reload_timestamp=\d+(&|$)/;

    if (timestampRegex.test(href)) {
      newHref = href.replace(timestampRegex, '$1' + timestampQuery + '$2');

    } else {
      var queryStringIndex = href.indexOf('?');

      if (queryStringIndex >= 0) {
        var queryString = href.slice(queryStringIndex + 1);

        if (queryString !== '') {
          queryString = queryString + '&' + timestampQuery;
        } else {
          queryString = timestampQuery;
        }

        newHref = href.slice(0, queryStringIndex) + '?' + queryString;

      } else {
        newHref = href + '?live_reload_timestamp=' + new Date().getTime();
      }
    }

    return newHref;
  }
})();
