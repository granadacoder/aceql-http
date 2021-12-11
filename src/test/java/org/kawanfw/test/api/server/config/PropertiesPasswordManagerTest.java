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
package org.kawanfw.test.api.server.config;

import java.io.FileInputStream;
import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.EncryptableProperties;

/**
 * @author Nicolas de Pomereu
 *
 */
public class PropertiesPasswordManagerTest {


    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	 StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
	 encryptor.setPassword("azerty123"); // could be got from web, env variable...
	 encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
	 encryptor.setIvGenerator(new RandomIvGenerator());
	 
	 /*
	  * Create our EncryptableProperties object and load it the usual way.
	  */
	 Properties props = new EncryptableProperties(encryptor);
	 props.load(new FileInputStream("I:\\_dev_awake\\aceql-http-main\\aceql-http\\conf\\aceql-server.properties"));
	 
	 System.out.println(props.get("sampledb.username"));
	 System.out.println(props.get("sampledb.password"));
    }

}
