/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import com.tern.dao.NamedValue;
import com.tern.util.Convert;
import com.tern.util.Trace;

public class Database
{
	final ThreadLocal<db.DBContext> dbConnection = new ThreadLocal<db.DBContext>();
	//protected IDBProvider dbSource;
	protected DataSource ds;
	protected boolean supportNestedTrans = true;
	
	protected Database()
	{
		//dbSource = ds;
	}
	
	String[][] sqlClauses()
	{
		return new String[][]{ {"SELECT"},
				               {"FROM"},
				               {"WHERE"},
				               {"GROUP BY"},
				               {"ORDER BY"},
				               {"LIMIT"},
				               {"OFFSET"}
				             };
	}
	
	public DBType getDbType()
	{
		return DBType.unknown;
	}
	
	public Connection getConnection() throws SQLException
	{
		db.DBContext s = dbConnection.get();
		if(s == null)
		{
			return ds.getConnection();
		}
		return s.con;
	}
	
	public void closeConnection(Connection conn)
	{
		db.DBContext s = dbConnection.get();
		if((s == null || s.con != conn) && conn!=null)
		{
			//dbSource.free(conn);
			try
			{			    
			    Trace.write(Trace.Information,"[con][%d] close connection" , conn.hashCode());
			    conn.close();
			}
			catch(SQLException e)
			{
				Trace.write(Trace.Error,e, "[con][%d] close connection" , conn.hashCode());
			}
		}
	}
	
	SQLExecutor createExecutor(SQLCmd cmd)
	{
		return new SQLExecutor(cmd);
	}
	
	public boolean inTransaction()
	{
		return dbConnection.get()!=null;
	}
	
	public db.Transaction transaction() throws SQLException
	{
		db.DBContext s = dbConnection.get();
		boolean f = false;
		if(s == null)
		{
			s = new db.DBContext();
			s.con = ds.getConnection();
			dbConnection.set(s);
			f = true;
		}
		
		try
		{
		    return new db.Transaction(s,this);
		}
		catch(SQLException e)
		{
			if(f) dbConnection.remove();
			throw e;
		}
	}
	
	public void commit()
	{
		db.DBContext s = dbConnection.get();
		if(s != null && s.trans!=null && s.trans.size()>0)
		{
			s.trans.get(0).commit();  //all commit
		}
	}
	
	public void rollback()
	{
		db.DBContext s = dbConnection.get();
		if(s != null && s.trans!=null && s.trans.size()>0)
		{
			s.trans.get(0).rollback();
		}
	}
	
	public SQL sql(String sqlstr)
	{	
		return new SQL(this,sqlstr,(Map<String, Object>)null);
	}

	public SQL sql(String sqlstr, Object[][] params)
	{
		if(params == null)
		{
			return new SQL(this,sqlstr,(Map<String, Object>)null);
		}
		else
		{
			Map<String, Object> ps = new java.util.HashMap<String, Object>(params.length);
			
			for(Object[] p:params)
    	    {
    	    	ps.put(p[0].toString(), p[1]);
    	    }
			
			return new SQL(this,sqlstr,ps);
		}
	}

	public SQL sql(String sqlstr, Map<String, Object> params)
	{
		return new SQL(this,sqlstr,params);
	}
	
	public SQL sql(String sqlstr, Object... params)
	{
		return new SQL(this,sqlstr,params);
	}

	public Query table(String tableName)
	{
		return new Query(this,tableName,null);
	}

	public Query table(String tableName, String aliasName)
	{
		return new Query(this,tableName,aliasName);
	}

	public UpdateCommand update(String tableName)
	{
		return new UpdateCommand(this,tableName);
	}

	public DeleteCommand delete(String tableName) 
	{
		return new DeleteCommand(this,tableName);
	}
	
	public InsertCommand insert(String tableName)
	{
		return new InsertCommand(this,tableName);
	}

	public List<Map<String, Object>> columns(String tableName) throws SQLException
	{
		return null;
	}
	
