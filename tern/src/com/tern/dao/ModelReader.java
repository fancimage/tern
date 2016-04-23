/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.tern.util.Convert;
import com.tern.util.TernContext;
import com.tern.util.config;

public abstract class ModelReader 
{
	static java.text.SimpleDateFormat SDF1 = new java.text.SimpleDateFormat("yyyy-MM-dd");
	static java.text.SimpleDateFormat SDF2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static java.text.SimpleDateFormat SDF3 = new java.text.SimpleDateFormat("HH:mm:ss");
	
	protected Model model;	
	public abstract boolean read(Model model) throws ModelException;
	
    protected void setCaption(String caption)
    {
    	if(caption == null) model.caption = model._name;
    	else
    	{
    		caption = caption.trim();
    		if(caption.length() <= 0) model.caption = model._name;
    		else model.caption = caption;	
    	}    	
    }
    
    protected void setRepresentation(String repr)
    {
    	if(repr!=null && repr.length()>0)
    	{
    		model.representation = repr;
    	}
    	else
    	{
    		model.representation = null;
    	}
    }
    
    protected void setName(String name)
    {
    	if(name != null && name.length()>0)
    	{
    		model._name = name;
    	}
    }
    
    protected DataType parseType(String str)
    {
    	try
		{
		    return DataType.valueOf(Convert.capitalize(str));
		}
		catch(Exception e)
		{
			if(str.equalsIgnoreCase("id")) return DataType.ID;
			else throw new ModelException(model,String.format("unknown data type:%s.", str));
		}
    }
    
    protected void check_column(Column col) throws ModelException
    {
    	if(col.name.length()<=0)
    	{
    		throw new ModelException(model,"column must have 'name' attribute.");
    	}
    	
    	String colName = col.name.toLowerCase();
    	if(model._colmaps.containsKey(colName))
    	{
    		throw new ModelException(model,String.format("column(%s) already exists.", col.name));
    	}    	
    	
    	switch(col.type)
    	{    	
    	    case Datetime:
    	        {
    	        	String _format = Convert.toStringIgnoreEmpty(col.extra1,"yyyy-MM-dd HH:mm:ss");
    	        	if(_format.equals("yyyy-MM-dd"))
    	        	{
    	        		col.extra1 = SDF1;
    	        	}
    	        	else if(_format.equals("yyyy-MM-dd HH:mm:ss"))
    	        	{
    	        		col.extra1 = SDF2;
    	        	}
    	        	else if(_format.equals("HH:mm:ss"))
    	        	{
    	        		col.extra1 = SDF3;
    	        	}
    	        	else
    	        	{
    	        		try
    	        		{
    	        		    col.extra1 = new java.text.SimpleDateFormat(_format);
    	        		}
    	        		catch(Exception e)
    	        		{
    	        			throw new ModelException(model,
    	        					String.format("column(%s):wrong datatime format.",
    	        					col.name));
    	        		}
    	        	}
    	        	//col.extra1 = Convert.toStringIgnoreEmpty(c.get("format"),"yyyy-MM-dd HH:mm:ss");
    	        }
    	        break;
    	    case Enum:
    	        {    	  
    	        	col.extra1 = Convert.toStringIgnoreEmpty(col.extra1,col.name);
    	        }
    	        break;
    	    default:
    		    break;  
    	};
    	
    	//default
    	if(col.default_val!=null && col.default_val.length()<=0) col.default_val = null;
    	
    	if(col.isId())
    	{
    		model.id = col;
    		col.readonly = true;
    		model._colmaps.put("id", col);
    	}
    	else
    	{
    		model._colmaps.put(colName, col);
    	}    	
    }
    
