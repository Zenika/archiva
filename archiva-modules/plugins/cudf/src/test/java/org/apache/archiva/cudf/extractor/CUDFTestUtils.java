package org.apache.archiva.cudf.extractor;

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

import com.zenika.cudf.model.Binaries;
import com.zenika.cudf.model.Binary;
import com.zenika.cudf.model.BinaryId;
import com.zenika.cudf.model.CUDFDescriptor;
import com.zenika.cudf.model.DefaultBinaries;
import com.zenika.cudf.model.Preamble;
import com.zenika.cudf.model.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
public class CUDFTestUtils
{
    public static final String DEFAULT_ORGANISATION = "org.apache";

    public static final BinaryId BINARY_ID_1 = new BinaryId( "jar1", DEFAULT_ORGANISATION, 1 );

    public static final BinaryId BINARY_ID_2 = new BinaryId( "jar2", DEFAULT_ORGANISATION, 1 );

    public static final BinaryId BINARY_ID_3 = new BinaryId( "jar3", DEFAULT_ORGANISATION, 2 );

    public static CUDFDescriptor createDescriptor()
    {
        CUDFDescriptor descriptor = new CUDFDescriptor();

        Preamble preamble = createPreamble();
        Binaries binaries = createBinaries( BINARY_ID_1, BINARY_ID_2, BINARY_ID_3 );
        Request request = createRequest( binaries.getBinaryById( BINARY_ID_1 ) );

        descriptor.setPreamble( preamble );
        descriptor.setBinaries( binaries );
        descriptor.setRequest( request );

        return descriptor;
    }

    private static Preamble createPreamble()
    {
        Preamble preamble = new Preamble();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put( "key", "value" );
        preamble.setProperties( properties );
        preamble.setReqChecksum( "req" );
        preamble.setStatusChecksum( "status" );
        preamble.setUnivChecksum( "univ" );
        return preamble;
    }

    public static Binaries createBinaries( BinaryId binaryId1, BinaryId binaryId2, BinaryId binaryId3 )
    {
        Binaries binaries = new DefaultBinaries();
        binaries.setBinaries( createBinarySet( binaryId1, binaryId2, binaryId3 ) );
        return binaries;
    }

    public static Set<Binary> createBinarySet( BinaryId binaryId1, BinaryId binaryId2, BinaryId binaryId3 )
    {
        Set<Binary> binaries = new HashSet<Binary>();
        Binary binary1 = createBinary( binaryId1, "1.0", "jar", false );
        Binary binary2 = createBinary( binaryId2, "1.0.0", "jar", false );
        Binary binary3 = createBinary( binaryId3, "1.2-SNAPSHOT", "jar", false );

        binary1.getDependencies().add( binary2 );
        binary1.getDependencies().add( binary3 );

        binaries.add( binary1 );
        binaries.add( binary2 );
        binaries.add( binary3 );
        return binaries;
    }

    public static Binary createBinary( BinaryId binaryId, String revision, String type, boolean installed )
    {
        Binary binary = new Binary( binaryId );
        binary.setInstalled( installed );
        binary.setRevision( revision );
        binary.setType( type );
        return binary;
    }

    private static Request createRequest( Binary binary1 )
    {
        Request request = new Request();
        request.getInstall().add( binary1 );
        return request;
    }
}