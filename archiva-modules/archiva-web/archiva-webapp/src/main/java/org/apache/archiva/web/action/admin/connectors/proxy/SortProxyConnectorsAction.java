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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * SortProxyConnectorsAction -
 *
 *
 */
@Controller( "sortProxyConnectorsAction" )
@Scope( "prototype" )
public class SortProxyConnectorsAction
    extends AbstractProxyConnectorAction
{
    private String source;

    private String target;

    public String getSource()
    {
        return source;
    }

    public String getTarget()
    {
        return target;
    }

    public void setSource( String id )
    {
        this.source = id;
    }

    public void setTarget( String id )
    {
        this.target = id;
    }

    public String sortDown()
        throws RepositoryAdminException
    {
        List<ProxyConnector> connectors = createProxyConnectorMap().get( source );

        int idx = findTargetConnector( connectors, target );

        if ( idx >= 0 )
        {
            incrementConnectorOrder( connectors, idx );
            decrementConnectorOrder( connectors, idx + 1 );
        }

        for ( ProxyConnector proxyConnector : connectors )
        {
            getProxyConnectorAdmin().updateProxyConnector( proxyConnector, getAuditInformation() );
        }
        return SUCCESS;
    }

    public String sortUp()
        throws RepositoryAdminException
    {
        List<ProxyConnector> connectors = createProxyConnectorMap().get( source );

        int idx = findTargetConnector( connectors, target );

        if ( idx >= 0 )
        {
            decrementConnectorOrder( connectors, idx );
            incrementConnectorOrder( connectors, idx - 1 );
        }

        for ( ProxyConnector proxyConnector : connectors )
        {
            getProxyConnectorAdmin().updateProxyConnector( proxyConnector, getAuditInformation() );
        }
        return SUCCESS;
    }

    private void decrementConnectorOrder( List<ProxyConnector> connectors, int idx )
    {
        if ( !validIndex( connectors, idx ) )
        {
            // Do nothing.
            return;
        }

        int order = connectors.get( idx ).getOrder();
        connectors.get( idx ).setOrder( Math.max( 1, order - 1 ) );
    }

    private int findTargetConnector( List<ProxyConnector> connectors, String targetRepoId )
    {
        int idx = ( -1 );

        for ( int i = 0; i < connectors.size(); i++ )
        {
            if ( StringUtils.equals( targetRepoId, connectors.get( i ).getTargetRepoId() ) )
            {
                idx = i;
                break;
            }
        }

        return idx;
    }

    private void incrementConnectorOrder( List<ProxyConnector> connectors, int idx )
    {
        if ( !validIndex( connectors, idx ) )
        {
            // Do nothing.
            return;
        }

        int order = connectors.get( idx ).getOrder();
        connectors.get( idx ).setOrder( order + 1 );
    }

    private boolean validIndex( List<ProxyConnector> connectors, int idx )
    {
        return ( idx >= 0 ) && ( idx < connectors.size() );
    }
}
