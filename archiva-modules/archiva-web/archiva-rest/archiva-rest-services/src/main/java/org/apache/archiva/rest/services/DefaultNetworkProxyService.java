package org.apache.archiva.rest.services;
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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.rest.api.model.NetworkProxy;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "networkProxyService#rest" )
public class DefaultNetworkProxyService
    extends AbstractRestService
    implements NetworkProxyService
{
    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    public List<NetworkProxy> getNetworkProxies()
        throws RepositoryAdminException
    {
        List<NetworkProxy> networkProxies = new ArrayList<NetworkProxy>();
        for ( org.apache.archiva.admin.repository.networkproxy.NetworkProxy networkProxy : networkProxyAdmin.getNetworkProxies() )
        {
            networkProxies.add( new BeanReplicator().replicateBean( networkProxy, NetworkProxy.class ) );
        }
        return networkProxies;
    }

    public NetworkProxy getNetworkProxy( String networkProxyId )
        throws RepositoryAdminException
    {
        org.apache.archiva.admin.repository.networkproxy.NetworkProxy networkProxy =
            networkProxyAdmin.getNetworkProxy( networkProxyId );
        return networkProxy == null ? null : new BeanReplicator().replicateBean( networkProxy, NetworkProxy.class );
    }

    public void addNetworkProxy( NetworkProxy networkProxy )
        throws RepositoryAdminException
    {
        if ( networkProxy == null )
        {
            return;
        }
        getNetworkProxyAdmin().addNetworkProxy( new BeanReplicator().replicateBean( networkProxy,
                                                                                    org.apache.archiva.admin.repository.networkproxy.NetworkProxy.class ),
                                                getAuditInformation() );
    }

    public void updateNetworkProxy( NetworkProxy networkProxy )
        throws RepositoryAdminException
    {
        if ( networkProxy == null )
        {
            return;
        }
        getNetworkProxyAdmin().updateNetworkProxy( new BeanReplicator().replicateBean( networkProxy,
                                                                                       org.apache.archiva.admin.repository.networkproxy.NetworkProxy.class ),
                                                   getAuditInformation() );
    }

    public Boolean deleteNetworkProxy( String networkProxyId )
        throws RepositoryAdminException
    {
        getNetworkProxyAdmin().deleteNetworkProxy( networkProxyId, getAuditInformation() );
        return Boolean.TRUE;
    }

    public NetworkProxyAdmin getNetworkProxyAdmin()
    {
        return networkProxyAdmin;
    }

    public void setNetworkProxyAdmin( NetworkProxyAdmin networkProxyAdmin )
    {
        this.networkProxyAdmin = networkProxyAdmin;
    }
}
