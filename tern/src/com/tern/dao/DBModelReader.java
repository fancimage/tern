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
import java.util.List;
import java.util.Map;

import com.tern.db.DataRow;
import com.tern.db.Database;
import com.tern.db.RowMapper;
import com.tern.util.Convert;
import com.tern.util.Trace;

public class DBModelReader extends ModelReader
{
	protected Database schemaDB;
	
	public DBModelReader(Database db)
	{
		schemaDB = db;
	}
	
	protected Column readColumn(ResultSet rs) throws SQLException 
	{
		Column col = createColumn();
		
		col.name = rs.getString("cname");
		if(col.name!=null) col.name = col.name.trim();
		col.caption = rs.getString("ccaption");
		col.type = parseType(rs.getString("ctype"));
		
		//nullable
		int iv = rs.getInt("nullable");
		if(1 == iv) col.nullable = true;
		else col.nullable = false;
		
		iv = rs.getInt("iskey");
		if(1 == iv) col.iskey = true;
		else col.iskey = false;
		
		iv = rs.getInt("readonly");
		if(1 == iv) col.readonly = true;
		else col.readonly = false;
		
		parse_auto(col,rs.getObject("auto"));		
		col.default_val = rs.getString("cdefault");
		
		/*关联显示条件*/  //---在adtech的自定义表单系统中使用,暂时去掉
		/*long cid = rs.getLong("rcid");
		if(cid > 0)
		{
			String rcval = rs.getString("rcvalue");
			if(rcval == null) rcval = "";
			
			col.showCondtion = "C"+cid+"="+rcval;
		}*/
		
		switch(col.type)
    	{
    	case Numeric:
    	    {
    	    	//len
	        	col.maxLen = rs.getInt("clength");
	        	col.minLen = rs.getInt("scale");
	        	Object cv = rs.getObject("cmax");
	        	if(cv != null)
	        	{
	        		col.max = Convert.round(Convert.parseDouble(cv),col.getScale());
	        	}
	        	
	        	cv = rs.getObject("cmin");
	        	if(cv != null)
	        	{
	        		col.min = Convert.round(Convert.parseDouble(cv),col.getScale());
	        	}
    	    }
    	    break;
    	case String:
    	case Text:
    	    {
    	    	col.maxLen = rs.getInt("cmax");
    	    	col.minLen = rs.getInt("cmin");
    	    }
    	    break;
    	case Datetime:
    	    {
    	    	col.extra1 = rs.getString("format");
    	    }
    	    break;
    	case Enum:
    	    {
    	    	col.extra1 = rs.getString("cref");
    	    }
    	    break;
    	case Belongs:
    	    {
    	    	col.extra1 = rs.getString("cref");
    	    }
    	    break;
    	case Having:
    	    {
    	    	col.extra1 = rs.getString("cref");
    	    	col.minLen = rs.getInt("scale"); /*子表的类型:0[一对多 1[一对一]]*/
    	    }
    	   break;
    	case Binary:
    	    {
    	    	col.extra1 = rs.getString("cref");  /*存储content type的字段名*/
    	    	col.extraInt = rs.getInt("scale");/*存储方式*/
    	    }
    	    break;
    	default:
    		break;
    	}
		
		return col;
	}
	
	protected void readRelations()
	{
		for(Column col : model._columns)
		{
			if(col.getType() == DataType.Belongs 
				|| col.getType() == DataType.Having)
			{
				if(col.extra1==null) continue;
				Relation relation = new Relation(col.getName()+"$"+col.extra1.toString(),model);
				relation.ref = col.extra1.toString();
				relation.caption = col.getCaption();
				
				relation.map = new String[1][2];
				if(col.getType() == DataType.Belongs)
				{
					relation.mode = Relation.BELONGS;
					
					relation.map[0][0] = col.getName();
					relation.map[0][1] = "id";
					
					col.type = DataType.Numeric;
					col.minLen = 0;
					col.belongsTo = relation;
				}
				else
				{
					if(col.minLen == 0)
					{
					    relation.mode = Relation.HAVE;
					}
					else
					{
						relation.mode = Relation.HAVE_ONE;
					}
					
					relation.map[0][0] = "id";
					relation.map[0][1] = "taskid";
				}
				
				if(model._relations == null)
				{
					model._relations = new java.util.HashMap<String, Relation>();
				}
				model._relations.put(relation.getName().toLowerCase(), relation);
			}
		}
	}
	
	@Override
	public boolean read(Model m) throws ModelException
	{
		this.model = m;
					
		try 
		{
			//long eid = Convert.parseLong(m.getName());
			/*model table: iap_entities*/
			DataRow row = schemaDB.table("iap_entities")
			        .where("ename=?" , m.getName() )
			        .queryOne(true);
			if(row == null)
			{
				//throw new ModelException(model,"can not find model.");
				return false;
			}
			
			this.setCaption(row.getString("ecaption"));
			this.setRepresentation(row.getString("repr"));
			//this.setName(row.getString("ename"));
			
			String tname = row.getString("tablename");
			if(tname==null || tname.length()<=0) tname = m.getName();
			this.setName(tname);
			
			int etype = row.getInt("etype");
			if(etype == 1) m.style = Model.MODEL_CHILD;
			else if(etype == 2) m.style = Model.MODEL_CHILD_ONE;
			else m.style = Model.MODEL_COMMON;
			
			//columns
			List<Column> columns = schemaDB.table("iap_columns")
					.where("eid=?" , row.getLong("eid"))
					.order("csort")
					.query(new RowMapper<Column>(){

				@Override
				public Column map(ResultSet rs, int rowNum) throws SQLException 
				{
					return readColumn(rs);
				}
				
			});
			
			if(null == columns)
			{
				//throw new ModelException(model,"model has no columns.");
				return false;
			}
			
			this.setColumns(columns);
			
			if(this.model._columns!=null && this.model._columns.length>0)
			{
			    readRelations();
			}
			//relations
			/*for(DataRow r:schemaDB.table("iap_relations")
					              .where("eid=?" , eid)
					              .query())
			{
				Map rinfo = new java.util.HashMap();
				
				long rid = r.getLong("rid");
				rinfo.put("name",  String.valueOf(rid) );
				
				long ref = r.getLong("rref");
				
				rinfo.put("ref", ref);
				rinfo.put("caption", r.getString("rcaption"));
				rinfo.put("mode", r.getString("rmode"));
				
				final List maps = new java.util.ArrayList();
				//fields map
				schemaDB.sql("select a.rsrc,a.rdst,b.cname src,c.cname dst from iap_relation_map a,iap_columns b,iap_columns c where a.rid=? and b.eid=? and b.cid=a.rsrc and c.eid=? and c.cid=a.rdst",
						rid,eid,ref)
				        //.where("rid=?", rid)
				        .query(new RowMapper<Object>(){

							@Override
							public Object map(ResultSet rs, int rowNum)
									throws SQLException {
								List arr = new java.util.ArrayList(2);
								arr.add(rs.getString("src"));
								arr.add(rs.getString("dst"));
								maps.add(arr);
								return null;
							}
				        	
				        });
				
				rinfo.put("map", maps);
				this.addRelation(rinfo);
			}
			*/
		} 
		catch (SQLException e) 
		{
			Trace.write(Trace.Error, e, "DBModelReader");
			throw new ModelException(m,e.getMessage());
		};
		
		return true;
	}
}
