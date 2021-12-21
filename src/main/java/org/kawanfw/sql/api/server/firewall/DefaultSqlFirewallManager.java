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
package org.kawanfw.sql.api.server.firewall;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kawanfw.sql.api.server.DefaultDatabaseConfigurator;
import org.kawanfw.sql.api.server.SqlEvent;

/**
 * Default firewall manager for all SQL databases. <br>
 * <br>
 * <b>WARNING</b>: This default implementation will allow to start immediate
 * remote SQL calls but is <b>*not*</b> at all secured. <br>
 * <b>It is highly recommended to override this class with a secured
 * implementation for all methods.</b>
 *
 * @author Nicolas de Pomereu
 * @since 4.0
 */
public class DefaultSqlFirewallManager implements SqlFirewallManager {

    /**
     * @return <code><b>true</b></code>. (Client programs will be allowed to create
     *         raw <code>Statement</code>, i.e. call statements without parameters.)
     */
    @Override
    public boolean allowStatementClass(String username, String database, Connection connection)
	    throws IOException, SQLException {
	return true;
    }

    /**
     * @return <code><b>true</b></code>. No analysis is done so all SQL statements
     *         are authorized.
     */
    @Override
    public boolean allowSqlRunAfterAnalysis(SqlEvent sqlEvent, Connection connection) throws IOException, SQLException {
	return true;
    }

    /**
     * @return <code><b>true</b></code>. (Client programs will be allowed to call a
     *         JDBC raw {@code Statement.execute}.)
     */
    @Override
    public boolean allowExecute(String username, String database, Connection connection)
	    throws IOException, SQLException {
	return true;
    }

    /**
     * @return <code><b>true</b></code>. (Client programs will be allowed to call a
     *         database update statement.)
     */
    @Override
    public boolean allowExecuteUpdate(String username, String database, Connection connection)
	    throws IOException, SQLException {
	return true;
    }

    /**
     * @return <code><b>true</b></code>. (Client programs will be allowed to call
     *         the Metadata Query API).
     */
    @Override
    public boolean allowMetadataQuery(String username, String database, Connection connection)
	    throws IOException, SQLException {
	return true;
    }

    /**
     * Logs the info using {@code DefaultDatabaseConfigurator#getLogger()}
     * {@code Logger}.
     */
    @Override
    public void runIfStatementRefused(SqlEvent sqlEvent, Connection connection) throws IOException, SQLException {
	String logInfo = null;

	if (sqlEvent.isMetadataQuery()) {
	    logInfo = "Client username " + sqlEvent.getUsername() + " (IP: " + sqlEvent.getIpAddress()
		    + ") has been denied by DefaultSqlFirewallManager SqlFirewallManager executing a Metadata Query API.";
	} else {
	    logInfo = "Client username " + sqlEvent.getUsername() + " (IP: " + sqlEvent.getIpAddress()
		    + ") has been denied by DefaultSqlFirewallManager SqlFirewallManager executing sql statement: "
		    + sqlEvent.getSql() + " with parameters: " + sqlEvent.getParameterStringValues();
	}

	DefaultDatabaseConfigurator defaultDatabaseConfigurator = new DefaultDatabaseConfigurator();
	Logger logger = defaultDatabaseConfigurator.getLogger();
	logger.log(Level.WARNING, logInfo);

    }
}
