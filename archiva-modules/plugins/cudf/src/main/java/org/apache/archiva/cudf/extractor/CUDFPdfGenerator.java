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

import com.zenika.cudf.parser.DefaultDeserializer;
import com.zenika.cudf.parser.FileDeserializer;
import com.zenika.cudf.parser.PDFSerializer;
import com.zenika.cudf.parser.ParsingException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Reader;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFPdfGenerator
{
    public File generateCUDFPdf(File cudfFile) {
        try
        {
            File parentDirectory = cudfFile.getParentFile();
            File cudfPdf = new File( parentDirectory, FilenameUtils.getBaseName( cudfFile.getName() ) + ".pdf" );
            if (cudfPdf.exists() && cudfPdf.length() != 0) {
                return cudfPdf;
            } else {
                PDFSerializer pdfSerializer = new PDFSerializer( cudfPdf );
                FileDeserializer deserializer = new FileDeserializer( cudfFile );
                pdfSerializer.serialize( deserializer.deserialize() );
                return cudfPdf;
            }
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to create pdf", e );
        }
    }

    public File generateCUDFPdf(Reader reader, File outputFile) {
        try
        {
            PDFSerializer pdfSerializer = new PDFSerializer( outputFile );
            DefaultDeserializer deserializer = new DefaultDeserializer( reader );
            pdfSerializer.serialize( deserializer.deserialize() );
            return outputFile;
        }
        catch ( ParsingException e )
        {
            throw new RuntimeException( "Unable to create pdf", e );
        }
    }
}
