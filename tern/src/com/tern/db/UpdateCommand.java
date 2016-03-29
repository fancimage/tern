/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.util.Map;
import java.sql.SQLException;

public class UpdateCommand extends SQLCmd
{
	protected String tableName;
	protected StringBuffer where;
	protected StringBuffer sets = new StringBuffer();
	
	UpdateCommand(Database db,String tableName)
	{
		this.db = db;
		this.tableName = tableName;
	}
	
	public UpdateCommand values(Map<String,Object> vals)
	{
		if(vals != null)
		{
			for(Map.Entry<String, Object> entry:vals.entrySet())
			{
				set(entry.getKey(),entry.getValue());
			}
		}
		return this;
	}
	
	public UpdateCommand set(String col,Object val)
	{
		if(sets.length()>0) sets.append(",");
		sets.append(col).append("=");
		
		Object obj = db.sqlvalue(val);
		if(obj==null)
		{
			sets.append("?");
			this._param(-1, val);
		}
		else
		{
		    sets.append(obj);
		}
		
		return this;
	}
	
	public UpdateCommand where(String where)
	{
		return where(where,(Map<String,Object>)null);
	}
	
	public UpdateCommand where(String where,Map<String,Object> vars)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}		
		
		Query.WHERE(this,this.where,where);
		Query.WHERE(this,vars);
		
		return this;
	}
	
	public UpdateCommand where(String where,Object...param)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}
		
		Query.WHERE(this,this.where,where);
		Query.WHERE(this,param);
		
		return this;
	}
	
	public UpdateCommand param(String key,Object v)
	{
		_param(key,v);
		return this;
	}
	
	public UpdateCommand param(int i,Object v)
	{
		_param(i,v);
		return this;
	}
	
	public int exec() throws SQLException
	{
		SQLExecutor executor = db.createExecutor(this);
    	
    	try
    	{
    		return executor.execute();
    	}
    	finally
    	{
    		executor.close();
    	}    
	}

	@Override
	String getSql() 
	{		
		StringBuffer buf = new StringBuffer("UPDATE ");
		buf.append(this.tableName).append(" SET ").append(sets.toString());
		
		if(this.where != null && this.where.length()>0)
		{
			buf.append(" WHERE ");
			buf.append(where.toString());
		}
		
		return buf.toString();
	}
    
}
