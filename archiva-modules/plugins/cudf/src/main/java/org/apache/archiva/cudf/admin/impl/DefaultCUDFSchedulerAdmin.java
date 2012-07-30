package org.apache.archiva.cudf.admin.impl;

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
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.CUDFConfiguration;
import org.apache.archiva.configuration.CUDFJobConfiguration;
import org.apache.archiva.cudf.admin.api.CUDFSchedulerAdmin;
import org.apache.archiva.cudf.admin.bean.CUDFJob;
import org.apache.archiva.redback.components.scheduler.CronExpressionValidator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "cudfSchedulerAdmin#default" )
public class DefaultCUDFSchedulerAdmin
    extends AbstractRepositoryAdmin
    implements CUDFSchedulerAdmin
{

    @Inject
    private CronExpressionValidator cronExpressionValidator;

    public List<CUDFJob> getCUDFJobs()
    {
        List<CUDFJob> cudfJobs = new ArrayList<CUDFJob>();
        List<CUDFJobConfiguration> cudfJobConfigurations =
            getArchivaConfiguration().getConfiguration().getCudf().getCudfJobs();
        for ( CUDFJobConfiguration cudfJobConfiguration : cudfJobConfigurations )
        {
            cudfJobs.add( new CUDFJob( cudfJobConfiguration.getId(), cudfJobConfiguration.getLocation(),
                                       cudfJobConfiguration.getCronExpression(),
                                       cudfJobConfiguration.isAllRepositories(),
                                       cudfJobConfiguration.getRepositoryGroup() ) );
        }
        return cudfJobs;
    }

    public void addCUDFJob( CUDFJob cudfJob )
        throws RepositoryAdminException
    {
        CUDFConfiguration cudfConfiguration = getArchivaConfiguration().getConfiguration().getCudf();

        checkIsNotExist( cudfJob, cudfConfiguration );
        validateCUDFJob( cudfJob );

        cudfConfiguration.addCudfJob( createCUDFJobConfiguration( cudfJob ) );
        saveConfiguration( getArchivaConfiguration().getConfiguration() );
    }


    public void updateCUDFJobs( CUDFJob cudfJob )
        throws RepositoryAdminException
    {
        CUDFConfiguration cudfConfiguration = getArchivaConfiguration().getConfiguration().getCudf();
        CUDFJobConfiguration cudfJobConfiguration = cudfConfiguration.findCUDFJobById( cudfJob.getId() );

        if ( cudfConfiguration != null )
        {
            cudfConfiguration.removeCudfJob( cudfJobConfiguration );
        }

        cudfConfiguration.addCudfJob( createCUDFJobConfiguration( cudfJob ) );
        saveConfiguration( getArchivaConfiguration().getConfiguration() );
    }

    private CUDFJobConfiguration createCUDFJobConfiguration( CUDFJob cudfJob )
    {
        CUDFJobConfiguration cudfJobConfiguration = new CUDFJobConfiguration();
        cudfJobConfiguration.setId( cudfJob.getId() );
        cudfJobConfiguration.setLocation( cudfJob.getLocation() );
        cudfJobConfiguration.setAllRepositories( cudfJob.isAllRepositories() );
        cudfJobConfiguration.setRepositoryGroup( cudfJob.getRepositoryGroup() );
        return cudfJobConfiguration;
    }

    private void validateCUDFJob( CUDFJob cudfJob)
        throws RepositoryAdminException
    {
        if ( !cronExpressionValidator.validate( cudfJob.getCronExpression() ) )
        {
            throw new RepositoryAdminException( "The cron expression of " + cudfJob.getId() + " is invalid" );
        }
    }

    private void checkIsNotExist( CUDFJob cudfJob, CUDFConfiguration cudfConfiguration )
        throws RepositoryAdminException
    {
        if ( cudfConfiguration.findCUDFJobById( cudfJob.getId() ) != null )
        {
            throw new RepositoryAdminException( "The CUDF job with " + cudfJob.getId() + "id is already" );
        }
    }
}
