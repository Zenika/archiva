package org.apache.maven.archiva.repository.layout;

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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * BidirectionalRepositoryLayout - Similar in scope to ArtifactRepositoryLayout, but does
 * the both the Path to Artifact, and Artifact to Path conversions.  
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface BidirectionalRepositoryLayout
{
    /**
     * Get the identifier for this layout.
     * 
     * @return the identifier for this layout.
     */
    public String getId();
    
    /**
     * Given an ArchivaArtifact, return the relative path to the artifact.
     * 
     * @param artifact the artifact to use.
     * @return the relative path to the artifact. 
     */
    public String toPath( ArchivaArtifact artifact );
    
    /**
     * Given an ArtifactReference, return the relative path to the artifact.
     * 
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact. 
     */
    public String toPath( ArtifactReference reference );
    
    /**
     * Given an {@link VersionedReference}, return the relative path to that reference.
     * 
     * @param reference the versioned project reference to use.
     * @return the relative path to the project reference. 
     */
    public String toPath( VersionedReference reference );
    
    /**
     * Given an ProjectReference, return the relative path to that reference.
     * 
     * @param reference the project reference to use.
     * @return the relative path to the project reference. 
     */
    public String toPath( ProjectReference reference );

    /**
     * Given a repository relative path to a filename, return the {@link ArchivaArtifact} object suitable for the path.
     * 
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link ArchivaArtifact} representing the path. (or null if path cannot be converted to 
     *         an {@link ArchivaArtifact})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    public ArchivaArtifact toArtifact( String path ) throws LayoutException;
    
    /**
     * Given a repository relative path to a filename, return the {@link ProjectReference} object suitable for the path.
     * 
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link ProjectReference} representing the path.  (or null if path cannot be converted to 
     *         a {@link ProjectReference})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    public ProjectReference toProjectReference( String path ) throws LayoutException;
    
    /**
     * Given a repository relative path to a filename, return the {@link VersionedReference} object suitable for the path.
     * 
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link VersionedReference} representing the path.  (or null if path cannot be converted to 
     *         a {@link VersionedReference})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    public VersionedReference toVersionedReference( String path ) throws LayoutException;
    
    /**
     * Given a repository relative path to a filename, return the {@link VersionedReference} object suitable for the path.
     * 
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link ArtifactReference} representing the path.  (or null if path cannot be converted to 
     *         a {@link ArtifactReference})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    public ArtifactReference toArtifactReference( String path ) throws LayoutException;
}
