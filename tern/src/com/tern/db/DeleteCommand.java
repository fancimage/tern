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
import java.util.HashMap;
import java.util.Map;

public class DeleteCommand extends SQLCmd
{
	protected String tableName;
	protected StringBuffer where;
	
	DeleteCommand(Database db,String tableName)
	{
		this.db = db;
		this.tableName = tableName;
	}
	
	public DeleteCommand param(String key,Object v)
	{
		_param(key,v);
		return this;
	}
	
	public DeleteCommand param(int i,Object v)
	{
		_param(i,v);
		return this;
	}				
	
	public DeleteCommand where(String where)
	{
		return where(where,(Map<String,Object>)null);
	}
	
	public DeleteCommand where(String where,Map<String,Object> vars)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}
		
		Query.WHERE(this,this.where,where);
		Query.WHERE(this,vars);
		
		return this;
	}
	
	public DeleteCommand where(String where,Object...param)
	{
		if(this.where==null)
		{
			this.where = new StringBuffer();
		}
		
		Query.WHERE(this,this.where,where);
		Query.WHERE(this,param);
		
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
		StringBuffer buf = new StringBuffer("DELETE FROM ");
		buf.append(this.tableName);
		
		if(this.where != null && this.where.length()>0)
		{
			buf.append(" WHERE ");
			buf.append(where.toString());
		}
		
		return buf.toString();
	}
}
