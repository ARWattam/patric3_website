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

import javax.portlet.*;
import java.io.IOException;

public class IncumbentBRCs extends GenericPortlet {

	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		response.setContentType("text/html");
		PortletRequestDispatcher prd;
		String page = request.getParameter("page");
		if (page == null || page.equals("") || page.equals("patric")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/patric.jsp");
		}
		else if (page.equals("bhb")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/bhb.jsp");
		}
		else if (page.equals("eric")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/eric.jsp");
		}
		else if (page.equals("nmpdr")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/nmpdr.jsp");
		}
		else if (page.equals("pathema")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/pathema.jsp");
		}
		else if (page.equals("rhizobiales")) {
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/rhizobiales.jsp");
		}
		else {
			// all the other cases
			prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ibrc/patric.jsp");
		}
		prd.include(request, response);
	}
}
