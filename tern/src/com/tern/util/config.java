/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.Map;
import java.util.Properties;
import org.yaml.snakeyaml.Yaml;

import com.tern.dao.ModelException;

/**
 * <p>Title: Configuration</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright iEAS(c) 2008</p>
 * @author iEAS Fancimage
 * @version 1.0
 */
public class config
{    
    static String encoding = "utf-8";
    static String root;
    static boolean _isDebug = false;
    static String configPath;
    static String configFile;
    
    static Map<String,Object> data;
    //private Properties properties;
    
    //private final static config cfg = new config();

    private config()
    {
    }

    public static String getEncoding()
    {
        return encoding;
    }
    
    public static boolean isDebug()
    {
    	return _isDebug;
    }
    
    @SuppressWarnings("unchecked")
	final static public boolean load(String url,String encode)
    {
    	configFile = url;
    	if(encode!=null) encoding = encode;
    	    
    	return reload();
    }
    
    public static final boolean reload()
    {
    	if ((configFile == null) || (configFile.length() <= 0))
        {
            System.out.println("configuration path is empty!");
            return false;
        }
    	
    	java.io.File cfgFile = new java.io.File(configFile);
        if (!cfgFile.exists())
        {
            System.out.println("can not load configuaration file:" + configFile);
            return false;
        }
        
        configPath = cfgFile.getParent();
        
        Yaml yaml = new Yaml();
    	java.io.Reader in = null;
    	
    	try
    	{
    		in = new java.io.BufferedReader(
    				new java.io.InputStreamReader(new java.io.FileInputStream(cfgFile.getPath()), 
    						encoding));
    		data = (Map<String,Object>)yaml.load(in);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		return false;   
    	}
    	finally
    	{
    		if(in != null) try{in.close();}catch(Exception e){}
    	}
    	
    	_isDebug = Convert.toBoolean(get("debug",true), false);
    	String tmp = Convert.toStringIgnoreEmpty(get("encoding",true), null);
    	if(tmp != null) encoding = tmp;
    	
    	String level = getString("log.level");
    	if(null != level)
    	{
    		level = level.toLowerCase();
    		if(level.compareTo("info")==0)
            {
                 Trace.setTraceLevel(Trace.Information);
            }
            else if(level.compareTo("warning")==0)
            {
                 Trace.setTraceLevel(Trace.Warning);
            }
            else if(level.compareTo("running")==0)
            {
                 Trace.setTraceLevel(Trace.Running);
            }
            else if(level.compareTo("error")==0)
            {
                 Trace.setTraceLevel(Trace.Error);
            }
            else if(level.compareTo("fatal")==0)
            {
                 Trace.setTraceLevel(Trace.Fatal);
            }
    	}
    	    	        
    	//return loadConfig(url,true);
    	return true;
    }
        
    public static final boolean addConfig(String name,java.io.File cfgFile)
    {
    	/*if ((filepath == null) || (filepath.length() <= 0))
        {
            return false;
        }
    	
    	java.io.File cfgFile = new java.io.File(filepath);
        if (!cfgFile.exists())
        {
        	System.out.println("can not load configuaration file:" + filepath);
            return false;
        }*/
        
        if(data == null)
        {
        	System.out.println("error:please load main configuration first!load failed:"+cfgFile.getName());
        	return false;
        }
        
        if(data.containsKey(name))
        {
        	System.out.println("error:configration name has been exists!name:"+name);
        	return false;
        }
        
        Yaml yaml = new Yaml();
    	java.io.Reader in = null;
    	Map<String,Object> configData = null;
    	
    	try
    	{
    		in = new java.io.BufferedReader(
    				new java.io.InputStreamReader(new java.io.FileInputStream(cfgFile.getPath()), 
    						encoding));
    		configData = (Map<String,Object>)yaml.load(in);    		
    		data.put(name, configData);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		return false;   
    	}
    	finally
    	{
    		if(in != null) try{in.close();}catch(Exception e){}
    	}
    	
    	return true;
    }
    
    public static String getString(String key)
    {
    	Object o = get(key,false);
    	if(o instanceof String) return (String)o;
    	else return null;
    }
    
    /*public static <T> T get(String key)
    {
    	Object o = get(key,false);
    	
    	try
    	{
    		return (T)o;
    	}
    	catch(Throwable t)
    	{
    		return null;
    	}
    }*/
    
    public static int getInt(String key,int def)
    {
    	Object o = get(key,false);
    	if(o instanceof Integer)
    	{
    		return ((Integer)o).intValue();
    	}
    	else
    	{
    		return def;
    	}
    }
    
    public static long getLong(String key,long def)
    {
    	Object o = get(key,false);
    	if(o instanceof Integer)
    	{
    		return ((Integer)o).intValue();
    	}
    	else if(o instanceof Long)
    	{
    		return ((Long)o).longValue();
    	}
    	else
    	{
    		return def;
    	}
    }
    
    public static boolean getBool(String key,boolean def)
    {
    	Object o = get(key,false);
    	if(o instanceof Boolean)
    	{
    		return ((Boolean)o).booleanValue();
    	}
    	else
    	{
    		return def;
    	}
    }
    
    public static void remove(String key)
    {
    	get(key,true);
    }
    
    public static Map getMap(String key)
    {
    	Object o = get(key,false);
    	if(o instanceof Map) return (Map)o;
    	else return null;
    }
    
    public static Object[] getArray(String key)
    {
    	Object o = get(key,false);
    	
    	if(o instanceof java.util.List)
    	{
    		java.util.List list = (java.util.List)o;
    		Object[] ret = new Object[list.size()];
    		ret = list.toArray(ret);
    		
    		return ret;
    	}
    	else if(o instanceof Object[])
    	{
    		 return ( Object[])o;
    	}
    	else
    	{
    		return null;
    	}
    }
    
