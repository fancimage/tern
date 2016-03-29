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
import com.tern.web.routes.ActionWrapper;
import com.tern.web.routes.RouteSet;

public class ActionHandler implements IHandler
{	
	protected boolean doAction(ActionWrapper action,HttpServletRequest request,HttpServletResponse response)
	{
		Controller ctrl = action.getController();
		ctrl.init(request, response);
		
		Object ret = null;
		
		try
		{
		    ret = action.invoke();
		}
		catch(Exception e)
		{
			Throwable t = e.getCause();
			if(t instanceof RedirectRequest)
			{
				RedirectRequest tr = (RedirectRequest)t;
				String url = null;
				if(tr.url!=null && tr.url.startsWith("/"))
				{
					url = request.getContextPath() + tr.url;
				}
				else
				{
					url = tr.url;
				}

				try
				{
					if(1 == tr.action)
					{
						response.sendRedirect(url);
					}
					else
					{
						request.getRequestDispatcher(url).forward(request,response);
					}
					
					return true;
				}
				catch(Exception e1)
				{
					Trace.write(Trace.Error, e1,"%s:%s", (1==tr.action?"redirect":"forward"), url);
					return false;
				}								
			}
			
			Trace.write(Trace.Error, e, "execute controller");
			
			//if(e instanceof ActionException){}
			
			try
			{
			    response.sendError(501);
			}
			catch(Exception e1)
			{					
			}
			
			return false;
		}			
		
		String view_path = null;
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
			if(!TernContext.current().getTemplate().render(ctrl,view_path,request, response))
			{
				try
				{
				    response.sendError(502); //template error
				}
				catch(Exception e1)
				{					
				}
				
				return false;
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
	
	@Override
	public boolean execute(String path, HttpServletRequest request,
			HttpServletResponse response) 
	{
		Object target = TernContext.current().getRouter().resolve(path, request.getMethod());
		if(target instanceof ActionWrapper)
		{
			doAction((ActionWrapper)target,request,response);
			return true;
		}
		else
		{
			return false;
		}		
	}
    
}
