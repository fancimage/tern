/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

import com.tern.db.Database;
import com.tern.db.RowMapper;
import com.tern.db.db;
import com.tern.util.config;
import com.tern.util.Convert;

/*
 * Data Model
 * */
public class Model 
{
	public static final int MODEL_COMMON = 0;  /*COMMON MODEL*/
	public static final int MODEL_CHILD = 1;
	public static final int MODEL_CHILD_ONE = 2;
	
	//db connection
	Database db;	
	
	String _name;
	String _fullname;
	String caption;
	
	Column[] _columns;
	Map<String,Column> _colmaps;
	Map<String,Relation> _relations;
	Column id;
	String representation;  //named columns
	int style; /*model style:commom,child,child(only have one)*/
	
	Column[] _keys;
		
	private static Map<String,Model> modelsCache = new java.util.HashMap<String, Model>();	
	
	private static ModelReaderFactory readerFactory = new DefaultModelReaderFactory();
	
	public static Model from(String name)
	{
		return from(name,null);
	}
	
	public static Model from(String name,Database db)
	{
		if(config.isDebug() || !modelsCache.containsKey(name))
		{
			Model m = new Model(name,db==null?Database.defaultDB():db);
			if(config.isDebug())
			{
				return m;
			}
			
			synchronized(modelsCache)
			{
				if(!modelsCache.containsKey(name))
				{
					modelsCache.put(name, m);
				}
			}
			
			return m;
		}
		else
		{
			Model m = modelsCache.get(name);
			if(db == null || db == m.db) return m;
			else
			{
				//clone self
				return m;
			}
		}		
	}
	
	public static void setModelReaderFactory(ModelReaderFactory f)
	{
		if(f != null)
		{
		    readerFactory = f;
		}
	}
	
	protected Model(String name)
    {
    	this(name,Database.defaultDB());
    }
	
	protected Model(String name,Database h)
    {
    	if(name == null || name.length()<=0)
    	{
    	    return;	
    	}
    	
    	this.db = h;
    	
    	_fullname = name.trim();
    	if(_fullname.length()<=0) return;
    	int i = _fullname.lastIndexOf('.');
    	if(i>=0) _name = _fullname.substring(i+1);
    	else _name = _fullname;
    	
    	//先尝试从yaml中获取schema
    	if(!readerFactory.createReader().read(this) )
    	{
    		//从数据库中获取schema
    		if(i < 0)
    		{
    			new DefaultModelReader().read(this);
    		}
    		else
    		{
    			throw new ModelException(this,"can not find model file.");
    		}
    	}
    	
    	if(caption==null || caption.length() <=0)
    	{
    		caption = name;
    	}
    }
	
	public Database getDb()
	{
		return db;
	}
	
    /*
    
    public Model(String name,Database db)
    {
    	this.db = db;    	
    	schema = from(name,this.db);
    }
    
    public Model(String name,String dbname)
    {    	
    	if(dbname == null || dbname.length()<=0)
    	{
    		this.db = Database.defaultDB();    		
    	}
    	else
    	{
    		this.db = Database.DB(dbname);
    		if(db == null)
    		{
    			throw new ModelException(name,"database("+dbname+") doest not exists.");
    		}
    	}
    	
    	schema = from(name,this.db);
    }*/
    
    private Model(){}   
    
    public String getName(){return _name;}
    public String getFullName(){return _fullname;}
    public String getCaption() {return caption;}
    
    public String toString(){return this.getCaption();}
    public String getRepr(){return this.representation;}
    
    private Map<String,Column> _readonly_maps = null;
    public Map<String,Column> getColumns()
    {
    	if(null == _readonly_maps && _colmaps!=null )
    	{
    		_readonly_maps = new ColumnsMap(this); 
    	}
    	return _readonly_maps;
    }
    
    public Column[] getColumnList(){return _columns;}
    public Column   getId(){return id;}
    public Column[] getKeys() {return this._keys;}
    public int getStyle(){return this.style;}
    
    public Column column(String name)
    {
    	String _name = (name==null?null:name.toLowerCase());
    	if(_colmaps!=null && _colmaps.containsKey(_name))
    	{
    		return _colmaps.get(_name);
    	}
    	else if(this.id != null && id.name.equalsIgnoreCase(_name))
    	{
    		return id;
    	}
    	
    	throw new ModelException(this, "no column named "+name );
    }
    
    public Relation relation(String name)
    {
    	if(null == _relations) return null;
    	return _relations.get(name);
    }
    
    public Relation relation(Column col)
    {
    	if(null == col || null == _relations) return null;
    	if(col.getBelongsTo() != null) return col.getBelongsTo();
    	else if(col.getType() == DataType.Having)
    	{
    		return _relations.get(col.extra1);
    	}
    	
    	return null;
    }
    
