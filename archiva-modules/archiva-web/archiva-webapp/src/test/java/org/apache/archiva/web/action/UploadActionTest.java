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
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;

/**
 * UploadActionTest
 */
public class UploadActionTest
    extends AbstractActionTestCase
{
    private UploadAction uploadAction;

    private RepositoryContentFactory repoFactory;

    private MockControl repoFactoryControl;

    private MockControl managedRepoAdminControl;

    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private MockControl archivaAdminControl;

    private ArchivaAdministration archivaAdministration;

    private static final String REPOSITORY_ID = "test-repo";


    private ManagedRepository managedRepository;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        MockControl schedulerControl = MockControl.createControl( ArchivaTaskScheduler.class );
        ArchivaTaskScheduler scheduler = (ArchivaTaskScheduler) schedulerControl.getMock();

        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        managedRepoAdminControl = MockControl.createControl( ManagedRepositoryAdmin.class );
        managedRepositoryAdmin = (ManagedRepositoryAdmin) managedRepoAdminControl.getMock();

        archivaAdminControl = MockControl.createControl( ArchivaAdministration.class );
        archivaAdministration = (ArchivaAdministration) archivaAdminControl.getMock();

        uploadAction = new UploadAction();
        uploadAction.setScheduler( scheduler );
        uploadAction.setManagedRepositoryAdmin( managedRepositoryAdmin );
        uploadAction.setArchivaAdministration( archivaAdministration );

        uploadAction.setRepositoryFactory( repoFactory );

        File testRepo = new File( FileUtil.getBasedir(), "target/test-classes/test-repo" );
        testRepo.mkdirs();

        assertTrue( testRepo.exists() );

        managedRepository = new ManagedRepository();
        managedRepository.setId( REPOSITORY_ID );
        managedRepository.setLayout( "default" );
        managedRepository.setLocation( testRepo.getPath() );
        managedRepository.setName( REPOSITORY_ID );
        managedRepository.setBlockRedeployments( true );

    }

    @Override
    public void tearDown()
        throws Exception
    {
        File testRepo = new File( this.managedRepository.getLocation() );
        FileUtils.deleteDirectory( testRepo );

        assertFalse( testRepo.exists() );

        super.tearDown();
    }

    private void setUploadParameters( String version, String classifier, File artifact, File pomFile,
                                      boolean generatePom )
    {
        uploadAction.setRepositoryId( REPOSITORY_ID );
        uploadAction.setGroupId( "org.apache.archiva" );
        uploadAction.setArtifactId( "artifact-upload" );
        uploadAction.setVersion( version );
        uploadAction.setPackaging( "jar" );

        uploadAction.setClassifier( classifier );
        uploadAction.setArtifact( artifact );
        uploadAction.setPom( pomFile );
        uploadAction.setGeneratePom( generatePom );
    }

    private void assertAllArtifactsIncludingSupportArtifactsArePresent( String repoLocation, String artifact,
                                                                        String version )
    {
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".jar" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".jar.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".jar.md5" ).exists() );

        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".pom" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".pom.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".pom.md5" ).exists() );

        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".md5" ).exists() );
    }

    private void verifyVersionMetadataChecksums( String repoLocation, String version )
        throws IOException
    {
        ChecksummedFile checksum = new ChecksummedFile( new File( repoLocation,
                                                                  "/org/apache/archiva/artifact-upload/" + version + "/"
                                                                      + MetadataTools.MAVEN_METADATA ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents = FileUtils.readFileToString( new File( repoLocation,
                                                                "/org/apache/archiva/artifact-upload/" + version + "/"
                                                                    + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString( new File( repoLocation,
                                                         "/org/apache/archiva/artifact-upload/" + version + "/"
                                                             + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyProjectMetadataChecksums( String repoLocation )
        throws IOException
    {
        ChecksummedFile checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyPomChecksums( String repoLocation, String artifact, String version )
        throws IOException
    {
        ChecksummedFile checksum;
        String sha1;
        String md5;
        String contents;
        checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".pom" ) );
        sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".pom.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".pom.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyArtifactChecksums( String repoLocation, String artifact, String version )
        throws IOException
    {
        ChecksummedFile checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".jar" ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".jar.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".jar.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private String getTimestamp( String[] artifactsList, int startIndex, int index )
    {
        int endIndex = -1;
        String timestamp;

        if ( artifactsList[index].contains( "jar" ) )
        {
            endIndex = artifactsList[index].indexOf( ".jar" );
        }
        else
        {
            endIndex = artifactsList[index].indexOf( ".pom" );
        }

        timestamp = artifactsList[index].substring( startIndex, endIndex );

        return timestamp;
    }

    private MockControl mockAuditLogs( List<String> resources )
    {
        return mockAuditLogs( AuditEvent.UPLOAD_FILE, resources );
    }

    private MockControl mockAuditLogs( String action, List<String> resources )
    {
        MockControl control = MockControl.createControl( AuditListener.class );
        AuditListener listener = (AuditListener) control.getMock();
        boolean matcherSet = false;
        for ( String resource : resources )
        {
            listener.auditEvent( new AuditEvent( REPOSITORY_ID, "guest", resource, action ) );
            if ( !matcherSet )
            {
                control.setMatcher( new AuditEventArgumentsMatcher() );
                matcherSet = true;
            }
        }
        control.replay();

        uploadAction.setAuditListeners( Collections.singletonList( listener ) );
        return control;
    }

    @Test
    public void testArtifactUploadWithPomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             new File( FileUtil.getBasedir(), "target/test-classes/upload-artifact-test/pom.xml" ),
                             false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();
        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    @Test
    public void testArtifactUploadWithClassifier()
        throws Exception
    {
        setUploadParameters( "1.0", "tests", new File( FileUtil.getBasedir(),
                                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             new File( FileUtil.getBasedir(), "target/test-classes/upload-artifact-test/pom.xml" ),
                             false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar" ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.sha1" ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.md5" ).exists() );

        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.sha1" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.md5" ).exists() );

        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".md5" ).exists() );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-tests", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    @Test
    public void testArtifactUploadGeneratePomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    @Test
    public void testArtifactUploadNoPomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        MockControl control =
            mockAuditLogs( Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.sha1" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.md5" ).exists() );

        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );
        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.sha1" ).exists() );
        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.md5" ).exists() );

        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".md5" ).exists() );

        // verify checksums of jar file
        ChecksummedFile checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );

        // verify checksums of metadata file
        checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ) );
        sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    @Test
    public void testArtifactUploadFailedRepositoryNotFound()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, false );

        repoFactoryControl.expectAndThrow( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ),
                                           new RepositoryNotFoundException() );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.ERROR, returnString );

        repoFactoryControl.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ).exists() );

        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );

        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
    }

    @Test
    public void testArtifactUploadSnapshots()
        throws Exception
    {
        setUploadParameters( "1.0-SNAPSHOT", null, new File( FileUtil.getBasedir(),
                                                             "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2, 5 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        String timestamp = fmt.format( new Date() );
        MockControl control = mockAuditLogs( Arrays.asList(
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-1.jar",
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-1.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        String[] artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();
        Arrays.sort( artifactsList );

        assertEquals( 9, artifactsList.length );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA
                                  + ".sha1" ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA
                                  + ".md5" ).exists() );

        int startIndex = "artifact-upload-1.0-".length();
        String timestampPath = getTimestamp( artifactsList, startIndex, 0 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestampPath,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number
        File metadataFile = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA );
        ArchivaRepositoryMetadata artifactMetadata = MavenMetadataReader.read( metadataFile );

        SnapshotVersion snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 1, snapshotVersion.getBuildNumber() );

        String timestampPart = StringUtils.substringBeforeLast( timestampPath, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        String buildnumber = StringUtils.substringAfterLast( timestampPath, "-" );
        assertEquals( "Incorrect build number in filename.", "1", buildnumber );

        repoFactoryControl.reset();
        control.reset();

        control.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        // MRM-1353
        // upload snapshot artifact again and check if build number was incremented
        setUploadParameters( "1.0-SNAPSHOT", null, new File( FileUtil.getBasedir(),
                                                             "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        repoFactoryControl.replay();

        fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        timestamp = fmt.format( new Date() );

        control = mockAuditLogs( Arrays.asList(
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-2.jar",
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-2.pom" ) );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();
        Arrays.sort( artifactsList );

        assertEquals( 15, artifactsList.length );

        timestampPath = getTimestamp( artifactsList, startIndex, 6 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestampPath,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number set in metadata and in filename
        metadataFile = new File( repoLocation,
                                 "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA );
        artifactMetadata = MavenMetadataReader.read( metadataFile );

        snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 2, snapshotVersion.getBuildNumber() );

        timestampPart = StringUtils.substringBeforeLast( timestampPath, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        buildnumber = StringUtils.substringAfterLast( timestampPath, "-" );
        assertEquals( "Incorrect build number in filename.", "2", buildnumber );
    }

    @Test
    public void testChecksumIsCorrectWhenArtifactIsReUploaded()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepository repoConfig = getManagedRepository();
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 repoConfig, 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2, 5 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();

        repoFactoryControl.reset();

        String repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );

        // RE-upload artifact
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-reuploaded.jar" ),
                             null, true );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        repoFactoryControl.replay();

        // TODO: track modifications?
//        MockControl control = mockAuditLogs( AuditEvent.MODIFY_FILE, Arrays.asList(
        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    @Test
    public void testUploadArtifactAlreadyExistingRedeploymentsBlocked()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( getManagedRepository() );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 1, 8 );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 getManagedRepository(), 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2, 5 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        MockControl control = mockAuditLogs( Collections.<String>emptyList() );

        returnString = uploadAction.doUpload();
        assertEquals( Action.ERROR, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    @Test
    public void testUploadArtifactAlreadyExistingRedeploymentsAllowed()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepository repoConfig = getManagedRepository();
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 1, 8 );

        managedRepoAdminControl.expectAndReturn( managedRepositoryAdmin.getManagedRepository( REPOSITORY_ID ),
                                                 repoConfig, 1, 8 );

        archivaAdminControl.expectAndReturn( archivaAdministration.getKnownContentConsumers(), new ArrayList<String>(),
                                             2, 5 );

        managedRepoAdminControl.replay();
        archivaAdminControl.replay();

        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null, new File( FileUtil.getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        // TODO: track modifications?
//        MockControl control = mockAuditLogs( AuditEvent.MODIFY_FILE, Arrays.asList(
        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        repoFactoryControl.verify();
        control.verify();

        String repoLocation = getManagedRepository().getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    ManagedRepository getManagedRepository()
    {
        return new BeanReplicator().replicateBean( this.managedRepository, ManagedRepository.class );
    }

}
