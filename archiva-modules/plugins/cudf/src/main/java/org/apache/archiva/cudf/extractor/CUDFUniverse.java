package org.apache.archiva.cudf.extractor;

import com.zenika.cudf.model.CUDFDescriptor;

import java.util.List;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
public interface CUDFUniverse
{
    void loadUniverse( List<String> repositoryIds );

    boolean isLoaded();

    CUDFDescriptor getDescriptor();
}
