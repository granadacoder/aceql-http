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
package org.kawanfw.sql.servlet.injection.classes.validator;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.kawanfw.sql.servlet.injection.properties.PropertiesFileUtil;
import org.kawanfw.sql.util.SqlTag;
import org.kawanfw.sql.util.Tag;
import org.kawanfw.sql.version.EditionUtil;

/**
 * Valid
 * 
 * @author Nicolas de Pomereu
 *
 */
public class EnterpriseWarner {

    private static final int MAX_QUEUE_CAPACITY_RECOMMANDED = 100;
    private static final int MIN_KEEP_ALIVE_TIME_RECOMMANDED = 60;
    
    private String propertiesFile;

    public EnterpriseWarner(String propertiesFile) {
	this.propertiesFile = propertiesFile;
    }

    public void warnOnThreadPoolExecutorParams() throws IOException {
	// Nothing todo if Community Edition
	if (EditionUtil.isCommunityEdition()) {
	    return;
	}

	File file = new File(propertiesFile);
	Properties properties = PropertiesFileUtil.getProperties(file);

	treatCapacityWarningMessage(properties);
	treatkeepAliveTimeWarningMessage(properties);

    }

    /**
     * @param properties
     * @throws NumberFormatException
     */
    public void treatCapacityWarningMessage(Properties properties) throws NumberFormatException {
	String workQueueClassName = properties.getProperty("workQueueClassName");

	// No problem for an always empty SynchronousQueue
	if (workQueueClassName != null
		&& !workQueueClassName.equals(java.util.concurrent.SynchronousQueue.class.getName())) {
	    // if capacity > 100, display warning message
	    String capcacityStr = properties.getProperty("capacity");
	    if (capcacityStr == null || capcacityStr.isEmpty()) {
		return;
	    }

	    // We have checked before format is OK
	    int capacity = Integer.parseInt(capcacityStr);

	    if (capacity > MAX_QUEUE_CAPACITY_RECOMMANDED) {
		System.err.println(SqlTag.SQL_PRODUCT_START + " " + Tag.WARNING
			+ " In Enterprise Edition, the ThreadPoolExecutor Queue \"" + "capacity" + "\" property"
			+ " should not be > " + MAX_QUEUE_CAPACITY_RECOMMANDED + ValidatorUtil.CR_LF
			+ SqlTag.SQL_PRODUCT_START + ValidatorUtil.WARNING_SECOND_LINE_SPACES + " "
			+ " because of a potential SQL run bottleneck. (Set value in .properties file: " + capacity
			+ ")");
	    }
	}
    }

    /**
     * @param properties
     * @throws NumberFormatException
     */
    public void treatkeepAliveTimeWarningMessage(Properties properties) throws NumberFormatException {
	String keepAliveTimeStr = properties.getProperty("keepAliveTime");

	if (keepAliveTimeStr != null && ! keepAliveTimeStr.isEmpty()) {

	    // We have checked before format is OK
	    int keepAliveTime = Integer.parseInt(keepAliveTimeStr);

	    if (keepAliveTime < MIN_KEEP_ALIVE_TIME_RECOMMANDED) {
		System.err.println(SqlTag.SQL_PRODUCT_START + " " + Tag.WARNING
			+ " In Enterprise Edition, the ThreadPoolExecutor  \"" + "keepAliveTime" + "\" property"
			+ " should not be < " + MIN_KEEP_ALIVE_TIME_RECOMMANDED + ValidatorUtil.CR_LF
			+ SqlTag.SQL_PRODUCT_START + ValidatorUtil.WARNING_SECOND_LINE_SPACES + " "
			+ " because of a potential SQL run abort. (Set value in .properties file: " + keepAliveTime
			+ ")");
	    }
	}
    }

}
