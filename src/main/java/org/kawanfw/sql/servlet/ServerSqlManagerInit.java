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

package org.kawanfw.sql.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletConfig;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kawanfw.sql.api.server.DatabaseConfigurationException;
import org.kawanfw.sql.api.server.DatabaseConfigurator;
import org.kawanfw.sql.api.server.auth.UserAuthenticator;
import org.kawanfw.sql.api.server.auth.headers.RequestHeadersAuthenticator;
import org.kawanfw.sql.api.server.blob.BlobDownloadConfigurator;
import org.kawanfw.sql.api.server.blob.BlobUploadConfigurator;
import org.kawanfw.sql.api.server.firewall.SqlFirewallManager;
import org.kawanfw.sql.api.server.session.SessionConfigurator;
import org.kawanfw.sql.servlet.injection.classes.creator.BlobDownloadConfiguratorCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.BlobUploadConfiguratorCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.DatabaseConfiguratorCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.RequestHeadersAuthenticatorCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.SessionConfiguratorCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.SqlFirewallsCreator;
import org.kawanfw.sql.servlet.injection.classes.creator.UserAuthenticatorCreator;
import org.kawanfw.sql.servlet.injection.properties.ConfProperties;
import org.kawanfw.sql.servlet.injection.properties.ConfPropertiesManager;
import org.kawanfw.sql.servlet.injection.properties.ConfPropertiesStore;
import org.kawanfw.sql.servlet.injection.properties.PropertiesFileStore;
import org.kawanfw.sql.servlet.injection.properties.PropertiesFileUtil;
import org.kawanfw.sql.tomcat.ThreadPoolExecutorStore;
import org.kawanfw.sql.tomcat.TomcatSqlModeStore;
import org.kawanfw.sql.tomcat.TomcatStarterUtil;
import org.kawanfw.sql.util.FrameworkDebug;
import org.kawanfw.sql.util.SqlTag;
import org.kawanfw.sql.util.Tag;
import org.kawanfw.sql.version.Version;

public class ServerSqlManagerInit {

    private static boolean DEBUG = FrameworkDebug.isSet(ServerSqlManager.class);
    public static String CR_LF = System.getProperty("line.separator");

    /** The UserAuthenticator instance */
    private static UserAuthenticator userAuthenticator = null;

    /** RequestHeadersAuthenticator instance */
    private RequestHeadersAuthenticator requestHeadersAuthenticator;

    /** The map of (database, DatabaseConfigurator) */
    private static Map<String, DatabaseConfigurator> databaseConfigurators = new ConcurrentHashMap<>();

    /** The map of (database, List<SqlFirewallManager>) */
    private static Map<String, List<SqlFirewallManager>> sqlFirewallMap = new ConcurrentHashMap<>();

    /** The BlobUploadConfigurator instance */
    private static BlobUploadConfigurator blobUploadConfigurator = null;

    /** The BlobUploadConfigurator instance */
    private static BlobDownloadConfigurator blobDownloadConfigurator = null;

    /** The SessionConfigurator instance */
    private static SessionConfigurator sessionConfigurator = null;

    /** The executor to use */
    private ThreadPoolExecutor threadPoolExecutor = null;
    
    /** The Exception thrown at init */
    private Exception exception = null;

    /** The init error message trapped */
    private String initErrrorMesage = null;

    private String classNameToLoad;

    /**
     * Constructor.
     *
     * @param config
     */
    public ServerSqlManagerInit(ServletConfig config) {
	treat(config);
    }

