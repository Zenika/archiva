package org.apache.archiva.rest.services.utils;
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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.rest.api.model.ArtifactDownloadInfo;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class ArtifactDownloadInfoBuilder
{

    private ManagedRepositoryContent managedRepositoryContent;

    private ArtifactMetadata artifactMetadata;

    public ArtifactDownloadInfoBuilder()
    {
        // no op
    }


    public ArtifactDownloadInfoBuilder withManagedRepositoryContent( ManagedRepositoryContent managedRepositoryContent )
    {
        this.managedRepositoryContent = managedRepositoryContent;
        return this;
    }

    public ArtifactDownloadInfoBuilder forArtifactMetadata( ArtifactMetadata artifactMetadata )
    {
        this.artifactMetadata = artifactMetadata;
        return this;
    }

    public ArtifactDownloadInfo build()
    {
        ArtifactReference ref = new ArtifactReference();
        ref.setArtifactId( artifactMetadata.getProject() );
        ref.setGroupId( artifactMetadata.getNamespace() );
        ref.setVersion( artifactMetadata.getVersion() );
        String path = managedRepositoryContent.toPath( ref );
        path = path.substring( 0, path.lastIndexOf( "/" ) + 1 ) + artifactMetadata.getId();

        String type = null, classifier = null;

        MavenArtifactFacet facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        if ( facet != null )
        {
            type = facet.getType();
            classifier = facet.getClassifier();
        }

        ArtifactDownloadInfo artifactDownloadInfo =
            new ArtifactDownloadInfo( this.artifactMetadata, path, type, classifier );
        return artifactDownloadInfo;

    }


}
