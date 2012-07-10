package org.apache.archiva.web.action.admin.networkproxies;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.web.action.AbstractActionSupport;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.integration.interceptor.SecureAction;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.List;

/**
 * NetworkProxiesAction
 *
 *
 */
@Controller( "networkProxiesAction" )
@Scope( "prototype" )
public class NetworkProxiesAction
    extends AbstractActionSupport
    implements Preparable, SecureAction
{

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    private List<NetworkProxy> networkProxies;

    public void prepare()
        throws Exception
    {
        networkProxies = getNetworkProxyAdmin().getNetworkProxies();
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public List<NetworkProxy> getNetworkProxies()
    {
        return networkProxies;
    }

    public void setNetworkProxies( List<NetworkProxy> networkProxies )
    {
        this.networkProxies = networkProxies;
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
