package org.apache.maven.archiva.web.validator;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.validator.validators.ValidatorSupport;
import com.opensymphony.xwork.validator.ValidationException;
import com.opensymphony.xwork.validator.ValidatorContext;

/**
 * Validator for synced repository form. The values to be validated depends on the
 * selected sync method to be used.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class SyncedRepositoryValidator
    extends ValidatorSupport
{

    public void validate( Object obj )
        throws ValidationException
    {

        String method = ( String ) getFieldValue( "method", obj );
        ValidatorContext ctxt = getValidatorContext();

        if( method.equals( "rsync" ) )
        {
            String rsyncHost = ( String ) getFieldValue( "rsyncHost", obj );
            if( rsyncHost == null || rsyncHost.equals("") )
            {
                ctxt.addActionError( "Rsync host is required." );
            }

            String rsyncDirectory = ( String ) getFieldValue( "rsyncDirectory", obj );
            if( rsyncDirectory == null || rsyncDirectory.equals("") )
            {
                ctxt.addActionError( "Rsync directory is required." );
            }

            String rsyncMethod = ( String ) getFieldValue( "rsyncMethod", obj );
            if( rsyncMethod == null || rsyncMethod.equals("") )
            {
                ctxt.addActionError( "Rsync method is required." );
            }
            else
            {
                if( !rsyncMethod.equals( "anonymous" ) && !rsyncMethod.equals( "ssh" ) )
                {
                    ctxt.addActionError( "Invalid rsync method" );
                }
            }

            String username = ( String ) getFieldValue( "username", obj );
            if( username == null || username.equals("") )
            {
                ctxt.addActionError( "Username is required." );
            }

        }
        else if ( method.equals( "svn" ) )
        {
            String svnUrl = ( String ) getFieldValue( "svnUrl", obj );
            if( svnUrl == null || svnUrl.equals("") )
            {
                ctxt.addActionError( "SVN url is required." );
            }

            String username = ( String ) getFieldValue( "username", obj );
            if( username == null || username.equals("") )
            {
                ctxt.addActionError( "Username is required." );
            }
        }
        else if ( method.equals( "cvs" ) )
        {
            String cvsRoot = ( String ) getFieldValue( "cvsRoot", obj );
            if( cvsRoot == null || cvsRoot.equals("") )
            {
                ctxt.addActionError( "CVS root is required." );
            }
        }
        else if ( method.equals( "file" ) )
        {
            String directory = ( String ) getFieldValue( "directory", obj );
            if( directory == null || directory.equals("") )
            {
                ctxt.addActionError( "Directory is required." );
            }
        }

        if( ctxt.hasActionErrors() )
        {
            return;
        }
    }

}
