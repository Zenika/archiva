package org.apache.archiva.admin.repository.runtime;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.admin.model.beans.CacheConfiguration;
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("archivaRuntimeConfigurationAdmin#default")
public class DefaultArchivaRuntimeConfigurationAdmin
    implements ArchivaRuntimeConfigurationAdmin
{

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "cache#url-failures-cache" )
    private Cache urlFailureCache;

    @PostConstruct
    public void initialize()
        throws RepositoryAdminException
    {
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration = getArchivaRuntimeConfiguration();

        boolean save = false;

        // NPE free
        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration() == null )
        {
            archivaRuntimeConfiguration.setUrlFailureCacheConfiguration( new CacheConfiguration() );
        }

        // if -1 it means non initialized to take values from the spring bean
        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToIdleSeconds() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setTimeToIdleSeconds(
                urlFailureCache.getTimeToIdleSeconds() );
            save = true;

        }
        urlFailureCache.setTimeToIdleSeconds(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToIdleSeconds() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToLiveSeconds() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setTimeToLiveSeconds(
                urlFailureCache.getTimeToLiveSeconds() );
            save = true;

        }
        urlFailureCache.setTimeToLiveSeconds(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToLiveSeconds() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsInMemory() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setMaxElementsInMemory(
                urlFailureCache.getMaxElementsInMemory() );
            save = true;
        }
        urlFailureCache.setMaxElementsInMemory(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsInMemory() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsOnDisk() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setMaxElementsOnDisk(
                urlFailureCache.getMaxElementsOnDisk() );
            save = true;
        }
        urlFailureCache.setMaxElementsOnDisk(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsOnDisk() );

        if ( save )
        {
            updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );
        }

    }

    public ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()
        throws RepositoryAdminException
    {
        return build( archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration() );
    }

    public void updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws RepositoryAdminException
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        configuration.setArchivaRuntimeConfiguration( build( archivaRuntimeConfiguration ) );
        try
        {
            archivaConfiguration.save( configuration );
        }
        catch ( RegistryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    protected ArchivaRuntimeConfiguration build(
        org.apache.archiva.configuration.ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        if ( archivaRuntimeConfiguration == null )
        {
            return new ArchivaRuntimeConfiguration();
        }

        ArchivaRuntimeConfiguration res =
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration, ArchivaRuntimeConfiguration.class );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration() != null )
        {

            res.setUrlFailureCacheConfiguration(
                new BeanReplicator().replicateBean( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration(),
                                                    CacheConfiguration.class ) );

        }

        return res;
    }

    protected org.apache.archiva.configuration.ArchivaRuntimeConfiguration build(
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        if ( archivaRuntimeConfiguration == null )
        {
            return new org.apache.archiva.configuration.ArchivaRuntimeConfiguration();
        }

        org.apache.archiva.configuration.ArchivaRuntimeConfiguration res =
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration,
                                                org.apache.archiva.configuration.ArchivaRuntimeConfiguration.class );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration() != null )
        {

            res.setUrlFailureCacheConfiguration(
                new BeanReplicator().replicateBean( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration(),
                                                    org.apache.archiva.configuration.CacheConfiguration.class ) );

        }

        return res;
    }
}


