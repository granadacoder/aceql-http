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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.kawanfw.sql.api.server.DatabaseConfigurator;
import org.kawanfw.sql.api.server.firewall.SqlFirewallManager;
import org.kawanfw.sql.metadata.util.GsonWsUtil;
import org.kawanfw.sql.servlet.connection.ConnectionIdUtil;
import org.kawanfw.sql.servlet.connection.ConnectionStore;
import org.kawanfw.sql.servlet.connection.ConnectionStoreGetter;
import org.kawanfw.sql.servlet.connection.RollbackUtil;
import org.kawanfw.sql.servlet.connection.SavepointUtil;
import org.kawanfw.sql.servlet.connection.TransactionUtil;
import org.kawanfw.sql.servlet.injection.classes.InjectedClassesStore;
import org.kawanfw.sql.servlet.injection.properties.ConfPropertiesUtil;
import org.kawanfw.sql.servlet.jdbc.metadata.JdbcDatabaseMetadataActionManager;
import org.kawanfw.sql.servlet.sql.ServerStatement;
import org.kawanfw.sql.servlet.sql.ServerStatementRawExecute;
import org.kawanfw.sql.servlet.sql.batch.ServerPreparedStatementBatch;
import org.kawanfw.sql.servlet.sql.batch.ServerStatementBatch;
import org.kawanfw.sql.servlet.sql.callable.ServerCallableStatement;
import org.kawanfw.sql.servlet.sql.dto.DatabaseInfo;
import org.kawanfw.sql.servlet.sql.dto.DatabaseInfoDto;
import org.kawanfw.sql.servlet.sql.json_return.JsonErrorReturn;
import org.kawanfw.sql.servlet.sql.json_return.JsonOkReturn;
import org.kawanfw.sql.util.FrameworkDebug;

/**
 * @author Nicolas de Pomereu
 *
 *         The method executeRequest() is to to be called from the SqlHttpServer
 *         Servlet and Class. <br>
 *         It will execute a client side request with a RemoteConnection
 *         connection.
 *
 */
public class ServerSqlDispatch {

    private static final boolean DUMP_HEADERS = false;
    private static boolean DEBUG = FrameworkDebug.isSet(ServerSqlDispatch.class);

