package org.commonjava.indy.metrics.zabbix.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * http://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in
 * -java
 * 
 */
public class HostUtil
{
	private static final Logger logger = LoggerFactory.getLogger( HostUtil.class);

	public static void main(String[] args) {
		System.err.println(getHostName());
		System.err.println(getHostAddress());
		System.err.println(getNotLoopbackAddress());
	}

	public static String getHostName() {
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			if (hostName != null && !hostName.isEmpty()) {
				return hostName;
			}
		} catch (UnknownHostException e) {
			logger.error("get hostName error!", e);
		}

		String host = System.getenv("COMPUTERNAME");
		if (host != null)
			return host;
		host = System.getenv("HOSTNAME");
		if (host != null)
			return host;

		return null;
	}

	public static String getNotLoopbackAddress() {
		String hostName = null;
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface nic = interfaces.nextElement();
				Enumeration<InetAddress> addresses = nic.getInetAddresses();
				while (hostName == null && addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress()) {
						hostName = address.getHostName();
					}
				}
			}
		} catch (SocketException e) {
			logger.error("getNotLoopbackAddress error!", e);
		}
		return hostName;
	}

	public static String getHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("get hostAddress error!", e);
		}

		return null;
	}

}
