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
			else if(appName.equals("static"))
			{
				String tpath = path.substring(i+1);
				int j = tpath.indexOf('/');
				if( j > 1 )
				{
					appName = tpath.substring(0,j);
					ctx = Application.appContexts.get(appName);
					if(ctx != null)
					{
						path = tpath.substring(j+1);
					}										
				}
				
				return doStaticReq(ctx,path,request,response,false);
			}
		}			
		
		if(null == ctx)
		{
			ctx = ProxyContext.defContext;
		}		
		
		ProxyContext.setCurrentContext(ctx,request,appName);
		
		RouteSet rs = ctx.getRouter();
		if(rs == null) rs = ProxyContext.router;
		
		ActionWrapper action = null;
		PathInfo pi = parseUrl(path);
		Object target = rs.resolve(pi.path, request.getMethod());
		if(target instanceof ActionWrapper)
		{
			action = (ActionWrapper)target;
		}
		else if(rs != ProxyContext.router)
		{
			target = ProxyContext.router.resolve(pi.path, request.getMethod());
			if(target instanceof ActionWrapper)
			{
				action = (ActionWrapper)target;
			}			
		}		
		
		if(action != null)
		{
			/*判断是否拥有访问权限*/
			try
			{
				if(!ctx.hasPermission(action))
				{
					return redirect(request,response,2,"static/noperm.html");
				}
			}
			catch(SessionExpireException e)
			{
				return redirect(request,response,1,e.getUrl());
			}			
			
			doAction(action,request,response,pi);
			return true;
		}
		else if(path.startsWith("static/"))
		{
			return doStaticReq(ctx,path,request,response,true);
		}
		else
		{
			return false;
		}
	}
	
	protected boolean doStaticReq(TernContext ctx,String path,HttpServletRequest request,
			HttpServletResponse response,boolean mode)
	{
		File file = null;
		if(ctx != null)
		{
			String fullFilePath;
			if(mode) 
			{
				fullFilePath = ctx.getResourcePath() + "/" + path;
			}
			else
			{
				fullFilePath = ctx.getResourcePath() + "/static/" + path;
			}
			
			file = new File(fullFilePath);			
		}
		else
		{
			String fullFilePath = ProxyContext.defContext.getResourcePath() + "/" + path;
			file = new File(fullFilePath);
		}
		
		if(!file.exists())
		{
			if(mode)
			{
				//from iap
				try 
				{
					request.getRequestDispatcher("/"+path).forward(request,response);
					return true;
				} 
				catch (Exception e) 
				{					
				}
			}
			
			return false;
		}
		else		
		{
			response.reset();
			//response.setContentType("image/jpeg");
	        //response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
	        
	        int fileLength = (int) file.length();
            response.setContentLength(fileLength);
            
            try
            {
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
            catch(Exception e)
            {
            	return false;
            }
                        
            return true;
		}
	}
    
	static ActionWrapper resolvePath(String path,RouteSet rs1,RouteSet rs2)
	{
		ActionHandler ah = new ActionHandler();
		
		PathInfo pi = ah.parseUrl(path);
		if(rs1 != null)
		{
			Object target = rs1.resolve(pi.path, "GET");
			if(target instanceof ActionWrapper)
			{
				return (ActionWrapper)target;
			}
		}

		if(rs2 != null)
		{
			Object target = rs2.resolve(pi.path, "GET");
			if(target instanceof ActionWrapper)
			{
				return (ActionWrapper)target;
			}
		}

		
		return null;
	}
}
