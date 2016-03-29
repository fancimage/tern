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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tern.util.Convert;
import com.tern.util.config;

public class MySqlDB extends Database
{
	public DBType getDbType()
	{
		return DBType.mysql;
	}
	
	protected void initDataSource(Map<String,Object> props) throws SQLException
	{
		if(!props.containsKey("driver"))
		{
		    props.put("driver", "com.mysql.jdbc.Driver");
		}
		
		String dbname = Convert.toString(props.get("db"));
		if(dbname==null || dbname.length()<=0)
		{
			throw new SQLException("no db attribue.");
		}
		
		String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=%s",
				Convert.toStringIgnoreEmpty(props.get("ip"), "127.0.0.1"),
				Convert.parseInt(props.get("port"),3306),
				dbname,
				Convert.toStringIgnoreEmpty(props.get("encoding"), config.getEncoding())
			);
		props.put("url",url);
		
		if(!props.containsKey("user"))
		{
			 props.put("user","root");
		}
		
		super.initDataSource(props);
	}
	
	public long lastID(String name) throws SQLException
	{
		return this.sql("SELECT last_insert_id();").queryLong();
	}
	
	public List<Map<String, Object>> columns(String tableName) throws SQLException
	{
		String sql = String.format("SHOW FULL FIELDS FROM `%s`", tableName);
		final Pattern pat = Pattern.compile("^(\\w+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
		
		return this.sql(sql)
				.query(new RowMapper<java.util.Map<String,Object>>(){

			@Override
			public Map<String, Object> map(ResultSet rs, int rowNum)
					throws SQLException 
			{
				java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
				
				m.put("name", rs.getString("Field"));
				
				String nullable = Convert.toStringIgnoreEmpty(rs.getString("Null"), "YES");
				if(nullable.equalsIgnoreCase("NO"))
				{
					m.put("nullable", false);
				}
				else
				{
					m.put("nullable", true);
				}
				
				m.put("default", rs.getString("Default"));
				
				String comment = rs.getString("Comment");
				if(comment!=null && comment.length()>0)
				{
					m.put("desc", comment.trim());
				}
				
				String key = rs.getString("Key");
				if(key != null && key.equals("PRI"))
				{
					m.put("key", true);
				}
				
				String extra = rs.getString("Extra");
				if(extra != null && extra.equals("auto_increment"))
				{
					m.put("auto", true);
				}
				
				Matcher matcher = pat.matcher(rs.getString("Type").toLowerCase());
				matcher.find();
				String _type = matcher.group(1);
				String _len = matcher.group(2);
				String _scale = matcher.group(3);
				
				if(_type.equals("int"))
				{
					m.put("type", "numeric");
					m.put("min",-2147483648);
					m.put("max", 2147483647);
				}
				else if(_type.equals("smallint"))
				{
					m.put("type", "numeric");
					m.put("min",-32768);
					m.put("max", 32767);
				}
				else if(_type.equals("mediumint"))
				{
					m.put("type", "numeric");
					m.put("min",-8388608);
					m.put("max", 8388607);
				}
				else if(_type.equals("tinyint"))
				{
					m.put("type", "numeric");
					m.put("min",-128);
					m.put("max", 127);
				}
				else if(_type.equals("bigint"))
				{
					m.put("type", "numeric");
				}
				else if(_type.equals("char"))
				{
					m.put("type", "string");
					int l = Convert.parseInt(_len,1);
					m.put("min", l);
					m.put("max", l);
				}
				else if(_type.equals("varchar"))
				{
					m.put("type", "string");
					m.put("min", 0);
					m.put("max", Convert.parseInt(_len,1));
				}
				else if(_type.equals("float")
						|| _type.equals("double")
						|| _type.equals("decimal"))
				{
					m.put("type", "numeric");
					m.put("length", Convert.parseInt(_len,1));
					m.put("scale",  Convert.parseInt(_scale,1));
				}
				else if(_type.equals("binary"))
				{
					m.put("type", "binary");
					int l = Convert.parseInt(_len,1);
					m.put("min", l);
					m.put("max", l);
				}
				else if(_type.equals("varbinary"))
				{
					m.put("type", "binary");
					m.put("min", 0);
					m.put("max", Convert.parseInt(_len,1));
				}
				else if(_type.equals("datetime"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy-MM-dd HH:mm:ss");
				}
				else if(_type.equals("date"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy-MM-dd");
				}
				else if(_type.equals("time"))
				{
					m.put("type", "datetime");
					m.put("format", "HH:mm:ss");
				}
				else if(_type.endsWith("text"))
				{
					m.put("type", "text");
				}
				else if(_type.equals("bit") || _type.endsWith("blob"))
				{
					m.put("type", "blob");
				}
				else if(_type.equals("year"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy");
				}
				else if(_type.equals("boolean"))
				{
					m.put("type", "bool");					
				}
				else
				{
					m.put("type", "string");
				}
				
				return m;
			}
		});
	}
}
