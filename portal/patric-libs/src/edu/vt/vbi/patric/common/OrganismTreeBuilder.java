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
package edu.vt.vbi.patric.common;

import edu.vt.vbi.patric.beans.Genome;
import edu.vt.vbi.patric.beans.Taxonomy;
import org.apache.solr.client.solrj.SolrQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Class to support Genome Selector. You just need to call buildOrganismTreeListView() method to create an instance of genome selector.
 * 
 * @author Harry Yoo
 * 
 */
@SuppressWarnings("unchecked")
public class OrganismTreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrganismTreeBuilder.class);

	private static ObjectReader jsonReader = (new ObjectMapper()).reader(Map.class);

	/**
	 * Build html to construct a genome selector.
	 * 
	 * @return html for genome selector
	 */
	public static String buildOrganismTreeListView() {
		StringBuilder view = new StringBuilder();
		view.append("<script type=\"text/javascript\" src=\"/patric-common/js/parameters.js\"></script>\n");
		view.append("<script type=\"text/javascript\" src=\"/patric/js/vbi/TriStateTree.min.js\"></script>\n");
		view.append("<script type=\"text/javascript\" src=\"/patric/js/vbi/GenomeSelector.min.js\"></script>\n");
		view.append("<div id=\"GenomeSelector\"></div>\n");
		return view.toString();
	}

	/**
	 * Builds an array of nodes for Genome List view
	 * 
	 * @param taxonId root taxon ID
	 * @return json array of feed for Genome List
	 */
	public static JSONArray buildGenomeList(DataApiHandler dataApi, int taxonId) throws IOException {

		JSONArray treeJSON = new JSONArray();

		SolrQuery query = new SolrQuery("taxon_lineage_ids:" + taxonId);
		query.addField("taxon_id,genome_id,genome_name").setRows(1000000);
		query.addSort("genome_name", SolrQuery.ORDER.asc);
		LOGGER.trace("buildGenomeList:{}", query.toString());

		String apiResponse = dataApi.solrQuery(SolrCore.GENOME, query);
		Map resp = jsonReader.readValue(apiResponse);
		Map respBody = (Map) resp.get("response");

		List<Genome> genomeList = dataApi.bindDocuments((List<Map>) respBody.get("docs"), Genome.class);
		Set<TaxonomyGenomeNode> children = new LinkedHashSet<>();

		for (Genome genome : genomeList) {
			TaxonomyGenomeNode node = new TaxonomyGenomeNode(genome);
			node.setParentId("0");

			children.add(node);
		}

		TaxonomyGenomeNode rootTaxonomyGenomeNode = new TaxonomyGenomeNode();
		Taxonomy rootTaxonomy = dataApi.getTaxonomy(taxonId);
		rootTaxonomyGenomeNode.setName(rootTaxonomy.getTaxonName() + " (" + rootTaxonomy.getGenomeCount() + ")");
		rootTaxonomyGenomeNode.setTaxonId(rootTaxonomy.getId());
		rootTaxonomyGenomeNode.setId("0");

		rootTaxonomyGenomeNode.setChildren(children);
		treeJSON.add(rootTaxonomyGenomeNode.getJSONObject());

		return treeJSON;
	}

	/**
	 * Build an array of nodes for Taxonomy Tree view
	 * 
	 * @param taxonId root taxon ID
	 * @return json array of feed for Taxonomy Tree
	 */
	public static JSONArray buildGenomeTree(DataApiHandler dataApi, int taxonId) throws IOException {
		JSONArray treeJSON = new JSONArray();

		SolrQuery query = new SolrQuery("lineage_ids:" + taxonId + " AND genomes:[1 TO *]");
		query.addField("taxon_id,taxon_rank,taxon_name,genomes,lineage_ids").setRows(1000000);
		query.addSort("lineage", SolrQuery.ORDER.asc);
		LOGGER.trace("buildGenomeTree:{}", query.toString());

		String apiResponse = dataApi.solrQuery(SolrCore.TAXONOMY, query);
		Map resp = jsonReader.readValue(apiResponse);
		Map respBody = (Map) resp.get("response");

		List<Taxonomy> taxonomyList = dataApi.bindDocuments((List<Map>) respBody.get("docs"), Taxonomy.class);

		Map<Integer, Taxonomy> taxonomyMap = new HashMap<>();

		// 1 populate map for detail info
		for (Taxonomy tx: taxonomyList) {
			taxonomyMap.put(tx.getId(), tx);
		}

		// 2 add to rawData array
		List<List<TaxonomyTreeNode>> rawData = new ArrayList<>();
		for (Taxonomy tx : taxonomyList) {
			List<Integer> lineage = tx.getLineageIds();
			List<Integer> descendantIds = lineage;
			if (lineage.indexOf(taxonId) > 0) {
				descendantIds = lineage.subList(lineage.indexOf(taxonId), lineage.size());
			}

			List<TaxonomyTreeNode> descendant = new LinkedList<>();
			for (Integer txId : descendantIds) {
				if (taxonomyMap.containsKey(txId)) {
					descendant.add(new TaxonomyTreeNode(taxonomyMap.get(txId)));
				}
			}
			rawData.add(descendant);
		}

		// 3 build a tree
		TaxonomyTreeNode wrapper = new TaxonomyTreeNode();
		TaxonomyTreeNode current = wrapper;
		for (List<TaxonomyTreeNode> tree: rawData) {
			TaxonomyTreeNode root = current;

			int parentId = taxonId;
			for (TaxonomyTreeNode node : tree) {
				node.setParentId(parentId);
				current = current.child(node);
				parentId = node.getTaxonId();
			}

			current = root;
		}

		TaxonomyTreeNode root = wrapper.getFirstChild();
		treeJSON.add(root.getJSONObject());

		return treeJSON;
	}

	/**
	 * Build a taxonomy-genome map.
	 * @param taxonId root taxon ID
	 * @return json array of mapping {ncbi_taxon_id,genome_info_id}
	 */
	public static JSONArray buildTaxonGenomeMapping(DataApiHandler dataApi, int taxonId) throws IOException {
		JSONArray mapJSON = new JSONArray();

		SolrQuery query = new SolrQuery("taxon_lineage_ids:" + taxonId);
		query.addField("genome_id,taxon_id").setRows(1000000);
		LOGGER.trace("buildTaxonGenomeMapping:{}", query.toString());

		String apiResponse = dataApi.solrQuery(SolrCore.GENOME, query);
		Map resp = jsonReader.readValue(apiResponse);
		Map respBody = (Map) resp.get("response");

		List<Genome> genomeList = dataApi.bindDocuments((List<Map>) respBody.get("docs"), Genome.class);

		for (Genome genome : genomeList) {
			JSONObject map = new JSONObject();
			map.put("genome_id", genome.getId());
			map.put("taxon_id", genome.getTaxonId());

			mapJSON.add(map);
		}

		return mapJSON;
	}
}
