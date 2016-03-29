/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tern.db.Query;
import com.tern.db.db;
import com.tern.util.Convert;

public class RecordSet implements Iterable<Record>,ITable
{	
    private List<Record> data = null;//new ArrayList<Record>();
    private boolean isDone = false;
    
    Model model;
    Query query;
    
    //sql clause
    //private String _select;
    //private String _where;
    //private String _order;
    //private int _limit;
    //private int _offset;
    //Map<String,Object> _vars;
    //List<Object> _params;
    List<Model> joins;
    
    private RecordSet(){}
    
    RecordSet(List<Record> src)
    {
    	this.data = src;
    	this.isDone = true;
    	this.model = src.get(0).getModel();
    	
    	for(Record r:src)
    	{
    		r._rs = this;
    	}
    }
    
    RecordSet(Model m,String where,Map<String,Object> vars)
    {
    	this.model = m;
    	query = m.db.table(m.getName(),"A");
    	query.where(where,vars);
    	
    	//this._where = where;
    	//this._vars = _cloneVars(vars);
    }
    
    RecordSet(Model m,String where,Object[] params)
    {
    	this.model = m;
    	query = m.db.table(m.getName() , "A");
    	query.where(where,params);
    	
    	/*this._where = where;    	
    	if(params != null)
    	{
    		_params = new ArrayList<Object>(params.length);
    		for(Object p : params) _params.add(p);
    	}*/
    }
    
    public RecordSet where(String where)
    {
    	return where(where,(Object[])null);
    }        
    
    public RecordSet where(String where,Map<String,Object> vars)
    {
    	if(isDone)
    	{
    	    return _clone().where(where,vars);	
    	}    	
    	
    	//this._where = where==null?null:where.trim();
    	//this._vars = _cloneVars(vars);
    	query.where(where,vars);
    	
    	return this;
    }
    
    public RecordSet where(String where,Object...param)
    {
    	if(isDone)
    	{
    	    return _clone().where(where,param);	
    	} 
    	
    	query.where(where,param);
    	
    	return this;
    }
    
    public RecordSet param(String key,Object value)
    {
    	/*if(_vars == null)
    	{
    		_vars = new java.util.HashMap<String, Object>();
    	}
    	
    	_vars.put(key, sqlvalue(value));*/
    	if(isDone)
    	{
    		return _clone().param(key, value);
    	}
    	
    	query.param(key, value);
    	
    	return this;
    }
    
    public RecordSet param(int pos,Object value)
    {
    	/*if(_params == null) _params = new ArrayList<Object>();
    	if(pos>=0 && pos < _params.size())
    	{
    		_params.set(pos, value);
    	}
    	else if(pos == _params.size())
    	{
    		_params.add(value);
    	}
    	else if(pos > _params.size())
    	{
    		for(int i= _params.size();i<pos;i++) _params.add(null);
    		_params.add(value);
    	}*/
    	if(isDone)
    	{
    		return _clone().param(pos, value);
    	}
    	
    	query.param(pos, value);    	
    	return this;
    }
    
    private boolean hasSelect = false;
    public RecordSet select(String columns)
    {
    	if(isDone)
    	{
    	    return _clone().select(columns);	
    	}
    	
    	//this._select = columns==null?null:columns.trim();
    	query.select(columns);
    	hasSelect = true;
    	return this;
    }
    
    public RecordSet order(String order)
    {
    	if(isDone)
    	{
    	    return _clone().order(order);
    	}
    	
    	//this._order = order==null?null:order.trim();
    	query.order(order);
    	return this;
    }
    
    public RecordSet limit(int limit)
    {
    	if(isDone)
    	{
    	    return _clone().limit(limit);
    	}
    	
    	//this._limit = limit;
    	query.limit(limit);
    	return this;
    }
    
    public RecordSet offset(int offset)
    {
    	if(isDone)
    	{
    	    return _clone().offset(offset);
    	}
    	
    	//this._offset = offset;
    	query.offset(offset);
    	return this;
    }
    
