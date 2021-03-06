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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
public class CUDFExtractor
{
    private final Map<String, String> illegals = new HashMap<String, String>();

    private Logger log = LoggerFactory.getLogger( CUDFExtractor.class );

    private List<String> repositories;

    private Writer writer;

    /**
     * Should be used for versions conversion table
     */
    private Writer optionalWriter;

    public CUDFExtractor( Writer writer, Writer optionalWriter )
    {
        this.writer = writer;
        this.optionalWriter = optionalWriter;
        initiateIllegalsCharatersForCUDF();
    }

    private void initiateIllegalsCharatersForCUDF()
    {
        illegals.put( "_", Integer.toHexString( '_' ) );
        illegals.put( ":", Integer.toHexString( ':' ) );
    }

    public void computeCUDFUniverse( List<String> repositoryIds, RepositorySessionFactory repositorySessionFactory )
        throws IOException
    {
        this.repositories = repositoryIds;

        this.writer.write( getCUDFPreambule() );
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
                repositorySession.close();
            }
            catch ( MetadataResolutionException e )
            {
                throw new RuntimeException( "Unable to extract CUDF Cone", e );
            }
            finally
            {
                if ( repositorySession != null )
                {
                    repositorySession.close();

                }
                this.writer.flush();
                this.writer.close();
            }
        }
    }

    public void computeCUDFCone( String groupId, String artifactId, String version, String type,
                                 List<String> repositories, RepositorySessionFactory repositorySessionFactory )
        throws IOException, MetadataResolutionException
    {
        this.repositories = repositories;
        this.writer.append( getCUDFPreambule() );
        for ( String repositoryId : repositories )
        {
            computeCUDFCone( groupId, artifactId, version, type, repositoryId, repositorySessionFactory );
        }
        this.writer.close();
    }

    public void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                 List<String> repositories, RepositorySessionFactory repositorySessionFactory )
        throws IOException, MetadataResolutionException
    {
        this.repositories = repositories;
        this.writer.append( getCUDFPreambule() );
        computeCUDFCone( groupId, artifactId, version, type, repositoryId, repositorySessionFactory );
        this.writer.close();
    }

    private void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                  RepositorySessionFactory repositorySessionFactory )
        throws IOException, MetadataResolutionException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
            MetadataResolver metadataResolver = repositorySession.getResolver();
            resolveProjectVersionMetadata( repositorySession, metadataResolver, repositoryId, groupId, artifactId,
                                           version,
                                           getCUDFVersion( repositorySession, metadataResolver, groupId, artifactId,
                                                           version ) );
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataResolutionException( "Unable to extract CUDF Cone", e );
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
        this.writer.flush();
    }

    private void resolveNamespaces( RepositorySession session, MetadataResolver metadataResolver, String repositoryId,
                                    String namespace )
        throws MetadataResolutionException, IOException
    {
        Collection<String> namespaces = metadataResolver.resolveNamespaces( session, repositoryId, namespace );
        if ( namespaces.size() == 0 )
        {
            resolveProjects( session, metadataResolver, repositoryId, namespace );
        }
        else
        {
            for ( String currentNamespace : namespaces )
            {
                resolveNamespaces( session, metadataResolver, repositoryId, namespace + "." + currentNamespace );
            }
        }
    }

    private void resolveProjects( RepositorySession session, MetadataResolver metadataResolver, String repositoryId,
                                  String namespace )
        throws MetadataResolutionException, IOException
    {
        Collection<String> projects = metadataResolver.resolveProjects( session, repositoryId, namespace );
        if ( projects.size() != 0 )
        {
            for ( String project : projects )
            {
                resolveProjectVersions( session, metadataResolver, repositoryId, namespace, project );
            }
        }
    }

    private void resolveProjectVersions( RepositorySession session, MetadataResolver metadataResolver,
                                         String repositoryId, String namespace, String project )
        throws IOException, MetadataResolutionException
    {
        List<String> projectVersions = new LinkedList<String>(
            metadataResolver.resolveProjectVersions( session, repositoryId, namespace, project ) );
        Collections.sort( projectVersions, VersionComparator.getInstance() );
        if ( projectVersions.size() != 0 )
        {
            for ( int i = 0; i < projectVersions.size(); i++ )
            {
                resolveProjectVersionMetadata( session, metadataResolver, repositoryId, namespace, project,
                                               projectVersions.get( i ), i + 1 );
            }
        }
    }

    private void resolveProjectVersionMetadata( RepositorySession session, MetadataResolver metadataResolver,
                                                String repositoryId, String namespace, String project,
                                                String projectVersion, int version )
        throws MetadataResolutionException, IOException
    {
        ProjectVersionMetadata projectVersionMetadata =
            metadataResolver.resolveProjectVersion( session, repositoryId, namespace, project, projectVersion );
        if ( projectVersionMetadata != null )
        {
            String projectOneLine = outputArtifactInCUDFInline( extractOrganisation( projectVersionMetadata ),
                                                                extractName( projectVersionMetadata ) );
            writer.append( "package: " ).append( projectOneLine ).append( '\n' );
            writer.append( "number: " ).append( projectVersionMetadata.getVersion() ).append( '\n' );
            writer.append( "version: " ).append( Integer.toString( version ) ).append( '\n' );
            writer.append( "type: " ).append( extractPackaging( projectVersionMetadata ) ).append( '\n' );
            if ( hasDependencies( projectVersionMetadata ) )
            {
                writer.append( "depends: " ).append(
                    extractDependencies( session, projectVersionMetadata.getDependencies(), metadataResolver ) ).append(
                    '\n' );
            }
            writer.append( '\n' );
            writer.flush();
        } else {
            throw new MetadataResolutionException( "Invalid Metadata, check the artifact metadata" );
        }
    }

    private boolean hasDependencies( ProjectVersionMetadata projectVersionMetadata )
    {
        return projectVersionMetadata.getDependencies().size() != 0;
    }

    private StringBuilder extractDependencies( RepositorySession repositorySession, List<Dependency> dependencies,
                                               MetadataResolver metadataResolver )
        throws MetadataResolutionException
    {
        StringBuilder builder = new StringBuilder( 50 );
        Iterator<Dependency> iterator = dependencies.iterator();
        while ( iterator.hasNext() )
        {
            Dependency dependency = iterator.next();
            builder.append( outputArtifactInCUDFInline( dependency.getGroupId(), dependency.getArtifactId() ) );
            builder.append( " = " );
            builder.append( getCUDFVersion( repositorySession, metadataResolver, dependency ) );
            if ( iterator.hasNext() )
            {
                builder.append( ", " );
            }
        }
        return builder;
    }

    private int getCUDFVersion( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                Dependency dependency )
        throws MetadataResolutionException
    {
        return getCUDFVersion( repositorySession, metadataResolver, dependency.getGroupId(), dependency.getArtifactId(),
                               dependency.getVersion() );
    }

    private int getCUDFVersion( RepositorySession repositorySession, MetadataResolver metadataResolver, String groupId,
                                String artifactId, String version )
        throws MetadataResolutionException
    {
        List<String> projectVersions = new LinkedList<String>();
        for ( String repositoryId : repositories )
        {
            projectVersions.addAll(
                metadataResolver.resolveProjectVersions( repositorySession, repositoryId, groupId, artifactId ) );
        }
        Collections.sort( projectVersions, VersionComparator.getInstance() );
        int cudfVersion = projectVersions.indexOf( version ) + 1;
        if ( optionalWriter != null )
        {
            try
            {
                writer.append( outputArtifactInCUDFInline( groupId, artifactId ) ).append( " " ).append(
                    version ).append( " => " ).append( Integer.toString( cudfVersion ) );
            }
            catch ( IOException e )
            {
                // nothing
            }
        }
        return cudfVersion;
    }

    private String extractPackaging( ProjectVersionMetadata projectVersionMetadata )
    {
        MavenProjectFacet facet = (MavenProjectFacet) projectVersionMetadata.getFacet( MavenProjectFacet.FACET_ID );
        if ( facet != null )
        {
            return facet.getPackaging() == null ? "" : facet.getPackaging();
        }
        else
        {
            return "";
        }
    }

    // FIXME: what if it is not MavenProjectFacet but NugetsProjectFacet or DebianProjectFacet?
    private String extractOrganisation( ProjectVersionMetadata projectVersionMetadata )
    {
        MavenProjectFacet facet = (MavenProjectFacet) projectVersionMetadata.getFacet( MavenProjectFacet.FACET_ID );
        if ( facet != null )
        {
            return facet.getGroupId();
        }
        else
        {
            return null;
        }
    }

    // FIXME: what if it is not MavenProjectFacet but NugetsProjectFacet or DebianProjectFacet?
    private String extractName( ProjectVersionMetadata projectVersionMetadata )
    {
        MavenProjectFacet facet = (MavenProjectFacet) projectVersionMetadata.getFacet( MavenProjectFacet.FACET_ID );
        if ( facet != null )
        {
            return facet.getArtifactId();
        }
        else
        {
            return null;
        }
    }

    private String getCUDFPreambule()
    {
        return "preamble: \nproperty: number: string, recommends: vpkgformula = [true!], suggests: vpkglist = [], \n"
            + " type: string = [\"\"]\n\n";
    }

    /**
     * @param organisation (groupId)
     * @param name         (artifactId)
     * @return organisation%3aname => org.apache.archiva%3aarchiva with %+hexadecimal code of every forbidden characters
     * @throws MetadataResolutionException if the organisation or the name is null
     */
    private String outputArtifactInCUDFInline( String organisation, String name )
        throws MetadataResolutionException
    {
        if ( organisation == null || name == null )
        {
            throw new MetadataResolutionException("Invalid Metadata, check the artifact metadata");
        }
        String packageLine = new StringBuilder( 20 ).append( organisation ).append( ':' ).append( name ).toString();
        return encodingString( packageLine );
    }

    /**
     * Returns the input string with all illegal characters replace by their equivalent in hexa
     *
     * @param input
     * @return the input with hexa in place of the illegal characters
     */
    String encodingString( String input )
    {
        for ( String illegal : illegals.keySet() )
        {
            input = input.replaceAll( illegal, '%' + illegals.get( illegal ) );
        }
        return input;
    }
}
