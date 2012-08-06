package org.apache.archiva.web.tags;

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

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;
import junit.framework.TestCase;
import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.archiva.webtest.memory.TestMetadataResolver;
import org.apache.archiva.webtest.memory.TestRepositorySessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml",
    "classpath:/spring-context-DependencyTreeTest.xml" } )
public class DependencyTreeTest
    extends TestCase
{
    @Inject
    private DependencyTree tree;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private ApplicationContext applicationContext;


    private static final String TEST_VERSION = "version";

    private static final String TEST_REPO_ID = "test-repo";

    private static final String TEST_GROUP_ID = "groupId";

    private static final String TEST_ARTIFACT_ID = "artifactId";


    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        ConfigurationManager configurationManager = new ConfigurationManager();
        configurationManager.addContainerProvider( new XWorkConfigurationProvider() );
        com.opensymphony.xwork2.config.Configuration config = configurationManager.getConfiguration();
        Container container = config.getContainer();

        ValueStack stack = container.getInstance( ValueStackFactory.class ).createValueStack();
        stack.getContext().put( ActionContext.CONTAINER, container );
        ActionContext.setContext( new ActionContext( stack.getContext() ) );

        assertNotNull( ActionContext.getContext() );

        Configuration configuration = new Configuration();
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_REPO_ID );
        repoConfig.setLocation( "src/test/repositories/test" );
        configuration.addManagedRepository( repoConfig );

        ArchivaConfiguration archivaConfiguration = applicationContext.getBean( ArchivaConfiguration.class );
        archivaConfiguration.save( configuration );

        TestMetadataResolver metadataResolver = applicationContext.getBean( TestMetadataResolver.class );
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_VERSION );
        metadataResolver.setProjectVersion( TEST_REPO_ID, TEST_GROUP_ID, TEST_ARTIFACT_ID, metadata );

        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getResolver() ).thenReturn( metadataResolver );
        TestRepositorySessionFactory repositorySessionFactory =
            applicationContext.getBean( TestRepositorySessionFactory.class );
        repositorySessionFactory.setRepositorySession( repositorySession );
    }

    @Test
    public void testTree()
        throws ArchivaException
    {
        List<DependencyTree.TreeEntry> entries = tree.gatherTreeList( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION );
        assertEquals( 8, entries.size() );

        DependencyTree.TreeEntry artifactId = entries.get( 0 );
        assertEquals( "<ul><li>", artifactId.getPre() );
        // olamy tree with aether always create jar so createPomArtifact failed but it's not used
        assertTrue( assertArtifactsEquals( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ),
                                           artifactId.getArtifact() ) );
        assertEquals( "</li>", artifactId.getPost() );

        DependencyTree.TreeEntry child1 = entries.get( 1 );
        assertEquals( "<ul><li>", child1.getPre() );
        assertTrue( assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "child1", "1.0" ), child1.getArtifact() ) );
        assertEquals( "</li>", child1.getPost() );

        DependencyTree.TreeEntry grandchild = entries.get( 2 );
        assertEquals( "<ul><li>", grandchild.getPre() );
        assertTrue(
            assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "grandchild1", "2.0" ), grandchild.getArtifact() ) );
        assertEquals( "</li>", grandchild.getPost() );

        DependencyTree.TreeEntry greatGrandchild = entries.get( 3 );
        assertEquals( "<ul><li>", greatGrandchild.getPre() );
        assertTrue( assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "great-grandchild", "3.0" ),
                                           greatGrandchild.getArtifact() ) );
        assertEquals( "</li></ul></ul>", greatGrandchild.getPost() );

        DependencyTree.TreeEntry child2 = entries.get( 4 );
        assertEquals( "<li>", child2.getPre() );
        assertTrue( assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "child2", "1.0" ), child2.getArtifact() ) );
        assertEquals( "</li>", child2.getPost() );

        DependencyTree.TreeEntry grandchild2 = entries.get( 5 );
        assertEquals( "<ul><li>", grandchild2.getPre() );
        assertTrue(
            assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "grandchild2", "2.0" ), grandchild2.getArtifact() ) );
        assertEquals( "</li>", grandchild2.getPost() );

        DependencyTree.TreeEntry grandchild3 = entries.get( 6 );
        assertEquals( "<li>", grandchild3.getPre() );
        assertTrue(
            assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "grandchild3", "2.0" ), grandchild3.getArtifact() ) );
        assertEquals( "</li></ul>", grandchild3.getPost() );

        DependencyTree.TreeEntry child3 = entries.get( 7 );
        assertEquals( "<li>", child3.getPre() );
        assertTrue( assertArtifactsEquals( createArtifact( TEST_GROUP_ID, "child3", "1.0" ), child3.getArtifact() ) );
        assertEquals( "</li></ul></ul>", child3.getPost() );
    }

    private Artifact createPomArtifact( String groupId, String artifactId, String version )
    {
        return new DefaultArtifact( groupId + ":" + artifactId + ":" + version );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return new DefaultArtifact( groupId, artifactId, "jar", version );
    }


    public boolean assertArtifactsEquals( Artifact a, Artifact b )
    {
        return a.getArtifactId().equals( b.getArtifactId() ) && a.getGroupId().equals( b.getGroupId() )
            && a.getVersion().equals( b.getVersion() ) && a.getExtension().equals( b.getExtension() )
            && a.getClassifier().equals( b.getClassifier() );
    }

}
