/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Query extends AbstractQuery implements Iterable<DataRow>
{	
	protected StringBuffer tableName;
	protected StringBuffer select;
	protected StringBuffer where;
	protected StringBuffer order;
	protected StringBuffer group;
	protected int limit = 0;
	protected int offset = 0;
	
	protected Query[] children;  /*子查询*/

	public Query(Database db,String tableName,String alias)
	{
		this.db = db;
		this.tableName = new StringBuffer(tableName);
		if(alias!=null && alias.trim().length()>0)
		{
			this.tableName.append(" ").append(alias.trim());
		}
	}
	
	public Query(Query c)
	{
		this.db = c.db;
		children = new Query[]{c};
	}
	
	public Query table(String tableName)
	{		
		return table(tableName,null);
	}
	
	public Query table(String tableName,String alias)
	{
		this.tableName.append(",").append(tableName);
		if(alias!=null && alias.trim().length()>0)
		{
			this.tableName.append(" ").append(alias.trim());
		}
		return this;
	}
	
	public Query addChild(Query q)
	{
		if(q != this)
		{
			if(children == null) children = new Query[]{q};
			else
			{
				Query[] arr = new Query[children.length+1];
				int i = 0;
				for(;i<children.length;i++) arr[i] = children[i];
				arr[i] = q;
				
				children = arr;
			}
		}
		return this;
	}
	
	public Query leftJoin(String tableName, String condition,String alias)
	{
		if(this.tableName == null) this.tableName = new StringBuffer();
		
		this.tableName.append(" LEFT JOIN ").append(tableName);
		if(alias!=null && alias.trim().length()>0)
		{
			this.tableName.append(" ").append(alias.trim());
		}
		
		this.tableName.append(" ON ").append(condition);
		return this;
	}
	
	public Query param(String key,Object v)
	{
		_param(key,v);
		return this;
	}	
	
	public Query param(int i,Object v)
	{
		_param(i,v);
		return this;
	}
	
	static void WHERE(SQLCmd cmd,StringBuffer _where,String where)
	{
		if(where == null) return;
		else where = where.trim();
		
		if(where.length()<=0) return;
		
		if(_where.length()>0)
		{
			_where.append(" AND (").append(where).append(")");
		}
		else
		{
			_where.append(where);
		}
	}
	
	static void WHERE(SQLCmd cmd,Map<String,Object> vars)
	{				
		if(vars!=null && vars.size()>0)
		{
			if(cmd.params == null) cmd.params = new HashMap<String,Object>();
			for(Map.Entry<String, Object> entry:vars.entrySet())
			{
				cmd.params.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	static void WHERE(SQLCmd cmd,Object...param)
	{		
		if(param!=null && param.length>0)
		{
			if(cmd.sqlparams == null)
			{
				cmd.sqlparams = new java.util.ArrayList<Object>();
			}
			
			for(Object o:param)
			{
				cmd.sqlparams.add(o);
			}					
		}
	}	
	
	public Query where(String where)
	{
		return where(where,(Map<String,Object>)null);
	}
	
	public Query where(String where,Map<String,Object> vars)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}		
		
		WHERE(this,this.where,where);
		WHERE(this,vars);
		
		return this;
	}
	
	public Query where(String where,Object...param)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}
		
		WHERE(this,this.where,where);
		WHERE(this,param);
		
		return this;
	}		
	
	public Query select(String columns)
	{
		if(select==null)
		{
			select = new StringBuffer(columns);
		}
		else
		{
			select.append(",").append(columns);
		}
		return this;
	}
	
	public StringBuffer getSelect(){return select;}
	
	public Query order(String order)
	{
		if(this.order == null)
		{
			this.order = new StringBuffer(order);
		}
		else
		{
			this.order.append(",").append(order);
		}
		return this;
	}
	
	public Query group(String groups)
	  {
	    if (this.group == null)
	    {
	      this.group = new StringBuffer(groups);
	    }
	    else
	    {
	      this.group.append(",").append(groups);
	    }
	    return this;
	  }
	
	public Query limit(int n)
	{
		this.limit = n;
	    return this;
	}
	
	public Query offset(int n)
	{
		this.offset = n;
		return this;
	}
	
	public int count() throws SQLException
	{
		StringBuffer old = this.select;
		this.select = new StringBuffer("COUNT(1)");
				
		try
		{
		    return this.queryInt();
		}
		finally
		{
			this.select = old;
		}
	}
	
	/*
	 * 将当前的查询条件处理成子查询
	 * */
	/*public Query subquery()
	{
		String child = getSql();
		
		Query nq = new Query(this.db , "("+child+")" , "A");
		nq.select("A.*");
		
		if(this.sqlparams != null )
		{
			nq.sqlparams = new ArrayList<Object>();
			nq.sqlparams.addAll(this.sqlparams);
		}
		
		if(this.params != null)
		{
			nq.params = new HashMap<String,Object>();
			nq.params.putAll(this.params);
		}
		
		return nq;
	}*/
	
	@Override
	protected Map<String,Object> getNamedParams() 
	{
		if(this.children == null)
		{
		    return params;
		}
		else
		{
			Map<String,Object> ret = new HashMap<String,Object>();
			if(this.params!=null) ret.putAll(this.params);
			
			for(Query q:children)
			{
				if(q.params!=null) ret.putAll(q.params);
			}
			
			return ret;
		}
	}
	
	@Override
	protected List<Object> getSQLParams()
	{
		if(this.children == null)
		{
		    return this.sqlparams;
		}
		else
		{
			List<Object> ret = new ArrayList<Object>();
			for(Query q:children)
			{
				if(q.sqlparams!=null) ret.addAll(q.sqlparams);
			}
			
			if(sqlparams!=null) ret.addAll(sqlparams);			
			return ret;
		}
	};
	
	@Override
	String getSql() 
	{		
		Map<String,String> clauses = new java.util.HashMap<String, String>();
		clauses.put("SELECT", select==null?"*":select.toString());
		
		StringBuffer from = null;
		if(this.children == null)
		{
			from = this.tableName;
		}
		else
		{
			from = new StringBuffer();
			for(int i = 0; i< children.length;i++)
			{
				if(from.length()>0) from.append(",");
				from.append("(").append(children[i].getSql()).append(") ").append("C").append((i+1));
			}
			
			if(tableName != null)
			{
				String str = this.tableName.toString();// LEFT 
				if(!str.startsWith(" LEFT "))
				{
					from.append(",");
				}
				from.append(str);
			}
		}
		
		clauses.put("FROM", from.toString());
		
		if(where!=null && where.length()>0)
		{
			clauses.put("WHERE", this.where.toString());
		}
		
		if (this.group != null)
	    {
	      clauses.put("GROUP BY", this.group.toString());
	    }
		
		if(this.order!=null)
		{
			clauses.put("ORDER BY", this.order.toString());
		}
		
		int mode = 0;
		if((this.limit>0 || this.offset > 0) 
				&& this.db.getDbType() == DBType.oracle)
		{
		    if(this.offset <= 0)
		    {
		    	if(this.order==null)
		    	{
		    		String _where = clauses.get("WHERE");
		    		if(_where!=null)
		    		{
		    			clauses.put("WHERE", "("+_where+") AND (ROWNUM<="+this.limit+")");
		    		}
		    		else
		    		{
		    			clauses.put("WHERE", "ROWNUM<="+this.limit);
		    		}		    		
		    	}
		    	else
		    	{
		    		mode = 1;
		    	}
		    }
		    else
		    {
		    	//String _sels = clauses.get("SELECT");
		    	//clauses.put("SELECT", _sels+",ROWNUM AS _ROWNUM");
		    	mode = 2;
		    }
		}
		else
		{
			if(this.limit>0)
			{
				clauses.put("LIMIT", String.valueOf(limit));
			}
			
			if(this.offset > 0)
			{ 
				clauses.put("OFFSET", String.valueOf(offset));
			}	
		}				
		
		StringBuffer buf = new StringBuffer();
		for(String[] key:db.sqlClauses())
		{
			String k = key[0];
			if(null == k)
			{
				if(key.length>1)  //const
				{
					buf.append(key[1]);					
				}
				
				continue;
			}
			
			if(clauses.containsKey(k))
			{
				if(buf.length()>0) buf.append(" ");
				String str = key.length>1?key[1]:k;
				buf.append(str);
				
				if(str.length()>0)
				{
				    buf.append(" ");
				}
				
				buf.append( clauses.get(k) );
			}
		}
		
		/*FOR ORACLE*/
		if(mode == 1)
		{
			buf.insert(0, "SELECT * FROM (");
			buf.append(") WHERE ROWNUM<=").append(this.limit);
		}
		else if(mode == 2)
		{
			buf.insert(0, "SELECT * FROM (SELECT ROW_.*,ROWNUM AS ROWNUM_ FROM (");
			buf.append(") ROW_ WHERE ROWNUM<=").append(this.offset+this.limit)
			   .append(") WHERE ROWNUM_>")
			   .append(this.offset);
		}
		
		return buf.toString();
	}		

	@Override
	public Iterator<DataRow> iterator()
	{
		try
		{
			DataTable dt = this.query();
			return dt.iterator();
		} 
		catch (SQLException e)
	    {
			return null;
		}
		
	}	

}
