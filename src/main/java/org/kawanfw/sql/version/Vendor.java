/*
 * Copyright (c)2022 KawanSoft S.A.S. All rights reserved.
 * 
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file in the project's root directory.
 *
 * Change Date: 2027-08-31
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by version 2.0 of the Apache License.
 */
package org.kawanfw.sql.version;
class Vendor {
    public static final String NAME = "KawanSoft SAS";
    public static final String WEB = "http://www.kawansoft.com";
    public static final String COPYRIGHT = "Copyright &copy; 2022";
    public static final String EMAIL = "contact@kawansoft.com";
    @Override
    public String toString() {
	return Vendor.NAME + " - " + Vendor.WEB;
    }
}