package org.apache.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork2.Action;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.struts2.StrutsSpringTestCase;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import java.util.Collections;

import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * EditRemoteRepositoryActionTest
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class EditRemoteRepositoryActionTest
    extends StrutsSpringTestCase
{
    private static final String REPO_ID = "remote-repo-ident";

    private EditRemoteRepositoryAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        action = (EditRemoteRepositoryAction) getActionProxy( "/admin/editRemoteRepository.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    @Test
    public void testEditRemoteRepository()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4, 6 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.prepare();

        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepository repository = action.getRepository();
        populateRepository( repository );
        repository.setName( "new repo name" );

        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        RemoteRepository newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        assertEquals( Collections.singletonList( repository ),
                      action.getRemoteRepositoryAdmin().getRemoteRepositories() );

        archivaConfigurationControl.verify();
    }

    @Test
    public void testEditRemoteRepositoryInitialPage()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 2 );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepository repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.input();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
    }

    @Test
    public void testSecureActionBundle()
        throws SecureActionException, RepositoryAdminException
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    private void assertRepositoryEquals( RemoteRepository expectedRepository, RemoteRepository actualRepository )
    {
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getUrl(), actualRepository.getUrl() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
    }

    private Configuration createConfigurationForEditing( RemoteRepository repositoryConfiguration )
    {
        Configuration configuration = new Configuration();
        RemoteRepositoryConfiguration conf = new RemoteRepositoryConfiguration();
        conf.setId( repositoryConfiguration.getId() );
        conf.setLayout( repositoryConfiguration.getLayout() );
        conf.setUrl( repositoryConfiguration.getUrl() );
        conf.setName( repositoryConfiguration.getName() );
        configuration.addRemoteRepository( conf );
        return configuration;
    }

    private RemoteRepository createRepository()
    {
        RemoteRepository r = new RemoteRepository();
        r.setId( REPO_ID );
        populateRepository( r );
        return r;
    }

    private void populateRepository( RemoteRepository repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setUrl( "url" );
        repository.setLayout( "default" );
    }
}
