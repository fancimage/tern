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
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tern.util.Trace;
import com.tern.util.config;
//import com.tern.util.Trace;

//import fancimage.web.db.ConnectionManager;

public class TernFilter implements Filter
{
    //protected FilterConfig filterConfig;
	TernWebApplication webApp = null;
	IHandler[] handlers;

	Set<String> staticPaths = new HashSet<String>();
	
	//private int rootPathLen;

    public void init(FilterConfig cfg)
    {
    	//Load configuration
		String appConfig = null;
		try
        {
			String encoding = cfg.getInitParameter("encoding");
			if(encoding==null || encoding.trim().length()<=0)
			{
				encoding = "utf-8";
			}
            appConfig = cfg.getServletContext().getRealPath("/WEB-INF/app.yml");
            config.load(appConfig,encoding);
        }
        catch(Exception e)
        {
        	System.out.println("load web-inf/app.config error:" + e.getMessage());        	
        }
				
		TernLoader.init();

		Object[] paths = config.getArray("server.static");
		if(paths!=null && paths.length > 0)
		{
			for(Object s : paths)
			{
				staticPaths.add(s.toString());
			}
		}
		else
		{
			staticPaths.add("static");
			staticPaths.add("skins");
			staticPaths.add("js");
		}
		
		String clazzName = config.getString("application.class");
		if(clazzName==null || clazzName.trim().length()<=0)
		{
			clazzName = "Application";
		}
		
		Class<?> webClass = TernLoader.loadClass(clazzName,false); 
		if(webClass!=null && TernWebApplication.class.isAssignableFrom(webClass))
	    {			
	    	try 
	    	{
				webApp = (TernWebApplication)webClass.newInstance();
			} 
	    	catch (Exception e) 
	    	{
	    		System.out.println("Create WebApplication failed: " + clazzName);
	    		throw new RuntimeException(e);
			}        	    	
	    }
	    else if( config.getString("application.class") != null )
	    {
	    	System.out.println("Wrong WebApplication definition: " + clazzName);        	    	
	    }    	        
        
        if(webApp == null)
        {
        	webApp = new TernWebApplication();
        }
		TernWebApplication.setInstance(webApp);
        
        if(!webApp.start(cfg.getServletContext()))
        {
        	throw new RuntimeException("=> fatal:init tern failed.");
        }
        
        //load plugins
        
        
        //rootPathLen = config.getRoot().length();
        handlers = webApp.getHandlers();
        if(null == handlers)
        {
        	handlers = new IHandler[0];
        }
    }

    public void destroy()
    {
    	if(webApp != null)
    	{
    		webApp.destory();
    	}
    }

    public void doFilter(ServletRequest srequest, ServletResponse sresponse,
                         FilterChain filterChain)
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) srequest;
        HttpServletResponse response = (HttpServletResponse) sresponse;
        
        String encoding = com.tern.util.config.getEncoding();
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        //response.setContentType("text/html; charset=" + encoding);

        //String path = request.getRequestURI().substring(request.getContextPath().length());
        String path = request.getServletPath();
        
        //regularize path
        if(path.endsWith("/"))
        {
        	path = path.substring(0,path.length()-1);
        }
        
        //if(path.endsWith(".jsp"))
        //{
        //	filterChain.doFilter(srequest, sresponse);
        // 	return;
        //}
        
        //pre-plug-handlers
        try
        {
        	for(IHandler h:handlers)
            {
            	if(h.execute(path, request, response))
            	{
            		return;
            	}
            }
        }
        catch(Throwable t)
        {        	
        	if(config.isDebug())
        	{
        		throw new ServletException(t);
        	}
        	else
        	{
        		Trace.write(Trace.Error, t,"handlers");
        		//to 500
        		return;
        	}
        }
        
        if(config.isDebug() && Trace.needTrace(Trace.Information))
        {
        	if(path.length() > 0 && !path.endsWith(".jsp"))
        	{
				//&& !path.startsWith("/static") && !path.startsWith("/js") && !path.startsWith("/skins")
				int _last = path.indexOf("/",1);
				if(_last < 1) _last = path.length();
				String prefix = path.substring(1,_last);
				if(!staticPaths.contains(prefix))
				{
					Trace.write(Trace.Information, "un-handler request: %s,method=%s", path , request.getMethod());
				}
        	}
        }
        filterChain.doFilter(srequest, sresponse);                
    }
}
