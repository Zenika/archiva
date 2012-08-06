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
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * DeleteRemoteRepositoryAction
 *
 *
 */
@Controller( "deleteRemoteRepositoryAction" )
@Scope( "prototype" )
public class DeleteRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable
{
    private RemoteRepository repository;

    private String repoid;

    public void prepare()
        throws RepositoryAdminException
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = getRemoteRepositoryAdmin().getRemoteRepository( repoid );
        }
    }

    public String confirmDelete()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Unable to delete remote repository: repository id was blank." );
            return ERROR;
        }

        return INPUT;
    }

    public String delete()
    {
        String result = SUCCESS;
        RemoteRepository existingRepository = repository;
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        try
        {
            getRemoteRepositoryAdmin().deleteRemoteRepository( existingRepository.getId(), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "RepositoryAdminException: " + e.getMessage() );
            result = ERROR;
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

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
}
