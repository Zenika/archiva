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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.dependency.tree.maven2.DependencyTreeBuilder;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CUDFService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Adrien Lecharpentier
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{

    private Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();

    public String getConeCUDF( String groupId, String artifactId, String version )
        throws ArchivaRestServiceException
    {
        StringBuilder response = new StringBuilder();

        List<String> repositories = getObservableRepos();
        if ( repositories.isEmpty() )
        {
            return "Error";
        }

        response.append( convertMavenArtifactToCUDF( groupId, artifactId, version ) );
        response.append( convertDependenciesToCUDF( groupId, artifactId, version, repositories ) ).append( "\n" );

        for ( Dependency dependency : getDependencies( groupId, artifactId, version, repositories ) )
        {
            if ( !dependency.isOptional() && ( dependency.getScope() == null || "compile".equals(
                dependency.getScope() ) ) )
            {
                response.append( convertMavenArtifactToCUDF( dependency.getGroupId(), dependency.getArtifactId(),
                                                             dependency.getVersion() ) );
                response.append( convertDependenciesToCUDF( dependency.getGroupId(), dependency.getArtifactId(),
                                                            dependency.getVersion(), repositories ) ).append( "\n" );
            }
        }

        return response.toString();
    }

    public Response getConeCUDFFile( String groupId, String artifactId, String version )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
            File output = File.createTempFile( fileName, ".txt" );
            output.deleteOnExit();
            BufferedWriter bw = new BufferedWriter( new FileWriter( output ) );
            bw.write( getConeCUDF( groupId, artifactId, version ) );
            bw.close();
            return Response.ok( output, MediaType.APPLICATION_OCTET_STREAM ).header( "Content-Disposition",
                                                                                     "attachment; filename=" + fileName
                                                                                         + ".txt" ).build();
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    public CharSequence getUniverseCUDF()
        throws ArchivaRestServiceException
    {
        // todo
        return null;
    }

    private String convertMavenArtifactToCUDF( String groupId, String artifactId, String version )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "package: " ).append( convertMavenArtifactInline( groupId, artifactId ) ).append( "\n" );
            sb.append( "source: " ).append( version ).append( "\n" );
            sb.append( "version: " ).append(
                convertArtifactVersionToCUDFVersion( groupId, artifactId, version ) ).append( "\n" );
            return sb.toString();
        }
        catch ( IllegalStateException e )
        {
            return "";
        }
    }

    private String convertMavenArtifactInline( String groupId, String artifactId )
    {
        return groupId + "\\:" + artifactId;
    }

    private String convertDependenciesToCUDF( String groupId, String artifactId, String version,
                                              List<String> repositories )
    {
        StringBuilder sb = new StringBuilder();

        List<Dependency> dependencies = getDependencies( groupId, artifactId, version, repositories );
        if ( dependencies.isEmpty() )
        {
            return "";
        }

        sb.append( "depends: " );
        Iterator<Dependency> it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency item = it.next();
            if ( !item.isOptional() && ( item.getScope() == null || "compile".equals( item.getScope() ) ) )
            {
                sb.append( convertMavenArtifactInline( item.getGroupId(), item.getArtifactId() ) ).append(
                    " = " ).append(
                    convertArtifactVersionToCUDFVersion( item.getGroupId(), item.getArtifactId(), item.getVersion() ) );
                if ( it.hasNext() )
                {
                    sb.append( ", " );
                }
                else
                {
                    sb.delete( sb.lastIndexOf( "," ), sb.lastIndexOf( "," ) + 1 );
                }
            }
            else
            {
                if ( !it.hasNext() )
                {
                    sb.delete( sb.lastIndexOf( "," ), sb.lastIndexOf( "," ) + 2 );
                }
            }
        }

        sb.append( "\n" );
        return sb.toString();
    }

    private int convertArtifactVersionToCUDFVersion( String groupId, String artifactId, String version )
        throws IllegalStateException
    {
        if ( cudfVersionMapper.containsKey( groupId + "\\:" + artifactId + "\\:" + version ) )
        {
            return cudfVersionMapper.get( groupId + "\\:" + artifactId + "\\:" + version );
        }
        List<String> versionList;
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> versions = new LinkedHashSet<String>();

            for ( String repoId : getObservableRepos() )
            {
                versions.addAll(
                    metadataResolver.resolveProjectVersions( repositorySession, repoId, groupId, artifactId ) );
            }
            versionList = new ArrayList<String>( versions );
            Collections.sort( versionList, VersionComparator.getInstance() );
        }
        catch ( MetadataResolutionException e )
        {
            return 0;
        }
        finally
        {
            repositorySession.close();
        }
        int cudfVersion = versionList.indexOf( version ) + 1;
        cudfVersionMapper.put( groupId + "\\:" + artifactId + "\\:" + version, cudfVersion );
        return cudfVersion;
    }

    private List<Dependency> getDependencies( String groupId, String artifactId, String version,
                                              List<String> repositories )
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();

            MetadataResolver metadataResolver = repositorySession.getResolver();

            ProjectVersionMetadata versionMetadata = null;
            for ( String repoId : repositories )
            {
                if ( versionMetadata == null || versionMetadata.isIncomplete() )
                {
                    try
                    {
                        versionMetadata =
                            metadataResolver.resolveProjectVersion( repositorySession, repoId, groupId, artifactId,
                                                                    version );
                    }
                    catch ( MetadataResolutionException e )
                    {
                        log.error(
                            "Skipping invalid metadata while compiling shared model for " + groupId + ":" + artifactId
                                + " in repo " + repoId + ": " + e.getMessage() );
                    }
                }
            }

            return versionMetadata != null ? versionMetadata.getDependencies() : Collections.<Dependency>emptyList();
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
