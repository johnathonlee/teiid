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

package org.teiid.query.processor.relational;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teiid.api.exception.query.ExpressionEvaluationException;
import org.teiid.common.buffer.BlockedException;
import org.teiid.common.buffer.BufferManager;
import org.teiid.common.buffer.TupleBatch;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.types.ClobType;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.types.TransformationException;
import org.teiid.query.execution.QueryExecPlugin;
import org.teiid.query.processor.ProcessorDataManager;
import org.teiid.query.sql.lang.TextTable;
import org.teiid.query.sql.lang.TextTable.TextColumn;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.util.CommandContext;

/**
 * Handles text file processing.
 * 
 * TODO: unix style escape handling \t \n, etc. 
 * TODO: parse using something more memory safe than strings
 * TODO: allow for escaping with fixed parsing
 * TODO: allow for fixed parsing without new lines
 * TODO: allow for a configurable line terminator
 */
public class TextTableNode extends SubqueryAwareRelationalNode {

	private TextTable table;
	
	//initialized state
	private int skip = 0;
	private int header = -1;
	private boolean noQuote;
	private char quote;
	private char delimiter;
	private int lineWidth;
	private Map elementMap;
    private int[] projectionIndexes;
	
    //per file state
	private BufferedReader reader;
	private int textLine = 0;
	private Map<String, Integer> nameIndexes;
	
	public TextTableNode(int nodeID) {
		super(nodeID);
	}
	
	@Override
	public void initialize(CommandContext context, BufferManager bufferManager,
			ProcessorDataManager dataMgr) {
		super.initialize(context, bufferManager, dataMgr);
		if (elementMap != null) {
			return;
		}
		if (table.getSkip() != null) {
			skip = table.getSkip();
		}
		if (table.getHeader() != null) {
			skip = Math.max(table.getHeader(), skip);
			header = table.getHeader() - 1;
		}
		if (table.isFixedWidth()) {
			for (TextColumn col : table.getColumns()) {
				lineWidth += col.getWidth();
			}
		} else {
			if (table.getDelimiter() == null) {
				delimiter = ',';
			} else {
				delimiter = table.getDelimiter();
			}
			if (table.getQuote() == null) {
				quote = '"';
			} else {
				noQuote = table.isEscape();
				quote = table.getQuote();
			}
		}
        this.elementMap = createLookupMap(table.getProjectedSymbols());
        this.projectionIndexes = getProjectionIndexes(elementMap, getElements());
	}
	
	@Override
	public void closeDirect() {
		super.closeDirect();
		reset();
	}
	
	@Override
	public void reset() {
		super.reset();
		if (this.reader != null) {
			try {
				this.reader.close();
			} catch (IOException e) {
			}
			this.reader = null;
		}
		this.nameIndexes = null;
		this.textLine = 0;
	}
	
	public void setTable(TextTable table) {
		this.table = table;
	}

	@Override
	public TextTableNode clone() {
		TextTableNode clone = new TextTableNode(getID());
		this.copy(this, clone);
		clone.setTable(table);
		return clone;
	}

	@Override
	protected TupleBatch nextBatchDirect() throws BlockedException,
			TeiidComponentException, TeiidProcessingException {
		
		if (reader == null) {
			initReader();
		}

		if (reader == null) {
			terminateBatches();
			return pullBatch();
		}
		
		while (!isBatchFull()) {
			String line = readLine();
			
			if (line == null) {
				terminateBatches();
				break;
			}
			
			List<String> vals = parseLine(line);
			
			List<Object> tuple = new ArrayList<Object>(projectionIndexes.length);
			for (int output : projectionIndexes) {
				TextColumn col = table.getColumns().get(output);
				String val = null;
				int index = output;
				if (nameIndexes != null) {
					index = nameIndexes.get(col.getName());
				}
				if (index >= vals.size()) {
					throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.no_value", col.getName(), textLine)); //$NON-NLS-1$
				}
				val = vals.get(index);
				try {
					tuple.add(DataTypeManager.transformValue(val, table.getColumns().get(output).getSymbol().getType()));
				} catch (TransformationException e) {
					throw new TeiidProcessingException(e, QueryExecPlugin.Util.getString("TextTableNode.conversion_error", col.getName(), textLine)); //$NON-NLS-1$
				}
			}
			addBatchRow(tuple);
		}
		
		return pullBatch();
	}

	private String readLine() throws TeiidProcessingException {
		while (true) {
			try {
				String line = reader.readLine();
				if (line != null) {
					textLine++;
					if (line.length() == 0) {
						continue;
					}
				}
				return line;
			} catch (IOException e) {
				throw new TeiidProcessingException(e);
			}
		}
	}

