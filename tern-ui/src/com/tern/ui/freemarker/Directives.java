/**
 * Tern-ui Library.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.ui.freemarker;

import java.util.Map;

import com.tern.util.Convert;

import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

public class Directives implements TemplateHashModel
{
	@SuppressWarnings("serial")
	static Map<String,TemplateModel> names=new java.util.HashMap<String, TemplateModel>(){{
		//this.put("relation", new com.tern.ui.freemarker.directive.Relation());
		this.put("block", new com.tern.ui.freemarker.directive.Block());
		this.put("override", new com.tern.ui.freemarker.directive.Overrides());
		this.put("query", new com.tern.ui.freemarker.directive.Query());
		this.put("search", new com.tern.ui.freemarker.directive.Search());
		this.put("form", new com.tern.ui.freemarker.directive.Form());
		this.put("field", new com.tern.ui.freemarker.directive.Field());
	}};
	
	@Override
	public TemplateModel get(String key) throws TemplateModelException 
	{
		return names.get(key);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException 
	{
		return false;
	}
	
	public static String getStringParam(final Environment env,Map params,String key)  throws TemplateException
	{
		return getStringParam(env,params,key,true,null);
	}
	
	public static String getStringParam(final Environment env,Map params,String key,String def)  throws TemplateException
	{
		return getStringParam(env,params,key,true,def);
	}
	
	public static String getStringParam(final Environment env,Map params,String key,boolean required,String def) throws TemplateException
	{
		final TemplateModel tv = (TemplateModel)params.get(key);
		if(tv == null) 
		{
			if(required && def == null)
			{
                throw new TemplateException(String.format("Missing required parameter '%s'",key), env);
			}
			else
			{
				return def;
			}
        }
		
		if(!(tv instanceof TemplateScalarModel))
		{
            throw new TemplateException(
            		String.format("Expected a scalar model. '%s' is instead %s", key,tv.getClass().getName()) 
                    , env);
        }
		
		String ret = ((TemplateScalarModel)tv).getAsString();
		if(ret == null)
		{
			ret = def;
			if(ret == null && required)
			{
			    throw new TemplateException(String.format("String value of '%s' parameter is null",key), env);
			}
		}
		
		return ret;
	}
	
	public static int getIntParam(final Environment env,Map params,String key,int def) throws TemplateException
	{
		final TemplateModel tv = (TemplateModel)params.get(key);
		if(tv == null) 
		{
			return def;
		}
		
		if(!(tv instanceof TemplateNumberModel))
		{
            throw new TemplateException(
            		String.format("Expected a number model. '%s' is instead %s", key,tv.getClass().getName()) 
                    , env);
        }
		
		return ((TemplateNumberModel)tv).getAsNumber().intValue();
	}
	
	public static boolean getBoolParam(final Environment env,Map params,String key,boolean def) throws TemplateException
	{
		final TemplateModel tv = (TemplateModel)params.get(key);
		if(tv == null) 
		{
			return def;
		}
		
		if(tv instanceof TemplateBooleanModel)
		{
			return ((TemplateBooleanModel)tv).getAsBoolean();
            //throw new TemplateException(
            //		String.format("Expected a boolean model. '%s' is instead %s", key,tv.getClass().getName()) 
            //        , env);
        }
		
		return Convert.parseBool(tv.toString());
	}

}
