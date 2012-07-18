package org.apache.archiva.cudf;

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

import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
@Service( "cudfEngine#org.apache.archiva.cudf.cudf" )
public class DefaultCUDFEngine
    implements CUDFEngine
{

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    public void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                 Writer writer )
        throws IOException
    {
        new CUDFExtractor( writer ).computeCUDFCone( groupId, artifactId, version, type, repositoryId,
                                                     repositorySessionFactory );
    }

    public void computeCUDFUniverse( List<String> repositoryIds, Writer writer )
        throws IOException
    {
        new CUDFExtractor( writer ).computeCUDFUniverse( repositoryIds, repositorySessionFactory );
    }
}
