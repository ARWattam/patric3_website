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
import com.google.gson.internal.LinkedTreeMap;
import edu.vt.vbi.patric.beans.Genome;
import edu.vt.vbi.patric.beans.GenomeFeature;
import edu.vt.vbi.patric.common.*;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class PathwayFinder extends GenericPortlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(PathwayFinder.class);

	private ObjectReader jsonReader;

	private ObjectWriter jsonWriter;

	@Override
	public void init() throws PortletException {
		super.init();

		ObjectMapper objectMapper = new ObjectMapper();
		jsonReader = objectMapper.reader(Map.class);
		jsonWriter = objectMapper.writerWithType(Map.class);
	}

	public boolean isLoggedIn(PortletRequest request) {

		String sessionId = request.getPortletSession(true).getId();
		Gson gson = new Gson();
		LinkedTreeMap sessionMap = gson.fromJson(SessionHandler.getInstance().get(sessionId), LinkedTreeMap.class);

		return sessionMap.containsKey("authorizationToken");
	}

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		response.setContentType("text/html");
		PortletRequestDispatcher prd;
		response.setTitle("Comparative Pathway Tool");
		SiteHelper.setHtmlMetaElements(request, response, "Comparative Pathway Tool");

		String mode = request.getParameter("display_mode");

		if (mode != null && mode.equals("result")) {

			String contextType = request.getParameter("context_type");
			String contextId = request.getParameter("context_id");

			String pk = request.getParameter("param_key");
			String ecNumber = request.getParameter("ec_number");
			String annotation = request.getParameter("algorithm");
			String pathwayId = request.getParameter("map");

			Map<String, String> key = jsonReader.readValue(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk));

			String searchOn = "";
			String keyword = "";
			String genomeId = "";
			String taxonId = "";

			if (key != null && key.containsKey("search_on")) {
				searchOn = key.get("search_on");
			}
			if (key != null && key.containsKey("taxonId")) {
				taxonId = key.get("taxonId");
			}
			if (key != null && key.containsKey("genomeId")) {
				genomeId = key.get("genomeId");
			}
			if (searchOn.equalsIgnoreCase("Keyword") && key != null && key.get("keyword") != null) {
				keyword = key.get("keyword");
			}

			request.setAttribute("contextType", contextType);
			request.setAttribute("contextId", contextId);

			request.setAttribute("pk", pk);
			request.setAttribute("searchOn", searchOn);
			request.setAttribute("ecNumber", ecNumber);
			request.setAttribute("annotation", annotation);
			request.setAttribute("pathwayId", pathwayId);
			request.setAttribute("keyword", keyword);

			request.setAttribute("genomeId", genomeId);
			request.setAttribute("taxonId", taxonId);
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/comp_pathway_finder_result.jsp");
		}
		else if (mode != null && mode.equals("featurelist")) {

			String pk = request.getParameter("param_key");
			Map<String, String> key = jsonReader.readValue(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk));

			String ecNumber = request.getParameter("ec_number");
			String annotation = request.getParameter("algorithm");
			String pathwayId = request.getParameter("map");

			if (ecNumber != null && !ecNumber.equals("")) {
				key.put("ec_number", ecNumber);
			}
			if (annotation != null && !annotation.equals("")) {
				key.put("algorithm", annotation);
			}
			if (pathwayId != null && !pathwayId.equals("")) {
				key.put("map", pathwayId);
			}

			SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, jsonWriter.writeValueAsString(key));

			request.setAttribute("pk", pk);

			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/comp_pathway_finder_result.jsp");
		}
		else {

			DataApiHandler dataApi = new DataApiHandler(request);
			String taxonId;
			String genomeId = "";
			String contextType = request.getParameter("context_type");
			String contextId = request.getParameter("context_id");

			if (contextType == null || contextType.equals("")) {
				contextType = "taxon";
			}
			else {
				if (contextId == null || contextId.equals("")) {
					contextId = "131567";
				}
			}

			String taxonName;
			if (contextType.equals("genome")) {
				genomeId = contextId;
				Genome genome = dataApi.getGenome(genomeId);
				taxonId = "" + genome.getTaxonId();
				taxonName = dataApi.getTaxonomy(genome.getTaxonId()).getTaxonName();
			}
			else {
				taxonId = contextId;
				taxonName = dataApi.getTaxonomy(Integer.parseInt(taxonId)).getTaxonName();
			}

			boolean isLoggedIn = isLoggedIn(request);

			request.setAttribute("contextType", contextType);
			request.setAttribute("contextId", contextId);
			request.setAttribute("taxonId", taxonId);
			request.setAttribute("genomeId", genomeId);
			request.setAttribute("taxonName", taxonName);
			request.setAttribute("isLoggedIn", isLoggedIn);

			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/comp_pathway_finder.jsp");
		}
		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		String sraction = request.getParameter("sraction");

		if (sraction != null && sraction.equals("save_params")) {

			String search_on = request.getParameter("search_on");
			String keyword = request.getParameter("keyword");
			String taxonId = request.getParameter("taxonId");
			String algorithm = request.getParameter("algorithm");
			String genomeId = request.getParameter("genomeId");
			String feature_id = request.getParameter("feature_id");

			Map<String, String> key = new HashMap<>();

			if (search_on != null) {
				key.put("search_on", search_on.trim());
				if (search_on.equalsIgnoreCase("Map_ID")) {
					key.put("map", keyword.trim());
				}
				else if (search_on.equalsIgnoreCase("Ec_Number")) {
					key.put("ec_number", keyword.trim());
				}
				else if (search_on.equalsIgnoreCase("Keyword")) {
					key.put("keyword", keyword.trim());
				}
			}
			if (taxonId != null && !taxonId.equals("")) {
				key.put("taxonId", taxonId);
			}

			if (genomeId != null && !genomeId.equals("")) {
				key.put("genomeId", genomeId);
			}

			if (algorithm != null && !algorithm.equals("")) {
				key.put("algorithm", algorithm);
			}

			if (feature_id != null && !feature_id.equals("")) {
				key.put("feature_id", feature_id);
			}

			long pk = (new Random()).nextLong();

			SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, jsonWriter.writeValueAsString(key));

			PrintWriter writer = response.getWriter();
			writer.write("" + pk);
			writer.close();
		}
		else {

			String need = request.getParameter("need");
			String pk = request.getParameter("pk");
			Map<String, String> key = null;
			if (pk != null && !pk.isEmpty()) {
				key = jsonReader.readValue(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk));
			}

			DataApiHandler dataApi = new DataApiHandler(request);
			switch (need) {
			case "0":
				JSONObject jsonResult = processPathwayTab(dataApi, key.get("map"), key.get("ec_number"), key.get("algorithm"), key.get("taxonId"),
						key.get("genomeId"), key.get("keyword"));
				response.setContentType("application/json");
				jsonResult.writeJSONString(response.getWriter());
				break;
			case "1":
				jsonResult = processEcNumberTab(dataApi, key.get("map"), key.get("ec_number"), key.get("algorithm"), key.get("taxonId"), key.get("genomeId"),
						key.get("keyword"));
				response.setContentType("application/json");
				jsonResult.writeJSONString(response.getWriter());
				break;
			case "2":
				jsonResult = processGeneTab(dataApi, key.get("map"), key.get("ec_number"), key.get("algorithm"), key.get("taxonId"), key.get("genomeId"),
						key.get("keyword"));
				response.setContentType("application/json");
				jsonResult.writeJSONString(response.getWriter());
				break;
			case "download":
				processDownload(request, response);
				break;
			case "downloadMapFeatureTable":
				processDownloadMapFeatureTable(request, response);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject processPathwayTab(DataApiHandler dataApi, String pathwayId, String ecNumber, String annotation, String taxonId, String genomeId, String keyword)
			throws PortletException, IOException {

		JSONObject jsonResult = new JSONObject();
		SolrQuery query = new SolrQuery("*:*");

		if (pathwayId != null && !pathwayId.equals("")) {
			query.addFilterQuery("pathway_id:" + pathwayId);
		}

		if (ecNumber != null && !ecNumber.equals("")) {
			query.addFilterQuery("ec_number:" + ecNumber);
		}

		if (annotation != null && !annotation.equals("") && !annotation.equalsIgnoreCase("ALL")) {
			query.addFilterQuery("annotation:" + annotation);
		}

		if (taxonId != null && !taxonId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + taxonId));
		}

		if (genomeId != null && !genomeId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "genome_id:(" + genomeId.replaceAll(",", " OR ") + ")"));
		}

		if (keyword != null && !keyword.equals("")) {
			query.setQuery(keyword);
		}

		JSONArray items = new JSONArray();
		int count_total = 0;
		int count_unique = 0;

		try {
			Set<String> listPathwayIds = new HashSet<>();
			Map<String, JSONObject> uniquePathways = new HashMap<>();

			// get pathway stat
			query.setRows(0).setFacet(true);
			query.add("json.facet",
					"{stat:{field:{field:pathway_id,limit:-1,facet:{genome_count:\"unique(genome_id)\",gene_count:\"unique(feature_id)\",ec_count:\"unique(ec_number)\",genome_ec:\"unique(genome_ec)\"}}}}");

			LOGGER.trace("processPathwayTab: [{}] {}", SolrCore.PATHWAY.getSolrCoreName(), query);

			String apiResponse = dataApi.solrQuery(SolrCore.PATHWAY, query);

			Map resp = jsonReader.readValue(apiResponse);
			List<Map> buckets = (List<Map>) ((Map) ((Map) resp.get("facets")).get("stat")).get("buckets");

			Map<String, Map> mapStat = new HashMap<>();
			for (Map value : buckets) {
				mapStat.put(value.get("val").toString(), value);
				listPathwayIds.add(value.get("val").toString());
			}

			if (!listPathwayIds.isEmpty()) {
				// get pathway list
				SolrQuery pathwayQuery = new SolrQuery("pathway_id:(" + StringUtils.join(listPathwayIds, " OR ") + ")");
				pathwayQuery.setFields("pathway_id,pathway_name,pathway_class");
				pathwayQuery.setRows(Math.max(dataApi.MAX_ROWS, listPathwayIds.size()));

				LOGGER.trace("processPathwayTab: [{}] {}", SolrCore.PATHWAY_REF.getSolrCoreName(), pathwayQuery);

				apiResponse = dataApi.solrQuery(SolrCore.PATHWAY_REF, pathwayQuery);
				resp = jsonReader.readValue(apiResponse);
				Map respBody = (Map) resp.get("response");

				List<Map> sdl = (List<Map>) respBody.get("docs");

				for (Map doc : sdl) {
					String aPathwayId = doc.get("pathway_id").toString();
					Map stat = mapStat.get(aPathwayId);

					if (!uniquePathways.containsKey(aPathwayId) && !stat.get("genome_count").toString().equals("0")) {
						JSONObject item = new JSONObject();
						item.put("pathway_id", aPathwayId);
						item.put("pathway_name", doc.get("pathway_name"));
						item.put("pathway_class", doc.get("pathway_class"));

						float genome_ec = Float.parseFloat(stat.get("genome_ec").toString());
						float genome_count = Float.parseFloat(stat.get("genome_count").toString());
						float ec_count = Float.parseFloat(stat.get("ec_count").toString());
						float gene_count = Float.parseFloat(stat.get("gene_count").toString());

						float ec_cons = 0;
						float gene_cons = 0;
						if (genome_count > 0 && ec_count > 0) {
							ec_cons = genome_ec / genome_count / ec_count * 100;
							gene_cons = gene_count / genome_count / ec_count;
						}

						item.put("ec_cons", ec_cons);
						item.put("ec_count", ec_count);
						item.put("gene_cons", gene_cons);
						item.put("gene_count", gene_count);
						item.put("genome_count", genome_count);
						item.put("algorithm", annotation);

						uniquePathways.put(aPathwayId, item);
					}
				}

				for (Map.Entry<String, JSONObject> pathway : uniquePathways.entrySet()) {
					items.add(pathway.getValue());
				}
				count_total = uniquePathways.entrySet().size();
				count_unique = count_total;
			}
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		// Wrapping jsonResult
		try {
			jsonResult.put("total", count_total);
			jsonResult.put("results", items);
			jsonResult.put("unique", count_unique);
		}
		catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
		}

		return jsonResult;
	}

	@SuppressWarnings("unchecked")
	private JSONObject processEcNumberTab(DataApiHandler dataApi, String pathwayId, String ecNumber, String annotation, String taxonId, String genomeId, String keyword)
			throws PortletException, IOException {

		JSONObject jsonResult = new JSONObject();
		SolrQuery query = new SolrQuery("*:*");

		if (pathwayId != null && !pathwayId.equals("")) {
			query.addFilterQuery("pathway_id:" + pathwayId);
		}

		if (ecNumber != null && !ecNumber.equals("")) {
			query.addFilterQuery("ec_number:" + ecNumber);
		}

		if (annotation != null && !annotation.equals("")) {
			query.addFilterQuery("annotation:" + annotation);
		}

		if (taxonId != null && !taxonId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + taxonId));
		}

		if (genomeId != null && !genomeId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "genome_id:(" + genomeId.replaceAll(",", " OR ") + ")"));
		}

		if (keyword != null && !keyword.equals("")) {
			query.setQuery(keyword);
		}

		JSONArray items = new JSONArray();
		int count_total = 0;
		int count_unique = 0;

		try {
			Set<String> listPathwayIds = new HashSet<>();
			Set<String> listEcNumbers = new HashSet<>();

			// get pathway stat
			query.setRows(0).setFacet(true);
			query.add("json.facet",
					"{stat:{field:{field:pathway_ec,limit:-1,facet:{genome_count:\"unique(genome_id)\",gene_count:\"unique(feature_id)\",ec_count:\"unique(ec_number)\"}}}}");

			LOGGER.trace("processEcNumberTab: [{}] {}", SolrCore.PATHWAY.getSolrCoreName(), query);

			String apiResponse = dataApi.solrQuery(SolrCore.PATHWAY, query);
			Map resp = jsonReader.readValue(apiResponse);
			List<Map> buckets = (List<Map>) ((Map) ((Map) resp.get("facets")).get("stat")).get("buckets");

			Map<String, Map> mapStat = new HashMap<>();
			for (Map value : buckets) {

				if (!value.get("genome_count").toString().equals("0")) {
					mapStat.put(value.get("val").toString(), value);

					String[] pathway_ec = value.get("val").toString().split("_");
					listPathwayIds.add(pathway_ec[0]);
					listEcNumbers.add(pathway_ec[1]);
				}
			}

			// get pathway list
			SolrQuery pathwayQuery = new SolrQuery("*:*");
			if (!listPathwayIds.isEmpty()) {
				pathwayQuery.setQuery("pathway_id:(" + StringUtils.join(listPathwayIds, " OR ") + ")");

				pathwayQuery.setFields("pathway_id,pathway_name,pathway_class,ec_number,ec_description");
				pathwayQuery.setRows(Math.max(1000000, listPathwayIds.size()));

				LOGGER.trace("processEcNumberTab: [{}] {}", SolrCore.PATHWAY_REF.getSolrCoreName(), pathwayQuery);

				apiResponse = dataApi.solrQuery(SolrCore.PATHWAY_REF, pathwayQuery);
				resp = jsonReader.readValue(apiResponse);
				Map respBody = (Map) resp.get("response");

				List<Map> sdl = (List<Map>) respBody.get("docs");

				for (Map doc : sdl) {
					String aPathwayId = doc.get("pathway_id").toString();
					String aEcNumber = doc.get("ec_number").toString();
					Map stat = mapStat.get(aPathwayId + "_" + aEcNumber);

					if (stat != null && !stat.get("genome_count").toString().equals("0")) {
						JSONObject item = new JSONObject();
						item.put("pathway_id", aPathwayId);
						item.put("pathway_name", doc.get("pathway_name"));
						item.put("pathway_class", doc.get("pathway_class"));

						float genome_count = Float.parseFloat(stat.get("genome_count").toString());
						float gene_count = Float.parseFloat(stat.get("gene_count").toString());

						item.put("ec_name", doc.get("ec_description"));
						item.put("ec_number", doc.get("ec_number"));
						item.put("gene_count", gene_count);
						item.put("genome_count", genome_count);
						item.put("algorithm", annotation);

						items.add(item);
					}
				}
				count_total = items.size();
				count_unique = listEcNumbers.size();
			}
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		// Wrapping jsonResult
		try {
			jsonResult.put("total", count_total);
			jsonResult.put("results", items);
			jsonResult.put("unique", count_unique);
		}
		catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
		}

		return jsonResult;
	}

	@SuppressWarnings("unchecked")
	private JSONObject processGeneTab(DataApiHandler dataApi, String pathwayId, String ecNumber, String annotation, String taxonId, String genomeId, String keyword)
			throws PortletException, IOException {

		LOGGER.debug("pathwayId:{}, ecNumber:{}, annotation:{}, taxonId:{}, genomeId:{}, keyword:{}", pathwayId, ecNumber, annotation, taxonId, genomeId, keyword);

		JSONObject jsonResult = new JSONObject();
		SolrQuery query = new SolrQuery("*:*");

		if (pathwayId != null && !pathwayId.equals("")) {
			query.addFilterQuery("pathway_id:" + pathwayId);
		}

		if (ecNumber != null && !ecNumber.equals("")) {
			query.addFilterQuery("ec_number:(" + ecNumber.replaceAll(",", " OR ").replaceAll("'","") + ")");
		}

		if (annotation != null && !annotation.equals("")) {
			query.addFilterQuery("annotation:" + annotation);
		}

		if (taxonId != null && !taxonId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + taxonId));
		}

		if (genomeId != null && !genomeId.equals("")) {
			query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "genome_id:(" + genomeId.replaceAll(",", " OR ") + ")"));
		}

		if (keyword != null && !keyword.equals("")) {
			query.setQuery(keyword);
		}

		JSONArray items = new JSONArray();
		int count_total = 0;
		int count_unique = 0;

		try {
			Set<String> listFeatureIds = new HashSet<>();

			query.setFields("pathway_id,pathway_name,feature_id,ec_number,ec_description");
			query.setRows(dataApi.MAX_ROWS);

			LOGGER.trace("processGeneTab: [{}] {}", SolrCore.PATHWAY.getSolrCoreName(), query);

			String apiResponse = dataApi.solrQuery(SolrCore.PATHWAY, query);
			Map resp = jsonReader.readValue(apiResponse);
			Map respBody = (Map) resp.get("response");

			List<Map> sdl = (List<Map>) respBody.get("docs");

			Map<String, Map> mapStat = new HashMap<>();
			for (Map doc : sdl) {

				mapStat.put(doc.get("feature_id").toString(), doc);
				listFeatureIds.add(doc.get("feature_id").toString());
			}

			// get pathway list
			if (!listFeatureIds.isEmpty()) {
				SolrQuery featureQuery = new SolrQuery("feature_id:(" + StringUtils.join(listFeatureIds, " OR ") + ")");
			featureQuery.setFields("genome_name,genome_id,accession,alt_locus_tag,refseq_locus_tag,patric_id,feature_id,gene,product");
				featureQuery.setRows(Math.max(dataApi.MAX_ROWS, listFeatureIds.size()));

				LOGGER.trace("processGeneTab: [{}] {}", SolrCore.FEATURE.getSolrCoreName(), featureQuery);

				apiResponse = dataApi.solrQuery(SolrCore.FEATURE, featureQuery);
				resp = jsonReader.readValue(apiResponse);
				respBody = (Map) resp.get("response");

				List<GenomeFeature> features = dataApi.bindDocuments((List<Map>) respBody.get("docs"), GenomeFeature.class);

				for (GenomeFeature feature : features) {
					String featureId = feature.getId();
					Map stat = mapStat.get(featureId);

					JSONObject item = new JSONObject();
					item.put("genome_name", feature.getGenomeName());
					item.put("genome_id", feature.getGenomeId());
					item.put("accession", feature.getAccession());
					item.put("feature_id", feature.getId());
					item.put("alt_locus_tag", feature.getAltLocusTag());
					item.put("refseq_locus_tag", feature.getRefseqLocusTag());
					item.put("algorithm", annotation);
					item.put("patric_id", feature.getPatricId());
					item.put("gene", feature.getGene());
					item.put("product", feature.getProduct());


					item.put("ec_name", stat.get("ec_description"));
					item.put("ec_number", stat.get("ec_number"));
					item.put("pathway_id", stat.get("pathway_id"));
					item.put("pathway_name", stat.get("pathway_name"));

					items.add(item);
				}
				count_total = items.size();
				count_unique = count_total;
			}
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		// Wrapping jsonResult
		try {
			jsonResult.put("total", count_total);
			jsonResult.put("results", items);
			jsonResult.put("unique", count_unique);
		}
		catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
		}

		return jsonResult;
	}

	private void processDownload(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		List<String> _tbl_header = new ArrayList<>();
		List<String> _tbl_field = new ArrayList<>();
		JSONArray _tbl_source = null;
		String fileFormat = request.getParameter("fileformat");
		String fileName;

		DataApiHandler dataApi = new DataApiHandler(request);

		String search_on = request.getParameter("search_on");
		String keyword = request.getParameter("keyword");
		String ecNumber = request.getParameter("ecN");
		String pathwayId = request.getParameter("pId");

		if (search_on.equalsIgnoreCase("Map_ID")) {
			pathwayId = keyword.trim();
		}
		else if (search_on.equalsIgnoreCase("Ec_Number")) {
			ecNumber = keyword.trim();
		}
		else if (search_on.equalsIgnoreCase("Keyword")) {
			keyword = keyword.trim();
		}

		String genomeId = request.getParameter("genomeId");
		String taxonId = request.getParameter("taxonId");
		String annotation = request.getParameter("alg");

		if (request.getParameter("aT").equals("0")) {
			_tbl_source = (JSONArray) this.processPathwayTab(dataApi, pathwayId, ecNumber, annotation, taxonId, genomeId, keyword).get("results");
			_tbl_header.addAll(Arrays
					.asList("Pathway ID", "Pathway Name", "Pathway Class", "Annotation", "Genome Count", "Unique Gene Count", "Unique EC Count",
							"Ec Conservation %", "Gene Conservation"));
			_tbl_field.addAll(Arrays
					.asList("pathway_id", "pathway_name", "pathway_class", "algorithm", "genome_count", "gene_count", "ec_count", "ec_cons",
							"gene_cons"));
		}
		else if (request.getParameter("aT").equals("1")) {
			_tbl_source = (JSONArray) this.processEcNumberTab(dataApi, pathwayId, ecNumber, annotation, taxonId, genomeId, keyword).get("results");
			_tbl_header.addAll(Arrays
					.asList("Pathway ID", "Pathway Name", "Pathway Class", "Annotation", "EC Number", "EC Description", "Genome Count",
							"Unique Gene Count"));
			_tbl_field.addAll(Arrays
					.asList("pathway_id", "pathway_name", "pathway_class", "algorithm", "ec_number", "ec_name", "genome_count", "gene_count"));
		}
		else if (request.getParameter("aT").equals("2")) {
			_tbl_source = (JSONArray) this.processGeneTab(dataApi, pathwayId, ecNumber, annotation, taxonId, genomeId, keyword).get("results");
			_tbl_header.addAll(Arrays
					.asList("Feature ID", "Genome Name", "Accession", "PATRIC ID", "RefSeq Locus Tag", "Alt Locus Tag", "Gene Symbol", "Product Name",
							"Annotation",
							"Pathway ID", "Pathway Name", "Ec Number", "EC Description"));
			_tbl_field.addAll(Arrays
					.asList("feature_id", "genome_name", "accession", "patric_id", "refseq_locus_tag", "alt_locus_tag", "gene", "product", "algorithm",
							"pathway_id",
							"pathway_name", "ec_number", "ec_name"));
		}

		fileName = "CompPathwayTable";
		ExcelHelper excel = new ExcelHelper("xssf", _tbl_header, _tbl_field, _tbl_source);
		excel.buildSpreadsheet();

		if (fileFormat.equalsIgnoreCase("xlsx")) {

			response.setContentType("application/octetstream");
			response.setProperty("Content-Disposition", "attachment; filename=\"" + fileName + "." + fileFormat + "\"");

			excel.writeSpreadsheettoBrowser(response.getPortletOutputStream());
		}
		else if (fileFormat.equalsIgnoreCase("txt")) {

			response.setContentType("application/octetstream");
			response.setProperty("Content-Disposition", "attachment; filename=\"" + fileName + "." + fileFormat + "\"");

			response.getWriter().write(excel.writeToTextFile());
		}
	}

	private void processDownloadMapFeatureTable(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		List<String> _tbl_header = new ArrayList<>();
		List<String> _tbl_field = new ArrayList<>();

		DataApiHandler dataApi = new DataApiHandler(request);

		String fileFormat = request.getParameter("fileformat");
		String fileName;

		String pathwayId = request.getParameter("map");
		String ecNumber = request.getParameter("ec_number");
		String annotation = request.getParameter("algorithm");

		String taxonId = request.getParameter("taxonId");
		String genomeId = request.getParameter("genomeId");

		JSONArray _tbl_source = (JSONArray) this.processGeneTab(dataApi, pathwayId, ecNumber, annotation, taxonId, genomeId, "").get("results");
		_tbl_header.addAll(Arrays
				.asList("Feature ID", "Genome Name", "Accession", "PATRIC ID", "RefSeq Locus Tag", "Alt Locus Tag", "Gene Symbol", "Product Name",
						"Annotation", "Pathway ID", "Pathway Name", "Ec Number", "EC Description"));
		_tbl_field.addAll(Arrays
				.asList("feature_id", "genome_name", "accession", "patric_id", "refseq_locus_tag", "alt_locus_tag", "gene", "product", "algorithm",
						"pathway_id", "pathway_name", "ec_number", "ec_name"));

		fileName = "MapFeatureTable";
		ExcelHelper excel = new ExcelHelper("xssf", _tbl_header, _tbl_field, _tbl_source);
		excel.buildSpreadsheet();

		if (fileFormat.equalsIgnoreCase("xlsx")) {

			response.setContentType("application/octetstream");
			response.setProperty("Content-Disposition", "attachment; filename=\"" + fileName + "." + fileFormat + "\"");

			excel.writeSpreadsheettoBrowser(response.getPortletOutputStream());
		}
		else if (fileFormat.equalsIgnoreCase("txt")) {

			response.setContentType("application/octetstream");
			response.setProperty("Content-Disposition", "attachment; filename=\"" + fileName + "." + fileFormat + "\"");

			response.getWriter().write(excel.writeToTextFile());
		}
	}
}
