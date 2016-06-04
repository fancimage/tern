/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.io.PrintWriter;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.tern.util.Trace;
import com.tern.util.Convert;

public class ConnectionPool implements DataSource//implements IDBProvider
{
	protected PrintWriter _logWriter = new PrintWriter(System.out);
	
    private String m_JDBCDriver; // JDBC 驱动名称
    private String m_JDBCConnectionURL; // JDBC 连接字符串
    private int m_ConnectionPoolSize; // 池的最小容量
    private int m_ConnectionPoolMax; // 池的最大容量
    private int m_ConnectionUseCount; //一个连接所允许使用的最大次数，－1表示无限制

    private int m_ConnectionTimeout = -1; //连接所能被占用的最大时间（秒),超过此时间，应移除
    private int m_ConnectionExpire = -1; //连接所能空闲的最大时间（秒),超过此时间，表示连接长久得不到应用，应移除
    private int m_listenerCycle = -1; //连接池监视器的调度时间(秒)
    private String db_username;
    private String db_password;
    
	private java.util.List<ConnectionObject> m_pool; //连接池,容纳ConnectionObject的集合
    private boolean stop_it = false;
    private boolean is_clean = false;
    private int m_MaxConnections = 0; //数据库支持的最大连接数目

    static final String Cannot_Get_Connection = "can not get database connection！";
    static final String Pool_Not_Init = "Connect Pool is not ready yet,can not get connection！";
    static final String ConCount_Exceed = "connection has been used so many times,close it.";
    static final String MaxConNum_Exceed = "it will exceed maxinum number of connections.";
    static final String SafeConNum_Exceed = "warn--it will exceed the safe number of connections database supported:";
    static final String CloseUnexpectedly = "connection was closed unexpectedly";
    static final String ConExpired = "too many free connections,close some of them.";
    static final String POOL = "[ConnectPool]:";

    public ConnectionPool()
    {}

    //从池中取出一个空闲的连接
    private long s3;    
	final public synchronized Connection getConnection()
        throws java.sql.SQLException
    {
        Connection con = null;
 
        long s1 = System.currentTimeMillis(), s2 = s1;
        while (con == null && s2 - s1 <= s3)
        {
            con = getCon();
            if (con == null)
            {
                try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new java.sql.SQLException(e);
				}
            }
            else
            {
            	Trace.write(Trace.Information, "[con][%d] get connection from pool.",con.hashCode());            	
                return new ConnectionWrapper(this,con);
            }
            s2 = System.currentTimeMillis();
         }
           
