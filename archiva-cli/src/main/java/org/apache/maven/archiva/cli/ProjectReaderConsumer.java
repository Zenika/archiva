package org.apache.maven.archiva.cli;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;

/**
 * ProjectReaderConsumer 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *                   role-hint="read-poms"
 *                   instantiation-strategy="per-lookup"
 */
public class ProjectReaderConsumer
    extends AbstractProgressConsumer
    implements KnownRepositoryContentConsumer
{
    /**
     * @plexus.configuration default-value="read-poms"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Read POMs and report anomolies."
     */
    private String description;

    private ProjectModelReader reader;

    private ManagedRepositoryConfiguration repo;

    private List includes;

    public ProjectReaderConsumer()
    {
        reader = new ProjectModel400Reader();

        includes = new ArrayList();
        includes.add( "**/*.pom" );
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return includes;
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        super.beginScan( repository, whenGathered );
        this.repo = repository;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        super.processFile( path );

        File pomFile = new File( repo.getLocation(), path );
        try
        {
            ArchivaProjectModel model = reader.read( pomFile );
            if ( model == null )
            {
                System.err.println( "Got null model on " + pomFile );
            }
        }
        catch ( ProjectModelException e )
        {
            System.err.println( "Unable to process: " + pomFile );
            e.printStackTrace( System.out );
        }
    }
}
