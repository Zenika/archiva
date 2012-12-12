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

import com.zenika.cudf.adapter.ArchivaBinaryAdapter;
import com.zenika.cudf.adapter.ArchivaDescriptorAdapter;
import com.zenika.cudf.adapter.cache.CachedBinaries;
import com.zenika.cudf.model.CUDFDescriptor;
import com.zenika.cudf.model.Preamble;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.cudf.cache.CUDFCache;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFUniverseLoader
{

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private CUDFCache cache;

    private List<ProjectVersionMetadata> projectVersionMetadatas = new ArrayList<ProjectVersionMetadata>();

    private CUDFDescriptor descriptor;

    private Logger log = LoggerFactory.getLogger( CUDFUniverseLoader.class );

    public void loadUniverse( List<String> repositoryIds )
    {
        if ( !isLoaded() )
        {
            log.info( "Load CUDF universe" );
            loadArchivaUniverse( repositoryIds );
        }
        ArchivaDescriptorAdapter archivaAdapter =
            new ArchivaDescriptorAdapter( new ArchivaVersionResolver( repositorySessionFactory, repositoryIds ),
                                          new ArchivaBinaryAdapter() );
        archivaAdapter.setCache( cache );
        descriptor = archivaAdapter.toCUDF( projectVersionMetadatas );
        descriptor.setPreamble( Preamble.getDefaultPreamble() );
    }

    private void loadArchivaUniverse( List<String> repositoryIds )
    {
        for ( String repositoryId : repositoryIds )
        {
            RepositorySession repositorySession = null;
            try
            {
                repositorySession = repositorySessionFactory.createSession();
                MetadataResolver metadataResolver = repositorySession.getResolver();
                Collection<String> rootNamespaces =
                    metadataResolver.resolveRootNamespaces( repositorySession, repositoryId );
                for ( String rootNamespace : rootNamespaces )
                {
                    resolveNamespaces( repositorySession, metadataResolver, repositoryId, rootNamespace );
                }
            }
            catch ( MetadataResolutionException e )
            {
                throw new RuntimeException( "Unable to load CUDF universe", e );
            }
            finally
            {
                if ( repositorySession != null )

                {
                    repositorySession.close();
                }
            }
        }
    }


    private void resolveNamespaces( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                    String repositoryId, String namespace )
        throws MetadataResolutionException
    {
        Collection<String> namespaces =
            metadataResolver.resolveNamespaces( repositorySession, repositoryId, namespace );
        if ( namespaces.isEmpty() )
        {
            resolveProjects( repositorySession, metadataResolver, repositoryId, namespace );
        }
        else
        {
            for ( String currentNamespace : namespaces )
            {
                resolveNamespaces( repositorySession, metadataResolver, repositoryId,
                                   namespace + "." + currentNamespace );
            }
        }
    }

    private void resolveProjects( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                  String repositoryId, String namespace )
        throws MetadataResolutionException
    {
        Collection<String> projects = metadataResolver.resolveProjects( repositorySession, repositoryId, namespace );
        if ( !projects.isEmpty() )
        {
            for ( String project : projects )
            {
                resolveProjectVersion( repositorySession, metadataResolver, repositoryId, namespace, project );
            }
        }
    }

    private void resolveProjectVersion( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                        String repositoryId, String namespace, String project )
        throws MetadataResolutionException
    {
        List<String> projectVersions = new LinkedList<String>(
            metadataResolver.resolveProjectVersions( repositorySession, repositoryId, namespace, project ) );
        Collections.sort( projectVersions, VersionComparator.getInstance() );
        if ( !projectVersions.isEmpty() )
        {
            for ( String projectVersion : projectVersions )
            {
                resolveProjectVersionMetadata( repositorySession, metadataResolver, repositoryId, namespace, project,
                                               projectVersion );
            }
        }
    }

    private void resolveProjectVersionMetadata( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                                String repositoryId, String namespace, String project,
                                                String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            ProjectVersionMetadata projectVersionMetadata =
                metadataResolver.resolveProjectVersion( repositorySession, repositoryId, namespace, project,
                                                        projectVersion );
            if ( projectVersionMetadata != null && !projectVersionMetadata.isIncomplete()
                && !projectVersionMetadata.getFacetIds().isEmpty() )
            {
                projectVersionMetadatas.add( projectVersionMetadata );
            }
        }
        catch ( MetadataResolutionException e )
        {
            log.warn( e.getMessage() );
        }
    }

    public CUDFDescriptor getDescriptor()
    {
        return descriptor;
    }

    public boolean isLoaded()
    {
        return cache.get( CachedBinaries.BINARY_ID_KEY_LIST ) != null && !( (Set) cache.get(
            CachedBinaries.BINARY_ID_KEY_LIST ) ).isEmpty();
    }
}
