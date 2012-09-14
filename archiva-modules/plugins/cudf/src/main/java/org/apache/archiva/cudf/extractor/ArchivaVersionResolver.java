package org.apache.archiva.cudf.extractor;

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

import com.zenika.cudf.model.Binary;
import com.zenika.cudf.model.BinaryId;
import com.zenika.cudf.resolver.VersionResolver;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.metadata.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
public class ArchivaVersionResolver
    implements VersionResolver
{

    private ManagedRepositoryAdmin managedRepositoryAdmin;
    
    private final Logger log = LoggerFactory.getLogger( ArchivaVersionResolver.class );

    private final RepositorySessionFactory repositorySessionFactory;

    private final List<String> repositoryIds;

    public ArchivaVersionResolver( RepositorySessionFactory repositorySessionFactory, List<String> repositoryIds )
    {
        this.repositorySessionFactory = repositorySessionFactory;
        this.repositoryIds = repositoryIds;
    }

    public ArchivaVersionResolver( RepositorySessionFactory repositorySessionFactory, ManagedRepositoryAdmin managedRepositoryAdmin ) {
        this.repositorySessionFactory = repositorySessionFactory;
        this.managedRepositoryAdmin = managedRepositoryAdmin;
        this.repositoryIds = getAllRepositoryIds();
    }
    
    public Binary resolveToCUDF( Binary binary )
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
            MetadataResolver metadataResolver = repositorySession.getResolver();
            List<String> projectVersions = new ArrayList<String>();
            for ( String repositoryId : repositoryIds )
            {
                projectVersions.addAll( metadataResolver.resolveProjectVersions( repositorySession, repositoryId,
                                                                                 binary.getBinaryId().getOrganisation(),
                                                                                 binary.getBinaryId().getName() ) );
            }
            Collections.sort( projectVersions, VersionComparator.getInstance() );
            int version = projectVersions.indexOf( binary.getRevision() ) + 1;
            BinaryId binaryId =
                new BinaryId( binary.getBinaryId().getName(), binary.getBinaryId().getOrganisation(), version );
            Binary result = new Binary( binaryId );
            result.setRevision( binary.getRevision() );
            result.setType( binary.getType() );
            result.setInstalled( binary.isInstalled() );
            result.setDependencies( binary.getDependencies() );
            return result;
        }
        catch ( MetadataResolutionException e )
        {
            log.warn( "Unable to resolve version for this binary: " + binary );
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
        return binary;
    }

    public Binary resolveFromCUDF( Binary binary )
    {
        
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    private List<String> getAllRepositoryIds()
    {
        List<String> repositoryIds = new ArrayList<String>();
        try
        {
            List<ManagedRepository> managedRepositories = managedRepositoryAdmin.getManagedRepositories();
            for ( ManagedRepository managedRepository : managedRepositories )
            {
                repositoryIds.add( managedRepository.getId() );
            }
        }
        catch ( RepositoryAdminException e )
        {
            throw new RuntimeException(
                "Unable to retrieve all repositories" ); //TODO: Find best way for exception management
        }
        return repositoryIds;
    }
}
