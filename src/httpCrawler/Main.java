package httpCrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import dnsResolver.DNSResolver;
import dnsResolver.DNSResponse;
import urlParser.URL;

public class Main {
	static List<String> allow = new LinkedList<String>();
	static List<String> disallow = new LinkedList<String>();

	public static void main(String argv[]) {

		Properties props = new Properties();

		try {
			File configFile = new File("config.properties");
			InputStream inputStream = new FileInputStream(configFile);
			props.load(inputStream);
		} catch (FileNotFoundException e) {
			System.err.println("Config file not found");
		} catch (IOException e) {
			System.err.println("Some IO exception on config load");
		}

		String link = props.getProperty("url", "http://www.w3.org/");
		String logFile = props.getProperty("logFile", "log.txt");

		HTTPCrawler crawler = null;
		URL url = null;

		if (null == (url = URL.ParseURL(link)))
			return;

		try {
			// http://www.robotstxt.org/robotstxt.html
			if (null == (crawler = InitCrawler(url.getHost() + "/robots.txt")))
				return;

			crawler.ParseRobotsFile(allow, disallow);
			crawler.Close();

			if (null == (crawler = InitCrawler(url))) {
				return;
			}

			crawler.GetHeader();
			crawler.WriteLogHtml(logFile);
			crawler.Close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private static HTTPCrawler InitCrawler(URL url) {
		String ip = null;

		if (null == (ip = GetIpAddress(url.getHost()))) {
			System.err.println("Could not resolve hostname to ip address");
			return null;
		}

		try {
			HTTPCrawler crawler = new HTTPCrawler(ip, url.getPort());
			String response = crawler.HTTPQuery("GET", url.getHost(), url.getResurce());

			if (response.contains("20"))
				return crawler;
			else if (response.contains("30")) {
				String redirect = null;
				if (null == (redirect = crawler.GetRedirectLocation())) {
					System.err.println("Could not find redirect location");
					return null;
				}
				return InitCrawler(redirect);
			} else
				return null;
		} catch (IOException e) {
			System.err.print("IOException in crawler");
			return null;
		}
	}

	private static HTTPCrawler InitCrawler(String link) {
		URL url = null;

		if (null == (url = URL.ParseURL(link)))
			return null;

		return InitCrawler(url);
	}

	private static String GetIpAddress(String host) {
		String ipAddress = null;
		DNSResponse dnsResp;
		if (DNSResolver.IsValidHosName(host)) {
			dnsResp = DNSResolver.GetIpByHostName(host);
			if (null != dnsResp) {
				ipAddress = dnsResp.RDATA();
			}
		}
		return ipAddress;
	}
}