    public RecordSet joinAll()
    {
    	if(model._relations != null)
    	{
    		for(Relation r:model._relations.values())
    		{
    			if(r.mode == Relation.BELONGS) join(r.getRef(),false,r.getMap());
    		}
    	}
    	
    	return this;
    }
    
    public RecordSet join(String pModelName)
    {
    	return join(pModelName,false,(String[][])null);
    }
    
    public RecordSet join(String pModelName,boolean allFields)
    {
    	return join(pModelName,allFields,(String[][])null);
    }
    
    public RecordSet join(String pModelName,boolean allFields,String[] mapping)
    {    	
    	if(mapping.length%2 != 0)
    	{
    		throw new ModelException(model,"join mapping parameter error.");
    	}
    	
    	int len = mapping.length/2;
    	String[][] arr = new String[len][2];
    	for(int i=0;i<len;i+=1)
    	{
    		arr[i] = new String[]{mapping[i*2],mapping[i*2+1]};
    	}
    	
    	return join(pModelName,allFields,arr);
    }
    
    public RecordSet join(String pModelName,boolean allFields,String[][] mapping)
    {
    	Model m;
    	if(mapping == null)
    	{
    		Relation r = model.relation(pModelName);
    		if(r == null)
    		{
    			throw new ModelException(model,String.format("relation(%) for %s does not exists.", pModelName,model.getName()));
    		}
    		
    		if(r.mode != Relation.BELONGS) return this;
    		
    		m = r.getRef();
    		mapping = r.getMap();
    	}
    	else
    	{
    		m = Model.from(pModelName);
    	}
    	
    	return join(m,allFields,mapping);
    }
    
    private static String fieldFormat = "%s.%s AS %s_%s_"; //oracle不支持以_开始的字段名
    static String aliasFieldName(String alias,String name)
    {
    	return String.format(fieldFormat, alias,name,alias,name);
    }
    
    private RecordSet join(Model m,boolean allFields,String[][] mapping)
    {
    	char ch = 'B';
    	if(joins!=null && joins.size()>0)
    	{
    		ch += joins.size();
    	}
    	
    	String alias = String.valueOf(ch);
    	
    	String tname = m.getName();
    	StringBuffer joinCondtion = new StringBuffer();
    	for(String[] arr : mapping)
    	{
    		if(arr.length!=2) return this;
    		
    		String f1 = model.column(arr[0]).getName();
    		String f2 = m.column(arr[1]).getName();
    		
    		if(joinCondtion.length() > 0) joinCondtion.append(" AND ");
    		//String w = String.format("%s.%s=A.%s", alias,f2,f1);
    		joinCondtion.append(alias).append('.').append(f2).append("=A.").append(f1);
    		//query.where(w);    		
    	}
    	query.leftJoin(tname, joinCondtion.toString(), alias);
    	
    	if(!hasSelect)
    	{
    		hasSelect = true;
    		query.select("A.*");
    	}
    	
    	//query.table(tname,alias);
    	if(allFields)
    	{
    		for(Column c:m._columns)
    		{
    			query.select( aliasFieldName(alias,c.getName()) );
    		}
    	}
    	else
    	{
    		String named = m.representation;
    		if(named == null) named = m.id.getName();
    		query.select( aliasFieldName(alias,named) );
    	}
    	
    	if(joins == null)
    	{
    		joins = new ArrayList<Model>();
    	}
    	joins.add(m);
    	
    	return this;
    }
    
    private RecordSet _clone()
    {
    	RecordSet rs = new RecordSet();
    	
    	/*rs._order = this._order;
    	rs._select = this._select;
    	rs._where = this._where;
    	rs._limit = this._limit;
    	rs._offset = this._offset;
    	rs._vars = this._vars;*/
    	rs.query = this.query;
    	rs.model = this.model;
    	
    	return rs;
    }
	
	@Override
	public Iterator<Record> iterator()
	{
		if(data == null)
		{
			try
			{
			    execute();
			}
			catch(ModelException e)
			{
				return null;
			}
			isDone = true;
		}
		
		return data.iterator();
	}
	
	public int size()
	{
		if(data == null)
		{
			execute();
			isDone = true;
		}
		
		return data.size();
	}
	
