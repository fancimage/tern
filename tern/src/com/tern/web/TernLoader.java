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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.tern.util.Trace;
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

	public static void findClasses(ClassLoader classLoader,ScanHandler handler,String path,String pkgname)
	{
		if(path == null || path.length() <= 0) return;
		File file = new File(path);
		if(!file.exists())
		{
			Trace.write(Trace.Warning, "find class[path=%s], but file does not exists.", path);
			return;
		}

		if(file.isDirectory())
		{
			String root = file.getAbsolutePath();
			if(pkgname != null && pkgname.length() > 0)
			{
				file = new File(file.getAbsolutePath()+File.separator + pkgname);
				if(!file.exists() || ! file.isDirectory())
				{
					Trace.write(Trace.Warning, "find class[path=%s], but not illegal class directory.", file.getAbsolutePath());
					return;
				}
			}
			findClassesFromDir(classLoader,file,root,pkgname,handler);
		}
		else if(path.endsWith(".jar"))
		{
			try
			{
				JarFile jarFile = new JarFile(file);
				findClassesFromJar(classLoader,jarFile,pkgname,handler);
			}
			catch (IOException e)
			{
				Trace.write(Trace.Error,e, "illegal jar file.");
				return;
			}
		}
	}

	private static void findClassesFromJar(ClassLoader classLoader,JarFile jarFile,String pkgname,ScanHandler handler)
	{
		Enumeration<JarEntry> enumes = jarFile.entries();
		int plen = pkgname.length();
		while (enumes.hasMoreElements())
		{
			JarEntry entry = enumes.nextElement();
			if(entry.getName().endsWith(".class") && entry.getName().startsWith(pkgname))
			{
				String className = entry.getName().replace('/', '.');
				if(!handler.matchName(className)) continue;

				className = className.substring(0, className.length() - 6);
				Class<?> clazz = null;//Class.forName(className);
				try
				{
					clazz = classLoader.loadClass(className);
				}
				catch (ClassNotFoundException e)
				{
				}

				if(clazz!=null)
				{
					int i = className.lastIndexOf('.');
					String pname = "";
					if(i>plen)
					{
						pname = className.substring(plen,i);
					}
					handler.process(clazz , pname);
				}
			}
		}
	}
	
	private static void findClassesFromDir(ClassLoader classLoader,File file,String root,
			String packageName,ScanHandler handler)
	{
		File[] files = file.listFiles();

		int plen = root.length()+1;
		String pname = file.getAbsolutePath().substring(plen+packageName.length());
		
		for (File f : files) 
		{
			if(f.isFile())
			{
				String fileName = f.getAbsolutePath().substring(plen);
				if (fileName.endsWith(".class") && fileName.startsWith(packageName))
				{
					String className = fileName.replace(File.separatorChar, '.');
					if(!handler.matchName(className)) continue;

					Class<?> clazz = null;
					className = className.substring(0, className.length() - 6);

					try
					{
						clazz = classLoader.loadClass(className);
					}
					catch (ClassNotFoundException e)
					{
					}

					if(clazz!=null)
					{
						handler.process(clazz , pname);
					}
				}
			}
			else
			{
				/*String _name = f.getName();
				if(packageName!=null && packageName.length()>0)
				{
					_name = packageName + "/" + _name;
				}*/

				findClassesFromDir(classLoader,f,root,packageName,handler);
			}
			
		}				
	}
	
	public static void scan(String _package, ScanHandler handler)
	{
		ClassLoader classLoader = TernLoader.class.getClassLoader();
		for(String src: srcPaths)
		{
			String _prefix = src + "." + _package;
			String _path = _prefix.replace('.','/');
			
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
					String protocol = resource.getProtocol();
					if("file".equals(protocol))
					{
						File f = new File(resource.getFile());
						String root = f.getAbsolutePath();
						root = root.substring(0,root.length() - _prefix.length()-1);

						String pname = _path;
						if(File.separatorChar == '\\')
						{
							pname = pname.replace('/','\\');
						}
						findClassesFromDir(classLoader,f,root,pname,handler);
					}
					else if("jar".equals(protocol))
					{
						try
						{
							JarFile jarFile = ((JarURLConnection) resource.openConnection()).getJarFile();
							findClassesFromJar(classLoader,jarFile,_path,handler);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
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
        		//Trace.write(Trace.Warning, "Application[%s] does not exists.", clazz);
    		}
    	}

		if(ret == null)
		{
			try
			{
				ret = Class.forName(name);
			}
			catch (ClassNotFoundException e)
			{
				Trace.write(Trace.Warning, "Application[%s] does not exists.", name);
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
