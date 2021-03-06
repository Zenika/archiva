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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.cudf.admin.api.CUDFJobsAdmin;
import org.apache.archiva.cudf.admin.bean.CUDFJob;
import org.apache.archiva.cudf.extractor.CUDFEngine;
import org.apache.archiva.cudf.extractor.CUDFFiles;
import org.apache.archiva.cudf.extractor.CUDFPdfGenerator;
import org.apache.archiva.cudf.report.CUDFReportGenerator;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutionException;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CUDFService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.cudf.ArchivaCUDFTaskExecutor;
import org.apache.archiva.scheduler.cudf.CUDFTask;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
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

    @Inject()
    @Named( "scheduler#cudf" )
    private ArchivaTaskScheduler<CUDFTask> cudfTaskArchivaTaskScheduler;

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;

    @Inject
    private CUDFFiles cudfFiles;

    @Inject
    private CUDFPdfGenerator cudfPdfGenerator;

    @Inject
    private CUDFReportGenerator cudfReportGenerator;

    public void getConeCUDF( String groupId, String artifactId, String version, String type, String repositoryId,
                             HttpServletResponse servletResponse )
        throws ArchivaRestServiceException, MetadataResolutionException
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
        throws ArchivaRestServiceException, MetadataResolutionException
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

    @Override
    public Response getConeCUDFPdf( String groupId, String artifactId, String version, String type, String repositoryId,
                                    boolean keep )
        throws IOException, ArchivaRestServiceException
    {
        StringWriter writer = new StringWriter(  );
        computeCUDFCone( groupId, artifactId, version, type, repositoryId, writer );
        String fileName = "extractCUDF_" + groupId + "-" + artifactId + "-" + version;
        File output = File.createTempFile( fileName, ".pdf" );
        if ( !keep )
        {
            output.deleteOnExit();
        }
        cudfPdfGenerator.generateCUDFPdf( new StringReader( writer.toString() ), output );
        return Response.ok( output, "application/pdf" ).header( "Content-Disposition",
                                                                                 "attachment; filename=" + fileName
                                                                                     + ".pdf" ).build();
    }

    private void computeCUDFCone( String groupId, String artifactId, String version, String type, String repositoryId,
                                  Writer writer )
        throws IOException, ArchivaRestServiceException
    {
        try
        {
            if ( repositoryId == null || repositoryId.isEmpty() )
            {
                if ( version.contains( "SNAPSHOT" ) )
                {
                    cudfEngine.computeCUDFCone( groupId, artifactId, version, type, getSnapshotRepository(), writer );
                }
                else
                {
                    cudfEngine.computeCUDFCone( groupId, artifactId, version, type, getReleaseRepository(), writer );
                }
            }
            else
            {
                cudfEngine.computeCUDFCone( groupId, artifactId, version, type, repositoryId, getObservableRepos(),
                                            writer );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( "Invalid Metadata, check the artifact metadata", e );
        }
    }

    private List<String> getSnapshotRepository()
        throws ArchivaRestServiceException
    {
        try
        {
            List<ManagedRepository> managedRepositories = userRepositories.getAccessibleRepositories( getPrincipal() );
            List<String> repositoryIds = new ArrayList<String>();
            for ( ManagedRepository managedRepository : managedRepositories )
            {
                if ( managedRepository.isSnapshots() )
                {
                    repositoryIds.add( managedRepository.getId() );
                }
            }
            return repositoryIds;
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( "Security exception", e );
        }
    }

    private List<String> getReleaseRepository()
        throws ArchivaRestServiceException
    {
        try
        {
            List<ManagedRepository> managedRepositories = userRepositories.getAccessibleRepositories( getPrincipal() );
            List<String> repositoryIds = new ArrayList<String>();
            for ( ManagedRepository managedRepository : managedRepositories )
            {
                if ( managedRepository.isReleases() )
                {
                    repositoryIds.add( managedRepository.getId() );
                }
            }
            return repositoryIds;
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( "Security exception", e );
        }
    }

    public void getUniverseCUDF( String repositoryId, HttpServletResponse servletResponse )
        throws ArchivaRestServiceException
    {
        Writer output = null;
        try
        {
            output = servletResponse.getWriter();
            cudfEngine.computeCUDFUniverse( getSelectedRepos( repositoryId ), output, null );
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
                cudfEngine.computeCUDFUniverse( getSelectedRepos( repositoryId ), fos, null );
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

    public Boolean startCUDFJob( String id )
        throws ArchivaRestServiceException
    {
        CUDFJob cudfJob = cudfJobsAdmin.getCUDFJob( id );
        CUDFTask cudfTask = new CUDFTask();
        cudfTask.setId( cudfJob.getId() );
        cudfTask.setResourceDestination( new File( cudfJob.getLocation() ) );
        cudfTask.setDebug( cudfJob.isDebug() );
        cudfTask.setRetentionCount( cudfJob.getRetentionCount() );
        if ( cudfJob.isAllRepositories() )
        {
            cudfTask.setAllRepositories( true );
        }
        else
        {
            try
            {
                RepositoryGroup repositoryGroup =
                    repositoryGroupAdmin.getRepositoryGroup( cudfJob.getRepositoryGroup() );
                cudfTask.setRepositoriesId( repositoryGroup.getRepositories() );
            }
            catch ( RepositoryAdminException e )
            {
                throw new ArchivaRestServiceException( e.getMessage(), e );
            }
        }
        try
        {
            cudfTaskArchivaTaskScheduler.queueTask( cudfTask );
        }
        catch ( TaskQueueException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        return true;
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

    public void updateCUDFJob( String id, CUDFJob cudfJob )
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

    public void addCUDFJob( CUDFJob cudfJob )
        throws ArchivaRestServiceException
    {
        try
        {
            cudfJobsAdmin.addCUDFJob( cudfJob );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void deleteCUDFJob( CUDFJob cudfJob )
        throws ArchivaRestServiceException
    {
        try
        {
            cudfJobsAdmin.deleteCUDFJob( cudfJob );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void deleteCUDFJob( String id )
        throws ArchivaRestServiceException
    {
        try
        {
            cudfJobsAdmin.deleteCUDFJob( getCUDFJob( id ) );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<String> getCudfFiles( String jobId )
        throws ArchivaRestServiceException
    {

        return cudfFiles.getCudfFiles( jobId );
    }

    @Override
    public Response getCudfFile( String jobId, String fileName )
        throws ArchivaRestServiceException
    {
        if ( FilenameUtils.isExtension( fileName, "cudf" ) )
        {
            File file = cudfFiles.getCudfFile( jobId, fileName );
            return Response.ok( file, "application/cudf" ).header( "content-disposition",
                                                                   "attachment; filename = " + file.getName() ).build();
        }
        else if ( FilenameUtils.isExtension( fileName, "pdf" ) )
        {
            File cudf = cudfFiles.getCudfFile( jobId, FilenameUtils.getBaseName( fileName ) + ".cudf" );
            File pdf = cudfPdfGenerator.generateCUDFPdf( cudf );
            return Response.ok( pdf, "application/pdf" ).header( "content-disposition",
                                                                 "attachment; filename = " + pdf.getName() ).build();
        }
        else if ( FilenameUtils.isExtension( fileName, "rep" ) )
        {
            File cudf = cudfFiles.getCudfFile( jobId, FilenameUtils.getBaseName( fileName ) );
            File report = cudfReportGenerator.generateReport( cudf );
            return Response.ok( report, "application/rep" ).header( "content-disposition", "attachment; filename = "
                + report.getName() ).build();
        }
        else
        {
            return Response.noContent().build();
        }
    }

    @Override
    protected String getSelectedRepoExceptionMessage()
    {
        return "cudf.root.group.repository.denied";
    }

    private List<String> getSelectedRepos( String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getObservableRepos();

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return Collections.emptyList();
        }

        if ( StringUtils.isNotEmpty( repositoryId ) )
        {
            // check user has karma on the repository
            if ( !selectedRepos.contains( repositoryId ) )
            {
                throw new ArchivaRestServiceException( "browse.root.groups.repositoy.denied",
                                                       Response.Status.FORBIDDEN.getStatusCode(), null );
            }
            selectedRepos = Collections.singletonList( repositoryId );
        }
        return selectedRepos;
    }
}
