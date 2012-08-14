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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
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

            String fileName = null;
            List<String> repositoriesId = null;
            if ( cudfTask.isAllRepositories() )
            {
                repositoriesId = getAllRepositories();
                fileName = "universe-" + generateDateForFilename( "yyyyMMdd-HHmmss-SS" ) + ".cudf";
            }
            else
            {
                repositoriesId = cudfTask.getRepositoriesId();
                fileName = generateFileName( cudfTask.getRepositoriesId() );
            }
            cudfEngine.computeCUDFUniverse( repositoriesId,
                                            new FileWriter( new File( cudfTask.getResourceDestination(), fileName ) ) );
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
        log.info( "Finished CUDF Task" );
    }

    private String generateFileName( List<String> repositoriesId )
    {
        StringBuilder sb = new StringBuilder( 10 );
        Iterator it = repositoriesId.iterator();
        while ( it.hasNext() )
        {
            sb.append( it.next() );
            if ( it.hasNext() )
            {
                sb.append( "-" );
            }
            else
            {
                sb.append( "-" ).append( generateDateForFilename( "yyyyMMdd-HHmmss-SS" ) ).append( ".cudf" );
            }
        }
        return sb.toString();
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
