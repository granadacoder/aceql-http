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
package org.kawanfw.test.api.server.auth;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.kawanfw.sql.api.server.auth.SshUserAuthenticator;
import org.kawanfw.sql.servlet.injection.properties.PropertiesFileStore;

/**
 * @author Nicolas de Pomereu
 *
 */
public class SshUserAuthenticatorTest {

    /**
     * Tests a login using SSH.
     * @throws IOException
     * @throws SQLException
     */
    public static void test() throws IOException, SQLException {
	PropertiesFileStore.set(new File("I:\\_dev_awake\\aceql-http-main\\aceql-http\\conf\\aceql-server.properties"));
	SshUserAuthenticator sshUserAuthenticator = new SshUserAuthenticator();
	String password = FileUtils.readFileToString(new File("I:\\__NDP\\_MyPasswords\\login_user1.txt"), "UTF-8");
	boolean logged = sshUserAuthenticator.login("user1", password.toCharArray(), "database", "10.0.0.10");
	System.out.println(new Date() + " SshUserAuthenticator logged: " + logged);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	test();
    }



}
