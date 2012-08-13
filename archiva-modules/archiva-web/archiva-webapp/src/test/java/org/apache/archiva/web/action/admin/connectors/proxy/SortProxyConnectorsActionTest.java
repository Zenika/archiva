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
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.functors.ProxyConnectorConfigurationOrderComparator;
import org.apache.archiva.web.action.AbstractWebworkTestCase;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.easymock.MockControl;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * SortProxyConnectorsActionTest
 *
 *
 */
public class SortProxyConnectorsActionTest
    extends AbstractWebworkTestCase
{
    private static final String JAVAX = "javax";

    private static final String CENTRAL = "central";

    private static final String CORPORATE = "corporate";

    private static final String CODEHAUS = "codehaus";

    private SortProxyConnectorsAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        action = (SortProxyConnectorsAction) getActionProxy( "/admin/sortUpProxyConnector.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultProxyConnectorAdmin) action.getProxyConnectorAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    private void expectConfigurationRequests( int requestConfigCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, requestConfigCount );

        archivaConfiguration.save( config );
    }

    @Test
    public void testSecureActionBundle()
        throws Exception
    {
        expectConfigurationRequests( 1 );
        archivaConfigurationControl.replay();

        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    @Test
    public void testSortDown()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        action.setSource( CORPORATE );
        action.setTarget( CENTRAL );
        String status = action.sortDown();
        assertEquals( Action.SUCCESS, status );

        assertOrder( new String[]{ JAVAX, CENTRAL, CODEHAUS } );
    }

    @Test
    public void testSortDownPastEnd()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Ask the last connector to sort down (essentially a no-op)
        action.setSource( CORPORATE );
        action.setTarget( CODEHAUS );
        String status = action.sortDown();
        assertEquals( Action.SUCCESS, status );

        // No order change.
        assertOrder( new String[]{ CENTRAL, JAVAX, CODEHAUS } );
    }

    @Test
    public void testSortUp()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        action.setSource( CORPORATE );
        action.setTarget( CODEHAUS );
        String status = action.sortUp();
        assertEquals( Action.SUCCESS, status );

        assertOrder( new String[]{ CENTRAL, CODEHAUS, JAVAX } );
    }

    @Test
    public void testSortUpPastBeginning()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Ask the first connector to sort up (essentially a no-op)
        action.setSource( CORPORATE );
        action.setTarget( CENTRAL );
        String status = action.sortUp();
        assertEquals( Action.SUCCESS, status );

        // No order change.
        assertOrder( new String[]{ CENTRAL, JAVAX, CODEHAUS } );
    }

    private void assertOrder( String[] targetRepoOrder )
    {
        List<ProxyConnectorConfiguration> connectors = archivaConfiguration.getConfiguration().getProxyConnectors();
        Collections.sort( connectors, ProxyConnectorConfigurationOrderComparator.getInstance() );

        for ( ProxyConnectorConfiguration connector : connectors )
        {
            assertEquals( "All connectors in list should have the same source id (in this test)", CORPORATE,
                          connector.getSourceRepoId() );
        }

        assertEquals( targetRepoOrder.length, connectors.size() );

        int orderFailedAt = ( -1 );
        for ( int i = 0; i < targetRepoOrder.length; i++ )
        {
            if ( !StringUtils.equals( targetRepoOrder[i], connectors.get( i ).getTargetRepoId() ) )
            {
                orderFailedAt = i;
                break;
            }
        }

        if ( orderFailedAt >= 0 )
        {
            StringBuilder msg = new StringBuilder();

            msg.append( "Failed expected order of the proxy connectors <" );
            msg.append( StringUtils.join( targetRepoOrder, ", " ) );
            msg.append( ">, actual <" );

            boolean needsComma = false;
            for ( ProxyConnectorConfiguration proxy : connectors )
            {
                if ( needsComma )
                {
                    msg.append( ", " );
                }
                msg.append( proxy.getTargetRepoId() );
                needsComma = true;
            }
            msg.append( "> failure at index <" ).append( orderFailedAt ).append( ">." );

            fail( msg.toString() );
        }
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

        remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( CODEHAUS );
        remoteRepo.setUrl( "http://repository.codehaus.org/" );
        config.addRemoteRepository( remoteRepo );

        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( CENTRAL );
        connector.setOrder( 1 );
        config.addProxyConnector( connector );

        connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( JAVAX );
        connector.setOrder( 2 );
        config.addProxyConnector( connector );

        connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( CODEHAUS );
        connector.setOrder( 3 );
        config.addProxyConnector( connector );

        return config;
    }
}
