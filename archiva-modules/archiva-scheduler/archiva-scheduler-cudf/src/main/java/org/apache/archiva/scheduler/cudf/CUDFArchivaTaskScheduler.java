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

import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.CUDFConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.redback.components.scheduler.CronExpressionValidator;
import org.apache.archiva.redback.components.scheduler.Scheduler;
import org.apache.archiva.redback.components.taskqueue.Task;
import org.apache.archiva.redback.components.taskqueue.TaskQueue;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.text.ParseException;
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
    implements ArchivaTaskScheduler<CUDFTask>, ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( CUDFArchivaTaskScheduler.class );

    static final String TASK_QUEUE = "TASK_QUEUE";

    static final String TASK_DESTINATION = "TASK_DESTINATION";

    private static final String CUDF_JOB = "cj";

    private static final String CUDF_GROUP = "cg";

    private static final String CUDF_JOB_TRIGGER = "cjt";

    public static final String CRON_HOURLY = "0 0 * * * ?";

    @Inject
    @Named( "taskQueue#cudf" )
    private TaskQueue taskQueue;

    @Inject
    private ArchivaConfiguration configuration;

    @Inject
    private CronExpressionValidator validator;

    @Inject
    private Scheduler scheduler;

    @PostConstruct
    public void startup()
        throws ArchivaException

    {
        configuration.addListener( this );
        try
        {
            scheduleCUDFJobs( configuration.getConfiguration().getCudf() );
        }
        catch ( SchedulerException e )
        {
            throw new ArchivaException( "Unable to start scheduler: " + e.getMessage(), e );
        }
    }

    public void queueTask( CUDFTask task )
        throws TaskQueueException
    {
        if ( isProcessingCUDFGeneration( task ) )
        {
            log.debug( "CUDF task '{}' is already queued. Skipping task.", task );
        }
        else
        {
            taskQueue.put( task );
        }
    }

    private boolean unQueueTask( CUDFTask task )
        throws TaskQueueException
    {
        if ( !isProcessingCUDFGeneration( task ) )
        {
            log.info( "cannot unqueue Repository task '{}' not already queued.", task );
            return false;
        }
        else
        {
            return taskQueue.remove( task );
        }
    }

    @SuppressWarnings( "unchecked" )
    private boolean isProcessingCUDFGeneration( Task task )
        throws TaskQueueException
    {
        synchronized ( taskQueue )
        {
            return taskQueue.getQueueSnapshot().contains( task );
        }
    }

    public void configurationEvent( ConfigurationEvent event )
    {
        configuration.addListener( this );
        try
        {
            scheduler.unscheduleJob( CUDF_JOB, CUDF_GROUP );
            scheduleCUDFJobs( configuration.getConfiguration().getCudf() );
        }
        catch ( SchedulerException e )
        {
            log.error( "Error restarting the CUDF job after property change." );
        }
    }

    private synchronized void scheduleCUDFJobs( CUDFConfiguration cudfConfiguration )
        throws SchedulerException
    {
        if ( cudfConfiguration.getCronExpression() == null )
        {
            log.warn( "Skipping job, no cron expression for " + cudfConfiguration.getCronExpression() );
            return;
        }

        String cronExpression = cudfConfiguration.getCronExpression();
        if ( !validator.validate( cronExpression ) )
        {
            log.warn( "Cron expression [" + cronExpression + "] is invalid.  Defaulting to hourly." );
            cronExpression = CRON_HOURLY;
        }

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( TASK_QUEUE, taskQueue );
        dataMap.put( TASK_DESTINATION, new File( cudfConfiguration.getLocation() ) );
        JobDetail cudfJob =
            JobBuilder.newJob( CUDFTaskJob.class ).withIdentity( CUDF_JOB, CUDF_GROUP ).usingJobData( dataMap ).build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity( CUDF_JOB_TRIGGER, CUDF_GROUP ).withSchedule(
            CronScheduleBuilder.cronSchedule( cronExpression ) ).build();
        scheduler.scheduleJob( cudfJob, trigger );
    }

    @PreDestroy
    public void stop()
        throws SchedulerException
    {
        scheduler.unscheduleJob( CUDF_JOB, CUDF_GROUP );
    }

    public Scheduler getScheduler()
    {
        return scheduler;
    }
}
