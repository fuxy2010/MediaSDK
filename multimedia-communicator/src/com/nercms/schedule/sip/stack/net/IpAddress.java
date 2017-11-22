/*
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of MjSip (http://www.mjsip.org)
 * 
 * MjSip is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MjSip is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MjSip; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package com.nercms.schedule.sip.stack.net;

import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
//fym import com.nercms.schedule.sip.engine.sipua.ui.Settings;
import com.nercms.schedule.sip.engine.sipua.ui.Sipdroid;

import android.preference.PreferenceManager;
import android.util.Log;
import android.content.Context;

import com.nercms.schedule.sip.stack.jstun.demo.DiscoveryTest;

/**
 * IpAddress is an IP address.
 */
public class IpAddress {

	/** The host address/name */
	String address;

	/** The InetAddress */
	InetAddress inet_address;
	
	/** Local IP address */
	public static String localIpAddress = "127.0.0.1";
	
	public static Context getUIContext() {
		return SipdroidReceiver.mContext;
	}
		
	// ********************* Protected *********************

	/** Creates an IpAddress */
	IpAddress(InetAddress iaddress) {
		init(null, iaddress);
	}

	/** Inits the IpAddress */
	private void init(String address, InetAddress iaddress) {
		this.address = address;
		this.inet_address = iaddress;
	}

	/** Gets the InetAddress */
	InetAddress getInetAddress() {
		if (inet_address == null)
			try {
				inet_address = InetAddress.getByName(address);
			} catch (java.net.UnknownHostException e) {
				inet_address = null;
			}
		return inet_address;
	}

	// ********************** Public ***********************

	/** Creates an IpAddress */
	public IpAddress(String address) {
		init(address, null);
	}

	/** Creates an IpAddress */
	public IpAddress(IpAddress ipaddr) {
		init(ipaddr.address, ipaddr.inet_address);
	}

	/** Gets the host address */
	/*
	 * public String getAddress() { if (address==null)
	 * address=inet_address.getHostAddress(); return address; }
	 */

	/** Makes a copy */
	public Object clone() {
		return new IpAddress(this);
	}

	/** Wthether it is equal to Object <i>obj</i> */
	public boolean equals(Object obj) {
		try {
			IpAddress ipaddr = (IpAddress) obj;
			if (!toString().equals(ipaddr.toString()))
				return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Gets a String representation of the Object */
	public String toString() {
		if (address == null && inet_address != null)
			address = inet_address.getHostAddress();
		return address;
	}

	// *********************** Static ***********************

	/** Gets the IpAddress for a given fully-qualified host name. */
	public static IpAddress getByName(String host_addr)
			throws java.net.UnknownHostException {
		InetAddress iaddr = InetAddress.getByName(host_addr);
		return new IpAddress(iaddr);
	}
	
	/** Sets the local IP address into the variable <i>localIpAddress</i> */
	public static void setLocalIpAddress() {
		localIpAddress = "127.0.0.1";

		try
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();

				for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();

					//Log.v("Baidu", "A local ip " + inetAddress.getHostAddress().toString() + " " + inetAddress.isAnyLocalAddress() + " " + inetAddress.isLinkLocalAddress());
					
					if (!inetAddress.isLoopbackAddress())
					{ 
						//Log.v("Baidu", "B local ip " + inetAddress.getHostAddress().toString());
						
						//fym if(!SipPreference.get_parameter(getUIContext(), SipPreference.PREF_STUN, SipPreference.DEFAULT_STUN))
						if(false == inetAddress.getHostAddress().toString().contains(":"))//fym 针对MIUI V4中的情况
						{
							localIpAddress = inetAddress.getHostAddress().toString();
							//Log.v("RTSP", "local ip " + localIpAddress.toString());
							
							return;//目前WIFI IP在前//WIFI网络下可能仍会检测到移动网络IP，取最后一个IP return;//WIFI网络下可能仍会检测到移动网络IP
						}
						/*fym else
						{
							try
							{
								String StunServer = SipPreference.get_parameter(getUIContext(), SipPreference.PREF_STUN_SERVER, SipPreference.DEFAULT_STUN_SERVER);
								int StunServerPort = Integer.valueOf(SipPreference.get_parameter(getUIContext(), SipPreference.PREF_STUN_SERVER_PORT, SipPreference.DEFAULT_STUN_SERVER_PORT));

								DiscoveryTest StunDiscover = new DiscoveryTest(inetAddress, StunServer, StunServerPort);

								// call out to stun server 
								StunDiscover.test();
								//System.out.println("Public ip is:" + StunDiscover.di.getPublicIP().getHostAddress());
								localIpAddress = StunDiscover.di.getPublicIP().getHostAddress();
							}
							catch (BindException be)
							{
								if (!Sipdroid.release)
									System.out.println(inetAddress.toString() + ": " + be.getMessage());
							}
							catch (Exception e)
							{
								if (!Sipdroid.release)
								{
									System.out.println(e.getMessage());
									e.printStackTrace();
								}
							} 
						}*/
					}					
				}
			}
		} catch (SocketException ex) {
			// do nothing
		}
	}
}
