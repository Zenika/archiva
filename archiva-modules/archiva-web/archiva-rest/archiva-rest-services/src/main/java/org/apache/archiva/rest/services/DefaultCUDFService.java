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
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CUDFService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Adrien Lecharpentier
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{

    @Inject
    private BrowseService browseService;

    @Inject
    private SearchService searchService;

    public String getConeCUDF( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();
        StringBuilder response = new StringBuilder();
        response.append( getCUDFPreambule() );

        List<String> repositories = getSelectedRepos( repositoryId );
        if ( repositories.isEmpty() )
        {
            return "Error";
        }

        TreeSet<Dependency> knownDependencies = new TreeSet<Dependency>( new Comparator<Dependency>()
        {
            public int compare( Dependency o1, Dependency o2 )
            {
                int c = o1.getGroupId().compareTo( o2.getGroupId() );
                if ( c != 0 )
                {
                    return c;
                }
                c = o1.getArtifactId().compareTo( o2.getArtifactId() );
                if ( c != 0 )
                {
                    return c;
                }
                return o1.getVersion().compareTo( o2.getVersion() );
            }
        } );
        LinkedList<Dependency> dependenciesQueue = new LinkedList<Dependency>();

        response.append( convertMavenArtifactToCUDF( groupId, artifactId, version, repositoryId, cudfVersionMapper ) );
        response.append(
            convertDependenciesToCUDF( groupId, artifactId, version, repositories, dependenciesQueue, knownDependencies,
                                       cudfVersionMapper ) ).append( "\n" );

        Dependency dependency = null;
        while ( ( dependency = dependenciesQueue.poll() ) != null )
        {
            if ( !dependency.isOptional() && ( dependency.getScope() == null || "compile".equals(
                dependency.getScope() ) ) && !knownDependencies.contains( dependency ) )
            {
                knownDependencies.add( dependency );
                response.append( convertMavenArtifactToCUDF( dependency.getGroupId(), dependency.getArtifactId(),
                                                             dependency.getVersion(), "", cudfVersionMapper ) );
                response.append( convertDependenciesToCUDF( dependency.getGroupId(), dependency.getArtifactId(),
                                                            dependency.getVersion(), repositories, dependenciesQueue,
                                                            knownDependencies, cudfVersionMapper ) ).append( "\n" );
            }
        }

        return response.toString();
    }

    public Response getConeCUDFFile( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
            File output = File.createTempFile( fileName, ".txt" );
            output.deleteOnExit();
            BufferedWriter bw = new BufferedWriter( new FileWriter( output ) );
            bw.write( getConeCUDF( groupId, artifactId, version, repositoryId ) );
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

    public String getUniverseCUDF( String repositoryId )
        throws ArchivaRestServiceException
    {
        Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();
        StringBuilder sb = new StringBuilder();
        sb.append( getCUDFPreambule() );

        List<String> repositories = getSelectedRepos( repositoryId );
        if ( repositories.isEmpty() )
        {
            return "Error";
        }

        List<String> projects = new ArrayList<String>();

        TreeSet<Dependency> knownDependencies = new TreeSet<Dependency>( new Comparator<Dependency>()
        {
            public int compare( Dependency o1, Dependency o2 )
            {
                int c = o1.getGroupId().compareTo( o2.getGroupId() );
                if ( c != 0 )
                {
                    return c;
                }
                c = o1.getArtifactId().compareTo( o2.getArtifactId() );
                if ( c != 0 )
                {
                    return c;
                }
                return o1.getVersion().compareTo( o2.getVersion() );
            }
        } );
        LinkedList<Dependency> dependenciesQueue = new LinkedList<Dependency>();

        for ( String repository : repositories )
        {
            BrowseResult rootGroupsResult = browseService.getRootGroups( repository );
            for ( BrowseResultEntry rootGroupsResultEntry : rootGroupsResult.getBrowseResultEntries() )
            {
                getRepositoryContent( browseService.browseGroupId( rootGroupsResultEntry.getName(), repository ),
                                      repository, projects );
            }
        }

        for ( String s : projects )
        {
            String groupId = s.substring( 0, s.lastIndexOf( "." ) );
            String artifactId = s.substring( s.lastIndexOf( "." ) + 1 );
            for ( String repository : repositories )
            {
                VersionsList versionsList = browseService.getVersionsList( groupId, artifactId, repository );
                for ( String version : versionsList.getVersions() )
                {
                    sb.append(
                        convertMavenArtifactToCUDF( groupId, artifactId, version, repository, cudfVersionMapper ) );
                    sb.append( convertDependenciesToCUDF( groupId, artifactId, version, repositories, dependenciesQueue,
                                                          knownDependencies, cudfVersionMapper ) );
                    sb.append( "\n" );
                }
            }
        }

        return sb.toString();
    }

    public Response getUniverseCUDFFile( String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_universe";
            File output = File.createTempFile( fileName, ".txt" );
            output.deleteOnExit();
            BufferedWriter bw = new BufferedWriter( new FileWriter( output ) );
            bw.write( getUniverseCUDF( repositoryId ) );
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

    private void getRepositoryContent( BrowseResult browseResult, String repository, List<String> projects )
        throws ArchivaRestServiceException
    {
        for ( BrowseResultEntry rootGroupsResultEntry : browseResult.getBrowseResultEntries() )
        {
            if ( rootGroupsResultEntry.isProject() )
            {
                projects.add( rootGroupsResultEntry.getName() );
            }
            else
            {
                getRepositoryContent( browseService.browseGroupId( rootGroupsResultEntry.getName(), repository ),
                                      repository, projects );
            }
        }
    }

    private String convertMavenArtifactToCUDF( String groupId, String artifactId, String version, String repositoryId,
                                               Map<String, Integer> cudfVersionMapper )
        throws ArchivaRestServiceException
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "package: " ).append( convertMavenArtifactInline( groupId, artifactId ) ).append( "\n" );
            sb.append( "number: " ).append( version ).append( "\n" );
            sb.append( "version: " ).append(
                convertArtifactVersionToCUDFVersion( groupId, artifactId, version, cudfVersionMapper ) ).append( "\n" );
            sb.append( "url: " );
            sb.append( convertURLToCUDFURL( getUrlForArtifact( groupId, artifactId, version, repositoryId ) ) );
            sb.append( "\n" );
            return sb.toString();
        }
        catch ( IllegalStateException e )
        {
            return "";
        }
    }

    private String convertMavenArtifactInline( String groupId, String artifactId )
    {
        return groupId + "%3" + artifactId.replaceAll( "_", "-" );
    }


    private String getUrlForArtifact( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        // FIXME not always a jar
        return searchService.getUrlForArtifact( groupId, artifactId, version, "jar", repositoryId );
    }

    private String convertURLToCUDFURL( String url )
    {
        return url.replaceAll( ":", "%3" );
    }

    private String convertDependenciesToCUDF( String groupId, String artifactId, String version,
                                              List<String> repositories, Queue<Dependency> dependencyQueue,
                                              Set<Dependency> knownDependencies,
                                              Map<String, Integer> cudfVersionMapper )
    {
        StringBuilder sb = new StringBuilder();

        List<Dependency> dependencies = getDependencies( groupId, artifactId, version, repositories );
        if ( dependencies.isEmpty() )
        {
            return "";
        }

        Iterator<Dependency> it = dependencies.iterator();
        List<Dependency> deps = new ArrayList<Dependency>();
        while ( it.hasNext() )
        {
            Dependency item = it.next();
            if ( !item.isOptional() && ( item.getScope() == null || "compile".equals( item.getScope() ) )
                && !knownDependencies.contains( item ) )
            {
                deps.add( item );
            }
        }
        if ( !deps.isEmpty() )
        {
            sb.append( "depends: " );
            it = deps.iterator();
            while ( it.hasNext() )
            {
                Dependency item = it.next();
                dependencyQueue.add( item );
                sb.append( convertMavenArtifactInline( item.getGroupId(), item.getArtifactId() ) ).append(
                    " = " ).append(
                    convertArtifactVersionToCUDFVersion( item.getGroupId(), item.getArtifactId(), item.getVersion(),
                                                         cudfVersionMapper ) );
                if ( it.hasNext() )
                {
                    sb.append( ", " );
                }
            }

            sb.append( "\n" );
        }
        return sb.toString();
    }

    private int convertArtifactVersionToCUDFVersion( String groupId, String artifactId, String version,
                                                     Map<String, Integer> cudfVersionMapper )
        throws IllegalStateException
    {
        String storeVersionKey = groupId + ":" + artifactId + ":" + version;
        if ( cudfVersionMapper.containsKey( storeVersionKey ) )
        {
            return cudfVersionMapper.get( storeVersionKey );
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
        cudfVersionMapper.put( storeVersionKey, cudfVersion );
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

    private String getCUDFPreambule()
    {
        return "preamble: \nproperty: number: string, recommends: vpkgformula = [true!], suggests: vpkglist = [], \n"
            + "          url: string = [\"\"]\n\n";
    }

    private List<String> getSelectedRepos( String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getObservableRepos();

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return Collections.<String>emptyList();
        }

        if ( StringUtils.isNotEmpty( repositoryId ) )
        {
            // check user has karma on the repository
            if ( !selectedRepos.contains( repositoryId ) )
            {
                throw new ArchivaRestServiceException( "browse.root.groups.repositoy.denied",
                                                       Response.Status.FORBIDDEN.getStatusCode() );
            }
            selectedRepos = Collections.singletonList( repositoryId );
        }
        return selectedRepos;
    }
}
