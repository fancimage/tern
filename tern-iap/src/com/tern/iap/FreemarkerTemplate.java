/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.iap.workflow.Service;
import com.tern.util.TernContext;

import freemarker.cache.TemplateLoader;

class FreemarkerTemplate extends com.tern.web.FreemarkerTemplate
{
	public FreemarkerTemplate(String path)
	{
		super();
		cfg.setTemplateLoader(new FreemarkerTemplateLoader(path));
	}
	
	@Override
	protected freemarker.template.Template getTemplate(String path,HttpServletRequest request)
	{
		try
		{
			return cfg.getTemplate(path+".html");
		}
		catch (IOException e)
		{
			String secondPath = null;
			if(request.getAttribute("records") instanceof RecordSet
				&& path.endsWith("/index") )  //search default template
			{
				if(path.endsWith("/index"))
				{
					if(request.getAttribute("service") instanceof Service)
					{
						secondPath = "process/index";
					}
					else
					{
					    secondPath = "model/index";
					}
				}							
			}
			else if(request.getAttribute("record") instanceof Record)
			{
				if(request.getAttribute("service") instanceof Service)  //workflow
				{
					if(path.endsWith("/edit"))
					{
						secondPath = "process/edit";
					} 
					else if(path.endsWith("/new"))
					{
						secondPath = "process/new";
					}
				}
				else if(path.endsWith("/edit") || path.endsWith("/new"))
				{
					secondPath = "model/edit";
				}
			}
			
			if(secondPath != null && !secondPath.equals(path))
			{
				//again?
				try
				{
					return cfg.getTemplate(secondPath+".html");
				}
				catch (IOException e1)
				{
				}
			}
			
			return null;
		}
	}
}

class FreemarkerTemplateLoader implements TemplateLoader
{
	private String viewsPath;
	//private String appName;
	private static String ServeletPath = "/WEB-INF/views/";
	
	public FreemarkerTemplateLoader(String appPath)
	{
		//this.appName = appName;
		this.viewsPath = appPath +"/views/";
	}
	
	@Override
	public void closeTemplateSource(Object arg0) throws IOException 
	{
		// Do nothing.
	}

	@Override
	public Object findTemplateSource(String name) throws IOException 
	{
		/*String path = name;
		if(name.startsWith(appName))
		{
			path = path.substring(appName.length());
		}*/
		String path = viewsPath + name;
		try 
		{
			File file = new File(path);
            if(!file.isFile())
            {
            	//search from iap
            	path = TernContext.getServletContext().getRealPath(ServeletPath+name);
            	if(path!=null)
            	{
            		file = new File(path);
            		if(!file.isFile())
            		{
            			return null;
            		}
            	}
            	else
            	{
                    return null;
            	}
            }
            
            if(file.canRead()) 
            {                    
                return file;
            }
	     }
		 catch (SecurityException e) 
	     {
	        ;// ignore
	     }
		 return null;
	}

	@Override
	public long getLastModified(Object templateSource) 
	{
		return ((File) templateSource).lastModified();
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException 
	{
		return new InputStreamReader(
                new FileInputStream((File) templateSource),
                encoding);
	}
}
