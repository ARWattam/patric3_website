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

import edu.vt.vbi.patric.beans.Genome;
import edu.vt.vbi.patric.beans.Taxonomy;
import edu.vt.vbi.patric.common.SolrInterface;
import edu.vt.vbi.patric.dao.DBPRC;
import edu.vt.vbi.patric.mashup.PRIDEInterface;
import org.json.simple.JSONObject;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;

public class ProteomicsSummary extends GenericPortlet {

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		response.setContentType("text/html");
		String contextType = request.getParameter("context_type");

		if (contextType != null) {
			String contextId = request.getParameter("context_id");

			request.setAttribute("contextType", contextType);
			request.setAttribute("contextId", contextId);

			PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/summary_proteomics_init.jsp");
			prd.include(request, response);
		}
		else {
			PrintWriter writer = response.getWriter();
			writer.write("<p>Invalid Parameter - missing context information</p>");
			writer.close();
		}
	}

	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
		response.setContentType("text/html");
		String contextType = request.getParameter("cType");

		if (contextType != null) {

			int taxonId = -1;
			String contextId = request.getParameter("cId");
			String speciesName = "";
			String errorMsg = "Data is not available temporarily";

			DBPRC conn_prc = new DBPRC();
			SolrInterface solr = new SolrInterface();

			if (contextType.equals("taxon")) {
				Taxonomy taxonomy = solr.getTaxonomy(Integer.parseInt(contextId));
				speciesName = taxonomy.getTaxonName();
				taxonId = taxonomy.getId();
			}
			else if (contextType.equals("genome")) {
				Genome genome = solr.getGenome(contextId);
				speciesName = genome.getGenomeName();
				taxonId = genome.getTaxonId();
			}

			//PRIDE
			PRIDEInterface api = new PRIDEInterface();
			JSONObject result = api.getResults(speciesName);

			//PRC
			int result_ms = conn_prc.getPRCCount("" + taxonId, "MS");

			// pass attributes through request
			request.setAttribute("contextType", contextType);
			request.setAttribute("contextId", contextId);
			request.setAttribute("result", result); // JSONObject
			request.setAttribute("result_ms", result_ms); // int
			request.setAttribute("errorMsg", errorMsg);

			PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/summary_proteomics.jsp");
			prd.include(request, response);
		}
		else {
			PrintWriter writer = response.getWriter();
			writer.write("<p>Invalid Parameter - missing context information</p>");
			writer.close();
		}
	}
}
