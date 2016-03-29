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

import com.tern.util.Convert;

public class Relation 
{
	public static final int BELONGS = 1;
	public static final int HAVE = 2;
	public static final int HAVE_ONE = 3;  /*表示一对一的关系*/
	
    String name;
    String caption;
    String ref;
    
    Model parent;
    private Model refModel;
    
    String[][] map;
    //String[] filter;
    
    int mode = BELONGS;
    
    Relation(String name,Model p)
    {
    	this.name = this.caption = name;
    	this.parent = p;
    }
    
    public String getName(){return name;}
    public String getCaption(){return caption;}
    public Model getParent(){return parent;}
        
    public Model getRef() throws ModelException
    {
    	if(refModel == null)
    	{
    		refModel = Model.from(ref,parent.db);
    	}
    	    	    	
    	return refModel;
    }
    
    public int getMode(){return mode;}
    public String[][] getMap(){return map;}
    //public String[] getFilter() {return filter;}
    
    public String toString(){return this.caption;}
    
    public String maped(String src)
    {
    	if(src == null || src.length()<=0) return null;
    	for(String[] m:this.map)
    	{
    		if(src.equals(m[0])) return m[1];
    	}
    	
    	return null;
    }
    
    /*public RecordSet queryAvailableParents(String src,Record child)
    {
    	return queryAvailableParents(src,child,null);
    }*/
    
    void deleteByParentIds(String ids) throws SQLException
    {
    	Model ref = this.getRef();
    	boolean hasOther = false;
    	boolean hasBool = false;
    	
    	if(mode == Relation.HAVE)
    	{
    		hasOther = true;
    	}
    	else if(mode == Relation.HAVE_ONE)
    	{
    		for(Column c:ref._columns)
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
    	
    	if(hasOther)
    	{
    		String src = map[0][0];
    		String dst = map[0][1];
    		if(src.equals("id"))
    		{
    			ref.delete(dst+" IN ("+ids+")");
    		}
    	}
    	
    	if(hasBool)
    	{
    		ref.db.delete("tn_commomvals")
    		  .where("tid=? and pid in ("+ids+")",Convert.parseLong(ref.getFullName()) )
    		  .exec();
    	}
    }
    
    void cascadeDelete(Record p) throws SQLException
    {
    	Model ref = this.getRef();
    	boolean hasOther = false;
    	boolean hasBool = false;
    	
    	if(mode == Relation.HAVE)
    	{
    		hasOther = true;
    	}
    	else if(mode == Relation.HAVE_ONE)
    	{
    		for(Column c:ref._columns)
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
    	
    	if(hasOther)
    	{
    		int c = 0;
    		StringBuffer buf = new StringBuffer();
    		for(String[] m:map)
    		{
    			if(c > 0) buf.append(" AND ");
    			buf.append(m[1]).append(" = ").append( p.orivalue(parent.column(m[0]), p.row.get(m[0]) ));
    			c++;
    		}
    		
    		ref.delete(buf.toString());
    	}
    	
    	if(hasBool)
    	{
    		ref.db.delete("tn_commomvals")
    		  .where("tid=? and pid=?",Convert.parseLong(ref.getFullName()),p.getId())
    		  .exec();
    	}
    }
    
    public RecordSet queryAvailableParents(String src,java.util.Map<String, Object> child/*,Model srcModel*/)
    {
    	if(mode != BELONGS)
    	{
    		throw new ModelException(parent,"Relation is not 'belongs',can not query parent data.");
    	}
    	
    	/*if(child instanceof Record)
    	{
    		srcModel = ((Record)child).getModel();
    	}*/
    	
    	if(src == null || src.length()<=0 || child==null || map.length<=1)
    	{
    		return this.getRef().query();
    	}
    	else
    	{
    		StringBuffer buf = new StringBuffer();
    		int c = 0;
    		for(String[] m:this.map)
    		{
    			if(src.equals(m[0])) continue;
    			
    			if(c > 0) buf.append(" AND ");
    			buf.append(m[1]).append(" = ").append(Model.sqlvalue(parent.column(m[0]),child.get(m[0])));
    			c++;
    		}
    		
    		return this.getRef().query(buf.toString());
    	}
    }
    /*public RecordSet queryAvailableParents(Record child)
    {
    	if(mode != BELONGS)
    	{
    		throw new ModelException(parent.getFullName(),"Relation is not 'belongs',can not query parent data.");
    	}
    	
    	if(this.filter == null || filter.length<=0)
    	{
    		return this.getRef().query();
    	}
    	else
    	{
    		StringBuffer buf = new StringBuffer();
    		for(String f : this.filter)
    		{
    			//Object v = child.get(f);
    			for(String[] m:this.map)
    			{
    				if(m[0].equals(f))
    				{
    					buf.append(m[1]).append(" = ").append(Model.sqlvalue(parent.getColumn(f),child.get(f)));
    					break;
    				}
    			}
    			
    			throw new ModelException(parent.getFullName(),"Filter("+f+" doest not in maps.)");
    		}
    		
    		return this.getRef().query(buf.toString());
    	}
    }*/
    
}
