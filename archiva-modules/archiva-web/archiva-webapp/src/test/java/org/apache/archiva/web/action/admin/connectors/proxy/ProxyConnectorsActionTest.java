package org.apache.archiva.web.action.admin.connectors.proxy;

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
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.proxyconnector.DefaultProxyConnectorAdmin;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.web.action.AbstractWebworkTestCase;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Test;

/**
 * ProxyConnectorsActionTest
 *
 *
 */
public class ProxyConnectorsActionTest
    extends AbstractWebworkTestCase
{
    private static final String JAVAX = "javax";

    private static final String CENTRAL = "central";

    private static final String CORPORATE = "corporate";

    private ProxyConnectorsAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        action = (ProxyConnectorsAction) getActionProxy( "/admin/proxyConnectors.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultProxyConnectorAdmin) action.getProxyConnectorAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    @Test
    public void testSecureActionBundle()
        throws Exception
    {
        expectConfigurationRequests( 5 );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    @Test
    public void testExecute()
        throws Exception
    {
        expectConfigurationRequests( 5 );
        archivaConfigurationControl.replay();

        action.prepare();

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
        assertNoErrors( action );

        assertNotNull( action.getProxyConnectorMap() );
        assertNotNull( action.getRepoMap() );

        assertEquals( 1, action.getProxyConnectorMap().size() );
        assertEquals( 3, action.getRepoMap().size() );
    }

    private void expectConfigurationRequests( int requestConfigCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, requestConfigCount + 1 );

        archivaConfiguration.save( config );
    }

    private Configuration createInitialConfiguration()
    {
        Configuration config = new Configuration();

        ManagedRepositoryConfiguration managedRepo = new ManagedRepositoryConfiguration();
        managedRepo.setId( CORPORATE );
        managedRepo.setLayout( "${java.io.tmpdir}/archiva-test/managed-repo" );
        managedRepo.setReleases( true );

        config.addManagedRepository( managedRepo );

        RemoteRepositoryConfiguration remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( CENTRAL );
        remoteRepo.setUrl( "http://repo1.maven.org/maven2/" );

        config.addRemoteRepository( remoteRepo );

        remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( JAVAX );
        remoteRepo.setUrl( "http://download.java.net/maven/2/" );

        config.addRemoteRepository( remoteRepo );

        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( CENTRAL );

        config.addProxyConnector( connector );

        connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( JAVAX );

        config.addProxyConnector( connector );

        return config;
    }
}
