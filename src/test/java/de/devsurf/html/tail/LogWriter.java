package de.devsurf.html.tail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;


public class LogWriter {
	private void ex() throws Exception{
		throw new Exception("Example Exception");
	}
	public static void main(String[] args) throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(new File("log.txt")));
		String newLine = System.getProperty("line.separator");
		LogWriter writer = new LogWriter();
		System.out.println("Using Separator: "+newLine+" to do a New Line");
		while(true){
			System.out.println("writing "+new Date());
			out.println("My Random Log Error Entry: "+new Date());
			out.flush();
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// ignore
			}
			out.println("My Random Log Info Entry for de.devsurf.html.tail.Tailor : "+new Date());
			out.flush();
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// ignore
			}
			out.println("My Random Log Warning Entry: "+new Date());
			out.flush();
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// ignore
			}
			try {
				writer.ex();
			} catch (Exception e) {
				e.printStackTrace(out);
			}
			out.flush();
			
		}
	}
}
