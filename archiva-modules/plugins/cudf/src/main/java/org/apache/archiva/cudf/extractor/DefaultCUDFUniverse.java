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

import com.zenika.cudf.adapter.cache.CachedBinaries;
import com.zenika.cudf.model.CUDFDescriptor;
import org.apache.archiva.cudf.cache.CUDFCache;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service("universe#default")
public class DefaultCUDFUniverse
    implements CUDFUniverse
{

    @Inject
    private CUDFCache cache;

    @Inject
    private CUDFUniverseLoader universeLoader;

    private CUDFDescriptor descriptor;

    public void loadUniverse( List<String> repositoryIds )
    {
        descriptor = universeLoader.loadUniverse( repositoryIds );
    }

    public boolean isLoaded()
    {
        return cache.get( CachedBinaries.BINARY_ID_KEY_LIST ) != null && !( (Set) cache.get(
            CachedBinaries.BINARY_ID_KEY_LIST ) ).isEmpty();
    }

    public CUDFDescriptor getDescriptor()
    {
        return descriptor;
    }
}