    /**
     * Execute the client sent sql request that is already wrapped in the
     * calling try/catch that handles Throwable
     *
     * @param request  the http request
     * @param response the http response
     * @param out
     * @throws IOException         if any IOException occurs
     * @throws SQLException
     * @throws FileUploadException
     */
    public void executeRequestInTryCatch(HttpServletRequest request, HttpServletResponse response, OutputStream out)
	    throws IOException, SQLException, FileUploadException {

	if (doBlobUpload(request, response, out)) {
	    return;
	}

	// Prepare the response
	response.setContentType("text/html; charset=UTF-8");

	String action = request.getParameter(HttpParameter.ACTION);
	String username = request.getParameter(HttpParameter.USERNAME);
	String database = request.getParameter(HttpParameter.DATABASE);
	String sessionId = request.getParameter(HttpParameter.SESSION_ID);
	String connectionId = request.getParameter(HttpParameter.CONNECTION_ID);

	debug("");
	debug("action      : " + action);
	debug("username    : " + username);
	debug("database    : " + database);
	debug("sessionId   : " + sessionId);
	debug("connectionId: " + connectionId);

	BaseActionTreater baseActionTreater = new BaseActionTreater(request, response, out);
	if (!baseActionTreater.treatAndContinue()) {
	    return;
	}

	DatabaseConfigurator databaseConfigurator = baseActionTreater.getDatabaseConfigurator();

	if (isGetVersion(out, action)) {
	    return;
	}

	Connection connection = null;
	
	try {
	    if (ConfPropertiesUtil.isStatelessMode()) {
		// Create the Connection because passed client Id is stateless
		connection = databaseConfigurator.getConnection(database);
	    } else {
		// Extracts the Connection from the store
		ConnectionStoreGetter connectionStoreGetter = new ConnectionStoreGetter(request, response);
		connection = connectionStoreGetter.getConnection();
		if (connectionStoreGetter.getJsonErrorReturn() != null) {
		    ServerSqlManager.writeLine(out, connectionStoreGetter.getJsonErrorReturn().build());
		    return;
		}
	    }

	    // get_database_info
	    if (isGetDatabaseInfo(out, action, connection)) {
		return;
	    }
	    
	    // Do not treat if not in auto-commit mode if Server is Stateless
	    if (! checkStatelessInAutoCommit(request, response, out, connection)) {
		return;
	    }
	    
	    // Release connection in pool & remove all references
	    if (action.equals(HttpParameter.CLOSE)) {
		treatCloseAction(response, out, username, sessionId, connectionId, databaseConfigurator, connection);
		return;
	    }

	    List<SqlFirewallManager> sqlFirewallManagers = InjectedClassesStore.get().getSqlFirewallMap().get(database);

	    if (doTreatJdbcDatabaseMetaData(request, response, out, action, connection, sqlFirewallManagers)) {
		return;
	    }

	    if (doTreatMetadataQuery(request, response, out, action, connection, sqlFirewallManagers)) {
		return;
	    }

	    dumpHeaders(request);

	    dispatch(request, response, out, action, connection, databaseConfigurator, sqlFirewallManagers);
	} 
	catch (Exception e) {
	    RollbackUtil.rollback(connection);
	    throw e;
	}
	finally {
	    // Immediate close of a  Connection for stateless sessions
	    if (ConfPropertiesUtil.isStatelessMode()) {
		databaseConfigurator.close(connection);
	    }
	}

    }

    /**
     * Returns on out srvlet stream all remote database & driver info.
     * @param out
     * @param action
     * @param connection
     * @return
     * @throws IOException
     * @throws SQLException
     */
    private boolean isGetDatabaseInfo(OutputStream out, String action, Connection connection) throws IOException, SQLException {
	if (action.equals(HttpParameter.GET_DATABASE_INFO)) {
	   
	    // Meta data
	    DatabaseMetaData meta = connection.getMetaData();
	    DatabaseInfo databaseInfo = new DatabaseInfo(meta);
	    
	    DatabaseInfoDto databaseInfoDto = new DatabaseInfoDto(databaseInfo);
	    String jsonString = GsonWsUtil.getJSonString(databaseInfoDto);
	    ServerSqlManager.writeLine(out, jsonString);
	    
	    return true;
	} else {
	    return false;
	}
    }


    /**
     * Checks that a stateless session is in auto commit, otherwise reply with error message.
     * @param request
     * @param response
     * @param out
     * @param connection
     * @return
     * @throws IOException
     * @throws SQLException
     */
    private boolean checkStatelessInAutoCommit(HttpServletRequest request, HttpServletResponse response, OutputStream out, Connection connection) throws IOException, SQLException {
	
	// Don't care in stateful mode
	if (! ConfPropertiesUtil.isStatelessMode()) {
	    return true;
	}
		
	// Stateless mode: 1) can not call setAutocommit(false):
	if (ServerSqlDispatchUtil.isActionsSetAutoCommitFalse(request)) {
	    JsonErrorReturn errorReturn = new JsonErrorReturn(response, HttpServletResponse.SC_BAD_REQUEST,
		    JsonErrorReturn.ERROR_JDBC_ERROR, "AceQL Server is in Stateless Mode: can not change auto-commit mode to false.");
	    ServerSqlManager.writeLine(out, errorReturn.build());
	    return false;	    
	}
	
	// Stateless mode: 2) Connection must be in autocommit mode:
	if (! connection.getAutoCommit() ) {
	    JsonErrorReturn errorReturn = new JsonErrorReturn(response, HttpServletResponse.SC_BAD_REQUEST,
		    JsonErrorReturn.ERROR_JDBC_ERROR, "AceQL Server is in Stateless Mode: can not process SQL request because Connection must be in auto-commit.");
	    ServerSqlManager.writeLine(out, errorReturn.build());
	    return false;
	}
	else {
	    return true;
	}
	
    }



