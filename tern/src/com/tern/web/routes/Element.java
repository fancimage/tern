/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

import java.util.Map;

abstract class Element 
{
	abstract public boolean match(PathReader input);
    
    public int priority() 
    {
        return 10;
    }
}

class StaticElement extends Element 
{
    private String value;

    public StaticElement(String value) 
    {
        this.value = value;
    }

    @Override
    public boolean match(PathReader input) 
    {
    	if(input.isBind())
    	{
    		input.appendStatic(input.current());
    		input.next();
    		return true;
    	}
    	else
    	{
            String item = input.current();
            input.next();  //?
            return value.equals(item);
    	}
    }
    
    @Override
    public int priority()
    {
        return 4;
    }
    
    @Override
    public String toString() 
    {
        return "/" + value;
    }
}

abstract class ParameterElement extends Element
{
    private int parameter = -1;
    private String properties;
    private boolean optional;
    
    public int getParameter() 
    {
        return parameter;
    }
    
    public void setParameter(int parameter)
    {
        this.parameter = parameter;
    }
    
    public String getProperties()
    {
        return properties;
    }
    
    public void setProperties(String properties) 
    {
        if (properties != null && properties.length() != 0)
        {
            this.properties = properties;
        }
    }
    
    public boolean isOptional() 
    {
        return optional;
    }
    
    public void setOptional(boolean optional) 
    {
        this.optional = optional;
    }
    
    public void bind(PathReader input, Object value) 
    {
        if (parameter < 0) 
        {
            return;
        }

        if (properties != null) 
        {
            Object object = input.getParameter(parameter);
            
            if (object instanceof Map) 
            {
                ((Map)object).put(properties, value == null ? null : value.toString());
            } 
            else
            {
                Property property = Property.getProperty(object.getClass(), properties);
                if (property != null) 
                {
                    property.setValue(object, Reflection.convert(value, property.getType()));
                }
            }
        } 
        else
        {
            input.setParameter(parameter, value);
        }
    }
    
    @Override
    public int priority()
    {
        return 5;
    }
}

class IntegerElement extends ParameterElement 
{
    @Override
    public boolean match(PathReader input) 
    {
        if (input.hasNext()) 
        {
        	String item = input.current();
            try
            {
                long n = Long.parseLong(item);
                input.next();
                
                if (input.isBind()) 
                {
                    bind(input, n);
                }
            } 
            catch (NumberFormatException ex) 
            {
                return isOptional();
            }
        
            return true;
        }
        else
        {            
            return isOptional();
        }
    }
    
    @Override
    public int priority()
    {
        return 5;
    }
    
    @Override
    public String toString() 
    {
        return "/%";
    }
}

class StringElement extends ParameterElement 
{
    @Override
    public boolean match(PathReader input)
    {
        if (input.hasNext()) 
        {
        	if (input.isBind()) 
            {
                bind(input, input.current());
            }
            
            input.next();
            return true;   
        } 
        else 
        {            
            return isOptional();
        }
    }

    @Override
    public int priority() 
    {
        return 6;
    }
    
    @Override
    public String toString() 
    {
        return "/$";
    }
}

class StarElement extends ParameterElement 
{
    @Override
    public boolean match(PathReader input)
    {
    	String v = input.remaining();
        if (input.isBind()) 
        {
            bind(input, v);
        }
        
        input.consume();
        return true;
    }
    
    @Override
    public int priority()
    {
        return 20;
    }

    @Override
    public String toString() 
    {
        return "/*";
    }
}

class OptionsElement extends ParameterElement 
{
    private String[] values;

    public OptionsElement(String[] values) 
    {
        this.values = values;
    }

    @Override
    public boolean match(PathReader input) 
    {
        String item = input.current();
        
        for (String value : values) 
        {
            if (value.equals(item)) 
            {
                if (input.isBind()) 
                {
                    bind(input, item);
                }

                input.next();
                return true;
            }
        }
        return isOptional();
    }
    
    @Override
    public int priority() 
    {
        return 4;
    }
}