    /**
     * @param config
     */
    private void treat(ServletConfig config) {
	classNameToLoad = null;

	if (!TomcatSqlModeStore.isTomcatEmbedded()) {
	    System.out.println(SqlTag.SQL_PRODUCT_START + " " + Version.getServerVersion());
	}

	// Test the only thing we can test in DatabaseConfigurator
	// getBlobsDirectory()

	try {
	    // Previously created by Tomcat
	    if (!TomcatSqlModeStore.isTomcatEmbedded()) {
		String propertiesFileStr = config.getInitParameter("properties");

		if (propertiesFileStr == null || propertiesFileStr.isEmpty()) {
		    throw new DatabaseConfigurationException(Tag.PRODUCT_USER_CONFIG_FAIL
			    + " AceQL servlet param-name \"properties\" not set. Impossible to load the AceQL Server properties file.");
		}
		File propertiesFile = new File(propertiesFileStr);

		if (!propertiesFile.exists()) {
		    throw new DatabaseConfigurationException(
			    Tag.PRODUCT_USER_CONFIG_FAIL + " properties file not found: " + propertiesFile);
		}

		Properties properties = PropertiesFileUtil.getProperties(propertiesFile);

		ThreadPoolExecutorStore threadPoolExecutorStore = new ThreadPoolExecutorStore(properties);
		threadPoolExecutorStore.create();
	    }

	    threadPoolExecutor = ThreadPoolExecutorStore.getThreadPoolExecutor();

	    if (!TomcatSqlModeStore.isTomcatEmbedded()) {
		createDataSources(config);
	    }

	    loadUserAuthenticator();
	    loadRequestHeadersAuthenticator();

	    Set<String> databases = ConfPropertiesStore.get().getDatabaseNames();

	    // Load the classes
	    loadDatabaseConfigurators(databases);
	    loadSqlFirewallManagers(databases);
	    loadBlobDownloadConfigurator();
	    loadBlobUploadConfigurator();
	    loadSessionManagerConfigurator();

	} catch (ClassNotFoundException e) {
	    initErrrorMesage = Tag.PRODUCT_USER_CONFIG_FAIL
		    + " Impossible to load (ClassNotFoundException) Configurator class: " + classNameToLoad;
	    exception = e;
	} catch (InstantiationException e) {
	    initErrrorMesage = Tag.PRODUCT_USER_CONFIG_FAIL
		    + " Impossible to load (InstantiationException) Configurator class: " + classNameToLoad;
	    exception = e;
	} catch (IllegalAccessException e) {
	    initErrrorMesage = Tag.PRODUCT_USER_CONFIG_FAIL
		    + " Impossible to load (IllegalAccessException) Configurator class: " + classNameToLoad;
	    exception = e;
	} catch (DatabaseConfigurationException e) {
	    initErrrorMesage = e.getMessage();
	    exception = e;
	} catch (Exception e) {
	    initErrrorMesage = Tag.PRODUCT_PRODUCT_FAIL + " Please contact support at: support@kawansoft.com";
	    exception = e;
	}

	treatException();
    }

    /**
     *
     */
    private void treatException() {
	if (exception == null) {
	    System.out.println(SqlTag.SQL_PRODUCT_START + " Loaded classes Status: OK.");

	    if (!TomcatSqlModeStore.isTomcatEmbedded()) {
		String runningMessage = SqlTag.SQL_PRODUCT_START + " " + Version.PRODUCT.NAME + " Start OK.";
		System.out.println(runningMessage);
	    }

	} else {
	    exception.printStackTrace();
	    if (!TomcatSqlModeStore.isTomcatEmbedded()) {
		String errorMessage1 = SqlTag.SQL_PRODUCT_START + "  -> Loaded classes Status: KO.";
		String errorMessage2 = initErrrorMesage;
		String errorMessage3 = ExceptionUtils.getStackTrace(exception);

		System.out.println(errorMessage1);
		System.out.println(errorMessage2);
		System.out.println(errorMessage3);

		System.out.println();
	    }
	}
    }

