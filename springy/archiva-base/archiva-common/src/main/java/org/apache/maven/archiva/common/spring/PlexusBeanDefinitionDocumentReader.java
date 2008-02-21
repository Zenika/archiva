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

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Document;

/**
 * A Spring {@link BeanDefinitionDocumentReader} that converts on the fly the
 * Plexus components descriptor to a spring XML context.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusBeanDefinitionDocumentReader
    extends DefaultBeanDefinitionDocumentReader
{

    public void registerBeanDefinitions( Document doc, XmlReaderContext readerContext )
    {
        doc = convertPlexusDescriptorToSpringBeans( doc );
        super.registerBeanDefinitions( doc, readerContext );
    }

    protected Document convertPlexusDescriptorToSpringBeans( Document doc )
    {
        if ( ! "component-set".equals( doc.getDocumentElement().getNodeName() ) )
        {
            return doc;
        }

        try
        {
            Source xmlSource = new DOMSource( doc );
            InputStream is = getClass().getResourceAsStream( "plexus2spring.xsl" );
            Source xsltSource = new StreamSource( is );

            DOMResult transResult = new DOMResult();

            // FIXME : uses Xalan extension. need either to force Xalan as Transformer or
            // register a XpathFunctionResolver (how ?)
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer( xsltSource );
            t.transform( xmlSource, transResult );

            return (Document) transResult.getNode();
        }
        catch ( Exception e )
        {
            // FIXME log the error;
            return doc;
        }
    }
}