	public long lastID(String name) throws SQLException
	{
		return 0;
	}
	
	/*protected void setBlob(java.sql.PreparedStatement st,int index,db.BLOB value) throws SQLException
	{
		if (value == null)
        {
            st.setNull(index, java.sql.Types.BINARY);
        }
        else
        {
            st.setBytes(index, value.value);
        }
	}
	
	protected void setClob(java.sql.PreparedStatement st,int index,db.CLOB value) throws SQLException
	{
		if (value == null)
        {
            st.setNull(index, java.sql.Types.VARCHAR);
        }
        else
        {
            st.setString(index, value.value);
        }
	}*/
	
	/*public DataRow find(String tableName,long id) throws SQLException
	{
		return this.table(tableName)
				   .where("id=?",id)
				   .queryOne();
	}*/	
	
	protected void initDataSource(Map<String,Object> props) throws SQLException
	{
		String poolType = Convert.toStringIgnoreEmpty(props.get("pool"), "").toLowerCase();
		DataSourceLoader loader = null;
		if(poolType == null || poolType.length()<=0 || poolType.equals("builtin"))
		{
			ConnectionPool pool = new ConnectionPool();
			pool.initialize(props);
			this.ds = pool;
		}
		else if(poolType.equals("dbcp"))
		{			
			loader = new DBCPLoader();
		}
		
		if(loader == null)
		{
			if(this.ds == null)
			{
			    throw new SQLException("Unknow pool type:"+poolType);
			}
		}
		else
		{
			//load ds-class
			Class cls = null;
			for(String s:loader.getClasses())
			{
				try
				{
					cls = Class.forName(s);
					break;
				}
				catch(Throwable t)
				{
					continue;
				}
			}
			
			if(cls == null)
			{
				throw new SQLException("Load datasource failed,can not find class:"+loader.getClasses()[0]);
			}
			
			javax.sql.DataSource ds = null;
			try 
			{
				ds = (DataSource) cls.newInstance();
			}
			catch (Exception e)
			{
				throw new SQLException(e);
			}
			
			for(String[] atts:loader.getAttribues())
			{
				String pname = atts[0];
				String key = atts.length>1?atts[1]:pname;
				if(props.containsKey(key))
				{
					try
					{
						setBeanProperty(ds,pname,props.get(key));
					}
					catch(Exception e)
					{
						throw new SQLException(e);
					}
				}
			}
			
			this.ds = ds;
		}			
				
		Connection con = null;
		
		try
		{
			//print ds-info
			con = ds.getConnection();
	        java.sql.DatabaseMetaData meta = con.getMetaData();
	        
			Trace.write(Trace.Running, "Database：%s(v%s)",meta.getDatabaseProductName(),
					meta.getDatabaseProductVersion());
			
	        Trace.write(Trace.Running, "Database："+"JDBC %s.%s, %s(v%s) Pool Class(%s)",
	        		meta.getJDBCMajorVersion(),meta.getJDBCMinorVersion(),meta.getDriverName(),
	        		meta.getDriverVersion(),ds.getClass().getName());
		}
		catch(Exception e)
		{
			Trace.write(Trace.Error, e, "Init database");
		}
		finally
		{
			if (con != null)
            {
                try
                {
                    con.close();
                }
                catch (java.sql.SQLException ex1)
                {
                }
            }
		}
	}
	
