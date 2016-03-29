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
import java.util.Date;
import java.util.Map;

public class InsertCommand extends SQLCmd
{
	protected String tableName;	
	protected StringBuffer cols = new StringBuffer();
	protected StringBuffer vals = new StringBuffer();

	InsertCommand(Database db,String tableName)
	{
		this.db = db;
		this.tableName = tableName;
	}
	
	public InsertCommand values(Map<String,Object> vals)
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
	
	public InsertCommand set(String col,Object val)
	{
		if(cols.length() > 0)
		{
			cols.append(",");
			vals.append(",");
		}
		
		cols.append(col);	
		
		Object obj = db.sqlvalue(val);
		if(null == obj)
		{
			vals.append("?"); //作为参数
			this._param(-1, val);
		}
		else
		{
		    vals.append(obj);
		}
	
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
		StringBuffer buf = new StringBuffer("INSERT INTO ");
		buf.append(this.tableName).append("(");
		buf.append(this.cols.toString()).append(")").append(" VALUES(");
		buf.append(this.vals).append(")");
		return buf.toString();
	}
    
}
