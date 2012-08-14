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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.cudf.admin.bean.CUDFJob;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 1.4-M3
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
     * @param type       the type of the desired artifact
     * @return The CUDF extract for the artifact in CharSequence
     * @throws ArchivaRestServiceException
     */
    @Path( "cone/{groupId}/{artifactId}/{version}" )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    void getConeCUDF( @PathParam( "groupId" ) String groupId, @PathParam( "artifactId" ) String artifactId,
                      @PathParam( "version" ) String version, @QueryParam( "type" ) String type,
                      @QueryParam( "repositoryId" ) String repositoryId, @Context HttpServletResponse servletResponse )
        throws ArchivaRestServiceException;

    /**
     * Gets the cone extract for the artifact in file.
     *
     * @param groupId    the groupId of the desired artifact
     * @param artifactId the artifactId of the desired artifact
     * @param version    the version of the desired artifact
     * @param type       the type of the desired artifact
     * @return The CUDF extract for the artifact in a file
     * @throws ArchivaRestServiceException
     */
    @Path( "cone/{groupId}/{artifactId}/{version}" )
    @GET
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Response getConeCUDFFile( @PathParam( "groupId" ) String groupId, @PathParam( "artifactId" ) String artifactId,
                              @PathParam( "version" ) String version, @QueryParam( "type" ) String type,
                              @QueryParam( "repositoryId" ) String repositoryId, @QueryParam( "keep" ) boolean keep )
        throws ArchivaRestServiceException;

    /**
     * Gets the entire repository into cudf output.
     *
     * @return the universe included into the repositories
     * @throws ArchivaRestServiceException
     */
    @Path( "universe" )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    void getUniverseCUDF( @QueryParam( "repositoryId" ) String repositoryId,
                          @Context HttpServletResponse servletResponse )
        throws ArchivaRestServiceException;

    /**
     * Gets the entire repository into a cudf file
     *
     * @return the universe included into the repositories
     * @throws ArchivaRestServiceException
     */
    @Path( "universe" )
    @GET
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Response getUniverseCUDFFile( @QueryParam( "repositoryId" ) String repositoryId,
                                  @QueryParam( "keep" ) boolean keep )
        throws ArchivaRestServiceException;

    @Path( "startCudfGeneration" )
    @GET
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Response startCudfTaskGeneration( @QueryParam( "filePath" ) String filePath )
        throws ArchivaRestServiceException;

    @Path( "jobs/{id}/start" )
    @POST
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void startCUDFJob( @PathParam( "id" ) String id )
        throws ArchivaRestServiceException;

    @Path( "jobs" )
    @GET
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    List<CUDFJob> getCUDFJobs()
        throws ArchivaRestServiceException;

    @Path( "jobs/{id}" )
    @GET
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    CUDFJob getCUDFJob( @PathParam( "id" ) String id )
        throws ArchivaRestServiceException;

    @Path( "jobs/{id}" )
    @PUT
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    void updateCUDFJob( @PathParam( "id" ) String id, CUDFJob cudfJob )
        throws ArchivaRestServiceException;

    @Path( "jobs" )
    @POST
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    void addCUDFJob( CUDFJob cudfJob )
        throws ArchivaRestServiceException;

    @Path( "jobs" )
    @DELETE
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    void deleteCUDFJob( CUDFJob cudfJob )
        throws ArchivaRestServiceException;

    @Path( "jobs/{id}/" )
    @DELETE
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    void deleteCUDFJob( @PathParam( "id" ) String id )
        throws ArchivaRestServiceException;
}
