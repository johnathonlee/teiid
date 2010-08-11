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

package org.teiid.common.buffer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.teiid.common.buffer.SPage.SearchResult;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;

/**
 * Implements intelligent browsing over a {@link STree}
 */
public class TupleBrowser implements TupleSource {
	
	private final STree tree;
	
	private List<List<Object>> valueSet;
	
	private SPage page;
	private int index;
	
	private SPage bound;
	private int boundIndex = -1;
	
	private TupleBatch values;
	private boolean updated;
	private boolean direction;

	/**
	 * Construct a value based browser
	 * @param sTree
	 * @param valueSet
	 * @param direction
	 */
	public TupleBrowser(STree sTree, List<List<Object>> valueSet, boolean direction) {
		this.tree = sTree;
		this.direction = direction;
		this.valueSet = valueSet;
	}
	
	/**
	 * Construct a range based browser
	 * @param sTree
	 * @param lowerBound
	 * @param upperBound
	 * @param direction
	 * @throws TeiidComponentException
	 */
	public TupleBrowser(STree sTree, List<Object> lowerBound, List<Object> upperBound, boolean direction) throws TeiidComponentException {
		this.tree = sTree;
		this.direction = direction;
		
		if (lowerBound != null) {
			setPage(lowerBound);
		} else {
			page = sTree.header[0];
		}
		
		boolean valid = true;
		
		if (upperBound != null) {
			if (lowerBound != null && this.tree.comparator.compare(upperBound, lowerBound) < 0) {
				valid = false;
			}
			LinkedList<SearchResult> places = new LinkedList<SearchResult>();
			this.tree.find(upperBound, places);
			SearchResult upper = places.getLast();
			bound = upper.page;
			boundIndex = upper.index;
			if (boundIndex < 0) {
				boundIndex = Math.min(upper.values.getTuples().size() - 1, -boundIndex -1);
			}
			if (!direction) {
				values = upper.values;
			}
		} else {
			while (bound == null || bound.children != null) {
				bound = tree.findChildTail(bound);
			}
			if (!direction) {
				if (page != bound || values == null) {
					values = bound.getValues();
				}
				boundIndex = values.getTuples().size() - 1;
			}
		}
				
		if (!direction) {
			SPage swap = page;
			page = bound;
			bound = swap;
			int upperIndex = boundIndex;
			boundIndex = index;
			index = upperIndex;
		}
		
		if (!valid) {
			page = null;
		}
	}

	private boolean setPage(List<?> lowerBound) throws TeiidComponentException {
		LinkedList<SearchResult> places = new LinkedList<SearchResult>();
		this.tree.find(lowerBound, places);
		
		SearchResult sr = places.getLast();
		page = sr.page;
		index = sr.index;
		boolean result = true;
		if (index < 0) {
			result = false;
			index = -index - 1;
		}
		values = sr.values;
		return result;
	}
	
	@Override
	public List<?> nextTuple() throws TeiidComponentException,
			TeiidProcessingException {
		for (;;) {
			//first check for value iteration
			if (valueSet != null) {
				if (valueSet.isEmpty()) {
					resetState();
					return null;
				}
				List<?> newValue = direction?valueSet.remove(0):valueSet.remove(valueSet.size() -1);
				if (values != null) {
					int possibleIndex = Collections.binarySearch(values.getTuples(), newValue, tree.comparator);
					if (possibleIndex >= 0) {
						//value exists in the current page
						index = possibleIndex;
						return values.getTuples().get(possibleIndex);
					}
					//check for end/terminal conditions
					if (direction && possibleIndex == -values.getTuples().size() -1) {
						if (page.next == null) {
							resetState();
							return null;
						}
					} else if (!direction && possibleIndex == -1) {
						if (page.prev == null) {
							resetState();
							return null;
						}
					} else {
						//the value simply doesn't exist
						continue;
					}
				}
				resetState();
				if (!setPage(newValue)) {
					continue;
				}
				return values.getTuples().get(index);
			}
			if (page == null) {
				return null;
			}
			if (values == null) {
				values = page.getValues();
				if (direction) {
					index = 0;
				} else {
					index = values.getTuples().size() - 1;
				}
			}
			if (index >= 0 && index < values.getTuples().size()) {
				List<?> result = values.getTuples().get(index);
				if (page == bound && index == boundIndex) {
					resetState();
					page = null; //terminate
				} else {
					index+=getOffset();
				}
				return result;
			}
			resetState();
			if (direction) {
				page = page.next;
			} else {
				page = page.prev;
			}
		}
	}

	private void resetState() throws TeiidComponentException {
		if (updated) {
			page.setValues(values);
		}
		updated = false;
		values = null;
	}
	
	private int getOffset() {
		if (valueSet != null) {
			return 0;
		}
		return direction?1:-1;
	}
	
	/**
	 * Perform an in-place update of the tuple just returned by the next method
	 * WARNING - this must not change the key value
	 * @param tuple
	 * @throws TeiidComponentException
	 */
	public void update(List<?> tuple) throws TeiidComponentException {
		values.getTuples().set(index - getOffset(), tuple);
		updated = true;
	}
	
	/**
	 * Notify the browser that the last value was deleted.
	 */
	public void removed() {
		index-=getOffset();
	}
	
	@Override
	public void closeSource() {
		
	}
	
}