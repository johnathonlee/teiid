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
package org.teiid.rhq.plugin;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.teiid.rhq.comm.ConnectionConstants;


/**
 * Component class for a Teiid VDB
 * 
 */
public class VDBComponent extends Facet {
	private final Log LOG = LogFactory
			.getLog(VDBComponent.class);

	/**
	 * The plugin container will call this method when your resource component
	 * has been scheduled to collect some measurements now. It is within this
	 * method that you actually talk to the managed resource and collect the
	 * measurement data that is has emitted.
	 * 
	 * @see MeasurementFacet#getValues(MeasurementReport, Set)
	 */
	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> requests) {
		for (MeasurementScheduleRequest request : requests) {
			String name = request.getName();

			// TODO: based on the request information, you must collect the
			// requested measurement(s)
			// you can use the name of the measurement to determine what you
			// actually need to collect
			try {
				Number value = new Integer(1); // dummy measurement value -
												// this should come from the
												// managed resource
				report.addData(new MeasurementDataNumeric(request, value
						.doubleValue()));
			} catch (Exception e) {
				LOG.error("Failed to obtain measurement [" + name //$NON-NLS-1$
						+ "]. Cause: " + e); //$NON-NLS-1$
			}
		}

		return;
	}
	
	protected void setOperationArguments(String name, Configuration configuration,
			Map argumentMap) {

		if (name.equals(ConnectionConstants.ComponentType.Operation.GET_PROPERTIES)){
			String key = ConnectionConstants.IDENTIFIER;
			//argumentMap.put(key, getComponentIdentifier());
		}
 		
	}

	@Override
	String getComponentType() {
		// TODO Auto-generated method stub
		return null;
	} 
	
}

