/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.tern.util.Convert;

abstract class AbstractQuery extends SQLCmd
{
	public DataTable query() throws SQLException
    {
		DataTable dt = new DataTable();
    	return query(dt);    	    
    }
	
	DataTable query(DataTable dt) throws SQLException
	{
		SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		dt.fill(rs);
    		rs.close();
    		
    		return dt;
    	}
    	finally
    	{
    		executor.close();
    	}
	}
	
	public <T> List<T> query(RowMapper<T> mapper) throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		
    		List<T> list = null;
    		if(mapper instanceof AbstractRowMapper)
    		{
    			list = ((AbstractRowMapper<T>)mapper).createList();
    		}
    		else
    		{
    		    list = new java.util.ArrayList<T>();
    		}
    		
    		int rowNum = 0;
    		while(rs.next())
    		{
    			T t = mapper.map(rs, rowNum);
    			rowNum++;
    			
    			if(t!=null) list.add(t);
    		}
    		rs.close();
    		
    		return list;
    	}
    	finally
    	{
    		executor.close();
    	}    	   
    }
	
	public <T> Set<T> querySet() throws SQLException
	{
		return querySet(null);
	}
	
	public <T> Set<T> querySet(RowMapper<T> mapper) throws SQLException
	{
		SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		
    		Set<T> list = new java.util.HashSet<T>();
    		if(null == mapper)
			{
    			while(rs.next())
    			{
    				@SuppressWarnings("unchecked")
					T v = (T)rs.getObject(1);
    				if(null != v)
    				{
    					list.add(v);
    				}
    			}
			}
    		else
    		{
    			int rowNum = 0;
        		while(rs.next())
        		{
        			
        			T t = mapper.map(rs, rowNum);
        			rowNum++;
        			
        			if(t!=null) list.add(t);
        		}
    		}
    		
    		rs.close();    		
    		return list;
    	}
    	finally
    	{
    		executor.close();
    	}    
	}
	
	public <T> T queryOne(RowMapper<T> mapper) throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		
    		T ret = null;
    		if(rs.next())
    		{
    			ret = mapper.map(rs, 0);
    		}
    		rs.close();
    		
    		return ret;
    	}
    	finally
    	{
    		executor.close();
    	}    	   
    }
	
	public DataRow queryOne() throws SQLException
	{
		return queryOne(false);
	}
	
	public DataRow queryOne(boolean restrict) throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		    		
    		DataTable dt = new DataTable();
    		dt.fill(rs,2);
    		rs.close();
    		
    		if(restrict && dt.size() > 1)
    		{
    			throw new SQLException("too many result!");
    		}
    		else if(dt.size() > 0)
    		{    			
    		    return dt.get(0);
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
    
    public Object queryObject() throws SQLException
    {
    	SQLExecutor executor = db.createExecutor(this);
    	try
    	{
    		ResultSet rs = executor.query();
    		Object ret = null;
    		if(rs.next())
    		{
    			ret = rs.getObject(1);
    		}
    		
    		rs.close();
    		return ret;
    	}
    	finally
    	{
    		executor.close();
    	}
    }
    
    public String queryString() throws SQLException
    {
    	Object v = queryObject();
    	if(v == null) return null;
    	else if(v instanceof String) return (String)v;
    	else return v.toString();
    }
    
    public int queryInt() throws SQLException
    {
    	return queryInt(0);
    }
    
    public int queryInt(int def) throws SQLException
    {
    	Object v = queryObject();
    	
    	if(null == v) return def;
    	else if(v instanceof Integer) return ((Integer)v).intValue();
    	else return Convert.parseInt(v, def);
    }
    
    public long queryLong() throws SQLException
    {
    	return queryLong(0);
    }
    
    public long queryLong(long def) throws SQLException
    {
    	Object v = queryObject();
    	
    	if(null == v) return def;
    	else if(v instanceof Long) return ((Long)v).longValue();
    	else return Convert.parseLong(v, def);
    }
    
    public double queryDouble() throws SQLException
    {
    	return queryDouble(0);
    }
    
    public double queryDouble(double def) throws SQLException
    {
    	Object v = queryObject();
    	
    	if(null == v) return def;
    	else if(v instanceof Double) return ((Double)v).doubleValue();
    	else return Convert.parseDouble(v, def);
    }
    
    public float queryFloat() throws SQLException
    {
    	return queryFloat(0);
    }
    
    public float queryFloat(float def) throws SQLException
    {
    	Object v = queryObject();
    	
    	if(null == v) return def;
    	else if(v instanceof Float) return ((Float)v).floatValue();
    	else return Convert.parseFloat(v, def);
    }
    
    public boolean queryBool() throws SQLException
    {
    	return queryBool(false);
    }
    
    public boolean queryBool(boolean def) throws SQLException
    {
    	Object v = queryObject();
    	
    	if(null == v) return def;
    	else if(v instanceof Boolean) return ((Boolean)v).booleanValue();
    	else return Convert.toBoolean(v, def);
    }
}
