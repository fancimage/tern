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

import com.tern.dao.Column;
import com.tern.dao.DataType;
import com.tern.dao.NamedValue;
import com.tern.dao.Record;
import com.tern.ui.freemarker.Directives;
import com.tern.util.Convert;

import freemarker.core.Environment;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class Field implements TemplateDirectiveModel
{
	@SuppressWarnings("serial")
	private static java.util.Set<String> PARAMS = new java.util.HashSet<String>(){{
		this.add("name");
		this.add("data");
		this.add("nullName");
		this.add("readonly");
		this.add("columnName");
	}};
	
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException 
	{
		TemplateModel tv = env.getCurrentNamespace().get(Form.FORM_RECORD_NAME);
		Object o = null;
		if(tv instanceof AdapterTemplateModel)
		{
			o = ((AdapterTemplateModel)tv).getAdaptedObject(null);
		}
		
		FieldRender render = null;
		if(o != null && (o instanceof FieldRender))
		{
			//in form
			render = (FieldRender)o;			
		}
		else
		{
			//get record
			Record row = null;
			tv = (TemplateModel)params.get("data");
			if(tv == null)
			{
				tv = env.getVariable("record");
			}
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
			
			render = new FieldRender(env,row);
		}
		
		String name = Directives.getStringParam(env, params, "name",true,null);
		String columnName = Directives.getStringParam(env, params, "columnName",false,null);
		if(columnName==null || columnName.length()<=0)
		{
			//from name
			columnName = name;
		}
		
		com.tern.dao.Column col = render.r.getModel().column(columnName);
		
		//params
		StringBuffer attrs=new StringBuffer();
		for(Object k:params.keySet())
		{
			if(PARAMS.contains(k))
			{
				continue;
			}
			
			String v = Directives.getStringParam(env, params, k.toString(),false,null);
			if(v != null && v.length()>0)
			{
				attrs.append(' ').append(k.toString()).append("=\"")
				     .append(v).append("\"");
			}
		}
		
		render.env=env;
		render.params = params;
		
		render.render(col, 
				Directives.getBoolParam(env, params,"readonly",false), 
				attrs.length()<=0?null:attrs.toString());
	}
}

class FieldRender
{
	protected Writer out;
	protected Record r;
	protected Map params;
	protected Environment env;
	
	protected int style;    //1-has date 2-has time 4-has enum
	
	public static final int STYLE_DATE = 1;
	public static final int STYLE_TIME = 2;
	public static final int STYLE_ENUM = 4;
	public static final int STYLE_HAVING = 8;
	public static final int STYLE_CONDTIONS = 16;
	
	public FieldRender(Environment env,Record r)
	{
		this.env = env;
		this.out = env.getOut();
		this.r = r;
	}
	
	public void setParams(Map p){params=p;}
	
	/*public void render(Column col,boolean readonly) throws IOException
	{
		render(col,readonly,null);
	}*/
	
	public void render(Column col,boolean readonly,String attrs) throws IOException,TemplateException
	{		
		if(col.getShowCondtion() != null)
		{
			out.append("<input type=\"hidden\" class=\"showcondi\" value=\"")
			   .append(col.getShowCondtion())
			   .append("\">");
			
			this.style |= STYLE_CONDTIONS;
		}
		
		if(readonly)
		{
			String v = null;
			if(col.getBelongsTo() != null)
			{
				v = r.getString(col.getBelongsTo().getName());
			}
			else
			{
				v = r.getString(col.getName());
			}
			
			out.append("<span class=\"help-inline\">").append(v==null?"":v).append("</span>");
		    return;
		}
		
		if(col.getBelongsTo() != null)
		{
			//relation
			render_relation(col,attrs);
			return;
		}
		
		DataType type = col.getType();
		if(type == DataType.Enum)
		{
			/*判断枚举值的个数，如果小于4则直接按单选框展示?*/
			NamedValue v = (NamedValue)r.get(col.getName());
			out.append("<div class=\"input-append\"><input type=\"text\" value=\"")
			   .append(v==null?"":v.getName()).append("\"><input type=\"hidden\" value=\"")
			   .append(v==null?"":v.getValue()).append("\" name=\"").append(col.getName())
			   .append("\" data-type=\"").append(col.getEnum())
			   .append("\"><span class=\"add-on enumbtn\"><i class=\"icon-folder-open\"></i></span></div>");
			
			style |= STYLE_ENUM;
		}
		else if(type == DataType.Bool)
		{
			boolean v = r.getBoolean(col.getName());
			
			out.append("<div class=\"boolean\"><span><input type=\"radio\" name=\"").append(col.getName());
			out.append("\" value=\"true\"");
			if(v) out.append("checked");
			out.append(">是</span>");
			
			out.append("<span><input type=\"radio\" name=\"").append(col.getName());
			out.append("\" value=\"false\"");
			if(!v) out.append("checked");
			out.append(">否</span></div>");
		}
		else if(type == DataType.Datetime)
		{
			render_date(col,attrs);
			return;
		}
		else if(type == DataType.Text)
		{
			//textarea
		}
		else if(type == DataType.Having)
		{
			//child table
			com.tern.dao.Relation rel = r.getModel().relation(col);
			if(rel != null)
			{
			    out.append("<label><a href=# data-ref=\"").append(rel.getName())
			       .append("\" data-pid=\"").append(String.valueOf(r.getId()));
			    
			    if(rel.getMode() == com.tern.dao.Relation.HAVE_ONE)
			    {
			    	out.append("\" data-mode=\"one");
			    }
			    
			    out.append("\" class=\"refbtn\"><i class=\"icon-edit\"></i></a></label>");
			    
			    style |= STYLE_HAVING;
			}
		}
		else  //default
		{		
			String val = r.getString(col.getName());
			if(val == null) val="";
			
			out.append("<input type=\"text\" name=\"").append(col.getName());
			out.append("\" placeholder=\"").append(col.getCaption());
			out.append("\" value=\"").append(val);
			out.append("\"");
			
			if(attrs != null) out.append(' ').append(attrs);
			out.append(">");
		}
	}
	
