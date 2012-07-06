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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * AddRemoteRepositoryAction
 *
 *
 */
@Controller( "addRemoteRepositoryAction" )
@Scope( "prototype" )
public class AddRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * The model for this action.
     */
    private RemoteRepository repository;

    public void prepare()
        throws RepositoryAdminException
    {
        this.repository = new RemoteRepository();
        setNetworkProxies( getNetworkProxyAdmin().getNetworkProxies() );
    }

    public String input()
    {
        return INPUT;
    }

    public String commit()
    {

        String result = SUCCESS;
        try
        {
            getRemoteRepositoryAdmin().addRemoteRepository( repository, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "RepositoryAdminException: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }


    public RemoteRepository getRepository()
    {
        return repository;
    }

    public void setRepository( RemoteRepository repository )
    {
        this.repository = repository;
    }
}
