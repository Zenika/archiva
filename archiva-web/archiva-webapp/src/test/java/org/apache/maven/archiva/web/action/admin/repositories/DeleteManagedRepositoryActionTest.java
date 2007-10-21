package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork.Action;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import org.apache.maven.archiva.model.ArchivaProjectModel;

import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;

import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;
import org.easymock.MockControl;

import java.io.File;
import java.util.Collections;

/**
 * DeleteManagedRepositoryActionTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DeleteManagedRepositoryActionTest
    extends PlexusTestCase
{
    private DeleteManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;
    
    private static final String REPO_ID = "repo-ident";

    private File location;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (DeleteManagedRepositoryAction) lookup( Action.class.getName(), "deleteManagedRepositoryAction" );
        
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );
        location = getTestFile( "target/test/location" );          
    }

    public void testSecureActionBundle()
        throws SecureActionException
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testDeleteRepositoryConfirmation()
        throws Exception
    {
        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );
    }

    public void testDeleteRepositoryKeepContent()
        throws Exception
    {    	
        prepareRoleManagerMock();
        
        Configuration configuration = prepDeletionTest( createRepository(), 3 );                
        
        String status = action.deleteEntry();        
                
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertTrue( location.exists() );
    }

    public void testDeleteRepositoryDeleteContent()
        throws Exception
    {
        prepareRoleManagerMock();
        
        Configuration configuration = prepDeletionTest( createRepository(), 3 );              
        
        String status = action.deleteContents();
               
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertFalse( location.exists() );
    }
    
    public void testDeleteRepositoryAndAssociatedProxyConnectors()
        throws Exception
    {
        Configuration configuration = prepDeletionTest( createRepository(), 4 );
        configuration.addRemoteRepository( createRemoteRepository( "codehaus", "http://repository.codehaus.org" ) );
        configuration.addRemoteRepository( createRemoteRepository( "java.net", "http://dev.java.net/maven2" ) );
        configuration.addProxyConnector( createProxyConnector( REPO_ID, "codehaus" ) );

        prepareRoleManagerMock();

        assertEquals( 1, configuration.getProxyConnectors().size() );
        
        String status = action.deleteContents();
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );
        assertEquals( 0, configuration.getProxyConnectors().size() );

        assertFalse( location.exists() );
    }
    
    public void testDeleteRepositoryCancelled()
        throws Exception
    {
        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = prepDeletionTest( originalRepository, 3 );
        String status = action.execute();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );

        assertTrue( location.exists() );
    }

    private Configuration prepDeletionTest( ManagedRepositoryConfiguration originalRepository, int expectCountGetConfig )
        throws RegistryException, IndeterminateConfigurationException
    {
        location.mkdirs();

        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, expectCountGetConfig );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        assertTrue( location.exists() );
        return configuration;
    }

    private void assertRepositoryEquals( ManagedRepositoryConfiguration expectedRepository,
                                         ManagedRepositoryConfiguration actualRepository )
    {
        assertEquals( expectedRepository.getDaysOlder(), actualRepository.getDaysOlder() );
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getIndexDir(), actualRepository.getIndexDir() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getLocation(), actualRepository.getLocation() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
        assertEquals( expectedRepository.getRefreshCronExpression(), actualRepository.getRefreshCronExpression() );
        assertEquals( expectedRepository.getRetentionCount(), actualRepository.getRetentionCount() );
        assertEquals( expectedRepository.isDeleteReleasedSnapshots(), actualRepository.isDeleteReleasedSnapshots() );
        assertEquals( expectedRepository.isScanned(), actualRepository.isScanned() );
        assertEquals( expectedRepository.isReleases(), actualRepository.isReleases() );
        assertEquals( expectedRepository.isSnapshots(), actualRepository.isSnapshots() );
    }

    private Configuration createConfigurationForEditing( ManagedRepositoryConfiguration repositoryConfiguration )
    {
        Configuration configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfiguration );
        return configuration;
    }

    private ManagedRepositoryConfiguration createRepository()
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID );
        r.setName( "repo name" );
        r.setLocation( location.getAbsolutePath() );
        r.setLayout( "default" );
        r.setRefreshCronExpression( "* 0/5 * * * ?" );
        r.setDaysOlder( 0 );
        r.setRetentionCount( 0 );
        r.setReleases( true );
        r.setSnapshots( true );
        r.setScanned( false );
        r.setDeleteReleasedSnapshots( false );
        return r;
    }

    private RemoteRepositoryConfiguration createRemoteRepository(String id, String url)
    {
        RemoteRepositoryConfiguration r = new RemoteRepositoryConfiguration();
        r.setId( id );
        r.setUrl( url );
        r.setLayout( "default" );
        
        return r;
    }
    
    private ProxyConnectorConfiguration createProxyConnector( String managedRepoId, String remoteRepoId )
    {
        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( managedRepoId );
        connector.setTargetRepoId( remoteRepoId );

        return connector;
    }

    private void prepareRoleManagerMock()
        throws RoleManagerException
    {
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( true );
        roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( true );
        roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.replay();
    }
    
    protected ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel projectModel = new ArchivaProjectModel();
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );

        return projectModel;
    }   
}
