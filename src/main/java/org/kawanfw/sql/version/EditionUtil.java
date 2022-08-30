/*
 * Copyright (c)2022 KawanSoft S.A.S.
 * This file is part of AceQL HTTP.
 * AceQL HTTP: SQL Over HTTP
 * 
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file in the project's root directory.
 *
 * Change Date: 2027-08-30
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by version 2.0 of the Apache License.
 */
package org.kawanfw.sql.version;

import java.io.File;

import org.kawanfw.sql.servlet.AceQLLicenseFileFinder;

/**
 * @author Nicolas de Pomereu
 *
 */
public class EditionUtil {

    /**
     * Says is edition is Community or Enterprise.
     */
    public static boolean isCommunityEdition() {
	File licenseFile = AceQLLicenseFileFinder.getLicenseFile();
	return licenseFile == null ? true : false;
    }

}
