/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tern.util.TernContext;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.web.routes.ActionWrapper;

import java.beans.XMLEncoder;
import java.io.IOException;

import com.alibaba.fastjson.JSON;

public class ActionHandler implements IHandler
{	
	protected boolean doAction(ActionWrapper action,HttpServletRequest request,HttpServletResponse response,PathInfo pi)
	{
		Controller ctrl = action.getController();
		ctrl.init(request, response);
		ctrl.page = pi.page;
		ctrl.pageSize = pi.pageSize;
		
		Object ret = null;
		boolean logflag = Trace.needTrace(Trace.Information);
		if(logflag)
		{
			Trace.write(Trace.Information, "start action:%s,path=%s",action.toString(), request.getServletPath());
		}
		
		try
		{
		    ret = action.invoke();
		}
		catch(Exception e)
		{
			if(logflag)
			{
				Trace.write(Trace.Information, "end action:%s,and redirect." , ctrl.toString());
			}
			
			Throwable t = e.getCause();
			if(t instanceof RedirectRequest)
			{
				RedirectRequest tr = (RedirectRequest)t;
				return redirect(request,response,tr.action,tr.url);						
			}
			
			Trace.write(Trace.Error, e, "execute controller");
			
			//if(e instanceof ActionException){}
			
			return redirect(request,response,2,"static/500.html");
		}	
		
		if(logflag)
		{
			Trace.write(Trace.Information, "end action:%s." , ctrl.toString());
		}
		
		String view_path = null;
		Object vo = ctrl.getViewObject();
		if(pi.disposition != null && vo != null)
		{
			if ("json".equals(pi.disposition))
			{
				response.setContentType("text/javascript");
				response.setCharacterEncoding(config.getEncoding());
				
				ctrl.getStream()
				    .append(JSON.toJSONString(vo))
				    .flush();
				return true;
			}
			else if ("xml".equals(pi.disposition))
			{
				response.setContentType("text/xml");
				response.setCharacterEncoding(config.getEncoding());
				
				
				XMLEncoder out;
				try 
				{
					out = new XMLEncoder(response.getOutputStream());
					out.writeObject(vo);
		            out.flush();
		            out.close();
				} 
				catch (IOException e) 
				{
					Trace.write(Trace.Error, e,"serialize xml failed");
				}
				
				return true;
			}
		}
		
		if(ret instanceof String)
		{
			//2. string     --> specified view
			view_path = (String)ret;			
		}				
		else if(ctrl.stream != null && ctrl.stream.hasContent )
		{
			ctrl.stream.flush();
			return true;
		}
		else
		{
			//Default view
			view_path = action.getStaticPath();
		}
		
		if(view_path != null)
		{				
			request.setAttribute("page", vo);
			try
			{
				if(!TernContext.current().getTemplate().render(ctrl,view_path,request, response))
				{
					return redirect(request,response,2,"static/500.html");	
				}
			}
			catch(RedirectRequest t)
			{
				return redirect(request,response,t.action,t.url);	
			}			
			
			if(logflag)
			{
				Trace.write(Trace.Information, "end render:%s." , ctrl.toString());
			}
		}
		
		/*try 
		{
			request.getRequestDispatcher("/WEB-INF/view/"+view_path+".jsp").forward(request,response);
		} 
		catch(Throwable e)
		{
			Trace.write(Trace.Error, e, "execute view(%s) failed." , view_path );
		}*/
		
		return true;
	}
	
	protected boolean redirect(HttpServletRequest request,HttpServletResponse response,int mode,String url)
	{
		if(url!=null && url.startsWith("/"))
		{
			url = request.getContextPath() + url;
		}		

		try
		{
			if(1 == mode)
			{
				response.sendRedirect(url);
			}
			else
			{
				if(url.startsWith("static/"))
				{
					doStaticReq(TernContext.current(),url,request,response,true);
				}
				else
				{
				    request.getRequestDispatcher(url).forward(request,response);
				}
			}						
		}
		catch(Exception e1)
		{
			Trace.write(Trace.Error, e1,"%s:%s", (1==mode?"redirect":"forward"), url);
		}
		
		return true;
	}
	
	protected boolean doStaticReq(TernContext ctx,String path,HttpServletRequest request,
			HttpServletResponse response,boolean mode)
	{
		try 
		{
			request.getRequestDispatcher(path).forward(request,response);
		}
		catch (Exception e) 
		{			
		}
		return true;
	}
	
	protected PathInfo parseUrl(String path)
	{
		PathInfo pi = new PathInfo();
		int index = path.lastIndexOf("/page/");
		if(index >= 0)
		{
            String[] elements = path.substring(index + 6).split("/");
            if (elements.length >=1 || elements.length<=2)
            {
            	try 
            	{
                    pi.page = Integer.parseInt(elements[0]);
                    if (elements.length > 1) 
                    {
                        pi.pageSize = Integer.parseInt(elements[1]);
                    }
                    
                    path = path.substring(0, index);
                } 
            	catch (NumberFormatException ex) 
            	{
                }            	
            }
		}
		
		index = path.lastIndexOf('.');
		if(index >= 0)
		{
			String disposition = path.substring(index + 1);
			if (disposition.indexOf('/') < 0)
			{
				path = path.substring(0, index);
				pi.disposition = disposition;
			}
		}
		
		pi.path = path;
		return pi;
	}
	
	@Override
	public boolean execute(String path, HttpServletRequest request,
			HttpServletResponse response) 
	{
		PathInfo pi = parseUrl(path);
		Object target = TernContext.current().getRouter().resolve(pi.path, request.getMethod());
		if(target instanceof ActionWrapper)
		{
			doAction((ActionWrapper)target,request,response,pi);
			return true;
		}
		else
		{
			return false;
		}		
	}
    
	public static class PathInfo
	{
		public String path;
		String disposition;
		int page = -1;
		int pageSize = -1;
	}
}
