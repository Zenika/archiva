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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.cudf.CUDFEngine;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CUDFService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{

    @Inject
    private CUDFEngine cudfEngine;


    public void getConeCUDF( String groupId, String artifactId, String version, String type, String repositoryId,
                             HttpServletResponse servletResponse )
        throws ArchivaRestServiceException
    {

        Writer output = null;
        try
        {
            output = servletResponse.getWriter();
            computeCUDFCone( groupId, artifactId, version, type, repositoryId, output );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            if ( output != null )
            {
                try
                {
                    output.close();
                }
                catch ( IOException e )
                {
                    //NOTHING
                }
            }
        }
    }

    public Response getConeCUDFFile( String groupId, String artifactId, String version, String type,
                                     String repositoryId, boolean keep )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
            File output = File.createTempFile( fileName, ".txt" );
            if ( !keep )
            {
                output.deleteOnExit();
            }
            FileWriter fos = null;
            try
            {
                fos = new FileWriter( output );
                computeCUDFCone( groupId, artifactId, version, type, repositoryId, fos );
            }
            finally
            {
                if ( fos != null )
                {
                    try
                    {
                        fos.close();
                    }
                    catch ( IOException e )
                    {
                        //NOTHING
                    }
                }
            }
            return Response.ok( output, MediaType.APPLICATION_OCTET_STREAM ).header( "Content-Disposition",
                                                                                     "attachment; filename=" + fileName
                                                                                         + ".txt" ).build();
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( "Unable to extract CUDF", e );
        }
    }

    private void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                  Writer writer )
        throws IOException
    {
        if (repositoryId == null || repositoryId.isEmpty()) {
            cudfEngine.computeCUDFCone( groupId, artifactId, version, type, getObservableRepos(), writer);
        } else {
            cudfEngine.computeCUDFCone( groupId, artifactId, version, type, repositoryId, getObservableRepos(), writer );
        }
    }

    public void getUniverseCUDF( String repositoryId, HttpServletResponse servletResponse )
        throws ArchivaRestServiceException
    {
        Writer output = null;
        try
        {
            output = servletResponse.getWriter();
            cudfEngine.computeCUDFUniverse( getSelectedRepos( repositoryId ), output );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            if ( output != null )
            {
                try
                {
                    output.close();
                }
                catch ( IOException e )
                {
                    //NOTHING
                }
            }
        }
    }

    public Response getUniverseCUDFFile( String repositoryId, boolean keep )
        throws ArchivaRestServiceException
    {
        try
        {
            String fileName = "extractCUDF_universe";
            File output = File.createTempFile( fileName, ".txt" );
            if ( !keep )
            {
                output.deleteOnExit();
            }
            Writer fos = null;
            try
            {
                fos = new FileWriter( output );
                cudfEngine.computeCUDFUniverse( getSelectedRepos( repositoryId ), fos );
            }
            finally
            {
                if ( fos != null )
                {
                    try
                    {
                        fos.close();
                    }
                    catch ( IOException e )
                    {
                        //NOTHING
                    }
                }
            }
            return Response.ok( output, MediaType.APPLICATION_OCTET_STREAM ).header( "Content-Disposition",
                                                                                     "attachment; filename=" + fileName
                                                                                         + ".txt" ).build();
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( "Unable to extract CUDF", e );
        }
    }

    public String backgroundUniverse( final String repositoryId, String output, HttpServletResponse response )
        throws ArchivaRestServiceException
    {
        final String outputFilename = output == null ? "/tmp/universe.cudf" : output;
        new Thread()
        {
            @Override
            public void run()
            {
                File output = new File( outputFilename );
                FileWriter fos = null;
                try
                {
                    fos = new FileWriter( output );
                    log.info( "Starting CUDF Extract in background" );
                    cudfEngine.computeCUDFUniverse( getSelectedRepos( repositoryId ), fos );
                    log.info(
                        "Background CUDF extraction of Universe is done. Find the output file " + outputFilename );
                }
                catch ( ArchivaRestServiceException e )
                {
                    log.error( "Error while background cudf extraction" );
                    log.error( e.getMessage(), e );
                }
                catch ( IOException e )
                {
                    log.error( "Error while background cudf extraction" );
                    log.error( e.getMessage(), e );
                }
                finally
                {
                    if ( fos != null )
                    {
                        try
                        {
                            fos.close();
                        }
                        catch ( IOException e )
                        {
                            //nothing to do
                        }
                    }
                    log.info( "CUDF Extraction ended." );
                }
            }
        }.start();
        return "Started in background";
    }


    @Override
    protected String getSelectedRepoExceptionMessage()
    {
        return "cudf.root.group.repository.denied";
    }

}
