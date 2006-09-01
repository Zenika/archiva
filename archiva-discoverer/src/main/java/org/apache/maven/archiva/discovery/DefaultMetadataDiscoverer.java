package org.apache.maven.archiva.discovery;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * This class gets all the paths that contain the metadata files.
 *
 * @plexus.component role="org.apache.maven.archiva.discovery.MetadataDiscoverer" role-hint="default"
 */
public class DefaultMetadataDiscoverer
    extends AbstractDiscoverer
    implements MetadataDiscoverer
{
    /**
     * Standard patterns to include in discovery of metadata files.
     *
     * @todo Note that only the remote format is supported at this time: you cannot search local repository metadata due
     * to the way it is later loaded in the searchers. Review code using pathOfRemoteMetadata. IS there any value in
     * searching the local metadata in the first place though?
     */
    private static final String[] STANDARD_DISCOVERY_INCLUDES = {"**/maven-metadata.xml"};

    public List discoverMetadata( ArtifactRepository repository, String operation, List blacklistedPatterns )
        throws DiscovererException
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported" );
        }

        Xpp3Dom dom = getLastMetadataDiscoveryDom( readRepositoryMetadataDom( repository ) );
        long comparisonTimestamp = readComparisonTimestamp( repository, operation, dom );

        // Note that last checked time is deliberately set to the start of the process so that anything added
        // mid-discovery and missed by the scanner will get checked next time.
        // Due to this, there must be no negative side-effects of discovering something twice.
        Date newLastCheckedTime = new Date();

        List metadataFiles = new ArrayList();
        List metadataPaths = scanForArtifactPaths( new File( repository.getBasedir() ), blacklistedPatterns,
                                                   STANDARD_DISCOVERY_INCLUDES, null, comparisonTimestamp );

        // Also note that the last check time, while set at the start, is saved at the end, so that if any exceptions
        // occur, then the timestamp is not updated so that the discovery is attempted again
        // TODO: under the list-return behaviour we have now, exceptions might occur later and the timestamp will not be reset - see MRM-83
        try
        {
            setLastCheckedTime( repository, operation, newLastCheckedTime );
        }
        catch ( IOException e )
        {
            throw new DiscovererException( "Error writing metadata: " + e.getMessage(), e );
        }

        for ( Iterator i = metadataPaths.iterator(); i.hasNext(); )
        {
            String metadataPath = (String) i.next();
            try
            {
                RepositoryMetadata metadata = buildMetadata( repository.getBasedir(), metadataPath );
                metadataFiles.add( metadata );
            }
            catch ( DiscovererException e )
            {
                addKickedOutPath( metadataPath, e.getMessage() );
            }
        }

        return metadataFiles;
    }

    private RepositoryMetadata buildMetadata( String repo, String metadataPath )
        throws DiscovererException
    {
        Metadata m;
        String repoPath = repo + "/" + metadataPath;
        try
        {
            URL url = new File( repoPath ).toURI().toURL();
            InputStream is = url.openStream();
            Reader reader = new InputStreamReader( is );
            MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

            m = metadataReader.read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DiscovererException( "Error parsing metadata file '" + repoPath + "': " + e.getMessage(), e );
        }
        catch ( MalformedURLException e )
        {
            // shouldn't happen
            throw new DiscovererException( "Error constructing metadata file '" + repoPath + "': " + e.getMessage(),
                                           e );
        }
        catch ( IOException e )
        {
            throw new DiscovererException( "Error reading metadata file '" + repoPath + "': " + e.getMessage(), e );
        }

        RepositoryMetadata repositoryMetadata = buildMetadata( m, metadataPath );

        if ( repositoryMetadata == null )
        {
            throw new DiscovererException( "Unable to build a repository metadata from path" );
        }

        return repositoryMetadata;
    }

    /**
     * Builds a RepositoryMetadata object from a Metadata object and its path.
     *
     * @param m            Metadata
     * @param metadataPath path
     * @return RepositoryMetadata if the parameters represent one; null if not
     * @todo should we just be using the path information, and loading it later when it is needed? (for reporting, etc)
     */
    private RepositoryMetadata buildMetadata( Metadata m, String metadataPath )
    {
        String metaGroupId = m.getGroupId();
        String metaArtifactId = m.getArtifactId();
        String metaVersion = m.getVersion();

        // check if the groupId, artifactId and version is in the
        // metadataPath
        // parse the path, in reverse order
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( metadataPath, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );
        // remove the metadata file
        pathParts.remove( 0 );
        Iterator it = pathParts.iterator();
        String tmpDir = (String) it.next();

        Artifact artifact = null;
        if ( !StringUtils.isEmpty( metaVersion ) )
        {
            artifact = artifactFactory.createProjectArtifact( metaGroupId, metaArtifactId, metaVersion );
        }

        // snapshotMetadata
        RepositoryMetadata metadata = null;
        if ( tmpDir != null && tmpDir.equals( metaVersion ) )
        {
            if ( artifact != null )
            {
                metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            }
        }
        else if ( tmpDir != null && tmpDir.equals( metaArtifactId ) )
        {
            // artifactMetadata
            if ( artifact != null )
            {
                metadata = new ArtifactRepositoryMetadata( artifact );
            }
            else
            {
                artifact = artifactFactory.createProjectArtifact( metaGroupId, metaArtifactId, "1.0" );
                metadata = new ArtifactRepositoryMetadata( artifact );
            }
        }
        else
        {
            String groupDir = "";
            int ctr = 0;
            for ( it = pathParts.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                if ( ctr == 0 )
                {
                    groupDir = path;
                }
                else
                {
                    groupDir = path + "." + groupDir;
                }
                ctr++;
            }

            // groupMetadata
            if ( metaGroupId != null && metaGroupId.equals( groupDir ) )
            {
                metadata = new GroupRepositoryMetadata( metaGroupId );
            }
        }

        return metadata;
    }

    public void setLastCheckedTime( ArtifactRepository repository, String operation, Date date )
        throws IOException
    {
        // see notes in resetLastCheckedTime

        File file = new File( repository.getBasedir(), "maven-metadata.xml" );

        Xpp3Dom dom = readDom( file );

        String dateString = new SimpleDateFormat( DATE_FMT, Locale.US ).format( date );

        setEntry( getLastMetadataDiscoveryDom( dom ), operation, dateString );

        saveDom( file, dom );
    }
}
