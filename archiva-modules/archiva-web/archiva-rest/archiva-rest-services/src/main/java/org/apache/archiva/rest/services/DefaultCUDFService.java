package org.apache.archiva.rest.services;

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

import org.apache.archiva.dependency.tree.maven2.DependencyTreeBuilder;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.TreeEntry;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CUDFService;
import org.apache.archiva.rest.services.utils.TreeDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Adrien Lecharpentier
 */
@Service( "cudfService#rest" )
public class DefaultCUDFService
    extends AbstractRestService
    implements CUDFService
{
    @Inject
    private DependencyTreeBuilder dependencyTreeBuilder;

    public CharSequence getConeCUDF( String groupId, String artifactId, String version )
        throws ArchivaRestServiceException
    {
        StringBuilder response = new StringBuilder();

        List<String> repositories = getObservableRepos();
        if ( repositories.isEmpty() )
        {
            return "Error";
        }
        List<TreeEntry> treeEntries = new ArrayList<TreeEntry>();
        TreeDependencyNodeVisitor treeDependencyNodeVisitor = new TreeDependencyNodeVisitor( treeEntries );
        try
        {
            dependencyTreeBuilder.buildDependencyTree( repositories, groupId, artifactId, version,
                                                       treeDependencyNodeVisitor );
            LinkedList<TreeEntry> entries = new LinkedList<TreeEntry>();
            Set<Artifact> knownArtifacts = new TreeSet<Artifact>( new Comparator<Artifact>()
            {
                public int compare( Artifact o1, Artifact o2 )
                {
                    int c = o1.getGroupId().compareTo( o2.getGroupId() );
                    if ( c != 0 )
                    {
                        return c;
                    }
                    c = o1.getArtifactId().compareTo( o2.getArtifactId() );
                    if ( c != 0 )
                    {
                        return c;
                    }
                    return o1.getVersion().compareTo( o2.getVersion() );
                }
            } );
            TreeEntry item;

            for ( TreeEntry treeEntry : treeEntries )
            {
                if ( !knownArtifacts.contains( treeEntry.getArtifact() ) )
                {
                    entries.add( treeEntry );
                    knownArtifacts.add( treeEntry.getArtifact() );
                }
            }
            while ( ( item = entries.poll() ) != null )
            {
                knownArtifacts.add( item.getArtifact() );
                response.append( convertMavenArtifactToCUDF( item.getArtifact() ) );
                if ( !item.getChilds().isEmpty() )
                {
                    response.append( convertTreeEntryChildToCUDF( item ) );
                    for ( TreeEntry treeEntry : item.getChilds() )
                    {
                        if ( !knownArtifacts.contains( treeEntry.getArtifact() ) )
                        {
                            entries.add( treeEntry );
                            knownArtifacts.add( treeEntry.getArtifact() );
                        }
                    }
                }
                response.append( "\n" );
            }
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        return response.toString();
    }

    public CharSequence getUniverseCUDF()
        throws ArchivaRestServiceException
    {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    private String convertMavenArtifactToCUDF( Artifact artifact )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Package: " ).append( convertMavenArtifactInline( artifact ) ).append( "\n" );
        sb.append( "Version: " ).append( artifact.getVersion() ).append( "\n" );
        return sb.toString();
    }

    private String convertMavenArtifactInline( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private String convertTreeEntryChildToCUDF( TreeEntry treeEntry )
    {
        StringBuilder response = new StringBuilder();
        response.append( "Depends: " );
        Iterator<TreeEntry> it = treeEntry.getChilds().iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = it.next().getArtifact();
            response.append( convertMavenArtifactInline( artifact ) ).append( " = " ).append( artifact.getVersion() );
            if ( it.hasNext() )
            {
                response.append( ", " );
            }
        }
        response.append( "\n" );

        return response.toString();
    }
}
