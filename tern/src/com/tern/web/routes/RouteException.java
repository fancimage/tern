/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

public class RouteException extends RuntimeException
{
	public RouteException(String message)
	{
        super(message);
    }

    public RouteException(String message, Throwable cause) 
    {
        super(message, cause);
    }
}
