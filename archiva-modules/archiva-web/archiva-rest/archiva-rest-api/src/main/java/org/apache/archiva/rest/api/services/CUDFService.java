package org.apache.archiva.rest.api.services;

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

import org.apache.archiva.redback.authorization.RedbackAuthorization;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Adrien Lecharpentier
 */
@Path( "/cudfService/" )
public interface CUDFService
{
    /**
     * Gets the cone extract for the artifact as text response.
     *
     * @param groupId    the groupId of the desired artifact
     * @param artifactId the artifactId of the desired artifact
     * @param version    the version of the desired artifact
     * @return The CUDF extract for the artifact in CharSequence
     * @throws ArchivaRestServiceException
     */
    @Path( "cone/{groupId}/{artifactId}/{version}" )
    @GET
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.TEXT_PLAIN )
    String getConeCUDF( @PathParam( "groupId" ) String groupId, @PathParam( "artifactId" ) String artifactId,
                        @PathParam( "version" ) String version, @QueryParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    /**
     * Gets the cone extract for the artifact in file.
     *
     * @param groupId    the groupId of the desired artifact
     * @param artifactId the artifactId of the desired artifact
     * @param version    the version of the desired artifact
     * @return The CUDF extract for the artifact in a file
     * @throws ArchivaRestServiceException
     */
    @Path( "cone/{groupId}/{artifactId}/{version}" )
    @POST
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Response getConeCUDFFile( @PathParam( "groupId" ) String groupId, @PathParam( "artifactId" ) String artifactId,
                              @PathParam( "version" ) String version,
                              @QueryParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    /**
     * Gets the entire repository into cudf output.
     *
     * @return the universe included into the repositories
     * @throws ArchivaRestServiceException
     */
    @Path( "universe" )
    @GET
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.TEXT_PLAIN )
    CharSequence getUniverseCUDF( @QueryParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    /**
     * Gets the entire repository into a cudf file
     *
     * @return the universe included into the repositories
     * @throws ArchivaRestServiceException
     */
    @Path( "universe" )
    @POST
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Response getUniverseCUDFFile( @QueryParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;
}
