/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.tern.db.Database;
import com.tern.db.RowMapper;
import com.tern.util.TernContext;
import com.tern.util.Trace;
import com.tern.web.Template;
import com.tern.web.routes.ActionWrapper;
import com.tern.web.routes.RouteSet;

public class AppContext extends TernContext
{
	private String appPath;
	String contextPath;
	String appName;
	RouteSet router;
	Template template;
	Database metadb;
	
	/*缓存controller的权限信息*/
	Map<Object,Integer> actionPermission;
	Map<Object,Integer> ctrlPermission;
	
	static final String OPERATOR_KEY = "tern.operator";
	
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
	
	static boolean setCurrentOperator(Operator op)
	{
		ProxyContext.ContextData d = ProxyContext.localContext.get();
		if(d == null) return false;
		
		if(null == op)
		{
			d.req.getSession().removeAttribute(OPERATOR_KEY);
		}
		else
		{
		    d.req.getSession().setAttribute(OPERATOR_KEY, op);
		}
		return true;
	}
	
	public Menu[] getMenus()
	{
		if(this.metadb == null) return null;
		
		try
		{
			List<Menu> list = metadb.table("t_menu").order("mcode").query(new RowMapper<Menu>(){

				@Override
				public Menu map(ResultSet rs, int rowNum) throws SQLException {
					Menu m = new Menu(rs.getInt("mid"),rs.getString("mcode"),rs.getString("mcaption"),rs.getString("murl"),
							rs.getString("mtarget"),rs.getString("micon"));
					
					/*是其他菜单的子菜单？*/
					
					return m;
				}
				
			});
			
			Menu[] arr = new Menu[list.size()];
			arr = list.toArray(arr);
			return arr;
		}
		catch(SQLException e)
		{
			Trace.write(Trace.Error, e, "load menus");
		}
		
		return null;
	}
	
	public boolean hasPermission(ActionWrapper action)
	{
		if(actionPermission == null) return true;
		
		Integer mid = actionPermission.get(action.getMethod());
		if(mid != null)
		{
			return Operator.current().hasPermission(mid);
		}
		
		if(ctrlPermission != null)
		{
			mid = ctrlPermission.get(action.getController().getClass());
			if(mid != null)
			{
			    return Operator.current().hasPermission(mid);
			}
		}
		
		return true;
	}
	
	void assignPermission()
	{
		Menu[] arr = getMenus();
		if(arr != null)
		{
			actionPermission = new HashMap<Object,Integer>();
			ctrlPermission = new HashMap<Object,Integer>();
			
			for(Menu m:arr)
			{
				String path = m.getUrl();
				if(path == null || path.length() <=0)
				{
					continue;
				}
				
				ActionWrapper action = ActionHandler.resolvePath(path, this.router, Application.getRouter());
				if(action == null)
				{
					Trace.write(Trace.Error, "menu[%d,%s]:%s,can not find handler.", m.getId(),m.getCode(),path);
				}
				else
				{
					actionPermission.put(action.getMethod(), m.getId());
					
					if(action.getMethod().getName().equals("index"))
					{
						ctrlPermission.put(action.getController().getClass(), m.getId());
					}
				}
			}
		}
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
	public Object currentOperator()
	{
		ContextData d = localContext.get();
		if(d == null) return null;
		
		Object ret = d.req.getSession().getAttribute(AppContext.OPERATOR_KEY);
		if(ret == null) throw new SessionExpireException();
		else return ret;
	}
	
	@Override
	public Database getMetaDB(){return proxy().metadb;}
}
