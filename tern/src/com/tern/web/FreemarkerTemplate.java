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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tern.dao.ModelException;
import com.tern.util.TernContext;
import com.tern.util.Trace;
import com.tern.util.config;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

public class FreemarkerTemplate extends Template
{
	protected Configuration cfg;
	static ObjectWrapper wrapper;
	
	//static TaglibFactory jspTaglibs;	
	//static Object lockObj = new Object();
	
	public FreemarkerTemplate()
	{
		cfg = new Configuration();
		try 
		{
			//File cfgPath = new File(config.getConfigurationPath());
			//cfg.setDirectoryForTemplateLoading( new File(cfgPath.getParent()) );
			cfg.setServletContextForTemplateLoading(TernContext.getServletContext(),"/WEB-INF/views");
			//cfg.setTemplateLoader(loader)
			
			//cfg.setLocale(Locale.CHINA);
			cfg.setSetting("date_format", "yyyy-MM-dd");
			cfg.setSetting("time_format", "HH:mm:ss");
			cfg.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss");
		}
		catch (Exception e) 
		{
			cfg = null;
			Trace.write(Trace.Error, e,"init freemarker failed!");
			return;
		}
		
		cfg.setDefaultEncoding(config.getEncoding());
		cfg.setOutputEncoding(config.getEncoding());
		
		wrapper = new ActionDataWrapper();//ObjectWrapper.BEANS_WRAPPER;//.DEFAULT_WRAPPER;
		/*wrapper = new DefaultObjectWrapper(){
			 public TemplateModel wrap(Object obj) throws TemplateModelException {
				 if(obj instanceof com.tern.dao.RecordSet){
					 return new freemarker.template.SimpleCollection(((com.tern.dao.RecordSet)obj).iterator(), this);
				 }
				 return super.wrap(obj);
			 }
		};*/
		cfg.setObjectWrapper(wrapper);
		
		
		if(config.isDebug())
		{
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setTemplateUpdateDelay(0);
		}
		else
		{
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			cfg.setTemplateUpdateDelay(config.getInt("template.delay", 3600));
		}
	}
	
	protected freemarker.template.Template getTemplate(String path,HttpServletRequest request)
	{
		try 
		{			
			return cfg.getTemplate(path+".html");						
		}
		catch (Exception e)
		{
			Trace.write(Trace.Error, e,"load template failed.");
			return null;
		}
	}	
	
	public boolean render(Controller ctl,String path,HttpServletRequest request,HttpServletResponse response)
	{
		//if(vars == null)
		//{
		//	vars = new java.util.HashMap<String, Object>();
		//}
		
		/*vars.put("request", new HttpRequestHashModel(request,cfg.getObjectWrapper()));
		vars.put("session",new HttpSessionHashModel(request.getSession(),cfg.getObjectWrapper()));
		vars.put("params", new HttpRequestParametersHashModel(request));
		vars.put("$home", config.getRoot());
		vars.put("$url", request.getRequestURI());*/
		
		freemarker.template.Template temp = getTemplate(path,request);
		if(temp != null)
		{
			Object attrContentType = temp.getCustomAttribute("content_type");
	        if(attrContentType != null) 
	        {
	            response.setContentType(attrContentType.toString());
	        }
	        else
	        {
	        	response.setContentType("text/html; charset=" + temp.getEncoding());
	        }
			
			try
			{
				temp.process( new ActionDataModel(request), response.getWriter() );
				return true;
			} 
			catch (Exception e)
			{
				Trace.write(Trace.Error, e,"parse template failed.");
			}
		}
		else
		{
			Trace.write(Trace.Error, "template[%s] does not exists." , path);
		}
		
		return false;
	}
	
	/*public static TaglibFactory getJspTaglibs()
	{
		if(jspTaglibs == null)
		{
			synchronized(lockObj)
			{
				if(jspTaglibs == null) jspTaglibs = new TaglibFactory(Template.context);
			}
		}
		return jspTaglibs;
	}*/
}

interface ICreateModel
{
	TemplateModel get(ActionDataModel m);
}

interface IWrapData
{
	TemplateModel get(Object obj);
}

