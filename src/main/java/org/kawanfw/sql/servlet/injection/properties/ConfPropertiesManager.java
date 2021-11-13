/*
 * This file is part of AceQL HTTP.
 * AceQL HTTP: SQL Over HTTP
 * Copyright (C) 2021,  KawanSoft SAS
 * (http://www.kawansoft.com). All rights reserved.
 *
 * AceQL HTTP is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * AceQL HTTP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301  USA
 *
 * Any modifications to this file must keep this entire header
 * intact.
 */

package org.kawanfw.sql.servlet.injection.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.kawanfw.sql.servlet.ServerSqlManager;
import org.kawanfw.sql.servlet.injection.properties.ConfProperties.ConfPropertiesBuilder;
import org.kawanfw.sql.tomcat.TomcatStarterUtil;
import org.kawanfw.sql.tomcat.TomcatStarterUtilFirewall;

public class ConfPropertiesManager {

    private Properties properties;

    /**
     * Constructor 
     * @param properties
     */
    public ConfPropertiesManager(Properties properties) {
	this.properties = Objects.requireNonNull(properties, "properties cannot be null!");	
    }

    /**
     * Create the ConfProperties instance created from the Properties.
     * @return the ConfProperties instance created from the Properties.
     */
    public ConfProperties createConfProperties() {

	ConfPropertiesBuilder confPropertiesBuilder = new ConfPropertiesBuilder();	
	
	//ServletParametersStore.init(); // Set back to null static values

	String aceQLManagerServletCallName = TomcatStarterUtil.getAceQLManagerSevletName(properties);

	//ServletParametersStore.setServletName(aceQLManagerServletCallName);
	confPropertiesBuilder.servletName(aceQLManagerServletCallName);
	
	boolean statelessMode = Boolean.parseBoolean(properties.getProperty(ServerSqlManager.STATELESS_MODE, "false"));
	//ServletParametersStore.setStatelessMode(statelessMode);
	confPropertiesBuilder.statelessMode(statelessMode);
	
	Set<String> databases = TomcatStarterUtil.getDatabaseNames(properties);
	//ServletParametersStore.setDatabaseNames(databases);
	confPropertiesBuilder.databaseSet(databases);

	String userAuthenticatorClassName = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.USER_AUTHENTICATOR_CLASS_NAME));
	if (userAuthenticatorClassName != null && !userAuthenticatorClassName.isEmpty()) {
	    //ServletParametersStore.setUserAuthenticatorClassName(userAuthenticatorClassName);
	    confPropertiesBuilder.userAuthenticatorClassName(userAuthenticatorClassName);
	}

	String requestHeadersAuthenticatorClassName = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.REQUEST_HEADERS_AUTHENTICATOR_CLASS_NAME));
	if (requestHeadersAuthenticatorClassName != null && !requestHeadersAuthenticatorClassName.isEmpty()) {
	    //ServletParametersStore.setRequestHeadersAuthenticatorClassName(requestHeadersAuthenticatorClassName);
	    confPropertiesBuilder.requestHeadersAuthenticatorClassName(requestHeadersAuthenticatorClassName);
	}

	Map<String, String> databaseConfiguratorClassNameMap = new HashMap<>();
	Map<String, List<String>> sqlFirewallClassNamesMap = new HashMap<>();
	 
	for (String database : databases) {
	    // Set the configurator to use for this database
	    String databaseConfiguratorClassName = TomcatStarterUtil.trimSafe(
		    properties.getProperty(database + "." + ServerSqlManager.DATABASE_CONFIGURATOR_CLASS_NAME));

	    if (databaseConfiguratorClassName != null && !databaseConfiguratorClassName.isEmpty()) {
		//ServletParametersStore.setInitParameter(database, new InitParamNameValuePair(
		//	ServerSqlManager.DATABASE_CONFIGURATOR_CLASS_NAME, databaseConfiguratorClassName));
		databaseConfiguratorClassNameMap.put(database, databaseConfiguratorClassName);
	    }

	    String sqlFirewallClassNameArray = TomcatStarterUtil.trimSafe(
		    properties.getProperty(database + "." + ServerSqlManager.SQL_FIREWALL_MANAGER_CLASS_NAMES));

	    if (sqlFirewallClassNameArray != null && !sqlFirewallClassNameArray.isEmpty()) {
		List<String> sqlFirewallClassNames = TomcatStarterUtilFirewall.getList(sqlFirewallClassNameArray);
		sqlFirewallClassNamesMap.put(database, sqlFirewallClassNames );
		//ServletParametersStore.setSqlFirewallClassNames(database, sqlFirewallClassNames);
	    } else {
		//ServletParametersStore.setSqlFirewallClassNames(database, new ArrayList<String>());
		sqlFirewallClassNamesMap.put(database, new ArrayList<String>() );
	    }
	}
	
	confPropertiesBuilder.databaseConfiguratorClassNameMap(databaseConfiguratorClassNameMap);
	confPropertiesBuilder.sqlFirewallClassNamesMap(sqlFirewallClassNamesMap);
	
	String blobDownloadConfiguratorClassName = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.BLOB_DOWNLOAD_CONFIGURATOR_CLASS_NAME));
	//ServletParametersStore.setBlobDownloadConfiguratorClassName(blobDownloadConfiguratorClassName);
	confPropertiesBuilder.blobDownloadConfiguratorClassName(blobDownloadConfiguratorClassName);
	
	String blobUploadConfiguratorClassName = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.BLOB_UPLOAD_CONFIGURATOR_CLASS_NAME));
	//ServletParametersStore.setBlobUploadConfiguratorClassName(blobUploadConfiguratorClassName);
	confPropertiesBuilder.blobUploadConfiguratorClassName(blobUploadConfiguratorClassName);
	
	String sessionConfiguratorClassName = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.SESSION_CONFIGURATOR_CLASS_NAME));
	//ServletParametersStore.setSessionConfiguratorClassName(sessionConfiguratorClassName);
	confPropertiesBuilder.sessionConfiguratorClassName(sessionConfiguratorClassName);

	String jwtSessionConfiguratorSecretValue = TomcatStarterUtil
		.trimSafe(properties.getProperty(ServerSqlManager.JWT_SESSION_CONFIGURATOR_SECRET));
	//ServletParametersStore.setJwtSessionConfiguratorSecretValue(jwtSessionConfiguratorSecretValue);	
	confPropertiesBuilder.jwtSessionConfiguratorSecretValue(jwtSessionConfiguratorSecretValue);
	
	ConfProperties confProperties = confPropertiesBuilder.build();
	return confProperties;
	
    }
    

}
