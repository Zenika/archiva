package org.apache.maven.archiva.common.spring;

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

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusClassPathXmlApplicationContext
    extends ClassPathXmlApplicationContext
{

    // TODO enable Field injection...
    // @see http://forum.springframework.org/showthread.php?t=50181

    public PlexusClassPathXmlApplicationContext( String path, Class clazz )
        throws BeansException
    {
        super( path, clazz );
    }

    public PlexusClassPathXmlApplicationContext( String configLocation )
        throws BeansException
    {
        super( configLocation );
    }

    public PlexusClassPathXmlApplicationContext( String[] configLocations, ApplicationContext parent )
        throws BeansException
    {
        super( configLocations, parent );
    }

    public PlexusClassPathXmlApplicationContext( String[] configLocations, boolean refresh, ApplicationContext parent )
        throws BeansException
    {
        super( configLocations, refresh, parent );
    }

    public PlexusClassPathXmlApplicationContext( String[] configLocations, boolean refresh )
        throws BeansException
    {
        super( configLocations, refresh );
    }

    public PlexusClassPathXmlApplicationContext( String[] paths, Class clazz, ApplicationContext parent )
        throws BeansException
    {
        super( paths, clazz, parent );
    }

    public PlexusClassPathXmlApplicationContext( String[] paths, Class clazz )
        throws BeansException
    {
        super( paths, clazz );
    }

    public PlexusClassPathXmlApplicationContext( String[] configLocations )
        throws BeansException
    {
        super( configLocations );
    }

    /**
     * Register a custom BeanDefinitionDocumentReader to convert plexus
     * descriptors to spring bean context format.
     * <p>
     * Implementation note : validation must be disabled as plexus descriptors
     * don't use DTD / XML schemas {@inheritDoc}
     *
     * @see org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.xml.XmlBeanDefinitionReader)
     */
    @Override
    protected void loadBeanDefinitions( XmlBeanDefinitionReader reader )
        throws BeansException, IOException
    {
        reader.setDocumentReaderClass( PlexusBeanDefinitionDocumentReader.class );
        reader.setValidationMode( XmlBeanDefinitionReader.VALIDATION_NONE );
        super.loadBeanDefinitions( reader );

    }

    /**
     * Post-process the beanFactory to adapt plexus concepts to spring :
     * <ul>
     * <li>register a beanPostPorcessor to support LogEnabled interface in
     * spring context
     * </ul>
     * {@inheritDoc}
     *
     * @see org.springframework.context.support.AbstractApplicationContext#prepareBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    @Override
    protected void prepareBeanFactory( ConfigurableListableBeanFactory beanFactory )
    {
        super.prepareBeanFactory( beanFactory );

        if ( logger.isDebugEnabled() )
        {
            String[] beans = getBeanFactory().getBeanDefinitionNames();
            logger.debug( "registered beans :" );
            for ( int i = 0; i < beans.length; i++ )
            {
                logger.debug( beans[i] );
            }
        }

        // Register a bean post-processor to handle plexus Logger injection
        getBeanFactory().addBeanPostProcessor( new PlexusLogEnabledBeanPostProcessor() );
    }

}