	static void setBeanProperty(Object ins,String propertyName,Object value)
			throws SecurityException,NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		String methodName="set"+propertyName.substring(0,1).toUpperCase() + propertyName.substring(1);
		java.lang.reflect.Method method = ins.getClass()
				                             .getDeclaredMethod(methodName, new Class[]{value.getClass()});
		method.invoke(ins, value);
	}
	
	static Database db;
	static Map<String,Database> dbs = new java.util.HashMap<String, Database>();
	
	public static Database defaultDB()
	{
		return db;
	}
	
	public static Database DB(String name)
	{
		Database d = dbs.get(name);
		if(d == null && (name==null || name.equalsIgnoreCase("default")))
		{
			d = db;
		}
		return d;
	}
	
	static Database createDB(Map<String,Object> props) throws SQLException
	{
		if(props==null)
		{
			throw new SQLException("no db attribute.");
		}
		    			
		DBType type = DBType.unknown;
		
		try
		{
		    type = DBType.valueOf(Convert.toString(props.get("dbn")).toLowerCase());
		}
		catch(Throwable e)
		{
			type = DBType.unknown;
		}
		
		props.remove("dbn");
		
		Database database = null;
		if(DBType.unknown == type)
		{
			if(props.containsKey("class"))
			{
				//直接实例化？？
				String constructor = Convert.toString(props.get("class"));
				props.remove("class");
				try
				{
				    Class<?> cls = Class.forName(constructor);
				    Object o = cls.newInstance();
				    if(!(o instanceof Database))
				    {
				    	throw new SQLException("no dbn attribute and try to construct database instance failed!");
				    }
				    
				    database = (Database)o;
				}
				catch(Throwable t)
				{
					throw new SQLException(t);
				}
			}
			else
			{
				throw new SQLException("Unknown dbn: " + props.get("dbn"));
			}
		}
		
		String name = Convert.toString(props.get("name"));
		if(name == null || name.length()<=0)
		{
			if(db == null)
			{
				name = "default";
			}
			else
			{
				//throw new SQLException("Database init failed: no name attribute.");
				name = null;
			}
		}
		
		if(name!=null)
		{
			if(dbs.containsKey(name))
			{
			    throw new SQLException("Has duplicate database name:"+name);
			}
			
			props.remove("name");
		}				
				
		switch(type)
		{
		case mysql:
			database = new MySqlDB();
			break;
		case mssql:
			database = new SqlServerDB();
			break;
		case sqlite:
			database = new SqliteDB();
			break;
		case oracle:
			database = new OracleDB();
			break;
		default:
		    if(null == database) throw new SQLException("unknown database type.");		
		};
		
		database.initDataSource(props);
		
		if(db == null)
		{
			db = database;
		}
		
		if(name!=null)
		{
		    dbs.put(name, database);
		}
		//Trace.write(Trace.Running, "database added,name = %s" , name );
		return database;
	}
	
	/*static void addDB(String name,IDBProvider p)
	{
		Database d = new Database(p);
		if(db == null) db = d;
		
		if(name==null || name.length()<=0)
		{
			name="default";
		}
		
		if(!dbs.containsKey(name))
		{
			dbs.put(name, d);
		}
	}	*/
	
	Object sqlvalue(Object value)
	{
		if(null == value) return "null";
		else if(value instanceof NamedValue)
		{
			value = ((NamedValue)value).getValue();
		}
		
		if(value instanceof Integer || value instanceof Double
				 || value instanceof Float || value instanceof Long
				 || value instanceof java.math.BigDecimal
				 || value instanceof java.math.BigInteger)
		{
			return value;
		}
		else if(value instanceof Boolean)
		{
			if( ((Boolean)value) )
			{
				return "1";
			}
			else
			{
				return "0";
			}
		}
		else if(value instanceof java.util.Date
				|| value instanceof java.sql.Date
				|| value instanceof java.sql.Time
				|| value instanceof java.sql.Timestamp)
		{
			return null;
		}
		else
		{
			return "'"+Convert.replaceAll(value.toString(), "'", "''")+ "'";
		}
	}		

}

abstract class SQLCmd
{
	protected Database db;
	protected Map<String,Object> params;
	//Object[] sqlparams;
	protected List<Object> sqlparams;
	
	Database getDb(){return db;}	
	
	protected Map<String,Object> getNamedParams() {return params;}
	protected List<Object> getSQLParams(){return this.sqlparams;};
	
	boolean isProcedure() {return false;}
	
	abstract String getSql();
	
	protected void _param(String key,Object v)
	{
		if(params == null)
		{
			params = new HashMap<String,Object>();
		}
		
		params.put(key, v);
	}
	