    /*DBCommand getCommand(String sql)
    {    	
    	if( null == _transaction )
    	{
    		return helper.createCommand(sql);
    	}
    	else
    	{
    		DBCommand cmd = _transaction.getCommand();
    		cmd.setCommand(sql);
    		return cmd;
    	}
    }
    
    void disposeCommand(DBCommand cmd)
    {
    	if(null == _transaction || cmd != _transaction.getCommand())
    	{
    		cmd.Dispose();
    	}
    }*/
    
    static class RecordMapper implements RowMapper<Record>
    {
    	Model m;
    	RecordSet set;
    	public RecordMapper(Model m)
    	{
    		this.m = m;
    	}
    	
    	public RecordMapper(RecordSet set)
    	{
    		this.m = set.model;
    		this.set = set;
    	}
    	
    	@Override
		public Record map(ResultSet rs, int rowNum) throws SQLException
		{			
			Record r = new Record();
			r._create_from_rs(m, rs,null);
			
			if(set != null)
			{
				r._rs = set;
				if(set.joins != null && set.joins.size() > 0)
				{
					char ch='B';
					r._relations = new java.util.HashMap<String, Object>();
					for(int i=0;i<set.joins.size();i++)
					{
						String alias = String.valueOf( (char)(ch+i) );
						Model model = set.joins.get(i);
						
						Record row = new Record();
						row._create_from_rs(model, rs,alias);
						
						String rname = null;
						if(m._relations != null)
						{
							for(Relation re:m._relations.values())
							{
								if(re.getRef() == model)
								{
									rname = re.getName();
									break;
								}
							}
						}						
						
						if(rname == null) rname = model.getFullName();
						r._relations.put(rname, row);
					}
				}
			}
			
			return r;
		}
    }
    
