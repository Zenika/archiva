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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.cudf.admin.api.CUDFJobsAdmin;
import org.apache.archiva.cudf.admin.bean.CUDFJob;
import org.apache.archiva.cudf.extractor.CUDFEngine;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutionException;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CUDFService;
import org.apache.archiva.scheduler.cudf.ArchivaCUDFTaskExecutor;
import org.apache.archiva.scheduler.cudf.CUDFTask;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

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

    @Inject
    private CUDFJobsAdmin cudfJobsAdmin;

    @Inject
    @Named( value = "taskExecutor#cudf" )
    private ArchivaCUDFTaskExecutor archivaCUDFTaskExecutor;

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
        if ( repositoryId == null || repositoryId.isEmpty() )
        {
            cudfEngine.computeCUDFCone( groupId, artifactId, version, type, getObservableRepos(), writer );
        }
        else
        {
            cudfEngine.computeCUDFCone( groupId, artifactId, version, type, repositoryId, getObservableRepos(),
                                        writer );
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

    public Response startCudfTaskGeneration( String filePath )
        throws ArchivaRestServiceException
    {
        if ( filePath == null || filePath.isEmpty() )
        {
            throw new IllegalArgumentException( "The file path for the generation is required" );
        }
        try
        {
            CUDFTask task = new CUDFTask();
            task.setResourceDestination( new File( filePath ) );
            archivaCUDFTaskExecutor.executeTask( task );
            return Response.ok().build();
        }
        catch ( TaskExecutionException e )
        {
            throw new ArchivaRestServiceException( "Unable to start CUDF generation.", e );
        }
    }

    public List<CUDFJob> getCUDFJobs()
        throws ArchivaRestServiceException
    {
        return cudfJobsAdmin.getCUDFJobs();
    }

    public CUDFJob getCUDFJob( String id )
        throws ArchivaRestServiceException
    {
        return cudfJobsAdmin.getCUDFJob( id );
    }

    public void updateCUDFScheduler( String id, CUDFJob cudfJob )
        throws ArchivaRestServiceException
    {
        try
        {
            if ( cudfJob.getId() == null || cudfJob.getId().isEmpty() )
            {
                cudfJob.setId( id );
            }
            cudfJobsAdmin.updateCUDFJobs( cudfJob );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }


    @Override
    protected String getSelectedRepoExceptionMessage()
    {
        return "cudf.root.group.repository.denied";
    }

}
