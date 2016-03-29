/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.net.InetAddress;
import java.util.StringTokenizer;
import java.text.ParseException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class Platform 
{
	public final static String getMacAddress()
	{
		String os = System.getProperty("os.name");

		try 
		{
		    if (os.startsWith("Windows"))
		    {
		       return windowsParseMacAddress(windowsRunIpConfigCommand());
		    }
		    else if (os.startsWith("Linux")) {
		       return linuxParseMacAddress(linuxRunIfConfigCommand());
		    }
		    else
		    {
		        return null;
		    }
		}
		catch (Exception ex) 
		{
			Trace.write(Trace.Error,ex, "getMacAddress");
		    return null;
		}
	}

	/*
	* Linux stuff
	*/
	private final static String linuxParseMacAddress(String ipConfigResponse) throws Exception 
	{
		String localHost = null;
	    try
	    {
	    	localHost = InetAddress.getLocalHost().getHostAddress();
	    } 
	    catch (java.net.UnknownHostException ex) 
	    {
	         throw ex;
	    }

	    StringTokenizer tokenizer = new StringTokenizer(ipConfigResponse, "\n");
	    String lastMacAddress = null;

	    while (tokenizer.hasMoreTokens())
	    {
	        String line = tokenizer.nextToken().trim();
	        boolean containsLocalHost = line.indexOf(localHost) >= 0;

	        // see if line contains IP address
	        if (containsLocalHost && lastMacAddress != null) 
	        {
	            return lastMacAddress;
	        }

	        // see if line contains MAC address
	       int macAddressPosition = line.indexOf("HWaddr");
	       if (macAddressPosition <= 0) continue;

	       String macAddressCandidate = line.substring(macAddressPosition + 6).trim();
	       if (linuxIsMacAddress(macAddressCandidate)) 
	       {
	           lastMacAddress = macAddressCandidate;
	           continue;
	       }
	     }

	    ParseException ex = new ParseException("cannot read MAC address for "
	    		+ localHost + " from [" + ipConfigResponse + "]", 0);
	    //ex.printStackTrace();
	    throw ex;
	}

	private final static boolean linuxIsMacAddress(String macAddressCandidate) 
	{
	    if (macAddressCandidate.length() != 17) return false;
	    return true;
	}

	private final static String linuxRunIfConfigCommand() throws IOException 
	{
		Process p = Runtime.getRuntime().exec("ifconfig");
	    InputStream stdoutStream = new BufferedInputStream(p.getInputStream());

	    StringBuffer buffer = new StringBuffer();
	    for (;;)
	    {
	    	int c = stdoutStream.read();
	    	if (c == -1) break;
	        buffer.append((char) c);
	    }
	    String outputText = buffer.toString();
	    stdoutStream.close();
	    return outputText;
	}

	/*
	* Windows stuff
	*/
	private final static String windowsParseMacAddress(String ipConfigResponse) throws ParseException 
	{	    
	    StringBuffer buf = new StringBuffer();
	    StringTokenizer tokenizer = new StringTokenizer(ipConfigResponse, "\n");	    
	    while (tokenizer.hasMoreTokens())
	    {
	    	String line = tokenizer.nextToken().trim();

	    	if (line.indexOf("Physical Address") >= 0 || line.indexOf("物理地址")>=0)
	    	{
	    		String MACAddr = line.substring(line.indexOf("-") - 2);
	    		if(buf.length()>0)
	    		{
	    			buf.append(",");
	    		}
	    		buf.append(MACAddr);
	    	}
	     }

	     return buf.toString();
	}	

	private final static String windowsRunIpConfigCommand() throws IOException 
	{
		Process p = Runtime.getRuntime().exec("ipconfig /all");
	    InputStream stdoutStream = new BufferedInputStream(p.getInputStream());

	    StringBuffer buffer = new StringBuffer();
	    for (;;) 
	    {
	        int c = stdoutStream.read();
	        if (c == -1)  break;
	        buffer.append((char) c);
	    }
	    
	    String outputText = buffer.toString();
	    stdoutStream.close();
	    
	    outputText = new String(outputText.getBytes("iso-8859-1"), config.getEncoding());
	    
	    return outputText;
	}
}
