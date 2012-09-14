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

import com.zenika.cudf.model.Binaries;
import com.zenika.cudf.model.Binary;
import com.zenika.cudf.model.Request;
import com.zenika.cudf.resolver.VersionResolver;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFBinaryCheck
{
    
    @Inject    
    private CUDFUniverse universe;
    
    @Inject
    private RepositorySessionFactory repositorySessionFactory;
    
    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    public void checkRequestedPackagesAreInUniverse( Request request )
    {
        checkPackagesAreInUniverse( request.getInstall() );
        checkPackagesAreInUniverse( request.getUpdate() );
        checkPackagesAreInUniverse( request.getRemove() );
    }

    public void checkPackagesAreInUniverse( Set<Binary> binaries )
    {
        Binaries universeBinaries = universe.getDescriptor().getBinaries();
        VersionResolver versionResolver = new ArchivaVersionResolver( repositorySessionFactory, getAllRepositoryIds());
        for ( Binary binary : binaries )
        {
            if ( universeBinaries.getBinaryById( versionResolver.resolveToCUDF( binary ).getBinaryId() ) == null )
            {
                throw new IllegalStateException( "The binary requested must be in the universe: " + binary ); //TODO: find a way to retrieve the missing binary in other repository
            }
        }
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