    protected void setColumns(List<Column> columns) throws ModelException
    {
    	if(columns == null || columns.size() <= 0)
    	{
    		throw new ModelException(model,"model has no columns.");
    	}
    	
    	Column[] cols = new Column[columns.size()];
    	int _index = 0;
    	model._colmaps = new java.util.HashMap<String,Column>();
    	List<Column> _tmpkeys = new java.util.ArrayList<Column>();
    	
    	for(Column col : columns)
    	{
    		this.check_column(col);
    		cols[_index] = col;			
			if(col.isKey()) _tmpkeys.add(col);			    		
    		
    		_index++;
    	}
    	
    	if(cols.length<=0)
    	{
    		throw new ModelException(model,"model has no columns.");
    	}
    	
    	model._columns = cols;    	
    	
    	if(model.id == null)
    	{
    		if(_tmpkeys.size()<=0)
    		{
    			throw new ModelException(model,"model has no key(or id) columns.");
    		}
    		
    		if(1 == _tmpkeys.size() && _tmpkeys.get(0).getType() == DataType.Numeric)
    		{
    			model.id = _tmpkeys.get(0);
    			model.id.type = DataType.ID;
    			model.id.readonly = true;
    			model._colmaps.put("id", model.id);
    			model._colmaps.remove(model.id.name);
    			_tmpkeys.remove(0);
    		}
    	}   
    	
    	model._keys = new Column[_tmpkeys.size()];
    	model._keys = _tmpkeys.toArray(model._keys);
    }
    
    protected Relation addRelation(Map r) throws ModelException
    {
    	String name = Convert.toString(r.get("name")).trim();
    	if(name.length()<=0)
    	{
    		throw new ModelException(model,"relation must have 'name' attribute.");
    	}
    	
    	if(null == model._relations)
    	{
    		model._relations = new java.util.HashMap<String, Relation>();
    	}
    	else if(model._relations.containsKey(name.toLowerCase()))
    	{
    		throw new ModelException(model,"relation("+name+") is duplicate.");
    	}
    	
    	Relation relation = new Relation(name,model);
    	
    	//ref
    	String ref = Convert.toString(r.get("ref")).trim();
    	if(ref.length()>0)
    	{
    		relation.ref = ref;
    	}
    	else
    	{
    		relation.ref = Convert.singular(name);  //physical table name is singular
    	}
    	
    	//caption
    	relation.caption = Convert.toStringIgnoreEmpty(r.get("caption"), name);
    	
    	//mode
    	String mode = Convert.toString(r.get("mode"));
    	if(mode.equalsIgnoreCase("have"))
    	{
    		relation.mode = Relation.HAVE;
    	}
    	else
    	{
    		relation.mode = Relation.BELONGS;
    	}
    	
    	//map
    	Object obj = r.get("map");
    	List maps = null;
    	if(obj == null){}
    	else if(!(obj instanceof List))
    	{
    		throw new ModelException(model,"relation's map atrribute should be list.");
    	}
    	else
    	{
    		maps = (List)obj;
    	}
    	
    	List<String[]> _maps = new java.util.ArrayList<String[]>();
    	if(maps!=null && maps.size()>0)
    	{
    		//throw new ModelException(this._fullname,"relation must have map atrribute.");
    		if(!(maps.get(0) instanceof List) && maps.size() == 1)
    		{
    			List tmp = new java.util.ArrayList();
    			tmp.add(maps);
    			maps = tmp;
    		}
    		
    		for(Object o:maps)
    		{
    			if(!(o instanceof List))
    			{
    				throw new ModelException(model,"each group in relation map should be list.");
    			}
    			
    			List item = (List)o;
    			if(item.size() != 2)
    			{
    				throw new ModelException(model,"each group in relation map should have 2 fields.");
    			}
    			
    			//does source field exists?
    			Column _col = null;
    			String _src = Convert.toString(item.get(0));
    			String _dst = Convert.toString(item.get(1));
    			
    			try
    			{
    				_col = model.column(_src);
    			}
    			catch(ModelException e)
    			{
    				throw new ModelException(model,"field("+_src+") for group in relation map does not exists.");
    			}
    			
    			if(relation.mode == Relation.BELONGS)
    			{
    				if(_col.belongsTo != null)
    				{
    					throw new ModelException(model,"field("+_col.name+") already belongs to other relation("+_col.belongsTo.name+").");
    				}
    				_col.belongsTo = relation;
    			}
    			
    			_maps.add(new String[]{_src,_dst});
    		}
    	}
    	
    	if(_maps.size() <= 0)
    	{
    		if(relation.mode == Relation.BELONGS)
    		{
    			String _src = Convert.singular(relation.ref)+"_id";  //??need to singular 
    			Column _col = null;
    			
    			try
    			{
    				_col = model.column(_src);
    			}
    			catch(ModelException e)
    			{
    				throw new ModelException(model,"field("+_src+") for group in relation map does not exists.");
    			}
    			
    			if(_col.belongsTo != null)
				{
					throw new ModelException(model,"field("+_col.name+") already belongs to other relation("+_col.belongsTo.name+").");
				}
    			
				_col.belongsTo = relation;				
				_maps.add(new String[]{_src,"id"});
    		}
    		else
    		{
    			if(model.id == null)
    			{
    				throw new ModelException(model,"no map attribue in relation while model has no id column.");
    			}
    			
    			_maps.add(new String[]{model.id.name,Convert.singular(model._name)+"_id"});
    		}
    	}   
    	
    	relation.map = new String[_maps.size()][];
    	relation.map = _maps.toArray(relation.map);
    	
    	//add to model
    	model._relations.put(relation.getName().toLowerCase(), relation);    	
    	return relation;
    }
    
