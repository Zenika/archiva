package org.apache.archiva.web.action.admin.repositories;

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

import org.apache.commons.lang.StringUtils;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.List;

/**
 * SortRepositoriesAction
 * FIXME remove access to archivaconfiguration
 */
@Controller( "sortRepositoriesAction" )
@Scope( "prototype" )
public class SortRepositoriesAction
    extends AbstractRepositoriesAdminAction
{

    private String repoGroupId;

    private String targetRepo;

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    public String sortDown()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        List<String> repositories = getRepositoriesFromGroup();

        int idx = findTargetRepository( repositories, targetRepo );

        if ( idx >= 0 && validIndex( repositories, idx + 1 ) )
        {
            repositories.remove( idx );
            repositories.add( idx + 1, targetRepo );
        }

        return saveConfiguration( config );
    }

    public String sortUp()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        List<String> repositories = getRepositoriesFromGroup();

        int idx = findTargetRepository( repositories, targetRepo );

        if ( idx >= 0 && validIndex( repositories, idx - 1 ) )
        {
            repositories.remove( idx );
            repositories.add( idx - 1, targetRepo );
        }

        return saveConfiguration( config );
    }

/**
     * Save the configuration.
     *
     * @param configuration the configuration to save.
     * @return the webwork result code to issue.
     * @throws java.io.IOException                   thrown if unable to save file to disk.
     * @throws org.apache.archiva.configuration.InvalidConfigurationException thrown if configuration is invalid.
     * @throws org.apache.archiva.redback.components.registry.RegistryException             thrown if configuration subsystem has a problem saving the configuration to disk.
     */
    protected String saveConfiguration( Configuration configuration )
    {
        try
        {
            archivaConfiguration.save( configuration );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }
        catch ( org.apache.archiva.redback.components.registry.RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }

    public String getRepoGroupId()
    {
        return repoGroupId;
    }

    public void setRepoGroupId( String repoGroupId )
    {
        this.repoGroupId = repoGroupId;
    }

    public String getTargetRepo()
    {
        return targetRepo;
    }

    public void setTargetRepo( String targetRepo )
    {
        this.targetRepo = targetRepo;
    }

    private int findTargetRepository( List<String> repositories, String targetRepository )
    {
        int idx = ( -1 );

        for ( int i = 0; i < repositories.size(); i++ )
        {
            if ( StringUtils.equals( targetRepository, repositories.get( i ) ) )
            {
                idx = i;
                break;
            }
        }
        return idx;
    }

    private List<String> getRepositoriesFromGroup()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryGroupConfiguration repoGroup = config.findRepositoryGroupById( repoGroupId );
        return repoGroup.getRepositories();
    }

    private boolean validIndex( List<String> repositories, int idx )
    {
        return ( idx >= 0 ) && ( idx < repositories.size() );
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
