/**
 * 
 */
package org.kawanfw.test.api.server.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kawanfw.sql.api.server.DefaultDatabaseConfigurator;
import org.kawanfw.sql.api.server.SqlEvent;
import org.kawanfw.sql.api.server.firewall.SqlFirewallManager;
import org.kawanfw.sql.api.server.firewall.SqlFirewallTrigger;

/**
 * @author Nicolas de Pomereu
 * 

org.kawanfw.test.api.server.config.MySqlFirewallTrigger
 
 */
public class MySqlFirewallTrigger implements SqlFirewallTrigger {


    @Override
    public void runIfStatementRefused(SqlEvent sqlEvent, SqlFirewallManager sqlFirewallManager, Connection connection)
	    throws IOException, SQLException {
	String logInfo = null;

	String sqlFirewallManagerClassName = sqlFirewallManager.getClass().getSimpleName();

	if (sqlEvent.isMetadataQuery()) {
	    logInfo = "==> Client username " + sqlEvent.getUsername() + " (IP: " + sqlEvent.getIpAddress()
		    + ") has been denied by " + sqlFirewallManagerClassName
		    + " SqlFirewallManager executing a Metadata Query API.";
	} else {
	    logInfo = "==> Client username " + sqlEvent.getUsername() + " (IP: " + sqlEvent.getIpAddress()
		    + ") has been denied by " + sqlFirewallManagerClassName
		    + " SqlFirewallManager executing sql statement: " + sqlEvent.getSql() + " with parameters: "
		    + sqlEvent.getParameterStringValues();
	}

	DefaultDatabaseConfigurator defaultDatabaseConfigurator = new DefaultDatabaseConfigurator();
	Logger logger = defaultDatabaseConfigurator.getLogger();
	logger.log(Level.WARNING, logInfo);
    }

}