    /**
     * Loads Session Manager Configurator.
     *
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    private void loadSessionManagerConfigurator()
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {
	// Load Configurators for SessionManager
	String sessionManagerConfiguratorClassName = ConfPropertiesStore.get().getSessionConfiguratorClassName();
	classNameToLoad = sessionManagerConfiguratorClassName;
	SessionConfiguratorCreator sessionConfiguratorCreator = new SessionConfiguratorCreator(
		sessionManagerConfiguratorClassName);
	sessionConfigurator = sessionConfiguratorCreator.getSessionConfigurator();
	sessionManagerConfiguratorClassName = sessionConfiguratorCreator.getSessionConfiguratorClassName();

	if (!sessionManagerConfiguratorClassName
		.equals(org.kawanfw.sql.api.server.session.DefaultSessionConfigurator.class.getName())) {
	    System.out.println(SqlTag.SQL_PRODUCT_START + " Loading sessionManagerConfiguratorClassName: ");
	    System.out.println(SqlTag.SQL_PRODUCT_START + "  -> " + sessionManagerConfiguratorClassName);
	}
    }

    /**
     * Loads Blob upload configurator.
     *
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    private void loadBlobUploadConfigurator()
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {
	String blobUploadConfiguratorClassName = ConfPropertiesStore.get().getBlobUploadConfiguratorClassName();
	classNameToLoad = blobUploadConfiguratorClassName;
	BlobUploadConfiguratorCreator blobUploadConfiguratorCreator = new BlobUploadConfiguratorCreator(
		blobUploadConfiguratorClassName);
	blobUploadConfigurator = blobUploadConfiguratorCreator.getBlobUploadConfigurator();
	blobUploadConfiguratorClassName = blobUploadConfiguratorCreator.getBlobUploadConfiguratorClassName();

	if (!blobUploadConfiguratorClassName
		.equals(org.kawanfw.sql.api.server.blob.DefaultBlobUploadConfigurator.class.getName())) {
	    System.out.println(SqlTag.SQL_PRODUCT_START + " Loading blobUploadConfiguratorClassName: ");
	    System.out.println(SqlTag.SQL_PRODUCT_START + " " + blobUploadConfiguratorClassName);
	}
    }

    /**
     * Loads Blob download configurator.
     *
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    private void loadBlobDownloadConfigurator()
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {
	// Load Configurators for Blobs/Clobs
	String blobDownloadConfiguratorClassName = ConfPropertiesStore.get().getBlobDownloadConfiguratorClassName();
	classNameToLoad = blobDownloadConfiguratorClassName;
	BlobDownloadConfiguratorCreator blobDownloadConfiguratorCreator = new BlobDownloadConfiguratorCreator(
		blobDownloadConfiguratorClassName);
	blobDownloadConfigurator = blobDownloadConfiguratorCreator.getBlobDownloadConfigurator();
	blobDownloadConfiguratorClassName = blobDownloadConfiguratorCreator.getBlobDownloadConfiguratorClassName();

	if (!blobDownloadConfiguratorClassName
		.equals(org.kawanfw.sql.api.server.blob.DefaultBlobDownloadConfigurator.class.getName())) {
	    System.out.println(SqlTag.SQL_PRODUCT_START + " Loading blobDownloadConfiguratorClassName: ");
	    System.out.println(SqlTag.SQL_PRODUCT_START + " " + blobDownloadConfiguratorClassName);
	}
    }

    /**
     * loads userAuthenticator.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private void loadUserAuthenticator() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
	    InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	String userAuthenticatorClassName = ConfPropertiesStore.get().getUserAuthenticatorClassName();

	classNameToLoad = userAuthenticatorClassName;
	UserAuthenticatorCreator userAuthenticatorCreator = new UserAuthenticatorCreator(userAuthenticatorClassName);
	userAuthenticator = userAuthenticatorCreator.getUserAuthenticator();
	userAuthenticatorClassName = userAuthenticatorCreator.getUserAuthenticatorClassName();

	System.out.println(SqlTag.SQL_PRODUCT_START + " Loading UserAuthenticator class:");
	System.out.println(SqlTag.SQL_PRODUCT_START + "  -> " + userAuthenticatorClassName);
    }

    /**
     * loads requestHeadersAuthenticator.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private void loadRequestHeadersAuthenticator()
	    throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
	    IllegalAccessException, IllegalArgumentException, InvocationTargetException {

	String requestHeadersAuthenticatorClassName = ConfPropertiesStore.get().getRequestHeadersAuthenticatorClassName();

	classNameToLoad = requestHeadersAuthenticatorClassName;
	RequestHeadersAuthenticatorCreator userAuthenticatorCreator = new RequestHeadersAuthenticatorCreator(
		requestHeadersAuthenticatorClassName);
	requestHeadersAuthenticator = userAuthenticatorCreator.getRequestHeadersAuthenticator();
	requestHeadersAuthenticatorClassName = userAuthenticatorCreator.getRequestHeadersAuthenticatorClassName();

	System.out.println(SqlTag.SQL_PRODUCT_START + " Loading RequestHeadersAuthenticator class:");
	System.out.println(SqlTag.SQL_PRODUCT_START + "  -> " + requestHeadersAuthenticatorClassName);
    }

    /**
     * loads Firewall Managers.
     *
     * @param databases
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws SQLException
     * @throws IOException
     */
    private void loadSqlFirewallManagers(Set<String> databases)
	    throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
	    IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException, IOException {

	for (String database : databases) {
	    List<String> sqlFirewallClassNames = ConfPropertiesStore.get().getSqlFirewallClassNames(database);
	    classNameToLoad = sqlFirewallClassNames.toString();

	    String tagSQLFirewallManager = null;
	    if (sqlFirewallClassNames.size() == 0)
		tagSQLFirewallManager = " SQLFirewallManager class: ";
	    else
		tagSQLFirewallManager = " SQLFirewallManager classes: ";

	    System.out.println(SqlTag.SQL_PRODUCT_START + " Loading Database " + database + tagSQLFirewallManager);

	    DatabaseConfigurator databaseConfigurator = databaseConfigurators.get(database);
	    SqlFirewallsCreator sqlFirewallsCreator = new SqlFirewallsCreator(sqlFirewallClassNames, database,
		    databaseConfigurator);
	    List<SqlFirewallManager> sqlFirewallManagers = sqlFirewallsCreator.getSqlFirewalls();
	    sqlFirewallMap.put(database, sqlFirewallManagers);

	    sqlFirewallClassNames = sqlFirewallsCreator.getSqlFirewallClassNames();
	    classNameToLoad = sqlFirewallClassNames.toString();

	    for (String sqlFirewallClassName : sqlFirewallClassNames) {
		System.out.println(SqlTag.SQL_PRODUCT_START + "   -> " + sqlFirewallClassName);
	    }
	}
    }

