package com.tern.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.tern.util.Convert;
import com.tern.util.TernContext;
import com.tern.util.config;

public class YamlModelReader extends ModelReader
{	
	@Override
	public boolean read(Model m) throws ModelException
	{
		this.model = m;
		
		String path = Convert.replaceAll(model.getFullName(), ".", "/");  
    	
    	String spath = TernContext.current().getResourcePath();    	
    	java.io.File file = new java.io.File(spath+"/models/data/"+path+".m");
    	if(!file.exists())
    	{
    		if(!spath.equals(config.getConfigurationPath()))
    		{
    			file = new java.io.File(config.getConfigurationPath()+"/models/data/"+path+".m");
    			if(!file.exists())
    			{
    				return false;
    			}
    		}
    		else return false;
    	}
    	
    	Yaml yaml = new Yaml();
    	java.io.Reader in = null;
    	Map data = null;
    	
    	try
    	{
    		in = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), 
    				config.getEncoding()));
    		data = (Map)yaml.load(in);    		
    	}
    	catch(Exception e)
    	{
    		//Trace.out(Trace.Error, "load model:"+this._fullname,e);
    		throw new ModelException(model, 
    				String.format("load model(%s) failed: %s",
    				model.getFullName(), e.getMessage()) );    
    	}
    	finally
    	{
    		if(in != null) try{in.close();}catch(Exception e){}
    	}
    	
    	if(data == null) return false;
    	
    	//caption
    	this.setCaption( Convert.toString(data.get("caption")) );    
    	
    	//representation    	
    	if(data.containsKey("repr"))
    	{
    		this.setRepresentation( Convert.toString(data.get("repr")) );    		
    	}
    	
    	//name
    	this.setName( Convert.toString(data.get("name")) );
    	
    	//columns
    	List<Column> columns = null;
    	if(data.containsKey("columns"))
    	{
    		Object obj = data.get("columns");
    		if(!(obj instanceof List))
    		{
    			//Trace.out(Trace.Error, "model("+this._fullname+"): columns atrribute should be list.");
    			throw new ModelException(model,"columns atrribute should be list.");
    			//return false;
    		}
    		
    		columns = new ArrayList<Column>();
    		for(Object c : ((List)obj))
        	{
        		if(c instanceof Map)
        		{    			    		
        			columns.add( readColumn((Map)c) );
        		}
        		else
        		{
        			//return false;
        			throw new ModelException(model,"wrong column format.");
        		}    		
        	}
    	}    	    	
    	
       	if(null == columns)
    	{
    		return false;
    	}
    	
    	//columns
    	this.setColumns(columns);
    	
    	//relations
    	if(data.containsKey("relations"))
    	{
    		Object obj = data.get("relations");
    		
    		if(obj instanceof List)
    		{
    			List rels = (List)obj;
    			for(Object o : rels)
    			{
    				if(o instanceof Map)
    				{
    					addRelation((Map)o);
    				}
    				else
    				{
    					throw new ModelException(model,"wrong relation definition.");
    				}
    			}
    		}
    		else if(obj instanceof Map)
    		{
    			addRelation((Map)obj);
    		}
    		else
    		{
    			throw new ModelException(model,"wrong relations definition.");
    		}
    	}
    	
    	return true;
	}
}

