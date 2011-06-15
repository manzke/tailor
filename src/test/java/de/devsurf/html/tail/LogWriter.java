package de.devsurf.html.tail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


public class LogWriter {
	public static void main(String[] args) throws IOException {
		FileOutputStream out = new FileOutputStream(new File("log.txt"));
		String newLine = System.getProperty("line.separator");
		System.out.println("Using Separator: "+newLine+" to do a New Line");
		while(true){
			System.out.println("writing "+new Date());
			out.write(("My Random Log Entry: "+new Date()+newLine).getBytes());
			out.flush();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
}