	private void initReader() throws ExpressionEvaluationException,
			BlockedException, TeiidComponentException, TeiidProcessingException {
		
		if (table.getCorrelatedReferences() != null) { 
			for (Map.Entry<ElementSymbol, Expression> entry : table.getCorrelatedReferences().asMap().entrySet()) {
				getContext().getVariableContext().setValue(entry.getKey(), getEvaluator(Collections.emptyMap()).evaluate(entry.getValue(), null));
			}
		}
		ClobType file = (ClobType)getEvaluator(Collections.emptyMap()).evaluate(table.getFile(), null);
		
		if (file == null) {
			return;
		}
		
		//get the reader
		try {
			Reader r = file.getCharacterStream();
			if (!(r instanceof BufferedReader)) {
				reader = new BufferedReader(r);
			} else {
				reader = (BufferedReader)r;
			}
		} catch (SQLException e) {
			throw new TeiidProcessingException(e);
		}
		
		//process the skip field
		if (skip <= 0) {
			return;
		}
		while (textLine < skip) {
			boolean isHeader = textLine == header;
			String line = readLine();
			if (line == null) { //just return an empty batch
				reset();
				return;
			}
			if (isHeader) {
				processHeader(parseLine(line));
			}
		}
	}

	private void processHeader(List<String> line) throws TeiidProcessingException {
		nameIndexes = new HashMap<String, Integer>();
		for (String string : line) {
			if (string == null) {
				continue;
			}
			nameIndexes.put(string.toUpperCase(), nameIndexes.size());
		}
		for (TextColumn col : table.getColumns()) {
			Integer index = nameIndexes.get(col.getName().toUpperCase());
			if (index == null) {
				throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.header_missing", col.getName())); //$NON-NLS-1$
			}
			nameIndexes.put(col.getName(), index);
		}
	}

	private List<String> parseLine(String line) throws TeiidProcessingException {
		if (table.isFixedWidth()) {
			return parseFixedWidth(line);
		} 
		return parseDelimitedLine(line);
	}

	private List<String> parseDelimitedLine(String line) throws TeiidProcessingException {
		ArrayList<String> result = new ArrayList<String>();
		StringBuilder builder = new StringBuilder();
		boolean escaped = false;
		boolean wasQualified = false;
		boolean qualified = false;
		while (true) {
			if (line == null) {
				if (escaped) {
					builder.append('\n'); //allow for escaped new lines
					escaped = false;
					line = readLine();
					continue;
				} 
				if (!qualified) {
					//close the last entry
					addValue(result, wasQualified, builder.toString());
					return result;
				} 
				line = readLine();
				if (line == null) {
					throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.unclosed")); //$NON-NLS-1$
				}
			}
			char[] chars = line.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				char chr = chars[i];
				if (chr == delimiter) {
					if (escaped || qualified) {
						builder.append(chr);
						escaped = false;
					} else {
						addValue(result, wasQualified, builder.toString());
						wasQualified = false;
						builder = new StringBuilder();  //next entry
					} 
				} else if (chr == quote) {
					if (noQuote) { 	//it's the escape char
						if (escaped) {
							builder.append(quote);
						} 
						escaped = !escaped;
					} else {
						if (qualified) {
							qualified = false;
						} else {
							if (wasQualified) {
								qualified = true;
								builder.append(chr);
							} else {
								if (builder.toString().trim().length() != 0) {
									throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.character_not_allowed", textLine)); //$NON-NLS-1$
								}
								qualified = true;
								builder = new StringBuilder(); //start the entry over
								wasQualified = true;
							}
						}
					}
				} else {
					if (escaped) {
						//don't understand other escape sequences yet
						throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.unknown_escape", chr, textLine)); //$NON-NLS-1$ 
					}
					if (wasQualified && !qualified) {
						if (!Character.isWhitespace(chr)) {
							throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.character_not_allowed", textLine)); //$NON-NLS-1$
						}
						//else just ignore
					} else {
						builder.append(chr);
					}
				}
			}
			line = null;
		}
	}

	private void addValue(ArrayList<String> result, boolean wasQualified, String val) {
		if (!wasQualified) {
			val = val.trim();
			if (val.length() == 0) {
				val = null;
			}
		}
		result.add(val);
	}

	private List<String> parseFixedWidth(String line)
			throws TeiidProcessingException {
		if (line.length() < lineWidth) {
			throw new TeiidProcessingException(QueryExecPlugin.Util.getString("TextTableNode.invalid_width", line.length(), lineWidth, textLine)); //$NON-NLS-1$
		}
		ArrayList<String> result = new ArrayList<String>();
		int beginIndex = 0;
		for (TextColumn col : table.getColumns()) {
			String val = line.substring(beginIndex, beginIndex + col.getWidth());
			addValue(result, false, val);
			beginIndex += col.getWidth();
		}
		return result;
	}
	
}
