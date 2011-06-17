function getKeyCode(ev) {
	if (window.event)
		return window.event.keyCode;
	return ev.keyCode;
}

var server = function(){
	return {
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
				var data = m.data;
				var lowerCaseData = data.toLowerCase();
				
				var index = lowerCaseData.indexOf($('#errortext').val());
				if(index > 0){
					data = data.fontcolor("red");
				}else{
					index = lowerCaseData.indexOf($('#warntext').val());
					if(index > 0){
						data = data.fontcolor("yellow");
					}
				}
				
				var pckge = $('#yourpackage').val();
				index = lowerCaseData.indexOf(pckge);
				if(index > 0){
					data = data.replace(pckge, pckge.fontcolor('blue'));
				}

				spanText.innerHTML = data;
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
}();

var dao = function(){
	return {
		save: function(name, location){
			if (Modernizr.localstorage) {
				localStorage.setItem(name, location);
			} else {
				alert("Your Browser does not support LocalStorage.");
			}	
		},
		load: function(name){
			if (Modernizr.localstorage) {
				var location = localStorage.getItem(name);
				if(location == null){
					alert("No Data was found in your LocalStorage.");
				}else{
					return location;
				}					
			} else {
				alert("Your Browser does not support LocalStorage.");
			}
		}
	};
}();

