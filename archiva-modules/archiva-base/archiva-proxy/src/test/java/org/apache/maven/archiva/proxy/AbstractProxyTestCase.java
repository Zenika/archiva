package org.apache.maven.archiva.proxy;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsDownloadPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsOnUpdateDownloadPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.wagon.Wagon;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * AbstractProxyTestCase
 *
 * @version $Id$
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public abstract class AbstractProxyTestCase
    extends TestCase
{

    @Inject
    protected ApplicationContext applicationContext;

    protected static final String ID_LEGACY_PROXIED = "legacy-proxied";

    protected static final String ID_PROXIED1 = "proxied1";

    protected static final String ID_PROXIED1_TARGET = "proxied1-target";

    protected static final String ID_PROXIED2 = "proxied2";

    protected static final String ID_PROXIED2_TARGET = "proxied2-target";

    protected static final String ID_DEFAULT_MANAGED = "default-managed-repository";

    protected static final String ID_LEGACY_MANAGED = "legacy-managed-repository";

    protected static final String REPOPATH_PROXIED_LEGACY = "src/test/repositories/legacy-proxied";

    protected static final String REPOPATH_PROXIED1 = "src/test/repositories/proxied1";

    protected static final String REPOPATH_PROXIED1_TARGET = "target/test-repository/proxied1";

    protected static final String REPOPATH_PROXIED2 = "src/test/repositories/proxied2";

    protected static final String REPOPATH_PROXIED2_TARGET = "target/test-repository/proxied2";

    protected static final String REPOPATH_DEFAULT_MANAGED = "src/test/repositories/managed";

    // protected static final String REPOPATH_DEFAULT_MANAGED_TARGET = "target/test-repository/managed";

    protected static final String REPOPATH_LEGACY_MANAGED = "src/test/repositories/legacy-managed";

    protected static final String REPOPATH_LEGACY_MANAGED_TARGET = "target/test-repository/legacy-managed";

    protected MockControl wagonMockControl;

    protected Wagon wagonMock;


    protected RepositoryProxyConnectors proxyHandler;

    protected ManagedRepositoryContent managedDefaultRepository;

    protected File managedDefaultDir;

    protected ManagedRepositoryContent managedLegacyRepository;

    protected File managedLegacyDir;

    protected MockConfiguration config;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    WagonDelegate delegate;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        config =
            (MockConfiguration) applicationContext.getBean( "archivaConfiguration#mock", ArchivaConfiguration.class );

        config.getConfiguration().setManagedRepositories( new ArrayList<ManagedRepositoryConfiguration>() );
        config.getConfiguration().setRemoteRepositories( new ArrayList<RemoteRepositoryConfiguration>() );
        config.getConfiguration().setProxyConnectors( new ArrayList<ProxyConnectorConfiguration>() );

        // Setup source repository (using default layout)
        String repoPath = "target/test-repository/managed/" + getName();
        File repoLocation = new File( repoPath );

        managedDefaultRepository =
            createRepository( ID_DEFAULT_MANAGED, "Default Managed Repository", repoPath, "default" );

        managedDefaultDir = new File( managedDefaultRepository.getRepoRoot() );

        ManagedRepositoryConfiguration repoConfig = managedDefaultRepository.getRepository();

        config.getConfiguration().addManagedRepository( repoConfig );

        // Setup source repository (using legacy layout)
        repoLocation = new File( REPOPATH_LEGACY_MANAGED_TARGET );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( new File( REPOPATH_LEGACY_MANAGED ), repoLocation );

        managedLegacyRepository =
            createRepository( ID_LEGACY_MANAGED, "Legacy Managed Repository", REPOPATH_LEGACY_MANAGED_TARGET,
                              "legacy" );



        managedLegacyDir = new File( managedLegacyRepository.getRepoRoot() );

        repoConfig = managedLegacyRepository.getRepository();

        config.getConfiguration().addManagedRepository( repoConfig );

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED1, "Proxied Repository 1",
                                    new File( REPOPATH_PROXIED1 ).toURL().toExternalForm(), "default" );

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED2, "Proxied Repository 2",
                                    new File( REPOPATH_PROXIED2 ).toURL().toExternalForm(), "default" );

        // Setup target (proxied to) repository using legacy layout.
        saveRemoteRepositoryConfig( ID_LEGACY_PROXIED, "Proxied Legacy Repository",
                                    new File( REPOPATH_PROXIED_LEGACY ).toURL().toExternalForm(), "legacy" );

        // Setup the proxy handler.
        //proxyHandler = applicationContext.getBean (RepositoryProxyConnectors) lookup( RepositoryProxyConnectors.class.getName() );

        proxyHandler =
            applicationContext.getBean( "repositoryProxyConnectors#test", RepositoryProxyConnectors.class );

        // Setup the wagon mock.
        wagonMockControl = MockControl.createNiceControl( Wagon.class );
        wagonMock = (Wagon) wagonMockControl.getMock();

        delegate = (WagonDelegate) plexusSisuBridge.lookup( Wagon.class, "test" );

        delegate.setDelegate( wagonMock );

        log.info( "\n.\\ " + getName() + "() \\._________________________________________\n" );
    }

    @Override
    public String getName()
    {
        return StringUtils.substringAfterLast( getClass().getName(), "." );
    }

    protected static final ArgumentsMatcher customWagonGetIfNewerMatcher = new ArgumentsMatcher()
    {

        public boolean matches( Object[] expected, Object[] actual )
        {
            if ( expected.length < 1 || actual.length < 1 )
            {
                return false;
            }
            return MockControl.ARRAY_MATCHER.matches( ArrayUtils.remove( expected, 1 ),
                                                      ArrayUtils.remove( actual, 1 ) );
        }

        public String toString( Object[] arguments )
        {
            return ArrayUtils.toString( arguments );
        }
    };

    protected static final ArgumentsMatcher customWagonGetMatcher = new ArgumentsMatcher()
    {

        public boolean matches( Object[] expected, Object[] actual )
        {
            if ( expected.length == 2 && actual.length == 2 )
            {
                if ( expected[0] == null && actual[0] == null )
                {
                    return true;
                }

                if ( expected[0] == null )
                {
                    return actual[0] == null;
                }

                if ( actual[0] == null )
                {
                    return expected[0] == null;
                }

                return expected[0].equals( actual[0] );
            }
            return false;
        }

        public String toString( Object[] arguments )
        {
            return ArrayUtils.toString( arguments );
        }
    };


    protected void assertChecksums( File expectedFile, String expectedSha1Contents, String expectedMd5Contents )
        throws Exception
    {
        File sha1File = new File( expectedFile.getAbsolutePath() + ".sha1" );
        File md5File = new File( expectedFile.getAbsolutePath() + ".md5" );

        if ( expectedSha1Contents == null )
        {
            assertFalse( "SHA1 File should NOT exist: " + sha1File.getPath(), sha1File.exists() );
        }
        else
        {
            assertTrue( "SHA1 File should exist: " + sha1File.getPath(), sha1File.exists() );
            String actualSha1Contents = readChecksumFile( sha1File );
            assertEquals( "SHA1 File contents: " + sha1File.getPath(), expectedSha1Contents, actualSha1Contents );
        }

        if ( expectedMd5Contents == null )
        {
            assertFalse( "MD5 File should NOT exist: " + md5File.getPath(), md5File.exists() );
        }
        else
        {
            assertTrue( "MD5 File should exist: " + md5File.getPath(), md5File.exists() );
            String actualMd5Contents = readChecksumFile( md5File );
            assertEquals( "MD5 File contents: " + md5File.getPath(), expectedMd5Contents, actualMd5Contents );
        }
    }

    protected void assertFileEquals( File expectedFile, File actualFile, File sourceFile )
        throws Exception
    {
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", actualFile );

        assertTrue( "Check actual file exists.", actualFile.exists() );
        assertEquals( "Check filename path is appropriate.", expectedFile.getCanonicalPath(),
                      actualFile.getCanonicalPath() );
        assertEquals( "Check file path matches.", expectedFile.getAbsolutePath(), actualFile.getAbsolutePath() );

        String expectedContents = FileUtils.readFileToString( sourceFile, null );
        String actualContents = FileUtils.readFileToString( actualFile, null );
        assertEquals( "Check file contents.", expectedContents, actualContents );
    }

    protected void assertNotDownloaded( File downloadedFile )
    {
        assertNull( "Found file: " + downloadedFile + "; but was expecting a failure", downloadedFile );
    }

    @SuppressWarnings( "unchecked" )
    protected void assertNoTempFiles( File expectedFile )
    {
        File workingDir = expectedFile.getParentFile();
        if ( ( workingDir == null ) || !workingDir.isDirectory() )
        {
            return;
        }

        Collection<File> tmpFiles = FileUtils.listFiles( workingDir, new String[]{ "tmp" }, false );
        if ( !tmpFiles.isEmpty() )
        {
            StringBuffer emsg = new StringBuffer();
            emsg.append( "Found Temp Files in dir: " ).append( workingDir.getPath() );
            for ( File tfile : tmpFiles )
            {
                emsg.append( "\n   " ).append( tfile.getName() );
            }
            fail( emsg.toString() );
        }
    }

    /**
     * A faster recursive copy that omits .svn directories.
     *
     * @param sourceDirectory the source directory to copy
     * @param destDirectory   the target location
     * @throws java.io.IOException if there is a copying problem
     * @todo get back into plexus-utils, share with converter module
     */
    protected void copyDirectoryStructure( File sourceDirectory, File destDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFile( file, new File( destination, file.getName() ), false );
                // TODO: Change when there is a FileUtils.copyFileToDirectory(file, destination, boolean) option
                //FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !".svn".equals( file.getName() ) )
                {
                    if ( !destination.exists() && !destination.mkdirs() )
                    {
                        throw new IOException(
                            "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
                    }

                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    protected ManagedRepositoryContent createManagedLegacyRepository()
        throws Exception
    {
        return createRepository( "testManagedLegacyRepo", "Test Managed (Legacy) Repository",
                                 "src/test/repositories/legacy-managed", "legacy" );
    }

    protected ManagedRepositoryContent createProxiedLegacyRepository()
        throws Exception
    {
        return createRepository( "testProxiedLegacyRepo", "Test Proxied (Legacy) Repository",
                                 "src/test/repositories/legacy-proxied", "legacy" );
    }

    protected ManagedRepositoryContent createRepository( String id, String name, String path, String layout )
        throws Exception
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( path );
        repo.setLayout( layout );

        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#" + layout, ManagedRepositoryContent.class );
        repoContent.setRepository( repo );
        return repoContent;
    }

    /**
     * Read the first line from the checksum file, and return it (trimmed).
     */
    protected String readChecksumFile( File checksumFile )
        throws Exception
    {
        FileReader freader = null;
        BufferedReader buf = null;

        try
        {
            freader = new FileReader( checksumFile );
            buf = new BufferedReader( freader );
            return buf.readLine();
        }
        finally
        {
            if ( buf != null )
            {
                buf.close();
            }

            if ( freader != null )
            {
                freader.close();
            }
        }
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS, SnapshotsPolicy.ALWAYS,
                       CachedFailuresPolicy.NO, disabled );
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, String checksumPolicy, String releasePolicy,
                                  String snapshotPolicy, String cacheFailuresPolicy, boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, checksumPolicy, releasePolicy, snapshotPolicy, cacheFailuresPolicy,
                       PropagateErrorsDownloadPolicy.QUEUE, disabled );
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, String checksumPolicy, String releasePolicy,
                                  String snapshotPolicy, String cacheFailuresPolicy, String errorPolicy,
                                  boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, checksumPolicy, releasePolicy, snapshotPolicy, cacheFailuresPolicy,
                       errorPolicy, PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT, disabled );
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, String checksumPolicy, String releasePolicy,
                                  String snapshotPolicy, String cacheFailuresPolicy, String errorPolicy,
                                  String errorOnUpdatePolicy, boolean disabled )
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setSourceRepoId( sourceRepoId );
        connectorConfig.setTargetRepoId( targetRepoId );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, checksumPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, releasePolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, snapshotPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, cacheFailuresPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS, errorPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS_ON_UPDATE, errorOnUpdatePolicy );
        connectorConfig.setDisabled( disabled );

        int count = config.getConfiguration().getProxyConnectors().size();
        config.getConfiguration().addProxyConnector( connectorConfig );

        // Proper Triggering ...
        String prefix = "proxyConnectors.proxyConnector(" + count + ")";
        config.triggerChange( prefix + ".sourceRepoId", connectorConfig.getSourceRepoId() );
        config.triggerChange( prefix + ".targetRepoId", connectorConfig.getTargetRepoId() );
        config.triggerChange( prefix + ".proxyId", connectorConfig.getProxyId() );
        config.triggerChange( prefix + ".policies.releases", connectorConfig.getPolicy( "releases", "" ) );
        config.triggerChange( prefix + ".policies.checksum", connectorConfig.getPolicy( "checksum", "" ) );
        config.triggerChange( prefix + ".policies.snapshots", connectorConfig.getPolicy( "snapshots", "" ) );
        config.triggerChange( prefix + ".policies.cache-failures", connectorConfig.getPolicy( "cache-failures", "" ) );
        config.triggerChange( prefix + ".policies.propagate-errors",
                              connectorConfig.getPolicy( "propagate-errors", "" ) );
        config.triggerChange( prefix + ".policies.propagate-errors-on-update",
                              connectorConfig.getPolicy( "propagate-errors-on-update", "" ) );
    }

    protected void saveManagedRepositoryConfig( String id, String name, String path, String layout )
    {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );

        repoConfig.setLocation( path );

        int count = config.getConfiguration().getManagedRepositories().size();
        config.getConfiguration().addManagedRepository( repoConfig );

        String prefix = "managedRepositories.managedRepository(" + count + ")";
        config.triggerChange( prefix + ".id", repoConfig.getId() );
        config.triggerChange( prefix + ".name", repoConfig.getName() );
        config.triggerChange( prefix + ".location", repoConfig.getLocation() );
        config.triggerChange( prefix + ".layout", repoConfig.getLayout() );
    }

    protected void saveRemoteRepositoryConfig( String id, String name, String url, String layout )
    {
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );
        repoConfig.setUrl( url );

        int count = config.getConfiguration().getRemoteRepositories().size();
        config.getConfiguration().addRemoteRepository( repoConfig );

        String prefix = "remoteRepositories.remoteRepository(" + count + ")";
        config.triggerChange( prefix + ".id", repoConfig.getId() );
        config.triggerChange( prefix + ".name", repoConfig.getName() );
        config.triggerChange( prefix + ".url", repoConfig.getUrl() );
        config.triggerChange( prefix + ".layout", repoConfig.getLayout() );
    }

    protected File saveTargetedRepositoryConfig( String id, String originalPath, String targetPath, String layout )
        throws IOException
    {
        File repoLocation = new File( targetPath );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( new File( originalPath ), repoLocation );

        saveRemoteRepositoryConfig( id, "Target Repo-" + id, targetPath, layout );

        return repoLocation;
    }


    /**
     * Copy the specified resource directory from the src/test/repository/managed/ to
     * the testable directory under target/test-repository/managed/${testName}/
     *
     * @param resourcePath
     * @throws IOException
     */
    protected void setupTestableManagedRepository( String resourcePath )
        throws IOException
    {
        String resourceDir = resourcePath;

        if ( !resourcePath.endsWith( "/" ) )
        {
            int idx = resourcePath.lastIndexOf( '/' );
            resourceDir = resourcePath.substring( 0, idx );
        }

        File sourceRepoDir = new File( REPOPATH_DEFAULT_MANAGED );
        File sourceDir = new File( sourceRepoDir, resourceDir );

        File destRepoDir = managedDefaultDir;
        File destDir = new File( destRepoDir, resourceDir );

        // Cleanout destination dirs.
        if ( destDir.exists() )
        {
            FileUtils.deleteDirectory( destDir );
        }

        // Make the destination dir.
        destDir.mkdirs();

        // Test the source dir.
        if ( !sourceDir.exists() )
        {
            // This is just a warning.
            System.err.println(
                "[WARN] Skipping setup of testable managed repository, source dir does not exist: " + sourceDir );
        }
        else
        {

            // Test that the source is a dir.
            if ( !sourceDir.isDirectory() )
            {
                fail( "Unable to setup testable managed repository, source is not a directory: " + sourceDir );
            }

            // Copy directory structure.
            copyDirectoryStructure( sourceDir, destDir );
        }
    }

    protected void setManagedNewerThanRemote( File managedFile, File remoteFile )
    {
        setManagedNewerThanRemote(managedFile, remoteFile, 55000);
    }

    protected void setManagedNewerThanRemote( File managedFile, File remoteFile, long time )
    {
        assertTrue( "Managed File should exist: ", managedFile.exists() );
        assertTrue( "Remote File should exist: ", remoteFile.exists() );

        managedFile.setLastModified( remoteFile.lastModified() + time );

        assertTrue( managedFile.lastModified() > remoteFile.lastModified() );
    }

    protected void setManagedOlderThanRemote( File managedFile, File remoteFile )
    {
        setManagedOlderThanRemote(managedFile, remoteFile, 55000 );
    }

    protected void setManagedOlderThanRemote( File managedFile, File remoteFile, long time )
    {
        assertTrue( "Managed File should exist: ", managedFile.exists() );
        assertTrue( "Remote File should exist: ", remoteFile.exists() );

        managedFile.setLastModified( remoteFile.lastModified() - time );

        assertTrue( managedFile.lastModified() < remoteFile.lastModified() );

    }

    protected void assertNotModified( File file, long expectedModificationTime )
    {
        assertEquals( "File <" + file.getAbsolutePath() + "> not have been modified.", expectedModificationTime,
                      file.lastModified() );
    }

    protected void assertNotExistsInManagedLegacyRepo( File file )
        throws Exception
    {
        String managedLegacyPath = managedLegacyDir.getCanonicalPath();
        String testFile = file.getCanonicalPath();

        assertTrue(
            "Unit Test Failure: File <" + testFile + "> should be have been defined within the legacy managed path of <"
                + managedLegacyPath + ">", testFile.startsWith( managedLegacyPath ) );

        assertFalse( "File < " + testFile + "> should not exist in managed legacy repository.", file.exists() );
    }

    protected void assertNotExistsInManagedDefaultRepo( File file )
        throws Exception
    {
        String managedDefaultPath = managedDefaultDir.getCanonicalPath();
        String testFile = file.getCanonicalPath();

        assertTrue( "Unit Test Failure: File <" + testFile
                        + "> should be have been defined within the managed default path of <" + managedDefaultPath
                        + ">", testFile.startsWith( managedDefaultPath ) );

        assertFalse( "File < " + testFile + "> should not exist in managed default repository.", file.exists() );
    }

    protected static Date getFutureDate()
        throws ParseException
    {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, 1 );
        return cal.getTime();
    }

    protected static Date getPastDate()
        throws ParseException
    {
        return new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2000-01-01" );
    }
}
