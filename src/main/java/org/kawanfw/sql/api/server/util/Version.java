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
package org.kawanfw.sql.api.server.util;

import org.kawanfw.sql.version.VersionWrapper;

/**
 *
 * Allows to get version info.
 *
 * @author Nicolas de Pomereu
 *
 */

public class Version {

    /**
     * Returns the product name, version and date
     * @return the product name, version and date
     */
    public String getVersion() {
	return VersionWrapper.getServerVersion();
    }

}
