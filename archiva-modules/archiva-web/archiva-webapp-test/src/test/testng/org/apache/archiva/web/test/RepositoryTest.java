package org.apache.archiva.web.test;

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

import org.apache.archiva.web.test.parent.AbstractRepositoryTest;
import org.testng.annotations.Test;

@Test( groups = { "repository" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class RepositoryTest
    extends AbstractRepositoryTest
{
    @Test
    public void testAddManagedRepoValidValues()
    {
        goToRepositoriesPage();
        getSelenium().open( "/archiva/admin/addRepository.action" );
        addManagedRepository( "managedrepo1", "Managed Repository Sample 1", getRepositoryDir() + "repository/", "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "", true );
        clickButtonWithValue( "Save" );
        assertTextPresent( "Managed Repository Sample 1" );
        assertRepositoriesPage();
    }

    @Test( dependsOnMethods = { "testAddManagedRepoValidValues" } )
    public void testAddManagedRepoInvalidValues()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "<> \\/~+[ ]'\"", "<>\\~+[]'\"", "<> ~+[ ]'\"", "<> ~+[ ]'\"", "Maven 2.x Repository", "",
                              "-1", "101", false );
        assertTextPresent(
            "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
        assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
        assertTextPresent( "Cron expression is required." );
    }

    @Test
    public void testAddManagedRepoInvalidIdentifier()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "<> \\/~+[ ]'\"", "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1",
                              "1", false );
        assertTextPresent(
            "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
    }

    @Test
    public void testAddManagedRepoInvalidRepoName()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "<>\\~+[]'\"", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?",
                              "1", "1", false );
        assertTextPresent(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
    }

    @Test
    public void testAddManagedRepoInvalidDirectory()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "<> ~+[ ]'\"", "/.index", "Maven 2.x Repository", "0 0 * * * ?",
                              "1", "1", false );
        assertTextPresent(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
    }

    @Test
    public void testAddManagedRepoInvalidIndexDir()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "/home", "<> ~+[ ]'\"", "Maven 2.x Repository", "0 0 * * * ?", "1",
                              "1", false );
        assertTextPresent(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
    }

    @Test
    public void testAddManagedRepoInvalidRetentionCount()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1",
                              "101", false );
        assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
    }

    @Test
    public void testAddManagedRepoInvalidDaysOlder()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "-1",
                              "1", false );
        assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
    }

    @Test
    public void testAddManagedRepoBlankValues()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "", "", "", "", "Maven 2.x Repository", "", "", "", false );
        assertTextPresent( "You must enter a repository identifier." );
        assertTextPresent( "You must enter a repository name." );
        assertTextPresent( "You must enter a directory." );
        assertTextPresent( "Cron expression is required." );
    }

    @Test
    public void testAddManagedRepoNoIdentifier()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "", "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "", false );
        assertTextPresent( "You must enter a repository identifier." );
    }

    @Test
    public void testAddManagedRepoNoRepoName()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "",
                              false );
        assertTextPresent( "You must enter a repository name." );
    }

    @Test
    public void testAddManagedRepoNoDirectory()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "",
                              false );
        assertTextPresent( "You must enter a directory." );
    }

    @Test
    public void testAddManagedRepoNoCron()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );

        addManagedRepository( "identifier", "name", "/home", "/.index", "Maven 2.x Repository", "", "", "", false );

        assertTextPresent( "Cron expression is required." );
    }

    @Test
    public void testAddManagedRepoForEdit()
    {
        getSelenium().open( "/archiva/admin/addRepository.action" );
        addManagedRepository( "managedrepo", "Managed Repository Sample", getRepositoryDir() + "local-repo/", "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "", true );
        clickButtonWithValue( "Save" );
        assertTextPresent( "Managed Repository Sample" );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidValues()
    {
        editManagedRepository( "<>\\~+[]'\"", "<> ~+[ ]'\"", "<> ~+[ ]'\"", "Maven 2.x Repository", "", "-1", "101" );
        assertTextPresent(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
        assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
        assertTextPresent( "Cron expression is required." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidRepoName()
    {
        editManagedRepository( "<>\\~+[]'\"", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
        assertTextPresent(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidDirectory()
    {
        editManagedRepository( "name", "<> ~+[ ]'\"", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
        assertTextPresent(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidIndexDir()
    {
        editManagedRepository( "name", "/home", "<> ~+[ ]'\"", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
        assertTextPresent(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidCronBadText()
    {
        editManagedRepository( "name", "/home", "/.index", "Maven 2.x Repository", "asdf", "1", "1" );
        assertTextPresent( "Invalid cron expression." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidCronBadValue()
    {
        editManagedRepository( "name", "/home", "/.index", "Maven 2.x Repository", "60 0 * * * ?", "1", "1" );
        assertTextPresent( "Invalid cron expression." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidCronTooManyElements()
    {
        editManagedRepository( "name", "/home", "/.index", "Maven 2.x Repository", "* * * * * * * *", "1", "1" );
        assertTextPresent( "Invalid cron expression." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidRetentionCount()
    {
        editManagedRepository( "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "101" );
        assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
    }

    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepoInvalidDaysOlder()
    {
        editManagedRepository( "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "-1", "1" );
        assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
    }

    // TODO
    @Test( dependsOnMethods = { "testAddManagedRepoForEdit" } )
    public void testEditManagedRepo()
    {
        editManagedRepository( "repository.name", "Managed Repo" );
        assertTextPresent( "Managed Repository Sample" );
    }

    // TODO
    @Test( dependsOnMethods = { "testEditManagedRepo" }, enabled = false )
    public void testDeleteManageRepo()
    {
        deleteManagedRepository();
        // assertTextNotPresent( "managedrepo" );
    }

    @Test( dependsOnMethods = { "testAddRemoteRepoValidValues" } )
    public void testAddRemoteRepoNullValues()
    {
        getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
        addRemoteRepository( "", "", "", "", "", "", "Maven 2.x Repository", false );
        assertTextPresent( "You must enter a repository identifier." );
        assertTextPresent( "You must enter a repository name." );
        assertTextPresent( "You must enter a url." );
    }

    @Test
    public void testAddRemoteRepositoryNullIdentifier()
    {
        getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
        addRemoteRepository( "", "Remote Repository Sample", "http://repository.codehaus.org/org/codehaus/mojo/", "",
                             "", "", "Maven 2.x Repository", false );
        assertTextPresent( "You must enter a repository identifier." );
    }

    @Test( dependsOnMethods = { "testAddRemoteRepositoryNullIdentifier" } )
    public void testAddRemoteRepoNullName()
    {
        addRemoteRepository( "remoterepo", "", "http://repository.codehaus.org/org/codehaus/mojo/", "", "", "",
                             "Maven 2.x Repository", false );
        assertTextPresent( "You must enter a repository name." );
    }

    @Test
    public void testAddRemoteRepoNullURL()
    {
        getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
        addRemoteRepository( "remoterepo", "Remote Repository Sample", "", "", "", "", "Maven 2.x Repository", false );
        assertTextPresent( "You must enter a url." );
    }

    @Test( dependsOnMethods = { "testAddRemoteRepoNullURL" } )
    public void testAddProxyConnectorValidValues()
        throws Exception
    {
        getSelenium().open( "/archiva/admin/addProxyConnector.action" );
        addProxyConnector( "(direct connection)", "internal", "remoterepo" );
        assertTextPresent( "remoterepo" );
        assertTextPresent( "Remote Repository Sample" );
    }

    @Test
    public void testAddRemoteRepoValidValues()
    {
        getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
        addRemoteRepository( "remoterepo", "Remote Repository Sample",
                             "http://repository.codehaus.org/org/codehaus/mojo/", "", "", "", "Maven 2.x Repository",
                             true );
        assertTextPresent( "Remote Repository Sample" );
    }

    // *** BUNDLED REPOSITORY TEST ***

    @Test( dependsOnMethods = { "testWithCorrectUsernamePassword" }, alwaysRun = true )
    public void testBundledRepository()
    {
        String repo1 = baseUrl + "repository/internal/";
        String repo2 = baseUrl + "repository/snapshots/";

        assertRepositoryAccess( repo1 );
        assertRepositoryAccess( repo2 );

        getSelenium().open( "/archiva" );
    }

    private void assertRepositoryAccess( String repo )
    {
        getSelenium().open( "/archiva" );
        goToRepositoriesPage();
        assertLinkPresent( repo );
        clickLinkWithText( repo );
        assertPage( "Collection: /" );
        assertTextPresent( "Collection: /" );
    }
}
