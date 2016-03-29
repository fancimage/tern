/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tern.util.TernContext;
import com.tern.web.routes.ActionWrapper;
import com.tern.web.routes.RouteSet;

public class ActionHandler extends com.tern.web.ActionHandler
{	
	@Override
	public boolean execute(String path, HttpServletRequest request,
			HttpServletResponse response) 
	{
		AppContext ctx = null;
		String appName = null;
		if(path.length() > 1)
		{			
			int i = path.indexOf('/', 1);  //the second /
			if( i > 1 )
			{
				appName = path.substring(1,i);
			}
			else 
			{
				appName = path.substring(1);
			}
			
			ctx = Application.appContexts.get(appName);
			if(ctx != null)
			{
				if( i > 1 )
				{
					path = path.substring(i+1);
				}
				else
				{
					path = "";
				}
			}
		}			
		
		if(null == ctx)
		{
			ctx = ProxyContext.defContext;
		}
		else
		{
			//static??
			if(path.startsWith("static/"))
			{
				try 
				{
					doStaticReq(ctx,path,request,response);
					return true;
				}
				catch (Exception e)
				{
					return false;
				}
			}
		}
		
		ProxyContext.setCurrentContext(ctx,request,appName);
		
		RouteSet rs = ctx.getRouter();
		if(rs == null) rs = ProxyContext.router;
		
		ActionWrapper action = null;
		Object target = rs.resolve(path, request.getMethod());
		if(target instanceof ActionWrapper)
		{
			action = (ActionWrapper)target;
		}
		else if(rs != ProxyContext.router)
		{
			target = ProxyContext.router.resolve(path, request.getMethod());
			if(target instanceof ActionWrapper)
			{
				action = (ActionWrapper)target;
			}			
		}		
		
		if(action != null)
		{
			doAction(action,request,response);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private void doStaticReq(TernContext ctx,String path,HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		String fullFilePath = ctx.getResourcePath() + "/" + path;
		File file = new File(fullFilePath);
		if(!file.exists())
		{
		    //from iap
			request.getRequestDispatcher("/"+path).forward(request,response);
		}
		else		
		{
			response.reset();
			//response.setContentType("image/jpeg");
	        //response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
	        
	        int fileLength = (int) file.length();
            response.setContentLength(fileLength);
            
            if (fileLength != 0) 
            {
                InputStream inStream = new FileInputStream(file);
                byte[] buf = new byte[4096];
                ServletOutputStream servletOS = response.getOutputStream();
                int readLength;
                while (((readLength = inStream.read(buf)) != -1)) 
                {
                    servletOS.write(buf, 0, readLength);
                }
                inStream.close();
                servletOS.flush();
                servletOS.close();
            }
		}
	}
}
