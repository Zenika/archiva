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

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
 */
public class CUDFTask
    implements Task
{

    private String repositoryId;

    private File resourceDestination;

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public File getResourceDestination()
    {
        return resourceDestination;
    }

    public void setResourceDestination( File resourceDestination )
    {
        this.resourceDestination = resourceDestination;
    }

    public long getMaxExecutionTime()
    {
        return 0;
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

        CUDFTask cudfTask = (CUDFTask) o;

        if ( repositoryId != null ? !repositoryId.equals( cudfTask.repositoryId ) : cudfTask.repositoryId != null )
        {
            return false;
        }
        if ( resourceDestination != null
            ? !resourceDestination.equals( cudfTask.resourceDestination )
            : cudfTask.resourceDestination != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = repositoryId != null ? repositoryId.hashCode() : 0;
        result = 31 * result + ( resourceDestination != null ? resourceDestination.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "CUDFTask{" +
            "repositoryId='" + repositoryId + '\'' +
            ", resourceDestination=" + resourceDestination +
            '}';
    }
}
