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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openide.util.NbBundle.Messages;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.QueryField;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.GenericFindQuery;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;

/* package protected */ class MavenCentralGenericFindQuery implements GenericFindQuery {
    private final OkHttpClient client;
    
    private final static int SEARCH_ROWS = 500;

    public MavenCentralGenericFindQuery(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public ResultImplementation<NBVersionInfo> find(List<QueryField> fields, final List<RepositoryInfo> repos) {
	assert repos.stream().anyMatch((RepositoryInfo r) -> Utils.isCentral(r));

	QueryField nameField = null;

	QueryField groupField = null;
	QueryField artifactField = null;
	QueryField versionField = null;

	QueryField packagingField = null;
	for (QueryField field : fields) {
	    if (QueryField.FIELD_NAME.equals(field.getField())) {
		nameField = field;
	    }
	    if (QueryField.FIELD_GROUPID.equals(field.getField())) {
		groupField = field;
	    }
	    if (QueryField.FIELD_ARTIFACTID.equals(field.getField())) {
		artifactField = field;
	    }
	    if (QueryField.FIELD_VERSION.equals(field.getField())) {
		versionField = field;
	    }
	    if (QueryField.FIELD_PACKAGING.equals(field.getField())) {
		packagingField = field;
	    }
	}

	if (nameField != null) {
	    //Since the search seems to be similar to this
	    // https://repository.sonatype.org/nexus-indexer-lucene-plugin/default/docs/path__lucene_search.html group/artifact/version is ignored anyhow
	    String mavenSearchURLText = "http://search.maven.org/solrsearch/select?rows=" + SEARCH_ROWS + "&wt=json&q=" //NOI18N
		    + encode(nameField.getValue());
	    final List<NBVersionInfo> results = queryCentralRepository(mavenSearchURLText);
	    return Utils.create(results);
	}

	if (packagingField != null) {
	    //according to https://repository.sonatype.org/nexus-indexer-lucene-plugin/default/docs/path__lucene_search.html , "can be combined with g, a, v & c params as well"
	    //but getGAVsForPackaging does not send such parameters
	    if (groupField != null || artifactField != null || versionField != null) {
		Logger.getLogger(MavenCentralGenericFindQuery.class.getName()).log(Level.WARNING, "Maven packaging search will ignore group/artifact/version fields");
	    }
	    String mavenSearchURLText = "http://search.maven.org/solrsearch/select?rows=" + SEARCH_ROWS + "&wt=json&core=gav&q="; //NOI18N
	    mavenSearchURLText += "p:%22"; //NOI18N
	    mavenSearchURLText += encode(packagingField.getValue());
	    mavenSearchURLText += "%22"; //NOI18N

	    final List<NBVersionInfo> results = queryCentralRepository(mavenSearchURLText);
	    return Utils.create(results);
	}

	if (groupField != null || artifactField != null || versionField != null) {
	    String mavenSearchURLText = "http://search.maven.org/solrsearch/select?rows=" + SEARCH_ROWS + "&wt=json&core=gav&q="; //NOI18N
	    boolean first = true;
	    if (groupField != null) {
		first = false;

		mavenSearchURLText += "g:%22"; //NOI18N
		mavenSearchURLText += encode(groupField.getValue());
		mavenSearchURLText += "%22"; //NOI18N
	    }
	    if (artifactField != null) {
		if (!first) {
		    mavenSearchURLText += "%20AND%20"; //NOI18N
		}
		first = false;

		mavenSearchURLText += "a:%22"; //NOI18N
		mavenSearchURLText += encode(artifactField.getValue());
		mavenSearchURLText += "%22"; //NOI18N
	    }
	    if (versionField != null) {
		if (!first) {
		    mavenSearchURLText += "%20AND%20"; //NOI18N
		}
		first = false;

		mavenSearchURLText += "v:%22"; //NOI18N
		mavenSearchURLText += encode(versionField.getValue());
		mavenSearchURLText += "%22"; //NOI18N
	    }

	    final List<NBVersionInfo> results = queryCentralRepository(mavenSearchURLText);
	    return Utils.create(results);
	}

	//fallback
//	System.out.println("Searching N/A");

	//a search without the central repository
	return Utils.emptyResult();
    }

    @Messages({
	"# {0} - URL",
	"query.central.url=Querying Maven central: {0}",
	"query.parsing=Querying Maven central: parsing results"
    })
    private List<NBVersionInfo> queryCentralRepository(String mavenSearchURLText) {
//	System.out.println("Searching " + mavenSearchURLText);

	ProgressHandle ph = ProgressHandle.createHandle(Bundle.query_central_url(mavenSearchURLText));
	ph.start();
	try {
	    URL u = new URL(mavenSearchURLText);

            Request okRequest = new Request.Builder()
                    .url(u)
                    .cacheControl(new CacheControl.Builder()
                            .maxStale(5, TimeUnit.MINUTES)
                            .build())
                    .build();

            Response okResponse = client.newCall(okRequest).execute();

	    try (InputStream in = okResponse.body().byteStream()) {
		ph.progress(Bundle.query_parsing());
		Object parse = JSONValue.parse(new BufferedReader(new InputStreamReader(in)));
		if (!(parse instanceof JSONObject)) {
		    return Collections.EMPTY_LIST;
		}
		JSONObject searchJSON = (JSONObject) parse;
		Object responseObj = searchJSON.get("response"); //NOI18N
		if (!(responseObj instanceof JSONObject)) {
		    return Collections.EMPTY_LIST;
		}
		JSONObject response = (JSONObject) responseObj;
		Object docsObj = response.get("docs"); //NOI18N
		if (!(docsObj instanceof JSONArray)) {
		    return Collections.EMPTY_LIST;
		}
		JSONArray docs = (JSONArray) docsObj;

		List<NBVersionInfo> infos = new ArrayList<>();
		for (int i = 0; i < docs.size(); i++) {
		    Object docObj = docs.get(i);
		    JSONObject doc = (JSONObject) docObj;

		    infos.addAll(jsonToInfo(doc));
		}

		return infos;
	    }
	} catch (MalformedURLException ex) {
	    Logger.getLogger(MavenCentralGenericFindQuery.class.getName()).log(Level.SEVERE, null, ex);
	} catch (IOException ex) {
	    Logger.getLogger(MavenCentralGenericFindQuery.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    ph.finish();
	}
	return Collections.EMPTY_LIST;
    }

    private static List<NBVersionInfo> jsonToInfo(JSONObject doc) {
	List<NBVersionInfo> infos = new ArrayList<>();

	String groupId = String.valueOf(doc.get("g")); //NOI18N
	String artifactId = String.valueOf(doc.get("a")); //NOI18N
	Object v = doc.get("latestVersion"); //NOI18N
	if (v == null) {
	    v = doc.get("v"); //NOI18N
	}
	String version = String.valueOf(v);
	String packaging = String.valueOf(doc.get("p")); //NOI18N

	//"ec":["-sources.jar","-javadoc.jar",".jar",".pom"]
	Pattern p = Pattern.compile("^(-([^\\.]+))*\\.(.*)$"); //NOI18N
	Object ecObj = doc.get("ec"); //NOI18N
	if (ecObj instanceof JSONArray) {
	    JSONArray ec = (JSONArray) ecObj;
	    for (Object o : ec) {
		String s = String.valueOf(o);

		Matcher matcher = p.matcher(s);
		if (matcher.matches()) {
		    String classifier = matcher.group(2);
		    String extension = matcher.group(3);

		    if ("javadoc".equals(classifier) || "sources".equals(classifier)) { //NOI18N
			continue;
		    }

		    infos.add(new NBVersionInfo(
			    "central", //NOI18N
			    groupId, artifactId, version,
			    extension,
			    packaging,
			    String.valueOf(doc.get("id")), null, classifier)); //NOI18N
		}
	    }
	}

	if (infos.isEmpty()) {
	    infos.add(new NBVersionInfo(
		    "central", //NOI18N
		    groupId, artifactId, version,
		    //guesswork
		    "jar", //NOI18N
		    packaging,
		    String.valueOf(doc.get("id")), null, null)); //NOI18N
	}

	return infos;
    }

    private static String encode(String s) {
	try {
	    return URLEncoder.encode(s, "UTF-8"); //NOI18N
	} catch (UnsupportedEncodingException ex) {
	    //UTF-8 should always(?) be a supported encoding
	    Logger.getLogger(MavenCentralGenericFindQuery.class.getName()).log(Level.SEVERE, null, ex);
	    return ""; //NOI18N
	}
    }
}
