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
package edu.vt.vbi.patric.proteinfamily;

import edu.vt.vbi.patric.beans.Genome;
import edu.vt.vbi.patric.beans.GenomeFeature;
import edu.vt.vbi.patric.common.SolrCore;
import edu.vt.vbi.patric.common.SolrInterface;
import edu.vt.vbi.patric.msa.Aligner;
import edu.vt.vbi.patric.msa.SequenceData;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.ResourceRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FIGfamData {

	public final static String FIGFAM_ID = "figfamId";

	private static final Logger LOGGER = LoggerFactory.getLogger(FIGfamData.class);

	private SolrInterface solr = new SolrInterface();

	// This function is called to get an ordering of the Figfams based on order of occurrence only for the ref. genome
	// It has nothing to do with the other genomes in a display.
	// This function returns an ordering of the Figfam ID's for the reference genome with paralogs removed
	// The counting for the number of paralogs occurs in the javascript code (I think)
	public JSONArray getSyntonyOrder(ResourceRequest req) {
		String genomeId = req.getParameter("syntonyId");
		JSONArray json_arr = null;

		if (genomeId != null) {

			try {
				solr.setCurrentInstance(SolrCore.FEATURE);
			}
			catch (MalformedURLException e) {
				LOGGER.error(e.getMessage(), e);
			}

			long end_ms, start_ms = System.currentTimeMillis();

			LBHttpSolrServer server = solr.getServer();

			SolrQuery solr_query = new SolrQuery("genome_id:" + genomeId);
			solr_query.setRows(1);
			solr_query.setFilterQueries("annotation:PATRIC AND feature_type:CDS");
			solr_query.addField("figfam_id");

			QueryResponse qr;
			SolrDocumentList sdl;

			try {
				qr = server.query(solr_query, SolrRequest.METHOD.POST);
				sdl = qr.getResults();

				solr_query.setRows((int) sdl.getNumFound());
				solr_query.addSort("seed_id", SolrQuery.ORDER.asc);
			}
			catch (SolrServerException e) {
				LOGGER.error(e.getMessage(), e);
			}

			LOGGER.debug("getSyntonyOrder() {}", solr_query.toString());

			int orderSet = 0;
			List<SyntonyOrder> collect = new ArrayList<>();
			try {
				qr = server.query(solr_query, SolrRequest.METHOD.POST);
				sdl = qr.getResults();

				end_ms = System.currentTimeMillis();

				LOGGER.debug("Genome anchoring query time - {}", (end_ms - start_ms));

				start_ms = System.currentTimeMillis();
				for (SolrDocument doc : sdl) {
					if (doc.get("figfam_id") != null) {
						collect.add(new SyntonyOrder(doc.get("figfam_id").toString(), orderSet));
						++orderSet;
					}
				}

			}
			catch (SolrServerException e) {
				LOGGER.error(e.getMessage(), e);
			}

			if (0 < collect.size()) {
				json_arr = new JSONArray();
				SyntonyOrder[] orderSave = new SyntonyOrder[collect.size()];
				collect.toArray(orderSave);// orderSave is array in order of Figfam ID
				SyntonyOrder[] toSort = new SyntonyOrder[collect.size()];
				System.arraycopy(orderSave, 0, toSort, 0, toSort.length);// copy the array so it can be sorted based on
																			// position in the genome
				Arrays.sort(toSort); // sort based on figfamIDs
				SyntonyOrder start = toSort[0];
				for (int i = 1; i < toSort.length; i++) {
					start = start.mergeSameId(toSort[i]);// set syntonyAt -1 to those objects which occur multiple times
				}
				orderSet = 0;
				for (SyntonyOrder anOrderSave : orderSave) {
					orderSet = (anOrderSave).compressAt(orderSet); // adjusts the syntonyAt number to get the correct
					// column based on replicon with -1's removed
				}
				for (SyntonyOrder aToSort : toSort) {// writes all those that don't have -1's
					(aToSort).write(json_arr);
				}
			}

			end_ms = System.currentTimeMillis();

			LOGGER.debug("Genome anchoring post processing time - {} ms", (end_ms - start_ms));
		}

		return json_arr;
	}

	@SuppressWarnings("unchecked")
	public void getLocusTags(ResourceRequest req, PrintWriter writer) {

		JSONArray arr = new JSONArray();
		try {
			solr.setCurrentInstance(SolrCore.FEATURE);
		}
		catch (MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		}

		SolrQuery solr_query = new SolrQuery();
		solr_query.setQuery("genome_id:(" + req.getParameter("genomeIds") + ") AND figfam_id:(" + req.getParameter("figfamIds") + ")");
		solr_query.setFilterQueries("annotation:PATRIC AND feature_type:CDS");
		solr_query.addField("locus_tag");
		solr_query.setRows(150000);

		LOGGER.debug("getLocusTags() {}", solr_query.toString());

		QueryResponse qr;
		SolrDocumentList sdl;

		try {
			qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
			sdl = qr.getResults();

			for (SolrDocument d : sdl) {
				for (Map.Entry<String, Object> el : d) {
					arr.add(el.getValue());
				}
			}
		}
		catch (SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}

		JSONObject data = new JSONObject();
		data.put("data", arr);
		try {
			data.writeJSONString(writer);
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public JSONArray getDetails(String genomeIds, String figfamIds) {

		JSONArray arr = new JSONArray();

		try {
			solr.setCurrentInstance(SolrCore.FEATURE);
		}
		catch (MalformedURLException e1) {
			LOGGER.error(e1.getMessage(), e1);
		}

		SolrQuery solr_query = new SolrQuery();
		solr_query.setQuery("genome_id:(" + genomeIds + ") AND figfam_id:(" + figfamIds + ")");
		solr_query.setFields("figfam_id,genome_name,accession,alt_locus_tag,start,end,na_length,strand,aa_length,gene,product");
		solr_query.setFilterQueries("annotation:PATRIC AND feature_type:CDS");
		solr_query.setRows(1500000);

		LOGGER.debug("getDetails() {}", solr_query.toString());

		try {
			QueryResponse qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
			SolrDocumentList sdl = qr.getResults();

			for (SolrDocument d : sdl) {
				JSONObject values = new JSONObject();
				for (Map.Entry<String, Object> el : d) {
					values.put(el.getKey(), el.getValue());
				}
				arr.add(values);
			}
		}
		catch (SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return arr;
	}

	public Aligner getFeatureAlignment(char needHtml, ResourceRequest req) {
		Aligner result = null;
		try {
			SequenceData[] sequences = getFeatureProteins(req);
			result = new Aligner(needHtml, req.getParameter(FIGFAM_ID), sequences);
		}
		catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return result;
	}

	public String getGenomeIdsForTaxon(String taxon) {
		String genomeIds = null;
		SolrQuery query = new SolrQuery("rast_cds:[1 TO *] AND taxon_lineage_ids:" + taxon);
		query.addField("genome_id");
		query.setRows(500000);
		QueryResponse qr;
		
		LOGGER.debug("getGenomeIdsForTaxon() {}", query.toString());
		
		try {
			solr.setCurrentInstance(SolrCore.GENOME);
			qr = solr.getServer().query(query);
			List<String> gIds = new ArrayList<>();
			List<Genome> genomes = qr.getBeans(Genome.class);
			for (Genome g: genomes) {
				gIds.add(g.getId());
			}
			genomeIds = StringUtils.join(gIds, ",");
		}
		catch (SolrServerException | MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return genomeIds;
	}

	public String getTaxonName(String taxon) {
		String result = null;
		try {
			solr.setCurrentInstance(SolrCore.TAXONOMY);

			SolrQuery query = new SolrQuery("taxon_id:" + taxon);
			query.addField("taxon_name");
			QueryResponse qr = solr.getServer().query(query);
			SolrDocumentList sdl = qr.getResults();
			for (SolrDocument doc: sdl) {
				result = doc.get("taxon_name").toString();
			}
		} catch (SolrServerException | MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return result;
	}

	private SequenceData[] getFeatureSequences(String[] featureIds) {
		List<SequenceData> collect = new ArrayList<>();

		try {
			solr.setCurrentInstance(SolrCore.FEATURE);

			SolrQuery query = new SolrQuery("feature_id:(" + StringUtils.join(featureIds, " OR ") + ")");
			query.addField("genome_name,seed_id,translation");
			query.setRows(featureIds.length);

			QueryResponse qr = solr.getServer().query(query);
			List<GenomeFeature> features = qr.getBeans(GenomeFeature.class);

			for (GenomeFeature feature: features) {
				collect.add(new SequenceData(feature.getGenomeName().replace(" ", "_"), feature.getSeedId(), feature.getTranslation()));
			}

		}
		catch (MalformedURLException | SolrServerException e) {
			e.printStackTrace();
		}

		SequenceData[] result = new SequenceData[collect.size()];
		collect.toArray(result);
		return result;
	}

	private SequenceData[] getFeatureProteins(ResourceRequest req) throws SQLException {
		SequenceData[] result = null;
		String featuresString = req.getParameter("featureIds");
		if (featuresString != null) {
			String[] idList = featuresString.split(",");
			result = getFeatureSequences(idList);
		}
		return result;
	}

	public void getFeatureIds(ResourceRequest req, PrintWriter writer, String keyword) {

		try {
			solr.setCurrentInstance(SolrCore.FEATURE);

			SolrQuery solr_query = new SolrQuery(keyword);
			solr_query.setRows(1);
			solr_query.addField("feature_id");

			QueryResponse qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
			SolrDocumentList sdl = qr.getResults();

			long rows = sdl.getNumFound();
			solr_query.setRows((int) rows);

			// replace this part with GenomeFeature bean
			qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
			sdl = qr.getResults();

			List<String> features = new ArrayList<>();
			for (SolrDocument d : sdl) {
				features.add(d.get("feature_id").toString());
			}

			writer.write(StringUtils.join(features, ","));
		}
		catch (SolrServerException | MalformedURLException e) {
			LOGGER.debug(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void getGenomeDetails(ResourceRequest req, PrintWriter writer) throws IOException {
		solr.setCurrentInstance(SolrCore.GENOME);

		String cType = req.getParameter("context_type");
		String cId = req.getParameter("context_id");
		String keyword = "";
		if (cType != null && cType.equals("taxon") && cId != null && !cId.equals("")) {
			keyword = "rast_cds:[1 TO *] AND taxon_lineage_ids:" + cId;
		}
		else if (req.getParameter("keyword") != null) {
			keyword = req.getParameter("keyword");
		}
		String fields = req.getParameter("fields");

		SolrQuery query = new SolrQuery();
		query.setQuery(keyword);
		if (fields != null && !fields.equals("")) {
			query.addField(fields);
		}
		query.setRows(500000);

		LOGGER.debug("getGenomeDetails() {}", query.toString());
		JSONObject object = null;
		try {
			object = solr.ConverttoJSON(solr.getServer(), query, false, false);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.debug("params: {}", req.getParameterMap().toString());
		}

		JSONObject obj = (JSONObject) object.get("response");
		JSONArray obj1 = (JSONArray) obj.get("docs");

		JSONObject jsonResult = new JSONObject();
		jsonResult.put("results", obj1);
		jsonResult.put("total", obj.get("numFound"));
		jsonResult.writeJSONString(writer);
	}

	@SuppressWarnings("unchecked")
	public void getGroupStats(ResourceRequest req, PrintWriter writer) throws IOException {
		solr.setCurrentInstance(SolrCore.FEATURE);

		// long start_ms, end_ms;
		JSONObject figfams = new JSONObject();
		List<String> figfamIdList = new ArrayList<>();
		List<String> genomeIdList = Arrays.asList(req.getParameter("genomeIds").split(","));

		// getting genome counts per figfamID (figfam)
		SolrQuery solr_query = new SolrQuery("annotation:PATRIC AND feature_type:CDS");
		solr_query.addFilterQuery(getSolrQuery(req));
		solr_query.setRows(0);
		solr_query.setFacet(true);
		solr_query.addFacetPivotField("figfam_id,genome_id");
		solr_query.setFacetMinCount(1);
		solr_query.setFacetLimit(-1);

		LOGGER.debug("getStroupStats() 1/3 " + solr_query.toString());

		try {
			QueryResponse qr = solr.getServer().query(solr_query);

			NamedList<List<PivotField>> pivots = qr.getFacetPivot();

			for (Map.Entry<String, List<PivotField>> pivot : pivots) {
				List<PivotField> pivotEntries = pivot.getValue();
				if (pivotEntries != null) {
					for (PivotField pivotEntry : pivotEntries) {
						JSONObject figfam = new JSONObject();

						List<PivotField> pivotGenomes = pivotEntry.getPivot();
						int count = 0, index;
						String hex;

						String[] genomeIdsStr = new String[genomeIdList.size()];
						Arrays.fill(genomeIdsStr, "00");

						for (PivotField pivotGenome : pivotGenomes) {
							index = genomeIdList.indexOf(pivotGenome.getValue().toString());
							hex = Integer.toHexString(pivotGenome.getCount());
							genomeIdsStr[index] = hex.length() < 2 ? "0" + hex : hex;
							count += pivotGenome.getCount();
						}

						figfamIdList.add(pivotEntry.getValue().toString());

						figfam.put("genomes", StringUtils.join(genomeIdsStr, ""));
						figfam.put("genome_count", pivotGenomes.size());
						figfam.put("feature_count", count);

						figfams.put(pivotEntry.getValue(), figfam);
					}
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.debug("::getGroupStats() 1/3, params: {}", req.getParameterMap().toString());
		}

		// getting distribution of aa length in each protein family

		solr_query = new SolrQuery("*:*");
		solr_query.addFilterQuery(getSolrQuery(req));
		solr_query.setRows(0);
		solr_query.set("stats", "true");
		solr_query.set("stats.field", "aa_length");
		solr_query.set("stats.facet", "figfam_id");

		try {
			QueryResponse qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
			Map<String, FieldStatsInfo> stats_map = qr.getFieldStatsInfo();

			if (stats_map != null) {
				FieldStatsInfo stats = stats_map.get("aa_length");
				if (stats != null) {
					List<FieldStatsInfo> fieldStats = stats.getFacets().get("figfam_id");
					for (FieldStatsInfo fieldStat : fieldStats) {
						if (fieldStat.getName() != null) {
							JSONObject figfam = (JSONObject) figfams.get(fieldStat.getName());
							JSONObject stat = new JSONObject();
							stat.put("max", fieldStat.getMax());
							stat.put("min", fieldStat.getMin());
							stat.put("mean", fieldStat.getMean());
							stat.put("stddev", Math.round(fieldStat.getStddev() * 1000) / (double) 1000);

							figfam.put("stats", stat);
							figfams.put(fieldStat.getName(), figfam);
						}
					}
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.debug("::getGroupStats() 2/3, params: {}", req.getParameterMap().toString());
		}

		// getting distinct figfam_product
		if (!figfamIdList.isEmpty()) {

			solr.setCurrentInstance(SolrCore.FIGFAM_DIC);

			solr_query = new SolrQuery("figfam_id:(" + StringUtils.join(figfamIdList, " OR ") + ")");
			solr_query.addField("figfam_id,figfam_product");
			solr_query.setRows(figfams.size());

			LOGGER.trace(solr_query.toString());
			try {
				QueryResponse qr = solr.getServer().query(solr_query, SolrRequest.METHOD.POST);
				SolrDocumentList sdl = qr.getResults();

				for (SolrDocument d : sdl) {
					JSONObject figfam = (JSONObject) figfams.get(d.get("figfam_id"));
					figfam.put("description", d.get("figfam_product"));
					figfams.put(d.get("figfam_id"), figfam);
				}
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				LOGGER.debug("::getGroupStats() 3/3, params: {}", req.getParameterMap().toString());
			}
			figfams.writeJSONString(writer);
		}
	}

	private class SyntonyOrder implements Comparable<SyntonyOrder> {
		String groupId;

		int syntonyAt;

		SyntonyOrder(String id, int at) {
			groupId = id;
			syntonyAt = at;
		}

		int compressAt(int orderSet) {
			if (0 <= syntonyAt) {
				syntonyAt = orderSet;
				++orderSet;
			}
			return orderSet;
		}

		@SuppressWarnings("unchecked")
		void write(JSONArray json_arr) {
			if (0 <= syntonyAt) {
				JSONObject j = new JSONObject();
				j.put("syntonyAt", this.syntonyAt);
				j.put("groupId", this.groupId);
				json_arr.add(j);
			}
		}

		SyntonyOrder mergeSameId(SyntonyOrder other) {
			SyntonyOrder result = other;
			if ((this.groupId).equals(other.groupId)) {
				other.syntonyAt = -1;
				result = this;
			}
			return result;
		}

		public int compareTo(SyntonyOrder o) {
			int result = (this.groupId).compareTo(o.groupId);
			if (result == 0) {
				result = this.syntonyAt - o.syntonyAt;
			}
			return result;
		}
	}

	public String getSolrQuery(ResourceRequest req) {
		String keyword = "";

		if (req.getParameter("keyword") != null && !req.getParameter("keyword").equals("")) {
			keyword += "(" + solr.KeywordReplace(req.getParameter("keyword")) + ")";
		}

		String cType = req.getParameter("context_type");
		String cId = req.getParameter("context_id");
		if (cType != null && cType.equals("taxon") && cId != null && !cId.equals("")) {
			keyword += SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + cId);
		}
		else {
			String listText = req.getParameter("genomeIds");

			if (listText != null) {
				if (req.getParameter("keyword") != null && !req.getParameter("keyword").equals(""))
					keyword += " AND ";
				keyword += "(genome_id:(" + listText.replaceAll(",", " OR ") + "))";
			}
		}

		return keyword;
	}
}
