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

import javax.servlet.http.HttpServletRequest;

import com.tern.dao.RecordSet;
import com.tern.ui.freemarker.Directives;
import com.tern.util.Convert;

import freemarker.core.Environment;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/*
 * Query: for RecordSet(model) or DataTable
 * */
public class Query implements TemplateDirectiveModel
{
	static final String QUERY_NAME = ".tern.query";
	
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException 
	{
		//pagesize
		int pageSize = Directives.getIntParam(env, params, "pagesize", 0);	
		String defSort = Directives.getStringParam(env, params, "sort",false,null);
		if(pageSize <= 0 && body == null && defSort == null) return;
				
		//name
		String name = Directives.getStringParam(env, params, "name",false,"_");		
		
		//source
		RecordSet rs = null;
		TemplateModel tv = (TemplateModel)params.get("source");
		if(tv != null)
		{
			if(tv instanceof AdapterTemplateModel)
			{
				Object obj = ((AdapterTemplateModel)tv).getAdaptedObject(null);
				if(obj instanceof RecordSet)
				{
					rs = (RecordSet)obj;
				}
			}
		}
		
		if(rs == null)
		{
			throw new TemplateException("Missing parameter 'source' or type mis-match.", env);
		}		
				
		QueryInfo info = new QueryInfo();
		
		info.name = name;
		info.records = rs;
		info.pageSize = pageSize;
				
		env.getCurrentNamespace().put(QUERY_NAME, info);
		
		HttpServletRequest request = (HttpServletRequest)((AdapterTemplateModel)env.getVariable("params")).getAdaptedObject(null);
		info.request = request;
		
		Writer out = env.getOut();
		out.append("<form id=\"").append(name).append("\" method=\"post\" style=\"margin:0 0;display:inline\"");
		
		String pv = Directives.getStringParam(env, params, "class",false,null);
		if(pv!=null)
		{
			out.append(" class=\"").append(pv).append("\"");
		}
		
		pv = Directives.getStringParam(env, params, "action",false,null);
		if(pv!=null)
		{
			out.append(" action=\"").append(pv).append("\"");
		}
		
		out.append(">\n");
		//to execute child-elements
		if(body!=null) body.render(out);
		env.getCurrentNamespace().remove(QUERY_NAME);
		
		boolean isPost = "POST".equalsIgnoreCase(request.getMethod());
		//query-conditions
		if(info.condtions != null)
		{
			for(String c:info.condtions.values())
			{
				rs.where(c);
			}
		}
		
		//sort
		String sort = null;
		if(isPost)
		{
			sort = request.getParameter(name+"_sort");
		}
		if(sort == null) sort = defSort;
		if(sort != null)
		{
			sort = sort.trim();
			if(sort.length()>0)
			{
				rs.order(sort);
			}
		}
		
		out.append("<input type=\"hidden\" name=\"").append(name+"_sort")
		   .append("\" value=\"").append(sort==null|| sort.length()<=0?"":sort).append("\">");
		
		if(info.pageSize > 0)
		{
			int current = 1;
			int total = -1;
			SimpleHash map = new SimpleHash();
			
			if(isPost)
			{
				current = Convert.parseInt(request.getParameter(name+"_page_current"), 1);								
				total = Convert.parseInt(request.getParameter(name+"_page_total"), 0);							
			}
			
			if(total < 0)
			{
				total = rs.count();
			}
			
			//to paged:limit and offset
			rs.limit(info.pageSize);
			if(current > 1)
			{
				rs.offset(info.pageSize*(current-1));
			}
			
			map.put("current", current);
			map.put("total", total);
			
			int pageCount = total/pageSize;
			if(total%pageSize > 0) pageCount++;
			map.put("pagecount", pageCount);
			
			env.setVariable(name, map);		
			
			out.append("<input type=\"hidden\" name=\"").append(name+"_page_current")
			   .append("\" value=\"").append(String.valueOf(current)).append("\">");
			out.append("<input type=\"hidden\" name=\"").append(name+"_page_total")
			   .append("\" value=\"").append(String.valueOf(total)).append("\">");
		}
		
		out.append("\n</form>");
	}
	
	static class QueryInfo
	{
		public String name;
		public RecordSet records;
		public int pageSize = -1;  //-1: no-paging
		
		public Map<String,String> condtions;
		HttpServletRequest request;
	}
}
