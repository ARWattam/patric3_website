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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.UnavailableException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.portlet.PortletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import edu.vt.vbi.patric.circos.Circos;
import edu.vt.vbi.patric.circos.CircosGenerator;
import edu.vt.vbi.patric.common.SiteHelper;

public class CircosGenomeViewerPortlet extends GenericPortlet {

	CircosGenerator circosGenerator;

	@Override
	public void init(PortletConfig config) throws PortletException {
		String contextPath = config.getPortletContext().getRealPath(File.separator);
		circosGenerator = new CircosGenerator(contextPath);
		super.init(config);
	}

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException, UnavailableException {

		response.setContentType("text/html");

		PortletRequestDispatcher prd = null;

		new SiteHelper().setHtmlMetaElements(request, response, "Circos Genome Viewer");
		response.setTitle("Circos Genome Viewer");
		prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/circos_html.jsp");
		prd.include(request, response);
	}

	@SuppressWarnings("unchecked")
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		String imageId = request.getParameter("imageId");
		String trackList = request.getParameter("trackList");
		JSONObject res = new JSONObject();
		res.put("success", true);
		res.put("imageId", imageId);
		res.put("trackList", trackList);

		PrintWriter writer = response.getWriter();
		res.writeJSONString(writer);
		writer.close();
	}

	public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {

		Map<String, Object> parameters = new LinkedHashMap<>();
		int fileCount = 0;

		try {
			List<FileItem> items = new PortletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			for (FileItem item : items) {
				if (item.isFormField()) {
					parameters.put(item.getFieldName(), item.getString());
				}
				else {
					if (item.getFieldName().matches("file_(\\d+)$")) {
						parameters.put("file_" + fileCount, item);
						// System.out.println("file_" + fileCount + ", " + item.getFieldName() + ", " + item.getName());
						fileCount++;
					}
				}
			}
		}
		catch (FileUploadException e) {
			e.printStackTrace();
		}

		System.out.println(parameters.toString());

		// Generate Circo Image
		Circos circosConf = circosGenerator.createCircosImage(parameters);

		if (circosConf != null) {
			response.sendRedirect("/portal/portal/patric/CircosGenomeViewer/CircosGenomeViewerWindow?action=b&cacheability=PAGE&imageId="
					+ circosConf.getUuid() + "&trackList=" + StringUtils.join(circosConf.getTrackList(), ", "));
		}
	}
}
