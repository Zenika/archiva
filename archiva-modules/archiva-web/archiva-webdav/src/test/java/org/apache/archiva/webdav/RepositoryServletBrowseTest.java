package org.apache.archiva.webdav;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * RepositoryServletBrowseTest
 *
 *
 */
public class RepositoryServletBrowseTest
    extends AbstractRepositoryServletTestCase
{
    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        new File( repoRootInternal, "org/apache/archiva" ).mkdirs();
        new File( repoRootInternal, "org/codehaus/mojo/" ).mkdirs();
        new File( repoRootInternal, "net/sourceforge" ).mkdirs();
        new File( repoRootInternal, "commons-lang" ).mkdirs();
    }

    @Test
    public void testBrowse()
        throws Exception
    {
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        // dumpResponse( response );

        String expectedLinks[] = new String[]{ ".indexer/", "commons-lang/", "net/", "org/" };
        assertLinks( expectedLinks, response.getLinks() );
    }

    @Test
    public void testBrowseSubdirectory()
        throws Exception
    {
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/org" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        String expectedLinks[] = new String[]{ "../", "apache/", "codehaus/" };
        assertLinks( expectedLinks, response.getLinks() );
    }

    @Test
    public void testGetDirectoryWhichHasMatchingFile() //MRM-893
        throws Exception
    {
        new File( repoRootInternal, "org/apache/archiva/artifactId/1.0" ).mkdirs();
        new File( repoRootInternal, "org/apache/archiva/artifactId/1.0/artifactId-1.0.jar" ).createNewFile();

        WebRequest request =
            new GetMethodWebRequest( "http://machine.com/repository/internal/org/apache/archiva/artifactId" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertEquals( "1st Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        request = new GetMethodWebRequest( "http://machine.com/repository/internal/org/apache/archiva/artifactId/" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "2nd Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/org/apache/archiva/artifactId/1.0/artifactId-1.0.jar" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "3rd Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/org/apache/archiva/artifactId/1.0/artifactId-1.0.jar/" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "4th Response", HttpServletResponse.SC_NOT_FOUND, response.getResponseCode() );
    }


    private void assertLinks( String expectedLinks[], WebLink actualLinks[] )
    {
        assertEquals( "Links.length", expectedLinks.length, actualLinks.length );
        for ( int i = 0; i < actualLinks.length; i++ )
        {
            assertEquals( "Link[" + i + "]", expectedLinks[i], actualLinks[i].getURLString() );
        }
    }
}
