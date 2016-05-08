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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.tern.dao.Column;
import com.tern.dao.DataType;
import com.tern.dao.Model;
import com.tern.dao.RecordSet;
import com.tern.db.DataRow;
import com.tern.db.db;
import com.tern.ui.freemarker.Directives;
import com.tern.util.Convert;
import com.tern.util.Trace;

import freemarker.core.Environment;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

public class Search implements TemplateDirectiveModel
{
	@SuppressWarnings("serial")
	private static java.util.Set<String> PARAMS = new java.util.HashSet<String>(){{
		this.add("name");
		this.add("caption");
		this.add("default");
		this.add("options");
		this.add("operator");
		this.add("columnName");
	}};
	
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException 
	{			
		TemplateModel tv = env.getCurrentNamespace().get(Query.QUERY_NAME);
		Object o = null;
		if(tv instanceof AdapterTemplateModel)
		{
			o = ((AdapterTemplateModel)tv).getAdaptedObject(null);
		}
		
		if(o == null || !(o instanceof Query.QueryInfo))
		{
			return;
		}
		
		Query.QueryInfo info = (Query.QueryInfo)o;
		if(info.records == null) return;
		
		Model model = info.records.getModel();
		
		String name = Directives.getStringParam(env, params, "name");
		String colname = Directives.getStringParam(env, params, "columnName",false,null);
		if(colname==null) colname = name;
		Column col = model.column(colname);
		
		String caption = Directives.getStringParam(env, params, "caption",false,null);
		if(caption==null)
		{
			caption = col.getCaption();
		}
		
		SearchUI ui = new SearchUI();
		ui.col = col;
		ui.request=info.request;
		ui.match_mode = Directives.getStringParam(env, params, "operator",false,null);
		
		//has options?
		tv = (TemplateModel)params.get("options");
		if(tv != null && (tv instanceof TemplateSequenceModel) )
		{
			ui.options = (TemplateSequenceModel)tv;
		}
		
		ui.pname = info.name+"_"+name;
		ui.value = info.request.getParameter(ui.pname);
		String defval = Directives.getStringParam(env, params, "default",false,null);
		if(ui.value == null)
		{
			//has default value
			ui.value = defval;
		}
		
		Writer writer = env.getOut();
		writer.append("<div class=\"form-group\"><label>").append(caption).append("</label>");
		writer.append( ui.getUIHtml(params) );
		writer.append("</div>");
		
		String condition = ui.getCondition();
		if(condition!=null)
		{
			if(info.condtions == null) info.condtions = new HashMap<String,String>();
			info.condtions.put(name, condition);
		}
	}
	
	static class SearchUI
	{
		public String pname;
		public String value;
		public String defval;
		public TemplateSequenceModel options;
		public Column col;
		public HttpServletRequest request;
		public String match_mode;
		
		private String condition = null;
		
		public String getUIHtml(Map params) throws TemplateModelException
		{
			if(options!=null)
			{
				return getOptionsUI(params);
			}					
			else if(col.getType() == DataType.Enum) //enum?
			{
				return createEnumUI(col.getEnum(),Convert.parseInt(value,-1));
			}
			
			StringBuffer buf = new StringBuffer();
			if(col.getBelongsTo() != null) //foreign key?
			{
				Map<String,Object> filter = null;
				com.tern.dao.Relation relation = col.getBelongsTo();
				if(relation.getMap().length>=2)
				{
					filter = new java.util.HashMap<String, Object>();
					for(String[] arr:relation.getMap())
					{
						if(arr[0].equals(col.getName())) continue;
						Object _val = request.getParameter(arr[0]);
						if(_val == null) _val = request.getAttribute(arr[0]);
						
						if(_val != null)
						{
							filter.put(arr[0], _val);
						}
					}
				    
					if(filter.size() <= 0) filter = null;
				}
				
				buf.append("<select name=\"").append(pname).append("\" id=\"");
    			buf.append(pname).append("\"");
    			
    			processParams(buf,params);    			
    			
    			buf.append(" class=\"form-control\">");
				
				RecordSet rs = relation.queryAvailableParents(col.getName() , filter);
				boolean isFound = false;
				String key = relation.maped(col.getName());
				
				for(com.tern.dao.Record pr:rs)
				{
					buf.append("<option value=\"");	
					
					Object _val = pr.get(key);
					buf.append(_val).append("\"");
					
					if(_val!=null && _val.toString().equals(value))
					{
						buf.append(" selected=\"selected\"");
						isFound = true;
					}
					
					buf.append(">").append(pr).append("</option>");
				}
				
				buf.append("<option value=\"\"");
				if(value==null || value.length()==0 || !isFound)
					buf.append(" selected=\"selected\" ");
				buf.append(">");
				buf.append(Relation.defaultNullName);
				buf.append("</option>");
				
				buf.append("</select>");
			}
			else
			{
				buf.append("<input type=\"text\" name=\"").append(pname).append("\" id=\"");
    			buf.append(pname).append("\"");
    			processParams(buf,params);
    			    			
    			if(value != null && value.length()>0)
    			{
    				buf.append(" value=\"").append(value).append("\"");
    			}
    			buf.append(" class=\"form-control\"/>");
			}
			
			return buf.toString();
		}
		
