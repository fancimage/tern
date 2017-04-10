/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

import com.tern.db.db;
import com.tern.util.TernContext;
import com.tern.util.config;
import com.tern.util.Trace;
import com.tern.web.routes.RouteSet;

public class TernWebApplication 
{
	private static final String VERSION = "v1.04 2017-04-10";
	private static TernWebApplication instance;
	
	/*default handlers*/
	protected IHandler[] handlers = new IHandler[]{
			new ActionHandler()
	};
	
	protected boolean beforStart(ServletContext c)
	{
		new TernContext(){
			private Template template;
			private RouteSet router;
			public void init(ServletContext ctx)
			{
				current = this;
				context = ctx;
				
				String type = config.getString("server.template");
				if(type == null)
				{
					type = "freemarker";
				}
				
				if(type.equals("jsp"))
				{
				}
				else
				{
					try
					{
					    template = new FreemarkerTemplate();
					}
					catch(Throwable t)
					{
						System.out.println("notice: no Freemarker support!");
					}
				}
				
				router = new RouteSet();
			}
			
			public Template getTemplate()
			{
				return template;
			}
			
			public RouteSet getRouter()
			{
				return router;
			}
			
		}.init(c);
		
		return true;
	}
	
	protected boolean onStarted(ServletContext context)
	{
		return true;
	}
	
	protected boolean onDestoryed()
	{
		return true;
	}
	
	protected void configRoutes()
	{
		String packageName = "controllers";
		final int plen = packageName.length();
		final RouteSet router = TernContext.current().getRouter();
		
		TernLoader.scan(packageName, new TernLoader.ScanHandler() {
			
			@Override
			public void process(Class<?> clazz,String relitivePath)
			{
				if(!Controller.class.isAssignableFrom(clazz))
				{
					return;
				}
				
				String defaultPath = "/";
				if(relitivePath!=null && relitivePath.length()>0)
				{
					defaultPath += relitivePath.replace('.', '/')+"/";
				}
				
				String str = clazz.getName();
				int i = str.lastIndexOf(".");
				if(i >= 0)
				{
					str = str.substring(i+1);
				}
				
				str =str.substring(0, str.length()-plen+1);
				str = str.substring(0,1).toLowerCase() + str.substring(1);
				
				if(str.equals("home"))
				{
					router.addController((Class<Controller>)clazz, defaultPath+"*");
				}
				else
				{
					router.addController((Class<Controller>)clazz, defaultPath+str+"/*");
				}
			}
			
			public boolean matchName(String fileName)
			{
				if(fileName.endsWith("Controller.class"))
				{
				    return true;
				}
				
				return false;
			}
			
		});
		
		router.sort();
	}
	
