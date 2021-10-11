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
package org.kawanfw.sql.util;

/**
 * @author Nicolas de Pomereu
 *
 */

public class ConnectionParms {

    public static final String CONNECTION_ID = "CONNECTION_ID";

    public static final String AUTOCOMMIT = "AUTOCOMMIT";
    public static final String READONLY = "READONLY";
    public static final String HOLDABILITY = "HOLDABILITY";
    public static final String TRANSACTION_ISOLATION = "TRANSACTION_ISOLATION";

    public static final String NO_PARM = "NO_PARM";
    // Connection Info
    public static final String TIMEOUT = "timeout";
    public static final String VALUE = "value";
    public static final String PROPERTIES = "properties";

    public static final String SCHEMA = "schema";

    public static final String ELEMENTS = "elements";
    public static final String TYPENAME = "typename";

    /**
     * Protected Constructor
     */
    protected ConnectionParms() {
    }

}
