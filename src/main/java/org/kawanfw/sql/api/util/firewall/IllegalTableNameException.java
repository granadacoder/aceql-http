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
package org.kawanfw.sql.api.util.firewall;

import java.io.File;

public class IllegalTableNameException extends IllegalArgumentException {

    private static final long serialVersionUID = -1392006668676537022L;

    private String table = null;
    private int lineNumber = -1;

    public IllegalTableNameException(File file, String table, int lineNumber) {
	super(file.getName() + ": " + "table \"" + table + "\" does no exists in database (line " + lineNumber + ").");
	this.table = table;
	this.lineNumber = lineNumber;
    }

    public String getTable() {
	return table;
    }

    public int getLineNumber() {
	return lineNumber;
    }

}
