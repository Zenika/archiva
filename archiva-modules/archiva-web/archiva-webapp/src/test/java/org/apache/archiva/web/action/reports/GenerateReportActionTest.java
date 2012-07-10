package org.apache.archiva.web.action.reports;

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

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.Action;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.webtest.memory.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.archiva.security.UserRepositoriesStub;
import org.apache.commons.io.IOUtils;
import org.apache.archiva.web.action.AbstractActionTestCase;
import org.easymock.MockControl;
import org.junit.After;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the GenerationReportAction. Note that we are testing for <i>current</i> behaviour, however there are several
 * instances below where other behaviour may actually be more appropriate (eg the error handling, download stats should
 * never forward to HTML page, etc). This is also missing tests for various combinations of paging at this point.
 */
public class GenerateReportActionTest
    extends AbstractActionTestCase
{
    private GenerateReportAction action;

    private static final String SNAPSHOTS = "snapshots";

    private static final String INTERNAL = "internal";

    private static final String GROUP_ID = "groupId";

    private RepositoryStatisticsManager repositoryStatisticsManager;

    private MockControl repositoryStatisticsManagerControl;

    private MockControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private static final String PROBLEM = "problem";


    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        UserRepositoriesStub stub = applicationContext.getBean( "userRepositories", UserRepositoriesStub.class );
        stub.setRepoIds( Lists.<String>newArrayList( "internal", "snapshots" ) );

        action = (GenerateReportAction) getActionProxy( "/report/generateReport.action" ).getAction();

        repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        repositoryStatisticsManager = (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();
        action.setRepositoryStatisticsManager( repositoryStatisticsManager );

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();

        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getRepository() ).thenReturn( metadataRepository );

        TestRepositorySessionFactory factory =
            applicationContext.getBean( "repositorySessionFactory#test", TestRepositorySessionFactory.class );
        factory.setRepositorySession( repositorySession );
    }

    
    @Override
    @After
    public void tearDown()
        throws Exception
    {
        UserRepositoriesStub stub = applicationContext.getBean( "userRepositories", UserRepositoriesStub.class );
        stub.setRepoIds( Lists.<String>newArrayList( "test-repo" ) );

        super.tearDown();

    }

    private void prepareAction( List<String> selectedRepositories, List<String> availableRepositories )
        throws Exception
    {
        MockControl managedRepositoryControl = MockControl.createControl( ManagedRepositoryAdmin.class );
        ManagedRepositoryAdmin managedRepositoryAdmin = (ManagedRepositoryAdmin) managedRepositoryControl.getMock();

        Map<String, ManagedRepository> map = new HashMap<String, ManagedRepository>( availableRepositories.size() );
        for ( String repoId : availableRepositories )
        {
            map.put( repoId, new ManagedRepository() );
        }

        managedRepositoryControl.expectAndReturn( managedRepositoryAdmin.getManagedRepositoriesAsMap(), map, 1, 10 );

        managedRepositoryControl.replay();
        action.setManagedRepositoryAdmin( managedRepositoryAdmin );


        action.setSelectedRepositories( selectedRepositories );
        action.prepare();

        List<String> repos = Arrays.asList( GenerateReportAction.ALL_REPOSITORIES, INTERNAL, SNAPSHOTS );

        Collections.sort( repos );

        Collections.sort( action.getRepositoryIds() );

        assertEquals( repos, action.getRepositoryIds() );
        Collections.sort( action.getAvailableRepositories() );

        availableRepositories = new ArrayList<String>( availableRepositories );
        Collections.sort( availableRepositories );


        assertEquals( availableRepositories, action.getAvailableRepositories() );
    }

    @Test
    public void testGenerateStatisticsInvalidRowCount()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setRowCount( 0 );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsInvalidEndDate()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsMalformedEndDate()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setEndDate( "This is not a date" );
        String result = action.generateStatistics();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsInvalidEndDateMultiRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsMalformedEndDateMultiRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setEndDate( "This is not a date" );
        String result = action.generateStatistics();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsNoRepos()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsSingleRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.singletonList( createDefaultStats() ) );

        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertSuccessResult( result );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsSingleRepoNoStats()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );

        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsOvershotPages()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.replay();
        action.setPage( 2 );
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsMultipleRepoNoResults()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.generateStatistics();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );
        assertFalse( action.hasFieldErrors() );

        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testGenerateStatisticsMultipleRepo()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.singletonList( createDefaultStats() ) );

        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.generateStatistics();
        assertSuccessResult( result );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsSingleRepo()
        throws Exception
    {
        Date date = new Date();
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.singletonList( createStats( date ) ) );
        repositoryStatisticsManagerControl.replay();

        prepareAction( Arrays.asList( SNAPSHOTS ), Arrays.asList( INTERNAL ) );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        assertEquals(
            "Date of Scan,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins,Archetypes,Jars,Wars\n"
                + date + ",0,0,0,0,0,0,0,0,0\n", IOUtils.toString( action.getInputStream() ) );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsMultipleRepos()
        throws Exception
    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        assertMultiRepoCsvResult();
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsMalformedEndDateMultiRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setEndDate( "This is not a date" );
        String result = action.downloadStatisticsReport();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsMalformedEndDateSingleRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS ), Arrays.asList( INTERNAL ) );

        action.setEndDate( "This is not a date" );
        String result = action.downloadStatisticsReport();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsInvalidEndDateMultiRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsInvalidEndDateSingleRepo()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS ), Arrays.asList( INTERNAL ) );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsSingleRepoNoStats()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.downloadStatisticsReport();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsNoRepos()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsMultipleRepoNoResults()
        throws Exception

    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.<Object>emptyList() );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );
        assertFalse( action.hasFieldErrors() );
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testDownloadStatisticsMultipleRepoInStrutsFormat()
        throws Exception
    {
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, SNAPSHOTS, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.expectAndReturn(
            repositoryStatisticsManager.getStatisticsInRange( metadataRepository, INTERNAL, null, null ),
            Collections.singletonList( createDefaultStats() ) );
        repositoryStatisticsManagerControl.replay();
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setSelectedRepositories( Collections.singletonList( "[" + SNAPSHOTS + "],[" + INTERNAL + "]" ) );
        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        assertMultiRepoCsvResult();
        repositoryStatisticsManagerControl.verify();
    }

    @Test
    public void testHealthReportSingleRepo()
        throws Exception
    {
        RepositoryProblemFacet problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblemFacet problem2 = createProblem( GROUP_ID, "artifactId-2", INTERNAL );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( INTERNAL, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem1.getName(), problem2.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem1.getName() ),
            problem1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem2.getName() ),
            problem2 );
        metadataRepositoryControl.replay();

        action.setRepositoryId( INTERNAL );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        assertEquals( Collections.singleton( INTERNAL ), action.getRepositoriesMap().keySet() );
        assertEquals( Arrays.asList( problem1, problem2 ), action.getRepositoriesMap().get( INTERNAL ) );

        metadataRepositoryControl.verify();
    }

    @Test
    public void testHealthReportInvalidRowCount()
        throws Exception
    {
        metadataRepositoryControl.replay();

        action.setRowCount( 0 );
        action.setRepositoryId( INTERNAL );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( Action.INPUT, result );
        assertFalse( action.hasActionErrors() );
        assertTrue( action.hasFieldErrors() );

        metadataRepositoryControl.verify();
    }

    @Test
    public void testHealthReportAllRepos()
        throws Exception
    {
        RepositoryProblemFacet problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblemFacet problem2 = createProblem( GROUP_ID, "artifactId-2", SNAPSHOTS );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( INTERNAL, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem1.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( SNAPSHOTS, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem2.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem1.getName() ),
            problem1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( SNAPSHOTS, RepositoryProblemFacet.FACET_ID, problem2.getName() ),
            problem2 );
        metadataRepositoryControl.replay();

        action.setRepositoryId( GenerateReportAction.ALL_REPOSITORIES );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        assertEquals( Arrays.asList( INTERNAL, SNAPSHOTS ),
                      new ArrayList<String>( action.getRepositoriesMap().keySet() ) );
        assertEquals( Arrays.asList( problem1 ), action.getRepositoriesMap().get( INTERNAL ) );
        assertEquals( Arrays.asList( problem2 ), action.getRepositoriesMap().get( SNAPSHOTS ) );

        metadataRepositoryControl.verify();
    }

    @Test
    public void testHealthReportSingleRepoByCorrectGroupId()
        throws Exception
    {
        RepositoryProblemFacet problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblemFacet problem2 = createProblem( GROUP_ID, "artifactId-2", INTERNAL );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( INTERNAL, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem1.getName(), problem2.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem1.getName() ),
            problem1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem2.getName() ),
            problem2 );
        metadataRepositoryControl.replay();

        action.setGroupId( GROUP_ID );
        action.setRepositoryId( INTERNAL );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        assertEquals( Collections.singleton( INTERNAL ), action.getRepositoriesMap().keySet() );
        assertEquals( Arrays.asList( problem1, problem2 ), action.getRepositoriesMap().get( INTERNAL ) );

        metadataRepositoryControl.verify();
    }

    @Test
    public void testHealthReportSingleRepoByCorrectGroupIdAllRepositories()
        throws Exception
    {
        RepositoryProblemFacet problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblemFacet problem2 = createProblem( GROUP_ID, "artifactId-2", SNAPSHOTS );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( INTERNAL, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem1.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( SNAPSHOTS, RepositoryProblemFacet.FACET_ID ),
            Arrays.asList( problem2.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( INTERNAL, RepositoryProblemFacet.FACET_ID, problem1.getName() ),
            problem1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( SNAPSHOTS, RepositoryProblemFacet.FACET_ID, problem2.getName() ),
            problem2 );
        metadataRepositoryControl.replay();

        action.setGroupId( GROUP_ID );
        action.setRepositoryId( GenerateReportAction.ALL_REPOSITORIES );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        assertEquals( Arrays.asList( INTERNAL, SNAPSHOTS ),
                      new ArrayList<String>( action.getRepositoriesMap().keySet() ) );
        assertEquals( Arrays.asList( problem1 ), action.getRepositoriesMap().get( INTERNAL ) );
        assertEquals( Arrays.asList( problem2 ), action.getRepositoriesMap().get( SNAPSHOTS ) );

        metadataRepositoryControl.verify();
    }

    @Test
    public void testHealthReportSingleRepoByIncorrectGroupId()
        throws Exception
    {
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( INTERNAL, RepositoryProblemFacet.FACET_ID ),
            Collections.<MetadataFacet>emptyList() );
        metadataRepositoryControl.replay();

        action.setGroupId( "not.it" );
        action.setRepositoryId( INTERNAL );

        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        metadataRepositoryControl.verify();
    }

    private void assertMultiRepoCsvResult()
        throws IOException
    {
        assertEquals(
            "Repository,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins,Archetypes,Jars,Wars\n"
                + "snapshots,0,0,0,0,0,0,0,0,0\n" + "internal,0,0,0,0,0,0,0,0,0\n",
            IOUtils.toString( action.getInputStream() ) );
    }

    private RepositoryProblemFacet createProblem( String groupId, String artifactId, String repoId )
    {
        RepositoryProblemFacet problem = new RepositoryProblemFacet();
        problem.setRepositoryId( repoId );
        problem.setNamespace( groupId );
        problem.setProject( artifactId );
        problem.setProblem( PROBLEM );
        return problem;
    }

    @Test
    public void testHealthReportNoRepositoryId()
        throws Exception
    {
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    private void assertSuccessResult( String result )
    {
        assertEquals( Action.SUCCESS, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );
    }

    private RepositoryStatistics createDefaultStats()
    {
        return createStats( new Date() );
    }

    private RepositoryStatistics createStats( Date date )
    {
        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( date );
        return stats;
    }
}
