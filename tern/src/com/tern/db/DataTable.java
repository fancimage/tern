/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.ResultSet;
import java.io.Serializable;
import java.sql.ResultSetMetaData;

import com.tern.util.Trace;

/**
 * <p>Title: 内存中的数据表</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2008</p>
 * <p>Company: iEAS</p>
 * @author Fancimage
 * @version 1.0
 */
public class DataTable implements Serializable,com.tern.dao.ITable,Iterable<DataRow>
{
    protected String _tableName;
    protected List<DataRow> _rows; //数据行集合
    protected Map<String,Integer> _columns;

    Map<String,Integer> getCols()
    {
        return _columns;
    }

    public DataTable()
    {
        this("Table1");
    }

    public DataTable(String name)
    {
        _tableName = name;
        _rows = new ArrayList<DataRow>();
        _columns = new HashMap<String,Integer>();
    }

    public String getTableName()
    {
        return _tableName;
    }

    public void setTableName(String name)
    {
        _tableName = name;
    }
    
    final public DataRow createRow()
    {
    	return new DataRow(this);
    }

    public void addColumn(String colName)
    {

    }

    //得到指定的行
    public DataRow get(int index)
    {
        /*if(index<0 || index>=getRowsCount)
                  {
                  }*/
        //在这里不判断和捕捉下标溢出错误
        return (DataRow) _rows.get(index);
    }

    //得到表内的行数
    @Deprecated
    public int getRowsCount()
    {
        return _rows.size();
    }
    
    public int size()
    {
    	return _rows.size();
    }

    public List<DataRow> getRows()
    {
        return _rows;
    }

    public String getColumnName(int index){
      String[] cNames = getColumnNames();
      return cNames[index];
    }

    public String[] getColumnNames()
    {
        if (_columns != null)
        {
            String[] ret = new String[_columns.size()];
            java.util.Iterator<String> it = _columns.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                int index = ( (Integer) _columns.get(key)).intValue();
                ret[index] = key;
            }
            return ret;
        }
        else
        {
            return null;
        }
    }

    //得到表内的列数
    public int getColumnsCount()
    {
        return _columns.size();
    }
    
    @Deprecated
    public final DataTable retriveFromDB(String sqlStr) throws java.sql.SQLException
    {
    	return db.sql(sqlStr).query(this);
    	/*try
    	{
    		
    		cmd = SqlHelper.helper().createCommand(sqlStr);
    		cmd.execute(this);
    	}
    	catch(Exception e)
    	{
    		throw new DBException(e.getMessage());
    	}
    	finally
    	{
    		if(cmd != null) cmd.Dispose();
    	}*/
    }
    
    /*@Deprecated
    public final void retriveFromDB(DBCommand cmd) throws DBException
    {
    	try
    	{
    	    cmd.execute(this);
    	}
    	catch(Exception e)
    	{
    		throw new DBException(e.getMessage());
    	}
    }
    
    @Deprecated
    public final void retriveFromDB(java.sql.ResultSet rs) throws SQLException
    {
    	if (rs != null)
    	{
    		try
    		{
    			this.fill(rs);
    		}
    		catch(Exception e)
        	{
        		throw new DBException(e.getMessage());
        	}
    	}
    }*/

    //从一个ResultSet获得DataTable的数据(目前，唯一生成DataTable的方法)
    final void fill(ResultSet rs) throws java.sql.SQLException
    {
    	fill(rs,0);
    }
    
    final void fill(ResultSet rs,int max) throws java.sql.SQLException
    {
        if (rs == null)
        {
            return;
        }
        //生成表的列
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();
        _columns.clear(); //int j=0;
        for (int i = 1; i <= columnCount; i++)
        {
            String o = md.getColumnLabel(i);//md.getColumnName(i);
            if(o==null || o.length()<=0) o = md.getColumnName(i);
            
            if (_columns.containsKey(o))
            {
                o = o.toString() + i;
            }
            if (o != null)
            {
                o = o.toString().toLowerCase();
            }
            _columns.put(o, new Integer(i - 1));
        }
        //生成各个行的数据
        _rows.clear();
        while (rs.next())
        {
            DataRow r = new DataRow(this);
            for (int i = 0; i < columnCount; i++)
            {
                Object o =null;
                try
                {
                    o= rs.getObject(i + 1);
                    if(o != null)
                    {
                    	if(o instanceof java.sql.Date)
                    	{
                    		o = rs.getTimestamp(i + 1);
                    	}
                    	else if(o instanceof java.sql.Clob)
                    	{
                    		java.sql.Clob clob = (java.sql.Clob)o;
                    		java.io.Reader reader = clob.getCharacterStream();
                    		char[] chs = new char[ (int) clob.length()];
                            
                    		try
                    		{
                    		    reader.read(chs);
                    		}
                    		finally
                    		{
                    			reader.close();
                    		}
                            
                            o = new String(chs);
                    	}
                    	else if(o instanceof java.sql.Blob)
                    	{
                    		java.sql.Blob blob = (java.sql.Blob)o;
                    		java.io.InputStream in = blob.getBinaryStream();
                            byte[] data = new byte[ (int) blob.length()];
                            
                            try
                            {
                                in.read(data);
                            }
                            finally
                            {
                            	in.close();
                            }
                            
                            o = data;
                    	}
                    }
                }
                catch (Exception e)
                {
                    Trace.write(Trace.Error,e,"");
                }
                r.set(i, o);
            }
            _rows.add(r);
            
            if(max > 0 && _rows.size() >= max)
            {
            	break;
            }
        }
    }

    public void addRow(DataRow row)
    {
        if (_rows == null)
        {
            _rows = new ArrayList<DataRow>();
        }
        _rows.add(row);
    }

    public int getIndex(String columnName)
    {
        if (_columns == null)
        {
            return -1;
        }
        try
        {
            int index = ( (Integer) _columns.get(columnName.toLowerCase())).
                intValue();
            return index;
        }
        catch (Exception e)
        {
            return -1;
            //throw new entc.common.UserException("请求的列不存在:" + columnName);
        }
    }

	@Override
	public Iterator<DataRow> iterator()
	{		
		return this._rows.iterator();
	}
	
	
}
