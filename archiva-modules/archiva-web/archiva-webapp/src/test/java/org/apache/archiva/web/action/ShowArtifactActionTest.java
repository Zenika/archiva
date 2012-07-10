package org.apache.archiva.web.action;

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

import com.opensymphony.xwork2.Action;
import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.webtest.memory.TestMetadataResolver;
import org.apache.archiva.webtest.memory.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.content.ManagedDefaultRepositoryContent;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShowArtifactActionTest
    extends AbstractActionTestCase
{
    private static final String ACTION_HINT = "showArtifactAction";

    private static final String TEST_VERSION = "version";

    private static final String TEST_SNAPSHOT_VERSION = "1.0-SNAPSHOT";

    private static final String TEST_TS_SNAPSHOT_VERSION = "1.0-20091120.111111-1";

    private static final String TEST_NAMESPACE = "namespace";

    private static final String OTHER_TEST_REPO = "first-repo";

    private ShowArtifactAction action;

    private static final List<ArtifactMetadata> TEST_SNAPSHOT_ARTIFACTS =
        Arrays.asList( createArtifact( TEST_TS_SNAPSHOT_VERSION ),
                       createArtifact( "1.0-20091120.222222-2", "20091120.222222", 2 ),
                       createArtifact( "1.0-20091123.333333-3", "20091123.333333", 3 ) );

    private static final long TEST_SIZE = 12345L;

    private static final String TEST_TYPE = "jar";

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        action = (ShowArtifactAction) getActionProxy( "/showArtifact.action" ).getAction();

        metadataResolver = new TestMetadataResolver();
        MetadataRepository repo = mock( MetadataRepository.class );
        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getResolver() ).thenReturn( metadataResolver );
        when( repositorySession.getRepository() ).thenReturn( repo );
        TestRepositorySessionFactory repositorySessionFactory =
            applicationContext.getBean( "repositorySessionFactory#test", TestRepositorySessionFactory.class );
        repositorySessionFactory.setRepositorySession( repositorySession );

        RepositoryContentFactory factory = mock( RepositoryContentFactory.class );

        action.setRepositoryFactory( factory );

        ManagedRepository config = new ManagedRepository();
        config.setId( TEST_REPO );
        config.setLocation( new File( "target/test-repo" ).getAbsolutePath() );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config );
        when( factory.getManagedRepositoryContent( TEST_REPO ) ).thenReturn( content );

        ArchivaConfiguration archivaConfig = mock( ArchivaConfiguration.class );

        Configuration configuration = new Configuration();
        configuration.addManagedRepository(
            new BeanReplicator().replicateBean( config, ManagedRepositoryConfiguration.class ) );
        when( archivaConfig.getConfiguration() ).thenReturn( configuration );

        when( factory.getArchivaConfiguration() ).thenReturn( archivaConfig );

    }

    @Test
    public void testInstantiation()
    {
        assertFalse( action == getActionProxy( "/showArtifact.action" ).getAction() );
    }

    @Test
    public void testGetArtifactUniqueRelease()
    {
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetArtifactUniqueSnapshot()
    {
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_SNAPSHOT_VERSION ) );
        metadataResolver.setArtifacts( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
                                       TEST_SNAPSHOT_ARTIFACTS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertEquals( TEST_GROUP_ID, action.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, action.getArtifactId() );
        assertEquals( TEST_SNAPSHOT_VERSION, action.getVersion() );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model, TEST_SNAPSHOT_VERSION );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertArtifacts( TEST_SNAPSHOT_ARTIFACTS, action.getArtifacts() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
    }

    @Test
    public void testGetArtifactUniqueSnapshotTimestamped()
    {
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_SNAPSHOT_VERSION ) );
        metadataResolver.setArtifacts( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
                                       TEST_SNAPSHOT_ARTIFACTS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_TS_SNAPSHOT_VERSION );

        String result = action.artifact();
        assertError( result );
        assertNoOutputFields();
    }

    @Test
    public void testGetMissingProject()
    {
        setActionParameters();

        String result = action.artifact();
        assertError( result );

        assertActionParameters( action );
        assertNoOutputFields();
    }

    @Test
    public void testGetArtifactNoObservableRepos()
    {
        setObservableRepos( Collections.<String>emptyList() );

        setActionParameters();

        String result = action.artifact();

        // Actually, it'd be better to have an error:
        assertError( result );
        assertActionParameters( action );
        assertNoOutputFields();
    }

    @Test
    public void testGetArtifactNotInObservableRepos()
    {
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();
        assertError( result );

        assertActionParameters( action );
        assertNoOutputFields();
    }

    @Test
    public void testGetArtifactOnlySeenInSecondObservableRepo()
    {
        setObservableRepos( Arrays.asList( OTHER_TEST_REPO, TEST_REPO ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetArtifactSeenInBothObservableRepo()
    {
        setObservableRepos( Arrays.asList( TEST_REPO, OTHER_TEST_REPO ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetArtifactCanOnlyObserveInOneOfTwoRepos()
    {
        setObservableRepos( Arrays.asList( TEST_REPO ) );
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetArtifactNoMavenFacet()
    {
        ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata();
        versionMetadata.setId( TEST_VERSION );
        versionMetadata.setUrl( TEST_URL );
        versionMetadata.setName( TEST_NAME );
        versionMetadata.setDescription( TEST_DESCRIPTION );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertEquals( TEST_VERSION, model.getVersion() );
        assertEquals( TEST_URL, model.getUrl() );
        assertEquals( TEST_NAME, model.getName() );
        assertEquals( TEST_DESCRIPTION, model.getDescription() );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testMetadataHasRepositoryFacetProblem()
    {
        String errMsg = "Error in resolving artifact's parent POM file: Sample Parent POM not found";
        ProjectVersionMetadata metaData = createProjectModel( TEST_SNAPSHOT_VERSION );
        metaData.addFacet(
            createRepositoryProblemFacet( TEST_REPO, errMsg, TEST_GROUP_ID, TEST_SNAPSHOT_VERSION, TEST_NAMESPACE ) );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, metaData );

        metadataResolver.setArtifacts( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
                                       TEST_SNAPSHOT_ARTIFACTS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertEquals( Action.SUCCESS, result );

        assertTrue( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );
        assertEquals( "Artifact metadata is incomplete: " + errMsg, action.getActionErrors().toArray()[0].toString() );
    }

    @Test
    public void testMetadataIncomplete()
    {
        ProjectVersionMetadata metaData = createProjectModel( TEST_SNAPSHOT_VERSION );
        metaData.setIncomplete( true );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, metaData );

        metadataResolver.setArtifacts( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
                                       TEST_SNAPSHOT_ARTIFACTS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_SNAPSHOT_VERSION );
        ;

        String result = action.artifact();

        assertEquals( Action.SUCCESS, result );

        assertTrue( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );

        assertEquals( "Artifact metadata is incomplete.", action.getActionErrors().toArray()[0].toString() );
    }

    @Test
    public void testGetMailingLists()
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );
        MailingList ml1 = createMailingList( "Users List", "users" );
        MailingList ml2 = createMailingList( "Developers List", "dev" );
        versionMetadata.setMailingLists( Arrays.asList( ml1, ml2 ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.mailingLists();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getMailingLists() );
        assertMailingList( action.getMailingLists().get( 0 ), "Users List", "users" );
        assertMailingList( action.getMailingLists().get( 1 ), "Developers List", "dev" );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetDependencies()
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );
        Dependency dependency1 = createDependencyBasic( "artifactId1" );
        Dependency dependency2 = createDependencyExtended( "artifactId2" );
        versionMetadata.setDependencies( Arrays.asList( dependency1, dependency2 ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.dependencies();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getDependencies() );
        assertDependencyBasic( action.getDependencies().get( 0 ), "artifactId1" );
        assertDependencyExtended( action.getDependencies().get( 1 ), "artifactId2" );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNull( action.getDependees() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetDependees()
        throws Exception
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );
        ProjectVersionReference dependee1 = createReference( "artifactId1" );
        ProjectVersionReference dependee2 = createReference( "artifactId2" );
        metadataResolver.setProjectReferences( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION,
                                               Arrays.asList( dependee1, dependee2 ) );

        setActionParameters();

        String result = action.dependees();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ProjectVersionMetadata model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getDependees() );
        assertCoordinate( action.getDependees().get( 0 ), "artifactId1" );
        assertCoordinate( action.getDependees().get( 1 ), "artifactId2" );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testGetProjectMetadata()
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.projectMetadata();

        assertActionSuccess( action, result );

        assertActionParameters( action );

        Map<String, String> genericMetadata = action.getGenericMetadata();
        assertNotNull( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ) );
        assertEquals( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ),
                      TEST_GENERIC_METADATA_PROPERTY_VALUE );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNotNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    @Test
    public void testAddAndDeleteMetadataProperty()
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();
        action.setPropertyName( "foo" );
        action.setPropertyValue( "bar" );
        action.setRepositoryId( TEST_REPO );

        String result = action.addMetadataProperty();

        assertActionSuccess( action, result );
        assertActionParameters( action );

        Map<String, String> genericMetadata = action.getGenericMetadata();
        assertNotNull( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ) );
        assertEquals( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ),
                      TEST_GENERIC_METADATA_PROPERTY_VALUE );

        assertNotNull( genericMetadata.get( "foo" ) );
        assertEquals( "bar", genericMetadata.get( "foo" ) );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNotNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );

        // test delete property
        setActionParameters();
        action.setDeleteItem( "foo" );

        result = action.deleteMetadataEntry();

        assertEquals( Action.SUCCESS, result );
        assertActionParameters( action );
        assertTrue( !action.getActionMessages().isEmpty() );
        assertTrue( action.getActionMessages().contains( "Property successfully deleted." ) );

        genericMetadata = action.getGenericMetadata();
        assertNotNull( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ) );
        assertEquals( genericMetadata.get( TEST_GENERIC_METADATA_PROPERTY_NAME ),
                      TEST_GENERIC_METADATA_PROPERTY_VALUE );

        assertNull( genericMetadata.get( "foo" ) );

        assertEquals( TEST_REPO, action.getRepositoryId() );
        assertNotNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    private void assertArtifacts( List<ArtifactMetadata> expectedArtifacts,
                                  Map<String, List<Artifact>> artifactMap )
    {
        // assuming only one of each version at this point
        assertEquals( expectedArtifacts.size(), artifactMap.size() );
        for ( ArtifactMetadata artifact : expectedArtifacts )
        {
            assertTrue( artifactMap.containsKey( artifact.getVersion() ) );
            List<Artifact> list = artifactMap.get( artifact.getVersion() );
            Artifact actual = list.get( 0 );
            assertEquals( artifact.getNamespace(), actual.getGroupId() );
            assertEquals( artifact.getId(),actual.getId() );
            // olamy test has no more sense as we reuse an other object now
            //assertEquals( artifact.getProject(), actual.getGroupId() );
            assertEquals( artifact.getRepositoryId(), actual.getRepositoryId() );
            assertEquals( artifact.getVersion(), actual.getVersion() );
            assertEquals( TEST_TYPE, actual.getPackaging() );
            assertEquals( "12.06 K", actual.getSize() );
            // FIXME url path test
            //assertEquals( artifact.getNamespace() + "/" + artifact.getProject() + "/" + TEST_SNAPSHOT_VERSION + "/"
            //                  + artifact.getId(), actual.getPath() );
        }
    }

    private static ArtifactMetadata createArtifact( String version )
    {
        return createArtifact( version, null, 0 );
    }

    private static ArtifactMetadata createArtifact( String version, String timestamp, int buildNumber )
    {
        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setProject( TEST_ARTIFACT_ID );
        metadata.setId( TEST_ARTIFACT_ID + "-" + version + ".jar" );
        metadata.setNamespace( TEST_GROUP_ID );
        metadata.setRepositoryId( TEST_REPO );
        metadata.setSize( TEST_SIZE );
        metadata.setProjectVersion( VersionUtil.getBaseVersion( version ) );
        metadata.setVersion( version );

        MavenArtifactFacet facet = new MavenArtifactFacet();
        facet.setType( "jar" );
        facet.setTimestamp( timestamp );
        facet.setBuildNumber( buildNumber );
        metadata.addFacet( facet );

        return metadata;
    }

    private ProjectVersionReference createReference( String projectId )
    {
        ProjectVersionReference reference = new ProjectVersionReference();
        reference.setNamespace( "groupId" );
        reference.setProjectId( projectId );
        reference.setProjectVersion( "version" );
        reference.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );
        return reference;
    }

    private void assertCoordinate( ProjectVersionReference dependee, String artifactId )
    {
        assertEquals( artifactId, dependee.getProjectId() );
        assertEquals( "groupId", dependee.getNamespace() );
        assertEquals( "version", dependee.getProjectVersion() );
    }

    private void assertDependencyBasic( Dependency dependency, String artifactId )
    {
        assertEquals( artifactId, dependency.getArtifactId() );
        assertEquals( "groupId", dependency.getGroupId() );
        assertEquals( "version", dependency.getVersion() );
    }

    private void assertDependencyExtended( Dependency dependency, String artifactId )
    {
        assertDependencyBasic( dependency, artifactId );
        assertEquals( true, dependency.isOptional() );
        assertEquals( "classifier", dependency.getClassifier() );
        assertEquals( "type", dependency.getType() );
        assertEquals( "scope", dependency.getScope() );
        assertEquals( "systemPath", dependency.getSystemPath() );
    }

    private Dependency createDependencyExtended( String artifactId )
    {
        Dependency dependency = createDependencyBasic( artifactId );
        dependency.setClassifier( "classifier" );
        dependency.setOptional( true );
        dependency.setScope( "scope" );
        dependency.setSystemPath( "systemPath" );
        dependency.setType( "type" );
        return dependency;
    }

    private Dependency createDependencyBasic( String artifactId )
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( artifactId );
        dependency.setGroupId( "groupId" );
        dependency.setVersion( "version" );
        return dependency;
    }

    private void assertMailingList( MailingList mailingList, String name, String prefix )
    {
        assertEquals( name, mailingList.getName() );
        assertEquals( prefix + "-post@", mailingList.getPostAddress() );
        assertEquals( prefix + "-subscribe@", mailingList.getSubscribeAddress() );
        assertEquals( prefix + "-unsubscribe@", mailingList.getUnsubscribeAddress() );
        assertEquals( prefix + "-archive-url", mailingList.getMainArchiveUrl() );
        assertEquals( Arrays.asList( "other-" + prefix + "-archive-url-1", "other-" + prefix + "-archive-url-2" ),
                      mailingList.getOtherArchives() );
    }

    private MailingList createMailingList( String name, String prefix )
    {
        MailingList ml1 = new MailingList();
        ml1.setName( name );
        ml1.setPostAddress( prefix + "-post@" );
        ml1.setSubscribeAddress( prefix + "-subscribe@" );
        ml1.setUnsubscribeAddress( prefix + "-unsubscribe@" );
        ml1.setMainArchiveUrl( prefix + "-archive-url" );
        ml1.setOtherArchives(
            Arrays.asList( "other-" + prefix + "-archive-url-1", "other-" + prefix + "-archive-url-2" ) );
        return ml1;
    }

    private void assertNoOutputFields()
    {
        assertNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getArtifacts().isEmpty() );
    }

    private void assertError( String result )
    {
        assertEquals( Action.ERROR, result );
        assertEquals( 1, action.getActionErrors().size() );
    }

    private void assertDefaultModel( ProjectVersionMetadata model )
    {
        assertDefaultModel( model, TEST_VERSION );
    }

    private void setActionParameters()
    {
        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_VERSION );
    }

    private void assertActionParameters( ShowArtifactAction action )
    {
        assertEquals( TEST_GROUP_ID, action.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, action.getArtifactId() );
        assertEquals( TEST_VERSION, action.getVersion() );
    }

    private void assertActionSuccess( ShowArtifactAction action, String result )
    {
        assertEquals( Action.SUCCESS, result );
        assertTrue( action.getActionErrors().isEmpty() );
        assertTrue( action.getActionMessages().isEmpty() );
    }

    private RepositoryProblemFacet createRepositoryProblemFacet( String repoId, String errMsg, String projectId,
                                                                 String projectVersion, String namespace )
    {
        RepositoryProblemFacet repoProblemFacet = new RepositoryProblemFacet();
        repoProblemFacet.setRepositoryId( repoId );
        repoProblemFacet.setId( repoId );
        repoProblemFacet.setMessage( errMsg );
        repoProblemFacet.setProblem( errMsg );
        repoProblemFacet.setProject( projectId );
        repoProblemFacet.setVersion( projectVersion );
        repoProblemFacet.setNamespace( namespace );
        return repoProblemFacet;
    }
}
