package org.apache.archiva.consumers.core;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AutoRemoveConsumer
 *
 *
 */
@Service( "knownRepositoryContentConsumer#auto-remove" )
@Scope( "prototype" )
public class AutoRemoveConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    /**
     * default-value="auto-remove"
     */
    private String id = "auto-remove";

    /**
     * default-value="Automatically Remove File from Filesystem."
     */
    private String description = "Automatically Remove File from Filesystem.";

    /**
     *
     */
    @Inject
    private ArchivaConfiguration configuration;

    /**
     *
     */
    @Inject
    private FileTypes filetypes;

    private File repositoryDir;

    private List<String> includes = new ArrayList<String>( 0 );

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = new File( repository.getLocation() );
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File file = new File( this.repositoryDir, path );
        if ( file.exists() )
        {
            triggerConsumerInfo( "(Auto) Removing File: " + file.getAbsolutePath() );
            file.delete();
        }
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes = new ArrayList<String>( filetypes.getFileTypePatterns( FileTypes.AUTO_REMOVE ) );
    }

    @PostConstruct
    public void initialize()
    {
        configuration.addChangeListener( this );

        initIncludes();
    }
}
