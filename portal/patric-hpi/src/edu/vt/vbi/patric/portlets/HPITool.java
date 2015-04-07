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

import edu.vt.vbi.patric.common.SiteHelper;

import javax.portlet.*;
import java.io.IOException;

public class HPITool extends GenericPortlet {

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		SiteHelper.setHtmlMetaElements(request, response, "Host-Pathogen Interaction Finder");
		response.setContentType("text/html");
		response.setTitle("Host-Pathogen Interactions");
		PortletRequestDispatcher prd;

		String mode = request.getParameter("display_mode");

		if (mode != null && mode.equals("tab")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/hpi_finder_tab.jsp");
		}
		else {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/hpi_finder.jsp");
		}

		prd.include(request, response);
	}
}
