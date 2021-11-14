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

import org.kawanfw.sql.api.server.blob.BlobDownloadConfigurator;
import org.kawanfw.sql.api.server.blob.DefaultBlobDownloadConfigurator;

/**
 * @author Nicolas de Pomereu
 *
 */
public class BlobDownloadConfiguratorCreator {

    private BlobDownloadConfigurator blobDownloadConfigurator = null;
    private String blobDownloadConfiguratorClassName = null;

    private static String[] PREDEFINED_CLASS_NAMES = { DefaultBlobDownloadConfigurator.class.getSimpleName(), };

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
		String theClassNameNew = DefaultBlobDownloadConfigurator.class.getPackage().getName() + "."
			+ theClassName;
		return theClassNameNew;
	    }
	}

	return theClassName;
    }

    public BlobDownloadConfiguratorCreator(final String theBlobDownloadConfiguratorClassName)
	    throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {

	if (theBlobDownloadConfiguratorClassName != null && !theBlobDownloadConfiguratorClassName.isEmpty()) {

	    String theBlobDownloadConfiguratorClassNameNew = getNameWithPackage(theBlobDownloadConfiguratorClassName);

	    Class<?> c = Class.forName(theBlobDownloadConfiguratorClassNameNew);
	    Constructor<?> constructor = c.getConstructor();
	    blobDownloadConfigurator = (BlobDownloadConfigurator) constructor.newInstance();
	    this.blobDownloadConfiguratorClassName = theBlobDownloadConfiguratorClassNameNew;
	} else {
	    blobDownloadConfigurator = new DefaultBlobDownloadConfigurator();
	    this.blobDownloadConfiguratorClassName = blobDownloadConfigurator.getClass().getName();
	}

    }

    public BlobDownloadConfigurator getBlobDownloadConfigurator() {
	return blobDownloadConfigurator;
    }

    public String getBlobDownloadConfiguratorClassName() {
	return blobDownloadConfiguratorClassName;
    }

}
