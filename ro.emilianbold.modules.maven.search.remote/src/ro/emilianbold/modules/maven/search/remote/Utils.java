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

import java.util.Collections;
import java.util.List;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;

public class Utils {

    public static boolean isCentral(RepositoryInfo r) {
	boolean handle = "central".equals(r.getId()) && !r.isLocal() && !r.isMirror() && (r.getRepositoryUrl() != null && r.getRepositoryUrl().contains("repo.maven.apache.org")); //NOI18N

	return handle;
    }

    public static ResultImplementation<String> emptyString() {
	return create((List<String>) Collections.EMPTY_LIST);
    }

    public static ResultImplementation<NBVersionInfo> emptyResult() {
	return create((List<NBVersionInfo>) Collections.EMPTY_LIST);
    }

    public static <T> ResultImplementation<T> create(final List<T> contents) {
	return new ResultImplementation<T>() {
	    @Override
	    public boolean isPartial() {
		return false;
	    }

	    @Override
	    public void waitForSkipped() {
		//nothing to do
	    }

	    @Override
	    public List<T> getResults() {
		return contents;
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
}
