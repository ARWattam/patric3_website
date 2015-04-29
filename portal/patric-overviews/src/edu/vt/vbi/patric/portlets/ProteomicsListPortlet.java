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
import edu.vt.vbi.patric.dao.ResultType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProteomicsListPortlet extends GenericPortlet {

	SolrInterface solr = new SolrInterface();

	private static final Logger LOGGER = LoggerFactory.getLogger(ProteomicsListPortlet.class);

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		response.setContentType("text/html");
		PortletRequestDispatcher prd;

		SiteHelper.setHtmlMetaElements(request, response, "Proteomics Experiment List");
		response.setTitle("Proteomics Experiment List");
		String type = request.getParameter("context_type");

		if (type.equals("feature")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/experiment/proteomics_list_feature.jsp");
		}
		else {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/experiment/proteomics_list.jsp");
		}

		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		String sraction = request.getParameter("sraction");
		Gson gson = new Gson();

		if (sraction != null && sraction.equals("save_params")) {

			ResultType key = new ResultType();

			String taxonId = "";
			String cType = request.getParameter("context_type");
			String cId = request.getParameter("context_id");
			if (cType != null && cId != null && cType.equals("taxon") && !cId.equals("")) {
				taxonId = cId;
			}
			String keyword = request.getParameter("keyword");
			String state = request.getParameter("state");

			if (taxonId != null && !taxonId.equalsIgnoreCase("")) {
				key.put("taxonId", taxonId);
			}
			if (keyword != null) {
				key.put("keyword", keyword.trim());
			}
			if (state != null) {
				key.put("state", state);
			}
			long pk = (new Random()).nextLong();

			SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, gson.toJson(key, ResultType.class));

			PrintWriter writer = response.getWriter();
			writer.write("" + pk);
			writer.close();
		}
		else {

			String need = request.getParameter("need");
			String facet = "", keyword, pk, state, experiment_id;
			boolean hl;

			ResultType key = new ResultType();
			JSONObject jsonResult = new JSONObject();

			if (need.equals("1")) {

				pk = request.getParameter("pk");

				keyword = request.getParameter("keyword");
				experiment_id = request.getParameter("experiment_id");
				facet = request.getParameter("facet");

				String json = SessionHandler.getInstance().get(SessionHandler.PREFIX + pk);
				if (json == null) {
					key.put("facet", facet);
					key.put("keyword", keyword);
					SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, gson.toJson(key, ResultType.class));
				}
				else {
					key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), ResultType.class);
					key.put("facet", facet);
				}

				String orig_keyword = key.get("keyword");

				if (experiment_id != null && !experiment_id.equals("")) {
					key.put("keyword", solr.ConstructKeyword("experiment_id", experiment_id));

				}
				else if (experiment_id != null && experiment_id.equals("")) {

					String solrId = "";
					solr.setCurrentInstance(SolrCore.PROTEOMICS_EXPERIMENT);
					JSONObject object = solr.getData(key, null, facet, 0, 10000, true, false, false);

					JSONObject obj = (JSONObject) object.get("response");
					JSONArray obj1 = (JSONArray) obj.get("docs");

					for (Object ob : obj1) {
						JSONObject doc = (JSONObject) ob;
						if (solrId.length() == 0) {
							solrId += doc.get("experiment_id").toString();
						}
						else {
							solrId += "," + doc.get("experiment_id").toString();
						}
					}

					key.put("keyword", solr.ConstructKeyword("experiment_id", solrId));

					JSONObject facets = (JSONObject) object.get("facets");
					if (facets != null) {
						key.put("facets", facets.toString());
					}
				}

				String start_id = request.getParameter("start");
				String limit = request.getParameter("limit");
				int start = Integer.parseInt(start_id);
				int end = Integer.parseInt(limit);

				// sorting
				JSONParser parser = new JSONParser();
				JSONArray sorter;
				String sort_field = "";
				String sort_dir = "";
				try {
					sorter = (JSONArray) parser.parse(request.getParameter("sort"));
					sort_field += ((JSONObject) sorter.get(0)).get("property").toString();
					sort_dir += ((JSONObject) sorter.get(0)).get("direction").toString();
					for (int i = 1; i < sorter.size(); i++) {
						sort_field += "," + ((JSONObject) sorter.get(i)).get("property").toString();
					}
				}
				catch (ParseException e) {
					LOGGER.error(e.getMessage(), e);
				}

				Map<String, String> sort = new HashMap<>();

				if (!sort_field.equals("") && !sort_dir.equals("")) {
					sort.put("field", sort_field);
					sort.put("direction", sort_dir);
				}
				solr.setCurrentInstance(SolrCore.PROTEOMICS_PROTEIN);

				JSONObject object = solr.getData(key, sort, facet, start, end, false, false, false);

				JSONObject obj = (JSONObject) object.get("response");
				JSONArray obj1 = (JSONArray) obj.get("docs");

				if (!key.containsKey("facets")) {
					JSONObject facets = (JSONObject) object.get("facets");
					key.put("facets", facets.toString());
				}

				key.put("keyword", orig_keyword);

				jsonResult.put("results", obj1);
				jsonResult.put("total", obj.get("numFound"));

				response.setContentType("application/json");
				PrintWriter writer = response.getWriter();
				writer.write(jsonResult.toString());
				writer.close();

			}
			else if (need.equals("0")) {

				solr.setCurrentInstance(SolrCore.PROTEOMICS_EXPERIMENT);

				pk = request.getParameter("pk");
				keyword = request.getParameter("keyword");
				facet = request.getParameter("facet");
				String highlight = request.getParameter("highlight");

				hl = Boolean.parseBoolean(highlight);

				if (pk != null) {
					String json = SessionHandler.getInstance().get(SessionHandler.PREFIX + pk);
					if (json == null) {
						key.put("facet", facet);
						key.put("keyword", keyword);
						SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, gson.toJson(key, ResultType.class));
					}
					else {
						key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), ResultType.class);
						key.put("facet", facet);
					}
				}
				else {
					key.put("keyword", keyword);
				}

				String start_id = request.getParameter("start");
				String limit = request.getParameter("limit");
				int start = Integer.parseInt(start_id);
				int end = Integer.parseInt(limit);

				Map<String, String> sort = null;
				if (request.getParameter("sort") != null) {
					// sorting
					JSONParser a = new JSONParser();
					JSONArray sorter;
					String sort_field = "";
					String sort_dir = "";
					try {
						sorter = (JSONArray) a.parse(request.getParameter("sort"));
						sort_field += ((JSONObject) sorter.get(0)).get("property").toString();
						sort_dir += ((JSONObject) sorter.get(0)).get("direction").toString();
						for (int i = 1; i < sorter.size(); i++) {
							sort_field += "," + ((JSONObject) sorter.get(i)).get("property").toString();
						}
					}
					catch (ParseException e) {
						LOGGER.error(e.getMessage(), e);
					}

					sort = new HashMap<>();

					if (!sort_field.equals("") && !sort_dir.equals("")) {
						sort.put("field", sort_field);
						sort.put("direction", sort_dir);
					}
				}

				JSONObject object = solr.getData(key, sort, facet, start, end, facet != null && !facet.equals(""), hl, false);

				JSONObject obj = (JSONObject) object.get("response");
				JSONArray obj1 = (JSONArray) obj.get("docs");

				if (!key.containsKey("facets")) {
					if (object.containsKey("facets")) {
						JSONObject facets = (JSONObject) object.get("facets");
						key.put("facets", facets.toString());
					}
				}

				jsonResult.put("results", obj1);
				jsonResult.put("total", obj.get("numFound"));

				response.setContentType("application/json");
				PrintWriter writer = response.getWriter();
				writer.write(jsonResult.toString());
				writer.close();

			}
			else if (need.equals("tree")) {

				solr.setCurrentInstance(SolrCore.PROTEOMICS_EXPERIMENT);

				pk = request.getParameter("pk");
				key = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), ResultType.class);

				if (key.containsKey("state"))
					state = key.get("state");
				else
					state = request.getParameter("state");

				key.put("state", state);
				SessionHandler.getInstance().set(SessionHandler.PREFIX + pk, gson.toJson(key, ResultType.class));

				try {
					if (!key.containsKey("tree")) {
						JSONObject facet_fields = (JSONObject) new JSONParser().parse(key.get("facets"));
						JSONArray arr1 = solr.processStateAndTree(key, need, facet_fields, key.get("facet"), state, null, 4, false);
						jsonResult.put("results", arr1);
						key.put("tree", arr1);
					}
					else {
						jsonResult.put("results", key.get("tree"));
					}
				}
				catch (ParseException e) {
					LOGGER.error(e.getMessage(), e);
				}

				response.setContentType("application/json");
				PrintWriter writer = response.getWriter();
				writer.write(jsonResult.get("results").toString());
				writer.close();

			}
			else if (need.equals("getFeatureIds")) {

				solr.setCurrentInstance(SolrCore.PROTEOMICS_PROTEIN);
				keyword = request.getParameter("keyword");
				key.put("keyword", keyword);
				JSONObject object = solr.getData(key, null, facet, 0, -1, false, false, false);

				response.setContentType("application/json");
				PrintWriter writer = response.getWriter();
				writer.write(object.get("response").toString());
				writer.close();

			}
			else if (need.equals("getPeptides")) {

				experiment_id = request.getParameter("experiment_id");
				String na_feature_id = request.getParameter("na_feature_id");

				solr.setCurrentInstance(SolrCore.PROTEOMICS_PEPTIDE);
				key.put("keyword", "na_feature_id:" + na_feature_id + " AND experiment_id:" + experiment_id);
				key.put("fields", "peptide_sequence");
				JSONObject object = solr.getData(key, null, facet, 0, -1, false, false, false);
				object = (JSONObject) object.get("response");
				// object.put("aa", FASTAHelper.getFASTAAASequence(na_feature_id));
				object.put("aa", FASTAHelper.getFASTASequence(Arrays.asList(na_feature_id), "protein"));

				response.setContentType("application/json");
				PrintWriter writer = response.getWriter();
				writer.write(object.toJSONString());
				writer.close();
			}
		}
	}
}
