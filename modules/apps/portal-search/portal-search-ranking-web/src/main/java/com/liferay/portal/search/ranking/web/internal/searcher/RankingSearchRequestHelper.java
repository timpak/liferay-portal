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

import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.search.filter.ComplexQueryPart;
import com.liferay.portal.search.filter.ComplexQueryPartBuilderFactory;
import com.liferay.portal.search.query.IdsQuery;
import com.liferay.portal.search.query.Queries;
import com.liferay.portal.search.ranking.web.internal.index.Ranking;
import com.liferay.portal.search.searcher.SearchRequestBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author André de Oliveira
 */
@Component(service = RankingSearchRequestHelper.class)
public class RankingSearchRequestHelper {

	public void contribute(
		SearchRequestBuilder searchRequestBuilder, Ranking ranking) {

		searchRequestBuilder.addComplexQueryPart(
			getBlockIdsQueryPart(ranking)
		).addComplexQueryPart(
			getPinIdsQueryPart(ranking)
		);
	}

	protected ComplexQueryPart getBlockIdsQueryPart(Ranking ranking) {
		List<String> ids = ranking.getBlockIds();

		if (ids.isEmpty()) {
			return null;
		}

		return complexQueryPartBuilderFactory.builder(
		).query(
			_getIdsQuery(ids)
		).occur(
			"must_not"
		).build();
	}

	protected ComplexQueryPart getPinIdsQueryPart(Ranking ranking) {
		Set<String> ids = _getPinIds(ranking);

		if (ids.isEmpty()) {
			return null;
		}

		return complexQueryPartBuilderFactory.builder(
		).boost(
			10000F
		).query(
			_getIdsQuery(ids)
		).occur(
			"should"
		).build();
	}

	@Reference
	protected ComplexQueryPartBuilderFactory complexQueryPartBuilderFactory;

	@Reference
	protected Queries queries;

	private IdsQuery _getIdsQuery(Collection<String> ids) {
		if (ids.isEmpty()) {
			return null;
		}

		IdsQuery idsQuery = queries.ids();

		idsQuery.addIds(ArrayUtil.toStringArray(ids));

		return idsQuery;
	}

	private Set<String> _getPinIds(Ranking ranking) {
		List<Ranking.Pin> pins = ranking.getPins();

		return pins.stream(
		).map(
			Ranking.Pin::getId
		).collect(
			Collectors.toSet()
		);
	}

}