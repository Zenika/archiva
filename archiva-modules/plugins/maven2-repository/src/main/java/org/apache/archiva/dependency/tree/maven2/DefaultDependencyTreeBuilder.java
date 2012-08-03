package org.apache.archiva.dependency.tree.maven2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.common.utils.Slf4JPlexusLogger;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.RepositoryModelResolver;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.ManagedVersionMap;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.StateDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of <code>DependencyTreeBuilder</code>. Customized wrapper for maven-dependency-tree to use
 * maven-model-builder instead of maven-project. Note that the role must differ to avoid conflicting with the
 * maven-shared implementation.
 */
@Service( "dependencyTreeBuilder#maven2" )
public class DefaultDependencyTreeBuilder
    implements DependencyTreeBuilder
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    /**
     *
     */
    private ArtifactFactory factory;

    /**
     *
     */
    private ArtifactCollector collector;

    /**
     *
     */
    private ModelBuilder builder;

    /**
     * TODO: can have other types, and this might eventually come through from the main request
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    /**
     *
     */
    @Inject
    @Named( value = "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private WagonFactory wagonFactory;

    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        factory = plexusSisuBridge.lookup( ArtifactFactory.class, "default" );
        collector = plexusSisuBridge.lookup( ArtifactCollector.class, "default" );

        DefaultModelBuilderFactory defaultModelBuilderFactory = new DefaultModelBuilderFactory();
        builder = defaultModelBuilderFactory.newInstance();
    }

    public void buildDependencyTree( List<String> repositoryIds, String groupId, String artifactId, String version,
                                     DependencyNodeVisitor nodeVisitor )
        throws DependencyTreeBuilderException
    {
        DependencyTreeResolutionListener listener =
            new DependencyTreeResolutionListener( new Slf4JPlexusLogger( getClass() ) );

        Artifact projectArtifact = factory.createProjectArtifact( groupId, artifactId, version );
        ManagedRepository repository = null;
        try
        {
            repository = findArtifactInRepositories( repositoryIds, projectArtifact );
        }
        catch ( RepositoryAdminException e )
        {
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }

        if ( repository == null )
        {
            // metadata could not be resolved
            return;
        }

        try
        {
            // MRM-1411
            // TODO: this is a workaround for a lack of proxy capability in the resolvers - replace when it can all be
            //       handled there. It doesn't cache anything locally!
            List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
            Map<String, NetworkProxy> networkProxies = new HashMap<String, NetworkProxy>();

            Map<String, List<ProxyConnector>> proxyConnectorsMap = proxyConnectorAdmin.getProxyConnectorAsMap();
            List<ProxyConnector> proxyConnectors = proxyConnectorsMap.get( repository.getId() );
            if ( proxyConnectors != null )
            {
                for ( ProxyConnector proxyConnector : proxyConnectors )
                {
                    remoteRepositories.add(
                        remoteRepositoryAdmin.getRemoteRepository( proxyConnector.getTargetRepoId() ) );

                    NetworkProxy networkProxyConfig = networkProxyAdmin.getNetworkProxy( proxyConnector.getProxyId() );

                    if ( networkProxyConfig != null )
                    {
                        // key/value: remote repo ID/proxy info
                        networkProxies.put( proxyConnector.getTargetRepoId(), networkProxyConfig );
                    }
                }
            }

            Model model = buildProject(
                new RepositoryModelResolver( repository, pathTranslator, wagonFactory, remoteRepositories, networkProxies,
                                             repository ), groupId, artifactId, version );

            Map managedVersions = createManagedVersionMap( model );

            Set<Artifact> dependencyArtifacts = createArtifacts( model, null );

            RepositorySession repositorySession = repositorySessionFactory.createSession();
            try
            {
                ArtifactMetadataSource metadataSource =
                    new MetadataArtifactMetadataSource( repositoryIds, repositorySession );

                // Note that we don't permit going to external repositories. We don't need to pass in a local and remote
                // since our metadata source has control over them
                collector.collect( dependencyArtifacts, projectArtifact, managedVersions, null, null, metadataSource,
                                   null, Collections.singletonList( listener ) );

                //collector.collect( dependencyArtifacts, projectArtifact, null, Collections.<ArtifactRepository>emptyList(),
                //                   metadataSource, null,  Collections.singletonList( (ResolutionListener) listener ) );

                /*
                Set<Artifact> artifacts, Artifact originatingArtifact,
                                      ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                      ArtifactMetadataSource source, ArtifactFilter filter,
                                      List< ResolutionListener > listeners
                */
            }
            finally
            {
                repositorySession.close();
            }

            DependencyNode rootNode = listener.getRootNode();

            // TODO: remove the need for this when the serializer can calculate last nodes from visitor calls only
            DependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor( nodeVisitor );

            CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
            DependencyNodeVisitor firstPassVisitor =
                new FilteringDependencyNodeVisitor( collectingVisitor, StateDependencyNodeFilter.INCLUDED );
            rootNode.accept( firstPassVisitor );

            DependencyNodeFilter secondPassFilter =
                new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
            visitor = new FilteringDependencyNodeVisitor( visitor, secondPassFilter );

            rootNode.accept( visitor );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new DependencyTreeBuilderException( "Invalid dependency version for artifact " + projectArtifact );
        }
        catch ( ModelBuildingException e )
        {
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }
        catch ( UnresolvableModelException e )
        {
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }
        catch ( RepositoryAdminException e )
        {
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }
    }

    private ManagedRepository findArtifactInRepositories( List<String> repositoryIds, Artifact projectArtifact )
        throws RepositoryAdminException
    {
        for ( String repoId : repositoryIds )
        {
            ManagedRepository managedRepository = managedRepositoryAdmin.getManagedRepository( repoId );

            File repoDir = new File( managedRepository.getLocation() );
            File file = pathTranslator.toFile( repoDir, projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                                               projectArtifact.getBaseVersion(),
                                               projectArtifact.getArtifactId() + "-" + projectArtifact.getVersion()
                                                   + ".pom" );

            if ( file.exists() )
            {
                return managedRepository;
            }
        }
        return null;
    }

    private Model buildProject( RepositoryModelResolver modelResolver, String groupId, String artifactId,
                                String version )
        throws ModelBuildingException, UnresolvableModelException
    {
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins( false );
        req.setModelSource( modelResolver.resolveModel( groupId, artifactId, version ) );
        req.setModelResolver( modelResolver );
        req.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        //MRM-1607. olamy this will resolve jdk profiles on the current running archiva jvm
        req.setSystemProperties( System.getProperties() );

        return builder.build( req ).getEffectiveModel();
    }

    // from maven-project to avoid the dependency on it
    private Set<Artifact> createArtifacts( Model model, ArtifactFilter dependencyFilter )
        throws InvalidVersionSpecificationException
    {
        Collection<Dependency> dependencies = model.getDependencies();
        Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>( dependencies.size() );

        for ( Dependency dependency : dependencies )
        {
            String scope = dependency.getScope();

            if ( StringUtils.isEmpty( scope ) )
            {
                scope = Artifact.SCOPE_COMPILE;

                dependency.setScope( scope );
            }

            VersionRange versionRange = VersionRange.createFromVersionSpec( dependency.getVersion() );
            Artifact artifact =
                factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(), versionRange,
                                                  dependency.getType(), dependency.getClassifier(), scope, null,
                                                  dependency.isOptional() );

            if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
            {
                artifact.setFile( new File( dependency.getSystemPath() ) );
            }

            ArtifactFilter artifactFilter = dependencyFilter;

            // MNG-3769: It would be nice to be able to process relocations here,
            // so we could have this filtering step apply to post-relocated dependencies.
            // HOWEVER, this would require a much more invasive POM resolution process
            // in order to look for relocations, which would make the early steps in
            // a Maven build way too heavy.
            if ( artifact != null && ( artifactFilter == null || artifactFilter.include( artifact ) ) )
            {
                if ( dependency.getExclusions() != null && !dependency.getExclusions().isEmpty() )
                {
                    List<String> exclusions = new ArrayList<String>();
                    for ( Object o : dependency.getExclusions() )
                    {
                        Exclusion e = (Exclusion) o;
                        exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                    }

                    ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

                    if ( artifactFilter != null )
                    {
                        AndArtifactFilter filter = new AndArtifactFilter();
                        filter.add( artifactFilter );
                        filter.add( newFilter );
                        artifactFilter = filter;
                    }
                    else
                    {
                        artifactFilter = newFilter;
                    }
                }

                artifact.setDependencyFilter( artifactFilter );

                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;

    }

    // from maven-project to avoid the dependency on it

    private Map createManagedVersionMap( Model model )
        throws InvalidVersionSpecificationException
    {
        DependencyManagement dependencyManagement = model.getDependencyManagement();

        Map<String, Artifact> map = null;
        List<Dependency> deps;
        if ( ( dependencyManagement != null ) && ( ( deps = dependencyManagement.getDependencies() ) != null ) && (
            deps.size() > 0 ) )
        {
            map = new ManagedVersionMap( map );

            for ( Dependency dependency : dependencyManagement.getDependencies() )
            {

                VersionRange versionRange = VersionRange.createFromVersionSpec( dependency.getVersion() );

                Artifact artifact =
                    factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(), versionRange,
                                                      dependency.getType(), dependency.getClassifier(),
                                                      dependency.getScope(), dependency.isOptional() );

                log.debug( "artifact {}", artifact );

                // If the dependencyManagement section listed exclusions,
                // add them to the managed artifacts here so that transitive
                // dependencies will be excluded if necessary.
                if ( ( null != dependency.getExclusions() ) && !dependency.getExclusions().isEmpty() )
                {
                    List<String> exclusions = new ArrayList<String>();

                    for ( Exclusion exclusion : dependency.getExclusions() )
                    {
                        exclusions.add( exclusion.getGroupId() + ":" + exclusion.getArtifactId() );
                    }
                    ExcludesArtifactFilter eaf = new ExcludesArtifactFilter( exclusions );
                    artifact.setDependencyFilter( eaf );
                }
                else
                {
                    artifact.setDependencyFilter( null );
                }
                map.put( dependency.getManagementKey(), artifact );
            }
        }
        else
        {
            map = Collections.emptyMap();
        }

        return map;
    }

    private class MetadataArtifactMetadataSource
        implements ArtifactMetadataSource
    {
        private final List<String> repositoryIds;

        private final RepositorySession session;

        private final MetadataResolver resolver;

        public MetadataArtifactMetadataSource( List<String> repositoryIds, RepositorySession session )
        {
            this.repositoryIds = repositoryIds;
            this.session = session;
            resolver = this.session.getResolver();
        }

        // modified version from MavenMetadataSource to work with the simpler environment
        public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                         List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            // TODO: we removed relocation support here. This is something that might need to be generically handled
            //       throughout this module

            Artifact pomArtifact =
                factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                               artifact.getScope() );

            ManagedRepository repository = null;
            try
            {
                repository = findArtifactInRepositories( repositoryIds, pomArtifact );
            }
            catch ( RepositoryAdminException e )
            {
                throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
            }
            Model project = null;
            if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) && repository != null )
            {

                try
                {

                    File basedir = new File( repository.getLocation() );
                    project =
                        buildProject( new RepositoryModelResolver( basedir, pathTranslator ), artifact.getGroupId(),
                                      artifact.getArtifactId(), artifact.getVersion() );
                }
                catch ( ModelBuildingException e )
                {
                    throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
                }
                catch ( UnresolvableModelException e )
                {
                    throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
                }

            }

            ResolutionGroup result;

            if ( project == null )
            {
                // TODO: we could record this so that it is displayed in the dependency tree as (...) or similar

                // if the project is null, we encountered an invalid model (read: m1 POM)
                // we'll just return an empty resolution group.
                // or used the inherited scope (should that be passed to the buildFromRepository method above?)
                result = new ResolutionGroup( pomArtifact, Collections.<Artifact>emptySet(),
                                              Collections.<ArtifactRepository>emptyList() );
            }
            else
            {
                Set<Artifact> artifacts = Collections.emptySet();
                if ( !artifact.getArtifactHandler().isIncludesDependencies() )
                {
                    try
                    {
                        artifacts = createArtifacts( project, artifact.getDependencyFilter() );
                    }
                    catch ( InvalidVersionSpecificationException e )
                    {
                        throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
                    }
                }

                result = new ResolutionGroup( pomArtifact, artifacts, Collections.<ArtifactRepository>emptyList() );
            }

            return result;
        }

        public List retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                               List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            Set<ArtifactVersion> versions = new HashSet<ArtifactVersion>();
            for ( String repoId : repositoryIds )
            {
                Collection<String> projectVersions;
                try
                {
                    projectVersions = resolver.resolveProjectVersions( session, repoId, artifact.getGroupId(),
                                                                       artifact.getArtifactId() );
                }
                catch ( MetadataResolutionException e )
                {
                    throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
                }
                for ( String version : projectVersions )
                {
                    versions.add( new DefaultArtifactVersion( version ) );
                }
            }

            return new ArrayList<ArtifactVersion>( versions );
        }


        public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact,
                                                                                        ArtifactRepository artifactRepository,
                                                                                        ArtifactRepository artifactRepository1 )
            throws ArtifactMetadataRetrievalException
        {
            // TODO
            return null;
        }
    }

    public ArtifactFactory getFactory()
    {
        return factory;
    }
}