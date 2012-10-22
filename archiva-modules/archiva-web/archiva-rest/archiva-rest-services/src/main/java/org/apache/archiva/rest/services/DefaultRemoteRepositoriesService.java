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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service ("remoteRepositoriesService#rest")
public class DefaultRemoteRepositoriesService
    extends AbstractRestService
    implements RemoteRepositoriesService
{

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    public List<RemoteRepository> getRemoteRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            List<RemoteRepository> remoteRepositories = remoteRepositoryAdmin.getRemoteRepositories();
            return remoteRepositories == null ? Collections.<RemoteRepository>emptyList() : remoteRepositories;
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }

    public RemoteRepository getRemoteRepository( String repositoryId )
        throws ArchivaRestServiceException
    {

        List<RemoteRepository> remoteRepositories = getRemoteRepositories();
        for ( RemoteRepository repository : remoteRepositories )
        {
            if ( StringUtils.equals( repositoryId, repository.getId() ) )
            {
                return repository;
            }
        }
        return null;
    }

    public Boolean deleteRemoteRepository( String repositoryId )
        throws Exception
    {
        try
        {
            return remoteRepositoryAdmin.deleteRemoteRepository( repositoryId, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }

    public Boolean addRemoteRepository( RemoteRepository remoteRepository )
        throws Exception
    {
        try
        {
            return remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }

    public Boolean updateRemoteRepository( RemoteRepository remoteRepository )
        throws Exception
    {
        try
        {
            return remoteRepositoryAdmin.updateRemoteRepository( remoteRepository, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }


}
