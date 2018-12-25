package com.main.wework;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;

/**
 * Class for performing concurrent Website searches with search term
 * 
 * Assumption - search term: "log in"
 * 
 * @author kaushik
 */
public class WebsiteSearcher {

	private AtomicInteger count = new AtomicInteger();

	private URL fileIn = null;
	private BufferedReader bufferedIn = null;
	
	private FileOutputStream fileOut = null;
	private BufferedWriter bufferedOut = null;

	public WebsiteSearcher(String outputPath) {
		try {
			fileIn = new URL("https://s3.amazonaws.com/fieldlens-public/urls.txt");
			bufferedIn = new BufferedReader(new InputStreamReader(fileIn.openStream()));
			
			fileOut = new FileOutputStream(outputPath);
			bufferedOut = new BufferedWriter(new OutputStreamWriter(fileOut));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void searchExecute() throws IOException, InterruptedException {
		String line = "";
		
		//to get rid of first line as they have only category names
		if((line = bufferedIn.readLine())!=null) {}
		
		while((line = bufferedIn.readLine())!=null) {
			String val = line.substring(line.indexOf(',')+2, line.indexOf('/')+1);

			Thread thread = new Thread(new Search(val));
			thread.start();
			thread.join();

			if(count.get()==20) {
				try {
					System.out.println("After 20 requests, Sleeping for 2 secs");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					//After 20 requests setting the count to 0
					count.set(0);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Enter the output folder: ");
		Scanner in = new Scanner(System.in);
		String outputPath = in.nextLine();
		
		WebsiteSearcher web = null;
		
		try {
			web = new WebsiteSearcher(outputPath);

			web.searchExecute();
			System.out.println("Search Over");

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				web.bufferedIn.close();
				web.bufferedOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class Search implements Runnable{
		private String url = "";
		private String searchTerm = "log in";

		Search(String url) {
			this.url = url;
		}

		public void run() {
			String html = "";
			try {
				//Increment the thread count to check for 20 threads
				count.incrementAndGet();
				String fullUrl = "http://" + url;
				
				//Using JSoup library for parsing the HTML page
				html = Jsoup.connect(fullUrl).timeout(5 * 1000).get().html();
				bufferedOut.write("Search term: \"" + searchTerm + "\" for URL: " + fullUrl + ": " + (html.toLowerCase().contains(searchTerm) ? true : false));
				bufferedOut.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
