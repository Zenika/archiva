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
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.repository.group.DefaultRepositoryGroupAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.web.action.AbstractActionTestCase;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
 * DeleteRepositoryGroupActionTest
 */
public class DeleteRepositoryGroupActionTest
    extends AbstractActionTestCase
{
    private static final String REPO_GROUP_ID = "repo-group-ident";

    private DeleteRepositoryGroupAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        action = (DeleteRepositoryGroupAction) getActionProxy( "/admin/deleteRepositoryGroup.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
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

    @Test
    public void testDeleteRepositoryGroupConfirmation()
        throws Exception
    {
        RepositoryGroupConfiguration origRepoGroup = createRepositoryGroup();
        Configuration configuration = createConfigurationForEditing( origRepoGroup );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 2 );
        archivaConfigurationControl.replay();

        action.setRepoGroupId( REPO_GROUP_ID );

        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroup repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );
        assertEquals( repoGroup.getId(), action.getRepoGroupId() );
        assertEquals( Collections.singletonList( origRepoGroup ), configuration.getRepositoryGroups() );
    }

    @Test
    public void testDeleteRepositoryGroup()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepositoryGroup() );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 7 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoGroupId( REPO_GROUP_ID );

        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroup repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );
        assertEquals( Collections.singletonList( repoGroup ),
                      action.getRepositoryGroupAdmin().getRepositoriesGroups() );

        String status = action.delete();
        assertEquals( Action.SUCCESS, status );
        assertTrue( configuration.getRepositoryGroups().isEmpty() );
    }

    @Test
    public void testDeleteRepositoryGroupCancelled()
        throws Exception
    {
        RepositoryGroupConfiguration origRepoGroup = createRepositoryGroup();
        Configuration configuration = createConfigurationForEditing( origRepoGroup );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoGroupId( REPO_GROUP_ID );

        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroup repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
        assertEquals( Collections.singletonList( repoGroup ),
                      action.getRepositoryGroupAdmin().getRepositoriesGroups() );
    }

    private Configuration createConfigurationForEditing( RepositoryGroupConfiguration repoGroup )
    {
        Configuration configuration = new Configuration();
        configuration.addRepositoryGroup( repoGroup );
        return configuration;
    }

    private RepositoryGroupConfiguration createRepositoryGroup()
    {
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( REPO_GROUP_ID );

        return repoGroup;
    }
}
