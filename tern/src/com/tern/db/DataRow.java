/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.io.Serializable;

import com.tern.util.Convert;

/**
 * <p>Title: 内存中的数据行</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2008</p>
 * <p>Company: iEAS</p>
 * @author Fancimage
 * @version 1.0
 */
public class DataRow implements Serializable,com.tern.dao.IRow
{
    protected DataTable dt;
    //protected Map<> _columns;

    protected Object[] _data;

    public DataRow(DataTable dt)
    {
        this.dt = dt;
        if (dt != null)
        {           
            _data = new Object[dt.getCols().size()];
        }
        else
        {
            _data = null;
        }
    }
    
    final public Object[] getData()
    {    	
    	return _data;
    }
    
    MapDataRow mr = null;
    final public java.util.Map<String , Object> getMap()
    {
    	if(mr==null)
    	{
    		mr=new MapDataRow(this);
    	}
    	return mr;
    }

    //得到本行对应列的数据
    final public Object get(String ColumnName)
    {
        int index = dt.getIndex(ColumnName);
        if (index == -1)
        {
            return null;
        }
        return get(index);
    }

    final public Object get(int index)
    {
        return (_data == null ? null : _data[index]);
    }
    
    final public long getLong(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return 0;
        }
        return getLong(index);
    }

    final public int getInt(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return 0;
        }
        return getInt(index);
    }

    public int getInt(int index)
    {
        Object o = get(index);
        /*if (o == null)
                {
            return 0; //Integer.MIN_VALUE;
                }*/
        return Convert.parseInt(o);
    }
    
    public long getLong(int index)
    {        
        return Convert.parseLong(get(index));
    }

    public float getFloat(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return 0;
        }
        return getFloat(index);
    }

    public float getFloat(int index)
    {
        Object o = get(index);
        if (o == null)
        {
            return 0;
        }
        return Convert.parseFloat(o.toString());
    }

    public double getDouble(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return 0;
        }
        return getDouble(index);
    }

    public double getDouble(int index)
    {
        Object o = get(index);
        if (o == null)
        {
            return 0;
        }
        return Convert.parseDouble(o.toString());
    }

    public boolean getBoolean(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return false;
        }
        return getBoolean(index);
    }

    public boolean getBoolean(int index)
    {
        Object o = get(index);
        /*if (o == null)
                {
            return false;
                }*/
        return Convert.toBoolean(o);
    }

    public String getString(int index)
    {
        Object o = get(index);
        if (o == null)
        {
            return null;
        }
        return o.toString().trim();
    }

    public String getString(String colName)
    {
        int index = dt.getIndex(colName);
        if (index == -1)
        {
            return null;
        }
        return getString(index);
    }
    
    public java.util.Date getDateTime(int index)
    {
    	Object o = get(index);
    	if(o instanceof java.util.Date)
    	{
    		return (java.util.Date)o;
    	}
    	else
    	{
    		return Convert.parseDate(o);
    	}
    }
    
    public java.util.Date getDateTime(String colName)
    {
    	int index = dt.getIndex(colName);
        if (index == -1)
        {
            return null;
        }
        return getDateTime(index);
    }

    public DataRow set(String ColumnName, Object o)
    {
        int index = dt.getIndex(ColumnName);
        set(index, o);
        return this;
    }

    public void set(int index, Object o)
    {
        if (_data == null)
        {
            return;
        }
        //如果不存在index处的元素，则扩充之
        if (index >= _data.length)
        {
            return;
        }
        if (o != null && o instanceof String)
        {
            o = Convert.toString(o);
        }
        /*if (_data.length < _columns.size())
                {
            int len = _columns.size() - _data.length;
            for (int i = 0; i < len; i++)
            {
                _data.add(null);
            }
                }*/
        _data[index] = o;
        // _data.set(index, o);
    }

}
