package org.apache.maven.repository.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;
import org.apache.maven.repository.indexing.RepositoryArtifactIndex;
import org.apache.maven.repository.indexing.RepositoryArtifactIndexFactory;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.lucene.LuceneQuery;
import org.apache.maven.repository.indexing.record.StandardArtifactIndexRecord;
import org.apache.maven.repository.indexing.record.StandardIndexRecordFields;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Validates an artifact file for duplicates within the same groupId based from what's available in a repository index.
 *
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.reporting.ArtifactReportProcessor" role-hint="duplicate"
 */
public class DuplicateArtifactFileReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory indexFactory;

    /**
     * @plexus.configuration
     */
    private String indexDirectory;

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
        throws ReportProcessorException
    {
        if ( artifact.getFile() != null )
        {
            RepositoryArtifactIndex index = indexFactory.createStandardIndex( new File( indexDirectory ), repository );

            String checksum;
            try
            {
                checksum = digester.createChecksum( artifact.getFile(), Digester.MD5 );
            }
            catch ( DigesterException e )
            {
                throw new ReportProcessorException( "Failed to generate checksum", e );
            }

            try
            {
                List results = index.search( new LuceneQuery(
                    new TermQuery( new Term( StandardIndexRecordFields.MD5, checksum.toLowerCase() ) ) ) );

                if ( results.isEmpty() )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    boolean hasDuplicates = false;
                    for ( Iterator i = results.iterator(); i.hasNext(); )
                    {
                        StandardArtifactIndexRecord result = (StandardArtifactIndexRecord) i.next();

                        //make sure it is not the same artifact
                        if ( !result.getFilename().equals( repository.pathOf( artifact ) ) )
                        {
                            //report only duplicates from the same groupId
                            String groupId = artifact.getGroupId();
                            if ( groupId.equals( result.getGroupId() ) )
                            {
                                hasDuplicates = true;
                                reporter.addFailure( artifact, "Found duplicate for " + artifact.getId() );
                            }
                        }
                    }

                    if ( !hasDuplicates )
                    {
                        reporter.addSuccess( artifact );
                    }
                }
            }
            catch ( RepositoryIndexSearchException e )
            {
                throw new ReportProcessorException( "Failed to search in index", e );
            }
        }
        else
        {
            reporter.addWarning( artifact, "Artifact file is null" );
        }
    }
}
