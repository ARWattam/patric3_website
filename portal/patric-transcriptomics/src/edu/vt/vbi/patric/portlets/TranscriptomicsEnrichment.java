/*******************************************************************************
 * Copyright 2014 Virginia Polytechnic Institute and State University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.vt.vbi.patric.portlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.*;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import edu.vt.vbi.patric.common.SolrCore;
import edu.vt.vbi.patric.common.SolrInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.vt.vbi.patric.common.SiteHelper;
import edu.vt.vbi.patric.dao.DBTranscriptomics;
import edu.vt.vbi.patric.dao.ResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranscriptomicsEnrichment extends GenericPortlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptomicsEnrichment.class);

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		new SiteHelper().setHtmlMetaElements(request, response, "Pathway Summary");

		response.setContentType("text/html");
		response.setTitle("Pathway Summary");

		String pk = request.getParameter("param_key");

		PortletSession session = request.getPortletSession(true);
		Map<String, String> key = (Map<String, String>) session.getAttribute("key"+pk, PortletSession.APPLICATION_SCOPE);
		String contextType = request.getParameter("context_type");
		String contextId = request.getParameter("context_id");
		String featureList = key.get("feature_id");

		request.setAttribute("contextType", contextType);
		request.setAttribute("contextId", contextId);
		request.setAttribute("pk", pk);
		request.setAttribute("featureList", featureList);

		PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/TranscriptomicsEnrichment.jsp");
		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest req, ResourceResponse resp) throws PortletException, IOException {

		resp.setContentType("text/html");
		String callType = req.getParameter("callType");

		if (callType.equals("saveParams")) {

			Map<String, String> key = new HashMap<>();
			key.put("feature_id", req.getParameter("feature_id"));

			LOGGER.debug("Enrichment params: {}", key);

			Random g = new Random();
			int random = g.nextInt();

			PortletSession session = req.getPortletSession(true);
			session.setAttribute("key" + random, key, PortletSession.APPLICATION_SCOPE);

			PrintWriter writer = resp.getWriter();
			writer.write("" + random);
			writer.close();
		}
//
//		if (callType.equals("getGenomeIds")) {
//
//			HashMap<String, String> key = new HashMap<>();
//
//			key.put("feature_info_id", req.getParameter("feature_info_id"));
//			key.put("map", req.getParameter("map"));
//			DBTranscriptomics conn_transcriptomics = new DBTranscriptomics();
//			String genomeId = conn_transcriptomics.getGenomeListFromFeatureIds(key, 0, -1);
//
//			PrintWriter writer = resp.getWriter();
//			writer.write(genomeId);
//			writer.close();
//		}
		else if (callType.equals("getFeatureTable")) {

			PortletSession session = req.getPortletSession();

			String pk = req.getParameter("pk");

			// TODO: implement sort and paging with solr query
//			String start_id = req.getParameter("start");
//			String limit = req.getParameter("limit");
//			int start = Integer.parseInt(start_id);
//			int end = start + Integer.parseInt(limit);
//
			Map<String, String> key = (Map<String, String>) session.getAttribute("key" + pk, PortletSession.APPLICATION_SCOPE);
//			// sorting
//			JSONParser a = new JSONParser();
//			JSONArray sorter;
//			String sort_field = "";
//			String sort_dir = "";
//			try {
//				sorter = (JSONArray) a.parse(req.getParameter("sort"));
//				sort_field += ((JSONObject) sorter.get(0)).get("property").toString();
//				sort_dir += ((JSONObject) sorter.get(0)).get("direction").toString();
//				for (int i = 1; i < sorter.size(); i++) {
//					sort_field += "," + ((JSONObject) sorter.get(i)).get("property").toString();
//				}
//			}
//			catch (ParseException e) {
//				LOGGER.error(e.getMessage(), e);
//			}
//
//			HashMap<String, String> sort = new HashMap<>();
//
//			if (!sort_field.equals("") && !sort_dir.equals("")) {
//				sort.put("field", sort_field);
//				sort.put("direction", sort_dir);
//			}

			SolrInterface solr = new SolrInterface();
			List<String> featureIDs = Arrays.asList(key.get("feature_id").split(","));

			// 1. get Pathway ID, Pathway Name & genomeID
			//solr/pathway/select?q=feature_id:(PATRIC.83332.12.NC_000962.CDS.34.1524.fwd)&fl=pathway_name,pathway_id,gid

			Map<String, JSONObject> pathwayMap = new LinkedHashMap<>();
			Set<String> listFeatureID = new HashSet<>();
			Set<String> listGenomeID = new HashSet<>();
			Set<String> listPathwayID = new HashSet<>();
			try {
				SolrQuery query = new SolrQuery("feature_id:(" + StringUtils.join(featureIDs, " OR ") + ")");
				query.addField("pathway_name,pathway_id,genome_id,feature_id");
				LOGGER.debug("Enrichment 1/3: {}", query.toString());

				QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query);
				SolrDocumentList pathwayList = qr.getResults();

				for (SolrDocument doc: pathwayList) {
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
				LOGGER.debug("Enrichment 2/3: {}", query.toString());

				QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query);
				List<SimpleOrderedMap> buckets = (List) ((SimpleOrderedMap) ((SimpleOrderedMap) qr.getResponse().get("facets")).get("stat")).get("buckets");

				for (SimpleOrderedMap value : buckets) {
					pathwayMap.get(value.get("val").toString()).put("ocnt", value.get("gene_count"));
				}
			}
			catch (MalformedURLException | SolrServerException e) {
				LOGGER.error(e.getMessage(), e);
			}

			// 3. with genomeID, get pathway ID & Ecnt
			//solr/pathway/select?q=genome_id:83332.12 AND pathway_id:(00230 OR 00240)&fq=annotation:PATRIC&rows=0&facet=true //&facet.mincount=1&facet.limit=-1
			// &json.facet={stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:"unique(feature_id)"}}}}
			try {
				SolrQuery query = new SolrQuery("genome_id:(" + StringUtils.join(listGenomeID, " OR") + ") AND pathway_id:(" + StringUtils.join(listPathwayID, " OR ") + ")");
				query.setRows(0).setFacet(true).addFilterQuery("annotation:PATRIC");
				query.add("json.facet", "{stat:{field:{field:pathway_id,limit:-1,facet:{gene_count:\"unique(feature_id)\"}}}}");
				LOGGER.debug("Enrichment 3/3: {}", query.toString());

				QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query);
				List<SimpleOrderedMap> buckets = (List) ((SimpleOrderedMap) ((SimpleOrderedMap) qr.getResponse().get("facets")).get("stat")).get("buckets");

				for (SimpleOrderedMap value : buckets) {
					pathwayMap.get(value.get("val").toString()).put("ecnt", value.get("gene_count"));
				}
			}
			catch (MalformedURLException | SolrServerException e) {
				LOGGER.error(e.getMessage(), e);
			}

			// 4. Merge hash and calculate percentage on the fly
			JSONObject jsonResult = new JSONObject();
			JSONArray results = new JSONArray();
			for (JSONObject item : pathwayMap.values()) {
				float ecnt = Float.parseFloat(item.get("ecnt").toString());
				float ocnt = Float.parseFloat(item.get("ocnt").toString());
				float percentage = ocnt / ecnt * 100;
				item.put("percentage", (int) percentage);
				results.add(item);
			}
			jsonResult.put("results", results);
			jsonResult.put("total", results.size());
			jsonResult.put("featureRequested", featureIDs.size());
			jsonResult.put("featureFound", listFeatureID.size());

//			DBTranscriptomics conn_transcriptomics = new DBTranscriptomics();
//			int count_total = conn_transcriptomics.getPathwayEnrichmentCount(key);
//			List<ResultType> items = conn_transcriptomics.getPathwayEnrichmentList(key, sort, start, end);
//
//			JSONObject jsonResult = new JSONObject();
//			try {
//				jsonResult.put("total", count_total);
//				JSONArray results = new JSONArray();
//
//				for (ResultType item : items) {
//					JSONObject obj = new JSONObject();
//					obj.putAll(item);
//					results.add(obj);
//				}
//				jsonResult.put("results", results);
//			}
//			catch (Exception ex) {
//				LOGGER.error(ex.getMessage(), ex);
//			}

			PrintWriter writer = resp.getWriter();
			jsonResult.writeJSONString(writer);
			writer.close();
		}
	}
}
