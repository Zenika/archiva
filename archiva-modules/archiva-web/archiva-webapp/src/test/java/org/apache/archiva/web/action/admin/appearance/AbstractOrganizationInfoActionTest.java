package org.apache.archiva.web.action.admin.appearance;

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

import org.apache.archiva.admin.repository.admin.DefaultArchivaAdministration;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.web.action.AbstractWebworkTestCase;
import org.easymock.MockControl;
import org.junit.Before;

/**
 */
public abstract class AbstractOrganizationInfoActionTest
    extends AbstractWebworkTestCase
{
    protected MockControl archivaConfigurationControl;

    protected ArchivaConfiguration configuration;

    protected AbstractAppearanceAction action;

    protected Configuration config;

    protected abstract AbstractAppearanceAction getAction();

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        config = new Configuration();
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        configuration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        configuration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, 1, 5 );

        configuration.save( config );
        archivaConfigurationControl.setVoidCallable( 1, 4 );

        archivaConfigurationControl.replay();

        DefaultArchivaAdministration defaultArchivaAdministration = new DefaultArchivaAdministration();
        defaultArchivaAdministration.setArchivaConfiguration( configuration );
        getAction().setArchivaAdministration( defaultArchivaAdministration );
    }

    protected void reloadAction()
    {
        action = getAction();
        ( (DefaultArchivaAdministration) action.getArchivaAdministration() ).setArchivaConfiguration( configuration );

    }
}
