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

import com.zenika.cudf.model.Binary;
import com.zenika.cudf.model.BinaryId;
import com.zenika.cudf.resolver.VersionResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.apache.archiva.cudf.extractor.CUDFTestUtils.*;
import static org.easymock.EasyMock.*;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
public class TestCUDFBinaryCheck
{

    private CUDFUniverse universe;

    private VersionResolver versionResolver;

    private CUDFBinaryCheck binaryCheck;

    @Before
    public void setUp()
    {
        universe = createMock( CUDFUniverse.class );
        versionResolver = createMock( VersionResolver.class );

        binaryCheck = new CUDFBinaryCheck();
        binaryCheck.setUniverse( universe );
        binaryCheck.setVersionResolver( versionResolver );
    }

    @Test
    public void testCheckPackagesAreInUniverse()
        throws Exception
    {
        expect( universe.getDescriptor() ).andStubReturn( createDescriptor() );

        expect( versionResolver.resolveToCUDF(
            createBinary( new BinaryId( "jar1", DEFAULT_ORGANISATION, 0 ), "1.0", "jar", false ) ) ).andReturn(
            createBinary( new BinaryId( "jar1", DEFAULT_ORGANISATION, 1 ), "1.0", "jar", false ) ).atLeastOnce();
        expect( versionResolver.resolveToCUDF(
            createBinary( new BinaryId( "jar2", DEFAULT_ORGANISATION, 0 ), "1.0.0", "jar", false ) ) ).andReturn(
            createBinary( new BinaryId( "jar2", DEFAULT_ORGANISATION, 1 ), "1.0.0", "jar", false ) ).atLeastOnce();
        expect( versionResolver.resolveToCUDF(
            createBinary( new BinaryId( "jar3", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar", false ) ) ).andReturn(
            createBinary( new BinaryId( "jar3", DEFAULT_ORGANISATION, 2 ), "1.2-SNAPSHOT", "jar",
                          false ) ).atLeastOnce();

        replay( universe );
        replay( versionResolver );

        Set<Binary> binaries = createBinarySet( new BinaryId( "jar1", DEFAULT_ORGANISATION, 0 ),
                                                new BinaryId( "jar2", DEFAULT_ORGANISATION, 0 ),
                                                new BinaryId( "jar3", DEFAULT_ORGANISATION, 0 ) );

        binaryCheck.checkPackagesAreInUniverse( binaries );

        verify( versionResolver );
    }

    @Test( expected = IllegalStateException.class )
    public void testCheckPackagesAreNotInUniverse()
    {
        expect( universe.getDescriptor() ).andStubReturn( createDescriptor() );

        expect( versionResolver.resolveToCUDF(
            createBinary( new BinaryId( "NotInUniverse", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar",
                          false ) ) ).andReturn(
            createBinary( new BinaryId( "NotInUniverse", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar", false ) );

        replay( universe, versionResolver );

        Set<Binary> binaries = new HashSet<Binary>();
        binaries.add(
            createBinary( new BinaryId( "NotInUniverse", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar", false ) );

        binaryCheck.checkPackagesAreInUniverse( binaries );
    }

    @Test( expected = IllegalStateException.class )
    public void testCheckPackagesWithUnknownVersion()
    {
        expect( universe.getDescriptor() ).andStubReturn( createDescriptor() );

        expect( versionResolver.resolveToCUDF(
            createBinary( new BinaryId( "NotInUniverse", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar",
                          false ) ) ).andReturn( null );

        replay( universe, versionResolver );

        Set<Binary> binaries = new HashSet<Binary>();
        binaries.add(
            createBinary( new BinaryId( "NotInUniverse", DEFAULT_ORGANISATION, 0 ), "1.2-SNAPSHOT", "jar", false ) );

        binaryCheck.checkPackagesAreInUniverse( binaries );
    }
}
