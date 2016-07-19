/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tern.dao.IRow;

class MapDataRow implements java.util.Map<String, Object>,IRow
{
    DataRow row = null;
    
    MapDataRow(DataRow r)
	{
		row = r;
	}
    
	@Override
	public void clear() {		
	}

	@Override
	public boolean containsKey(Object arg0) 
	{
		return row.dt._columns.containsKey(arg0);
	}

	@Override
	public boolean containsValue(Object arg0) {
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() 
	{
		return null;
	}

	@Override
	public Object get(Object key) {
		if(key == null) return null;

		String k = key.toString();
		Object ret = row.get(k);
		if(ret == null)
		{
			if(k.equals("data")) return row.getData();
			else if(k.equals("map")) return this;
			else return null;
		}
		else
		{
			return ret;
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Set<String> keySet() {
		return row.dt._columns.keySet();
	}

	@Override
	public Object put(String arg0, Object arg1) {
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> arg0) {		
	}

	@Override
	public Object remove(Object arg0) {
		return null;
	}

	@Override
	public int size() {
		return row._data.length;
	}

	@Override
	public Collection<Object> values() {
		return null;
	}

	@Override
	public Object get(String key) 
	{		
		return row.get(key);
	}

	@Override
	public IRow set(String key, Object val) 
	{
		row.set(key, val);
		return this;
	}
    
}
