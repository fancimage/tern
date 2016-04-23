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
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tern.db.Database;
import com.tern.db.DeleteCommand;
import com.tern.db.InsertCommand;
import com.tern.db.UpdateCommand;
import com.tern.db.db;
import com.tern.util.Convert;
import com.tern.util.TernContext;
import com.tern.util.Trace;

//表示一行数据
public class Record implements Map<String,Object>,IRow
{
	protected Model model;
	protected Map<String,Object> row;
	protected Map<String,Object> _keys;
	protected Map<String,Object> _relations;
	protected RecordState state = RecordState.Unchange;
	RecordSet _rs;
	
	static NumberFormat numFormat =  NumberFormat.getNumberInstance();
	
	StringBuffer errmsg;
		
	Record()
	{
	}
	
	Record(Model m,Map<String,Object> vars,RecordState state) throws ModelException
	{
		this.model = m;
		this.row = new java.util.HashMap<String, Object>();
		this.state = state;
				
		int errcount = 0;
		for(Column col:m.getColumnList())
    	{
			String key = null;
    		if(vars!=null)
    		{
    			if(vars.containsKey(col.name))
    			{
    				key = col.name;
    			}
    			else if(col.isId() && vars.containsKey("id"))
    			{
    				key = "id";
    			}
    		}
    		
    		if(key != null)
    		{
    			Object val = vars.get(key);
    			try
    			{
    				val = this._format(col, val);
    			}
    			catch(ModelException e)
    			{
    				errcount++;
    				this.state = RecordState.Error;
    				if(errcount <= 5)
    				{
    					if(errmsg == null) errmsg = new StringBuffer();
    					else errmsg.append("\n");
    					errmsg.append(e.getMessage());
    				}
    			} 
    			
    			row.put(col.name, val);
    		}
    		else if(state == RecordState.New && col.default_val != null) //set default value
    		{
    			Object val = this._format(col, col.default_val);
    			row.put(col.name, val);
    		}
    	}				
	}
	
	public Model getModel()
	{
		return model;
	}
	
	public RecordState getState(){return state;}	
	protected void setState(RecordState s) {state = s;}
	
	public String getErrorMessage()
	{
		return errmsg==null?null:errmsg.toString();
	}
	
	public long getId() throws ModelException
	{
		Object obj = this.get("id");
		return Convert.parseLong(obj);
	}
	
	public int getInt(String key)
	{
		return getInt(key,0);
	}
	
	public int getInt(String key,int def)
	{
		Object obj = get(key);
		if(null == obj) return def;
		else if(obj instanceof Integer) return (Integer)obj;
		else 
		{
		    try
		    {
		    	return Integer.parseInt(obj.toString());
		    }
		    catch(Throwable t)
		    {
		    	return def;
		    }
		}
	}
	
	public String getString(String key)
	{
		Object obj = get(key);
		if(null == obj) return null;
		else if(obj instanceof String) return (String)obj;
		else return obj.toString();
	}
	
	public double getDouble(String key)
	{
		return getDouble(key,0);
	}
	
	public double getDouble(String key,double def)
	{
		Object obj = get(key);
		if(null == obj) return def;
		else if(obj instanceof Double) return (Double)obj;
		else return Double.parseDouble(obj.toString());
	}
	
	public float getFloat(String key)
	{
		return getFloat(key,0);
	}
	
	public float getFloat(String key,float def)
	{
		Object obj = get(key);
		if(null == obj) return def;
		else if(obj instanceof Float) return (Float)obj;
		else return Float.parseFloat(obj.toString());
	}
	
	public long getLong(String key)
	{
		return getLong(key,0);
	}
	
	public long getLong(String key,long def)
	{
		Object obj = get(key);
		if(null == obj) return def;
		else if(obj instanceof Long) return (Long)obj;
		else return Long.parseLong(obj.toString());
	}
	
	public boolean getBoolean(String key)
	{
		return getBoolean(key,false);
	}
	
