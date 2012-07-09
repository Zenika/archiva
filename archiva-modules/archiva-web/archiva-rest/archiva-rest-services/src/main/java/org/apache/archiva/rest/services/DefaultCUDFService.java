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
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
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
 * @since 1.4-M3
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{

    private static final String SEPARATOR = "%3a";

    @Inject
    private BrowseService browseService;

    public void getConeCUDF( String groupId, String artifactId, String version, String type, String repositoryId,
                             HttpServletResponse servletResponse )
        throws ArchivaRestServiceException
    {

        Writer output = null;
        try
        {
            output = servletResponse.getWriter();
            computeCUDFCone( groupId, artifactId, version, type, repositoryId, output );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            if ( output != null )
            {
                try
                {
                    output.close();
                }
                catch ( IOException e )
                {
                    //NOTHING
                }
            }
        }
    }

    public Response getConeCUDFFile( String groupId, String artifactId, String version, String type,
                                     String repositoryId, boolean keep )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
            File output = File.createTempFile( fileName, ".txt" );
            if ( !keep )
            {
                output.deleteOnExit();
            }
            FileWriter fos = null;
            try
            {
                fos = new FileWriter( output );
                computeCUDFCone( groupId, artifactId, version, type, repositoryId, fos );
            }
            finally
            {
                if ( fos != null )
                {
                    try
                    {
                        fos.close();
                    }
                    catch ( IOException e )
                    {
                        //NOTHING
                    }
                }
            }
            return Response.ok( output, MediaType.APPLICATION_OCTET_STREAM ).header( "Content-Disposition",
                                                                                     "attachment; filename=" + fileName
                                                                                         + ".txt" ).build();
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    private void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                  Writer output )
        throws ArchivaRestServiceException, IOException
    {
        output.write( getCUDFPreambule() );
        Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();
        List<String> repositories = getSelectedRepos( repositoryId );
        LinkedList<Artifact> queue = new LinkedList<Artifact>();
        Set<String> known = new TreeSet<String>();

        Artifact root = null;
        if ( repositoryId == null )
        {
            for ( String repoId : repositories )
            {
                root = createArtifact( repoId, groupId, artifactId, version, type );
                if ( root != null )
                {
                    break;
                }
            }
        }
        else
        {
            root = createArtifact( repositoryId, groupId, artifactId, version, type );
        }

        queue.add( root );
        Artifact artifact = null;
        while ( ( artifact = queue.poll() ) != null )
        {
            if ( !known.contains( generateArtifactKey( artifact ) ) )
            {
                known.add( generateArtifactKey( artifact ) );
                output.write( outputArtifactInCUDF( artifact, cudfVersionMapper ) );
                output.write( convertDependenciesToCUDF( artifact, repositories, queue, known, cudfVersionMapper ) );
                output.write( "\n" );
            }
        }
    }

    public void getUniverseCUDF( String repositoryId, HttpServletResponse servletResponse )
        throws ArchivaRestServiceException
    {
        Writer output = null;
        try
        {
            output = servletResponse.getWriter();
            computeCUDFUniverse( repositoryId, output );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            if ( output != null )
            {
                try
                {
                    output.close();
                }
                catch ( IOException e )
                {
                    //NOTHING
                }
            }
        }
    }

    public Response getUniverseCUDFFile( String repositoryId, boolean keep )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_universe";
            File output = File.createTempFile( fileName, ".txt" );
            if ( !keep )
            {
                output.deleteOnExit();
            }
            Writer fos = null;
            try
            {
                fos = new FileWriter( output );
                computeCUDFUniverse( repositoryId, fos );
            }
            finally
            {
                if ( fos != null )
                {
                    try
                    {
                        fos.close();
                    }
                    catch ( IOException e )
                    {
                        //NOTHING
                    }
                }
            }
            return Response.ok( output, MediaType.APPLICATION_OCTET_STREAM ).header( "Content-Disposition",
                                                                                     "attachment; filename=" + fileName
                                                                                         + ".txt" ).build();
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    private void computeCUDFUniverse( String repositoryId, Writer output )
        throws ArchivaRestServiceException, IOException
    {
        Map<String, Integer> cudfVersionMapper = new HashMap<String, Integer>();

        output.write( getCUDFPreambule() );

        List<String> repositories = getSelectedRepos( repositoryId );

        LinkedList<Artifact> queue = new LinkedList<Artifact>();
        Set<String> known = new TreeSet<String>();

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
                    Artifact artifact = createArtifact( repository, groupId, artifactId, version, null );
                    known.add( generateArtifactKey( artifact ) );
                    output.write( outputArtifactInCUDF( artifact, cudfVersionMapper ) );
                    output.write(
                        convertDependenciesToCUDF( artifact, repositories, queue, known, cudfVersionMapper ) );
                    output.write( "\n" );
                }
            }
            projects = new ArrayList<String>();
        }
    }

    public String backgroundUniverse( final String repositoryId, String output, HttpServletResponse response )
        throws ArchivaRestServiceException
    {
        final String outputFilename = output == null ? "/home/zenika/universe.cudf" : output;
        new Thread()
        {
            @Override
            public void run()
            {
                File output = new File( outputFilename );
                FileWriter fos = null;
                try
                {
                    fos = new FileWriter( output );
                    log.info( "Starting CUDF Extract in background" );
                    computeCUDFUniverse( repositoryId, fos );
                    log.info(
                        "Background CUDF extraction of Universe is done. Find the output file " + outputFilename );
                }
                catch ( ArchivaRestServiceException e )
                {
                    log.error( "Error while background cudf extraction" );
                    log.error( e.getMessage(), e );
                }
                catch ( IOException e )
                {
                    log.error( "Error while background cudf extraction" );
                    log.error( e.getMessage(), e );
                }
                finally
                {
                    if ( fos != null )
                    {
                        try
                        {
                            fos.close();
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

    @Override
    protected String getSelectedRepoExceptionMessage()
    {
        return "cudf.root.group.repository.denied";
    }

    private Artifact createArtifact( String repository, String groupId, String artifactId, String version, String type )
    {
        Artifact artifact = new Artifact( groupId, artifactId, version );
        if ( type != null )
        {
            artifact.setPackaging( type );
        }
        else
        {
            artifact.setPackaging( resolveArtifactPackaging( repository, artifact ) );
        }
        return artifact;
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
                                              Set<String> known, Map<String, Integer> cudfVersionMapper )
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
                if ( !known.contains( generateArtifactKey( item ) ) )
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
                            "Skipping invalid metadata  while compiling shared model for " + groupId + ":" + artifactId
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

    private String generateArtifactKey( Artifact artifact )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( artifact.getGroupId() ).append( "#" ).append( artifact.getArtifactId() ).append( "#" ).append(
            artifact.getVersion() );
        return sb.toString();
    }
}
