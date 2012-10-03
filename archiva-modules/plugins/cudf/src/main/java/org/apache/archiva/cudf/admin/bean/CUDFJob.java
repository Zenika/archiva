package org.apache.archiva.cudf.admin.bean;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
@XmlRootElement(name = "cudfJob")
public class CUDFJob
    implements Serializable
{
    private String id;

    private String location;

    private String cronExpression;

    private boolean allRepositories;

    private String repositoryGroup;

    private boolean debug;

    public CUDFJob()
    {
    }

    public CUDFJob( String id, String location, String cronExpression, boolean allRepositories, String repositoryGroup,
                    boolean debug )
    {
        this.id = id;
        this.location = location;
        this.cronExpression = cronExpression;
        this.allRepositories = allRepositories;
        this.repositoryGroup = repositoryGroup;
        this.debug = debug;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public boolean isAllRepositories()
    {
        return allRepositories;
    }

    public void setAllRepositories( boolean allRepositories )
    {
        this.allRepositories = allRepositories;
    }

    public String getRepositoryGroup()
    {
        return repositoryGroup;
    }

    public void setRepositoryGroup( String repositoryGroup )
    {
        this.repositoryGroup = repositoryGroup;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CUDFJob" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", cronExpression='" ).append( cronExpression ).append( '\'' );
        sb.append( ", allRepositories=" ).append( allRepositories );
        sb.append( ", repositoryGroup='" ).append( repositoryGroup ).append( '\'' );
        sb.append( ", debug='" ).append( debug ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
