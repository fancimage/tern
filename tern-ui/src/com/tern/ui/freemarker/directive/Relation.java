/**
 * Tern-ui Library.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.ui.freemarker.directive;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.tern.dao.Record;
import com.tern.ui.freemarker.Directives;
import com.tern.util.Convert;

import freemarker.core.Environment;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class Relation implements TemplateDirectiveModel
{
	static final String defaultNullName="请选择...";
	
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException
	{
		Record row = null;
		TemplateModel tv = (TemplateModel)params.get("data");
		if(tv != null)
		{
			if(tv instanceof AdapterTemplateModel)
			{
				Object obj = ((AdapterTemplateModel)tv).getAdaptedObject(null);
				if(obj instanceof Record)
				{
					row = (Record)obj;
				}
			}
		}
		
		if(row == null)
		{
			throw new TemplateException("Missing parameter 'data' or type mis-match.", env);
		}
		
		String name = Directives.getStringParam(env, params, "name",true,null);
		String columnName = Directives.getStringParam(env, params, "columnName",false,null);
		if(columnName==null || columnName.length()<=0)
		{
			//from name
			columnName = name;
		}
		
		com.tern.dao.Column col = row.getModel().column(columnName);
		com.tern.dao.Relation relation = col.getBelongsTo();
		if(relation == null)
		{
			throw new TemplateException(String.format("can not find belongs-to relation: %s", columnName), env);
		}
		
		String nullName = Directives.getStringParam(env, params, "nullName",false,defaultNullName);
		
		com.tern.dao.RecordSet rs = relation.queryAvailableParents(col.getName() , row);		
		final Writer envOut = env.getOut();
		envOut.append("<select");
		
		//params
		for(Object k:params.keySet())
		{
			if("data".equals(k) || "columnName".equals(k) || "nullName".equals(k))
			{
				continue;
			}
			
			String v = Directives.getStringParam(env, params, k.toString(),false,null);
			if(v != null && v.length()>0)
			{
				envOut.append(' ').append(k.toString()).append("=\"")
				      .append(v).append("\"");
			}
		}
		envOut.append(">\n");
		
		//options
		String key = relation.maped(col.getName());
		Object value = row.get(col.getName());
		
		boolean isFound = false;
		for(com.tern.dao.Record pr:rs)
		{
			envOut.append("<option value=\"");	
			
			Object v = pr.get(key);
			envOut.append(Convert.toString(v)).append("\"");
			
			if(v == value || (v!=null && v.equals(value)) )
			{
				envOut.append(" selected=\"selected\"");
				isFound = true;
			}
			
			envOut.append(">").append(pr.toString()).append("</option>");			
		}
		
		envOut.append("<option value=\"\"");
		if(value==null || value.toString().length()==0 || !isFound)
		{
			envOut.append(" selected=\"selected\" ");
		}
		
		envOut.append(">").append(nullName)		
		      .append("</option>")				
		      .append("\n</select>");
	}

}