	protected void _param(int i,Object v)
	{
		//if(i<0)
		//{
		//	throw new ArrayIndexOutOfBoundsException();
		//}
		
		if(sqlparams == null)
		{
		    sqlparams = new java.util.ArrayList<Object>();
		}
		
		if(sqlparams.size() < i)
		{
		    for(int j=sqlparams.size();j<i;j++)
		    {
		    	sqlparams.add(null);
		    }
		}
		
		
		if(v instanceof java.util.Date)
		{
			if(v instanceof java.sql.Date
				|| v instanceof java.sql.Timestamp
				|| v instanceof java.sql.Time)
			{
				//pass
			}
			else
			{
			    v = new java.sql.Date(((java.util.Date)v).getTime());
			    //v = new java.sql.Time(((java.util.Date)v).getTime());
			}
		}
		
		if(i < 0 || sqlparams.size() == i)
		{
			i = sqlparams.size();
			sqlparams.add(v);
		}
		else
		{
			sqlparams.set(i, v);	
		}	
		
		if(v instanceof OutParam)
		{
			OutParam op = (OutParam)v;
			op.index = i+1;
		}
	}
}

class SQLExecutor implements Convert.FormatFilter
{
	protected Connection conn;
	protected Statement st;
	protected String sql;
	
	protected SQLCmd cmd;
	protected int rsIndex = 0;
	protected int cmdType = 1;
	
	protected boolean hasOutParam = false;
	protected List<Object> sps;
	
	public SQLExecutor(SQLCmd c)
	{
		cmd = c;
	}
	
	@Override
	public Object filter(String key, Object value, int index)
	{
		Object obj = cmd.db.sqlvalue(value);
		if(null == obj)
		{
			cmd._param(-1, value);  //转换成sql参数
			return "?";
		}
		else
		{
		    return obj;
		}
	}		
	
	protected void exec() throws SQLException
	{
		sql = cmd.getSql();
		
		Map<String,Object> nps = cmd.getNamedParams();
		if(nps != null)
		{
			sql = Convert.format(sql, nps,this);
		}				
		
		if(sql == null || sql.length()<=0)
    	{
    		throw new SQLException("SQL is empty");
    	}
		
		conn = cmd.getDb().getConnection();
		
		sps = cmd.getSQLParams();
		boolean hasPara = sps != null && sps.size()>0;
		
		if (cmd.isProcedure())
		{
			sql = "{call " + sql + "}";
            st = conn.prepareCall(sql);
            cmdType = 3;
		}
		else if(hasPara)
		{
			st = conn.prepareStatement(sql);
			cmdType = 2;
		}
		else
		{
		    st = conn.createStatement();
		    cmdType = 1;
		}
		
		if(hasPara)
		{
			PreparedStatement pst = (PreparedStatement)st;
			for(int i=0;i<sps.size();i++)
			{
				Object o = sps.get(i);
				if(o instanceof OutParam)
				{
					if(hasOutParam || (st instanceof CallableStatement))
					{
						OutParam op = (OutParam)o;
						((CallableStatement)st).registerOutParameter(op.index, op.type);
						hasOutParam = true;
					}
					else
					{
						throw new SQLException("out-param should be used in procedure");
					}
				}
				/*else if(o instanceof db.BLOB)
				{
					cmd.getDb().setBlob(pst, i+1, (db.BLOB)o );
				}
				else if(o instanceof db.CLOB)
				{
					cmd.getDb().setClob(pst, i+1, (db.CLOB)o );					
				}*/
				else
				{
				    pst.setObject(i+1, o);
				}
			}
			
			//print params-value
			if(Trace.needTrace(Trace.Information))
			{
				StringBuffer buf = new StringBuffer("Params:");
				for(int i=0;i<sps.size();i++)
				{
					buf.append(" [").append(i).append("] = ").append(sps.get(i));
				}
				
				Trace.write(Trace.Information, buf.toString());
			}
		}
	}
	