    /*是否是一个父表*/
    public boolean isParent()
    {
    	for(Column c:this._columns)
    	{
    		if(c.getType() == DataType.Having)
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    //按键
    public Record find(long id) throws ModelException
    {
    	if(this.id == null)
    	{
    		throw new ModelException(this,"no id column.");
    	}
    	
    	boolean hasBool = false;
    	boolean hasOther = true;
    	if(this.style == Model.MODEL_CHILD_ONE)
    	{
    		hasOther = false;
    		/*判断是否存在物理表?*/
    		for(Column c:this._columns)
    		{
    			if(c.type == DataType.ID)
    			{
    				continue;
    			}
    			else if(c.type == DataType.Bool)
    			{
    				hasBool = true;
    				if(hasOther) break;
    			}
    			else if(!c.isStyle(Column.COLUMN_SYS))
    			{
    				hasOther = true;
    				if(hasBool) break;
    			}
    		}
    	}
    	
    	Record r = null;
    	if(hasOther)
    	{
    		String sql = String.format("SELECT * FROM %s WHERE %s=%d", _name,this.id.name,id);    	
        	
        	try
        	{
        		r = db.sql(sql).queryOne(new RecordMapper(this));  
        	}
        	catch(SQLException e)
        	{
        		throw new ModelException(this,e.getMessage());
        	}
    	}
    	
    	if(hasBool)
    	{
    		/*read from tn_commomvals*/
    		final Map<String, Object> ret = new HashMap<String,Object>();
    		try 
    		{
				db.table("tn_commomvals").select("cid,bval")
				  .where("tid=? and pid=?" , Convert.parseLong(this._fullname) , id)
				  .query(new RowMapper<Object>(){

					@Override
					public Object map(ResultSet rs, int rowNum)
							throws SQLException 
					{
						ret.put("C"+rs.getLong("cid"), Convert.parseBool(rs.getInt("bval")));
						return null;
					}
					  
				  });
			} 
    		catch (SQLException e)
    		{
    			//throw new ModelException(this,e.getMessage());
    			return null;
			}
    		
    		if(ret.size()>0)
    		{
    			if(r == null)
    			{
    				ret.put("id", id);
    				r = new Record(this,ret,RecordState.Unchange);
    			}
    			else
    			{
    				for(String k:ret.keySet())
    				{
    					r.set(k, ret.get(k));
    				}
    				r.state = RecordState.Unchange;
    			}
    		}
    	}
    	    	
    	if(r != null) return r;
    	else throw new ModelException(this,"record(id="+id+") does not exists!");
    }
    
    public RecordSet find(long[] id) throws ModelException
    {
    	if(this.id == null)
    	{
    		throw new ModelException(this,"no id column.");
    	}
    	    	
    	String sql = String.format("SELECT * FROM %s WHERE %s in (%s)",
    			_name,this.id.name,Convert.join(",", id));
    	
    	try
    	{
    		List<Record> list = db.sql(sql).query(new RecordMapper(this));
    		return new RecordSet(list);
    	}
    	catch(SQLException e)
    	{
    		throw new ModelException(this,e.getMessage());
    	}    	    
    }

	public RecordSet sql(String sql,Object... params)
	{
		RecordSet rs = new RecordSet(this);

		rs.query = this.db.table("("+sql+")" , "A");
		rs.query.where(null,params);

		return rs;
	}
    
    //select('t1,t2').where('s=$s',vars).order('').limit(10)
    public RecordSet query(String where,Map<String,Object> vars)
    {
    	return new RecordSet(this,where,vars);
    }
    
    public RecordSet query(String where,Object[][] params)
    {
    	Map<String,Object> vars = null;
    	
    	if(params != null)
    	{
    	    vars = new java.util.HashMap<String, Object>();
    	    
    	    for(Object[] p:params)
    	    {
    	    	vars.put(p[0].toString(), RecordSet.sqlvalue(p[1]));
    	    }
    	}
    	
    	return new RecordSet(this,where,vars);
    }
    
    public RecordSet query(String where,Object... params)
    {
    	return new RecordSet(this,where, params ); 
    }
    
    public RecordSet query(String where)
    {
    	return new RecordSet(this,where,(Object[])null);
    }
    
    public RecordSet query(){return new RecordSet(this,null,(Object[])null);}
    
    public Record create() throws ModelException
    {
    	return this.create(null);
    }
    
    public Record create(Map<String,Object> vars) throws ModelException 
    {    	
    	return new Record(this,vars,RecordState.New);
    }
    
    public Record update(Map<String,Object> vars) throws ModelException
    {
    	if(vars == null)
    	{
    		throw new ValueException(this,"no values for update record.");
    	}
    	
    	//has invalide values?
    	if(this.id != null && !vars.containsKey(this.id.name) && !vars.containsKey("id"))
    	{
    		throw new ValueException(this,"no id value for update record.");
    	}
    	else
    	{
    		//keys
    		for(Column col:this._keys)
    		{
    			if(!vars.containsKey(col.name))
    			{
    				throw new ValueException(this,"no key values for update record.");
    			}
    		}
    	}
    	
    	return new Record(this,vars,RecordState.Update);
    }
    
    public void delete(long id) throws ModelException
    {
    	/*if(this.id == null)
    	{
    	    throw new ModelException(this,"no id column.");	
    	}
    	
    	String sql = String.format("DELETE FROM %s WHERE %s=%s", this.getName(),this.id.name,id);
    	execSQL(sql);*/
    	
    	delete(new long[]{id});
    }
    
    void execSQL(String sql) throws ModelException
    {		
		try
		{
			db.sql(sql).exec();
		}
		catch(Exception e)
		{
			throw new ModelException(this,e.getMessage()); 
		}		
    }
    
    public void delete(long[] ids) throws ModelException
    {
    	if(this.id == null)
    	{
    	    throw new ModelException(this,"no id column.");
    	}
    	
    	if(ids == null || ids.length<=0) return;
    	
    	String str_ids = Convert.join(",", ids);
    	
    	db.Transaction trans = null;
    	try
    	{
    		if(_relations!=null)
        	{
        		for(Relation rel:_relations.values())
        		{
        			if(rel.mode == Relation.HAVE
        			   || rel.mode == Relation.HAVE_ONE)
        			{
        				if(!db.inTransaction())
						{
						    trans = db.transaction();
						}
        				rel.deleteByParentIds(str_ids);
        			}
        		}
        	}
    		
    		String sql = String.format("DELETE FROM %s WHERE %s IN (%s)", 
        			this.getName(),this.id.name,str_ids);
        	execSQL(sql);
        	
        	if(trans !=null) trans.commit();
    	}
    	catch(ModelException e)
    	{
    		if(trans !=null) trans.rollback();
    		throw e;
    	}
    	catch(Throwable t)
    	{
    		if(trans !=null) trans.rollback();
    		throw new ModelException(this,t.getMessage()); 
    	}
    }
    
    public void delete(String[] ids) throws ModelException
    {
    	/*if(this.id == null)
    	{
    	    throw new ModelException(this,"no id column.");
    	}
    	
    	if(ids == null || ids.length<=0) return;
    	
    	String sql = String.format("DELETE FROM %s WHERE %s IN (%s)", 
    			this.getName(),this.id.name,Convert.join(",", ids));
    	execSQL(sql);*/
    	
    	if(ids == null || ids.length<=0) return;
    	long[] arr = new long[ids.length];
    	for(int i=0;i<ids.length;i++)
    	{
    		arr[i] = Convert.parseLong(ids[i]);
    	}
    	
    	delete(arr);
    }
    
    public void delete(String where) throws ModelException
    {
    	delete(where, (Map<String,Object>)null );
    }
    
    public void delete(String where,Map<String,Object> vars) throws ModelException
    {
    	if(where == null || where.length()<=0)
    	{
    		throw new ModelException(this,"no 'where' clause.");
    	}
    	
    	if(vars != null)
    	{
            Map<String,Object> dst = new java.util.HashMap<String, Object>();
		
		    for(Map.Entry<String,Object> item:vars.entrySet())
		    {
			    dst.put(item.getKey(), RecordSet.sqlvalue(item.getValue()));
		    }
		    
		    where = Convert.format(where, dst);
    	}
    	
    	execSQL(String.format("DELETE FROM %s WHERE %s", _name, where));    	
    }
        
    public void delete(String where,Object[][] params) throws ModelException
    {
    	if(where == null || where.length()<=0)
    	{
    		throw new ModelException(this,"no 'where' clause.");
    	}
    	
    	if(params != null && params.length > 0)
    	{
    		Map<String,Object> vars = new java.util.HashMap<String, Object>();
    	    
    	    for(Object[] p:params)
    	    {
    	    	vars.put(p[0].toString(), RecordSet.sqlvalue(p[1]));
    	    }
    	    
    	    where = Convert.format(where, vars);
    	}
    	
    	execSQL(String.format("DELETE FROM %s WHERE %s", _name, where));
    }    
    
    public void delete(String where,Object... params) throws ModelException
    {
    	if(where == null || where.length()<=0)
    	{
    		throw new ModelException(this,"no 'where' clause.");
    	}
    	
    	String sql = String.format("DELETE FROM %s WHERE %s", _name, where);
    	
    	try
		{
			db.sql(sql,params).exec();
		}
		catch(Exception e)
		{
			throw new ModelException(this,e.getMessage()); 
		}
    }
    
    public static String sqlvalue(Column col,Object v)
    {
    	if(v==null)
    	{
    		return "null";
    	}
    	else if(v instanceof NamedValue)
    	{
    		return ((NamedValue)v).getValue();
    	}
    	else if(col.type == DataType.String || col.type == DataType.Datetime || col.type == DataType.Text )
    	{
    		return "'"+Convert.replaceAll(v.toString(), "'", "''")+"'";
    	}
    	else if(col.type == DataType.Bool)
    	{
    		if( ((Boolean)v).booleanValue() )
    		{
    			return "1";
    		}
    		else
    		{
    			return "0";
    		}
    	}    	
    	else
    	{
    		return v.toString();
    	}
    }    		                        
}

class ColumnsMap implements Map<String,Column>,IRow
{
	Model model;
	
	public ColumnsMap(Model m)
	{
		this.model = m;
	}
	
	@Override
	public void clear() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) 
	{
		return key!=null && model.column(key.toString())!=null;
	}

	@Override
	public boolean containsValue(Object value)
	{
		return model._colmaps.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Column>> entrySet() 
	{
		return model._colmaps.entrySet();
	}

	@Override
	public Column get(Object key) 
	{
		Column c = null;
		try
		{
			c = model.column(key.toString());
		}
		catch(Exception e)
		{
			
		}
		return c;
	}

	@Override
	public Object get(int i)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() 
	{
		return model._colmaps.isEmpty();
	}

	@Override
	public Set<String> keySet()
	{
		return model._colmaps.keySet();
	}

	@Override
	public Column put(String arg0, Column arg1) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Column> arg0) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Column remove(Object arg0) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() 
	{
		return model._colmaps.size();
	}

	@Override
	public Collection<Column> values()
	{
		return model._colmaps.values();
	}

	@Override
	public Column get(String key)
	{
		return model.column(key);
	}

	@Override
	public IRow set(String key, Object val) 
	{
		throw new UnsupportedOperationException();
	}
}

class DefaultModelReader extends ModelReader
{
	@Override
	public boolean read(Model model) throws ModelException
	{
		this.model = model;
		List<Map<String,Object>> cols = null;
    	
    	try
    	{
    		cols = model.db.columns(model._name);
    	}
    	catch(Exception e)
    	{
    		throw new ModelException(model,String.format("Table(%s) does not exists.", model._name));
    	}
    	
    	List<Column> columns = null;
    	if(cols != null && cols.size() > 0)
    	{
    		columns = new ArrayList<Column>(cols.size());
    		for(Map<String,Object> m:cols)
    		{
    			columns.add( this.readColumn(m) );
    		}
    	}
    	
    	this.setColumns(columns);
    	
    	String repr = model.representation;
    	if(repr == null)
    	{
    		if(model._colmaps.containsKey("caption"))
    		{
    			repr = "caption";
    		}
    		else if(model._colmaps.containsKey("title"))
    		{
    			repr = "title";
    		}
    		else if(model._colmaps.containsKey("name"))
    		{
    			repr = "name";
    		}
    		else if(model._colmaps.containsKey("sname"))
    		{
    			repr = "sname";
    		}
    		
    		this.setRepresentation(repr);
    	}

    	return true;
	}
}
