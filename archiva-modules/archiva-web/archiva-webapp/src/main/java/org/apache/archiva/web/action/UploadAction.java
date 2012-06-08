package org.apache.archiva.web.action;

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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Upload an artifact using Jakarta file upload in webwork. If set by the user a pom will also be generated. Metadata
 * will also be updated if one exists, otherwise it would be created.
 */
@SuppressWarnings( "serial" )
@Controller( "uploadAction" )
@Scope( "prototype" )
public class UploadAction
    extends AbstractActionSupport
    implements Validateable, Preparable, Auditable
{
    /**
     * The groupId of the artifact to be deployed.
     */
    private String groupId;

    /**
     * The artifactId of the artifact to be deployed.
     */
    private String artifactId;

    /**
     * The version of the artifact to be deployed.
     */
    private String version;

    /**
     * The packaging of the artifact to be deployed.
     */
    private String packaging;

    /**
     * The classifier of the artifact to be deployed.
     */
    private String classifier;

    /**
     * The temporary file representing the artifact to be deployed.
     */
    private File artifactFile;

    /**
     * The temporary file representing the pom to be deployed alongside the artifact.
     */
    private File pomFile;

    /**
     * The repository where the artifact is to be deployed.
     */
    private String repositoryId;

    /**
     * Flag whether to generate a pom for the artifact or not.
     */
    private boolean generatePom;

    /**
     * List of managed repositories to deploy to.
     */
    private List<String> managedRepoIdList;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private UserRepositories userRepositories;

    @Inject
    private ArchivaAdministration archivaAdministration;

    @Inject
    private RepositoryContentFactory repositoryFactory;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private ArchivaTaskScheduler scheduler;

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    public void setArtifact( File file )
    {
        this.artifactFile = file;
    }

    public void setArtifactContentType( String contentType )
    {
        StringUtils.trim( contentType );
    }

    public void setArtifactFileName( String filename )
    {
        StringUtils.trim( filename );
    }

    public void setPom( File file )
    {
        this.pomFile = file;
    }

    public void setPomContentType( String contentType )
    {
        StringUtils.trim( contentType );
    }

    public void setPomFileName( String filename )
    {
        StringUtils.trim( filename );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = StringUtils.trim( groupId );
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = StringUtils.trim( artifactId );
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = StringUtils.trim( version );
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = StringUtils.trim( packaging );
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = StringUtils.trim( classifier );
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public boolean isGeneratePom()
    {
        return generatePom;
    }

    public void setGeneratePom( boolean generatePom )
    {
        this.generatePom = generatePom;
    }

    public List<String> getManagedRepoIdList()
    {
        return managedRepoIdList;
    }

    public void setManagedRepoIdList( List<String> managedRepoIdList )
    {
        this.managedRepoIdList = managedRepoIdList;
    }

    public void prepare()
    {
        managedRepoIdList = getManagableRepos();
    }

    public String input()
    {
        return INPUT;
    }

    private void reset()
    {
        // reset the fields so the form is clear when 
        // the action returns to the jsp page
        groupId = "";
        artifactId = "";
        version = "";
        packaging = "";
        classifier = "";
        artifactFile = null;
        pomFile = null;
        repositoryId = "";
        generatePom = false;
    }

    public String doUpload()
    {
        try
        {
            ManagedRepository repoConfig = managedRepositoryAdmin.getManagedRepository( repositoryId );

            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setArtifactId( artifactId );
            artifactReference.setGroupId( groupId );
            artifactReference.setVersion( version );
            artifactReference.setClassifier( classifier );
            artifactReference.setType( packaging );

            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            String artifactPath = repository.toPath( artifactReference );

            int lastIndex = artifactPath.lastIndexOf( '/' );

            String path = artifactPath.substring( 0, lastIndex );
            File targetPath = new File( repoConfig.getLocation(), path );

            log.debug( "artifactPath: {} found targetPath: {}", artifactPath, targetPath );

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = -1;
            String timestamp = null;

            File versionMetadataFile = new File( targetPath, MetadataTools.MAVEN_METADATA );
            ArchivaRepositoryMetadata versionMetadata = getMetadata( versionMetadataFile );

            if ( VersionUtil.isSnapshot( version ) )
            {
                TimeZone timezone = TimeZone.getTimeZone( "UTC" );
                DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
                fmt.setTimeZone( timezone );
                timestamp = fmt.format( lastUpdatedTimestamp );
                if ( versionMetadata.getSnapshotVersion() != null )
                {
                    newBuildNumber = versionMetadata.getSnapshotVersion().getBuildNumber() + 1;
                }
                else
                {
                    newBuildNumber = 1;
                }
            }

            if ( !targetPath.exists() )
            {
                targetPath.mkdirs();
            }

            String filename = artifactPath.substring( lastIndex + 1 );
            if ( VersionUtil.isSnapshot( version ) )
            {
                filename = filename.replaceAll( VersionUtil.SNAPSHOT, timestamp + "-" + newBuildNumber );
            }

            boolean fixChecksums =
                !( archivaAdministration.getKnownContentConsumers().contains( "create-missing-checksums" ) );

            try
            {
                File targetFile = new File( targetPath, filename );
                if ( targetFile.exists() && !VersionUtil.isSnapshot( version ) && repoConfig.isBlockRedeployments() )
                {
                    addActionError(
                        "Overwriting released artifacts in repository '" + repoConfig.getId() + "' is not allowed." );
                    return ERROR;
                }
                else
                {
                    copyFile( artifactFile, targetPath, filename, fixChecksums );
                    triggerAuditEvent( repository.getId(), path + "/" + filename, AuditEvent.UPLOAD_FILE );
                    queueRepositoryTask( repository.getId(), targetFile );
                }
            }
            catch ( IOException ie )
            {
                addActionError( "Error encountered while uploading file: " + ie.getMessage() );
                return ERROR;
            }

            String pomFilename = filename;
            if ( classifier != null && !"".equals( classifier ) )
            {
                pomFilename = StringUtils.remove( pomFilename, "-" + classifier );
            }
            pomFilename = FilenameUtils.removeExtension( pomFilename ) + ".pom";

            if ( generatePom )
            {
                try
                {
                    File generatedPomFile = createPom( targetPath, pomFilename );
                    triggerAuditEvent( repoConfig.getId(), path + "/" + pomFilename, AuditEvent.UPLOAD_FILE );
                    if ( fixChecksums )
                    {
                        fixChecksums( generatedPomFile );
                    }
                    queueRepositoryTask( repoConfig.getId(), generatedPomFile );
                }
                catch ( IOException ie )
                {
                    addActionError( "Error encountered while writing pom file: " + ie.getMessage() );
                    return ERROR;
                }
            }

            if ( pomFile != null && pomFile.length() > 0 )
            {
                try
                {
                    copyFile( pomFile, targetPath, pomFilename, fixChecksums );
                    triggerAuditEvent( repoConfig.getId(), path + "/" + pomFilename, AuditEvent.UPLOAD_FILE );
                    queueRepositoryTask( repoConfig.getId(), new File( targetPath, pomFilename ) );
                }
                catch ( IOException ie )
                {
                    addActionError( "Error encountered while uploading pom file: " + ie.getMessage() );
                    return ERROR;
                }

            }

            // explicitly update only if metadata-updater consumer is not enabled!
            if ( !archivaAdministration.getKnownContentConsumers().contains( "metadata-updater" ) )
            {
                updateProjectMetadata( targetPath.getAbsolutePath(), lastUpdatedTimestamp, timestamp, newBuildNumber,
                                       fixChecksums );

                if ( VersionUtil.isSnapshot( version ) )
                {
                    updateVersionMetadata( versionMetadata, versionMetadataFile, lastUpdatedTimestamp, timestamp,
                                           newBuildNumber, fixChecksums );
                }
            }

            String msg = "Artifact \'" + groupId + ":" + artifactId + ":" + version
                + "\' was successfully deployed to repository \'" + repositoryId + "\'";

            addActionMessage( msg );

            reset();
            return SUCCESS;
        }
        catch ( RepositoryNotFoundException re )
        {
            addActionError( "Target repository cannot be found: " + re.getMessage() );
            return ERROR;
        }
        catch ( RepositoryException rep )
        {
            addActionError( "Repository exception: " + rep.getMessage() );
            return ERROR;
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "RepositoryAdmin exception: " + e.getMessage() );
            return ERROR;
        }
    }

    private void fixChecksums( File file )
    {
        ChecksummedFile checksum = new ChecksummedFile( file );
        checksum.fixChecksums( algorithms );
    }

    private void copyFile( File sourceFile, File targetPath, String targetFilename, boolean fixChecksums )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( new File( targetPath, targetFilename ) );
        FileInputStream input = new FileInputStream( sourceFile );

        try
        {
            IOUtils.copy( input, out );
        }
        finally
        {
            IOUtils.closeQuietly( out );
            IOUtils.closeQuietly( input );
        }

        if ( fixChecksums )
        {
            fixChecksums( new File( targetPath, targetFilename ) );
        }
    }

    private File createPom( File targetPath, String filename )
        throws IOException
    {
        Model projectModel = new Model();
        projectModel.setModelVersion( "4.0.0" );
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );
        projectModel.setPackaging( packaging );

        File pomFile = new File( targetPath, filename );
        MavenXpp3Writer writer = new MavenXpp3Writer();
        FileWriter w = new FileWriter( pomFile );
        try
        {
            writer.write( w, projectModel );
        }
        finally
        {
            IOUtils.closeQuietly( w );
        }

        return pomFile;
    }

    private ArchivaRepositoryMetadata getMetadata( File metadataFile )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if ( metadataFile.exists() )
        {
            try
            {
                metadata = MavenMetadataReader.read( metadataFile );
            }
            catch ( XMLException e )
            {
                throw new RepositoryMetadataException( e.getMessage(), e );
            }
        }
        return metadata;
    }


    /**
     * Update version level metadata for snapshot artifacts. If it does not exist, create the metadata and fix checksums
     * if necessary.
     */
    private void updateVersionMetadata( ArchivaRepositoryMetadata metadata, File metadataFile,
                                        Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                        boolean fixChecksums )
        throws RepositoryMetadataException
    {
        if ( !metadataFile.exists() )
        {
            metadata.setGroupId( groupId );
            metadata.setArtifactId( artifactId );
            metadata.setVersion( version );
        }

        if ( metadata.getSnapshotVersion() == null )
        {
            metadata.setSnapshotVersion( new SnapshotVersion() );
        }

        metadata.getSnapshotVersion().setBuildNumber( buildNumber );
        metadata.getSnapshotVersion().setTimestamp( timestamp );
        metadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );

        RepositoryMetadataWriter.write( metadata, metadataFile );

        if ( fixChecksums )
        {
            fixChecksums( metadataFile );
        }
    }

    /**
     * Update artifact level metadata. If it does not exist, create the metadata and fix checksums if necessary.
     */
    private void updateProjectMetadata( String targetPath, Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                        boolean fixChecksums )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<String>();
        String latestVersion = version;

        File projectDir = new File( targetPath ).getParentFile();
        File projectMetadataFile = new File( projectDir, MetadataTools.MAVEN_METADATA );

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetadataFile );

        if ( projectMetadataFile.exists() )
        {
            availableVersions = projectMetadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( version ) )
            {
                availableVersions.add( version );
            }

            latestVersion = availableVersions.get( availableVersions.size() - 1 );
        }
        else
        {
            availableVersions.add( version );

            projectMetadata.setGroupId( groupId );
            projectMetadata.setArtifactId( artifactId );
        }

        if ( projectMetadata.getGroupId() == null )
        {
            projectMetadata.setGroupId( groupId );
        }

        if ( projectMetadata.getArtifactId() == null )
        {
            projectMetadata.setArtifactId( artifactId );
        }

        projectMetadata.setLatestVersion( latestVersion );
        projectMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        projectMetadata.setAvailableVersions( availableVersions );

        if ( !VersionUtil.isSnapshot( version ) )
        {
            projectMetadata.setReleasedVersion( latestVersion );
        }

        RepositoryMetadataWriter.write( projectMetadata, projectMetadataFile );

        if ( fixChecksums )
        {
            fixChecksums( projectMetadataFile );
        }
    }

    public void validate()
    {
        try
        {
            // is this enough check for the repository permission?
            if ( !userRepositories.isAuthorizedToUploadArtifacts( getPrincipal(), repositoryId ) )
            {
                addActionError( "User is not authorized to upload in repository " + repositoryId );
            }

            if ( artifactFile == null || artifactFile.length() == 0 )
            {
                addActionError( "Please add a file to upload." );
            }

            if ( version == null || !VersionUtil.isVersion( version ) )
            {
                addActionError( "Invalid version." );
            }
        }
        catch ( PrincipalNotFoundException pe )
        {
            addActionError( pe.getMessage() );
        }
        catch ( ArchivaSecurityException ae )
        {
            addActionError( ae.getMessage() );
        }
    }

    private List<String> getManagableRepos()
    {
        try
        {
            return userRepositories.getManagableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
            // TODO: pass this onto the screen.
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    private void queueRepositoryTask( String repositoryId, File localFile )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setResourceFile( localFile );
        task.setUpdateRelatedArtifacts( true );
        task.setScanAll( false );

        try
        {
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Unable to queue repository task to execute consumers on resource file ['" + localFile.getName()
                           + "']." );
        }
    }

    public void setScheduler( ArchivaTaskScheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
