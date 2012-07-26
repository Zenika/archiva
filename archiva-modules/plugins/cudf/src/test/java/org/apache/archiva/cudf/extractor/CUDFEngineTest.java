package org.apache.archiva.cudf.extractor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.cudf.extractor.CUDFExtractor;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class CUDFEngineTest
{

    private static final String TEST_REPO = "default-repository";

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( "repositorySessionFactory#file" )
    private RepositorySessionFactory factory;

    @Before
    public void setUp()
        throws Exception
    {
        ManagedRepositoryConfiguration configuration = new ManagedRepositoryConfiguration();
        configuration.setLocation( "src/test/repositories/default-repository" );
        configuration.setId( TEST_REPO );
        configuration.setName( TEST_REPO );
        archivaConfiguration.getConfiguration().addManagedRepository( configuration );
    }

    @Test
    public void testCone()
        throws IOException
    {
        StringWriter writer = new StringWriter();
        CUDFExtractor extractor = new CUDFExtractor( writer );
        extractor.computeCUDFCone( "commons-lang", "commons-lang", "2.1", "jar", TEST_REPO, getRepositoriesList(),
                                   factory );
        String result = writer.toString();
        assertTrue( result.contains( "package: commons-lang%3acommons-lang" ) );
        assertTrue( result.contains( "number: 2.1" ) );
        assertTrue( result.contains( "version: 1" ) );
        assertTrue( result.contains( "type: jar" ) );
        assertTrue( result.contains( "depends: junit%3ajunit = 1" ) );
    }

    @Test
    public void testConeWithLongOrganisation()
        throws IOException
    {
        StringWriter writer = new StringWriter();
        CUDFExtractor extractor = new CUDFExtractor( writer );
        extractor.computeCUDFCone( "com.zenika", "cudf", "1.0", "jar", TEST_REPO, getRepositoriesList(),
                                   factory );
        String result = writer.toString();
        assertTrue( result.contains( "package: com.zenika%3acudf" ) );
        assertTrue( result.contains( "number: 1.0" ) );
        assertTrue( result.contains( "version: 1" ) );
        assertTrue( result.contains( "type: jar" ) );
    }

    @Test
    public void testCUDFVersion()
        throws IOException
    {
        StringWriter writer = new StringWriter();
        CUDFExtractor extractor = new CUDFExtractor( writer );
        extractor.computeCUDFCone( "com.zenika", "cudf-version", "1.3-RELEASE", "jar", TEST_REPO, getRepositoriesList(),
                                   factory );
        String result = writer.toString();
        assertTrue( result.contains( "version: 6" ) );
    }

    @Test
    public void testUniverse()
        throws IOException
    {
        StringWriter writer = new StringWriter();
        CUDFExtractor extractor = new CUDFExtractor( writer );
        extractor.computeCUDFUniverse( getRepositoriesList(), factory );
        String result = writer.toString();
        System.out.println( result );
        assertTrue( result.contains( "package: commons-lang%3acommons-lang" ) );
        assertTrue( result.contains( "package: commons-logging%3acommons-logging" ) );
    }

    @Test
    public void charToHexaConverto()
    {
        CUDFExtractor extractor = new CUDFExtractor( null );
        assertTrue( "%3a".equalsIgnoreCase( extractor.encodingString( ":" ) ) );
        assertTrue( "%5f".equalsIgnoreCase( extractor.encodingString( "_" ) ) );
    }

    private List<String> getRepositoriesList()
    {
        List<String> repositories = new ArrayList<String>();
        repositories.add( TEST_REPO );
        return repositories;
    }
}
