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

public class PathScanner 
{
    private enum Type {NUMBER, STRING, NAME, SYMBOL, TEXT, END};
    
    private PeekReader in;
    private StringBuffer out = new StringBuffer();
    private String token;
    private Type type;
    private boolean cached = false;
    
    public PathScanner(PeekReader in) 
    {
        this.in = in;
    }
    
    public void peek() throws IOException 
    {
        if (!cached) 
        {
            next();
        }
        cached = true;
    }
    
    public void read() throws IOException 
    {
        if (!cached)
        {
            next();
        }
        cached = false;
    }

    private void next() throws IOException 
    {
        nextRun();
    }

    private void nextRun() throws IOException 
    {
        out.setLength(0);

        type = null;
        token = null;

        int c = in.peek();
        if (c == -1) 
        {
            type = Type.END;
            return;
        }
        
        clearSpace();
        c = in.peek();
        
        if (c == -1) 
        {
            type = Type.END;
            return;
        }
        else if ("/.(){}[]$%|:*?".indexOf(c) > -1) 
        {
            type = Type.SYMBOL;
            out.append((char)in.read());
        }
        else if (Character.isDigit(c)) 
        {
            type = Type.NUMBER;
            parseNumber();
        } 
        else if (Character.isLetter(c) || c == '_' || c == '.') 
        {
            type = Type.NAME;
            parseName();
        }
        else 
        {
            error("Invalid character `" + (char)c + "'");
        }
    }
    
    public void error(String text) throws IOException 
    {
        throw new IOException("Line " + in.line() + ": " + text);
    }

    private void clearSpace() throws IOException 
    {
        int c = in.peek();
        while (Character.isWhitespace(c)) 
        {
            in.read();
            c = in.peek();
        }
    }

    private void parseName() throws IOException 
    {
        int c = in.peek();
        while (Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.') 
        {
            out.append((char)in.read());
            c = in.peek();
        }
    }

    private void parseNumber() throws IOException 
    {
        int c = in.peek();
        while (Character.isDigit(c) || c == '_') 
        {
            out.append((char)in.read());
            c = in.peek();
        }
    }
    
    public String getToken() 
    {
        if (token == null) 
        {
            token = out.toString();
        }
        return token;
    }
    
    public boolean isNumber()
    {
        return type == Type.NUMBER;
    }
    
    public boolean isName() 
    {
        return type == Type.NAME;
    }

    public boolean isEnd() 
    {
        return type == Type.END;
    }

    public boolean isSymbol(String text)
    {
        return type == Type.SYMBOL && getToken().equals(text);
    }

    public int line() 
    {
        return in.line();
    }
}
