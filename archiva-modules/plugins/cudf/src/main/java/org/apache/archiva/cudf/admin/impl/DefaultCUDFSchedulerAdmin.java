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
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.CUDFConfiguration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.cudf.admin.api.CUDFSchedulerAdmin;
import org.apache.archiva.cudf.admin.bean.CUDFScheduler;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@Service( "cudfSchedulerAdmin#default" )
public class DefaultCUDFSchedulerAdmin
    extends AbstractRepositoryAdmin
    implements CUDFSchedulerAdmin
{
    public CUDFScheduler getCUDFScheduler()
    {
        CUDFConfiguration cudfConfiguration = getArchivaConfiguration().getConfiguration().getCudf();
        return new CUDFScheduler( cudfConfiguration.getLocation(), cudfConfiguration.getCronExpression(),
                                  cudfConfiguration.isAllRepositories(), cudfConfiguration.getRepositoryGroup() );
    }

    public void updateCUDFScheduler( CUDFScheduler cudfScheduler )
        throws RepositoryAdminException
    {
        CUDFConfiguration cudfConfiguration = new CUDFConfiguration();
        cudfConfiguration.setLocation( cudfScheduler.getLocation() );
        cudfConfiguration.setCronExpression( cudfScheduler.getCronExpression() );
        cudfConfiguration.setAllRepositories( cudfScheduler.isAllRepositories() );
        cudfConfiguration.setRepositoryGroup( cudfScheduler.getRepositoryGroup() );
        getArchivaConfiguration().getConfiguration().setCudf( cudfConfiguration );
        saveConfiguration( getArchivaConfiguration().getConfiguration() );
    }
}
