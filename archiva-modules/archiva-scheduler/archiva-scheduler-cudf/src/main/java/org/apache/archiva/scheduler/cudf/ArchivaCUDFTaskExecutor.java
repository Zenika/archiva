package org.apache.archiva.scheduler.cudf;

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
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.cudf.extractor.CUDFEngine;
import org.apache.archiva.redback.components.taskqueue.Task;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutionException;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutor;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "taskExecutor#cudf" )
public class ArchivaCUDFTaskExecutor
    implements TaskExecutor
{
    private Logger log = LoggerFactory.getLogger( ArchivaCUDFTaskExecutor.class );

    @Inject
    private CUDFEngine cudfEngine;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private SimpleDateFormat simpleDateFormat = null;

    private final Object simpleDateFormatMonitor = new Object();

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        CUDFTask cudfTask = (CUDFTask) task;
        try
        {
            if ( !cudfTask.getResourceDestination().exists() )
            {
                cudfTask.getResourceDestination().mkdirs();
            }
            else
            {
                if ( !cudfTask.getResourceDestination().isDirectory() )
                {
                    log.warn( "cudf output configuration is not a folder" );
                    cudfTask.getResourceDestination().renameTo(
                        new File( cudfTask.getResourceDestination().getAbsolutePath() + "-old" ) );
                    cudfTask.getResourceDestination().mkdir();
                }
            }

            String fileName = cudfTask.getId() + "-" + generateDateForFilename( "yyyyMMdd-HHmmss-SS" ) + ".cudf";
            List<String> repositoriesId = null;
            if ( cudfTask.isAllRepositories() )
            {
                repositoriesId = getAllRepositories();
            }
            else
            {
                repositoriesId = cudfTask.getRepositoriesId();
            }
            FileWriter debugWriter = null;
            if ( cudfTask.isDebug() )
            {
                debugWriter = new FileWriter( new File( cudfTask.getResourceDestination(), fileName + ".debug" ) );
            }
            cudfEngine.computeCUDFUniverse( repositoriesId,
                                            new FileWriter( new File( cudfTask.getResourceDestination(), fileName ) ),
                                            debugWriter );

            deleteOldFile( cudfTask.getResourceDestination(), cudfTask.getRetentionCount(), "cudf" );
            deleteOldFile( cudfTask.getResourceDestination(), cudfTask.getRetentionCount(), "pdf" );

            log.info( "Finished CUDF Task. Saved in " + cudfTask.getResourceDestination().getAbsolutePath() +
                          File.separator + fileName );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new TaskExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new TaskExecutionException( e.getMessage(), e );
        }
    }

    private void deleteOldFile( File resourceDestination, int retentionCount, String extension )
    {
        List<File> cudfFiles = new ArrayList<File>( FileUtils.listFiles( resourceDestination, new String[]{ extension }, false ));
        if (cudfFiles.size() > retentionCount )
        {
            Collections.sort( cudfFiles , new ReverseComparator( new NameFileComparator() ));
            for (int i = retentionCount; i < cudfFiles.size(); i++) {
                FileUtils.deleteQuietly( cudfFiles.get( i ) );
            }
        }
    }

    private List<String> getAllRepositories()
        throws RepositoryAdminException
    {
        List<String> repositoriesId;
        List<ManagedRepository> repositories = managedRepositoryAdmin.getManagedRepositories();

        repositoriesId = new ArrayList<String>();
        for ( ManagedRepository managedRepository : repositories )
        {
            repositoriesId.add( managedRepository.getId() );
        }
        return repositoriesId;
    }

    public String generateDateForFilename( String format )
    {
        synchronized ( simpleDateFormatMonitor )
        {
            simpleDateFormat = new SimpleDateFormat( format );
            return simpleDateFormat.format( Calendar.getInstance().getTime() );
        }
    }
}
