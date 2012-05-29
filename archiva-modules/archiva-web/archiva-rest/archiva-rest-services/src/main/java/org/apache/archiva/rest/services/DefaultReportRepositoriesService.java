package org.apache.archiva.rest.services;

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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ReportRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DefaultReportRepositoriesService
 *
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 * @since 2012-05-28 10:34
 */
@Service( "reportRepositoriesService#rest" )
public class DefaultReportRepositoriesService
    extends AbstractRestService
    implements ReportRepositoriesService
{

    private static final String ALL_REPOSITORIES = "all";

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    public List<RepositoryStatistics> getStatisticsReport( List<String> repositoriesId, int rowCount, Date startDate,
                                                           Date endDate )
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            return Collections.emptyList();
        }
        finally
        {
            repositorySession.close();
        }
    }

    public Map<String, List<RepositoryProblemFacet>> getHealthReport( String repository, String groupId, int rowCount )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            List<String> observableRepositories = getObservableRepos();
            if ( !ALL_REPOSITORIES.equals( repository ) && !observableRepositories.contains( repository ) )
            {
                throw new ArchivaRestServiceException(
                    "${$.i18n.prop('report.repository.illegal-access', " + repository + ")}", "repositoryId",
                    new IllegalAccessException() );
            }

            if ( !ALL_REPOSITORIES.equals( repository ) )
            {
                observableRepositories = Collections.<String>singletonList( repository );
            }

            List<RepositoryProblemFacet> problemArtifacts = new ArrayList<RepositoryProblemFacet>();
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( String repoId : observableRepositories )
            {
                for ( String name : metadataRepository.getMetadataFacets( repoId, RepositoryProblemFacet.FACET_ID ) )
                {
                    RepositoryProblemFacet metadataFacet =
                        (RepositoryProblemFacet) metadataRepository.getMetadataFacet( repoId,
                                                                                      RepositoryProblemFacet.FACET_ID,
                                                                                      name );
                    if ( StringUtils.isEmpty( groupId ) || groupId.equals( metadataFacet.getNamespace() ) )
                    {
                        problemArtifacts.add( metadataFacet );
                    }
                }
            }
            Map<String, List<RepositoryProblemFacet>> repositoriesMap =
                new TreeMap<String, List<RepositoryProblemFacet>>();
            for ( RepositoryProblemFacet problem : problemArtifacts )
            {
                List<RepositoryProblemFacet> problemsList;
                if ( repositoriesMap.containsKey( problem.getRepositoryId() ) )
                {
                    problemsList = repositoriesMap.get( problem.getRepositoryId() );
                }
                else
                {
                    problemsList = new ArrayList<RepositoryProblemFacet>();
                    repositoriesMap.put( problem.getRepositoryId(), problemsList );
                }

                problemsList.add( problem );
            }

            return repositoriesMap;
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }
}
