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
import java.util.List;
import java.util.HashMap;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.ResultSet;

import com.tern.util.Convert;

public class SQL extends AbstractQuery
{
	protected String sql;
	protected boolean isProc=false;		
	
	//protected int resCount;	
	//protected boolean isdone;
	
	SQL(Database db,String sql,Map<String,Object> params)
	{
		this.db = db;
		this.sql = sql;
		this.params = params;
	}
	
	SQL(Database db,String sql,Object... params)
	{
		this.db = db;
		this.sql = sql;
		if(params!=null && params.length>0)
		{
			this.sqlparams = new java.util.ArrayList<Object>();
			for(Object o:params)
			{
				this.sqlparams.add(o);
			}
		}		
	}
	
	public SQL param(String key,Object v)
	{
		_param(key,v);
		return this;
	}
	
	public SQL param(int i,Object v)
	{
		_param(i,v);
		return this;
	}
	
    public SQL procedure(boolean isProcedure)
    {
    	this.isProc = isProcedure;
    	return this;
    }
    
    /*public SQL resultCount(int count)
    {
    	this.resCount = count>0?count:1;
    	return this;
    }*/
    
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
    
    public int exec(String sql)  throws SQLException
    {
    	this.sql = sql;
    	return exec();
    }
    
    /*public db.list query() throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		
    		ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            db.list list = new db.list();
    		
    		while(rs.next())
    		{
    			db.row row = new db.row(columnCount);
    			for (int i = 1; i <= columnCount; i++)
    	        {
    	            String name = md.getColumnLabel(i);
    	            row.put(name, rs.getObject(name));
    	        }
    			
    			list.add(row);
    		}
    		rs.close();
    		
    		return list;
    	}
    	finally
    	{
    		executor.close();
    	}     	  
    }*/         
    
    public void queryAll(ResultAction act) throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		int index = 0;
    		ResultSet rs = executor.queryNext();
    		while(rs != null)
    		{
    			act.run(rs, index);
    			rs.close();
    			
    			index++;
    			//next
    			rs = executor.queryNext();
    		}
    	}
    	finally
    	{
    		executor.close();
    	}
    }
    
    public DataTable[] queryAll() throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		List<DataTable> ret = new java.util.ArrayList<DataTable>();
    		ResultSet rs = executor.queryNext();
    		while(rs != null)
    		{
    			DataTable dt = new DataTable();
    			dt.fill(rs);
    			rs.close();
    			
    			ret.add(dt);
    			
    			//next
    			rs = executor.queryNext();
    		}
    		
    		if(ret.size() > 0)
    		{
    			DataTable[] dts = new DataTable[ret.size()];
    			dts = ret.toArray(dts);
    			return dts;
    		}
    		else
    		{
    			return null;
    		}
    	}
    	finally
    	{
    		executor.close();
    	}
    }        

	@Override
	String getSql()
	{
		return this.sql;
	}

	@Override
	boolean isProcedure() 
	{
		return this.isProc;
	}
        
}

