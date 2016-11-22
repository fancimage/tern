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
import java.util.Map;

import com.tern.util.config;
import com.tern.util.Convert;

class SqlServerDB extends Database
{
	public DBType getDbType()
	{
		return DBType.mssql;
	}
	
	protected void initDataSource(Map<String,Object> props) throws SQLException
	{
		if(!props.containsKey("driver"))
		{
		    props.put("driver", "net.sourceforge.jtds.jdbc.Driver");
		}
		
		String dbname = Convert.toString(props.get("db"));
		if(dbname==null || dbname.length()<=0)
		{
			throw new SQLException("no db attribue.");
		}
		
		String url = String.format("jdbc:jtds:sqlserver://%s:%d/%s;tds=8.0;charset=%s", 
				               Convert.toStringIgnoreEmpty(props.get("ip"), "127.0.0.1"),
				               Convert.parseInt(props.get("port"),1433),
				               dbname,
				               Convert.toStringIgnoreEmpty(props.get("encoding"), config.getEncoding())
				            );
		props.put("url",url);
		
		if(!props.containsKey("user"))
		{
			 props.put("user","sa");
		}
		
		super.initDataSource(props);
	}
	
	@Override
	String[][] sqlClauses()
	{
		return new String[][]{ {null,"SELECT"}, //const
				               {"LIMIT" , "TOP"},
				               {"SELECT",""},
				               {"FROM"},
				               {"WHERE"},
				               {"GROUP BY"},
				               {"ORDER BY"},
				               {"OFFSET"}
				             };
	}
	
	public List<Map<String, Object>> columns(String tableName) throws SQLException
	{
		String sql = "SELECT name,xusertype,length,prec,scale,isnullable,COLUMNPROPERTY(a.id,name,'IsIdentity') as isauto," +
				"case when exists(SELECT 1 FROM sysobjects where xtype='PK' and name in (SELECT name FROM sysindexes WHERE indid in(" +
				"SELECT indid FROM sysindexkeys WHERE id=a.id AND colid=a.colid ))) then 1 else 0 end as iskey,e.text as defval " +
				"from syscolumns a left join syscomments e on a.cdefault=e.id where a.id = object_id('"+tableName+"') ";
		
		return this.sql(sql)
				.query(new RowMapper<java.util.Map<String,Object>>(){

			@Override
			public Map<String, Object> map(ResultSet rs, int rowNum)
					throws SQLException 
			{
				java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
				
				//name
				m.put("name", rs.getString("name"));
				
				//nullable
				int nullable = rs.getInt("isnullable");
				if(0 == nullable)
				{
					m.put("nullable", false);
				}
				else
				{
					m.put("nullable", true);
				}
				
				//default
				String defval = rs.getString("defval");
				if(defval !=null)
				{
					if(defval.startsWith("((")) defval=defval.substring(2);
					if(defval.endsWith("")) defval=defval.substring(0,defval.length()-2);
					m.put("default", defval);
				}
				
				//iskey
				if(1 == rs.getInt("iskey"))
				{
					m.put("key", true);
				}
				
				//auto
				if(1 == rs.getInt("isauto"))
				{
					m.put("auto", true);
				}
				
				//type
				int type = rs.getInt("xusertype");
				switch(type)
				{
				case 56:  //int
				   {
				       m.put("type", "numeric");
					   m.put("min",-2147483648);
					   m.put("max", 2147483647);
				   }
				   break;
				case 52: //smallint
				   {
					   m.put("type", "numeric");
					   m.put("min",-32768);
					   m.put("max", 32767);
				   }
				   break;
				case 48:  //tinyint
				   {
					   m.put("type", "numeric");
					   m.put("min",-128);
					   m.put("max", 127);
					   break;
				   }
				case 127: //bigint            :length=8
				case 36:  //uniqueidentifier  :length=16
				   {
					   m.put("type", "numeric");
					   break;
				   }
				case 175: //char
				case 239: //nchar
				   {
					   m.put("type", "string");
					   int len = rs.getInt("length");
					   m.put("min", len);
					   m.put("max", len);
					   break;
				   }
				case 167:  //varchar
				case 231:  //nvarchar
				case 256:  //sysname
				   {
					   m.put("type", "string");
					   m.put("min", 0);
					   m.put("max", rs.getInt("length"));
					   break;
				   }
				case 62:  //float
				case 106: //decimal
				case 108: //numeric
				case 60:  //money
				case 59:  //real
				case 122: //smallmoney					
				   {
					   m.put("type", "numeric");
					   m.put("length", rs.getInt("prec"));
					   m.put("scale", rs.getInt("scale"));
					   break;
				   }
				case 61: //datetime
				case 42: //datetime2
				case 43: //datetimeoffset
				   {
					   m.put("type", "datetime");
					   m.put("format", "yyyy-MM-dd HH:mm:ss");
					   break;
				   }
				case 40: //date
				   {
					   m.put("type", "datetime");
					   m.put("format", "yyyy-MM-dd");
					   break;
				   }
				case 41: //time
				   {
					   m.put("type", "time");
					   m.put("format", "HH:mm:ss");
					   break;
				   }				   
				case 58: //smalldatetime
				   {
					   m.put("type", "datetime");
					   m.put("format", "yyyy-MM-dd HH:mm");
					   break;
				   }
				case 104:  //bit
				   {
					   m.put("type", "bool");
					   break;
				   }	
				case 173:  //binary
				   {
					   m.put("type", "binary");
					   int len = rs.getInt("length");
					   m.put("min", len);
					   m.put("max", len);
					   break;
				   }
				case 165: //varbinary
				   {
					   m.put("type", "binary");
					   m.put("min", 0);
					   m.put("max", rs.getInt("length"));
					   break;
				   }
				case 189: //timestamp
				   {
					   m.put("type", "binary");
					   m.put("min", 8);
					   m.put("max", 8);
					   break;
				   }
				case 35: //text
				case 99: //ntext
				   {
					   m.put("type", "text");
					   break;
				   }
				case 34:  //image
				   {
					   m.put("type", "blob");
					   break;
				   }
				default:
				   {
					   m.put("type", "string");
				   }
				   break;
				};
				
				return m;
			}
					
		});//new java.util.ArrayList<Map<String,String>>();
	}
	
	public long lastID(String name) throws SQLException
	{
		return this.sql(String.format("Select IDENT_CURRENT(%s)", name)).queryLong();
	}
	
}
