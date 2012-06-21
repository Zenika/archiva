package org.apache.archiva.rest.services;

/*
 * Copyright 2012 Zenika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CUDFService;
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
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 *         <p/>
 *         FIXME comparator for known artifact break compareTo contract!
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{

    private static final String SEPARATOR = "%3a";

    @Inject
    private BrowseService browseService;

    public String getConeCUDF( String groupId, String artifactId, String version, String type, String repositoryId )
        throws ArchivaRestServiceException
    {
        Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();
        StringBuilder response = new StringBuilder();
        response.append( getCUDFPreambule() );

        List<String> repositories = getSelectedRepos( repositoryId );

        LinkedList<Artifact> queue = new LinkedList<Artifact>();
        Set<Artifact> known = new TreeSet<Artifact>( new ArtifactComparator() );

        // todo: what if the repository is null?? NPE!
        if ( repositoryId == null )
        {
            throw new ArchivaRestServiceException( "Not working if there is no repository", null );
        }

        queue.add( createArtifact( repositoryId, groupId, artifactId, version ) );
        Artifact artifact = null;
        while ( ( artifact = queue.poll() ) != null )
        {
            if ( !known.contains( artifact ) )
            {
                known.add( artifact );
                response.append( outputArtifactInCUDF( artifact, cudfVersionMapper ) );
                response.append( convertDependenciesToCUDF( artifact, repositories, queue, known, cudfVersionMapper ) );
                response.append( "\n" );
            }
        }

        return response.toString();
    }

    @Override
    protected String getSelectedRepoExceptionMessage()
    {
        return "cudf.root.group.repository.denied";
    }

    public Response getConeCUDFFile( String groupId, String artifactId, String version, String type,
                                     String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
            File output = File.createTempFile( fileName, ".txt" );
            output.deleteOnExit();
            BufferedWriter bw = new BufferedWriter( new FileWriter( output ) );
            bw.write( getConeCUDF( groupId, artifactId, version, type, repositoryId ) );
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
        StringBuilder response = new StringBuilder();
        response.append( getCUDFPreambule() );

        List<String> repositories = getSelectedRepos( repositoryId );

        LinkedList<Artifact> queue = new LinkedList<Artifact>();
        Set<Artifact> known = new TreeSet<Artifact>( new ArtifactComparator() );

        List<String> projects = new ArrayList<String>();
        for ( String repository : repositories )
        {
            BrowseResult rootGroupsResult = browseService.getRootGroups( repository );
            for ( BrowseResultEntry rootGroupsResultEntry : rootGroupsResult.getBrowseResultEntries() )
            {
                getRepositoryContent( browseService.browseGroupId( rootGroupsResultEntry.getName(), repository ),
                                      repository, projects );
            }
            for ( String project : projects )
            {
                String groupId = project.substring( 0, project.lastIndexOf( "." ) );
                String artifactId = project.substring( project.lastIndexOf( "." ) + 1 );
                VersionsList versionsList = browseService.getVersionsList( groupId, artifactId, repository );
                for ( String version : versionsList.getVersions() )
                {
                    Artifact artifact = createArtifact( repository, groupId, artifactId, version );
                    known.add( artifact );
                    response.append( outputArtifactInCUDF( artifact, cudfVersionMapper ) );
                    response.append(
                        convertDependenciesToCUDF( artifact, repositories, queue, known, cudfVersionMapper ) );
                    response.append( "\n" );
                }
            }
            projects = new ArrayList<String>();
        }
        return response.toString();
    }

    private Artifact createArtifact( String repository, String groupId, String artifactId, String version )
    {
        Artifact artifact = new Artifact( groupId, artifactId, version );
        artifact.setPackaging( resolveArtifactPackaging( repository, artifact ) );
        return artifact;
    }

    private String resolveArtifactPackaging( String repository, Artifact artifact )
    {
        String packaging = null;
        RepositorySession repositorySession = null;
        try
        {
            try
            {
                repositorySession = repositorySessionFactory.createSession();
                MetadataResolver metadataResolver = repositorySession.getResolver();
                ProjectVersionMetadata versionMetadata =
                    metadataResolver.resolveProjectVersion( repositorySession, repository, artifact.getGroupId(),
                                                            artifact.getArtifactId(), artifact.getVersion() );
                if ( versionMetadata == null )
                {
                    return "";
                }
                MavenProjectFacet projectFacet =
                    (MavenProjectFacet) versionMetadata.getFacet( MavenProjectFacet.FACET_ID );
                if ( projectFacet != null )
                {
                    packaging = projectFacet.getPackaging();
                }
            }
            catch ( MetadataResolutionException e )
            {
                log.error( "Skipping invalid metadata while compiling shared model for " + artifact.getGroupId() + ":"
                               + artifact.getArtifactId() + " in repo " + repository + ": " + e.getMessage() );
            }
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
        return packaging;
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

    public String backgroundUniverse( final String repositoryId, String output )
        throws ArchivaRestServiceException
    {
        final String outputFilename = output == null ? "/home/zenika/universe.cudf" : output;
        new Thread()
        {
            @Override
            public void run()
            {
                File output = new File( outputFilename );
                BufferedWriter bw = null;
                try
                {
                    bw = new BufferedWriter( new FileWriter( output ) );
                    log.info( "Starting CUDF Extract in background" );
                    bw.write( getUniverseCUDF( repositoryId ) );
                    bw.close();
                    log.info(
                        "Background CUDF extraction of Universe is done. Find the output file " + outputFilename );
                }
                catch ( ArchivaRestServiceException e )
                {
                    log.error( e.getMessage(), e );
                }
                catch (IOException e)
                {
                    log.error( e.getMessage(), e );
                }
                finally
                {
                    if ( bw != null )
                    {
                        try
                        {
                            bw.close();
                        }
                        catch ( IOException e )
                        {
                            //nothing to do
                        }
                    }
                    log.info( "CUDF Extraction ended." );
                }
            }
        }.start();
        return "Started in background";
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

    private String outputArtifactInCUDF( Artifact artifact, Map<String, Integer> cudfVersionMapper )
        throws ArchivaRestServiceException
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "package: " ).append(
                outputArtifactInCUDFInline( artifact.getGroupId(), artifact.getArtifactId() ) ).append( "\n" );
            sb.append( "number: " ).append( artifact.getVersion() ).append( "\n" );
            sb.append( "version: " ).append(
                convertArtifactVersionToCUDFVersion( artifact, cudfVersionMapper ) ).append( "\n" );
            sb.append( "type: " ).append( StringUtils.defaultString( artifact.getPackaging() ) ).append( "\n" );
            return sb.toString();
        }
        catch ( IllegalStateException e )
        {
            return "";
        }
    }

    private String outputArtifactInCUDFInline( String groupId, String artifactId )
    {
        return groupId + SEPARATOR + artifactId.replaceAll( "_", "-" );
    }

    private String convertDependenciesToCUDF( Artifact artifact, List<String> repositories, Queue<Artifact> queue,
                                              Set<Artifact> known, Map<String, Integer> cudfVersionMapper )
        throws ArchivaRestServiceException
    {
        List<Dependency> dependencies =
            getDependencies( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), repositories );
        if ( dependencies.isEmpty() )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        List<Artifact> artifacts = new ArrayList<Artifact>();

        {
            Iterator<Dependency> it = dependencies.iterator();
            while ( it.hasNext() )
            {
                Dependency item = it.next();
                if ( !item.isOptional() && ( item.getScope() == null || "compile".equals( item.getScope() ) ) )
                {
                    Artifact art = new Artifact( item.getGroupId(), item.getArtifactId(), item.getVersion() );
                    art.setPackaging( item.getType() );
                    artifacts.add( art );
                }
            }
        }

        if ( !artifacts.isEmpty() )
        {
            Iterator<Artifact> it = artifacts.iterator();
            sb.append( "depends: " );
            while ( it.hasNext() )
            {
                Artifact item = it.next();
                if ( !known.contains( item ) )
                {
                    queue.add( item );
                }
                sb.append( outputArtifactInCUDFInline( item.getGroupId(), item.getArtifactId() ) ).append( " = " ).
                    append( convertArtifactVersionToCUDFVersion( item, cudfVersionMapper ) );
                if ( it.hasNext() )
                {
                    sb.append( ", " );
                }
            }

            sb.append( "\n" );
        }
        return sb.toString();
    }

    private int convertArtifactVersionToCUDFVersion( Artifact artifact, Map<String, Integer> cudfVersionMapper )
        throws IllegalStateException
    {
        String storeVersionKey = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
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
                    metadataResolver.resolveProjectVersions( repositorySession, repoId, artifact.getGroupId(),
                                                             artifact.getArtifactId() ) );
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
        int cudfVersion = versionList.indexOf( artifact.getVersion() ) + 1;
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
            + "          type: string = [\"\"]\n\n";
    }

    // FIXME its violate Collections obligation on compareTo / equals methods
    static class ArtifactComparator
        implements Comparator<Artifact>
    {
        public int compare( Artifact o1, Artifact o2 )
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
            c = o1.getVersion().compareTo( o2.getVersion() );
            if ( c != 0 )
            {
                return c;
            }
            if ( o1.getPackaging() == null && o2.getPackaging() == null )
            {
                return 0;
            }
            if ( StringUtils.isEmpty( o1.getPackaging() ) )
            {
                return -1;
            }
            if ( StringUtils.isEmpty( o2.getPackaging() ) )
            {
                return 1;
            }
            return o1.getPackaging().compareTo( o2.getPackaging() );
        }
    }
}
