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
package org.kawanfw.sql.api.server.session;

import java.util.Date;
import java.util.Objects;

/**
 *
 * Utility holder class for session info.
 *
 * @author Nicolas de Pomereu
 */
public class SessionInfo {

    private String sessionId = null;
    private String username = null;
    private String database = null;
    private long creationTime;

    /**
     * Constructor
     *
     * @param sessionId the unique session id that is generated after login by
     *                  {@link SessionConfigurator#generateSessionId(String, String)}
     * @param username  the logged client username
     * @param database  the database to use for this session
     */
    public SessionInfo(String sessionId, String username, String database) {

	Objects.requireNonNull(sessionId, "file cannot be null!");
	Objects.requireNonNull(username, "username cannot be null!");
	Objects.requireNonNull(database, "database cannot be null!");

	this.sessionId = sessionId;
	this.username = username;
	this.database = database;

	this.creationTime = new Date().getTime();
    }

    /**
     * Returns the session id
     *
     * @return the session id
     */
    public String getSessionId() {
	return sessionId;
    }

    /**
     * Returns the client username
     *
     * @return the client username
     */
    public String getUsername() {
	return username;
    }

    /**
     * Returns the database in use for this session
     *
     * @return the database in use for this session
     */
    public String getDatabase() {
	return database;
    }

    /**
     * Returns the date/time in milliseconds when this {@code SessionInfo} instance was created
     * @return the date/time in milliseconds when this {@code SessionInfo} instance was created
     */
    public long getCreationTimeMillis() {
	return creationTime;
    }

}
