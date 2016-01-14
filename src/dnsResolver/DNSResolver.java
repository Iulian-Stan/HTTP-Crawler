package dnsResolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class DNSResolver {
	private static byte serverDNS[] = null;
	private static String regExIp = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	private static String regExHost = "^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])$";
	private static String delimiter = "[.]";
	
	public static boolean IsValidIPAddress(String ipAddress)
	{
		return ipAddress.matches(regExIp);
	}	
	
	public static boolean IsValidHosName(String hostName)
	{
		return hostName.matches(regExHost);
	}

	// get DNS server address as array of byte
	private static void Init() {
		if (serverDNS == null) {
			List<?> nameservers = sun.net.dns.ResolverConfiguration.open().nameservers();
			if (!nameservers.isEmpty()) {
				String[] tokens = nameservers.get(nameservers.size() - 1).toString().split(delimiter);
				serverDNS = new byte[4];
				for (int i = 0; i < 4; ++i)
					serverDNS[i] = (byte) Short.parseShort(tokens[i]);
			}
		}
	}
	
	private static byte[] Resolve(byte[] sendData) {
		byte[] receiveData = new byte[512];

		Init();
		try {
			DatagramSocket dnsSocket = new DatagramSocket();

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
					InetAddress.getByAddress(serverDNS), 53);
			dnsSocket.send(sendPacket);

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			dnsSocket.receive(receivePacket);

			dnsSocket.close();

			// test Identification
			if (receiveData[0] != sendData[0] || receiveData[1] != sendData[1])
				System.err.println("Request Id does not match the response !");
			else if ((receiveData[2] & 0xff) >> 7 != 1) // test message type
				System.err.println("Received a request instead of a response !");
			else if ((receiveData[3] & 0x0f) != 0) // test error code
				System.err.println("Request failed with error code : " + (receiveData[3] & 0x0f) + " !");
			else if ((receiveData[6] | receiveData[7]) == 0) // test count
				System.err.println("Server error. Response not found !");
			else
				return receiveData;

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static DNSResponse ExtractResponse(int offset, byte[] receiveData) {
		DNSResponse dnsr = new DNSResponse((short) ((receiveData[offset - 10] << 8) + receiveData[offset - 9]),
				(short) ((receiveData[offset - 8] << 8) + receiveData[offset - 7]),
				(receiveData[offset - 6] << 24) + (receiveData[offset - 5] << 16) + (receiveData[offset - 4] << 8)
						+ receiveData[offset - 3],
				Arrays.copyOfRange(receiveData, offset,
						offset + ((receiveData[offset - 2] << 8) + receiveData[offset - 1])));
		if (dnsr.PTR() != 0) {
			StringBuilder sb = new StringBuilder();
			int k = dnsr.PTR(), j = k + receiveData[k];
			while (0 != receiveData[k]) {
				while (k < j) {
					sb.append((char) receiveData[++k]);
				}
				j += receiveData[++k] + 1;
				if (0 != receiveData[k])
					sb.append('.');
			}
			dnsr.DATA(sb.toString());
		}
		return dnsr;
	}

	// resolve site name to an IP address
	public static DNSResponse GetIpByHostName(String hostName) {
		if (!IsValidHosName(hostName)) {
			System.err.println("The provided string is not a valid URL !");
			return null;
		}
		

		// split URL into tokens (separated by points)
		String[] tokens = hostName.split(delimiter);
		int length = 17, offset = 0;
		
		for (int i = 0; i < tokens.length; ++i)
			length += tokens[i].length() + 1;
		
		byte[] sendData = new byte[length];

		sendData[0] = 0; // byte 0 and 1
		sendData[1] = 1; // Identification
		sendData[2] = 1; // byte 2 and 3
		sendData[3] = 0; // Flags
		sendData[4] = 0; // byte 4 and 5
		sendData[5] = 1; // Total Questions
		sendData[6] = 0; // byte 6 and 7
		sendData[7] = 0; // Total Answers
		sendData[8] = 0; // byte 8 and 9
		sendData[9] = 0; // Total Authority Resource Records
		sendData[10] = 0; // byte 10 and 11
		sendData[11] = 0; // Total Additional Resource Records
		offset = 12; // total 12 bytes of the header are hard coded

		// the URL is encoded as a sequence of tokens
		for (int i = 0; i < tokens.length; ++i) {
			// the length precedes the token itself www.site.net ->
			// 3www4site3net
			sendData[offset++] = (byte) tokens[i].length();
			for (int j = 0; j < tokens[i].length(); ++j)
				sendData[offset++] = (byte) tokens[i].charAt(j);
		}

		sendData[offset++] = 0; // end of the question
		sendData[offset++] = 0; // QType
		sendData[offset++] = 1; // QType 1 -> hostname
		sendData[offset++] = 0; // QClass
		sendData[offset++] = 1; // QClass 1 -> ipv4

		byte[] receiveData = Resolve(sendData);
		if (null != receiveData) {
			offset = length + receiveData[length + 1];
			int i = (receiveData[6] << 8) + receiveData[7];
			while (--i > -1) {
				DNSResponse dnsr = ExtractResponse(offset, receiveData);
				if (dnsr.Class() == 1 && dnsr.Type() == 1)
					return dnsr;
				offset += ((receiveData[offset - 2] << 8) + receiveData[offset - 1]) + 12;
			}
		}
		return null;
	}

	public static DNSResponse GetHostNameByIp(String ipAddress) {
		if (!IsValidIPAddress(ipAddress)) {
			System.err.println("The provided string is not a valid IP !");
			return null;
		}

		// split IP into tokens (separated by points)
		String[] tokens = ipAddress.split(delimiter);
		int length = 30, offset = 0;
		
		for (int i = 0; i < tokens.length; ++i)
			length += tokens[i].length() + 1;
		
		byte[] sendData = new byte[length];

		sendData[0] = 0; // byte 0 and 1
		sendData[1] = 1; // Identification
		sendData[2] = 1; // byte 2 and 3
		sendData[3] = 0; // Flags
		sendData[4] = 0; // byte 4 and 5
		sendData[5] = 1; // Total Questions
		sendData[6] = 0; // byte 6 and 7
		sendData[7] = 0; // Total Answers
		sendData[8] = 0; // byte 8 and 9
		sendData[9] = 0; // Total Authority Resource Records
		sendData[10] = 0; // byte 10 and 11
		sendData[11] = 0; // Total Additional Resource Records
		offset = 12; // total 12 bytes of the header are hard coded

		// the IP is encoded as a sequence of tokens
		for (int i = tokens.length - 1; i > -1; --i) {
			// the length precedes the token itself 127.0.0.1 -> 3127101011
			sendData[offset++] = (byte) tokens[i].length();
			for (int j = 0; j < tokens[i].length(); ++j)
				sendData[offset++] = (byte) tokens[i].charAt(j);
		}

		// inverse dns requests a followed by "in-addr.arpa"
		// encoded as a part of the URL and ends with 0
		sendData[offset++] = 7;
		sendData[offset++] = 'i';
		sendData[offset++] = 'n';
		sendData[offset++] = '-';
		sendData[offset++] = 'a';
		sendData[offset++] = 'd';
		sendData[offset++] = 'd';
		sendData[offset++] = 'r';
		sendData[offset++] = 4;
		sendData[offset++] = 'a';
		sendData[offset++] = 'r';
		sendData[offset++] = 'p';
		sendData[offset++] = 'a';
		sendData[offset++] = 0;
		sendData[offset++] = 0; // QType
		sendData[offset++] = 12; // QType 12 -> ip address
		sendData[offset++] = 0; // QClass
		sendData[offset++] = 1; // QClass 1 -> ipv4

		byte[] receiveData = Resolve(sendData);
		if (null != receiveData) {
			offset = length + receiveData[length + 1];
			int i = (receiveData[6] << 8) + receiveData[7];
			while (--i > -1) {
				DNSResponse dnsr = ExtractResponse(offset, receiveData);
				if ((dnsr.Class() == 5 || dnsr.Class() == 12) && dnsr.Type() == 1)
					return dnsr;
				offset += ((receiveData[offset - 2] << 8) + receiveData[offset - 1]) + 12;
			}
		}
		return null;
	}
}
