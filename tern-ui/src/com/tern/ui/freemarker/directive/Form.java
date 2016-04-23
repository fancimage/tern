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
import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.dao.RecordState;
import com.tern.ui.freemarker.Directives;
import com.tern.util.Convert;

import freemarker.core.Environment;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

public class Form implements TemplateDirectiveModel
{
	static final String FORM_RECORD_NAME = ".tern.record";
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException 
	{
		//record,fields,readonly?
		Record record = null;
		TemplateModel tv = (TemplateModel)params.get("record");
		if(tv != null)
		{
			if(tv instanceof AdapterTemplateModel)
			{
				Object obj = ((AdapterTemplateModel)tv).getAdaptedObject(null);
				if(obj instanceof Record)
				{
					record = (Record)obj;
				}
			}
		}
		
		if(record == null)
		{
			throw new TemplateException("Missing parameter 'record' or type mis-match.", env);
		}
		
		boolean defReadonly = Directives.getBoolParam(env, params, "readonly", false);
		//auto generate?
		boolean auto = (body == null);
		auto = Directives.getBoolParam(env, params, "auto", auto);
		
		params.remove("record");
		params.remove("readonly");
		params.remove("auto");
		
		Writer out = env.getOut();
		out.append("<form");
		
		//params
		for(Object k:params.keySet())
		{
			if("fields".equals(k))
			{
				continue;
			}
					
			String v = Directives.getStringParam(env, params, k.toString(),false,null);
			if(v != null && v.length()>0)
			{
				out.append(' ').append(k.toString()).append("=\"")
				   .append(v).append("\"");
			}
		}
				
		//id
		String id = Directives.getStringParam(env, params, "id",false,null);
		if(id==null)
		{
			String name = Directives.getStringParam(env, params, "name",false,null);
			if(name == null) id="datafrm";
			else id=name;
			
			out.append(" id=\"").append(id).append("\"");
		}		
		
		//method
		String method = Directives.getStringParam(env, params, "method",false,null);
		if(method == null)
		{
			out.append(" method=\"post\"");
		}
		
		FieldRender frender = null;
		Model model = record.getModel();
		if(auto)
		{
			String css = Directives.getStringParam(env, params, "class",false,null);						
			TemplateSequenceModel fields = null;
			tv = (TemplateModel)params.get("fields");
			if(tv != null )
			{
				if(tv instanceof TemplateSequenceModel)
				{
					fields = (TemplateSequenceModel)tv;
				}
				else
				{
					throw new TemplateException("Parameter 'fields' must be Sequence.", env);
				}
				
				if(fields.size() <=5 && (css==null || !css.equals("form-horizontal")) )
				{
					frender = new VLayoutRender(env,record);
					//out.append(">\n<fieldset>\n");
					out.append(">\n");
				}
				else
				{
					if(css==null)
					{
						out.append(" class=\"form-horizontal\"");
					}
					//out.append(">\n<fieldset>\n");
					out.append(">\n");
					frender = new HLayoutRender(env,record);
				}
				
				frender.params = params;
				frender.env = env;
				
				for(int i=0;i<fields.size();i++)
				{
					TemplateModel obj = fields.get(i);
					boolean readonly = defReadonly;
					Column col = null;
					if(obj instanceof TemplateSequenceModel)
					{
						TemplateSequenceModel arr = (TemplateSequenceModel)obj;
						if(arr.size() < 2) continue;
						
						col = model.column(arr.get(0).toString());
						TemplateModel tmp = arr.get(1);
						if(tmp instanceof TemplateBooleanModel)
						{
							readonly = ((TemplateBooleanModel)tmp).getAsBoolean();
						}
						else readonly = Convert.parseBool(arr.get(1));
					}
					else
					{
						col = model.column(obj.toString());
					}
					
					if(col.isId() && record.getState()!=RecordState.New) continue;
					
					frender.render(col, readonly,null);
				}
			}
			else
			{
				//default render				
				Column[] cols = model.getColumnList();
				if(cols.length <=5 && (css==null || !css.equals("form-horizontal")) )
				{			
					//out.append(">\n<fieldset>\n");
					out.append(">\n");
					frender = new VLayoutRender(env,record);
				}
				else
				{
					if(css==null)
					{
						out.append(" class=\"form-horizontal\"");
					}
					//out.append(">\n<fieldset>\n");
					out.append(">\n");
					frender = new HLayoutRender(env,record);
				}
				
				frender.params = params;
				frender.env = env;
				
				for(Column col : cols)
				{
					if(col.isId()) continue;
					frender.render(col, defReadonly,null);
				}
			}
			
			//out.append("</fieldset>\n");
		}
		else
		{
			out.append(">\n");
			
			frender = new FieldRender(env,record);			
			env.getCurrentNamespace().put(FORM_RECORD_NAME, frender);
			body.render(out);  //render body
			env.getCurrentNamespace().remove(FORM_RECORD_NAME);
		}
		
		if((record.getState() != RecordState.New) 
			|| (model.getStyle() == Model.MODEL_CHILD_ONE) )
		{
			if(model.getId() != null)
			{
			    out.append("<input type=\"hidden\" name=\"id\" value=\"");
			    out.append(record.getString("id")).append("\">\n");
			}
			else if(model.getKeys() != null)
			{
				for(Column col:model.getKeys())
				{
					out.append("<input type=\"hidden\" name=\"")
					   .append(col.getName())
					   .append("_ori\" value=\"")
					   .append(record.getString(col.getName()))
					   .append("\">\n");
				}
			}
		}
		
		//has date field? has time field?
		//if((frender.style & FieldRender.STYLE_DATE)!=0
		//	|| (frender.style & FieldRender.STYLE_TIME)!=0)
		if(frender.style > 0)
		{
			out.append("<script language=\"javascript\">\n");
			if((frender.style & FieldRender.STYLE_DATE)!=0 )
			{
				out.append("$('.date').datetimepicker({locale:'zh_cn'});\n"); //,pickTime:false
			}
			
			if((frender.style & FieldRender.STYLE_TIME)!=0)
			{
				//out.append("$('.bootstrap-timepicker input').timepicker({template: false,showSeconds: true,showInputs: false,minuteStep: 1});\n");
				out.append("$('.time').datetimepicker({locale:'zh_cn'});\n");//,pickDate:false
				
				if((frender.style & FieldRender.STYLE_DATE)!=0 )
				{
					out.append("$('.datetime').datetimepicker({locale:'zh_cn'});\n");
				}
			}
			
			if((frender.style & FieldRender.STYLE_ENUM)!=0)
			{
				out.append("$('.enumbtn').click(function(){chooseEnum(this)});\n");
			}
			
			if((frender.style & FieldRender.STYLE_HAVING)!=0)
			{
				out.append("$('.refbtn').click(function(){editChild(this)});\n");
			}
			
			if((frender.style & FieldRender.STYLE_CONDTIONS)!=0)
			{
				out.append("procCondtions();\n");
			}
			
			out.append("</script>\n");
		}
		
		if(auto && body!=null) body.render(env.getOut());
		
		out.append("</form>\n");
	}
}
