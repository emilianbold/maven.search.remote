/**
 * Copyright (c) 2016, Emilian Marius Bold
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ro.emilianbold.modules.maven.search.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.QueryField;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.ArchetypeQueries;
import org.netbeans.modules.maven.indexer.spi.BaseQueries;
import org.netbeans.modules.maven.indexer.spi.ChecksumQueries;
import org.netbeans.modules.maven.indexer.spi.ClassUsageQuery;
import org.netbeans.modules.maven.indexer.spi.ClassesQuery;
import org.netbeans.modules.maven.indexer.spi.ContextLoadedQuery;
import org.netbeans.modules.maven.indexer.spi.DependencyInfoQueries;
import org.netbeans.modules.maven.indexer.spi.GenericFindQuery;
import org.netbeans.modules.maven.indexer.spi.RepositoryIndexQueryProvider;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = RepositoryIndexQueryProvider.class, position = 100)
public class MavenCentralOnlineRepositoryIndexQueryProvider implements RepositoryIndexQueryProvider {

    public MavenCentralOnlineRepositoryIndexQueryProvider() {
    }

    @Override
    public boolean handlesRepository(RepositoryInfo r) {
	return Utils.isCentral(r);
    }

    @Override
    public GenericFindQuery getGenericFindQuery() {
	return new MavenCentralGenericFindQuery();
    }

    @Override
    public BaseQueries getBaseQueries() {
	return new BaseQueries() {
	    @Override
	    public ResultImplementation<NBVersionInfo> getRecords(String groupId, String artifactId, String version, List<RepositoryInfo> repos) {
		List<QueryField> query = new ArrayList<>();
		if (groupId != null && !groupId.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_GROUPID);
		    qf.setValue(groupId);

		    query.add(qf);
		}
		if (artifactId != null && !artifactId.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_ARTIFACTID);
		    qf.setValue(artifactId);

		    query.add(qf);
		}
		if (version != null && !version.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_VERSION);
		    qf.setValue(version);

		    query.add(qf);
		}
		return getGenericFindQuery().find(query, repos);
	    }

	    @Override
	    public ResultImplementation<NBVersionInfo> getVersions(String groupId, String artifactId, List<RepositoryInfo> repos) {
		return getRecords(groupId, artifactId, null, repos);
	    }

	    @Override
	    public ResultImplementation<String> getGroups(List<RepositoryInfo> repos) {
		return new ResultImplementation<String>() {
		    @Override
		    public boolean isPartial() {
			return true;
		    }

		    @Override
		    public void waitForSkipped() {
			//nothing
		    }

		    @Override
		    public List<String> getResults() {
			return Arrays.asList("1.Central-is-BIG-apply-a-filter",
				//top 10 groups, from http://search.maven.org/#stats , May 1st 2016
				"commons-collections",
				"commons-lang",
				"junit",
				"org.apache.maven.plugins",
				"org.apache.maven.shared",
				"org.codehaus.plexus",
				//from other articles
				"com.google.guava",
				"commons-io",
				"org.springframework"
				);
		    }

		    @Override
		    public int getTotalResultCount() {
			return getResults().size();
		    }

		    @Override
		    public int getReturnedResultCount() {
			return getResults().size();
		    }
		};
	    }

	    @Override
	    public ResultImplementation<String> getArtifacts(String groupId, List<RepositoryInfo> repos) {
		ResultImplementation<NBVersionInfo> records = getRecords(groupId, null, null, repos);

		List<String> artifacts = records.getResults()
			.stream()
			.map((NBVersionInfo info) -> info.getArtifactId())
			.distinct()
			.collect(Collectors.toList());

		return Utils.create(artifacts);
	    }

	    @Override
	    public ResultImplementation<String> filterPluginGroupIds(String prefix, List<RepositoryInfo> repos) {
		//TODO: it would be better to to a filtered search directly
		ResultImplementation<String> groups = getGroups(repos);
		List<String> filtered = groups.getResults()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.distinct()
			.collect(Collectors.toList());
		return Utils.create(filtered);
	    }

	    @Override
	    public ResultImplementation<String> filterPluginArtifactIds(String groupId, String prefix, List<RepositoryInfo> repos) {
		//TODO: it would be better to to a filtered search directly
		ResultImplementation<String> artifacts = getArtifacts(groupId, repos);
		List<String> filtered = artifacts.getResults()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.collect(Collectors.toList());
		return Utils.create(filtered);
	    }

	    @Override
	    public ResultImplementation<String> getGAVsForPackaging(String packaging, List<RepositoryInfo> repos) {
		QueryField qf = new QueryField();
		qf.setField(QueryField.FIELD_PACKAGING);
		qf.setValue(packaging);

		ResultImplementation<NBVersionInfo> results = getGenericFindQuery().find(Collections.singletonList(qf), repos);

		List<String> gavs = results.getResults()
			.stream()
			.map((NBVersionInfo info) -> info.getGroupId() + ":" + info.getArtifactId() + ":" + info.getVersion()) //NOI18N
			.distinct()
			.collect(Collectors.toList());

		return Utils.create(gavs);
	    }
	};
    }

    @Override
    public ArchetypeQueries getArchetypeQueries() {
	return null;
    }

    @Override
    public ChecksumQueries getChecksumQueries() {
	return null;
    }

    @Override
    public ClassUsageQuery getClassUsageQuery() {
	return null;
    }

    @Override
    public ClassesQuery getClassesQuery() {
	return null;
    }

    @Override
    public ContextLoadedQuery getContextLoadedQuery() {
	return null;
    }

    @Override
    public DependencyInfoQueries getDependencyInfoQueries() {
	return null;
    }

}
