package my;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import my.DNSResolver;
import my.DNSResponse;

public class Crawler
{

  static Socket clientSocket;
  static DataOutputStream outToServer;
  static BufferedReader inFromServer;

  public static void main(String argv[]) throws IOException
  {
    File configFile = new File("config.properties");
    InputStream inputStream = new FileInputStream(configFile);
    Properties props = new Properties();
    props.load(inputStream);
    String adr = props.getProperty("hostName", "http://www.w3.org/");
    String host;
    String res;
    String proxy = null;
    adr = adr.substring(adr.indexOf("//") + 2);
    host = adr.substring(0, adr.indexOf('/'));
    adr = adr.substring(adr.indexOf('/'));
    res = adr;
    switch (HTTPRequest(host, res, proxy))
    {
      case -1:
        System.out.println("Wrong or inactive host !");
        break;
      case 0:
        System.out.println("Not enough permissions !");
        break;
      case 1:
        System.out.println("Error : If there is no DNS message look into resp.txt");
        break;
      default:
        System.out.println("Success!");
    }
  }

  public static int HTTPRequest(String host, String res, String proxy) throws IOException
  {

    List<String> allow = new LinkedList<String>(); // allowed resources
    List<String> disallow = new LinkedList<String>(); // restricted resources
    DNSResponse dnsResp;
    String ip = null, resp = null, redir = null, allowed = "", disallowed = "";
    String robot = "/robots.txt";
    boolean ok = false;
    int i = 0;

    byte[] dns = DNSResolver.Init();

    if (host.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"))
    {
      ip = host;
      dnsResp = DNSResolver.GetNameByIp(host, dns);
      if (null != dnsResp)
      {
        host = dnsResp.RDATA();
      }
    } else if (null == proxy)
    {
      dnsResp = DNSResolver.GetIpByName(host, dns);
      if (null != dnsResp)
      {
        ip = dnsResp.RDATA();
      }
    }
    if (null != proxy)
      if (proxy
          .matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"))
        ip = proxy;
      else
      {
        dnsResp = DNSResolver.GetIpByName(proxy, dns);
        if (null != dnsResp)
          ip = dnsResp.RDATA();
      }

    if (null != ip)
    {
      while (!ok && i < 5)
      {
        HTTPOpenConnection(ip);
        resp = HTTPQuery("GET", host, robot);
        if (resp.contains("20"))
        {
          Robots(resp, allow, disallow);
          ok = true;
        } else if (resp.contains("30"))
        {
          if (null == (redir = HTTPRedirect(resp, host, robot)))
            i = 5;
          else
          {
            redir = redir.substring(redir.indexOf("//") + 2);
            host = redir.substring(0, redir.indexOf('/'));
            redir = redir.substring(redir.indexOf('/'));
            robot = redir;
          }
          if (resp.contains("301"))
            System.out.println("Permanent redirection to " + host + robot);
        } else
          i = 5;
        HTTPCloseConnection();
        ++i;
      }

      if (ok)
      {
        for (String p : allow)
        {
          if (res.contains(p))
            if (allowed.length() < p.length())
              allowed = p;
        }
        for (String p : disallow)
        {
          if (res.contains(p))
            if (disallowed.length() < p.length())
              disallowed = p;
        }
        if (allowed.length() < disallowed.length())
          return 0;
      }
      allowed = disallowed = "";
      ok = false;
      i = 0;

      while (!ok && i < 5)
      {
        HTTPOpenConnection(ip);
        resp = HTTPQuery("OPTIONS", host, res);
        if (resp.contains("20"))
        {
          if (resp.contains("HTTP/1.0"))
          {
            WriteLogHtml(WriteLogTxt(resp));
            HTTPCloseConnection();
            return 2;
          } else if (HTTPAllowed(resp))
            ok = true;
          else
            i = 5;
        } else if (resp.contains("30"))
        {
          if (null == (redir = HTTPRedirect(resp, host, res)))
            i = 5;
          else
          {
            redir = redir.substring(redir.indexOf("//") + 2);
            host = redir.substring(0, redir.indexOf('/'));
            redir = redir.substring(redir.indexOf('/'));
            res = redir;
            for (String p : allow)
            {
              if (res.contains(p))
                if (allowed.length() < p.length())
                  allowed = p;
            }
            for (String p : disallow)
            {
              if (res.contains(p))
                if (disallowed.length() < p.length())
                  disallowed = p;
            }
            if (allowed.length() < disallowed.length())
            {
              HTTPCloseConnection();
              return 0;
            }
          }
          if (resp.contains("301"))
            System.out.println("Permanent redirection to  " + host + robot);
        } else
        {
          WriteLogTxt(resp);
          i = 5;
        }
        HTTPCloseConnection();
        ++i;
      }
      if (ok)
      {
        HTTPOpenConnection(ip);
        resp = HTTPQuery("GET", host, res);
        WriteLogHtml(WriteLogTxt(resp));
        HTTPCloseConnection();
        if (resp.contains("20"))
          return 2;
      }
      return 1;
    }
    return -1;
  }

  // open stream to the host on port 80 (default http)
  static void HTTPOpenConnection(String ip) throws IOException
  {
    clientSocket = new Socket(ip, 80);
    outToServer = new DataOutputStream(clientSocket.getOutputStream());
    inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  }

  // close the stream
  static void HTTPCloseConnection() throws IOException
  {
    inFromServer.close();
    outToServer.close();
    clientSocket.close();
  }

  // send a http querry
  static String HTTPQuery(String function, String host, String res) throws IOException
  {
    String sentence =
        function + " " + res + " HTTP/1.1\r\n" + "Host: " + host + "\r\n"
            + "User-Agent: CLIENT RIW\r\n" + "\r\n";
    outToServer.writeBytes(sentence);
    return inFromServer.readLine();
  }

  // handle redirection
  static String HTTPRedirect(String response, String host, String res) throws IOException
  {
    String s = null;
    Writer output = new BufferedWriter(new FileWriter(new File("resp.txt")));
    output.write(response + "\r\n");
    while ((response = inFromServer.readLine()) != null && !response.startsWith("<"))
    {
      if (response.toUpperCase().startsWith("LOCATION"))
        s = response;
      output.write(response);
      output.write("\r\n");
    }
    output.close();
    return s;
  }


  static boolean HTTPAllowed(String response) throws IOException
  {
    boolean s = false;
    Writer output = new BufferedWriter(new FileWriter(new File("resp.txt")));
    output.write(response + "\r\n");
    while ((response = inFromServer.readLine()) != null && !response.startsWith("<"))
    {
      if (response.toUpperCase().startsWith("ALLOW") && response.toUpperCase().contains("GET"))
        s = true;
      output.write(response);
      output.write("\r\n");
    }
    output.close();
    return s;
  }

  // log error messages
  static String WriteLogTxt(String response) throws IOException
  {
    Writer output = new BufferedWriter(new FileWriter(new File("resp.txt")));
    output.write(response + "\r\n");
    while (null != (response = inFromServer.readLine()) && !response.startsWith("<"))
      output.write(response + "\r\n");
    output.close();
    return response;
  }

  // log success response
  static void WriteLogHtml(String response) throws IOException
  {
    if (null != response)
    {
      Writer output = new BufferedWriter(new FileWriter(new File("index.html")));
      output.write(response + "\r\n");
      while (null != (response = inFromServer.readLine())
          && !response.toUpperCase().contains("</HTML>"))
        output.write(response + "\r\n");
      output.close();
    }
  }

  // parse robots file
  static void Robots(String response, List<String> allow, List<String> disallow) throws IOException
  {
    while (null != (response = inFromServer.readLine())
        && (!response.startsWith("User-agent") || !response.contains("*")));
    if (null != response)
      while ((response = inFromServer.readLine()).startsWith("Allow")
          || response.startsWith("Disallow"))
      {
        if (response.startsWith("Allow"))
          allow.add((response.split(" "))[1]);
        else
          disallow.add((response.split(" "))[1]);
      }
  }
}
