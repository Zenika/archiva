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
import com.zenika.cudf.parser.DefaultDeserializer;
import com.zenika.cudf.parser.ParsingException;
import com.zenika.cudf.resolver.Resolver;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
    private CUDFBinaryCheck binaryCheck; 
    
    @Inject
    private CUDFUniverse universe;

    @Inject
    private Resolver resolver;

    public void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                 List<String> repositories, Writer writer )
        throws IOException
    {
        new CUDFExtractor( writer, null ).computeCUDFCone( groupId, artifactId, version, type, repositoryId, repositories,
                                                     repositorySessionFactory );
    }

    public void computeCUDFCone( String groupId, String artifactId, String version, String type,
                                 List<String> repositories, Writer writer )
        throws IOException
    {
        new CUDFExtractor( writer, null ).computeCUDFCone( groupId, artifactId, version, type, repositories,
                                                     repositorySessionFactory );
    }

    public void computeCUDFUniverse( List<String> repositoryIds, Writer writer, Writer debugWriter )
        throws IOException
    {
        if ( !universe.isLoaded() )
        {
            universe.loadUniverse( repositoryIds );
        }
        CUDFDescriptor descriptor = universe.getDescriptor();
        DefaultSerializer serializer = new DefaultSerializer( writer );
        try
        {
            serializer.serialize( descriptor );
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to serialize the CUDF document", e );
        }
    }

    public String resolve( String request )

    {
        try
        {
            DefaultDeserializer deserializer = new DefaultDeserializer( new StringReader( request ) );
            CUDFDescriptor requestCUDF = deserializer.deserialize();
            binaryCheck.checkPackagesAreInUniverse( requestCUDF.getBinaries().getAllBinaries() );
            binaryCheck.checkRequestedPackagesAreInUniverse( requestCUDF.getRequest() );
            CUDFDescriptor responseCUDF = resolver.resolve( requestCUDF );
            StringWriter stringWriter = new StringWriter();
            DefaultSerializer serializer = new DefaultSerializer( stringWriter );
            serializer.serialize( responseCUDF );
            return stringWriter.toString();
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to parse CUDF" );
        }
    }

    public void resolve( InputStream request, Writer response )
    {
        try
        {
            DefaultDeserializer deserializer = new DefaultDeserializer( new InputStreamReader( request, "UTF-8" ) );
            CUDFDescriptor requestCUDF = deserializer.deserialize();
            binaryCheck.checkPackagesAreInUniverse( requestCUDF.getBinaries().getAllBinaries() );
            binaryCheck.checkRequestedPackagesAreInUniverse( requestCUDF.getRequest() );
            CUDFDescriptor responseCUDF = resolver.resolve( requestCUDF );
            DefaultSerializer serializer = new DefaultSerializer( response );
            serializer.serialize( responseCUDF );
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to parse CUDF" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "Bad encoding" );
        }
    }
    
    
}
