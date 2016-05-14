/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tern.web.Controller;
import com.tern.web.Route;

public class RouteSet
{
	 //private static RouteSet root = new RouteSet();   //root of the Route-Treew	 
	
     private Map<String,List<Path>> paths = new HashMap<String,List<Path>>();     
     
     public RouteSet(){}
     
     /*
      * Add a controller to Route Set*/
     public void addController(Class<Controller> target,String defaultPath/*,String appName*/)
     {
    	 String[] paths = null;
    	 
    	 Route route = (Route)target.getAnnotation(Route.class);
    	 if(route != null)
    	 {
    		 paths = route.value();
    	 }
    	     	 
    	 if(paths == null || paths.length<=0)
    	 {
    		 if(defaultPath == null || defaultPath.length() <= 0)
    		 {
    			 return;
    		 }
    		 paths = new String[]{defaultPath};
    	 }
    	 
    	 for(String path:paths)
    	 {
    		 //if(appName!=null && appName.length()>0) path = appName+path;
    		 //exists?--search in root-set
    		 if (path != null && path.indexOf('(') >= 0)
    		 {
    			 for (String item : new PartCombinator(new PeekReader(path), null).values())
    			 {
    				 try
        			 {
        		         Path _path = new ControllerPath(target,item);
        		         this.addPath(_path);
        			 }
        			 catch(IOException e){}
    			 }
    		 }
    		 else
    		 {
    			 try
    			 {
    		         Path _path = new ControllerPath(target,path);
    		         this.addPath(_path);
    			 }
    			 catch(IOException e){}
    		 }
    	 }
     }
     
     public Object resolve(String path,String method )
     {
    	 int httpMethod = HttpMethod.parse(method);
    	 PathReader _path = new PathReader(path,httpMethod);
    	 
    	 return this.resolve(_path);
     }
     
     protected Object resolve(PathReader input)
     {
    	 String key = input.current();
         boolean topLevel = false;
         
         if (key == null)
         {
             key = "";
             topLevel = true;
         }
         
         List<Path> list = paths.get(key);
         if (list == null) 
         {
        	 if(!topLevel)
        	 {
                 list = paths.get("");
                 topLevel = true;
        	 }
        	 
        	 if(list == null)
        	 {
        		 return null;
        	 }
         }
         
         for(Path path:list)
         {
        	 Object ret = path.resolve(input);
        	 if(ret != null)
        	 {
        		 return ret;
        	 }
         }
         
         if (!topLevel) 
         {
             list = paths.get("");

             if (list == null) 
             {
                 return null;
             }
             
             for(Path path:list)
             {
            	 Object ret = path.resolve(input);
            	 if(ret != null)
            	 {
            		 return ret;
            	 }
             }
         }
         
    	 return null;
     }
     
     void addPath(Path path)
     {
    	 String key = path.getKey();
         
         List<Path> list = paths.get(key);
         if (list == null)
         {
             list = new ArrayList<Path>();
             paths.put(key, list);
         }
         
         list.add(path);
     }
     
     public void sort()
     {
         for (String key : paths.keySet()) 
         {
             List<Path> list = paths.get(key);
         
             if (list != null) 
             {
                 Collections.sort(list);
             }
         }
     }
     
     public Map<String,List<Path>> getPaths(){return paths;}
     
     /*public static void sort()
     {
    	 root._sort();
     }*/
}
