/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.search.ranking.web.internal.searcher;

import com.liferay.portal.search.ranking.web.internal.index.Ranking;
import com.liferay.portal.search.ranking.web.internal.index.RankingIndexReader;
import com.liferay.portal.search.searcher.SearchRequest;
import com.liferay.portal.search.searcher.SearchRequestBuilder;
import com.liferay.portal.search.searcher.SearchRequestBuilderFactory;
import com.liferay.portal.search.spi.searcher.SearchRequestContributor;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author André de Oliveira
 */
@Component(
	immediate = true,
	property = "search.request.contributor.id=com.liferay.portal.search.ranking",
	service = SearchRequestContributor.class
)
public class RankingSearchRequestContributor
	implements SearchRequestContributor {

	@Override
	public SearchRequest contribute(SearchRequest searchRequest) {
		Optional<Ranking> optional =
			rankingIndexReader.fetchByQueryStringOptional(
				searchRequest.getQueryString());

		return optional.map(
			ranking -> contribute(searchRequest, ranking)
		).orElse(
			searchRequest
		);
	}

	protected SearchRequest contribute(
		SearchRequest searchRequest, Ranking ranking) {

		SearchRequestBuilder searchRequestBuilder =
			searchRequestBuilderFactory.builder(searchRequest);

		rankingSearchRequestHelper.contribute(searchRequestBuilder, ranking);

		return searchRequestBuilder.build();
	}

	@Reference
	protected RankingIndexReader rankingIndexReader;

	@Reference
	protected RankingSearchRequestHelper rankingSearchRequestHelper;

	@Reference
	protected SearchRequestBuilderFactory searchRequestBuilderFactory;

}