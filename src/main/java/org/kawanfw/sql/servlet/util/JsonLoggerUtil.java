/*
 * Copyright (c)2022 KawanSoft S.A.S. All rights reserved.
 * 
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file in the project's root directory.
 *
 * Change Date: 2026-09-01
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by version 2.0 of the Apache License.
 */
package org.kawanfw.sql.servlet.util;
/**
 * @author Nicolas de Pomereu
 *
 */

public class JsonLoggerUtil {
    /**
     * Returns the simple name + ".log" of this class
     * @return the simple name + ".log" of this class
     */
    public static String getSimpleName(Class<?> clazz) {
	return clazz.getSimpleName() + ".log";
    }

}
