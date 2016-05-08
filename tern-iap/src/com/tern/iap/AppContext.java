/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.tern.db.Database;
import com.tern.util.TernContext;
import com.tern.web.Template;
import com.tern.web.routes.RouteSet;

public class AppContext extends TernContext
{
	private String appPath;
	String contextPath;
	String appName;
	RouteSet router;
	Template template;
	Database metadb;
	
	public AppContext(String name,String path)
	{
		appName = name;
		appPath = path;		
		template = new FreemarkerTemplate(path);
	}
	
	@Override
	public Template getTemplate()
	{
		return template;
	}
	
	@Override
	public String getResourcePath()
	{
		return appPath;
	}
	
	@Override
	public RouteSet getRouter()
	{
		return router;
	}
	
	@Override
	public Database getMetaDB(){return metadb;}
	
	public String getApplicationName()
	{
		return appName;
	}
	
	public static AppContext current()
    {
    	return ((ProxyContext)current).proxy();
    }
	
	public String getContextPath()
    {
    	return contextPath;
    }
	
	public static AppContext getAppContext(String name)
	{
		return (AppContext)Application.appContexts.get(name);
	}
	
	public static AppContext getDefault()
	{
		return ProxyContext.defContext;
	}
	
	public static String getCurrentAppName()
	{
		ProxyContext.ContextData d = ProxyContext.localContext.get();
		return d == null?null:d.appName;
	}
	
	public static HttpServletRequest getRequest()
	{
		ProxyContext.ContextData d = ProxyContext.localContext.get();
		return d == null?null:d.req;
	}
	
	public static Operator getCurrentOperator()
	{
		ProxyContext.ContextData d = ProxyContext.localContext.get();
		return d == null?null:(Operator)d.req.getSession().getAttribute("tern.operator");
	}
}

class ProxyContext extends TernContext
{
	static AppContext defContext;
	static final ThreadLocal<ContextData> localContext = new ThreadLocal<ContextData>();
	
	static RouteSet router;
	
	static class ContextData
	{
		public AppContext app;
		public HttpServletRequest req;
		public String appName;			
	}
	
	void init(ServletContext c)
	{
		current = this;
		context = c;
		router = new RouteSet();
	}
	
	static void setCurrentContext(AppContext ctx,HttpServletRequest req,String appname)
	{
		ContextData d= new ContextData();
		d.app = ctx;
		d.req = req;
		d.appName = appname;
		localContext.set(d);
	}
	
	AppContext proxy()
	{
		ContextData d = localContext.get();
		if(null == d) return defContext;
		return d.app;
	}
	
	@Override
	public Template getTemplate()
	{
		return proxy().getTemplate();
	}
	
	@Override
	public String getResourcePath()
	{
		return proxy().getResourcePath();
	}
	
	@Override
	public RouteSet getRouter()
	{
		TernContext c = proxy();
		if(c != null) return c.getRouter();
		else return router;
	}
	
	public String getApplicationName()
	{
		return proxy().appName;
	}
	
	public String getContextPath()
    {
		return proxy().getContextPath();
    }
	
	@Override
	public Database getMetaDB(){return proxy().metadb;}
}