	static void setInstance(TernWebApplication ins)
	{
		instance = ins;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends TernWebApplication> T getInstance()
	{
		return (T)instance;
	}
	
	public boolean isAuthorized(HttpSession session)
	{
		return session!=null&&session.getAttribute("user") != null;
	}
	
	public String getLoginURL(){return "login.jsp";}
	
	protected void pushHandler(IHandler h)
	{
		if( h == null ) return;
		
		if(handlers == null)
		{
			handlers = new IHandler[1];
		}
		else
		{
			IHandler[] arr = new IHandler[handlers.length+1];
			
			for(int i=0;i<handlers.length;i++)
			{  
				if(h == handlers[i]) return; //allready in handers
				
				arr[i+1] = handlers[i];
			}
			
			handlers = arr;
		}
		
		handlers[0] = h;
	}
	
	final IHandler[] getHandlers()
	{
		return handlers;
	}
	
	boolean start(ServletContext context)
	{
		String rootPath = context.getContextPath();//getWebRootPath(context);
		if (rootPath == null)
        {
            return false;
        }
		
		System.out.println("["+rootPath+"] "+this.getClass().getName()+" starting...");
		
		config.setRoot(rootPath);
		//context.setAttribute("root", config.getRoot());
		
		//Template.context = context;
		if(!beforStart(context))
		{
			return false;
		}
		
		//Template.init(context);
        
        /*init logger*/
        String traceFile = config.getString("log.file");
        if(traceFile == null)
        {
        	//traceFile = "std";
        	traceFile = "tern.log";
        }
        if(traceFile!=null && traceFile.compareToIgnoreCase("std")!=0)
        {
            traceFile = context.getRealPath("/WEB-INF/"+traceFile);
            //TRACE.Force
            Trace.setTraceFile(traceFile,config.getBool("log.force", config.isDebug()));
            System.out.println("["+rootPath+"] log to [" + traceFile+"]");
        }
        
        //log4j
        java.io.File log4jConfig = new java.io.File(context.getRealPath("/WEB-INF/log4j.properties"));
        if(log4jConfig.exists())
        {
        	org.apache.log4j.PropertyConfigurator.configure(log4jConfig.getPath());        	
        }
        else
        {
        	if(null == Thread.currentThread().getContextClassLoader().getResource("log4j.properties"))
        	{
        		//default,using Trace-logger
            	java.util.Properties props = new java.util.Properties();
            	props.put("log4j.rootCategory", "ALL, console");
            	props.put("log4j.appender.console", "com.tern.util.TraceAppender");
            	org.apache.log4j.PropertyConfigurator.configure(props);	
        	}        	
        }
        
        Logger rootLogger = Logger.getRootLogger();
        Enumeration allAppenders = rootLogger.getAllAppenders();
        while (allAppenders.hasMoreElements())
        {
            Appender appender = (Appender) allAppenders.nextElement();
            
            System.out.println("Log4j: " + appender.getName() + " = "
                + appender.getClass().getCanonicalName() + ",Level = "
                + rootLogger.getLevel().toString());
        }
                
        //Trace.write(Trace.Running,"======================================================================================================");
        Trace.wrap();
        Trace.write(Trace.Running,"=> Tern(Version: %s) Web Appliaction[%s] starting",VERSION,
        		this.getClass().getName());
        Trace.write(Trace.Running,"Container: v%d.%d (%s),Context Path= %s",
        		context.getMajorVersion(),context.getMinorVersion(),
        		context.getClass().getName(),
        		rootPath);
        
        /*resource*/
        try
        {
            String tmpPath = context.getRealPath("/WEB-INF/resource");
            if(tmpPath != null)
            {
        	    StringResource.resPath = tmpPath;//.substring(0,tmpPath.lastIndexOf("/"));
            }
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
        
        //routes
        configRoutes();
        
        //init connections pool
        Map dbConfig = config.getMap("database");
    	if(dbConfig == null)
    	{
    		Trace.write(Trace.Running, "notice: No database config!");
    	}    	    	
    	else
    	{
    		String poolType = config.getString("database.pool");
    		Object[] arr = config.getArray("database.all");
    		if(arr != null)
    		{
    			for(Object obj:arr)
        		{
        			if(!(obj instanceof Map)) continue;
        			
        			Map props = (Map)obj;
        			if(!props.containsKey("pool"))
        			{
        			    props.put("pool", poolType);
        			}
        			
        			Trace.write(Trace.Running, "=> initialize database:"+props);
        			try
        			{
        			    db.establish(props);
        			}
        			catch(SQLException e)
        			{
        				Trace.write(Trace.Error,e, "Database initialze failed");
        				e.printStackTrace();
        			}
        		}
    		}    		
    	}
        
        Trace.write(Trace.Running, "timezone = "+ System.getProperty("user.timezone"));
        Trace.flush();
        
        //load default IDCreator
        //com.tern.data.RowID.installCreator(new com.tern.data.DefaultIDCreator());
		
        boolean ret = onStarted(context);
        Trace.flush();
        
		return ret;
	}	
	
	boolean destory()
	{
		//DBProviderFactory.destoryAll();
    	db.closeAll();
		
    	boolean ret = onDestoryed();    
    	
    	Trace.write(Trace.Running,"======Tern Web Appliaction stoped============================");
    	Trace.flush();
    	
    	return ret;
	}
	
	/*private static String getWebRootPath(ServletContext context)
    {
        try
        {
            String contextStr = context.toString();
            String str = context.getContextPath();
            //System.out.println(contextStr);
            java.net.URL url = context.getResource("/");
            String tmp = null;
            if (url == null) //resin
            {
                tmp = context.toString();
                tmp = tmp.substring(tmp.indexOf("["), tmp.lastIndexOf("]"));
            }
            else 
            {
                //webLogic(ServletContext(id=33095721,name=entc_web,context-path=))
                if (contextStr.indexOf("context-path") >= 0) //webLogic
                {
                    int i = contextStr.indexOf("context-path");
                    int j = contextStr.indexOf(",", i);
                    int m = contextStr.indexOf(")", i);
                    if (j < 0)
                    {
                        j = contextStr.length();
                    }
                    if (m < 0)
                    {
                        m = contextStr.length();
                    }
                    if (m < j)
                    {
                        j = m;
                    }
                    tmp = contextStr.substring(i, j);
                    String[] arr = tmp.split("=");
                    if (arr.length >= 2)
                    {
                        tmp = arr[1];
                    }
                    else
                    {
                        tmp = "";
                    }
                }
                else
                { //tomcat
                    tmp = url.getPath();
                    if (tmp.endsWith("/"))
                    {
                        tmp = tmp.substring(0, tmp.length() - 1);
                    }
                }
            }

            int i = tmp.lastIndexOf("/");
            if (i < 0)
            {
                i = 0;
            }
            tmp = tmp.substring(i);
            return tmp;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Trace.write(Trace.Error,"can not get the root path of web application.");
            return null;
        }
    }*/
}
