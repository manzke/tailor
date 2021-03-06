package de.devsurf.html.tail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TailorWebSocketServlet extends WebSocketServlet {
	private static Logger LOGGER = LoggerFactory
			.getLogger(TailorWebSocketServlet.class);
	private static final long serialVersionUID = -7289719281366784056L;

	private ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request,
				response);
	}

	public WebSocket doWebSocketConnect(HttpServletRequest request,
			String protocol) {
		String logfile = request.getParameter("logfile");
		if (logfile == null || logfile.length() == 0) {
			logfile = "unknown.txt";
		}

		boolean newOnly = "on"
				.equalsIgnoreCase(request.getParameter("newOnly"));

		return new TailorSocket(logfile, newOnly);
	}

	class TailorSocket implements WebSocket.OnTextMessage {
		private Connection _connection;
		private String _logFileName;
		private RandomAccessFile _log;
		private boolean _newOnly;
		private Runnable _job;
		private ScheduledFuture<?> _scheduledJob;

		public TailorSocket() {
			_logFileName = "unknown.txt";
		}

		public TailorSocket(String logfile, boolean newOnly) {
			this._logFileName = logfile;
			this._newOnly = newOnly;
		}

		@Override
		public void onClose(int closeCode, String message) {
			destroy();
		}

		@Override
		public void onMessage(String data) {
			if ("Pause".equalsIgnoreCase(data)) {
				if (_scheduledJob != null) {
					_scheduledJob.cancel(true);
					_scheduledJob = null;
					try {
						_connection
								.sendMessage("Sending Log Entries is paused.");
					} catch (IOException e) {
						// ignore, connection is not available anymore -> remove it
						destroy();
					}
				}
			} else if ("Resume".equalsIgnoreCase(data)) {
				if (_scheduledJob == null) {
					schedule();
					try {
						_connection
								.sendMessage("Sending Log Entries will be resumed shortly.");
					} catch (IOException e) {
						// ignore, connection is not available anymore -> remove it
						destroy();
					}
				}
			}
		}

		public void sendMessage(String data) throws IOException {
			_connection.sendMessage(data);
		}

		public boolean isOpen() {
			return _connection.isOpen();
		}

		@Override
		public void onOpen(Connection connection) {
			File logFile = new File(_logFileName);
			if (!logFile.exists()) {
				try {
					connection.sendMessage("Log File \""
							+ logFile.getAbsolutePath()
							+ "\" doesn't exist. Closing Connection.");
				} catch (IOException e) {
					// ignore
					LOGGER.info(
							"Failure while trying to tell the Client, that the Log File \""
									+ logFile.getAbsolutePath()
									+ "\" doesn't exist.", e);
				} finally{
					destroy();	
				}
			} else {
				try {
					_log = new RandomAccessFile(logFile, "r");
					connection.sendMessage("Log File \""
							+ logFile.getAbsolutePath()
							+ "\" found. Starting Tailing. New Stuff only? "
							+ _newOnly);
					if (_newOnly) {
						_log.seek(_log.length());
					}
				} catch (IOException e) {
					LOGGER.warn(
							"Error while trying to load or send a Log Line to the Web Socket-Client.",
							e);
					destroy();
					return;
				}
				_job = new Runnable() {
					private long _lastLength;

					@Override
					public void run() {
						if (isOpen()) {
							try {
								long actualLength = _log.length();
								if (actualLength < _lastLength) {
									// Rolling Log Files
									_log.seek(0);
								}
								String line = null;
								while ((line = _log.readLine()) != null) {
									_connection.sendMessage(line);
								}
								_lastLength = _log.length();
							} catch (IOException e) {
								LOGGER.warn(
										"Error while trying to send a Log Line to the Web Socket-Client.",
										e);
							}
						}
					}
				};
				schedule();
				_connection = connection;
			}
		}
		
		private void destroy(){
			if(_connection != null){
				_connection.disconnect();
			}
			if (_log != null) {
				try {
					_log.close();
				} catch (IOException e) {
					LOGGER.info(e.getMessage(), e);
				}
			}
			if(_scheduledJob != null){
				_scheduledJob.cancel(true);
			}
		}

		private void schedule() {
			_scheduledJob = executor.scheduleAtFixedRate(_job, 2, 2,
					TimeUnit.SECONDS);
		}
	}
}
