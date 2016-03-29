/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import java.util.HashMap;
import java.util.Properties;
//import java.util.Locale;

import com.tern.util.config;
import com.tern.util.Trace;

/**
 * <p>Title: 支持字符串国际化</p>
 * <p>Description: 为页面提供国际化支持</p>
 * <p>Copyright: Copyright iEAS(c) 2010</p>
 * @author iEAS Fancimage
 * @version 1.0
 */
public class StringResource
{
	HashMap resMap = new HashMap();
	
	static String resPath;
	static String defaultLang;
	
	static StringResource res = new StringResource();
	
	static
	{
		defaultLang = config.getString("lang");
		if(defaultLang == null)
		{
			defaultLang = "zh-CN";
		}
	}
	
	private StringResource()
	{
		
	}
		
	
	public static String getString(String key)
	{
		return getString(key,null,null);
	}
	
	public static String getString(String key,String lang)
	{
		return getString(key,lang,null);
	}
	
	public static String getString(String key,String lang,String defaultStr)
	{
		if(lang==null || lang.length()<=0)
		{
		    lang = defaultLang;	
		}
		
		Properties p = (Properties)res.resMap.get(lang);
		if(p==null)
		{
			//lang是否合法?
			//Locale local = new Locale(lang);
			synchronized(res.resMap)
			{
				p = (Properties)res.resMap.get(lang);
				if(p==null)
				{
					if(resPath == null)
					{
						return defaultStr;
					}
					
					String path = resPath+"/string."+lang+".properties";
					java.io.FileInputStream ins= null;
					
					try
					{
						ins = new java.io.FileInputStream(path);
						
						//读取资源文件
						p = new Properties();
						p.load(ins);
						
						//对编码进行处理，统一按UTF-8进行处理？				
						/*java.util.Enumeration it = p.propertyNames();
		                while (it.hasMoreElements())
		                {
		                    String name = it.nextElement().toString();
		                    String value = p.getProperty(name);
		                    value = new String(value.getBytes("iso-8859-1"), "UTF-8");
		                    p.setProperty(name, value);
		                }*/		                
						
						res.resMap.put(lang, p);
					}
					catch(Exception e)
					{
						Trace.write(Trace.Error, e,"Load %s resource failed:",lang);
						return defaultStr;
					}
					finally
					{
					     if(ins!=null)
					     {
					    	 try
					    	 {
					    	     ins.close();
					    	 }catch(Exception e1){}
					     }
					}
				}
			}											 
		}		
		
		String ret = p.getProperty(key);
		return ret==null?defaultStr:ret;
	}
	
	public static String getLanguage(javax.servlet.jsp.PageContext pageContext)
	{
		Object obj = null;
		
		try
		{
		    obj = pageContext.getSession().getAttribute("lang");
		    if(obj==null)
		    {
			    obj = pageContext.getAttribute("lang");
			    if(obj == null)
			    {
				    obj = pageContext.getRequest().getParameter("lang");
			    }
		    }
		}
		catch(Exception e)
		{
			Trace.write(Trace.Error,e, "Get Language:");
			obj = null;
		}
		
		if(obj==null)
		{
			return null;
		}
		else
		{
		    return obj.toString();	
		}
	}
}
