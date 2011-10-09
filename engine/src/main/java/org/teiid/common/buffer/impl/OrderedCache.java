/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.common.buffer.impl;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class OrderedCache<K, V> {
	
	protected Map<K, V> map = new ConcurrentHashMap<K, V>(); 
	protected NavigableMap<V, K> expirationQueue = new ConcurrentSkipListMap<V, K>();
	protected Map<K, V> limbo = new ConcurrentHashMap<K, V>();
		
	public V get(K key) {
		V result = map.get(key);
		if (result == null) {
			result = limbo.get(key);
		}
		if (result != null) {
			synchronized (result) {
				expirationQueue.remove(result);
				recordAccess(key, result, false);
				expirationQueue.put(result, key);
			}
		}
		return result;
	}
	
	public V remove(K key) {
		V result = map.remove(key);
		if (result != null) {
			synchronized (result) {
				expirationQueue.remove(result);
			}
		}
		return result;
	}
	
	public V put(K key, V value) {
		V result = map.put(key, value);
		if (result != null) {
			synchronized (result) {
				expirationQueue.remove(result);
			}
		}
		synchronized (value) {
			recordAccess(key, value, result == null);
			expirationQueue.put(value, key);
		}
		return result;
	}
	
	public V evict() {
		Map.Entry<V, K> entry = expirationQueue.pollFirstEntry();
		if (entry == null) {
			return null;
		}
		limbo.put(entry.getValue(), entry.getKey());
		return map.remove(entry.getValue());
	}
	
	public void finishedEviction(K key) {
		limbo.remove(key);
	}
	
	public int size() {
		return map.size();
	}
	
	protected abstract void recordAccess(K key, V value, boolean initial);
	
}
