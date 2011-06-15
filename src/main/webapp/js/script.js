if (!Modernizr.websockets) {
	alert("WebSocket not supported by this browser");
}

function getKeyCode(ev) {
	if (window.event)
		return window.event.keyCode;
	return ev.keyCode;
}

var server = {
	connect : function(logfile, newOnly) {
		var location = document.location.toString().replace('http://', 'ws://')
				.replace('https://', 'wss://');
		location = location + 'servlet/tailingwebsocket?logfile=' + logfile
				+ '&newOnly=' + newOnly;

		this._ws = new WebSocket(location);
		this._ws.onopen = this._onopen;
		this._ws.onmessage = this._onmessage;
		this._ws.onclose = this._onclose;
	},

	disconnect : function() {
		if (this._ws)
			this._ws.close();
	},

	_onopen : function() {
		server._send('websockets are open for communications!');
	},

	_send : function(message) {
		if (this._ws)
			this._ws.send(message);
	},

	send : function(text) {
		if (text != null && text.length > 0)
			server._send(text);
	},

	_onmessage : function(m) {
		if (m.data) {
			var messageBox = $('#messageBox')[0];
			var spanText = document.createElement('span');
			spanText.className = 'text';
			spanText.innerHTML = m.data;
			var lineBreak = document.createElement('br');
			messageBox.appendChild(spanText);
			messageBox.appendChild(lineBreak);
			messageBox.scrollTop = messageBox.scrollHeight
					- messageBox.clientHeight;
		}
	},

	_onclose : function(m) {
		this._ws = null;
	}
};
