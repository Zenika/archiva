package org.apache.archiva.rest.services.interceptors;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.ArchivaRestError;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Provider
@Service( "archivaRestServiceExceptionMapper" )
public class ArchivaRestServiceExceptionMapper
    implements ExceptionMapper<ArchivaRestServiceException>
{
    public Response toResponse( ArchivaRestServiceException e )
    {
        try
        {
            ArchivaRestError restError = new ArchivaRestError( e );
            ObjectMapper mapper = new ObjectMapper(  );
            String errorString = mapper.writeValueAsString( restError );
            Response.ResponseBuilder responseBuilder = Response.status( e.getHttpErrorCode() ).entity( errorString );
            return responseBuilder.build();
        }
        catch ( IOException e1 )
        {
            throw new RuntimeException( "Unable to map exception", e1 );
        }
    }
}
