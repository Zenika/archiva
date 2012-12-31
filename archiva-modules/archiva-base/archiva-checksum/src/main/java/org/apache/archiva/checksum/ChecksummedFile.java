package org.apache.archiva.checksum;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChecksummedFile
 * <p/>
 * <dl>
 * <lh>Terminology:</lh>
 * <dt>Checksum File</dt>
 * <dd>The file that contains the previously calculated checksum value for the reference file.
 * This is a text file with the extension ".sha1" or ".md5", and contains a single entry
 * consisting of an optional reference filename, and a checksum string.
 * </dd>
 * <dt>Reference File</dt>
 * <dd>The file that is being referenced in the checksum file.</dd>
 * </dl>
 *
 *
 */
public class ChecksummedFile
{
    private Logger log = LoggerFactory.getLogger( ChecksummedFile.class );

    private final File referenceFile;

    /**
     * Construct a ChecksummedFile object.
     *
     * @param referenceFile
     */
    public ChecksummedFile( final File referenceFile )
    {
        this.referenceFile = referenceFile;
    }

    /**
     * Calculate the checksum based on a given checksum.
     *
     * @param checksumAlgorithm the algorithm to use.
     * @return the checksum string for the file.
     * @throws IOException if unable to calculate the checksum.
     */
    public String calculateChecksum( ChecksumAlgorithm checksumAlgorithm )
        throws IOException
    {
        FileInputStream fis = null;
        try
        {
            Checksum checksum = new Checksum( checksumAlgorithm );
            fis = new FileInputStream( referenceFile );
            checksum.update( fis );
            return checksum.getChecksum();
        }
        finally
        {
            IOUtils.closeQuietly( fis );
        }
    }

    /**
     * Creates a checksum file of the provided referenceFile.
     *
     * @param checksumAlgorithm the hash to use.
     * @return the checksum File that was created.
     * @throws IOException if there was a problem either reading the referenceFile, or writing the checksum file.
     */
    public File createChecksum( ChecksumAlgorithm checksumAlgorithm )
        throws IOException
    {
        File checksumFile = new File( referenceFile.getAbsolutePath() + "." + checksumAlgorithm.getExt() );
        String checksum = calculateChecksum( checksumAlgorithm );
        FileUtils.writeStringToFile( checksumFile, checksum + "  " + referenceFile.getName() );
        return checksumFile;
    }

    /**
     * Get the checksum file for the reference file and hash.
     *
     * @param checksumAlgorithm the hash that we are interested in.
     * @return the checksum file to return
     */
    public File getChecksumFile( ChecksumAlgorithm checksumAlgorithm )
    {
        return new File( referenceFile.getAbsolutePath() + "." + checksumAlgorithm.getExt() );
    }

    /**
     * <p>
     * Given a checksum file, check to see if the file it represents is valid according to the checksum.
     * </p>
     * <p/>
     * <p>
     * NOTE: Only supports single file checksums of type MD5 or SHA1.
     * </p>
     *
     * @param checksumFile the algorithms to check for.
     * @return true if the checksum is valid for the file it represents. or if the checksum file does not exist.
     * @throws IOException if the reading of the checksumFile or the file it refers to fails.
     */
    public boolean isValidChecksum( ChecksumAlgorithm algorithm )
        throws IOException
    {
        return isValidChecksums( new ChecksumAlgorithm[]{ algorithm } );
    }

    /**
     * Of any checksum files present, validate that the reference file conforms
     * the to the checksum.
     *
     * @param algorithms the algorithms to check for.
     * @return true if the checksums report that the the reference file is valid, false if invalid.
     */
    public boolean isValidChecksums( ChecksumAlgorithm algorithms[] )
    {
        FileInputStream fis = null;
        try
        {
            List<Checksum> checksums = new ArrayList<Checksum>( algorithms.length );
            // Create checksum object for each algorithm.
            for ( ChecksumAlgorithm checksumAlgorithm : algorithms )
            {
                File checksumFile = getChecksumFile( checksumAlgorithm );

                // Only add algorithm if checksum file exists.
                if ( checksumFile.exists() )
                {
                    checksums.add( new Checksum( checksumAlgorithm ) );
                }
            }

            // Any checksums?
            if ( checksums.isEmpty() )
            {
                // No checksum objects, no checksum files, default to is invalid.
                return false;
            }

            // Parse file once, for all checksums.
            try
            {
                fis = new FileInputStream( referenceFile );
                Checksum.update( checksums, fis );
            }
            catch ( IOException e )
            {
                log.warn( "Unable to update checksum:" + e.getMessage() );
                return false;
            }

            boolean valid = true;

            // check the checksum files
            try
            {
                for ( Checksum checksum : checksums )
                {
                    ChecksumAlgorithm checksumAlgorithm = checksum.getAlgorithm();
                    File checksumFile = getChecksumFile( checksumAlgorithm );

                    String rawChecksum = FileUtils.readFileToString( checksumFile );
                    String expectedChecksum = parseChecksum( rawChecksum, checksumAlgorithm, referenceFile.getName() );

                    if ( !StringUtils.equalsIgnoreCase( expectedChecksum, checksum.getChecksum() ) )
                    {
                        valid = false;
                    }
                }
            }
            catch ( IOException e )
            {
                log.warn( "Unable to read / parse checksum: " + e.getMessage() );
                return false;
            }

            return valid;
        }
        finally
        {
            IOUtils.closeQuietly( fis );
        }
    }