		public String getCondition()
		{
			if(condition == null)
			{
				if(value == null || value.length()<=0) return null;
				else if(options!=null) return null;
								
    			String mode = match_mode;
    			if(mode == null)
    			{
    				if(col.getType() == DataType.String)
    				{
    					mode = "like";
    				}
    				else
    				{
    					mode = "=";
    				}
    			}
    			
    			String val = null;
    			if(mode.equalsIgnoreCase("like"))
    			{
    				mode = " LIKE ";
    				val = "%"+value+"%";
    			}
    			else
    			{
    				val = value;
    			}
    			
    			return col.getName() + mode + Model.sqlvalue(col, val);
			}
			
			return condition;
		}
		
		private void processParams(StringBuffer buf,Map params)
		{
			for(Object k:params.keySet())
			{
				if(!PARAMS.contains(k))
				{
					buf.append(' ').append(k).append("=\"").append(params.get(k)).append("\"");
				}
			}
		}
		
		private String createEnumUI(String enumType,int value)
    	{    		
			StringBuffer buf = new StringBuffer();
    		try
    		{
    			for(DataRow rs:db.table("tn_enums").select("eid,ecode,ecaption").where("etype=?",enumType))
    			{
    				int v = rs.getInt(0);
    				buf.append("<option value=\"");
    				buf.append(v);
    				buf.append("\"");
    				
    				if(value == v)
    				{
    					buf.append("selected=\"selected\"");
    				}
    				buf.append(">");
    				
    				buf.append(rs.getString(2));
    				buf.append("</option> ");
    			}
    			
    			buf.append("</select> ");
    		}
    		catch(Exception e)
    		{
    			Trace.write(Trace.Error,e, "SearchBar-Enum:");
    			if(buf.length() > 0) buf.delete(0, buf.length());
    		}    		  	
    		
    		return buf.toString();
    	}
		
		private String getOptionsUI(Map params) throws TemplateModelException
		{
			StringBuffer buf = new StringBuffer();
			buf.append("<select name=\"").append(pname).append("\" id=\"");
			buf.append(pname).append("\"");
			
			processParams(buf,params);
			buf.append(" class=\"form-control\">");
			
			int sel = Convert.parseInt(value,-1);
			boolean flag = false;			
			for(int index = 0;index<options.size();index++)
			{
				TemplateModel tv = options.get(index);
				TemplateSequenceModel option = null;
				if(tv instanceof TemplateSequenceModel)
				{
					option = (TemplateSequenceModel)tv;
				}
				if(option==null || option.size()<2) continue;
				
				buf.append("<option value=\"").append(index).append("\"");
				if(sel == index)
				{
					buf.append(" selected");
					flag = true;
					condition = option.get(1).toString();
				}
				buf.append(">").append(option.get(0)).append("</option>");
			}
			
			if(defval == null)
			{
				buf.append("<option value=\"-1\"");
				if(!flag)
				{
					buf.append(" selected");
				}
				
				buf.append(">请选择").append("</option>");
			}
			
			buf.append("</select>");
			return buf.toString();
		}
	}
}
