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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.dependency.tree.maven2.DependencyTreeBuilder;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.ArtifactContentEntry;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.Entry;
import org.apache.archiva.rest.api.model.TreeEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.services.utils.ArtifactContentEntryComparator;
import org.apache.archiva.rest.services.utils.TreeDependencyNodeVisitor;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service( "browseService#rest" )
public class DefaultBrowseService
    extends AbstractRestService
    implements BrowseService
{

    @Inject
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Inject
    private RepositoryContentFactory repositoryContentFactory;

    public BrowseResult getRootGroups( String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        Set<String> namespaces = new LinkedHashSet<String>();

        // TODO: this logic should be optional, particularly remembering we want to keep this code simple
        //       it is located here to avoid the content repository implementation needing to do too much for what
        //       is essentially presentation code
        Set<String> namespacesToCollapse;
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();
            namespacesToCollapse = new LinkedHashSet<String>();

            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveRootNamespaces( repositorySession, repoId ) );
            }
            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add( collapseNamespaces( repositorySession, metadataResolver, selectedRepos, n ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            repositorySession.close();
        }

        List<BrowseResultEntry> browseGroupResultEntries = new ArrayList<BrowseResultEntry>( namespaces.size() );
        for ( String namespace : namespaces )
        {
            browseGroupResultEntries.add( new BrowseResultEntry( namespace, false ) );
        }

        Collections.sort( browseGroupResultEntries );
        return new BrowseResult( browseGroupResultEntries );
    }

    public BrowseResult browseGroupId( String groupId, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        Set<String> projects = new LinkedHashSet<String>();

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        Set<String> namespaces;
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> namespacesToCollapse = new LinkedHashSet<String>();
            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, groupId ) );

                projects.addAll( metadataResolver.resolveProjects( repositorySession, repoId, groupId ) );
            }

            // TODO: this logic should be optional, particularly remembering we want to keep this code simple
            // it is located here to avoid the content repository implementation needing to do too much for what
            // is essentially presentation code
            namespaces = new LinkedHashSet<String>();
            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add(
                    collapseNamespaces( repositorySession, metadataResolver, selectedRepos, groupId + "." + n ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            repositorySession.close();
        }
        List<BrowseResultEntry> browseGroupResultEntries =
            new ArrayList<BrowseResultEntry>( namespaces.size() + projects.size() );
        for ( String namespace : namespaces )
        {
            browseGroupResultEntries.add( new BrowseResultEntry( namespace, false ) );
        }
        for ( String project : projects )
        {
            browseGroupResultEntries.add( new BrowseResultEntry( groupId + '.' + project, true ) );
        }
        Collections.sort( browseGroupResultEntries );
        return new BrowseResult( browseGroupResultEntries );

    }

    public VersionsList getVersionsList( String groupId, String artifactId, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        try
        {
            return new VersionsList( new ArrayList<String>( getVersions( selectedRepos, groupId, artifactId ) ) );
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }

    }

    private Collection<String> getVersions( List<String> selectedRepos, String groupId, String artifactId )
        throws MetadataResolutionException

    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> versions = new LinkedHashSet<String>();

            for ( String repoId : selectedRepos )
            {
                versions.addAll(
                    metadataResolver.resolveProjectVersions( repositorySession, repoId, groupId, artifactId ) );
            }

            List<String> sortedVersions = new ArrayList<String>( versions );

            Collections.sort( sortedVersions, VersionComparator.getInstance() );

            return sortedVersions;
        }
        finally
        {
            repositorySession.close();
        }
    }

    public ProjectVersionMetadata getProjectMetadata( String groupId, String artifactId, String version,
                                                      String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();

            MetadataResolver metadataResolver = repositorySession.getResolver();

            ProjectVersionMetadata versionMetadata = null;
            for ( String repoId : selectedRepos )
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

            return versionMetadata;
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }

    }

    public ProjectVersionMetadata getProjectVersionMetadata( String groupId, String artifactId, String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getSelectedRepos( repositoryId );

        RepositorySession repositorySession = null;
        try
        {

            Collection<String> projectVersions = getVersions( selectedRepos, groupId, artifactId );

            repositorySession = repositorySessionFactory.createSession();

            MetadataResolver metadataResolver = repositorySession.getResolver();

            ProjectVersionMetadata sharedModel = new ProjectVersionMetadata();

            MavenProjectFacet mavenFacet = new MavenProjectFacet();
            mavenFacet.setGroupId( groupId );
            mavenFacet.setArtifactId( artifactId );
            sharedModel.addFacet( mavenFacet );

            boolean isFirstVersion = true;

            for ( String version : projectVersions )
            {
                ProjectVersionMetadata versionMetadata = null;
                for ( String repoId : selectedRepos )
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
                            log.error( "Skipping invalid metadata while compiling shared model for " + groupId + ":"
                                           + artifactId + " in repo " + repoId + ": " + e.getMessage() );
                        }
                    }
                }

                if ( versionMetadata == null )
                {
                    continue;
                }

                if ( isFirstVersion )
                {
                    sharedModel = versionMetadata;
                    sharedModel.setId( null );
                }
                else
                {
                    MavenProjectFacet versionMetadataMavenFacet =
                        (MavenProjectFacet) versionMetadata.getFacet( MavenProjectFacet.FACET_ID );
                    if ( versionMetadataMavenFacet != null )
                    {
                        if ( mavenFacet.getPackaging() != null && !StringUtils.equalsIgnoreCase(
                            mavenFacet.getPackaging(), versionMetadataMavenFacet.getPackaging() ) )
                        {
                            mavenFacet.setPackaging( null );
                        }
                    }

                    if ( StringUtils.isEmpty( sharedModel.getName() ) && !StringUtils.isEmpty(
                        versionMetadata.getName() ) )
                    {
                        sharedModel.setName( versionMetadata.getName() );
                    }

                    if ( sharedModel.getDescription() != null && !StringUtils.equalsIgnoreCase(
                        sharedModel.getDescription(), versionMetadata.getDescription() ) )
                    {
                        sharedModel.setDescription( StringUtils.isNotEmpty( versionMetadata.getDescription() )
                                                        ? versionMetadata.getDescription()
                                                        : "" );
                    }

                    if ( sharedModel.getIssueManagement() != null && versionMetadata.getIssueManagement() != null
                        && !StringUtils.equalsIgnoreCase( sharedModel.getIssueManagement().getUrl(),
                                                          versionMetadata.getIssueManagement().getUrl() ) )
                    {
                        sharedModel.setIssueManagement( versionMetadata.getIssueManagement() );
                    }

                    if ( sharedModel.getCiManagement() != null && versionMetadata.getCiManagement() != null
                        && !StringUtils.equalsIgnoreCase( sharedModel.getCiManagement().getUrl(),
                                                          versionMetadata.getCiManagement().getUrl() ) )
                    {
                        sharedModel.setCiManagement( versionMetadata.getCiManagement() );
                    }

                    if ( sharedModel.getOrganization() != null && versionMetadata.getOrganization() != null
                        && !StringUtils.equalsIgnoreCase( sharedModel.getOrganization().getName(),
                                                          versionMetadata.getOrganization().getName() ) )
                    {
                        sharedModel.setOrganization( versionMetadata.getOrganization() );
                    }

                    if ( sharedModel.getUrl() != null && !StringUtils.equalsIgnoreCase( sharedModel.getUrl(),
                                                                                        versionMetadata.getUrl() ) )
                    {
                        sharedModel.setUrl( versionMetadata.getUrl() );
                    }
                }

                isFirstVersion = false;
            }
            return sharedModel;
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
    }

    public List<TreeEntry> getTreeEntries( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        List<TreeEntry> treeEntries = new ArrayList<TreeEntry>();
        TreeDependencyNodeVisitor treeDependencyNodeVisitor = new TreeDependencyNodeVisitor( treeEntries );
        try
        {
            dependencyTreeBuilder.buildDependencyTree( selectedRepos, groupId, artifactId, version,
                                                       treeDependencyNodeVisitor );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        return treeEntries;
    }

    public List<ManagedRepository> getUserRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            return userRepositories.getAccessibleRepositories( getPrincipal() );
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( "repositories.read.observable.error",
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
    }

    public List<Artifact> getDependees( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        // TODO: what if we get duplicates across repositories?
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();
            for ( String repoId : getObservableRepos() )
            {
                // TODO: what about if we want to see this irrespective of version?
                references.addAll(
                    metadataResolver.resolveProjectReferences( repositorySession, repoId, groupId, artifactId,
                                                               version ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            repositorySession.close();
        }

        List<Artifact> artifacts = new ArrayList<Artifact>( references.size() );

        for ( ProjectVersionReference projectVersionReference : references )
        {
            artifacts.add( new Artifact( projectVersionReference.getNamespace(), projectVersionReference.getProjectId(),
                                         projectVersionReference.getProjectVersion() ) );
        }
        return artifacts;
    }

    public List<Entry> getMetadatas( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );
        if ( projectVersionMetadata == null )
        {
            return Collections.emptyList();
        }
        MetadataFacet metadataFacet = projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet == null )
        {
            return Collections.emptyList();
        }
        Map<String, String> map = metadataFacet.toProperties();
        List<Entry> entries = new ArrayList<Entry>( map.size() );

        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            entries.add( new Entry( entry.getKey(), entry.getValue() ) );
        }

        return entries;
    }

    public Boolean addMetadata( String groupId, String artifactId, String version, String key, String value,
                                String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );

        if ( projectVersionMetadata == null )
        {
            return Boolean.FALSE;
        }

        Map<String, String> properties = new HashMap<String, String>();

        MetadataFacet metadataFacet = projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet != null && metadataFacet.toProperties() != null )
        {
            properties.putAll( metadataFacet.toProperties() );
        }
        else
        {
            metadataFacet = new GenericMetadataFacet();
        }

        properties.put( key, value );

        metadataFacet.fromProperties( properties );

        projectVersionMetadata.addFacet( metadataFacet );

        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.updateProjectVersion( repositoryId, groupId, artifactId, projectVersionMetadata );

            repositorySession.save();
        }
        catch ( MetadataRepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    public Boolean deleteMetadata( String groupId, String artifactId, String version, String key, String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );

        if ( projectVersionMetadata == null )
        {
            return Boolean.FALSE;
        }

        GenericMetadataFacet metadataFacet =
            (GenericMetadataFacet) projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet != null && metadataFacet.toProperties() != null )
        {
            Map<String, String> properties = metadataFacet.toProperties();
            properties.remove( key );
            metadataFacet.setAdditionalProperties( properties );
        }
        else
        {
            return Boolean.TRUE;
        }

        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.updateProjectVersion( repositoryId, groupId, artifactId, projectVersionMetadata );

            repositorySession.save();
        }
        catch ( MetadataRepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    public List<ArtifactContentEntry> getArtifactContentEntries( String groupId, String artifactId, String version,
                                                                 String classifier, String type, String path,
                                                                 String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );
        try
        {
            for ( String repoId : selectedRepos )
            {

                ManagedRepositoryContent managedRepositoryContent =
                    repositoryContentFactory.getManagedRepositoryContent( repoId );
                ArchivaArtifact archivaArtifact = new ArchivaArtifact( groupId, artifactId, version, classifier,
                                                                       StringUtils.isEmpty( type ) ? "jar" : type,
                                                                       repositoryId );
                File file = managedRepositoryContent.toFile( archivaArtifact );
                if ( file.exists() )
                {
                    return readFileEntries( file, path );
                }
            }
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        catch ( RepositoryNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        return Collections.emptyList();
    }

    //---------------------------
    // internals
    //---------------------------

    protected List<ArtifactContentEntry> readFileEntries( File file, String filterPath )
        throws IOException
    {
        Map<String, ArtifactContentEntry> artifactContentEntryMap = new HashMap<String, ArtifactContentEntry>();
        int filterDepth = StringUtils.countMatches( filterPath, "/" );
        if ( filterDepth == 0 )
        {
            filterDepth = 1;
        }
        JarFile jarFile = new JarFile( file );
        try
        {
            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
            while ( jarEntryEnumeration.hasMoreElements() )
            {
                JarEntry currentEntry = jarEntryEnumeration.nextElement();
                String cleanedEntryName =
                    StringUtils.endsWith( currentEntry.getName(), "/" ) ? StringUtils.substringBeforeLast(
                        currentEntry.getName(), "/" ) : currentEntry.getName();
                String entryRootPath = getRootPath( cleanedEntryName );
                int depth = StringUtils.countMatches( cleanedEntryName, "/" );
                if ( StringUtils.isEmpty( filterPath ) && !artifactContentEntryMap.containsKey( entryRootPath ) )
                {

                    artifactContentEntryMap.put( entryRootPath,
                                                 new ArtifactContentEntry( entryRootPath, !currentEntry.isDirectory(),
                                                                           depth ) );
                }
                else
                {
                    if ( StringUtils.startsWith( cleanedEntryName, filterPath ) && ( depth >= filterDepth || (
                        !currentEntry.isDirectory() && depth == filterDepth ) ) )
                    {
                        artifactContentEntryMap.put( cleanedEntryName, new ArtifactContentEntry( cleanedEntryName,
                                                                                                 !currentEntry.isDirectory(),
                                                                                                 depth ) );
                    }
                }
            }

            if ( StringUtils.isNotEmpty( filterPath ) )
            {
                Map<String, ArtifactContentEntry> filteredArtifactContentEntryMap =
                    new HashMap<String, ArtifactContentEntry>();

                for ( Map.Entry<String, ArtifactContentEntry> entry : artifactContentEntryMap.entrySet() )
                {
                    filteredArtifactContentEntryMap.put( entry.getKey(), entry.getValue() );
                }

                List<ArtifactContentEntry> sorted = getSmallerDepthEntries( filteredArtifactContentEntryMap );
                if ( sorted == null )
                {
                    return Collections.emptyList();
                }
                Collections.sort( sorted, ArtifactContentEntryComparator.INSTANCE );
                return sorted;
            }
        }
        finally
        {
            if ( jarFile != null )
            {
                jarFile.close();
            }
        }
        List<ArtifactContentEntry> sorted = new ArrayList<ArtifactContentEntry>( artifactContentEntryMap.values() );
        Collections.sort( sorted, ArtifactContentEntryComparator.INSTANCE );
        return sorted;
    }

    private List<ArtifactContentEntry> getSmallerDepthEntries( Map<String, ArtifactContentEntry> entries )
    {
        int smallestDepth = Integer.MAX_VALUE;
        Map<Integer, List<ArtifactContentEntry>> perDepthList = new HashMap<Integer, List<ArtifactContentEntry>>();
        for ( Map.Entry<String, ArtifactContentEntry> entry : entries.entrySet() )
        {

            ArtifactContentEntry current = entry.getValue();

            if ( current.getDepth() < smallestDepth )
            {
                smallestDepth = current.getDepth();
            }

            List<ArtifactContentEntry> currentList = perDepthList.get( current.getDepth() );

            if ( currentList == null )
            {
                currentList = new ArrayList<ArtifactContentEntry>();
                currentList.add( current );
                perDepthList.put( current.getDepth(), currentList );
            }
            else
            {
                currentList.add( current );
            }

        }

        return perDepthList.get( smallestDepth );
    }

    /**
     * @param path
     * @return org/apache -> org , org -> org
     */
    private String getRootPath( String path )
    {
        if ( StringUtils.contains( path, '/' ) )
        {
            return StringUtils.substringBefore( path, "/" );
        }
        return path;
    }

    private List<String> getSelectedRepos( String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getObservableRepos();

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            // FIXME 403 ???
            return null;
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

    private List<String> getSortedList( Set<String> set )
    {
        List<String> list = new ArrayList<String>( set );
        Collections.sort( list );
        return list;
    }

    private String collapseNamespaces( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                       Collection<String> repoIds, String n )
        throws MetadataResolutionException
    {
        Set<String> subNamespaces = new LinkedHashSet<String>();
        for ( String repoId : repoIds )
        {
            subNamespaces.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, n ) );
        }
        if ( subNamespaces.size() != 1 )
        {
            log.debug( "{} is not collapsible as it has sub-namespaces: {}", n, subNamespaces );
            return n;
        }
        else
        {
            for ( String repoId : repoIds )
            {
                Collection<String> projects = metadataResolver.resolveProjects( repositorySession, repoId, n );
                if ( projects != null && !projects.isEmpty() )
                {
                    log.debug( "{} is not collapsible as it has projects", n );
                    return n;
                }
            }
            return collapseNamespaces( repositorySession, metadataResolver, repoIds,
                                       n + "." + subNamespaces.iterator().next() );
        }
    }
}