	public List<Record> getList() throws ModelException
	{
		if(data == null)
		{
			execute();
			isDone = true;
		}
		
		return data;
	}
	
	public List<Record> find(String key,Object v) throws ModelException
	{
		List<Record> ret = new ArrayList<Record>();
		
		for(Record r:this)
		{
			Object val = r.get(key);
			if(v==val || (v!=null && v.equals(val)) )
			{
				ret.add(r);
			}
		}
		
		return ret;
	}
	
	public java.util.Set<Object> values(String columnName)  throws ModelException
	{
		//Column col = model.getColumn(columnName);
		
		java.util.Set<Object> ret = new java.util.HashSet<Object>();
		for(Record r:this)
		{
			ret.add(r.get(columnName));
		}
		
		return ret;
	}
	
	static Object sqlvalue(Object value)
	{
		if(value != null)
		{
			if(value instanceof Integer || value instanceof Double
			 || value instanceof Float || value instanceof Long
			 || value instanceof java.math.BigDecimal
			 || value instanceof java.math.BigInteger)
			{
				//pass
			}
			else if(value instanceof Boolean)
			{
				if( ((Boolean)value) )
				{
					value = "1";
				}
				else
				{
					value = "0";
				}
			}
			else if(value instanceof NamedValue)
			{
				value = ((NamedValue)value).getValue();
			}
			else
			{
				value = "'"+Convert.replaceAll(value.toString(), "'", "''")+ "'";
			}
		}
		
		return value;
	}
	
	/*private static Map<String,Object> _cloneVars(Map<String,Object> src)
	{
		if(src == null || src.size()<=0) return null;
		
		Map<String,Object> dst = new java.util.HashMap<String, Object>();
		
		for(Map.Entry<String,Object> item:src.entrySet())
		{
			dst.put(item.getKey(), sqlvalue(item.getValue()));
		}
		
		return dst;
	}*/
	
	public int count() throws ModelException
	{
		/*Query query = db.table(model.getName());
		query.select("count(1)");
		
		//where
    	if(this._where != null && this._where.length()>0)
    	{
    		query.where(this._where,this._vars);    		
    	}
    	
    	//order by
    	if(this._order != null && this._order.length()>0)
    	{
    		query.order(this._order);
    	}        
    	
    	if(this._limit > 0)
    	{
    		query.limit(this._limit);
    	}
    	
    	//offset
    	if(this._offset > 0)
    	{
    		query.limit(this._offset);
    	}*/		
		
		try 
		{
			return query.count();
		} 
		catch (SQLException e) 
		{
			throw new ModelException(model,e.getMessage());
		}
	}
	
    private void execute() throws ModelException
    {
    	//Query query = db.table(model.getName());    	    	
    	
    	//select
    	/*if(this._select != null && this._select.length()>0)
    	{
    		query.select(this._select);
    	}
    	
    	//where
    	if(this._where != null && this._where.length()>0)
    	{
    		query.where(this._where,this._vars);
    		if(_params != null)
    		{
    			for(int i=0;i<_params.size();i++)
    			{
    				query.param(i, _params.get(i));
    			}
    		}
    	}
    	
    	//order by
    	if(this._order != null && this._order.length()>0)
    	{
    		query.order(this._order);
    	}        
    	
    	if(this._limit > 0)
    	{
    		query.limit(this._limit);
    	}
    	
    	//offset
    	if(this._offset > 0)
    	{
    		query.offset(this._offset);
    	}*/
    	
    	//join?
    	
    	try
    	{
    	    data = query.query(new Model.RecordMapper(this));
    	}
    	catch(ModelException me)
    	{
    		throw me;
    	}
    	catch(Exception e)
    	{
    		throw new ModelException(model,e.getMessage());
    	}    	
    }

	@Override
	public Record get(int index)
	{
		if(data == null)
		{
			execute();
			isDone = true;
		}
		
		if(data!=null && index>=0 && index<data.size())
		{
			return data.get(index);
		}
		else
		{
			throw new ArrayIndexOutOfBoundsException();			
		}
	}
	
	public Model getModel(){return model;}
	
}

/*class JoinInfo
{
	public Model model;
	public String alias;
}*/
