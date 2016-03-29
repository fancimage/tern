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
import java.lang.reflect.Method;

public class Reflection 
{
	public static Field getField(Class<?> clazz, String name)
	{
        while (clazz != null) 
        {
            for (Field field : clazz.getDeclaredFields()) 
            {
                if (field.getName().equals(name)) 
                {
                    field.setAccessible(true);
                    return field;
                }
            }

            clazz = clazz.getSuperclass();
        }

        return null;
    }
    
    public static Method getMethod(Class<?> self, String name, Class[] params)
    {
        Method method = null;

        for (int i = 0; i < params.length; i++) 
        {
            if (params[i] != null) 
            {
                if (params[i].equals(Integer.class)) 
                {
                    params[i] = int.class;
                }
                else if (params[i].equals(Double.class)) 
                {
                    params[i] = double.class;
                }
                else if (params[i].equals(Long.class)) 
                {
                    params[i] = long.class;
                }
                else if (params[i].equals(Boolean.class))
                {
                    params[i] = boolean.class;
                }
            }
        }

        try 
        {
            method = self.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } 
        catch (NoSuchMethodException ex) 
        {
            while (self != null)
            {
                Method[] methods = self.getDeclaredMethods();

                // FIXME: Later change to find best match
                for (int i = 0; i < methods.length; i++)
                {
                    if (!methods[i].getName().equals(name)) 
                    {
                        continue;
                    }

                    Class[] targetParams = methods[i].getParameterTypes();

                    if (isAssignable(targetParams, params)) 
                    {
                        methods[i].setAccessible(true);
                        return methods[i];
                    }
                }

                self = self.getSuperclass();
            }
        }

        return method;
    }

    private static boolean isAssignable(Class<?>[] formal, Class<?>[] actual) 
    {
        if (formal.length != actual.length) 
        {
            return false;
        }

        for (int i = 0; i < formal.length; i++) 
        {
            if (actual[i] == null) 
            {
                if ((formal[i].equals(int.class))
                    || (formal[i].equals(double.class))
                    || (formal[i].equals(boolean.class))) 
                {
                    return false;
                }
                
                continue;
            }

            if (!formal[i].isAssignableFrom(actual[i])) 
            {
                return false;
            }
        }

        return true;
    }
    
    public final static Object convert(Object value, Class<?> type)
	{
        if (value instanceof Long) 
        {
            if (type == int.class || type == Integer.class) 
            {
                return ((Long)value).intValue();
            }
            else if (type == short.class || type == Short.class) 
            {
                return ((Long)value).shortValue();
            }
            else if (type == byte.class || type == Byte.class) 
            {
                return ((Long)value).byteValue();
            }
        } 
        
        if (!(value instanceof Boolean) && (type == boolean.class || type == Boolean.class))
        {
            return value != null;
        }
        
        return value;
    }
    
}
