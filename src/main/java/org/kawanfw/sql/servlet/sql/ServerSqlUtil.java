/*
 * Copyright (c)2022 KawanSoft S.A.S.
 * This file is part of AceQL HTTP.
 * AceQL HTTP: SQL Over HTTP
 * 
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file in the project's root directory.
 *
 * Change Date: 2027-08-30
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by version 2.0 of the Apache License.
 */
package org.kawanfw.sql.servlet.sql;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.kawanfw.sql.api.server.DatabaseConfigurator;
import org.kawanfw.sql.servlet.util.max_rows.MaxRowsSetter;
import org.kawanfw.sql.servlet.util.max_rows.MaxRowsSetterCreator;
import org.kawanfw.sql.util.FrameworkDebug;

/**
 *
 * Utility class to use for SQL on the server side:
 * <ul>
 * <li>setConnectionProperties: set the Connection properties on the server
 * side.</li>
 * <li>decryptSqlOrder: decrypt the SQL orders on the server.</li>
 * </ul>
 *
 * @author Nicolas de Pomereu
 */
public class ServerSqlUtil {

    private static boolean DEBUG = FrameworkDebug.isSet(ServerSqlUtil.class);

    /**
     * Protected constructor
     */
    protected ServerSqlUtil() {

    }

    /**
     * Set the maximum rows to return to the client side
     *
     * @param request
     * @param username
     * @param database
     * @param statement            the statement to set
     * @param databaseConfigurator the DatabaseConfigurator which contains the
     *                             getMaxRowsToReturn() method
     *
     * @throws SQLException
     * @throws IOException
     */
    public static void setMaxRowsToReturn(HttpServletRequest request, String username, String database,
	    Statement statement, DatabaseConfigurator databaseConfigurator) throws SQLException, IOException {

	MaxRowsSetter maxRowsSetter = MaxRowsSetterCreator.createInstance();
	maxRowsSetter.setMaxRows(request, username, database, statement, databaseConfigurator);
	
    }

    /**
     * @param s
     */

    public static void debug(String s) {
	if (DEBUG) {
	    System.out.println(new Date() + " " + s);
	}
    }
}