    /**
     * Loads the database configurators.
     *
     * @param databases
     *
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    private void loadDatabaseConfigurators(Set<String> databases)
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {
	String databaseConfiguratorClassName;

	// WARNING: Database configurator must be loaded prior to firewalls
	// because a getConnection() is used to test SqlFirewallManager
	for (String database : databases) {
	    databaseConfiguratorClassName = ConfPropertiesStore.get().getDatabaseConfiguratorClassName(database);

	    debug("databaseConfiguratorClassName    : " + databaseConfiguratorClassName);

	    // Check spelling with first letter capitalized
	    //HACK NDP
	    //TODO LATER
//	    if (databaseConfiguratorClassName == null || databaseConfiguratorClassName.isEmpty()) {
//		String capitalized = StringUtils.capitalize(ServerSqlManager.DATABASE_CONFIGURATOR_CLASS_NAME);
//		databaseConfiguratorClassName = ServletParametersStore.getInitParameter(database, capitalized);
//	    }

	    // Call the specific DatabaseConfigurator class to use
	    classNameToLoad = databaseConfiguratorClassName;
	    DatabaseConfiguratorCreator databaseConfiguratorCreator = new DatabaseConfiguratorCreator(
		    databaseConfiguratorClassName);
	    DatabaseConfigurator databaseConfigurator = databaseConfiguratorCreator.getDatabaseConfigurator();
	    databaseConfiguratorClassName = databaseConfiguratorCreator.getDatabaseConfiguratorClassName();

	    databaseConfigurators.put(database, databaseConfigurator);

	    System.out.println(
		    SqlTag.SQL_PRODUCT_START + " Loading Database " + database + " DatabaseConfigurator class:");
	    System.out.println(SqlTag.SQL_PRODUCT_START + "  -> " + databaseConfiguratorClassName);
	}
    }

    /**
     * Creates the data sources - this is called only if AceQL is used in Servlet
     * Container
     *
     * @param config
     * @throws IOException
     */
    private void createDataSources(ServletConfig config) throws IOException, Exception {
	String propertiesFileStr = config.getInitParameter("properties");

	if (propertiesFileStr == null || propertiesFileStr.isEmpty()) {
	    throw new DatabaseConfigurationException(Tag.PRODUCT_USER_CONFIG_FAIL
		    + " AceQL servlet param-name \"properties\" not set. Impossible to load the AceQL Server properties file.");
	}

	File propertiesFile = new File(propertiesFileStr);

	if (!propertiesFile.exists()) {
	    throw new DatabaseConfigurationException(
		    Tag.PRODUCT_USER_CONFIG_FAIL + " properties file not found: " + propertiesFile);
	}

	System.out.println(TomcatStarterUtil.getJavaInfo());
	System.out.println(SqlTag.SQL_PRODUCT_START + " " + "Using properties file: ");
	System.out.println(SqlTag.SQL_PRODUCT_START + "  -> " + propertiesFile);

	// Set properties file. Will be used elsewhere
	// (for CsvRulesManager load file, per example).
	PropertiesFileStore.set(propertiesFile);
	Properties properties = PropertiesFileUtil.getProperties(propertiesFile);
	
	// Create all configuration properties from the Properties and store
	ConfPropertiesManager confPropertiesManager = new ConfPropertiesManager(properties);
	ConfProperties confProperties = confPropertiesManager.createConfProperties();
	ConfPropertiesStore.set(confProperties);
	

	// Create the default DataSource if necessary
	TomcatStarterUtil.createAndStoreDataSources(properties);

    }

    public UserAuthenticator getUserAuthenticator() {
	return userAuthenticator;
    }

    /**
     * @return the requestHeadersAuthenticator
     */
    public RequestHeadersAuthenticator getRequestHeadersAuthenticator() {
	return requestHeadersAuthenticator;
    }

    public Map<String, DatabaseConfigurator> getDatabaseConfigurators() {
	return databaseConfigurators;
    }

    public Map<String, List<SqlFirewallManager>> getSqlFirewallMap() {
	return sqlFirewallMap;
    }

    public BlobUploadConfigurator getBlobUploadConfigurator() {
	return blobUploadConfigurator;
    }

    public BlobDownloadConfigurator getBlobDownloadConfigurator() {
	return blobDownloadConfigurator;
    }

    public SessionConfigurator getSessionConfigurator() {
	return sessionConfigurator;
    }

    public Exception getException() {
	return exception;
    }

    public String getInitErrrorMesage() {
	return initErrrorMesage;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
	return threadPoolExecutor;
    }

    /**
     * Method called by children Servlet for debug purpose Println is done only if
     * class name name is in kawansoft-debug.ini
     */
    public static void debug(String s) {
	if (DEBUG) {
	    System.out.println(new Date() + " " + s);
	}
    }

}
