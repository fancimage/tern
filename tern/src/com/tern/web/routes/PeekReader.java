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
import java.io.Reader;
import java.io.StringReader;

public class PeekReader 
{
	private Reader in;
    private boolean cached = false;
    private int value;
    private int line = 1;
    
    public PeekReader(String text) 
    {
        this.in = new StringReader(text);
    }
    
    private int next() throws IOException 
    {
        int c = in.read();
        if (c == '\n')
        {
            line++;
        }
        
        return c;
    }

    public int read() throws IOException 
    {
        if (cached) 
        {
            cached = false;
            return value;
        } 
        else 
        {
            return next();
        }
    }

    public int peek() throws IOException
    {
        if (!cached) 
        {
            cached = true;
            value = next();
        }
        return value;
    }
    
    public int line()
    {
        return line;
    }
}
