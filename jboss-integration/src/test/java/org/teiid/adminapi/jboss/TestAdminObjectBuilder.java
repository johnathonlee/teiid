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
package org.teiid.adminapi.jboss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.managed.plugins.factory.ManagedObjectFactoryBuilder;
import org.junit.Test;
import org.teiid.adminapi.DataPolicy;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.Translator;
import org.teiid.adminapi.impl.DataPolicyMetadata;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.TranslatorMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.DataPolicyMetadata.PermissionMetaData;
import org.teiid.translator.ExecutionFactory;

@SuppressWarnings("nls")
public class TestAdminObjectBuilder {

	@Test
	public void testVDB() {
		
		VDBMetaData vdb = new VDBMetaData();
		vdb.setName("myVDB"); 
		vdb.setDescription("vdb description"); 
		vdb.setVersion(1);
		vdb.addProperty("vdb-property", "vdb-value");  
		
		ModelMetaData modelOne = new ModelMetaData();
		modelOne.setName("model-one"); 
		modelOne.addSourceMapping("s1", "translator", "java:mybinding");  
		modelOne.setModelType(Model.Type.PHYSICAL); 
		modelOne.addProperty("model-prop", "model-value");  
		modelOne.addProperty("model-prop", "model-value-override");  
		modelOne.setVisible(false);
		modelOne.addError("ERROR", "There is an error in VDB");  
		modelOne.setDescription("model description");
		
		vdb.addModel(modelOne);
		
		ModelMetaData modelTwo = new ModelMetaData();
		modelTwo.setName("model-two"); 
		modelTwo.addSourceMapping("s1", "translator", "java:binding-one");  
		modelTwo.addSourceMapping("s2", "translator", "java:binding-two");  
		modelTwo.setModelType(Model.Type.VIRTUAL); 
		modelTwo.addProperty("model-prop", "model-value");  
		
		vdb.addModel(modelTwo);
		
		TranslatorMetaData t1 = new TranslatorMetaData();
		t1.setName("oracleOverride");
		t1.setType("oracle");
		t1.addProperty("my-property", "my-value");
		List<Translator> list = new ArrayList<Translator>();
		list.add(t1);
		vdb.setOverrideTranslators(list);
		
		DataPolicyMetadata roleOne = new DataPolicyMetadata();
		roleOne.setName("roleOne"); 
		roleOne.setDescription("roleOne described"); 
		
		PermissionMetaData perm1 = new PermissionMetaData();
		perm1.setResourceName("myTable.T1"); 
		perm1.setAllowRead(true);
		roleOne.addPermission(perm1);
		
		PermissionMetaData perm2 = new PermissionMetaData();
		perm2.setResourceName("myTable.T2"); 
		perm2.setAllowRead(false);
		perm2.setAllowDelete(true);
		roleOne.addPermission(perm2);
		
		roleOne.setMappedRoleNames(Arrays.asList("ROLE1", "ROLE2"));  
		
		vdb.addDataPolicy(roleOne);
		
		// convert to managed object and build the VDB out of MO
		ManagedObjectFactory mof = ManagedObjectFactoryBuilder.create();
		ManagedObject mo = mof.initManagedObject(vdb, null, null);
		vdb = AdminObjectBuilder.buildAO(mo, VDBMetaData.class);
		
		assertEquals("myVDB", vdb.getName()); 
		assertEquals("vdb description", vdb.getDescription()); 
		assertEquals(1, vdb.getVersion());
		assertEquals("vdb-value", vdb.getPropertyValue("vdb-property"));  
		
		assertNotNull(vdb.getModel("model-one")); 
		assertNotNull(vdb.getModel("model-two")); 
		assertNull(vdb.getModel("model-unknown")); 
		
		modelOne = vdb.getModel("model-one"); 
		assertEquals("model-one", modelOne.getName()); 
		assertEquals("s1", modelOne.getSourceNames().get(0)); 
		assertEquals(Model.Type.PHYSICAL, modelOne.getModelType()); 
		assertEquals("model-value-override", modelOne.getPropertyValue("model-prop"));  
		assertFalse(modelOne.isVisible());
		assertEquals("model description", modelOne.getDescription()); 
		
		modelTwo = vdb.getModel("model-two"); 
		assertEquals("model-two", modelTwo.getName()); 
		assertTrue(modelTwo.getSourceNames().contains("s1")); 
		assertTrue(modelTwo.getSourceNames().contains("s2")); 
		assertEquals(Model.Type.VIRTUAL, modelTwo.getModelType()); // this is not persisted in the XML
		assertEquals("model-value", modelTwo.getPropertyValue("model-prop"));  
		
		
		assertTrue(vdb.getValidityErrors().contains("There is an error in VDB")); 
		
		List<Translator> translators = vdb.getOverrideTranslators();
		assertTrue(translators.size() == 1);
		
		Translator translator = translators.get(0);
		assertEquals("oracleOverride", translator.getName());
		assertEquals("oracle", translator.getType());
		assertEquals("my-value", translator.getPropertyValue("my-property"));
				
		List<DataPolicy> roles = vdb.getDataPolicies();
		
		assertTrue(roles.size() == 1);
		
		DataPolicyMetadata role = vdb.getDataPolicy("roleOne"); 
		assertEquals("roleOne described", role.getDescription()); 
		assertNotNull(role.getMappedRoleNames());
		assertTrue(role.getMappedRoleNames().contains("ROLE1")); 
		assertTrue(role.getMappedRoleNames().contains("ROLE2")); 
		
		List<DataPolicy.DataPermission> permissions = role.getPermissions();
		assertEquals(2, permissions.size());
		
		for (DataPolicy.DataPermission p: permissions) {
			if (p.getResourceName().equalsIgnoreCase("myTable.T1")) { 
				assertTrue(p.getAllowRead());
				assertNull(p.getAllowDelete());
			}
			else {
				assertFalse(p.getAllowRead());
				assertTrue(p.getAllowDelete());
			}
		}
	}
	
	@Test
	public void testTranslator() {
		TranslatorMetaData tm = new TranslatorMetaData();
		
		tm.setExecutionFactoryClass(ExecutionFactory.class);
		tm.setName("Oracle");
		tm.addProperty("ExtensionTranslationClassName", "org.teiid.translator.jdbc.oracle.OracleSQLTranslator");
		
		// convert to managed object and build the VDB out of MO
		ManagedObjectFactory mof = ManagedObjectFactoryBuilder.create();
		ManagedObject mo = mof.initManagedObject(tm, null, null);
		tm = AdminObjectBuilder.buildAO(mo, TranslatorMetaData.class);
		
		assertEquals("Oracle", tm.getName());
		assertEquals(ExecutionFactory.class.getName(), tm.getPropertyValue(Translator.EXECUTION_FACTORY_CLASS));
		assertEquals("org.teiid.translator.jdbc.oracle.OracleSQLTranslator", tm.getPropertyValue("ExtensionTranslationClassName"));		
	}
}
