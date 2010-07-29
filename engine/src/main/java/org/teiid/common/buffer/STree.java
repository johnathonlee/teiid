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

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.teiid.common.buffer.SPage.SearchResult;
import org.teiid.core.TeiidComponentException;

/**
 * Self balancing search tree using skip list like logic
 * This has similar performance similar to a B+/-Tree,
 * but with fewer updates. 
 */
@SuppressWarnings("unchecked")
public class STree {

	private static final Random seedGenerator = new Random();

	protected int randomSeed;
	private int mask = 1;
	private int shift = 1;
	
	protected SPage[] header = new SPage[] {new SPage(this, true)};
    protected BatchManager keyManager;
    protected BatchManager leafManager;
    protected Comparator comparator;
    protected int pageSize;
    protected int keyLength;
    protected String[] types;
    protected String[] keytypes;
    
    private AtomicInteger rowCount = new AtomicInteger();
	
	public STree(BatchManager manager,
			BatchManager leafManager,
            final Comparator comparator,
            int pageSize,
            int keyLength,
            String[] types) {
		randomSeed = seedGenerator.nextInt() | 0x00000100; // ensure nonzero
		this.keyManager = manager;
		this.leafManager = leafManager;
		this.comparator = comparator;
		this.pageSize = Math.max(pageSize, SPage.MIN_PERSISTENT_SIZE);
		pageSize >>>= 4;
		while (pageSize > 0) {
			pageSize >>>= 1;
			shift++;
			mask <<= 1;
			mask++;
		}
		this.keyLength = keyLength;
		this.types = types;
		this.keytypes = Arrays.copyOf(types, keyLength);
	}

	/**
	 * Determine a new random level using an XOR rng.
	 * 
	 * This uses the simplest of the generators described in George
     * Marsaglia's "Xorshift RNGs" paper.  This is not a high-quality
     * generator but is acceptable here.
	 * 
	 * See also the JSR-166 working group ConcurrentSkipListMap implementation.
	 * 
	 * @return
	 */
    private int randomLevel() {
        int x = randomSeed;
        x ^= x << 13;
        x ^= x >>> 17;
        randomSeed = x ^= x << 5;
        int level = 0;
        while ((x & mask) == mask) { 
        	++level;
        	x >>>= shift;
        }
        return level;
    }
    
	/**
	 * Search each level to find the pointer to the next level
	 * @param n
	 * @param places
	 * @return
	 * @throws IOException
	 * @throws TeiidComponentException 
	 */
	List find(List n, LinkedList<SearchResult> places) throws TeiidComponentException {
		SPage x = null;
		for (int i = header.length - 1; i >= 0; i--) {
			if (x == null) {
				x = header[i];
			}
			SearchResult s = SPage.search(x, n, places);
			if (places != null) {
				places.add(s);
			}
			if ((s.index == -1 && s.page == header[i]) || s.values.getTuples().isEmpty()) {
				x = null;
				continue; //start at the beginning of the next level
			}
			x = s.page;
			int index = s.index;
			boolean matched = true;
			if (index < 0) {
				matched = false;
				index = Math.max(0, -index - 2);
			}
			if (i == 0) {
				if (!matched) {
					return null;
				}
				return s.values.getTuples().get(index);
			}
			x = x.children.get(index);
		}
		return null;
	}
	
	public List insert(List tuple, boolean replace) throws TeiidComponentException {
		LinkedList<SearchResult> places = new LinkedList<SearchResult>();
		List match = find(tuple, places);
		if (match != null) {
			if (!replace) {
				return match;
			}
			SearchResult last = places.getLast();
			SPage page = last.page;
			last.values.getTuples().set(last.index, tuple);
			page.setValues(last.values);
			return match;
		}
		List key = extractKey(tuple);
		int level = randomLevel(); 
		assert header.length == places.size();
		if (level >= header.length) {
			header = Arrays.copyOf(header, level + 1);
		}
		rowCount.addAndGet(1);
		SPage page = null;
		for (int i = 0; i <= level; i++) {
			if (places.isEmpty()) {
				SPage newHead = new SPage(this, false);
				TupleBatch batch = newHead.getValues();
				batch.getTuples().add(key);
				newHead.setValues(batch);
				newHead.children.add(page);
				header[i] = newHead;
				page = newHead;
			} else {
				SearchResult result = places.removeLast();
				Object value = (i == 0 ? tuple : page);
				page = insert(key, result, places.peekLast(), value);
			}
		}
		return null;
	}
	
	List extractKey(List tuple) {
		return tuple.subList(0, keyLength);
	}

