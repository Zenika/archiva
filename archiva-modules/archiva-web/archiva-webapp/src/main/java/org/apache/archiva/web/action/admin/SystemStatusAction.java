package org.apache.archiva.web.action.admin;

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

import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.web.action.AbstractActionSupport;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.components.taskqueue.TaskQueue;
import org.apache.archiva.redback.integration.interceptor.SecureAction;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;

/**
 * Shows system status information for the administrator.
 *
 *
 */
@Controller( "systemStatus" )
@Scope( "prototype" )
public class SystemStatusAction
    extends AbstractActionSupport
    implements SecureAction
{

    private Map<String, TaskQueue> queues;

    private Map<String, Cache> caches;

    @Inject
    private RepositoryScanner scanner;

    private String memoryStatus;

    private String cacheKey;

    @PostConstruct
    public void initialize()
    {
        super.initialize();
        queues = getBeansOfType( TaskQueue.class );
        caches = getBeansOfType( Cache.class );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String execute()
    {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long total = runtime.totalMemory();
        long used = total - runtime.freeMemory();
        long max = runtime.maxMemory();
        memoryStatus = formatMemory( used ) + "/" + formatMemory( total ) + " (Max: " + formatMemory( max ) + ")";

        return SUCCESS;
    }
    
    public String flush()
    {
        if( !StringUtils.isEmpty( cacheKey ) )
        {
            Cache cache = caches.get( cacheKey );
            cache.clear();
        }

        return SUCCESS;
    }

    private static String formatMemory( long l )
    {
        return l / ( 1024 * 1024 ) + "M";
    }

    public String getMemoryStatus()
    {
        return memoryStatus;
    }

    public RepositoryScanner getScanner()
    {
        return scanner;
    }

    public Map<String, Cache> getCaches()
    {
        return caches;
    }

    public Map<String, TaskQueue> getQueues()
    {
        return queues;
    }

    public String getCacheKey()
    {
        return cacheKey;
    }

    public void setCacheKey( String cacheKey )
    {
        this.cacheKey = cacheKey;
    }
}
