/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.util.List;

public abstract class AbstractRowMapper<T> implements RowMapper<T>
{
    public List<T> createList()
    {
    	return new java.util.ArrayList<T>();
    }
}
