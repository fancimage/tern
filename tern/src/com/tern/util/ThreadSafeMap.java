/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThreadSafeMap<K, V> implements Map<K, V> 
{
	private volatile HashMap<K, V> map = new HashMap<K, V>();
    private final Object lock = new Object();

    public void clear() 
    {
        synchronized (lock) 
        {
            map = new HashMap<K, V>();
        }
    }

    public Object clone() 
    {
        return map.clone();
    }

    public boolean containsKey(Object key) 
    {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return map.containsValue(value);
    }

    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        return map.entrySet();
    }

    public boolean equals(Object o) 
    {
        return map.equals(o);
    }

    public V get(Object key) 
    {
        return map.get(key);
    }

    public int hashCode() 
    {
        return map.hashCode();
    }

    public boolean isEmpty() 
    {
        return map.isEmpty();
    }

    public Set<K> keySet() 
    {
        return map.keySet();
    }

    public V put(K key, V value) 
    {
        synchronized (lock) 
        {
            HashMap<K, V> _map = new HashMap<K, V>(map);
            V result = _map.put(key, value);
            map = _map;
            return result;
        }
    }

    public void putAll(Map<? extends K, ? extends V> m) 
    {
        synchronized (lock) 
        {
            HashMap<K, V> _map = new HashMap<K, V>(map);
            _map.putAll(m);
            map = _map;
        }
    }

    public V remove(Object key) 
    {
        synchronized (lock) 
        {
            HashMap<K, V> _map = new HashMap<K, V>(map);
            V result = _map.remove(key);
            map = _map;
            return result;
        }
    }

    public int size() 
    {
        return map.size();
    }

    public String toString() 
    {
        return map.toString();
    }

    public Collection<V> values() {
        return map.values();
    }
}
