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
import java.util.*;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.vt.vbi.patric.common.SiteHelper;
import edu.vt.vbi.patric.dao.DBSearch;
import edu.vt.vbi.patric.dao.ResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IDMapping extends GenericPortlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDMapping.class);

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		response.setContentType("text/html");
		response.setTitle("ID Mapping");
		String mode = request.getParameter("display_mode");
		new SiteHelper().setHtmlMetaElements(request, response, "ID Mapping");

		PortletRequestDispatcher prd;
		if (mode != null && mode.equals("result")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/id_mapping_result.jsp");
		}
		else {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/id_mapping.jsp");
		}
		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		String sraction = request.getParameter("sraction");

		if (sraction != null && sraction.equals("save_params")) {

			ResultType key = new ResultType();
			String keyword = request.getParameter("keyword");
			String from = request.getParameter("from");
			String to = request.getParameter("to");

			if (!keyword.equals(""))
				key.put("keyword", keyword);

			key.put("from", from);
			key.put("to", to);

			// random
			Random g = new Random();
			int random = g.nextInt();

			PortletSession sess = request.getPortletSession(true);
			sess.setAttribute("key" + random, key);

			PrintWriter writer = response.getWriter();
			writer.write("" + random);
			writer.close();
		}
		else {

			String pk = request.getParameter("pk");

			String start_id = request.getParameter("start");
			String limit = request.getParameter("limit");
			int start = Integer.parseInt(start_id);
			int end = start + Integer.parseInt(limit);
			PortletSession sess = request.getPortletSession();
			ResultType key = (ResultType) sess.getAttribute("key" + pk);

			// sorting
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

			DBSearch conn_search = new DBSearch();

			int count_total = conn_search.getIDSearchCount(key.toHashMap());

			List<ResultType> items;
			if (count_total > 0) {
				items = conn_search.getIDSearchResult(key.toHashMap(), sort, start, end);
			}
			else {
				items = new ArrayList<>();
			}

			JSONObject jsonResult = new JSONObject();
			try {
				jsonResult.put("total", count_total);

				JSONArray results = new JSONArray();

				for (ResultType item : items) {
					JSONObject obj = new JSONObject();
					obj.putAll(item);
					results.add(obj);
				}
				jsonResult.put("results", results);
			}
			catch (Exception ex) {
				LOGGER.error(ex.getMessage(), ex);
			}

			response.setContentType("application/json");
			PrintWriter writer = response.getWriter();
			jsonResult.writeJSONString(writer);
			writer.close();
		}
	}

}
