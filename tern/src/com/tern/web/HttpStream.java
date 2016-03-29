/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.tern.util.Trace;

final public class HttpStream 
{
	private HttpServletResponse res;
	private PrintWriter writer;
	boolean hasContent;
	
	HttpStream(HttpServletResponse res)
	{
		this.res = res;
		hasContent = false;
	}
	
	final public HttpStream append(String str)
	{
		if(null == str || str.length()<=0) return this;				
		
		try
		{
			if(null == writer)
			{
				if(null == res.getContentType())
	    		{
					res.setContentType("text/html; charset=" + com.tern.util.config.getEncoding());
	    		}
				writer = res.getWriter();
			}
			
			writer.print(str);
		    if(!hasContent) hasContent = true;
		}
		catch (IOException e)
		{
			Trace.write(Trace.Error, e, "HttpStream write text");
			return null;
		}
		
		return this;
	}
	
	final public HttpStream append(String str,Object...params)
	{
		if(null == str) return this;			
		return append(String.format(str, params));
	}
	
	final public HttpStream append(byte[] content)
	{
		if(null == content || content.length<=0) return this;
		return append(content,0,content.length);
	}
	
	final public HttpStream append(byte[] content,int offset,int len)
	{
		try
		{
			if(null == res.getContentType())
    		{
				res.setContentType("application/octet-stream");
    		}
			
		    res.getOutputStream().write(content, offset, len);
		    if(!hasContent) hasContent = true;
		}
		catch (IOException e)
		{
			Trace.write(Trace.Error, e, "HttpStream write binary");
			return null;
		}
		
		return this;
	}
	
	final void flush()
	{
		if(!hasContent) return;
		
		try 
		{
			if(writer != null) writer.flush();
			else res.getOutputStream().flush();
		} 
		catch (IOException e)
		{
			Trace.write(Trace.Error, e, "HttpStream flush");
		}
	}
}
