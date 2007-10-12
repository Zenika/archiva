package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.custommonkey.xmlunit.XMLAssert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RepositoryPurgeConsumerTest
    extends AbstractRepositoryPurgeTest
{
    private void setLastModified( String path )
    {
        File dir = new File( path );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 );
        }
    }

    public void testConsumerByRetentionCount()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-retention-count" );

        populateDbForRetentionCountTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDaysOlder( 0 ); // force days older off to allow retention count purge to execute.
        repoConfiguration.setRetentionCount( TEST_RETENTION_COUNT );
        addRepoToConfiguration( "retention-count", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        String repoRoot = prepareTestRepo();

        repoPurgeConsumer.processFile( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );
        
        String versionRoot = repoRoot + "/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT";
        
        // assert if removed from repo
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" );

        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.sha1" );

        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.sha1" );
    }

    private void addRepoToConfiguration( String configHint, ManagedRepositoryConfiguration repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class,
                                                                                   configHint );
        archivaConfiguration.getConfiguration().addManagedRepository( repoConfiguration );
    }

    public void testConsumerByDaysOld()
        throws Exception
    {
        populateDbForDaysOldTest();

        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-days-old" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDaysOlder( TEST_DAYS_OLDER ); 
        addRepoToConfiguration( "days-old", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration );

        String repoRoot = prepareTestRepo();
        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-install-plugin";

        setLastModified( projectRoot + "/2.2-SNAPSHOT" );

        repoPurgeConsumer.processFile( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar"  );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" );
    }

    /**
     * Test the snapshot clean consumer on a repository set to NOT clean/delete snapshots
     * based on released versions. 
     * 
     * @throws Exception
     */
    public void testReleasedSnapshotsWereNotCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-retention-count" );

        populateDbForReleasedSnapshotsTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDeleteReleasedSnapshots( false ); // Set to NOT delete released snapshots.
        addRepoToConfiguration( "retention-count", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        String repoRoot = prepareTestRepo();

        repoPurgeConsumer.processFile( PATH_TO_RELEASED_SNAPSHOT );

        // check if the snapshot wasn't removed
        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";
        		
        assertExists( projectRoot + "/2.3-SNAPSHOT" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file wasn't updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );
        
        String expectedVersions = "<expected><versions><version>2.3-SNAPSHOT</version></versions></expected>";
        
        XMLAssert.assertXpathEvaluatesTo( "2.3-SNAPSHOT", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );        
    }

    public void testReleasedSnapshotsWereCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-days-old" );

        populateDbForReleasedSnapshotsTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDeleteReleasedSnapshots( true );
        addRepoToConfiguration( "days-old", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        String repoRoot = prepareTestRepo();

        repoPurgeConsumer.processFile( PATH_TO_RELEASED_SNAPSHOT );

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file was updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );
        
        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );
        
        String expectedVersions = "<expected><versions><version>2.2</version>"
            + "<version>2.3</version></versions></expected>";
        
        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        // FIXME [MRM-535]: XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );        
   }

    public void populateDbForRetentionCountTest()
        throws ArchivaDatabaseException
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0RC1-20070504.153317-1" );
        versions.add( "1.0RC1-20070504.160758-2" );
        versions.add( "1.0RC1-20070505.090015-3" );
        versions.add( "1.0RC1-20070506.090132-4" );

        populateDb( "org.jruby.plugins", "jruby-rake-plugin", versions );
    }

    private void populateDbForDaysOldTest()
        throws ArchivaDatabaseException
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "2.2-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-install-plugin", versions );
    }

    public void populateDbForReleasedSnapshotsTest()
        throws ArchivaDatabaseException
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "2.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-plugin-plugin", versions );
    }
}
