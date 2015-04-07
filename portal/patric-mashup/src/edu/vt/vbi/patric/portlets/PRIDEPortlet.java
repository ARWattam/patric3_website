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

import edu.vt.vbi.patric.dao.DBSummary;
import edu.vt.vbi.patric.mashup.PRIDEInterface;
import org.json.simple.JSONObject;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;

public class PRIDEPortlet extends GenericPortlet {

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		response.setContentType("text/html");
		String contextType = request.getParameter("context_type");

		if (contextType != null) {

			String contextId = request.getParameter("context_id");
			// String qKeyword = request.getParameter("keyword");
			String organismName = ExperimentDataPortlet.getSpeciesName(contextType, contextId);

			request.setAttribute("contextType", contextType);
			request.setAttribute("contextId", contextId);
			request.setAttribute("organismName", organismName);

			PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/pride_list.jsp");
			prd.include(request, response);
		}
		else {
			PrintWriter writer = response.getWriter();
			writer.write("<p>Invalid Parameter - missing context information</p>");
			writer.close();
		}
	}

	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		response.setContentType("application/json");

		String cType = request.getParameter("cType");
		String cId = request.getParameter("cId");

		DBSummary conn_summary = new DBSummary();
		String species_name;

		if (cType.equals("taxon")) {
			species_name = conn_summary.getPRIDESpecies(cId);
		}
		else {
			species_name = "";
		}

		PRIDEInterface api = new PRIDEInterface();

		JSONObject jsonResult = api.getResults(species_name);

		PrintWriter writer = response.getWriter();
		jsonResult.writeJSONString(writer);
		writer.close();
	}
}
