/**
 * ****************************************************************************
 * Copyright 2014 Virginia Polytechnic Institute and State University
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package edu.vt.vbi.patric.portlets;

import com.google.gson.Gson;
import edu.vt.vbi.patric.common.*;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.*;

public class TranscriptomicsEnrichment extends GenericPortlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptomicsEnrichment.class);

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		new SiteHelper().setHtmlMetaElements(request, response, "Pathway Summary");

		response.setContentType("text/html");
		response.setTitle("Pathway Summary");

		String pk = request.getParameter("param_key");
		Gson gson = new Gson();

		Map<String, String> key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), Map.class);

		String contextType = request.getParameter("context_type");
		String contextId = request.getParameter("context_id");
		String featureList = null;
		if (key != null && key.containsKey("feature_id")) {
			featureList = key.get("feature_id");
		}

		request.setAttribute("contextType", contextType);
		request.setAttribute("contextId", contextId);
		request.setAttribute("pk", pk);
		request.setAttribute("featureList", featureList);

		PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/TranscriptomicsEnrichment.jsp");
		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		response.setContentType("text/html");
		String callType = request.getParameter("callType");
		Gson gson = new Gson();

		switch (callType) {
		case "saveParams": {

			Map<String, String> key = new HashMap<>();
			key.put("feature_id", request.getParameter("feature_id"));

			long pk = (new Random()).nextLong();

			SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, gson.toJson(key, Map.class));

			PrintWriter writer = response.getWriter();
			writer.write("" + pk);
			writer.close();
			break;
		}
		case "getGenomeIds": {

			String featureIds = request.getParameter("feature_id");
			String pathwayId = request.getParameter("map");
			SolrInterface solr = new SolrInterface();

			String genomeId = "";
			if (featureIds != null && !featureIds.equals("") && pathwayId != null && !pathwayId.equals("")) {
				String[] listFeatureId = featureIds.split(",");
				try {
					SolrQuery query = new SolrQuery(
							"pathway_id:(" + pathwayId + ") AND feature_id:(" + StringUtils.join(listFeatureId, " OR ") + ")");
					query.addField("genome_id").setRows(listFeatureId.length);

					QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query, SolrRequest.METHOD.POST);
					SolrDocumentList sdl = qr.getResults();

					Set<String> listGenomeId = new HashSet<>();
					for (SolrDocument doc : sdl) {
						listGenomeId.add(doc.get("genome_id").toString());
					}
					genomeId = StringUtils.join(listGenomeId, ",");
				}
				catch (MalformedURLException | SolrServerException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
			PrintWriter writer = response.getWriter();
			writer.write(genomeId);
			writer.close();
			break;
		}
		case "download": {

			String _filename = "PathwaySummary";
			String _fileformat = request.getParameter("fileformat");
			String pk = request.getParameter("pk");
			Map<String, String> key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), Map.class);

			if (key != null && key.containsKey("feature_id")) {
				List<String> featureIDs = Arrays.asList(key.get("feature_id").split(","));
				JSONObject jsonResult = getEnrichmentPathway(featureIDs);

				List<String> _tbl_header = new ArrayList<>();
				List<String> _tbl_field = new ArrayList<>();
				JSONArray _tbl_source = (JSONArray) jsonResult.get("results");

				_tbl_header.addAll(Arrays.asList("Pathway Name", "# of Genes Selected	", "# of Genes Annotated", "% Coverage"));
				_tbl_field.addAll(Arrays.asList("pathway_name", "ocnt", "ecnt", "percentage"));

				ExcelHelper excel = new ExcelHelper("xssf", _tbl_header, _tbl_field, _tbl_source);
				excel.buildSpreadsheet();

				if (_fileformat.equalsIgnoreCase("xlsx")) {

					response.setContentType("application/octetstream");
					response.setProperty("Content-Disposition", "attachment; filename=\"" + _filename + "." + _fileformat + "\"");

					excel.writeSpreadsheettoBrowser(response.getPortletOutputStream());
				}
				else if (_fileformat.equalsIgnoreCase("txt")) {

					response.setContentType("application/octetstream");
					response.setProperty("Content-Disposition", "attachment; filename=\"" + _filename + "." + _fileformat + "\"");

					response.getWriter().write(excel.writeToTextFile());
				}
			}
			break;
		}
		case "getFeatureTable": {

			String pk = request.getParameter("pk");
			Map<String, String> key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), Map.class);

			if (key != null && key.containsKey("feature_id")) {

				List<String> featureIDs = Arrays.asList(key.get("feature_id").split(","));
				JSONObject jsonResult = getEnrichmentPathway(featureIDs);

				PrintWriter writer = response.getWriter();
				jsonResult.writeJSONString(writer);
				writer.close();
			}
			else {
				PrintWriter writer = response.getWriter();
				writer.write("{}");
				writer.close();
			}
			break;
		}
		}
	}

	private JSONObject getEnrichmentPathway(List<String> featureIDs) {

		SolrInterface solr = new SolrInterface();

		LOGGER.trace("# features passed:{}", featureIDs.size());

		// 1. get Pathway ID, Pathway Name & genomeID
		//solr/pathway/select?q=feature_id:(PATRIC.83332.12.NC_000962.CDS.34.1524.fwd)&fl=pathway_name,pathway_id,gid

		Map<String, JSONObject> pathwayMap = new LinkedHashMap<>();
		Set<String> listFeatureID = new HashSet<>();
		Set<String> listGenomeID = new HashSet<>();
		Set<String> listPathwayID = new HashSet<>();
		try {
			SolrQuery query = new SolrQuery("feature_id:(" + StringUtils.join(featureIDs, " OR ") + ")");
			int queryRows = Math.max(300000, (featureIDs.size() * 2));
			query.addField("pathway_name,pathway_id,genome_id,feature_id").setRows(queryRows);
			LOGGER.trace("Enrichment 1/3: {}", query.toString());

			QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query, SolrRequest.METHOD.POST);
			SolrDocumentList pathwayList = qr.getResults();

			for (SolrDocument doc : pathwayList) {
				JSONObject pw = new JSONObject();
				pw.put("pathway_id", doc.get("pathway_id"));
				pw.put("pathway_name", doc.get("pathway_name"));
				pathwayMap.put(doc.get("pathway_id").toString(), pw);

				// LOGGER.debug("{}", pw.toJSONString());
				listFeatureID.add(doc.get("feature_id").toString());
				listGenomeID.add(doc.get("genome_id").toString());
				listPathwayID.add(doc.get("pathway_id").toString());
			}
		}
		catch (MalformedURLException | SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}

		// 2. get pathway ID & Ocnt
		//solr/pathway/select?q=feature_id:(PATRIC.83332.12.NC_000962.CDS.34.1524.fwd)&rows=0&facet=true
		// &json.facet={stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:"unique(feature_id)"}}}}
		try {
			SolrQuery query = new SolrQuery("feature_id:(" + StringUtils.join(featureIDs, " OR ") + ")");
			query.setRows(0).setFacet(true);
			query.add("json.facet", "{stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:\"unique(feature_id)\"}}}}");
			LOGGER.trace("Enrichment 2/3: {}", query.toString());

			QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query, SolrRequest.METHOD.POST);
			List<SimpleOrderedMap> buckets = (List) ((SimpleOrderedMap) ((SimpleOrderedMap) qr.getResponse().get("facets")).get("stat"))
					.get("buckets");

			for (SimpleOrderedMap value : buckets) {
				String aPathwayId = value.get("val").toString();

				if (pathwayMap.containsKey(aPathwayId)) {
					pathwayMap.get(aPathwayId).put("ocnt", value.get("gene_count"));
				}
			}
		}
		catch (MalformedURLException | SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}

		// 3. with genomeID, get pathway ID & Ecnt
		//solr/pathway/select?q=genome_id:83332.12 AND pathway_id:(00230 OR 00240)&fq=annotation:PATRIC&rows=0&facet=true //&facet.mincount=1&facet.limit=-1
		// &json.facet={stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:"unique(feature_id)"}}}}
		if (!listGenomeID.isEmpty() && !listPathwayID.isEmpty()) {
			try {
				SolrQuery query = new SolrQuery(
						"genome_id:(" + StringUtils.join(listGenomeID, " OR ") + ") AND pathway_id:(" + StringUtils
								.join(listPathwayID, " OR ")
								+ ")");
				query.setRows(0).setFacet(true).addFilterQuery("annotation:PATRIC");
				query.add("json.facet", "{stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:\"unique(feature_id)\"}}}}");
				LOGGER.trace("Enrichment 3/3: {}", query.toString());

				QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query, SolrRequest.METHOD.POST);
				List<SimpleOrderedMap> buckets = (List) ((SimpleOrderedMap) ((SimpleOrderedMap) qr.getResponse().get("facets")).get("stat"))
						.get("buckets");

				for (SimpleOrderedMap value : buckets) {
					pathwayMap.get(value.get("val").toString()).put("ecnt", value.get("gene_count"));
				}
			}
			catch (MalformedURLException | SolrServerException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		// 4. Merge hash and calculate percentage on the fly
		JSONObject jsonResult = new JSONObject();
		JSONArray results = new JSONArray();
		for (JSONObject item : pathwayMap.values()) {
			if (item.get("ecnt") != null && item.get("ocnt") != null) {
				float ecnt = Float.parseFloat(item.get("ecnt").toString());
				float ocnt = Float.parseFloat(item.get("ocnt").toString());
				float percentage = ocnt / ecnt * 100;
				item.put("percentage", (int) percentage);
				results.add(item);
			}
		}
		jsonResult.put("results", results);
		jsonResult.put("total", results.size());
		jsonResult.put("featureRequested", featureIDs.size());
		jsonResult.put("featureFound", listFeatureID.size());

		return jsonResult;
	}
}