	protected void render_date(Column col,String attrs) throws IOException,TemplateException
	{//icon-calendar, icon-time
		int m = col.getDateMode();
		String val = r.getString(col.getName());
		if(val == null) val="";
		String formater = col.getFormat().toPattern();
		if(1==m)  //date
		{
			/*out.append("<div class=\"input-append date\" data-date=\"").append(val).append("\"")
			   .append(" data-date-format=\"").append(Convert.replaceAll(formater, "MM", "mm"))
			   .append("\">\n<input name=\"").append(col.getName()).append("\" type=\"text\" value=\"")
			   .append(val).append("\" readonly>\n<span class=\"add-on\"><i class=\"icon-calendar\"></i></span>\n")
			   .append("</div>\n");*/
			
			out.append("<div class=\"input-append date\">\n<input name=\"").append(col.getName()).append("\" data-format=\"").append(formater)
			   .append("\" type=\"text\" value=\"")
			   .append(val).append("\">\n<span class=\"add-on\"><i data-time-icon=\"icon-time\" data-date-icon=\"icon-calendar\"></i></span>\n")
			   .append("</div>\n");
			
			style |= STYLE_DATE;
		}
		else if(2 == m)  //time
		{
			formater = Convert.replaceAll(formater, "HH", "hh");
			/*out.append("<div class=\"bootstrap-timepicker\">\n<input name=\"")
			   .append(col.getName()).append("\" type=\"text\" class=\"input-small\">\n")
			   .append("<i class=\"icon-time\"></i>\n</div>");*/
			
			out.append("<div class=\"input-append time\">\n<input name=\"").append(col.getName()).append("\" data-format=\"").append(formater)
			   .append("\" type=\"text\" value=\"")
			   .append(val).append("\">\n<span class=\"add-on\"><i data-time-icon=\"icon-time\" data-date-icon=\"icon-calendar\"></i></span>\n")
			   .append("</div>\n");
			
			style |= STYLE_TIME;
		}
		else   //date & time
		{
			style |= STYLE_DATE;
			style |= STYLE_TIME;
			formater = Convert.replaceAll(formater, "HH", "hh");
			
			/*out.append("<div class=\"input-append date\" data-date=\"").append(val).append("\"")
			   .append(" data-date-format=\"").append(Convert.replaceAll(formater, "MM", "mm"))
			   .append("\">\n<input name=\"").append(col.getName()).append("\" type=\"text\" value=\"")
			   .append(val).append("\" readonly>\n<span class=\"add-on\"><i class=\"icon-calendar\"></i></span>\n")
			   .append("</div>\n");
			
			out.append("<div class=\"bootstrap-timepicker\">\n<input name=\"")
			   .append(col.getName()).append("\" type=\"text\" class=\"input-small\">\n")
			   .append("<i class=\"icon-time\"></i>\n</div>");*/
			out.append("<div class=\"input-append datetime\">\n<input name=\"").append(col.getName()).append("\" data-format=\"").append(formater)
			   .append("\" type=\"text\" value=\"")
			   .append(val).append("\">\n<span class=\"add-on\"><i data-time-icon=\"icon-time\" data-date-icon=\"icon-calendar\"></i></span>\n")
			   .append("</div>\n");
		}
	}
	
	static final String defaultNullName="请选择...";
	protected void render_relation(Column col,String attrs) throws IOException,TemplateException
	{
		com.tern.dao.Relation relation = col.getBelongsTo();
		com.tern.dao.RecordSet rs = relation.queryAvailableParents(col.getName() , r);
		
		out.append("<select name=\"").append(col.getName()).append("\"");
		if(attrs != null) out.append(' ').append(attrs);
		out.append(">\n");
		
		//options
		String key = relation.maped(col.getName());
		Object value = r.get(col.getName());
				
		boolean isFound = false;
		for(com.tern.dao.Record pr:rs)
		{
			out.append("<option value=\"");	
					
			Object v = pr.get(key);
			out.append(Convert.toString(v)).append("\"");
					
			if(v == value || (v!=null && v.equals(value)) )
			{
				out.append(" selected=\"selected\"");
				isFound = true;
			}
					
			out.append(">").append(pr.toString()).append("</option>");			
		}
				
		out.append("<option value=\"\"");
		if(value==null || value.toString().length()==0 || !isFound)
		{
			out.append(" selected=\"selected\" ");
		}
				
		String nullName = Directives.getStringParam(env, params, "nullName",false,defaultNullName);
		out.append(">").append(nullName)
		   .append("</option>")
		   .append("\n</select>\n");
	}
}

class VLayoutRender extends FieldRender
{
	public VLayoutRender(Environment env,Record r)
	{
		super(env,r);
	}
	
	public void render(Column col,boolean readonly,String attrs) throws IOException,TemplateException
	{
		out.append("<label>").append(col.getCaption()).append("</label>\n");
		super.render(col, readonly,attrs);
	}
}

class HLayoutRender extends FieldRender
{
	public HLayoutRender(Environment env,Record r)
	{
		super(env,r);
	}
	
	public void render(Column col,boolean readonly,String attrs) throws IOException,TemplateException
	{
		out.append("<div class=\"control-group\">\n");
		out.append("<label class=\"control-label\">").append(col.getCaption()).append("</label>\n");
		
		out.append("<div class=\"controls\">\n");
		
		super.render(col, readonly,attrs);
		
		out.append("</div>\n");
		out.append("</div>\n");
	}
}
