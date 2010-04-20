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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.plugins.ManagedObjectImpl;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.GenericValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.ProfileServiceUtil;

/**
 * Discovery component for the MetaMatrix Host controller process
 * 
 */
public class ModelDiscoveryComponent implements ResourceDiscoveryComponent {

	private final Log log = LogFactory.getLog(this.getClass());

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {
		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

		for (int i = 0; i < 3; i++) {
			String modelName = "myModel" + i;

			DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
					discoveryContext.getResourceType(), // ResourceType
					modelName, // Resource Key
					modelName, // Resource Name
					null, // Version TODO can we get that from discovery ?
					PluginConstants.ComponentType.Model.DESCRIPTION, // Description
					discoveryContext.getDefaultPluginConfiguration(), // Plugin
					// Config
					null // Process info from a process scan
			);

			Configuration c = detail.getPluginConfiguration();
			c.put(new PropertySimple(modelName, "name"));

			PropertyList list = new PropertyList("multisourceModels");
			PropertyMap map = new PropertyMap("model",
					new PropertySimple("oraclesource", "sourceName"),
					new PropertySimple("JNDINameOracle", "jndiName"),
					new PropertySimple("MySQLsource", "sourceName"),
					new PropertySimple("JNDINameMySQL", "jndiName"));
			list.add(map);
			
			detail.setPluginConfiguration(c);
			// Add to return values
			discoveredResources.add(detail);
			log.info("Discovered Teiid Model: " + modelName);
		}

		return discoveredResources;
	}

	/**
	 * @param mcVdb
	 * @param configuration
	 */
	private void getConnectors(ManagedComponent model,
			Configuration configuration) {
		// Get Connector(s) from Model
		ManagedProperty property = model.getProperty("connectorBindingNames");
		CollectionValueSupport valueSupport = (CollectionValueSupport) property
				.getValue();
		MetaValue[] metaValues = valueSupport.getElements();

		PropertyList connectorsList = new PropertyList("connectors");
		configuration.put(connectorsList);

		for (MetaValue value : metaValues) {
			SimpleValueSupport simpleValueSupport = (SimpleValueSupport) value;
			String connectorName = (String) simpleValueSupport.getValue();

			PropertyMap connector = new PropertyMap("connector",
					new PropertySimple("name", connectorName));
			connectorsList.add(connector);
		}
	}

}