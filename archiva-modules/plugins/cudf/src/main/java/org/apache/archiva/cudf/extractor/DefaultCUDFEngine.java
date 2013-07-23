package org.apache.archiva.cudf.extractor;

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

import com.zenika.cudf.model.CUDFDescriptor;
import com.zenika.cudf.parser.DefaultSerializer;
import com.zenika.cudf.parser.ParsingException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "cudfEngine#default" )
public class DefaultCUDFEngine
    implements CUDFEngine
{

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private CUDFUniverseLoader universeLoader;

    public void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                 List<String> repositories, Writer writer )
        throws IOException, MetadataResolutionException
    {
        new CUDFExtractor( writer, null ).computeCUDFCone( groupId, artifactId, version, type, repositoryId,
                                                           repositories, repositorySessionFactory );
    }

    public void computeCUDFCone( String groupId, String artifactId, String version, String type,
                                 List<String> repositories, Writer writer )
        throws IOException, MetadataResolutionException
    {
        new CUDFExtractor( writer, null ).computeCUDFCone( groupId, artifactId, version, type, repositories,
                                                           repositorySessionFactory );
    }

    public void computeCUDFUniverse( List<String> repositoryIds, Writer writer, Writer debugWriter )
        throws IOException
    {
        if ( !universeLoader.isLoaded() )
        {
            universeLoader.loadUniverse( repositoryIds );
        }
        CUDFDescriptor descriptor = universeLoader.getDescriptor();
        DefaultSerializer serializer = new DefaultSerializer( writer, debugWriter );
        try
        {
            serializer.serialize( descriptor );
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to serialize the CUDF document", e );
        }
        finally
        {
            if ( writer != null )
            {
                writer.close();
            }
            if ( debugWriter != null )
            {
                debugWriter.close();
            }
        }
    }
}
