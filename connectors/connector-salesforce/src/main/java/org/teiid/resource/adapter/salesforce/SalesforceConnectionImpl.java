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
package org.teiid.resource.adapter.salesforce;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.resource.ResourceException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.resource.spi.BasicConnection;
import org.teiid.translator.salesforce.SalesforceConnection;
import org.teiid.translator.salesforce.execution.DataPayload;
import org.teiid.translator.salesforce.execution.DeletedObject;
import org.teiid.translator.salesforce.execution.DeletedResult;
import org.teiid.translator.salesforce.execution.UpdatedResult;

import com.sforce.soap.partner.CallOptions;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DeletedRecord;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.GetDeletedResult;
import com.sforce.soap.partner.GetUpdatedResult;
import com.sforce.soap.partner.InvalidFieldFault;
import com.sforce.soap.partner.InvalidIdFault;
import com.sforce.soap.partner.LoginFault;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.MalformedQueryFault;
import com.sforce.soap.partner.MruHeader;
import com.sforce.soap.partner.ObjectFactory;
import com.sforce.soap.partner.PackageVersionHeader;
import com.sforce.soap.partner.QueryOptions;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.SessionHeader;
import com.sforce.soap.partner.SforceService;
import com.sforce.soap.partner.Soap;
import com.sforce.soap.partner.sobject.SObject;

public class SalesforceConnectionImpl extends BasicConnection implements SalesforceConnection {
	private SforceService sfService;
	private Soap sfSoap;
	private SessionHeader sh;
	private CallOptions co;
	
	private ObjectFactory partnerFactory = new ObjectFactory();
	
	PackageVersionHeader pvHeader = partnerFactory.createPackageVersionHeader();
	
	public SalesforceConnectionImpl(String username, String password, URL url) throws ResourceException {
		login(username, password, url);
	}
	
	String getUserName() throws ResourceException {
			try {
				return sfSoap.getUserInfo(sh).getUserName();
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
	}
	
	Soap getBinding() {
		return sfSoap;
	}
	
	private void login(String username, String password, URL url) throws ResourceException {
		if (!isValid()) {
			LoginResult loginResult = null;
			sfSoap = null;
			sfService = null;
			co = new CallOptions();
			// This value identifies Teiid as a SF certified solution.
			// It was provided by SF and should not be changed.
			co.setClient("RedHat/MetaMatrix/"); //$NON-NLS-1$
			
			if(url == null) {
				throw new ResourceException("SalesForce URL is not specified, please provide a valid URL"); //$NON-NLS-1$
			}

			try {
				sfService = new SforceService();
				sfSoap = sfService.getSoap();
				((BindingProvider)sfSoap).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url.toExternalForm());
				loginResult = sfSoap.login(username, password);
			} catch (LoginFault e) {
				throw new ResourceException(e);
			} catch (InvalidIdFault e) {
				throw new ResourceException(e);
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
			LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Login was successful for username " + username); //$NON-NLS-1$

			sh = new SessionHeader();
			sh.setSessionId(loginResult.getSessionId());
			// Reset the SOAP endpoint to the returned server URL
			((BindingProvider)sfSoap).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,loginResult.getServerUrl());
			// or maybe org.apache.cxf.message.Message.ENDPOINT_ADDRESS
			((BindingProvider)sfSoap).getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY,Boolean.TRUE);
			// Set the timeout.
			//((BindingProvider)sfSoap).getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, timeout);

			
			// Test the connection.
			try {
				sfSoap.getUserInfo(sh);
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
		}
	}
	
	
	public boolean isValid() {
		boolean result = true;
		if(sfSoap == null) {
			result = false;
		} else {
			try {
				sfSoap.getServerTimestamp(sh);
			} catch (Throwable t) {
				LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Caught Throwable in isAlive", t); //$NON-NLS-1$
				result = false;
			}
		}
		return result;
	}

	public QueryResult query(String queryString, int batchSize, Boolean queryAll) throws ResourceException {
		
		if(batchSize > 2000) {
			batchSize = 2000;
			LogManager.logDetail(LogConstants.CTX_CONNECTOR, "reduced.batch.size"); //$NON-NLS-1$
		}
		
		QueryResult qr = null;
		QueryOptions qo = partnerFactory.createQueryOptions();
		qo.setBatchSize(batchSize);
		try {
			if(queryAll) {
				qr = sfSoap.queryAll(queryString, sh);
			} else {
				MruHeader mruHeader = partnerFactory.createMruHeader();
				mruHeader.setUpdateMru(false);
				
				qr = sfSoap.query(queryString, sh);
			}
		} catch (InvalidFieldFault e) {
			throw new ResourceException(e);
		} catch (MalformedQueryFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
			throw new ResourceException(e);
		} catch (InvalidIdFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.InvalidQueryLocatorFault e) {
			throw new ResourceException(e);
		}
		return qr;
	}

	public QueryResult queryMore(String queryLocator, int batchSize) throws ResourceException {
		QueryOptions qo = partnerFactory.createQueryOptions();
		qo.setBatchSize(batchSize);
		try {
			return sfSoap.queryMore(queryLocator, sh);
		} catch (InvalidFieldFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.InvalidQueryLocatorFault e) {
			throw new ResourceException(e);
		}
		
	}

