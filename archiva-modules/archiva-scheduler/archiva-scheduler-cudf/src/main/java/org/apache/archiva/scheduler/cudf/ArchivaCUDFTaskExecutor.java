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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        CUDFTask cudfTask = (CUDFTask) task;
        try
        {
            List<String> repositoriesId = null;
            if ( cudfTask.isAllRepositories() )
            {
                repositoriesId = getAllRepositories();
            } else {
                repositoriesId = cudfTask.getRepositoriesId();
            }
            cudfEngine.computeCUDFUniverse( repositoriesId, new FileWriter( cudfTask.getResourceDestination() ) );
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
}
