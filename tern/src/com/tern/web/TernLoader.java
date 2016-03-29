/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

import com.tern.util.config;

public class TernLoader 
{
	private static String[] srcPaths;
	private static Map<String,Class<?>> caches = new HashMap<String,Class<?>>();
	
	static void init()
	{
		int buildin = 0;
		boolean iap = false;
		if(testPlugin("com.iap"))
		{
			iap = true;
			buildin++;
		}		
		
		boolean ternJsp = false;
		if(testPlugin("com.tern.jsp"))
		{
			ternJsp = true;
			buildin++;
		}	
		
		Object[] v = config.getArray("application.src");
		int i = 0;
		if(v == null || v.length <= 0)
		{
			srcPaths = new String[buildin];
		}
		else
		{
			srcPaths = new String[v.length + buildin];
			for(;i<v.length;i++)
			{
				srcPaths[i] = v[i]==null?null:v[i].toString();
			}
		}
		
		if(iap)
		{
			srcPaths[i] = "com.iap";
			i++;
		}
		
		if(ternJsp)
		{
			srcPaths[i] = "com.tern.jsp";
		}
	}
	
	private static boolean testPlugin(String plugin)
	{
		try
		{
			Class.forName(plugin+".Application");
		}
		catch(Exception e)
		{
			return false;
		}
		
		return true;
	}
	
	/*public static void addSourcePath(String src)
	{
		if(srcPaths == null)
		{
			srcPaths = new String[1];
			srcPaths[0] = src;
		}
		else
		{}
	}*/
	
	public static Class<?> loadClass(String name)
	{
		return loadClass(name,true);
	}
	
	public abstract static class ScanHandler
	{
		//Class<?> type,
		public boolean matchName(String fileName)
		{
			return true;
		}
		
		public abstract void process(Class<?> clazz,String relitivePath);
	}
	
	public static void findClasses(ClassLoader classLoader,File file,String prefix,
			String packageName,ScanHandler handler)
	{		
		if(!file.isDirectory()) return;
		
		File[] files = file.listFiles();
		
		for (File f : files) 
		{
			if(f.isFile())
			{
				String fileName = f.getName();
				if (fileName.endsWith(".class")) 
				{
					if(handler.matchName(fileName))
					{
						try 
						{
							String className = prefix;
							if(packageName!=null && packageName.length()>0)
							{
								if(className.length()>0)
								{
									className += '.';
								}
								className += packageName;
							}
							if(className.length()>0)
							{
								className += '.';
							}
							
							className += fileName.substring(0, fileName.length() - 6);							
							Class<?> clazz = classLoader.loadClass(className);//Class.forName(className);
							if(clazz!=null) handler.process(clazz , packageName);
						} 
						catch (ClassNotFoundException e)
						{
						}
					}
				}
			}
			else
			{
				String _name = f.getName();
				if(packageName!=null && packageName.length()>0)
				{
					_name = packageName + "." + _name;
				}
				
				findClasses(classLoader,f,prefix,_name,handler);
			}
			
		}				
	}	
	
	public static void scan(String _package, ScanHandler handler)
	{		
		ClassLoader classLoader = TernLoader.class.getClassLoader();
		for(String src: srcPaths)
		{
			String _prefix = src + "." + _package;
			String _path = _prefix.replace('.', '/'); 
			
			Enumeration<URL> resources = null;
			
			try
			{
				resources = classLoader.getResources(_path);
			}
			catch (IOException e) 
			{
			} 
			
			if(resources != null)
			{
				while (resources.hasMoreElements()) 
				{ 
					URL resource = resources.nextElement(); 
					File f = new File(resource.getFile());					
					
					findClasses(classLoader,f,_prefix,"",handler);
				}
			}
			
		}
	}
	
    public static Class<?> loadClass(String name,boolean cached)
    {
    	Class<?> ret = null;
    	if(!config.isDebug() && caches.containsKey(name))
    	{
    		return caches.get(name);
    	}
    	
    	for(String src: srcPaths)
    	{
    		String clazz;
    		if(name.startsWith(src))
    		{
    			//fullname?
    			clazz = name;
    			name = name.substring(src.length());
    			if(!config.isDebug() && caches.containsKey(name))
    	    	{
    	    		return caches.get(name);
    	    	}
    		}
    		else
    		{
    			clazz = src + "." + name;
    		}
    		
    		try 
        	{
    			ret = Class.forName(clazz);
    			break;
    		} 
        	catch (ClassNotFoundException e) 
        	{
        		//pass
    		}
    	}
    	
    	if(!config.isDebug() && cached )
    	{
    		synchronized(caches)
    		{
    			if(!caches.containsKey(name))
    			{
    				caches.put(name, ret);	
    			}    			    		   
    		}
    	}
    	
    	return ret;
    }
}