class ActionDataWrapper extends DefaultObjectWrapper
{	
    public TemplateModel wrap(Object obj) throws TemplateModelException 
	{
    	if(obj instanceof com.tern.dao.ITable)
    	{
    		return new ActionSequence((com.tern.dao.ITable)obj , this);
    	}
    	else if(obj instanceof com.tern.dao.IRow)
    	{
    		return new ActionHash((com.tern.dao.IRow)obj);
    	}
		 
		return super.wrap(obj);    	
	}
}

class ActionSequence extends BeanModel implements TemplateSequenceModel,
            TemplateScalarModel,AdapterTemplateModel,TemplateHashModel
{
	private com.tern.dao.ITable table;
	
	public ActionSequence(com.tern.dao.ITable table , BeansWrapper wrapper)
	{
		super(table,wrapper);
		this.table = table;
	}
	
	@Override
	public TemplateModel get(int index) throws TemplateModelException 
	{
		return new ActionHash(table.get(index));
	}

	@Override
	public int size() //throws TemplateModelException 
	{
		try
		{
		    return table.size();
		}
		catch(Exception e)
		{
			//throw new TemplateModelException(e);
			Trace.write(Trace.Error, e, "ActionSequence");
			return 0;
		}
	}

	@Override
	public String getAsString() throws TemplateModelException 
	{		
		try
		{
		    return table.toString();
		}
		catch(Exception e)
		{
			throw new TemplateModelException(e);
		}
	}

	@Override
	public Object getAdaptedObject(Class arg0) 
	{
		return table;
	}

	@Override
	public TemplateModel get(String arg0) throws TemplateModelException 
	{
		try
		{
			TemplateModel v = super.get(arg0);
		    return v;
		}
		catch(TemplateModelException e1)
		{
			throw e1;
		}
	}

	@Override
	public boolean isEmpty() //throws TemplateModelException 
	{
		return false;
	}
}

@SuppressWarnings("deprecation")
class ActionHash implements TemplateHashModel,TemplateScalarModel,
    AdapterTemplateModel, WrapperTemplateModel
{
	private com.tern.dao.IRow row;
	
	public ActionHash(com.tern.dao.IRow row)
	{
		this.row = row;
	}

	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		Object obj = null;
		
		/*try
		{
		    obj = row.get(key);
		}
		catch(Throwable t)
		{			
		}
		
		if(null == obj)
		{
			
		}*/
		
		try
		{
		    obj = row.get(key);
		}
		catch(ModelException e)
		{
			if(key.equals("state") && (row instanceof com.tern.dao.Record) )
			{
				obj = ((com.tern.dao.Record)row).getState();
			}
			else throw new TemplateModelException(e);
		}
		return FreemarkerTemplate.wrapper.wrap(obj);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException 
	{
		return false;
	}
	
	public Object getAdaptedObject(Class hint) 
	{
		return row;
	}

	public Object getWrappedObject() 
	{
		return row;
	}

	@Override
	public String getAsString() throws TemplateModelException 
	{
		try
		{
		    return row.toString();
		}
		catch(Exception e)
		{
			throw new TemplateModelException(e);
		}
	}
}

class ActionDataModel implements TemplateHashModel
{
    final HttpServletRequest request;
    
    SessionModel sessionModel;
    ParametersModel paramsModel;
    static ApplicationModel appModel = new ApplicationModel();
    
