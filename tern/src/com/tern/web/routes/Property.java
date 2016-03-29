/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.tern.util.ThreadSafeMap;

abstract class Property 
{
	// Maps names to properties
	private static Map<String, Property> propertiesCache = new ThreadSafeMap<String, Property>();
	// Results of checking for property
    private static Map<String, Boolean> searchCache = new ThreadSafeMap<String, Boolean>(); 
    
	abstract public String getName();
	abstract public void setValue(Object self, Object value);      
	abstract public Object getValue(Object self);    
	abstract public Class getType();
	
	 public static Property getProperty(Class clazz, String name)
	 {
	        if (name.indexOf('.') != -1)
	        {
	            String key = clazz.getName() + "#" + name;
	            Property property = propertiesCache.get(key);
	            
	            if (property == null)
	            {
	                property = new CompositeProperty(clazz, name);
	                addProperty(key, property);
	                return property;
	            }
	            else
	            {
	                return property;
	            }
	        } 
	        else 
	        {
	            String key = clazz.getName() + "#" + name;
	            Property property = propertiesCache.get(key);
	            
	            if (property == null) 
	            {
	                property = new SimpleProperty(clazz, name);
	                addProperty(key, property);
	                return property;
	            }
	            else
	            {
	                return property;
	            }
	        }
	    }

	    private static void addProperty(String key, Property property) 
	    {
	        propertiesCache.put(key, property);
	        propertiesCache.put(key.toUpperCase(), property);
	    }
	    
	    public static boolean hasProperty(Class clazz, String name) 
	    {
	        String key = clazz.getName() + "#" + name;
	        if (searchCache.containsKey(key)) 
	        {
	            return searchCache.get(key).booleanValue();
	        }
	        else
	        {
	            boolean found = true;
	            try
	            {
	                getProperty(clazz, name);
	            }
	            catch (Throwable ex) 
	            {
	                found = false;
	            }
	            
	            searchCache.put(key, found);
	            return found;
	        }
	    }
}

final class SimpleProperty extends Property 
{
    private Method readMethod;
    private Method writeMethod;
    private Field field;
    private Class type;
    private String name = "";
    private boolean isBoolean  = false;
    
    public SimpleProperty(Class clazz, String name) 
    {
        this.name = name;
        
        Class[] readParams = {};
        readMethod = Reflection.getMethod(clazz, "get" + name, readParams);
        if (readMethod == null) 
        {
            readMethod = Reflection.getMethod(clazz, "is" + name, readParams);
        }
        
        if (readMethod == null) 
        {
            String fieldName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            
            field = Reflection.getField(clazz, fieldName);
            
            if (field == null) 
            {
//                throw new RuntimeException("Missing Method: get" + name + " " + fieldName + " " + clazz);
            }
            else
            {
                field.setAccessible(true);
                type = field.getType();
            }
        } 
        else
        {
            type = readMethod.getReturnType();
        }

        if (type == null && writeMethod != null)
        {
            type = writeMethod.getReturnType();
        }
        
        Class[] writeParams = {type};
        writeMethod = Reflection.getMethod(clazz, "set" + name, writeParams);

        if (writeMethod == null && field == null)
        {
//            throw new RuntimeException("Missing Method: set" + name);
        }
        
        if (type != null) 
        {
            isBoolean = type.equals(Boolean.class) || type.equals(boolean.class);
        }
    }

    public void setValue(Object self, Object value) 
    {
        if (isBoolean && value instanceof Number) 
        {
            value = ((Number)value).longValue() > 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        
        try 
        {
            if (writeMethod != null)
            {
                writeMethod.invoke(self, new Object[]{value});
            }
            else if (field != null)
            {
                field.set(self, value);
            }
            else 
            {
                throw new RuntimeException("Field or field setter missing: " + name);
            }
        } 
        catch (Exception ex)
        {
            throw new RuntimeException("Cannot set value of field: " + name, ex);
        }
    }
 
    public Object getValue(Object self) 
    {
        try
        {
            Object[] params = {};
            if (readMethod != null) 
            {
                return readMethod.invoke(self, params);
            }
            else if (field != null)
            {
                return field.get(self);
            }
            else
            {
                throw new RuntimeException("Field or field getter missing: " + name);
            }
        } 
        catch (InvocationTargetException ex) 
        {
            throw new RuntimeException(ex);
        }
        catch (IllegalAccessException ex) 
        {
            throw new RuntimeException(ex);
        }
    }

    public String getName() 
    {
        return name;
    }

    public Class getType() 
    {
        return type;
    }
}

final class CompositeProperty extends Property 
{
    private String name = "";
    private Property property;
    private Property valueProperty;
    
    public CompositeProperty(Class clazz, String name) 
    {
        this.name = name;
        
        int index = name.indexOf('.');
        String first = name.substring(0, index);
        String last  = name.substring(index + 1);

        property = Property.getProperty(clazz, first);       
        valueProperty = Property.getProperty(property.getType(), last); 
    }

    public void setValue(Object self, Object value) 
    {
        Object field = property.getValue(self);
        if (field == null) 
        {
            try
            {
                field = property.getType().newInstance();
                property.setValue(self, field);
            }
            catch (Exception ex) 
            {            	
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        
        valueProperty.setValue(field, value);
    }
 
    public Object getValue(Object self) 
    {
        Object value = property.getValue(self);
        if (value == null) 
        {
            return null;
        }
        return valueProperty.getValue(value);
    }

    public String getName() 
    {
        return name;
    }

    public Class getType() 
    {
        return property.getType();
    }
}