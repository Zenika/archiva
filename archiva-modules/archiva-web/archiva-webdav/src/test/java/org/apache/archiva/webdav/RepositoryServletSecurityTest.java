package org.apache.archiva.webdav;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.InvocationContext;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.repository.audit.TestAuditListener;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.memory.SimpleUser;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.integration.filter.authentication.basic.HttpBasicAuthentication;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;

/**
 * RepositoryServletSecurityTest Test the flow of the authentication and authorization checks. This does not necessarily
 * perform redback security checking.
 * 
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RepositoryServletSecurityTest
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected ServletUnitClient sc;

    protected File repoRootInternal;

    private ServletRunner sr;

    protected ArchivaConfiguration archivaConfiguration;

    private DavSessionProvider davSessionProvider;

    private MockControl servletAuthControl;

    private ServletAuthenticator servletAuth;

    private MockClassControl httpAuthControl;

    private HttpAuthenticator httpAuth;

    private RepositoryServlet servlet;

    @Inject
    ApplicationContext applicationContext;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        String appserverBase =  System.getProperty( "appserver.base", new File( "target/appserver-base" ).getAbsolutePath() );

        File testConf = new File( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf, testConfDest );

        repoRootInternal = new File( appserverBase, "data/repositories/internal" );

        archivaConfiguration = applicationContext.getBean( ArchivaConfiguration.class );
        Configuration config = archivaConfiguration.getConfiguration();

        if (!config.getManagedRepositoriesAsMap().containsKey( REPOID_INTERNAL ))
        {
            config.addManagedRepository( createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal ) );
        }
        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().clearAll();

        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        sr = new ServletRunner( new File( "src/test/resources/WEB-INF/repository-servlet-security-test/web.xml" ) );
        sr.registerServlet( "/repository/*", RepositoryServlet.class.getName() );
        sc = sr.newClient();

        servletAuthControl = MockControl.createControl( ServletAuthenticator.class );
        servletAuthControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        servletAuth = (ServletAuthenticator) servletAuthControl.getMock();

        httpAuthControl =
            MockClassControl.createControl( HttpBasicAuthentication.class, HttpBasicAuthentication.class.getMethods() );
        httpAuthControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        httpAuth = (HttpAuthenticator) httpAuthControl.getMock();

        davSessionProvider = new ArchivaDavSessionProvider( servletAuth, httpAuth );
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }

    protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
        FileUtils.deleteDirectory( repoRootDir );
        if ( !repoRootDir.exists() )
        {
            repoRootDir.mkdirs();
        }
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {
        if ( sc != null )
        {
            sc.clearContents();
        }

        if ( sr != null )
        {
            sr.shutDown();
        }

        if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }

        servlet = null;

        super.tearDown();
    }

    // test deploy with invalid user, and guest has no write access to repo
    // 401 must be returned
    @Test
    public void testPutWithInvalidUserAndGuestHasNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndThrow( servletAuth.isAuthenticated( null, null ),
                                           new AuthenticationException( "Authentication error" ) );

        servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
        servletAuthControl.setMatcher( MockControl.EQUALS_MATCHER );
        servletAuthControl.setThrowable( new UnauthorizedException( "'guest' has no write access to repository" ) );

        httpAuthControl.replay();
        servletAuthControl.replay();

        servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode());
    }

    // test deploy with invalid user, but guest has write access to repo
    @Test
    public void testPutWithInvalidUserAndGuestHasWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndThrow( servletAuth.isAuthenticated( null, null ),
                                           new AuthenticationException( "Authentication error" ) );

        servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
        servletAuthControl.setMatcher( MockControl.EQUALS_MATCHER );
        servletAuthControl.setReturnValue( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        servletAuthControl.expectAndThrow( servletAuth.isAuthenticated( null, result ),
                                           new AuthenticationException( "Authentication error" ) );

        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), null );

        // check if guest has write access
        servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
        servletAuthControl.setMatcher( MockControl.EQUALS_MATCHER );
        servletAuthControl.setReturnValue( true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals( HttpServletResponse.SC_CREATED, response.getResponseCode() );
    }

    // test deploy with a valid user with no write access
    @Test
    public void testPutWithValidUserWithNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, null ), true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), new SimpleUser() );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, result ), true );
        servletAuthControl.expectAndThrow(
                                           servletAuth.isAuthorized( null, session, "internal",
                                                                     ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ),
                                           new UnauthorizedException( "User not authorized" ) );

        httpAuthControl.replay();
        servletAuthControl.replay();

        servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode());
    }

    // test deploy with a valid user with write access
    @Test
    public void testPutWithValidUserWithWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
        assertTrue( repoRootInternal.exists() );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        TestAuditListener listener = new TestAuditListener();
        archivaDavResourceFactory.addAuditListener( listener );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, null ), true );

        User user = new SimpleUser();
        user.setUsername( "admin" );
        
        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), user );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, result ), true );
        servletAuthControl.expectAndReturn(
                                            servletAuth.isAuthorized( null, session, "internal",
                                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ),
                                            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals(HttpServletResponse.SC_CREATED, response.getResponseCode());

        assertEquals( "admin", listener.getEvents().get( 0 ).getUserId() );
    }

    // test get with invalid user, and guest has read access to repo
    @Test
    public void testGetWithInvalidUserAndGuestHasReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.forName( "UTF-8" )  );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndThrow( servletAuth.isAuthenticated( null, null ),
                                           new AuthenticationException( "Authentication error" ) );
        servletAuthControl.expectAndReturn(
                                            servletAuth.isAuthorized( "guest", "internal",
                                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ),
                                            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), null );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, result ), true );
        servletAuthControl.expectAndReturn(
                                            servletAuth.isAuthorized( null, session, "internal",
                                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ),
                                            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, response.getResponseCode() );
        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    // test get with invalid user, and guest has no read access to repo
    @Test
    public void testGetWithInvalidUserAndGuestHasNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.forName( "UTF-8" )  );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndThrow( servletAuth.isAuthenticated( null, null ),
                                           new AuthenticationException( "Authentication error" ) );
        servletAuthControl.expectAndReturn(
                                            servletAuth.isAuthorized( "guest", "internal",
                                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ),
                                            false );

        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode() );
    }

    // test get with valid user with read access to repo
    @Test
    public void testGetWithAValidUserWithReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.forName( "UTF-8" )  );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, null ), true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), new SimpleUser() );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, result ), true );
        servletAuthControl.expectAndReturn(
                                            servletAuth.isAuthorized( null, session, "internal",
                                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ),
                                            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, response.getResponseCode() );
        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    // test get with valid user with no read access to repo
    @Test
    public void testGetWithAValidUserWithNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.forName( "UTF-8" )  );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        InvocationContext ic = sc.newInvocation( request );
        servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, null ), true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();
        httpAuthControl.expectAndReturn( httpAuth.getAuthenticationResult( null, null ), result );
        httpAuthControl.expectAndReturn( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ), session );
        httpAuthControl.expectAndReturn( httpAuth.getSessionUser( ic.getRequest().getSession() ), new SimpleUser() );
        servletAuthControl.expectAndReturn( servletAuth.isAuthenticated( null, result ), true );
        servletAuthControl.expectAndThrow(
                                           servletAuth.isAuthorized( null, session, "internal",
                                                                     ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ),
                                           new UnauthorizedException( "User not authorized to read repository." ) );

        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode() );
    }
}
