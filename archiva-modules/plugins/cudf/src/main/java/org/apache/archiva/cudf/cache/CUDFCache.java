package org.apache.archiva.cudf.cache;

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

import com.zenika.cudf.adapter.cache.Cache;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFCache
    implements Cache
{

    @Inject
    @Named( "cache#cudf" )
    private org.apache.archiva.redback.components.cache.Cache cache;


    public void put( Object key, Object value )
    {
        cache.put( key, value );
    }

    public Object get( Object key )
    {
        return cache.get( key );
    }
}
