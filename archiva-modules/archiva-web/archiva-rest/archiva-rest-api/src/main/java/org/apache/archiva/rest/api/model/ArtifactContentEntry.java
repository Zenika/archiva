package org.apache.archiva.rest.api.model;
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

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "artifactContentEntry" )
public class ArtifactContentEntry
{
    private String name;

    private boolean file;

    private int depth;

    public ArtifactContentEntry()
    {
        // no op
    }

    public ArtifactContentEntry( String name, boolean file, int depth )
    {
        this.name = name;
        this.file = file;
        this.depth = depth;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public boolean isFile()
    {
        return file;
    }

    public void setFile( boolean file )
    {
        this.file = file;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth( int depth )
    {
        this.depth = depth;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ArtifactContentEntry" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", file=" ).append( file );
        sb.append( ", depth=" ).append( depth );
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ArtifactContentEntry ) )
        {
            return false;
        }

        ArtifactContentEntry that = (ArtifactContentEntry) o;

        if ( depth != that.depth )
        {
            return false;
        }
        if ( file != that.file )
        {
            return false;
        }
        if ( !name.equals( that.name ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + ( file ? 1 : 0 );
        result = 31 * result + depth;
        return result;
    }
}