    /**
     * Fix or create checksum files for the reference file.
     *
     * @param algorithms the hashes to check for.
     * @return true if checksums were created successfully.
     */
    public boolean fixChecksums( ChecksumAlgorithm[] algorithms )
    {
        List<Checksum> checksums = new ArrayList<Checksum>( algorithms.length );
        // Create checksum object for each algorithm.
        for ( ChecksumAlgorithm checksumAlgorithm : algorithms )
        {
            checksums.add( new Checksum( checksumAlgorithm ) );
        }

        // Any checksums?
        if ( checksums.isEmpty() )
        {
            // No checksum objects, no checksum files, default to is valid.
            return true;
        }

        FileInputStream fis = null;
        try
        {
            // Parse file once, for all checksums.
            fis = new FileInputStream( referenceFile );
            Checksum.update( checksums, fis );
        }
        catch ( IOException e )
        {
            log.warn( e.getMessage(), e );
            return false;
        }
        finally
        {
            IOUtils.closeQuietly( fis );
        }

        boolean valid = true;

        // check the hash files
        for ( Checksum checksum : checksums )
        {
            ChecksumAlgorithm checksumAlgorithm = checksum.getAlgorithm();
            try
            {
                File checksumFile = getChecksumFile( checksumAlgorithm );
                String actualChecksum = checksum.getChecksum();

                if ( checksumFile.exists() )
                {
                    String rawChecksum = FileUtils.readFileToString( checksumFile );
                    String expectedChecksum = parseChecksum( rawChecksum, checksumAlgorithm, referenceFile.getName() );

                    if ( !StringUtils.equalsIgnoreCase( expectedChecksum, actualChecksum ) )
                    {
                        // create checksum (again)
                        FileUtils.writeStringToFile( checksumFile, actualChecksum + "  " + referenceFile.getName() );
                    }
                }
                else
                {
                    FileUtils.writeStringToFile( checksumFile, actualChecksum + "  " + referenceFile.getName() );
                }
            }
            catch ( IOException e )
            {
                log.warn( e.getMessage(), e );
                valid = false;
            }
        }

        return valid;

    }

    private boolean isValidChecksumPattern( String filename, String path )
    {
        // check if it is a remote metadata file
        Pattern pattern = Pattern.compile( "maven-metadata-\\S*.xml" );
        Matcher m = pattern.matcher( path );
        if ( m.matches() )
        {
            return filename.endsWith( path ) || ( "-".equals( filename ) ) || filename.endsWith( "maven-metadata.xml" );
        }

        return filename.endsWith( path ) || ( "-".equals( filename ) );
    }

    /**
     * Parse a checksum string.
     * <p/>
     * Validate the expected path, and expected checksum algorithm, then return
     * the trimmed checksum hex string.
     *
     * @param rawChecksumString
     * @param expectedHash
     * @param expectedPath
     * @return
     * @throws IOException
     */
    public String parseChecksum( String rawChecksumString, ChecksumAlgorithm expectedHash, String expectedPath )
        throws IOException
    {
        String trimmedChecksum = rawChecksumString.replace( '\n', ' ' ).trim();

        // Free-BSD / openssl
        String regex = expectedHash.getType() + "\\s*\\(([^)]*)\\)\\s*=\\s*([a-fA-F0-9]+)";
        Matcher m = Pattern.compile( regex ).matcher( trimmedChecksum );
        if ( m.matches() )
        {
            String filename = m.group( 1 );
            if ( !isValidChecksumPattern( filename, expectedPath ) )
            {
                throw new IOException(
                    "Supplied checksum file '" + filename + "' does not match expected file: '" + expectedPath + "'" );
            }
            trimmedChecksum = m.group( 2 );
        }
        else
        {
            // GNU tools
            m = Pattern.compile( "([a-fA-F0-9]+)\\s+\\*?(.+)" ).matcher( trimmedChecksum );
            if ( m.matches() )
            {
                String filename = m.group( 2 );
                if ( !isValidChecksumPattern( filename, expectedPath ) )
                {
                    throw new IOException(
                        "Supplied checksum file '" + filename + "' does not match expected file: '" + expectedPath
                            + "'" );
                }
                trimmedChecksum = m.group( 1 );
            }
        }
        return trimmedChecksum;
    }
}
