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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.CUDFJobConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFFiles
{

    @Inject
    private ArchivaConfiguration configuration;

    public List<String> getCudfFiles(String jobId) {
        CUDFJobConfiguration cudfJobConfiguration = configuration.getConfiguration().getCudf().findCUDFJobById( jobId );
        File file = new File( cudfJobConfiguration.getLocation() );
        if (!file.isDirectory()) {
            throw new IllegalStateException( "File: " + file.getPath() + " isn't directory" );
        }
        List<String> cudfFiles = new ArrayList<String>(  );
        File[] children = file.listFiles();
        for ( File child : children )
        {
            if ( FilenameUtils.isExtension( child.getName(), "cudf" )) {
                cudfFiles.add( child.getName() );
            }
        }
        return cudfFiles;
    }

    public File getCudfFile(String jobId, String fileName) {
        CUDFJobConfiguration cudfJobConfiguration = configuration.getConfiguration().getCudf().findCUDFJobById( jobId );
        File file = new File( cudfJobConfiguration.getLocation() );
        if (!file.isDirectory()) {
            throw new IllegalStateException( "File: " + file.getPath() + " isn't directory" );
        }
        return FileUtils.getFile( file, fileName );
    }

}
