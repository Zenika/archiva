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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.integration.interceptor.SecureAction;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 *
 */
@Controller( "editOrganisationInfo" )
@Scope( "prototype" )
public class EditOrganisationInfoAction
    extends AbstractAppearanceAction
    implements SecureAction, Validateable
{
    @Override
    public String execute()
        throws RepositoryAdminException
    {

        OrganisationInformation orgInfo = archivaAdministration.getOrganisationInformation();

        orgInfo.setLogoLocation( getOrganisationLogo() );
        orgInfo.setName( getOrganisationName() );
        orgInfo.setUrl( getOrganisationUrl() );

        archivaAdministration.setOrganisationInformation( orgInfo );
        return SUCCESS;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );
        return bundle;
    }

    public void validate()
    {
        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( super.getOrganisationName() ) )
        {
            super.setOrganisationName( super.getOrganisationName().trim() );
        }

        if ( StringUtils.isNotEmpty( super.getOrganisationUrl() ) )
        {
            super.setOrganisationUrl( super.getOrganisationUrl().trim() );
        }

        if ( StringUtils.isNotEmpty( super.getOrganisationLogo() ) )
        {
            super.setOrganisationLogo( super.getOrganisationLogo().trim() );
        }
    }
}