    protected void parse_auto(Column col,Object ov)
    {
    	if(ov == null)
		{
			if(col.type == DataType.ID) col.auto = true;
			else col.auto = false;
		}
		else
		{
			col.auto = Convert.toBoolean(ov,false);
		}
    }
    
    protected Column createColumn()
    {
    	return new Column();
    }
    
    protected Column readColumn(Map c)
	{
		Column col = createColumn();
    	col.name = Convert.toString(c.get("name"));    	
    	
    	//caption
    	col.caption = Convert.toStringIgnoreEmpty(c.get("caption"), col.name);
    	 
    	//type
    	col.type = parseType(Convert.toString(c.get("type")));    	
    	
    	//nullable
    	col.nullable = Convert.toBoolean(c.get("nullable"),true);
    	
    	parse_auto(col,c.get("auto"));    	
    	
    	switch(col.type)
    	{
    	    case Numeric:
    	        {    	
    	        	//len
    	        	col.maxLen = Convert.parseInt(c.get("length"));
    	        	//scale
    	        	col.minLen = Convert.parseInt(c.get("scale"));
    	        	
    	        	//max
    	        	if(c.containsKey("max"))
    	        	{
    	        		col.max = Convert.round(Double.parseDouble(c.get("max").toString()), col.getScale());    	        		
    	        	}
    	        	
    	        	//min
    	        	if(c.containsKey("min"))
    	        	{
    	        		col.min = Convert.round(Double.parseDouble(c.get("min").toString()), col.getScale());    	        		
    	        	}
    	        }
    	        break;
    	    case String:
    	        {
    	    	    col.maxLen = Convert.parseInt(c.get("max"),-1);
    	    	    col.minLen = Convert.parseInt(c.get("min"),-1);
    	        }
    	        break;
    	    case Datetime:
    	        {
    	        	col.extra1 = Convert.toStringIgnoreEmpty(c.get("format"),"yyyy-MM-dd HH:mm:ss");
    	        }
    	        break;
    	    case Enum:
    	        {    	  
    	        	col.extra1 = Convert.toStringIgnoreEmpty(c.get("ref"),col.name);
    	        }
    	        break;
    	    default:
    		    break;  
    	};
    	
    	//default
    	col.default_val = Convert.toStringIgnoreEmpty(c.get("default"),"");
    	
    	//iskey
    	col.iskey = Convert.toBoolean(c.get("key"),false);
    	
    	return col;
	}
}
