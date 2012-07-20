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

import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.redback.components.taskqueue.TaskQueue;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "archivaTaskScheduler#cudf" )
public class CUDFArchivaTaskScheduler
    implements ArchivaTaskScheduler<CUDFTask>
{

    @Inject
    @Named("taskQueue#cudf")
    private TaskQueue taskQueue;


    public void queueTask( CUDFTask task )
        throws TaskQueueException
    {
        taskQueue.put( task );
    }
}
