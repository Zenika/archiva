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

import org.apache.archiva.redback.components.taskqueue.Task;

import java.io.File;
import java.util.List;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
public class CUDFTask
    implements Task
{

    private String id;

    private boolean allRepositories;

    private List<String> repositoriesId;

    private File resourceDestination;

    private boolean debug;

    public CUDFTask()
    {
        this.allRepositories = false;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public File getResourceDestination()
    {
        return resourceDestination;
    }

    public void setResourceDestination( File resourceDestination )
    {
        this.resourceDestination = resourceDestination;
    }

    public boolean isAllRepositories()
    {
        return allRepositories;
    }

    public void setAllRepositories( boolean allRepositories )
    {
        this.allRepositories = allRepositories;
    }

    public List<String> getRepositoriesId()
    {
        return repositoriesId;
    }

    public void setRepositoriesId( List<String> repositoriesId )
    {
        this.repositoriesId = repositoriesId;
    }

    public long getMaxExecutionTime()
    {
        return 0;
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
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof CUDFTask ) )
        {
            return false;
        }

        CUDFTask task = (CUDFTask) o;

        if ( allRepositories != task.allRepositories )
        {
            return false;
        }
        if ( id != null ? !id.equals( task.id ) : task.id != null )
        {
            return false;
        }
        if ( repositoriesId != null ? !repositoriesId.equals( task.repositoriesId ) : task.repositoriesId != null )
        {
            return false;
        }
        if ( resourceDestination != null
            ? !resourceDestination.equals( task.resourceDestination )
            : task.resourceDestination != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + ( allRepositories ? 1 : 0 );
        result = 31 * result + ( repositoriesId != null ? repositoriesId.hashCode() : 0 );
        result = 31 * result + ( resourceDestination != null ? resourceDestination.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CUDFTask" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", allRepositories=" ).append( allRepositories );
        sb.append( ", repositoriesId=" ).append( repositoriesId );
        sb.append( ", resourceDestination=" ).append( resourceDestination );
        sb.append( '}' );
        return sb.toString();
    }
}
