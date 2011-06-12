package com.saperion.html.tail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

public class TailorWebSocketServlet extends WebSocketServlet {
	private static final long serialVersionUID = -7289719281366784056L;
	public static String newLine = System.getProperty("line.separator");
	
	private final Set<TailorSocket> _members = new CopyOnWriteArraySet<TailorSocket>();
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request,
				response);
	}

	public WebSocket doWebSocketConnect(HttpServletRequest request,
			String protocol) {
		String logfile = request.getParameter("logfile");
		if(logfile != null && logfile.length() > 0){
			return new TailorSocket(logfile);	
		}
		return new TailorSocket();
	}

	class TailorSocket implements WebSocket.OnTextMessage {
		private Connection _connection;
		private String logFileName;
		private RandomAccessFile log;
		private long lastLength;
		
		public TailorSocket() {
			logFileName = "unknown.txt";
		}
		
		public TailorSocket(String logfile){
			logFileName = logfile;
		}
		
		@Override
		public void onClose(int closeCode, String message) {
			if(log != null){
				try {
					log.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
			_members.remove(this);
		}
		
		public void sendMessage(String data) throws IOException {
			_connection.sendMessage(data);
		}
		
		@Override
		public void onMessage(String data) {
			System.out.println("Received: "+data);
		}
		
		public boolean isOpen() {
			return _connection.isOpen();
		}

		@Override
		public void onOpen(Connection connection) {
			_members.add(this);
			_connection = connection;
			try {
				File logFile = new File(logFileName);
				if(!logFile.exists()){
					connection.sendMessage("Log File \""+logFile.getAbsolutePath()+"\" doesn't exist. Closing Connection.");
					connection.disconnect();
				}else{
					log = new RandomAccessFile(logFile, "r");	
					connection.sendMessage("Log File \""+logFile.getAbsolutePath()+"\" found. Starting Tailing.");

					executor.scheduleAtFixedRate(new Runnable() {
						@Override
						public void run() {
							if(isOpen()){
								try {
									long actualLength = log.length();
									if(actualLength < lastLength){
										//Rolling Log Files
										log.seek(0);
									}
									String line = null;
									while((line = log.readLine()) != null){
										_connection.sendMessage(line);
									}
									lastLength = log.length();
								} catch (IOException e) {
									e.printStackTrace();
								}	
							}
						}
					}, 2, 2, TimeUnit.SECONDS);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