    private void dumpHeaders(HttpServletRequest request) {

	if (! DUMP_HEADERS) {
	    return;
	}

        Enumeration<String> reqHeaderEnum = request.getHeaderNames();
        while( reqHeaderEnum.hasMoreElements() ) {
            String name = reqHeaderEnum.nextElement();
            System.out.println("Header: " + name + " / " + request.getHeader(name));
       }
    }

    /**
     * Treat if action is get_version
     *
     * @param out
     * @param action
     * @throws IOException
     */
    private boolean isGetVersion(OutputStream out, String action) throws IOException {
	if (action.equals(HttpParameter.GET_VERSION)) {
	    String version = new org.kawanfw.sql.version.Version.PRODUCT().server();
	    ServerSqlManager.writeLine(out, JsonOkReturn.build("result", version));
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Dispatch the request.
     *
     * @param request
     * @param response
     * @param out
     * @param action
     * @param connection
     * @param databaseConfigurator
     * @param sqlFirewallManagers
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private void dispatch(HttpServletRequest request, HttpServletResponse response, OutputStream out, String action,
	    Connection connection, DatabaseConfigurator databaseConfigurator, List<SqlFirewallManager> sqlFirewallManagers)
	    throws SQLException, FileNotFoundException, IOException, IllegalArgumentException {
	if (ServerSqlDispatchUtil.isExecute(action) && !ServerSqlDispatchUtil.isStoredProcedure(request)) {
	    ServerStatementRawExecute serverStatement = new ServerStatementRawExecute(request, response, sqlFirewallManagers, connection);
	    serverStatement.execute(out);
	}
	else if (ServerSqlDispatchUtil.isExecuteQueryOrExecuteUpdate(action) && !ServerSqlDispatchUtil.isStoredProcedure(request)) {
	    ServerStatement serverStatement = new ServerStatement(request, response, sqlFirewallManagers, connection);
	    serverStatement.executeQueryOrUpdate(out);
	}
	else if (ServerSqlDispatchUtil.isStatementExecuteBatch(action)) {
	    ServerStatementBatch serverStatement = new ServerStatementBatch(request, response, sqlFirewallManagers, connection, databaseConfigurator);
	    serverStatement.executeBatch(out);
	}
	else if (ServerSqlDispatchUtil.isPreparedStatementExecuteBatch(action)) {
	    ServerPreparedStatementBatch serverPreparedStatementBatch = new ServerPreparedStatementBatch(request, response, sqlFirewallManagers, connection, databaseConfigurator);
	    serverPreparedStatementBatch.executeBatch(out);
	}
	else if (ServerSqlDispatchUtil.isStoredProcedure(request)) {
	    ServerCallableStatement serverCallableStatement = new ServerCallableStatement(request, response,
		    sqlFirewallManagers, connection);
	    serverCallableStatement.executeOrExecuteQuery(out);
	} else if (ServerSqlDispatchUtil.isConnectionModifier(action)) {
	    TransactionUtil.setConnectionModifierAction(request, response, out, action, connection);
	} else if (ServerSqlDispatchUtil.isSavepointModifier(action)) {
	    SavepointUtil.setSavepointExecute(request, response, out, action, connection);
	} else if (ServerSqlDispatchUtil.isConnectionReader(action)) {
	    TransactionUtil.getConnectionionInfosExecute(request, response, out, action, connection);
	} else {
	    throw new IllegalArgumentException("Invalid Sql Action: " + action);
	}
    }

    /**
     * Treat
     * @param request
     * @param response
     * @param out
     * @param action
     * @param connection
     * @param sqlFirewallManagers
     * @return
     */
    private boolean doTreatJdbcDatabaseMetaData(HttpServletRequest request, HttpServletResponse response,
	    OutputStream out, String action, Connection connection, List<SqlFirewallManager> sqlFirewallManagers)
		    throws SQLException, IOException {
	// Redirect if it's a JDBC DatabaseMetaData call
	if (ActionUtil.isJdbcDatabaseMetaDataQuery(action)) {
	    JdbcDatabaseMetadataActionManager jdbcDatabaseMetadataActionManager = new JdbcDatabaseMetadataActionManager(request, response,
		    out, sqlFirewallManagers, connection);
	    jdbcDatabaseMetadataActionManager.execute();
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Tread metadata query.
     *
     * @param request
     * @param response
     * @param out
     * @param action
     * @param connection
     * @param sqlFirewallManagers
     * @throws SQLException
     * @throws IOException
     */
    private boolean doTreatMetadataQuery(HttpServletRequest request, HttpServletResponse response, OutputStream out,
	    String action, Connection connection, List<SqlFirewallManager> sqlFirewallManagers)
	    throws SQLException, IOException {
	// Redirect if it's a metadaquery
	if (ActionUtil.isMetadataQueryAction(action)) {
	    MetadataQueryActionManager metadataQueryActionManager = new MetadataQueryActionManager(request, response,
		    out, sqlFirewallManagers, connection);
	    metadataQueryActionManager.execute();
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * @param response
     * @param out
     * @param username
     * @param sessionId
     * @param connectionId
     * @param databaseConfigurator
     * @param connection
     * @throws IOException
     */
    private void treatCloseAction(HttpServletResponse response, OutputStream out, String username, String sessionId,
	    final String connectionId, DatabaseConfigurator databaseConfigurator, Connection connection) throws IOException {
	try {
	    
	    // Nothing to do in stateless
	    if (ConfPropertiesUtil.isStatelessMode()) {
		ServerSqlManager.writeLine(out, JsonOkReturn.build());
		return;
	    }
	    
	    databaseConfigurator.close(connection);

	    String connectionIdNew = connectionId;
	    if (connectionIdNew == null) {
		connectionIdNew = ConnectionIdUtil.getConnectionId(connection);
	    }
	    ConnectionStore connectionStore = new ConnectionStore(username, sessionId, connectionIdNew);
	    connectionStore.remove();
	    ServerSqlManager.writeLine(out, JsonOkReturn.build());
	} catch (SQLException e) {
	    RollbackUtil.rollback(connection);

	    JsonErrorReturn errorReturn = new JsonErrorReturn(response, HttpServletResponse.SC_BAD_REQUEST,
		    JsonErrorReturn.ERROR_JDBC_ERROR, e.getMessage());
	    ServerSqlManager.writeLine(out, errorReturn.build());

	} catch (Exception e) {
	    RollbackUtil.rollback(connection);
	    
	    JsonErrorReturn errorReturn = new JsonErrorReturn(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		    JsonErrorReturn.ERROR_ACEQL_FAILURE, e.getMessage(), ExceptionUtils.getStackTrace(e));
	    ServerSqlManager.writeLine(out, errorReturn.build());
	}
    }


    /**
     * @param request
     * @param response
     * @param out TODO
     * @throws IOException
     * @throws FileUploadException
     * @throws SQLException
     */
    private boolean doBlobUpload(HttpServletRequest request, HttpServletResponse response, OutputStream out)
	    throws IOException, FileUploadException, SQLException {
	// Immediate catch if we are asking a file upload, because
	// parameters are in unknown sequence.
	// We know it's a upload action if it's mime Multipart
	if (ServletFileUpload.isMultipartContent(request)) {
	    BlobUploader blobUploader = new BlobUploader(request, response, out);
	    blobUploader.blobUpload();
	    return true;
	} else {
	    return false;
	}
    }


    public static void debug(String s) {
	if (DEBUG) {
	    System.out.println(new Date() + " " + s);
	}
    }
}
