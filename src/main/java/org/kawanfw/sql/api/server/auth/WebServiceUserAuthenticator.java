/*
 * Copyright (c)2022 KawanSoft S.A.S. All rights reserved.
 * 
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file in the project's root directory.
 *
 * Change Date: 2026-09-01
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by version 2.0 of the Apache License.
 */
package org.kawanfw.sql.api.server.auth;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;

import org.apache.commons.lang3.StringUtils;
import org.kawanfw.sql.api.server.DefaultDatabaseConfigurator;
import org.kawanfw.sql.api.server.util.SimpleHttpClient;
import org.kawanfw.sql.servlet.injection.properties.PropertiesFileStore;
import org.kawanfw.sql.servlet.injection.properties.PropertiesFileUtil;
import org.kawanfw.sql.util.Tag;

/**
 * A concrete {@code UserAuthenticator} that allows zero-code remote client
 * {@code (username, password)} authentication against a Web service. <br>
 * <br>
 * The URL of the Web service is defined in
 * {@code the webServiceUserAuthenticator.url} in the
 * {@code aceql-server.properties} file. <br>
 * <br>
 * The Web service must just implement these features:
 * <ul>
 * <li>It must accept the 2 POST parameters {@code username} and {@code password}.</li>
 * <li>It must return either:
 * <ul>
 * <li>The JSON string <code>{"status"="OK"}</code> if the authentication
 * succeeds.</li>
 * <li>The JSON string <code>{"status"="FAIL"}</code> if the authentication
 * fails.</li>
 * </ul></li>
 * </ul>
 *
 * @see UserAuthenticator
 * @author Nicolas de Pomereu
 * @since 5.0
 */

public class WebServiceUserAuthenticator implements UserAuthenticator {

    private Properties properties = null;
    private Logger logger = null;

    /*
     * (non-Javadoc)
     *
     * @see
     * org.kawanfw.sql.api.server.auth.UserAuthenticator#login(java.lang.String,
     * char[], java.lang.String, java.lang.String)
     */
    /**
     * @return <code>true</code> if the Authentication Web Service defined in
     *         {@code aceql-server.properties} returns the JSON String
     *         <code>{"status"="OK"}</code>, else <code>false</code> .
     */
    @Override
    public boolean login(String username, char[] password, String database, String ipAddress)
	    throws IOException, SQLException {

	if (properties == null) {
	    File file = PropertiesFileStore.get();
	    properties = PropertiesFileUtil.getProperties(file);
	}

	String url = properties.getProperty("webServiceUserAuthenticator.url");
	String timeoutSecondsStr = properties.getProperty("webServiceUserAuthenticator.timeoutSeconds");
	String httpTraceStr = properties.getProperty("webServiceUserAuthenticator.httpTrace");

	// Accept free login if no Web Service URL defined or is localhost
	if (url == null || url.contentEquals("localhost")) {
	    return true;
	}

	if (timeoutSecondsStr == null) {
	    timeoutSecondsStr = "0";
	}

	if (!StringUtils.isNumeric(timeoutSecondsStr)) {
	    throw new IllegalArgumentException(
		    "The default.login.webService.timeoutSeconds property is not numeric: " + timeoutSecondsStr);
	}

	int timeoutSeconds = Integer.parseInt(timeoutSecondsStr);
	int connectTimeout = timeoutSeconds * 1000;
	int readTimeout = timeoutSeconds * 1000;

	setTraceActivity(httpTraceStr);

	Map<String, String> parametersMap = buildParametersMap(username, password);

	String jsonResult = buildJsonResult(username, url, connectTimeout, readTimeout, parametersMap);
	
	if (jsonResult == null) {
	    return false;
	}

	return statusIsOk(jsonResult.trim());

    }

    /**
     * @param httpTraceStr
     */
    private void setTraceActivity(String httpTraceStr) {
	if (Boolean.parseBoolean(httpTraceStr)) {
	    SimpleHttpClient.TRACE_ON = true;
	} else {
	    SimpleHttpClient.TRACE_ON = false;
	}
    }

    /**
     * @param jsonResult
     * @return
     * @throws IOException
     */
    private boolean statusIsOk(String jsonResult) throws IOException {

	try {
	    JsonReader reader = Json.createReader(new StringReader(jsonResult));
	    JsonStructure jsonst = reader.read();

	    JsonObject object = (JsonObject) jsonst;
	    JsonString status = (JsonString) object.get("status");

	    return status != null && status.getString().equals("OK");

	} catch (Exception e) {
	    if (logger == null) {
		logger = new DefaultDatabaseConfigurator().getLogger();
	    }
	    logger.log(Level.SEVERE,
		    getInitTag() + "Error when parsing jsonResult of Authentication Web Service: " + e.getMessage());
	    return false;
	}
    }

    /**
     * @param username
     * @param url
     * @param connectTimeout
     * @param readTimeout
     * @param parametersMap
     * @return
     * @throws IOException
     */
    private String buildJsonResult(String username, String url, int connectTimeout, int readTimeout, Map<String, String> parametersMap) throws IOException {
	SimpleHttpClient simpleHttpClient = new SimpleHttpClient(connectTimeout, readTimeout);

	String jsonResult = null;
	try {
	    jsonResult = simpleHttpClient.callWithPost(new URL(url), parametersMap);
	} catch (Exception e) {
	    if (logger == null) {
		logger = new DefaultDatabaseConfigurator().getLogger();
	    }
	    logger.log(Level.SEVERE, getInitTag() + "Username " + username
		    + " can not authenticate. Error when calling SimpleHttpClient: " + e.getMessage());
	    return null;
	}

	return jsonResult;
    }

    /**
     * @param username
     * @param password
     * @return
     */
    private Map<String, String> buildParametersMap(String username, char[] password) {
	Map<String, String> parametersMap = new HashMap<>();
	parametersMap.put("username", username);
	parametersMap.put("password", new String(password));
	return parametersMap;
    }

    /**
     * @return the beginning of the log line
     */
    private String getInitTag() {
	return Tag.PRODUCT + " " + WebServiceUserAuthenticator.class.getSimpleName() + ": ";
    }

}
