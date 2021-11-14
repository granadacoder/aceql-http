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

package org.kawanfw.sql.servlet.injection.classes.creator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.kawanfw.sql.api.server.blob.BlobUploadConfigurator;
import org.kawanfw.sql.api.server.blob.DefaultBlobUploadConfigurator;

public class BlobUploadConfiguratorCreator {

    private BlobUploadConfigurator blobUploadConfigurator = null;
    private String blobUploadConfiguratorClassName = null;

    private static String[] PREDEFINED_CLASS_NAMES = {
	    DefaultBlobUploadConfigurator.class.getSimpleName(),
	    };

    /**
     * Allows to add automatically the package for predefined classes
     *
     * @param theClassName
     * @return
     */
    private static String getNameWithPackage(final String theClassName) {

	for (int i = 0; i < PREDEFINED_CLASS_NAMES.length; i++) {
	    if (PREDEFINED_CLASS_NAMES[i].equals(theClassName)) {
		// Add prefix package
		String theClassNameNew = DefaultBlobUploadConfigurator.class.getPackage()
			.getName() + "." + theClassName;
		return theClassNameNew;
	    }
	}

	return theClassName;
    }

    public BlobUploadConfiguratorCreator(final String theBlobUploadConfiguratorClassName)
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {

	if (theBlobUploadConfiguratorClassName != null && !theBlobUploadConfiguratorClassName.isEmpty()) {

	    String theBlobUploadConfiguratorClassNameNew = getNameWithPackage(theBlobUploadConfiguratorClassName);

	    Class<?> c = Class.forName(theBlobUploadConfiguratorClassNameNew);
	    Constructor<?> constructor = c.getConstructor();
	    blobUploadConfigurator = (BlobUploadConfigurator) constructor.newInstance();
	    this.blobUploadConfiguratorClassName = theBlobUploadConfiguratorClassNameNew;
	} else {
	    blobUploadConfigurator = new DefaultBlobUploadConfigurator();
	    this.blobUploadConfiguratorClassName = blobUploadConfigurator.getClass().getName();
	}

    }

    public BlobUploadConfigurator getBlobUploadConfigurator() {
        return blobUploadConfigurator;
    }

    public String getBlobUploadConfiguratorClassName() {
        return blobUploadConfiguratorClassName;
    }

}