	protected void setOutputParamters() throws SQLException
	{
		CallableStatement cst = (CallableStatement)st;
		for(Object o: sps)
		{
			if(o instanceof OutParam)
			{
				OutParam op = (OutParam)o;
				op.value = cst.getObject(op.index);
			}
		}
	}
	
	public int execute() throws SQLException
	{			
		try
		{
			exec();
			
			rsIndex = -1;
						
		    int ret;
		    
		    if(1 == cmdType)
		    {
		    	ret = st.executeUpdate(sql);
		    }
		    else
		    {
		    	ret = ((PreparedStatement)st).executeUpdate();
		    }
		    
		    Trace.write(Trace.Information, "[con][%d] SQL: %s",
    				conn.hashCode() , sql);
		    
		    if(hasOutParam)
		    {
		    	this.setOutputParamters();
		    }
		    
		    return ret;
		}
		catch(SQLException e)
		{
			Trace.write(Trace.Error, e,"[con][%d] SQL failed: %s",conn.hashCode() , sql);
			throw e;
		}
	}
	
	public ResultSet query() throws SQLException
	{		
		try
		{
			exec();
			
			rsIndex = -1;
			ResultSet rs ;// = st.executeQuery(sql);
			if(1 == cmdType)
			{
				rs = st.executeQuery(sql);
			}
			else
			{
				rs = ((PreparedStatement)st).executeQuery();
			}
		    
		    Trace.write(Trace.Information, "[con][%d] SQL: %s",
    				conn.hashCode() , sql);
		    
		    if(hasOutParam)
		    {
		    	this.setOutputParamters();
		    }
		    return rs;
		}
		catch(SQLException e)
		{
			Trace.write(Trace.Error, e,"[con][%d] SQL failed: %s",conn.hashCode() , sql);
			throw e;
		}
	}
	
	public ResultSet queryNext() throws SQLException
	{
		if(rsIndex < 0)
		{
			throw new SQLException("invalide operation");
		}
		
		exec();
		
		ResultSet rs = null;
        int j = -1;
        boolean b = false;
        if (rsIndex == 0)
        {
        	if(1 == cmdType)
			{
				b = st.execute(sql);
			}
			else
			{
				b = ((PreparedStatement)st).execute();
			}
        	
        	j = st.getUpdateCount();
            while (b || j > 0)
            {
                if (b)
                {
                    rs = st.getResultSet();
                    break;
                }
                b = st.getMoreResults();
                j = st.getUpdateCount();
            }
            
            rsIndex++;
            
            if(hasOutParam)
		    {
		    	this.setOutputParamters();
		    }
        }
        else
        {
            b = st.getMoreResults();
            j = st.getUpdateCount();
            while (b || j > 0)
            {
                if (b)
                {
                    rs = st.getResultSet();
                    break;
                }
                b = st.getMoreResults();
                j = st.getUpdateCount();
            }
            rsIndex++;
        }
        
        if(Trace.needTrace(Trace.Information))
        {
        	if(rs==null)
        	{
        		if(1 == rsIndex)
        		{
        			Trace.write(Trace.Information, "[con][%d] execute(next rs) failed: %s",
        		    		conn.hashCode(),sql);
        		}
        		else
        		{
    		        Trace.write(Trace.Information, "[con][%d] execute(next rs) failed",
    		    		conn.hashCode());
        		}
        	}
        	else
        	{
        		if(1 == rsIndex)
        		{
        			Trace.write(Trace.Information, "[con][%d] execute(next rs): %s",
            				conn.hashCode(),sql);
        		}
        		else
        		{
        		    Trace.write(Trace.Information, "[con][%d] execute(next rs)",
        				conn.hashCode());
        		}
        	}
        }        
		
		return rs;
	}
	
	public void close()
	{
		if(st != null)
		{
			try
			{
			    st.close();
			}catch(SQLException e){}    			
		}
		
		if(conn != null)
		{
		    cmd.getDb().closeConnection(conn);
		}
	}
}
