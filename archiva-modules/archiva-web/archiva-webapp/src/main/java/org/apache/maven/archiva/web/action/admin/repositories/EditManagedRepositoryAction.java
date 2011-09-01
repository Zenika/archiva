package org.apache.maven.archiva.web.action.admin.repositories;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.redback.components.scheduler.CronExpressionValidator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.io.File;

/**
 * AddManagedRepositoryAction
 *
 * @version $Id$
 */
@Controller( "editManagedRepositoryAction" )
@Scope( "prototype" )
public class EditManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * FIXME we must manipulate beans from repo admin api
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    private ManagedRepositoryConfiguration stagingRepository;

    private String repoid;

    private final String action = "editRepository";

    private boolean stageNeeded;


    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
            stagingRepository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid + "-stage" );
        }
        else if ( repository != null )
        {
            repository.setReleases( false );
            repository.setScanned( false );
        }
    }

    public String input()
    {
        if ( repository == null )
        {
            addActionError( "Edit failure, unable to edit a repository with a blank repository id." );
            return ERROR;
        }

        return INPUT;
    }

    public String confirmUpdate()
    {
        // location was changed
        return save( true );
    }

    public String commit()
    {
        ManagedRepositoryConfiguration existingConfig =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repository.getId() );
        boolean resetStats = false;

        // check if the location was changed
        repository.setLocation( removeExpressions( repository.getLocation() ) );

        if ( !StringUtils.equalsIgnoreCase( existingConfig.getLocation().trim(), repository.getLocation().trim() ) )
        {
            resetStats = true;

            File dir = new File( repository.getLocation() );
            if ( dir.exists() )
            {
                return CONFIRM;
            }
        }

        return save( resetStats );
    }

    private String save( boolean resetStats )
    {

        String result = SUCCESS;
        try
        {
            ManagedRepository managedRepository =
                new ManagedRepository( repository.getId(), repository.getName(), repository.getLocation(),
                                       repository.getLayout(), repository.isSnapshots(), repository.isReleases(),
                                       repository.isBlockRedeployments(), repository.getRefreshCronExpression() );

            getManagedRepositoryAdmin().updateManagedRepository( managedRepository, stageNeeded, getAuditInformation(),
                                                                 resetStats );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "Repository Administration Exception: " + e.getMessage() );
            result = ERROR;
        }

        return result;
    }


    @Override
    public void validate()
    {
        CronExpressionValidator validator = new CronExpressionValidator();

        if ( !validator.validate( repository.getRefreshCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
        }

        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( repository.getId() ) )
        {
            repository.setId( repository.getId().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getName() ) )
        {
            repository.setName( repository.getName().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getLocation() ) )
        {
            repository.setLocation( repository.getLocation().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getIndexDir() ) )
        {
            repository.setIndexDir( repository.getIndexDir().trim() );
        }
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public boolean isStageNeeded()
    {
        return stageNeeded;
    }

    public void setStageNeeded( boolean stageNeeded )
    {

        this.stageNeeded = stageNeeded;
    }

    public String getAction()
    {
        return action;
    }

    public ManagedRepositoryConfiguration getStagingRepository()
    {
        return stagingRepository;
    }

    public void setStagingRepository( ManagedRepositoryConfiguration stagingRepository )
    {
        this.stagingRepository = stagingRepository;
    }
}
