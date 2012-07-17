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

import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.rest.api.services.CUDFService;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 */
public class CUDFServiceTest extends AbstractArchivaRestTest{

    @Test
    @Ignore
    public void cudfConeTest() throws Exception
    {
//        String testRepoId = "test-repo";
//        createAndIndexRepo(testRepoId, new File(getBasedir(), "src/test/repo-with-osgi").getAbsolutePath());
//        CUDFService cudfService = getCUDFService(authorizationHeader);
//        String result = cudfService.getConeCUDF("commons-cli", "commons-cli", "1.0", "jar", testRepoId);
//        assertTrue(result.contains("commons-cli%3acommons-cli"));
//        assertTrue(result.contains("type: jar"));
//        assertTrue(result.contains("number: 1.0"));
//        assertEquals(2, numberOfOccurrences(result, "commons-logging%3acommons-logging"));
//        assertEquals(2, numberOfOccurrences(result, "commons-lang%3acommons-lang"));
    }

    @Test
    @Ignore
    public void cudfUniverseTest() throws Exception
    {
//        String testRepoId = "test-repo";
//        createAndIndexRepo(testRepoId, new File(getBasedir(), "src/test/repo-with-osgi").getAbsolutePath());
//        CUDFService cudfService = getCUDFService(authorizationHeader);
//        String result = cudfService.getUniverseCUDF(testRepoId).toString();
//        assertTrue(result.contains("commons-cli%3acommons-cli"));
//        assertEquals(7, numberOfOccurrences(result, "commons-logging%3acommons-logging"));
    }

    @Test
    @Ignore
    public void cudfUniverseWithDependenciesInOtherRepository() throws Exception
    {
//        String testRepoId1 = "test-repo-1";
//        String testRepoId2 = "test-repo-2";
//
//        createAndIndexRepo(testRepoId1, new File(getBasedir(), "src/test/repo-cudf-1").getAbsolutePath());
//        createAndIndexRepo(testRepoId2, new File(getBasedir(), "src/test/repo-cudf-2").getAbsolutePath());
//
//        CUDFService cudfService = getCUDFService(authorizationHeader);
//        String result = cudfService.getConeCUDF("commons-cli", "commons-cli", "1.0", "jar", testRepoId1);
//        assertTrue(result.contains("commons-cli%3acommons-cli"));
//        assertTrue(result.contains("type: jar"));
//        assertTrue(result.contains("number: 1.0"));
//        assertEquals(2, numberOfOccurrences(result, "commons-logging%3acommons-logging"));
//        assertEquals(2, numberOfOccurrences(result, "commons-lang%3acommons-lang"));
    }

    private int numberOfOccurrences(String stringToCompute, String regex)
    {
        Matcher matcher = Pattern.compile(regex).matcher(stringToCompute);
        int occurrences = 0;
        while (matcher.find())
        {
            occurrences++;
        }
        return occurrences;
    }
}
