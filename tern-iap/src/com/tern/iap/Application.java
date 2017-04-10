/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;

import com.tern.dao.DBModelReader;
import com.tern.dao.Model;
import com.tern.dao.ModelException;
import com.tern.dao.ModelReader;
import com.tern.dao.ModelReaderFactory;
import com.tern.dao.YamlModelReader;
import com.tern.db.Database;
import com.tern.db.db;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.web.Controller;
import com.tern.web.TernLoader;
import com.tern.web.TernWebApplication;
import com.tern.web.routes.RouteSet;

import freemarker.template.Configuration;

public class Application extends TernWebApplication
{
	static Map<String,AppContext> appContexts = new HashMap<String,AppContext>();
	
	public String[] getAppNames()
	{
		Set<String> sets = appContexts.keySet();
		String[] ret = new String[sets.size()];
		return sets.toArray(ret);
	}
	
	public AppContext getAppContext(String name)
	{
		return (AppContext)appContexts.get(name);
	}
	
	protected boolean beforStart(ServletContext context)
	{
		handlers[0] = new ActionHandler();  //replace the default action handler
		new ProxyContext().init(context);
		return true;
	}
	
	public static RouteSet getRouter()
	{
		return ProxyContext.router;
	}
	
	protected boolean onStarted(ServletContext context)
	{
		Model.setModelReaderFactory(new ModelReaderFactory(){

			@Override
			public ModelReader createReader()
			{
				Database metadb = AppContext.current().metadb;
				if(metadb == null)
				{
					return new YamlModelReader();
				}
				else
				{
					return new IapModelReader( metadb );
				}
			}

		});

		//find user applications
		int appCount = 0;
		Object[] appConfigs = config.getArray("apps");
		if(appConfigs != null)
		{
			config.remove("apps");
			
			for(Object obj:appConfigs)
    		{
    			if(!(obj instanceof Map)) continue;
    			Map props = (Map)obj;
    			    			
    			String name = Convert.toString(props.get("name"));
    			String path = Convert.toString(props.get("path"));
    			File f = new File(path);
    			if(!f.exists() || !f.isDirectory())
    			{
    				Trace.write(Trace.Error, "the path of app[%s] is invalide!!", name);
    				continue;
    			}
    			
    			//app.yml MUST exist!
    			f = new File(path+"/app.yml");
    			if(!f.exists() || !f.isFile())
    			{
    				Trace.write(Trace.Error, "the config file(app.yml) of app[%s] does not exists!!", name);
    				continue;
    			}
    			
    			config.addConfig(name, f);
    			    			
    			load_app(name,path,appCount);
    			appCount++;
    		}
		}
		
		//load user application from WEB-INF/apps/
		
		return true;
	}
	
	private void load_app(String name,String path,int index)
	{
		ClassLoader pLoader = getClass().getClassLoader();
		IapClassLoader appLoader = new IapClassLoader(path,pLoader);
		//1. load configuration	
		
		//meta.db
		Database metadb = null;
		if(!name.equals("ide"))
		{
		    final File f = new File(path+"/models/meta.db");
		    if(f.exists() && f.isFile())
		    {
		    	try
		    	{
		    		metadb = db.establish(new java.util.HashMap<String, Object>(){{
		    			this.put("pool", config.getString("database.pool"));
			    	    this.put("dbn", "sqlite");
			    	    this.put("db", f.getAbsolutePath());
			    	    this.put("encoding", "utf-8");
			    	    this.put("user", "root");
		    		}});
		    		
		    		Trace.write(Trace.Running, "load meta db for app[%s] succ!" , name);
		    	}
		    	catch(java.sql.SQLException e)
		    	{
		    		Trace.write(Trace.Error, e, "load meta db for app[%s] failed!" , name);
		    	}
		    	
		    }
		}
		
		//2. to replace Trace , Config??
		
		//3. load controllers
		String defPre = index==0?null:name;
		ControllerLoader loader = new ControllerLoader(defPre);
		TernLoader.findClasses(appLoader,loader,
				appLoader.getClassesPath(), "controllers");
		
		//app context
		AppContext ctx = new AppContext(name,path);
		ctx.router = loader.router;
		ctx.metadb = metadb;
		ctx.classLoader = appLoader;
		if(0 == index) 
		{
			ProxyContext.defContext = ctx;
			ctx.contextPath = config.getRoot();
		}
		else
		{
			ctx.contextPath = config.getRoot()+name+"/";
		}
		appContexts.put(name, ctx);
		
		/*标记菜单的权限*/
		ctx.assignPermission();
		
		Trace.write(Trace.Running, "app[%s] load successfully!!", name);
	}
	
	protected boolean onDestoryed()
	{
		return true;
	}
	
	class ControllerLoader extends TernLoader.ScanHandler
	{
		final int plen = "controllers".length();
		
		private String appName;
		RouteSet router;
		
		public ControllerLoader(String name)
		{
			appName = name;
		}
		
		@Override
		public void process(Class<?> clazz, String relitivePath) 
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
			
			if(router == null) router = new RouteSet();
			if(str.equals("home"))
			{
				router.addController((Class<Controller>)clazz, defaultPath+"*");
			}
			else
			{
				router.addController((Class<Controller>)clazz, defaultPath+str+"/*");
			}
		}
		
	}
}

class IapModelReader extends DBModelReader
{
	public IapModelReader(Database db)
	{
		super(db);
	}
	
	@Override
	public boolean read(Model m) throws ModelException
	{
		if(!super.read(m))
		{
			return new YamlModelReader().read(m);
		}
		
		return true;
	}
}

class IapClassLoader extends ClassLoader
{
	private String appPath;
	Configuration templateCfg;
	URLClassLoader jarLoader;

	public IapClassLoader(String path,ClassLoader p)
	{
		super(p);
		appPath = path;

		File file = new File(path+"/classes.jar");
		if(file.exists() && file.isFile())
		{
			try
			{
				jarLoader = new URLClassLoader(new URL[]{new URL(file.getAbsolutePath())});
			}
			catch(Exception e)
			{
				jarLoader = null;
			}
		}
	}
	
	public String getAppPath(){return appPath;}
	public String getClassesPath()
	{
		if(jarLoader != null) return appPath+"/classes.jar";
		else return appPath+"/bin";
	}
	
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException
	{
		if(jarLoader != null)
		{
			return jarLoader.loadClass(name);
		}

		byte[] data = loadClassData(name);
		if(data==null || data.length<=0) return null;
		else return defineClass(name,data,0,data.length);
	}
		
	public byte[] loadClassData(String name)
	{
		FileInputStream fis=null ;
        byte[] data = null ;
        
        name = Convert.replaceAll(name, ".", "/");
        
        try
        {
           fis = new FileInputStream(new File(appPath+"/bin/"+name+".class"));
           ByteArrayOutputStream baos = new  ByteArrayOutputStream();
           int  ch  =   0 ;
           while  ((ch  =  fis.read())  !=   - 1 )
           {
               baos.write(ch);              
           }
           data  =  baos.toByteArray();
        }  
        catch(IOException e)
        {
           return null;
        }
        finally
        {
        	if(fis != null)
        	{
				try 
				{
					fis.close();
				} 
				catch (IOException e) 
				{}
        	}
        }
       
        return  data;
	}
}
