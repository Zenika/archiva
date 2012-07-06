package org.apache.archiva.web.startup;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * BannerTest
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class BannerTest
    extends TestCase
{
    private static final String eol = System.getProperty( "line.separator" );

    private void assertEncodeDecode( String encoded, String decoded )
    {
        assertEquals( "Encoding: ", encoded, Banner.encode( decoded ) );
        assertEquals( "Decoding: ", decoded, Banner.decode( encoded ) );
    }
    
    @Test
    public void testEncodeDecode()
    {
        assertEncodeDecode( "[$10 ]", "[          ]" );
        assertEncodeDecode( "$$$5_$n$5_", "$_____" + eol + "_____" );
        assertEncodeDecode( "$${Refgjuvyr}", "${Erstwhile}" );
    }
    
    @Test
    public void testInjectVersion()
    {
        assertEquals( "[ 1.0 ]", Banner.injectVersion( "[#####]", "1.0" ) );
        assertEquals( ".\\  1.0-SNAPSHOT  \\._____",
                      Banner.injectVersion( ".\\################\\._____", "1.0-SNAPSHOT" ) );
        assertEquals( "Archiva:\"+eol+\" ( 1.0-alpha-1  )",
                      Banner.injectVersion( "Archiva:\"+eol+\" (##############)", "1.0-alpha-1" ) );
    }

    @Test
    public void testGetBanner()
        throws IOException
    {
        String version = "1.0-alpha-1-SNAPSHOT";
        String banner = Banner.getBanner( version );
        assertNotNull( "Banner should not be null.", banner );
        assertTrue( "Banner contains version.", banner.indexOf( version ) > 0 );

        /* Want to make a new banner?
        * Steps to do it.
        * 1) Edit the src/test/resources/banner.gz file.
        * 2) Save it compressed.
        * 3) Add (to this test method) ...
        *    System.out.println( "\"" + Banner.encode( getRawBanner() ) + "\"" );
        * 4) Run the test
        * 5) Copy / Paste the encoded form into the Banner.getBanner() method.
        */
    }

    public String getRawBanner()
        throws IOException
    {
        File gzBanner = new File( "src/test/resources/banner.gz" );
        assertTrue( "File [" + gzBanner.getPath() + "] not found.", gzBanner.exists() );
        FileInputStream fis = new FileInputStream( gzBanner );
        GZIPInputStream gzis = new GZIPInputStream( fis );
        String str = IOUtils.toString( gzis );
        IOUtils.closeQuietly( gzis );
        return str;
    }
}