    @SuppressWarnings("serial")
	private final static Map<String,ICreateModel> internals=new HashMap<String,ICreateModel>(){{
    	//for session
		this.put("session", new ICreateModel(){
    		public TemplateModel get(ActionDataModel m)
    		{
    			if(m.sessionModel == null)
    			{
    				m.sessionModel = new SessionModel(m.request.getSession(false));
    			}
    			
    			return m.sessionModel;
    		}
    	});
    	
		//params
    	this.put("params", new ICreateModel(){
    		public TemplateModel get(ActionDataModel m)
    		{
    			if(m.paramsModel == null)
    			{
    				m.paramsModel = new ParametersModel(m.request);
    			}
    			
    			return m.paramsModel;
    		}
    	});
    	
    	//application
    	this.put("app", new ICreateModel(){
    		public TemplateModel get(ActionDataModel m)
    		{		
    			return ActionDataModel.appModel;
    		}
    	});
    	
    	//tern jsp taglib
    	/*this.put("tern", new ICreateModel(){
    		private TemplateModel ternModel;
    		public TemplateModel get(ActionDataModel m)
    		{
    			if(null == ternModel)
    			{
    				synchronized(FreemarkerTemplate.lockObj)
    				{
    					if(null == ternModel)
    					{
    						try
    						{
    							ternModel = FreemarkerTemplate.getJspTaglibs().get("/WEB-INF/tern/tern-tags.tld");
    						}
    						catch (TemplateModelException e)
    						{
    							Trace.write(Trace.Error, e, "loading tern jsp taglib");
    						}
    					}
    				}			
    			}
    			return ternModel;
    		}
    	});
    	
    	//jsp tag libs
    	this.put("JspTaglibs", new ICreateModel(){
    		public TemplateModel get(ActionDataModel m) 
			{
    			return FreemarkerTemplate.getJspTaglibs();
			}    		
    	});*/
    	
    	//HOME variable
    	this.put("HOME", new ICreateModel(){
			public TemplateModel get(ActionDataModel m)
			{
				return new SimpleScalar(TernContext.current().getContextPath());
			}    		
    	});
    	
    	//URL variable
    	this.put("URL", new ICreateModel(){
			public TemplateModel get(ActionDataModel m) 
			{
				return new SimpleScalar( m.request.getRequestURI() );
			}    		
    	});
    	
    	try
    	{
    		final Object obj = Class.forName("com.tern.ui.freemarker.Directives").newInstance();
    		if(obj instanceof TemplateHashModel)
    		{
    			this.put("tern", new ICreateModel(){
    				public TemplateModel get(ActionDataModel m) 
    				{
    					return (TemplateHashModel)obj;
    				}    		
    	    	});
    		}
    	}
    	catch(Exception e)
    	{}
    	
    }};
    
	public ActionDataModel(HttpServletRequest request) 
	{
        this.request = request;
    }
	
	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		try
    	{
			// Lookup in request scope
	        Object obj = request.getAttribute(key);
	        if(obj != null) 
	        {
	            return FreemarkerTemplate.wrapper.wrap(obj);
	        }
	        
	        //session,params,app,tern(the jsp taglib) ... 
	        ICreateModel im = internals.get(key);
	        if(im != null)
	        {
	        	return im.get(this);
	        }              
    	}
    	catch(TemplateModelException tme)
    	{
    		throw tme;
    	}
		catch(Exception e)
		{
			throw new TemplateModelException(e);
		}
    	catch(Throwable t)
    	{
    		throw new TemplateModelException(new Exception(t));
    	}
		        
        return FreemarkerTemplate.wrapper.wrap(null);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException
	{		
		return false;
	}
}

class SessionModel implements TemplateHashModel
{
	private HttpSession session;
	SessionModel(HttpSession session)
	{
		this.session = session;
	}

	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		return FreemarkerTemplate.wrapper.wrap(session != null ? session.getAttribute(key) : null);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException 
	{
		return session == null || !session.getAttributeNames().hasMoreElements();
	}
}

class ParametersModel implements TemplateHashModel,AdapterTemplateModel
{
	private HttpServletRequest request;
	ParametersModel(HttpServletRequest request)
	{
		this.request = request;
	}

	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		return FreemarkerTemplate.wrapper.wrap(request != null ? request.getParameter(key) : null);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException 
	{
		return false;
	}
	
	public String toString()
	{
		return request.getQueryString();
	}

	@Override
	public Object getAdaptedObject(Class arg0) 
	{
		return request;
	}
}

class ApplicationModel implements TemplateHashModel
{
	ApplicationModel()
	{
	}

	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		return FreemarkerTemplate.wrapper.wrap(TernContext.getServletContext().getAttribute(key));
	}

	@Override
	public boolean isEmpty() throws TemplateModelException 
	{
		return TernContext.getServletContext().getAttributeNames().hasMoreElements();
	}
	
}