	public int delete(String[] ids) throws ResourceException {
		List<DeleteResult> results = null;
		try {
			results = sfSoap.delete(Arrays.asList(ids), sh);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		}
		
		boolean allGood = true;
		StringBuffer errorMessages = new StringBuffer();
		for(int i = 0; i < results.size(); i++) {
			DeleteResult result = results.get(i);
			if(!result.isSuccess()) {
				if(allGood) {
					errorMessages.append("Error(s) executing DELETE: "); //$NON-NLS-1$
					allGood = false;
				}
				List<com.sforce.soap.partner.Error> errors = result.getErrors();
				if(null != errors && errors.size() > 0) {
					for(int x = 0; x < errors.size(); x++) {
						com.sforce.soap.partner.Error error = errors.get(x);
						errorMessages.append(error.getMessage()).append(';');
					}
				}
				
			}
		}
		if(!allGood) {
			throw new ResourceException(errorMessages.toString());
		}
		return results.size();
	}

	public int create(DataPayload data) throws ResourceException {
		SObject toCreate = new SObject();
		toCreate.setType(data.getType());
		toCreate.getAny().addAll(data.getMessageElements());
		List<SObject> objects = new ArrayList<SObject>();
		objects.add(toCreate);
		List<SaveResult> result;
		try {
			result = sfSoap.create(objects, sh);
		} catch (InvalidFieldFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
			throw new ResourceException(e);
		} catch (InvalidIdFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		}
		return analyzeResult(result);
	}

	public int update(List<DataPayload> updateDataList) throws ResourceException {
		List<SObject> params = new ArrayList<SObject>(updateDataList.size());
		for(int i = 0; i < updateDataList.size(); i++) {
			DataPayload data = updateDataList.get(i);
			SObject toCreate = new SObject();
			toCreate.setType(data.getType());
			toCreate.setId(data.getID());
			toCreate.getAny().addAll(data.getMessageElements());
			params.add(i, toCreate);
		}
		List<SaveResult> result;
			try {
				result = sfSoap.update(params, sh);
			} catch (InvalidFieldFault e) {
				throw new ResourceException(e);
			} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
				throw new ResourceException(e);
			} catch (InvalidIdFault e) {
				throw new ResourceException(e);
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
		return analyzeResult(result);
	}
	
	private int analyzeResult(List<SaveResult> results) throws ResourceException {
		for (SaveResult result : results) {
			if(!result.isSuccess()) {
				throw new ResourceException(result.getErrors().get(0).getMessage());
			}
		}
		return results.size();
	}

	public UpdatedResult getUpdated(String objectType, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) throws ResourceException {
			GetUpdatedResult updated;
			try {
				updated = sfSoap.getUpdated(objectType, startDate, endDate, sh);
			} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
				throw new ResourceException(e);
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
			UpdatedResult result = new UpdatedResult(); 
			result.setLatestDateCovered(updated.getLatestDateCovered().toGregorianCalendar());
			result.setIDs(updated.getIds());
			return result;
	}

	public DeletedResult getDeleted(String objectName, XMLGregorianCalendar startCalendar,
			XMLGregorianCalendar endCalendar) throws ResourceException {
			GetDeletedResult deleted;
			try {
				deleted = sfSoap.getDeleted(objectName, startCalendar, endCalendar, sh);
			} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
				throw new ResourceException(e);
			} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
				throw new ResourceException(e);
			}
			DeletedResult result = new DeletedResult();
			result.setLatestDateCovered(deleted.getLatestDateCovered().toGregorianCalendar());
			result.setEarliestDateAvailable(deleted.getEarliestDateAvailable().toGregorianCalendar());
			List<DeletedRecord> records = deleted.getDeletedRecords();
			List<DeletedObject> resultRecords = new ArrayList<DeletedObject>();
			DeletedObject object;
			if(null !=records) {
				for (DeletedObject record : resultRecords) {
					object = new DeletedObject();
					object.setID(record.getID());
					object.setDeletedDate(record.getDeletedDate());
					resultRecords.add(object);
				}
			}
			result.setResultRecords(resultRecords);
			return result;
	}
	
	public  QueryResult retrieve(String fieldList, String sObjectType, List<String> ids) throws ResourceException {
		try {
			List<SObject> objects = sfSoap.retrieve(fieldList, sObjectType, ids, sh);
			QueryResult result = new QueryResult();
			result.getRecords().addAll(objects);
			result.setSize(objects.size());
			result.setDone(true);
			return result;			
		} catch (InvalidFieldFault e) {
			throw new ResourceException(e);
		} catch (MalformedQueryFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
			throw new ResourceException(e);
		} catch (InvalidIdFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		}
		
	}

	public DescribeGlobalResult getObjects() throws ResourceException {
		try {
			return sfSoap.describeGlobal(sh);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		}
	}

	public DescribeSObjectResult getObjectMetaData(String objectName) throws ResourceException {
		try {
			return sfSoap.describeSObject(objectName, sh);
		} catch (com.sforce.soap.partner.InvalidSObjectFault e) {
			throw new ResourceException(e);
		} catch (com.sforce.soap.partner.UnexpectedErrorFault e) {
			throw new ResourceException(e);
		}
	}

	@Override
	public void close() throws ResourceException {
		
	}
	
	@Override
	public boolean isAlive() {
		return isValid();
	}	

}