	SPage insert(List k, SearchResult result, SearchResult parent, Object value) throws TeiidComponentException {
		SPage page = result.page;
		int index = -result.index - 1;
		if (result.values.getTuples().size() == pageSize) {
			boolean leaf = !(value instanceof SPage);
			SPage nextPage = new SPage(this, leaf);
			TupleBatch nextValues = nextPage.getValues();
			nextValues.getTuples().addAll(result.values.getTuples().subList(pageSize/2, pageSize));
			result.values.getTuples().subList(pageSize/2, pageSize).clear();
			if (!leaf) {
				nextPage.children.addAll(page.children.subList(pageSize/2, pageSize));
				page.children.subList(pageSize/2, pageSize).clear();
			}
			nextPage.next = page.next;
			nextPage.prev = page;
			if (nextPage.next != null) {
				nextPage.next.prev = nextPage;
			}
			page.next = nextPage;
			boolean inNext = false;
			if (index <= pageSize/2) {
				setValue(index, k, value, result.values, page);
			} else {
				inNext = true;
				setValue(index - pageSize/2, k, value, nextValues, nextPage);
			}
			nextPage.setValues(nextValues);
			page.setValues(result.values);
			if (parent != null) {
				List min = nextPage.getValues().getTuples().get(0);
				SPage.correctParents(parent.page, min, page, nextPage);
			}
			if (inNext) {
				page = nextPage;
			}
		} else {
			setValue(index, k, value, result.values, page);
			page.setValues(result.values);
		}
		return page;
	}
	
	static void setValue(int index, List key, Object value, TupleBatch values, SPage page) {
		if (value instanceof SPage) {
			values.getTuples().add(index, key);
			page.children.add(index, (SPage) value);
		} else {
			values.getTuples().add(index, (List)value);
		}
	}
	
	public List remove(List key) throws TeiidComponentException {
		LinkedList<SearchResult> places = new LinkedList<SearchResult>();
		List tuple = find(key, places);
		if (tuple == null) {
			return null;
		}
		rowCount.addAndGet(-1);
		for (int i = 0; i < header.length; i++) {
			SearchResult searchResult = places.removeLast();
			if (searchResult.index < 0) {
				continue;
			}
			searchResult.values.getTuples().remove(searchResult.index);
			if (searchResult.page.children != null) {
				searchResult.page.children.remove(searchResult.index);
			}
			int size = searchResult.values.getTuples().size();
			if (size == 0) {
				if (header[i] != searchResult.page) {
					searchResult.page.remove();
					if (searchResult.page.next != null) {
						searchResult.page.next.prev = searchResult.page.prev;
					}
					searchResult.page.prev.next = searchResult.page.next;
					searchResult.page.next = header[i];
					searchResult.page.prev = null;
					continue;
				}
				header[i].remove();
				if (header[i].next != null) {
					header[i] = header[i].next;
					header[i].prev = null;
				} else {
					if (i != 0) {
						header = Arrays.copyOf(header, i);
						break;
					}
					header[0] = new SPage(this, true);
				}
				continue;
			} else if (size < pageSize/2) {
				//check for merge
				if (searchResult.page.next != null) {
					TupleBatch nextValues = searchResult.page.next.getValues();
					if (nextValues.getTuples().size() < pageSize/4) {
						SPage.merge(places, nextValues, searchResult.page, searchResult.values);
						continue;
					}
				}
				if (searchResult.page.prev != null) {
					TupleBatch prevValues = searchResult.page.prev.getValues();
					if (prevValues.getTuples().size() < pageSize/4) {
						SPage.merge(places, searchResult.values, searchResult.page.prev, prevValues);
						continue;
					}
				}
			}
			searchResult.page.setValues(searchResult.values);
		}
		return tuple;
	}
	
	public void remove() {
		truncate();
		this.keyManager.remove();
		this.leafManager.remove();
	}

	public int getRowCount() {
		return this.rowCount.get();
	}
	
	public int truncate() {
		int oldSize = rowCount.getAndSet(0);
		for (int i = 0; i < header.length; i++) {
			SPage page = header[i];
			while (page != null) {
				page.remove();
				page = page.next;
			}
		}
		header = new SPage[] {new SPage(this, true)};
		return oldSize;
	}
	
	public int getHeight() {
		return header.length;
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = header.length -1; i >= 0; i--) {
			SPage page = header[i];
			result.append("Level ").append(i).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
			while (page != null) {
				result.append(page);
				result.append(", "); //$NON-NLS-1$
				page = page.next;
			}
			result.append("\n"); //$NON-NLS-1$
		}
		return result.toString();
	}
	
	public int getKeyLength() {
		return keyLength;
	}
	
}