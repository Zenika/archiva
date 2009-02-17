package org.apache.maven.archiva.consumers.lucene;

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

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;

import java.io.File;
import java.util.List;

/**
 * LuceneCleanupRemoveIndexedConsumer
 * 
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer"
 *                   role-hint="not-present-remove-indexed" instantiation-strategy="per-lookup"
 */
public class LuceneCleanupRemoveIndexedConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    /**
     * @plexus.configuration default-value="not-present-remove-indexed"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Remove indexed content if not present on filesystem."
     */
    private String description;
//
//    /**
//     * @plexus.requirement role-hint="lucene"
//     */
//    private RepositoryContentIndexFactory repoIndexFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repoFactory;

    public void beginScan()
    {
        // TODO Auto-generated method stub

    }

    public void completeScan()
    {
        // TODO Auto-generated method stub

    }

    public List<String> getIncludedTypes()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        try
        {   
            ManagedRepositoryContent repoContent =
                repoFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );

            File file = new File( repoContent.getLocalPath(), repoContent.toPath( artifact ) );
            
            if( !file.exists() )
            {   
//                RepositoryContentIndex bytecodeIndex = repoIndexFactory.createBytecodeIndex( repoContent.getRepository() );
//                RepositoryContentIndex hashcodesIndex = repoIndexFactory.createHashcodeIndex( repoContent.getRepository() );
//                RepositoryContentIndex fileContentIndex =
//                    repoIndexFactory.createFileContentIndex( repoContent.getRepository() );
    
//                FileContentRecord fileContentRecord = new FileContentRecord();
//                fileContentRecord.setFilename( repoContent.toPath( artifact ) );
//                fileContentIndex.deleteRecord( fileContentRecord );
//
//                HashcodesRecord hashcodesRecord = new HashcodesRecord();
//                hashcodesRecord.setArtifact( artifact );
//                hashcodesIndex.deleteRecord( hashcodesRecord );
//
//                BytecodeRecord bytecodeRecord = new BytecodeRecord();
//                bytecodeRecord.setArtifact( artifact );
//                bytecodeIndex.deleteRecord( bytecodeRecord );
            }                
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run index cleanup consumer: " + e.getMessage() );
        }
//        catch ( RepositoryIndexException e )
//        {
//            throw new ConsumerException( e.getMessage() );
//        }
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

    public void setRepositoryContentFactory( RepositoryContentFactory repoFactory )
    {
        this.repoFactory = repoFactory;
    }
}