        Trace.write(Trace.Error, "Can not get a connect from the pool.");
        throw new java.sql.SQLException(Cannot_Get_Connection);
    }
    
    final public String getDiverName()
    {
    	return m_JDBCDriver;
    }

    final public void destroy(Connection con)
    {
        free(con);
        try
        {
            con.close();
        }
        catch (Exception e)
        {}
    }

    /**
     * <p>用指定的配置文件初始化连接池
     *
     * @param config 配置文件名
     * @如果初始化顺利，则返回真
     */
    final public boolean initialize(Map config)
        throws SQLException
    {
        //Configuration config = Configuration.getInstance();
        //得到配置内容
        m_JDBCDriver = Convert.toString(config.get("driver"));
        m_JDBCConnectionURL = Convert.toString(config.get("url"));
        this.db_username = Convert.toString(config.get("user"));
        
        if(m_JDBCDriver==null || m_JDBCDriver.length() <=0
        		|| m_JDBCConnectionURL==null 
        		|| m_JDBCConnectionURL.length() <= 0
        		|| db_username == null || db_username.length()<=0)
        {
        	throw new SQLException("the Configuration of Connect Pool is wrong.");
        }
                
        this.db_password = Convert.toString(config.get("pw"));
        
        m_ConnectionPoolSize = Convert.parseInt(config.get("minActive"));
        if(m_ConnectionPoolSize<=0)
        {
        	m_ConnectionPoolSize = 1;
        }
        
        m_ConnectionPoolMax = Convert.parseInt(config.get("maxActive"));
        if(m_ConnectionPoolMax<=0)
        {
        	m_ConnectionPoolMax = 2*m_ConnectionPoolSize;
        }
        
        m_ConnectionTimeout = Convert.parseInt(config.get("maxUsedTime"));                
        m_ConnectionUseCount = Convert.parseInt(config.get("maxUsedCount"));
        if(m_ConnectionUseCount<=0)
        {
        	m_ConnectionUseCount = 30000;
        }
        
        m_listenerCycle = Convert.parseInt(config.get("checkTime"),20);
        m_ConnectionExpire = Convert.parseInt(config.get("maxIdleTime"),10);        

        s3 = m_ConnectionExpire * 1000;

        //建立连接池
        createPool();
        //启动定时器，监视连接的超时，定时器周期为20秒
        m_timer = new Timer();
        m_timer.start();
        return true;
    }

    Timer m_timer = null;

    /**
     * <p>清空连接池，关闭所有打开的连接，释放所有持有的资源
     */
    public synchronized void close()
    {
        if (!is_clean)
        {
            try
            {
                // 停止定时器
                stop_it = true;
                if (m_timer != null)
                {
                    m_timer.interrupt();
                }
            }
            catch (Exception ex)
            {
                //ex.printStackTrace();
                Trace.write(Trace.Error,ex, "");
            }
            try
            {
                //清空连接池
                if (m_pool != null)
                {
                    int count = m_pool.size();
                    for (int i = count - 1; i >= 0; i--)
                    {
                        dispose( m_pool.get(i));
                    }
                    m_pool.clear();
                }                
                
                m_pool = null;
            }
            catch (Exception ex)
            {
                //ex.printStackTrace();
            	Trace.write(Trace.Error,ex,"");
            }
            trace("[Connection Pool] Destory the Pool.......");
            is_clean = true;
        }
    }

    //保证连接池对象的资源会被释放，因此在其析构函数中也作摧毁动作
    public void finalize()
    {
    	close();
    }

    /**
     * <p>得到一个可用的连接。需要时会建立连接除非池中数目已经达到最大
     *
     * @返回一个JDBC Connection, 如果池已满，则返回空
     */
    private synchronized java.sql.Connection getCon()
    {
        if (m_pool == null)
        {
            return null;
        }
        java.sql.Connection con = null;
        ConnectionObject connectionObject = null;
        int poolSize = m_pool.size();
        // 得到下一个可用连接
        for (int i = 0; i < poolSize; i++)
        {
            ConnectionObject co = m_pool.get(i);
            if (co.isAvailable())
            {
                connectionObject = co;
                break;
            }
        }

        // 没用可用连接，且池未满，则新建一个连接
        if (connectionObject == null)
        {
            if ( (m_ConnectionPoolMax < 0) ||
                ( (m_ConnectionPoolMax > 0) && (poolSize < m_ConnectionPoolMax)))
            {
                int i = addConnection();
                if (i >= 0)
                {
                    connectionObject = m_pool.get(i);
                }
            }
            else
            {
                //trace(MaxConNum_Exceed);
            }
        }
        // 如果取得了连接，设定最后访问时间，增加其被访问的次数，并置为“忙”
        if (connectionObject != null)
        {
            connectionObject.inUse = true;
            connectionObject.useCount++;
            touch(connectionObject);
            con = connectionObject.con;
        }
        return con;
    }

    private int find(java.sql.Connection con)
    {
        int index = -1;
        if ( (con != null) && (m_pool != null))
        {
            int count = m_pool.size();
            for (int j = 0; j < count; j++)
            {
                ConnectionObject co =  m_pool.get(j);
                if (co.con == con)
                {
                    index = j;
                    break;
                }
            }
        }
        return index;
    }

    /**
     * <p>将连接返回池中,已超过规定的使用次数,则从池中移除连接
     *
     * @param 要关闭的数据库连接
     */
    synchronized void free(Connection con)
    {
        // 寻找连接在池中的位置
        int index = find(con);
        if (index != -1)
        {
        	if(Trace.needTrace(Trace.Information))
        	{
        		Trace.write(Trace.Information, "[con][%d] free connection to pool.",con.hashCode() );
        	}
        	
            ConnectionObject co =  m_pool.get(index);
            // 已超过规定的使用次数,则从池中移除连接
            if ( (m_ConnectionUseCount > 0) &&
                (co.useCount >= m_ConnectionUseCount))
            {
                trace(ConCount_Exceed);
                removeFromPool(co);
            }
            else
            {
                //touch(co);
                co.inUse = false;
            }
        }
    }      

    /**
     * <p>打印出连接池的信息
     */
    public void printPool()
    {
        System.out.println("--[Connection Pool]:--");
        if (m_pool != null)
        {
            synchronized (m_pool)
            {
                int count = m_pool.size();
                for (int i = 0; i < count; i++)
                {
                    System.out.println("" + i + "=" + m_pool.get(i));
                }
            }
        }
    }

    /**
     * <p>以给定的索引，将连接从池中移除
     *
     * @param 连接在池中的位置
     */
    private synchronized void removeFromPool(ConnectionObject co)
    {
        if (m_pool != null)
        {
            if (m_pool.contains(co))
            {
                dispose(co);
                m_pool.remove(co);
            }
        }
    }

    private void dispose(ConnectionObject connectionObject)
    {
        if (connectionObject != null)
        {
            if (connectionObject.con != null)
            {
            	Trace.write(Trace.Information, "[con][%d] close connection.",connectionObject.con.hashCode());
            	
                try
                {
                    connectionObject.con.close();
                }
                catch (Exception ex)
                {
                }
                connectionObject.con = null;
            }
        }
    }

    /**
     * <p>建立一个初始的连接池，并建立定时器处理连接的超时状况
     *
     * @如果初始化正确，则返回真
     */    
	private void createPool()
        throws SQLException
    {
        if (m_JDBCDriver == null)
        {
            throw new SQLException("JDBCDriver property not found");
        }
        if (m_JDBCConnectionURL == null)
        {
            throw new SQLException("JDBCConnectionURL property not found");
        }
        if (m_ConnectionPoolSize < 0)
        {
            throw new SQLException("ConnectionPoolSize property not found");
        }
        if (m_ConnectionPoolSize == 0)
        {
            throw new SQLException("ConnectionPoolSize invalid");
        }
        if (m_ConnectionPoolMax < m_ConnectionPoolSize)
        {
            trace("WARNING - ConnectionPoolMax is invalid and will " +
                  "be ignored");
            m_ConnectionPoolMax = -1;
        }
        if (m_ConnectionTimeout < 0)
        {
            m_ConnectionTimeout = 5;
        }
        if (m_ConnectionExpire < 0)
        {
            m_ConnectionExpire = 100;
        }
        if (m_listenerCycle < 0)
        {
            m_listenerCycle = 20;
        }

        trace("JDBCDriver = " + m_JDBCDriver);
        trace("JDBCConnectionURL = " + m_JDBCConnectionURL);
        trace("ConnectionPoolSize = " + m_ConnectionPoolSize);
        trace("ConnectionPoolMax = " + m_ConnectionPoolMax);
        trace("ConnectionUseCount = " + m_ConnectionUseCount);
        trace("ConnectionTimeout = " + m_ConnectionTimeout + " seconds");
        trace("ConnectionExpire = " + m_ConnectionExpire + " seconds");
        trace("ListenerCycle = " + m_listenerCycle + " seconds");

        //trace("Registering " + m_JDBCDriver);
        java.sql.Driver dr = null;
        try 
        {
			dr = (java.sql.Driver) Class.forName(m_JDBCDriver).newInstance();
		} 
        catch (Exception e) 
		{
        	throw new SQLException(e);
		}
        
        m_pool = new java.util.ArrayList<ConnectionObject>();

        fillPool(m_ConnectionPoolSize);
        
        Connection con = m_pool.get(0).con;
        java.sql.DatabaseMetaData meta = con.getMetaData();
        trace(" JDBC: v" + meta.getJDBCMajorVersion()+"."+meta.getJDBCMinorVersion()
        		+" , Database: "+meta.getDatabaseProductName() +" v"+meta.getDatabaseProductVersion()
        		+"(Driver: v"+dr.getMajorVersion()+"."+dr.getMinorVersion()+")");
    }

    private int addConnection()
    {
        int index = -1;
        try
        {
            int size = m_pool.size() + 1;
            fillPool(size);
            if (size == m_pool.size())
            {
                index = size - 1;
            }
        }
        catch (Exception ex)
        {
            //ex.printStackTrace();
        	Trace.write(Trace.Error,ex, "");
        }
        return index;
    }

    /**
     * <p>将池中连接填充至指定数目
     */
	private synchronized void fillPool(int size)
        throws SQLException
    {
        while (m_pool.size() < size)
        {
            ConnectionObject co = new ConnectionObject();
            try
            {
                co.con = java.sql.DriverManager.getConnection(
                    m_JDBCConnectionURL,
                    db_username, this.db_password);
            }
            catch (SQLException e)
            {
                //e.printStackTrace();
            	Trace.write(Trace.Warning, e , "DBPool Create con failed,now pool size=%d",m_pool.size() );
                throw e;
            }

            if (m_pool.size() == 0)
            {
                java.sql.DatabaseMetaData md = co.con.getMetaData();
                m_MaxConnections = md.getMaxConnections(); //数据库所能承受的最大连接数目
            }

            if ( (m_MaxConnections > 0) && (size > m_MaxConnections))
            {
                trace(SafeConNum_Exceed + m_MaxConnections);
            }
            co.inUse = false;
            touch(co);
            m_pool.add(co);
        }
    }

    private void touch(ConnectionObject co)
    {
        if (co != null)
        {
            co.lastAccess = System.currentTimeMillis();
        }
    }

    private void trace(String s)
    {
        //System.out.println(POOL + s);
        Trace.write(Trace.Running,POOL + s);
    }

    private synchronized void TimerEvent()
    {
        if (m_pool == null)
        {
            return;
        }
        long now = System.currentTimeMillis(); //得到现在时间

        // 检查超时的连接，将他们从连接池移除
        long Expire = m_ConnectionExpire * 1000;
        long TimeOut = m_ConnectionTimeout * 1000;
        for (int i = m_pool.size() - 1; i >= 0; i--)
        {
            ConnectionObject co = (ConnectionObject) m_pool.get(i);

            if (co.inUse) //强行关闭长时间被占用的连接
            {
                if ( (m_ConnectionTimeout > 0) &&
                    (co.lastAccess + TimeOut < now))
                {
                    //System.out.println("强行关闭："+co);
                    dispose(co);
                }
            }
            else //连接池已超出了最低连接数目，且有长久不用的连接，则应移除这些连接
            {
                if (m_pool.size() > m_ConnectionPoolSize &&
                    (m_ConnectionExpire > 0) &&
                    (co.lastAccess + Expire < now))
                {
                    removeFromPool(co);
                }
            }
        }

        // 移除那些从未被打开过的连接
        for (int i = m_pool.size() - 1; i >= 0; i--)
        {
            ConnectionObject co = (ConnectionObject) m_pool.get(i);
            try
            {
                if (co.con == null)
                {
                    trace(ConExpired);
                    removeFromPool(co);
                }
                else if (co.con.isClosed())
                {
                    trace(CloseUnexpectedly);
                    removeFromPool(co);
                }
            }
            catch (Exception ex)
            {
            }
        }

        // 保证连接池内的连接数目要维持到最低水平
        try
        {
            if (m_pool != null && m_pool.size() < m_ConnectionPoolSize)
            {
                //System.out.println("重新填充.....");
                fillPool(m_ConnectionPoolSize);
            }
        }
        catch (Exception ex)
        {
            //ex.printStackTrace();
        	Trace.write(Trace.Error, ex, "");
        }
    }

    private class Timer extends Thread
    {
        public Timer()
        {}

        public void run() // throws Exception
        {
            long sleepTime = m_listenerCycle * 1000;
            try
            {
                //控制线程的调度周期
                sleep(sleepTime);
            }
            catch (InterruptedException ex)
            {
            }

            while (!stop_it)
            {
                //监控连接池的状态
                TimerEvent();
                if (m_pool == null)
                {
                    return;
                }
                try
                {
                    //控制线程的调度周期
                    sleep(sleepTime);
                }
                catch (InterruptedException ex)
                {
                }
            }
            stop_it = false;
            trace("Monitor thread terminated.");
        }
    }

    //数据库连接的包装
    private class ConnectionObject
    {
        public java.sql.Connection con; //数据库连接
        public boolean inUse; //如果连接正在被使用则为真
        public long lastAccess; //连接最后被使用的时间(in milliseconds)
        public int useCount; //连接被使用的次数

        /**
         * <p>判断连接是否可用
         *
         * @连接可用则返回真
         */
        public boolean isAvailable()
        {
            boolean available = false;
            try
            {
                // To be available, the connection cannot be in use
                // and must be open
                if (con != null)
                {
                    if (!inUse && !con.isClosed())
                    {
                        available = true;
                    }
                }
            }
            catch (Exception ex)
            {
            }
            return available;
        }

        /**
         * <p>重写类的toString方法
         */
        public String toString()
        {
            return "Connection=" + con.hashCode() + ","+ con + ",inUse=" + inUse + ",lastAccess=" +
                lastAccess + ",useCount=" + useCount;
        }             
     }

	@Override
	public Connection getConnection(String arg0, String arg1)
			throws SQLException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return _logWriter;
	}

	@Override
	public int getLoginTimeout() throws SQLException
	{
		throw new UnsupportedOperationException("Login timeout is not supported.");
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException 
	{
		_logWriter = out;
	}

	@Override
	public void setLoginTimeout(int arg0) throws SQLException
	{
		throw new UnsupportedOperationException("Login timeout is not supported.");
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		return null;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}


class ConnectionWrapper implements Connection
{
	private Connection con;
	private ConnectionPool pool;
	
	public ConnectionWrapper(ConnectionPool pool,Connection con)
	{
		this.con = con;
		this.pool = pool;
	}
	
	public boolean equals(Object obj)
	{
        if (obj == null) 
        {
            return false;
        }
        
        if (obj == this)
        {
            return true;
        }
                
        if (con == null) 
        {
            return false;
        }
        
        if (obj instanceof ConnectionWrapper)
        {    
        	ConnectionWrapper c = (ConnectionWrapper) obj;
        	if(c.con == null) return con==null;
        	else return c.con.equals(con);
        }        
        else 
        {
            return con.equals(obj);
        }
    }

    public int hashCode() 
    {        
        if (con == null)
        {
            return 0;
        }
        return con.hashCode();
    }
	
	public String toString()
	{
        String s = null;
                
        if (con != null) 
        {
            try 
            {
                if (con.isClosed()) 
                {
                    s = "connection is closed";
                }
                else
                {
                    DatabaseMetaData meta = con.getMetaData();
                    if (meta != null)
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append(meta.getURL());
                        sb.append(", UserName=");
                        sb.append(meta.getUserName());
                        sb.append(", ");
                        sb.append(meta.getDriverName());
                        s = sb.toString();
                    }
                }
            }
            catch (SQLException ex)
            {
            }
        }
        
        if (s == null)
        {
            s = super.toString();
        }
        
        return s;
    }

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException 
	{
		return iface.isAssignableFrom(getClass()) || (con !=null && con.isWrapperFor(iface));
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if (iface.isAssignableFrom(getClass())) 
        {
            return iface.cast(this);
        }
        else if(con != null)
        {
        	if (iface.isAssignableFrom(con.getClass()))
            {
                return iface.cast(con);
            } 
            else 
            {
                return con.unwrap(iface);
            }
        }
		
		return null;
	}
	
	protected void checkOpen() throws SQLException 
	{
        if(con == null)
        {
            throw new SQLException("Connection is closed.");
        }
    }

	@Override
	public void clearWarnings() throws SQLException 
	{
		checkOpen();
		con.clearWarnings();
	}

	@Override
	public void close() throws SQLException
	{
		if(con != null)
		{
		    pool.free(con);
		    con = null;
		}
	}

	@Override
	public void commit() throws SQLException
	{
		checkOpen();
		con.commit();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException
	{
		checkOpen();
		return con.createArrayOf(typeName, elements);
	}

	@Override
	public Blob createBlob() throws SQLException 
	{
		checkOpen();
		return con.createBlob();
	}

	@Override
	public Clob createClob() throws SQLException 
	{
		checkOpen();
		return con.createClob();
	}

	@Override
	public NClob createNClob() throws SQLException 
	{
		checkOpen();
		return con.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException
	{
		checkOpen();
		return con.createSQLXML();
	}

	@Override
	public Statement createStatement() throws SQLException
	{
		checkOpen();
		return con.createStatement();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException 
	{
		checkOpen();
		return con.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException
	{
		checkOpen();
		return con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException
	{
		checkOpen();
		return con.createStruct(typeName, attributes);
	}

	@Override
	public boolean getAutoCommit() throws SQLException 
	{
		checkOpen();
		return con.getAutoCommit();
	}

	@Override
	public String getCatalog() throws SQLException
	{
		checkOpen();
		return con.getCatalog();
	}

	@Override
	public Properties getClientInfo() throws SQLException
	{
		checkOpen();
		return con.getClientInfo();
	}

	@Override
	public String getClientInfo(String name) throws SQLException
	{
		checkOpen();
		return con.getClientInfo(name);
	}

	@Override
	public int getHoldability() throws SQLException 
	{
		checkOpen();
		return con.getHoldability();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException 
	{
		checkOpen();
		return con.getMetaData();
	}

	@Override
	public int getTransactionIsolation() throws SQLException 
	{
		checkOpen();
		return con.getTransactionIsolation();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException 
	{
		checkOpen();
		return con.getTypeMap();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		checkOpen();
		return con.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException
	{		
		return con == null || con.isClosed();
	}

	@Override
	public boolean isReadOnly() throws SQLException
	{
		checkOpen();
		return con.isReadOnly();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		checkOpen();
		return con.isValid(timeout);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException
	{
		checkOpen();
		return con.nativeSQL(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException 
	{
		checkOpen();
		return con.prepareCall(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		checkOpen();
		return con.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException 
	{
		checkOpen();
		return con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException 
    {
		checkOpen();
		return con.prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException 
	{
		checkOpen();
		return con.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException 
	{
		checkOpen();
		return con.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException 
	{
		checkOpen();
		return con.prepareStatement(sql, columnNames);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException 
	{
		checkOpen();
		return con.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException 
	{
		checkOpen();
		return con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException 
	{
		checkOpen();
		con.releaseSavepoint(savepoint);
	}

	@Override
	public void rollback() throws SQLException 
	{
		checkOpen();
		con.rollback();
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException 
	{
		checkOpen();
		con.rollback(savepoint);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException 
	{
		checkOpen();
		con.setAutoCommit(autoCommit);
	}

	@Override
	public void setCatalog(String catalog) throws SQLException 
	{
		checkOpen();
		con.setCatalog(catalog);
	}
	
	private static final Map<String, ClientInfoStatus> EMPTY_FAILED_PROPERTIES =
	        Collections.<String, ClientInfoStatus>emptyMap();

	@Override
	public void setClientInfo(Properties properties)
			throws SQLClientInfoException 
	{
		try 
		{
            checkOpen();
            con.setClientInfo(properties);
        }
        catch (SQLClientInfoException e)
        {
            throw e;
        }
        catch (SQLException e) 
        {
            throw new SQLClientInfoException("Connection is closed.", EMPTY_FAILED_PROPERTIES, e);
        }
	}

	@Override
	public void setClientInfo(String name, String value)
			throws SQLClientInfoException 
	{
		try 
		{
            checkOpen();
            con.setClientInfo(name, value);
        }
        catch (SQLClientInfoException e) 
        {
            throw e;
        }
        catch (SQLException e) 
        {
            throw new SQLClientInfoException("Connection is closed.", EMPTY_FAILED_PROPERTIES, e);
        }
	}

	@Override
	public void setHoldability(int holdability) throws SQLException 
	{
		checkOpen();
		con.setHoldability(holdability);
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException 
	{
		checkOpen();
		con.setReadOnly(readOnly);
	}

	@Override
	public Savepoint setSavepoint() throws SQLException 
	{
		checkOpen();
		return con.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException 
	{
		checkOpen();
		return con.setSavepoint(name);
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException 
	{
		checkOpen();
		con.setTransactionIsolation(level);
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException 
	{
		checkOpen();
		con.setTypeMap(map);
	}

	public void setSchema(String schema) throws SQLException
	{
		throw new UnsupportedOperationException();
		//checkOpen();
		//con.setSchema(schema);
	}

	public String getSchema() throws SQLException 
	{
		throw new UnsupportedOperationException();
		//checkOpen();
		//return con.getSchema();
	}

	public void abort(Executor executor) throws SQLException 
	{
		throw new UnsupportedOperationException();
		//checkOpen();
		//con.abort(executor);
	}

	public void setNetworkTimeout(Executor executor, int milliseconds)
			throws SQLException 
	{
		throw new UnsupportedOperationException();
		//checkOpen();
		//con.setNetworkTimeout(executor, milliseconds);
	}

	public int getNetworkTimeout() throws SQLException 
	{
		throw new UnsupportedOperationException();
		//checkOpen();
		//return con.getNetworkTimeout();
	}
		
}
