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
package org.kawanfw.sql.api.server.auth.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.kawanfw.sql.servlet.injection.properties.PropertiesFileStore;
import org.kawanfw.sql.util.FrameworkDebug;
import org.kawanfw.sql.util.SqlTag;

/**
 * This default implementation will extract the password from the "password"
 * property of the file {@code properties_password_manager.properties} which
 * must be located in the same directory as the {@code aceql-server.properties}
 * file. <br/>
 * <br/>
 * This default implementation is provided <i>as is</i>: password is not secured
 * if an attacker gets access to the server. <br/>
 * Note that the {@link #getPassword()} will return {@code null} if the file
 * does not exists. <br/>
 * <br/>
 * 
 * @author Nicolas de Pomereu
 *
 */
public class DefaultPropertiesPasswordManager implements PropertiesPasswordManager {

    /** Debug info */
    private static boolean DEBUG = FrameworkDebug.isSet(DefaultPropertiesPasswordManager.class);
    
    /**
     * Returns the value of the "password" property contained in the file
     * {@code properties_password_manager.properties} which must be located in the
     * same directory as the {@code aceql-server.properties} file. <br>
     * Returns {@code null} if the file does not exist.
     */
    @Override
    public char[] getPassword() throws IOException, SQLException {

	File dir = PropertiesFileStore.get().getParentFile();
	File file = new File(dir + File.separator + "properties_password_manager.properties");
	
	debug("Dir of aceql-server.properties                :" + dir);
	debug("File of properties_password_manager.properties:" + file);
	
	if (!file.exists()) {
	    debug(file.toString() + " does not exist. No decryption todo.");
	    return null;
	}

	Properties properties = new Properties();
	try (InputStream in = new FileInputStream(file);) {
	    properties.load(in);
	}

	String password = properties.getProperty("password");
	debug("password: " + password);

	if (password == null || password.isEmpty()) {
	    throw new IOException(SqlTag.USER_CONFIGURATION + " password property not defined in file: " + file);
	}

	return password.toCharArray();
    }

    /**
     * Print debug info
     *
     * @param s
     */

    private static void debug(String s) {
	if (DEBUG)
	    System.out.println(new Date() + " "  + DefaultPropertiesPasswordManager.class.getSimpleName() + " " + s);
    }
}