	public boolean getBoolean(String key,boolean def)
	{
		Object obj = get(key);
		if(null == obj) return def;
		else if(obj instanceof Boolean) return (Boolean)obj;
		else
		{
			String str = obj.toString().trim().toLowerCase();
			if(str.equals("true") || str.equals("1") || str.equals("t") 
					|| str.equals("yes") || str.equals("y"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	public java.util.Date getDate(String key)
	{
		Column col = model.column(key);
		if(col.type != DataType.Datetime)
		{
			throw new ValueException(model,"field type is not datetime.");
		}
		
		Object obj = get(key);
		if(obj == null) return null;
		else if(obj instanceof java.util.Date) return (java.util.Date)obj;
		else
		{
			try
			{
			    return col.getFormat().parse(obj.toString());
			}
			catch( java.text.ParseException e)
			{
				throw new ValueException(model,"parse datetime string failed.");
			}
		}
	}
	
	public Object get(String key)
	{
		if(key == null)
		{
			throw new ValueException(model,"field name is empty.");
		}
		
		Column col = null;
		String _key = key.toLowerCase();
		
		try
		{
		    col = model.column(_key);
		}
		catch(ModelException e)
		{
			//is relation?
			Object o = __get_relation(_key);
		    if(o != null) return o;
		    
			if(this._relations!=null 
				&& this._relations.containsKey(_key)) /*外键值确实为空*/
			{
				return null;
			}
			
			throw e;
		}
		
		if(row.containsKey(col.name))
		{
			Object v = row.get(col.name);
			
			if(col.type == DataType.Enum && v != null && !(v instanceof NamedValue) )
			{
				v = getEnumValue(this.model,v);
				row.put(col.name, v);
			}
			
			return v;
		}
		else
		{
			if(this.state == RecordState.New)
			{
				if(col.type == DataType.Bool) return false;
				else return null;
			}
			else
			{
			    throw new ValueException(model,"column("+key+") not retrived.");
			}
		}
	}	
	
	public Record set(String key,Object val)
	{
		Column col = model.column(key);
		if(col.readonly)
		{
			if(col.isId() && this.state == RecordState.New)
			{		
				//this.row.put(col.name, val);
				//return this;
			}
			else
			{
			    throw new ValueException(model,String.format("column(%s) can not be modified.",col.caption));
			}
		}
		
		if(val == null)
		{
			if(!col.nullable)
			{
				throw new ValueException(model,String.format("column(%s) can not be set none.",col.caption));
			}
		}
		else
		{
			val = this._format(col, val);
		}
		
		if(state == RecordState.Unchange)
		{
			state = RecordState.Update;					
		}
		
		//save old value of keys
		if(col.iskey && model.id == null && _keys==null)
		{
			_keys = new java.util.HashMap<String,Object>();
			for(Column c : model._keys)
			{
				_keys.put(c.name, this.row.get(c.name));
			}
		}
		
		this.row.put(col.name, val);
		return this;
	}
	
	private Object __get_relation(String key) throws ModelException
	{
		if(_relations!=null && _relations.containsKey(key))
		{
			return _relations.get(key);
		}
		else if(model._relations!=null && model._relations.containsKey(key))
		{
			if(this._relations == null) _relations = new java.util.HashMap<String, Object>();
			
			Relation rel = this.model._relations.get(key);
			if(Relation.BELONGS == rel.mode
				&& rel.map.length==1 && ( this._rs != null && this._rs.size()>1) )
			{
				//load all
				Column src = model.column(rel.map[0][0]);
				java.util.Set<Object> vals = _rs.values(src.name);
				if(vals==null || vals.size()<=0)
				{
					_relations.put(key, null);
					return null;
				}
				
				StringBuffer buf = new StringBuffer();
				for(Object v:vals)
				{
					if(null == v) continue;
					if(buf.length()>0) buf.append(",");
					buf.append(Model.sqlvalue(src,v));
				}
				
				if(buf.length()<=0)
				{
					_relations.put(key, null);
					return null;
				}
				
				buf.insert(0, src.name+" IN (");
				buf.append(")");
				
				RecordSet rs = rel.getRef().query(buf.toString());				
				String dst = rel.getRef().column(rel.map[0][1]).name;
				for(Record r:_rs)
				{
					Object v = r.get(src.name);					
					List<Record> pr = rs.find(dst, v);
					
					if(r._relations == null) r._relations = new java.util.HashMap<String, Object>();
					
					if(pr != null && pr.size() == 1)
					{
						r._relations.put(key, pr.get(0));
					}
					else
					{						
						r._relations.put(key, null);
					}
				}
				
				return _relations.get(key);
			}
			else
			{
				StringBuffer buf = new StringBuffer();
				for(String[] m:rel.map)
				{
					//String src = m[0].equals("id")?model.getColumn("id").getName():m[0];
					Column src = model.column(m[0]);
					Object v = this.row.get(src.name);
					if(null == v)
					{
						_relations.put(key, null);
						return null;
					}
					
					String dst = m[1].equals("id")?rel.getRef().column("id").getName():m[1];
					
					if(buf.length()>0) buf.append(" AND ");
					buf.append(dst).append("=").append(Model.sqlvalue(src,v));
				}
				
				RecordSet rs = rel.getRef().query(buf.toString());
				if(Relation.BELONGS == rel.mode)
				{
					rs.limit(1);  //only one record!!
					
					List<Record> list = rs.getList();
					if( list.size() >0 )
					{
						_relations.put(key, list.get(0));
						return list.get(0);
					}
					else
					{
						_relations.put(key, null);
						return null;
					}
				}
				else
				{
				    _relations.put(key, rs);
				}
				return rs;
			}
			
		}
		else
		{
			return null;
		}
	}
	
	private Object _format(Column col,Object val) throws ValueException
	{
		Object _val = val;
		if(val==null || val.toString().length()<=0)
		{
			if(col.nullable)
			{
				if(DataType.Numeric == col.type)
				{
					return null;
				}
				else if(DataType.Bool == col.type)
				{
					return false;
				}
				else
				{
				    return val;
				}
			}
		}
		
		if(DataType.Numeric == col.type || DataType.ID == col.type)
		{
			if(0 == col.getScale())  //Integer
			{
				if(!(val instanceof Integer) 
				 && !(val instanceof Long)
				 && !(val instanceof java.math.BigInteger))
				{
					//double v;
					Number n;
					
					try
					{
						//v = Double.parseDouble(val.toString());
						n = numFormat.parse(val.toString());
					}
					catch(Exception e)
					{
						throw new ValueException(model,
								String.format("column(%s):%s is not numeric.",col.caption,val));
					}
					
					//_val = (int)Convert.round(v, 0); //to integer
					_val = n.intValue();
				}
			}
			else
			{
				if((val instanceof Integer) || (val instanceof Long) || (val instanceof java.math.BigInteger))
				{
					//pass
				}
				else if( (val instanceof Double) || (val instanceof Float) )
				{
					_val = Convert.round( (Double)val , col.getScale());
				}
				else if(val instanceof java.math.BigDecimal)
				{
					java.math.BigDecimal bd = (java.math.BigDecimal)val;
					_val = bd.divide(new java.math.BigDecimal("1"), col.getScale(),
							java.math.BigDecimal.ROUND_HALF_UP);
				}
				else
				{
					//double v;
					Number n;
					
					try
					{
						//v = Double.parseDouble(val.toString());
						n = numFormat.parse(val.toString());
					}
					catch(Exception e)
					{
						throw new ValueException(model,
								String.format("column(%s):%s is not numeric.",col.caption,val));
					}
					
					//_val = Convert.round(v, col.getScale());
					_val = Convert.round(n.doubleValue(), col.getScale());
				}
			}
			
			if(col.getMin() != null || col.getMax() != null)
			{
				java.math.BigDecimal bd = new java.math.BigDecimal(_val.toString());
				
				if(col.getMin()!=null && bd.compareTo(new java.math.BigDecimal(col.getMin()))<0)
				{
					throw new ValueException(model,
							String.format("column(%s):value must > minimum(%s).",col.caption,col.getMin()) );
				}
				
				if(col.getMax()!=null && bd.compareTo(new java.math.BigDecimal(col.getMax()))>0)
				{
					throw new ValueException(model,
							String.format("column(%s):value must < maximum(%s).",col.caption,col.getMax()) );
				}
			}
		}
		else if(DataType.String == col.type)
		{
			if(!(val instanceof String))
			{
				_val = val.toString();
			}
			
			if(col.getMinLength()>0 && ((String)_val).length() < col.getMinLength() )
			{
				throw new ValueException(model,
						String.format("column(%s):length must >= minimum(%d).",col.caption,col.getMinLength()) );
			}
			
			if(col.getLength()>0 && ((String)_val).length() > col.getLength() )
			{
				throw new ValueException(model,
						String.format("column(%s):length must <= maximum(%d).",col.caption,col.getLength()) );
			}
		}
		else if(DataType.Datetime == col.type)
		{
		    if(val instanceof java.util.Date)
		    {
		    	//pass
		    }
		    else if(val instanceof java.util.Calendar)
		    {
		    	_val = ((java.util.Calendar)val).getTime();
		    }
		    else if(val instanceof Long)
		    {
		    	_val = new java.util.Date( ((Long)val).longValue() );
		    }
		    else
		    {
		    	try
		    	{
		    	    _val = col.getFormat().parse(val.toString());
		    	}
		    	catch(java.text.ParseException e)
		    	{
		    		throw new ValueException(model,
		    				String.format("column(%s):illegal date value.",col.caption));
		    	}
		    }
		    
		    _val = col.getFormat().format((java.util.Date)_val);
		}
		else if(DataType.Bool == col.type)
		{
			if(!(val instanceof Boolean))
			{
				String t = val.toString().toLowerCase();
				if(t.equals("false") || t.equals("f") || t.equals("0") || t.equals("n"))
				{
					_val = false;
				}
				else if(t.equals("true") || t.equals("t") || t.equals("1") || t.equals("y"))
				{
					_val = true;
				}
				else
				{
					throw new ValueException(model,
		    				String.format("column(%s):need boolean value.",col.caption));
				}
			}
		}
		
		return _val;
	}
	
	public String toString()
	{
		if(model.representation != null)
		{
			if(model._colmaps.containsKey(model.representation.toLowerCase()))
			{
				return Convert.toString(this.get(model.representation));
			}
			else
			{
				com.tern.util.Expression expr = new com.tern.util.Expression(model.representation);
				Map<String,Object> vars = expr.getVariables();
				if(vars != null && vars.size()>0)
				{
					for(String key:vars.keySet())
					{
						expr.setVariableValue(key, this.get(key));
					}
				}
				
				try
				{
				    return Convert.toString(expr.calculate());
				}
				catch(Exception e)
				{
					throw new ModelException(model,"error repr=>"+e.getMessage());
				}
			}
		}			
		else return String.valueOf(this.getId());
	}
	
	Object orivalue(Column col,Object v)
	{
		if(v instanceof NamedValue)
    	{
    		return ((NamedValue)v).getValue();
    	}
    	else if(col.type == DataType.Datetime)
    	{
    		if(v instanceof java.util.Date)
		    {
    			return v;
		    }
		    else if(v instanceof java.util.Calendar)
		    {
		    	return ((java.util.Calendar)v).getTime();
		    }
		    else
		    {
		    	if(v == null || v.toString().length()<=0)
		    	{
		    		if(col.nullable) return null;
		    	}
		    	
		    	try
		    	{
		    	    return col.getFormat().parse(v.toString());
		    	}
		    	catch(java.text.ParseException e)
		    	{
		    		throw new ValueException(model,
		    				String.format("column(%s):illegal date value.",col.caption));
		    	}
		    }
    	}  
    	else
    	{
    		return v;
    	}
	}
	
	public void save() throws ModelException
	{
		if(RecordState.Unchange == state || RecordState.Delete == state)
		{
			return;
		}
		else if(RecordState.Error == state)
		{
			throw new ValueException(model,this.errmsg.toString());
		}
		else if(RecordState.Update == state)
		{
			db.Transaction trans = null;
			boolean flag = (model.style == Model.MODEL_CHILD_ONE);
			UpdateCommand cmd = model.db.update(model.getName());
			
			//StringBuffer buf = new StringBuffer("UPDATE ").append(model.getName()).append(" SET ");			
			int _count = 0;
			for(java.util.Map.Entry<String,Object> item:this.row.entrySet())
			{
				Column col = model.column(item.getKey());
				if(col.isId()) continue;
				Object v = item.getValue();
				
				//key has changed?
				if(col.iskey && this._keys!=null && v.equals(this._keys.get(col.name)))
				{
					continue;
				}						
				
				//if(_count >0) buf.append(",");								
			    //buf.append(item.getKey()).append("=").append(Model.sqlvalue(col, v));   
			    
				if(flag && col.type == DataType.Bool)
				{
					boolean bv = Convert.parseBool(v);
					try 
					{
						long tid = Convert.parseLong(model.getFullName());
						long cid = Convert.parseLong(col.getName().substring(1));
						if(!model.db.inTransaction())
						{
						    trans = model.db.transaction();
						}
						
						int fc = model.db.update("tn_commomvals")
								.set("bval", bv?1:0)
								.where("tid=? and pid=? and cid=?",tid,getId(),cid)
						        .exec();
						if(fc <=0)
						{
							/*record does not exits,to insert*/
							model.db.insert("tn_commomvals")
					             .set("tid",  tid )
					             .set("pid", getId())
					             .set("cid", cid )
					             .set("bval", bv?1:0)
					             .exec();
						}
					} 
					catch (SQLException e) 
					{
						if(trans!=null) trans.rollback();
						throw new ModelException(this.model,e.getMessage());
					}
				}
				else
				{
			        cmd.set(item.getKey(), orivalue(col,v));
			        if(!flag || !col.isStyle(Column.COLUMN_SYS)) _count++;
				}
			}
			
			if(_count <= 0)  //no value has been changed!!
			{
				if(flag && trans != null)
				{
					trans.commit();
				}
				this._keys = null;
				return;
			}
			
			//where
			//buf.append(" WHERE ");
			if(model.id != null)
			{
				//buf.append(model.id.name).append("=").append(this.getId());
				cmd.where(model.id.name+"=?",this.getId());
			}
			else
			{
				_count = 0;
				for(Column c:model._keys)
				{
					Object v ;
					if(this._keys!=null) v = _keys.get(c.name);
					else v = this.row.get(c.name);
					
					//if(_count>0) buf.append(" AND ");
					//buf.append(c.name).append("=").append(Model.sqlvalue(c, v));
					
					cmd.where(c.name+"=?",orivalue(c,v));
					_count++;
				}
			}
			
			//model.execSQL(buf.toString());
			
			try
			{
				cmd.exec();
				if(trans != null) trans.commit();
			} 
			catch (SQLException e) 
			{
				if(trans != null) trans.rollback();
				throw new ModelException(this.model,e.getMessage());
			}
			this._keys = null;
		}
		else  //insert
		{
			InsertCommand cmd = model.db.insert(model.getName());
			
			Column autoCol = null;
			long autoID = 0;
			db.Transaction trans = null;
			boolean flag = (model.style == Model.MODEL_CHILD_ONE); //insert boolean values to table 'tn_commomvals'
			if(flag)
			{
				autoID = Convert.parseLong(this.row.get(model.id.name));
				if(autoID <=0 )
				{
					throw new ValueException(model,
		    				String.format("child model has no id value."));
				}
			}
			
			int _count = 0;
			for(java.util.Map.Entry<String,Object> item:this.row.entrySet())
			{
				Column col = model.column(item.getKey());
				if(col.auto)
				{
					autoCol = col;
					continue;
				}
				if(col.isId()) continue;
				
				if(flag && col.type == DataType.Bool)
				{
					boolean v = Convert.parseBool(item.getValue());
					try 
					{
						if(!model.db.inTransaction())
						{
						    trans = model.db.transaction();
						}
						
						model.db.insert("tn_commomvals")
						        .set("tid",  Convert.parseLong(model.getFullName()) )
						        .set("pid", autoID)
						        .set("cid", Convert.parseLong(col.getName().substring(1)) )
						        .set("bval", v?1:0)
						        .exec();
					} 
					catch (SQLException e) 
					{
						if(trans!=null) trans.rollback();
						throw new ModelException(this.model,e.getMessage());
					}
				}
				else
				{
					cmd.set(col.name, orivalue(col,item.getValue()));
					if(!flag || !col.isStyle(Column.COLUMN_SYS)) _count++;  /*一些系统自己增加的列(如HECD中的taskid、userid等)不做计数*/
				}
			}
			
			if(_count <= 0 && flag)
			{
				if(trans!=null) trans.commit();
				setState(RecordState.Unchange);
				return;
			}
			
			try
			{
				if(model.id!=null && !model.id.auto)
				{			
					autoID = Convert.parseLong(this.row.get(model.id.name));
					if(autoID <= 0)  //forced id??
					{
						String sql = String.format("SELECT MAX(%s) FROM %s", model.id.name,model.getName());
						
						if(!model.db.inTransaction())
						{
						    trans = model.db.transaction();
						}
						
						long idvalue = model.db.sql(sql).queryLong();
						idvalue++;
						autoID = idvalue;
					}
					
					cmd.set(model.id.name, autoID);
					_count++;
				}
				
				cmd.exec();
				
				if(model.id!=null && model.id.auto) autoCol = model.id;
				
				if(autoCol != null)
				{
					autoID = model.db.lastID(model.getName());
					this.row.put(autoCol.name, autoID);
				}
				else if(autoID > 0)
				{
					this.row.put(model.id.name, autoID);
				}
				
				if(trans!=null) trans.commit();
			}
			catch(Exception e)
			{
				if(trans!=null) trans.rollback();
				throw new ModelException(model,e.getMessage()); 
			}			
			
		}
				
		setState(RecordState.Unchange);
	}
	
	public void delete() throws ModelException
	{
		if(RecordState.New == state || RecordState.Delete == state)
		{
			return;
		}
		else if(RecordState.Error == state)
		{
			throw new ValueException(model,this.errmsg.toString());
		}
		
		//String sql;
		DeleteCommand cmd = model.db.delete(model.getName());
		if(model.id!=null)
		{
			//sql = String.format("DELETE FROM %s WHERE %s=%s", model.getName(),model.id.name,this.getId());
			cmd.where(model.id.name+"=?",this.getId());
		}
		else
		{
			//StringBuffer buf = new StringBuffer("DELETE FROM ").append(model.getName()).append(" WHERE ");
			//int _count = 0;
			for(Column c:model._keys)
			{
				Object v ;
				if(this._keys!=null) v = _keys.get(c.name);
				else v = this.row.get(c.name);
				
				//if(_count>0) buf.append(" AND ");
				
				//buf.append(c.name).append("=").append(Model.sqlvalue(c, v));
				//_count++;
				cmd.where(c.name+"=?",orivalue(c,v));
			}
			
			//sql = buf.toString();
		}
		
		//model.execSQL(sql);
		db.Transaction trans = null;
		try 
		{
			if(model.style == Model.MODEL_COMMON)
			{
				/*同时删除子表的数据*/
				for(Relation rel:model._relations.values())
				{
					if(rel.mode == Relation.HAVE
					   || rel.mode == Relation.HAVE_ONE )
					{
						if(!model.db.inTransaction())
						{
						    trans = model.db.transaction();
						}
						
						rel.cascadeDelete(this);
					}
				}
			}
			
			cmd.exec();
			
			if(trans !=null) trans.commit();
		} 
		catch (SQLException e) 
		{
			if(trans !=null) trans.rollback();
			throw new ModelException(model,e.getMessage());
		}
		
		this._keys = null;
		setState(RecordState.Delete);
	}
	
	//得到父表数据
	public Record parent(String rname) throws ModelException
	{
		String _name = rname.toLowerCase();
		Relation r = model._relations.get(_name);
		if(r==null || r.mode != Relation.BELONGS)
		{
			throw new ModelException(model,"Relation("+rname+") does not exists or is not 'BELONGS' mode.");
		}
		
		return (Record)__get_relation(_name);
	}
	
	//得到从表数据
	public RecordSet children(String rname) throws ModelException
	{
		String _name = rname.toLowerCase();
		Relation r = model._relations.get(_name);
		if(r==null || r.mode != Relation.HAVE)
		{
			throw new ModelException(model,"Relation("+rname+") does not exists or is not 'HAVE' mode.");
		}
		
		return (RecordSet)__get_relation(_name);
	}
	
	void _create_from_rs(Model m,java.sql.ResultSet rs,String prefix) throws ValueException
	{
		this.model = m;
		this.row = new java.util.HashMap<String,Object>();
		
		String format = "%s_%s_";
		for(Column c:m._columns)
		{
			Object val = null;
			String name = (prefix==null?c.name:String.format(format, prefix,c.name));
			try
			{
				val = rs.getObject(name);
			}
			catch(java.sql.SQLException e)
			{
				continue;
			}
			
			if(val != null)
			{
				val = this._format(c, val);
			}
			
			row.put(c.name, val);
		}
		
		//has relations?
		
	}

	private static String enumSql = "select ecaption from tn_enums where eid=?";
	//private static java.util.HashMap<Integer, String> enumMap=new java.util.HashMap<Integer, String>();
	public static NamedValue getEnumValue(Model m,Object v)
	{
		int enumID = Convert.parseInt(v);
		
		//枚举值缓存中是否有此值
		java.util.HashMap<Integer, String> enumMap = TernContext.current().getEnumCache();
		String ret = enumMap.get(enumID);
		if(ret != null)
		{
			NamedValue nv=new NamedValue();
			nv.value = v.toString();
			nv.name = ret;
			return nv;
		}
		
		Database metadb = TernContext.current().getMetaDB();
		if(null == metadb)
		{
			metadb = m.db;
		}
		
		try
		{
		    ret = metadb.sql(enumSql,enumID).queryString();
		}
		catch(Exception e)
		{
			Trace.write(Trace.Error,e, "getEnumValue:");
			return null;
		}
		
		synchronized(enumMap)
		{
			if(!enumMap.containsKey(enumID))
			{
				enumMap.put(enumID, ret);
			}
		}
		NamedValue nv=new NamedValue();
		nv.value = v.toString();
		nv.name = ret;
		return nv;
	}
	
	/*to implements Map interface*/
	@Override
	public void clear()
	{		
		throw new UnsupportedOperationException();
	}
		
	@Override
	public boolean containsKey(Object key) 
	{
		if(key == null) return false;
		
		Column col = this.model.column(key.toString());
		return this.row.containsKey(col.name);
	}
		
	@Override
	public boolean containsValue(Object value)
	{
		return this.row.containsValue(value);
	}

	@Override
	public Set<Entry<String,Object>> entrySet() 
	{	
		return this.row.entrySet();
	}
	
	@Override
	public boolean isEmpty() 
	{	
		return this.row == null || this.row.isEmpty();
	}
	
	@Override
	public Set<String> keySet()
	{
		return row.keySet();
	}
		
	public Object put(String key, Object value) 
	{
		this.set(key, value);
		return this.get(key);
	}	
	
	@Override
	public Object remove(Object key)
	{	
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int size() 
	{	
		return row.size();
	}

	@Override
	public Collection<Object> values() 
	{	
		return row.values();
	}
		
	@Override
	public Object get(Object key) 
	{
		return this.get(key.toString());
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m)
	{
		if(m != null)
	    {
	    	for(Entry<? extends String, ? extends Object> item:m.entrySet() )
	    	{
	    		this.set(item.getKey(), item.getValue());
	    	}
	    }	
	}
}