    private static void set(String key,String v)
    {
    	
    }
    
    private static Object get(String key,boolean consumed)
    {
    	if(key == null ) return null;
    	String[] arr = key.split("[.]");
    	
    	Map curr = data;
    	Map p = null;
    	for(int i=0;i<arr.length;i++)
    	{
    		if(curr.containsKey(arr[i]))
    		{
    			Object o = curr.get(arr[i]);
    			if(i == arr.length-1)
    			{
    				if(consumed)
    				{
    					curr.remove(arr[i]);
    					if(curr.size()<=0 && p != null)
    					{
    						p.remove(arr[i-1]);
    					}
    				}
    				return o;
    			}
    			else if(o instanceof Map)
    			{
    				p = curr;
    				curr = (Map)o;
    			}
    			else
    			{
    				return null;
    			}
    		}
    		else
    		{
    			return null;
    		}
    	}
    	
    	return null;
    }
    
    public static String getConfigurationPath()
    {
    	return configPath;
    }

    /*final private boolean loadConfig(String url,boolean isFirst)
    {
        java.io.File cfgFile = new java.io.File(url);
        if (!cfgFile.exists())
        {
            System.out.println("can not load configuaration file:" + url);
            return false;
        }
        
        if(isFirst){
            configPath = cfgFile.getParent();
        }
        
        encoding = System.getProperty("file.encoding");
        
        String path=null;
        if(url.lastIndexOf('/') >=0)
        {
        	path = url.substring(0,url.lastIndexOf('/'));
        }
        else if(url.lastIndexOf('\\') >=0)
        {
        	path = url.substring(0,url.lastIndexOf('\\'));
        }
        
        java.io.FileInputStream ins= null;
        Configuration cfg = Configuration.getInstance();
        java.util.Properties p = new java.util.Properties();
        try
        {
        	ins=new java.io.FileInputStream(cfgFile);
            p.load(ins);
            
            //处理一些特殊的配置项
            if(isFirst)
            {
            	String tmp = p.getProperty("SYS.ENCODING");
                if (tmp != null)
                {
                    encoding=tmp;
                    p.remove("SYS.ENCODING");
                }
                
                //日志级别
                tmp = p.getProperty("TRACE.LEVEL");
                if(tmp==null)
                {
                	tmp="running";
                }
                else
                {
                	tmp=tmp.trim().toLowerCase();
                	p.remove("TRACE.LEVEL");
                }
                
                System.out.println("Trace level:" + tmp);
                
                if(tmp.compareTo("infor")==0)
                {
                     Trace.setTraceLevel(Trace.Information);
                }
                else if(tmp.compareTo("warning")==0)
                {
                     Trace.setTraceLevel(Trace.Warning);
                }
                else if(tmp.compareTo("running")==0)
                {
                     Trace.setTraceLevel(Trace.Running);
                }
                else if(tmp.compareTo("error")==0)
                {
                     Trace.setTraceLevel(Trace.Error);
                }
                else if(tmp.compareTo("fatal")==0)
                {
                     Trace.setTraceLevel(Trace.Fatal);
                }
                
                //是否调试
                tmp = p.getProperty("SYS.Debug");
                if(tmp!=null && tmp.equalsIgnoreCase("true"))
                {
                	_isDebug = true;
                }
                else
                {
                	_isDebug = false;
                }
            }
            cfg.setProperties(p,path);
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error ocurrs while load configuration file:" + e.getMessage());
            return false;
        }
        finally
        {
            try
            {
                ins.close();
            }
            catch (Exception e)
            {}
        }
    }
    
    private boolean setProperties(Properties p,String path)
    {
        boolean ret = true;
        if (p != null)
        {
            if(this.properties==null)
            {
                this.properties=new Properties();
            }            
            
            try
            {
                //System.out.println("--------属性------------");
                java.util.Enumeration it = p.propertyNames();
                while (it.hasMoreElements())
                {
                    String name = it.nextElement().toString();
                    String value = p.getProperty(name);
                    value = new String(value.getBytes("iso-8859-1"), encoding);
                    
                    //处理子配置文件
                    if (name.equals("SYS.SubConfiguration") &&
                        value.length() > 0 && path != null)
                    {
                        String[] subs = value.split(";");
                        for (int i = 0; i < subs.length; i++)
                        {
                            ret = loadConfig(path+"/" + subs[i],false);
                        }
                    }                    
                    else
                    {
                        properties.setProperty(name, value);
                    }
                }
                //System.out.println("--------end 属性------------");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                ret=false;
            }
        }
        //this.properties = p;
        return ret;
    }
    
    */

    /**
     * 返回配置的唯一实例.
     * @return Configuration
     */
    /*public static config getInstance()
    {
        return cfg;
    }*/
    
    public static String getRoot()
    {
        return root;
    }
    
    public static void setRoot(String path)
    {    	
    	root = path;
    	if(root==null)
    	{
    		root = "/";
    	}
    	else
    	{
    		if(root.indexOf("\\") >= 0)
    		{
    			root = Convert.replaceAll(root, "\\", "/");
    		}
    		
    		if(!root.endsWith("/"))
    		{
    			root = root + "/";
    		}
    	}
    }

    /**
     * 返回指定key所对应的value
     * @param key:指定搜索的key
     * @return String
     */
    /*public String getValue(String key)
    {
        return properties.getProperty(key);
    }
    
    public void setValue(String key,String value)
    {
    	properties.setProperty(key, value);
    }

    public Properties asProperties()
    {
        return properties;
    }*/
}