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

import edu.vt.vbi.patric.common.*;
import edu.vt.vbi.patric.dao.DBTranscriptomics;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unchecked")
public class TestFileUploader extends GenericPortlet {

	private final String collectionId = "9beeee5d-585a-4f26-88ec-749e568553c9";

	private final String token = "fcd1c6d9ba9401660af0aec9095af61702c50e6b19fdc7e6e1a75d03711c431e75e13a184ec8eee3";

	private ExpressionDataFileReader reader;

	private static final Logger LOGGER = LoggerFactory.getLogger(TestFileUploader.class);

	@Override
	protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

		response.setContentType("text/html");
		response.setTitle("Test File Uploader");

		PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/TestFileUploader.jsp");
		prd.include(request, response);
	}

	public void serveResource(ResourceRequest req, ResourceResponse resp) throws PortletException, IOException {

		resp.setContentType("text/html");

		String callType = req.getParameter("sraction");
		PrintWriter writer = resp.getWriter();
		JSONObject jsonResult = new JSONObject();

		PolyomicHandler polyomic = new PolyomicHandler();
		polyomic.setAuthenticationToken(token);

		LOGGER.debug(callType);

		if (callType.equals("first")) {

			JSONObject config = polyomic.getExpressionDataFileReaderConfig(collectionId);
			config.put("idMappingType", "refseq_source_id");

			LOGGER.debug("testfilereader");

			/*
			 * JSONObject config = new JSONObject();
			 * 
			 * config.put("sampleFilePresent","true"); config.put("sampleURL", polyomic.findRawFileUrl("sample", collectionId));
			 * config.put("sampleFileType", "txt"); config.put("dataURL", polyomic.findRawFileUrl("expression", collectionId));
			 * config.put("dataFileType", "txt"); config.put("dataFileFormat", "matrix"); config.put("dataFileOrientation", "svg");
			 * config.put("idMappingType", "refseq_source_id"); config.put("collectionID", collectionId);
			 */

			LOGGER.debug(config.toString());
			/*
			 * JSONObject json = new JSONObject(); String idx = ""; for (int i=0; i<8000; i++) { idx +=
			 * "[1234567890!@#$%^&*()abcdefghijklmnopqrstuvwxyz]"; } json.put("string", idx);
			 * 
			 * polyomic.saveJSONtoCollection(collectionId, "harrytest5.json", json, "");
			 */

			reader = new ExpressionDataFileReader(config);

			if (reader.doRead()) {
				reader.calculateExpStats();

				try {
					IDMap();
				}
				catch (InvalidFormatException e) {
					LOGGER.error(e.getMessage(), e);
				}

				polyomic.saveJSONFilesToCollection(collectionId, reader);
			}
		}
		else if (callType.equals("second")) {

			String expId = req.getParameter("expId");
			String sampleId = req.getParameter("sampleId");
			String colId = req.getParameter("colId");
			String colsampleId = req.getParameter("colsampleId");

			DBTranscriptomics dbomics = new DBTranscriptomics();

			JSONArray sample = null;// = dbomics.getSamples(sampleId, expId);

			String colFlag = "true";
			if (colFlag.equals("true")) {
				/*
				 * Start reading from JSON files
				 */

				ExpressionDataCollection parser = new ExpressionDataCollection(colId, token);
				parser.read(ExpressionDataCollection.CONTENT_SAMPLE);
				if (colsampleId != null && !colsampleId.equals(""))
					parser.filter(colsampleId, ExpressionDataCollection.CONTENT_SAMPLE);

				/*
				 * End reading from JSON files
				 */

				// Append samples from collection to samples from DB
				sample = parser.append(sample, ExpressionDataCollection.CONTENT_SAMPLE);
			}

			String sampleList = "";
			sampleList += ((JSONObject) sample.get(0)).get("pid");

			for (int i = 1; i < sample.size(); i++) {
				sampleList += "," + ((JSONObject) sample.get(i)).get("pid");
			}

			jsonResult.put(ExpressionDataCollection.CONTENT_SAMPLE + "Total", sample.size());
			jsonResult.put(ExpressionDataCollection.CONTENT_SAMPLE, sample);

			JSONArray data = null;// = dbomics.getGenes(sampleId, expId);

			if (colFlag.equals("true")) {
				/*
				 * Start reading from JSON files
				 */

				ExpressionDataCollection parser = new ExpressionDataCollection(colId, token);
				parser.read(ExpressionDataCollection.CONTENT_EXPRESSION);
				if (colsampleId != null && !colsampleId.equals(""))
					parser.filter(colsampleId, ExpressionDataCollection.CONTENT_EXPRESSION);

				/*
				 * End reading from JSON files
				 */

				// Append samples from collection to samples from DB
				data = parser.append(data, ExpressionDataCollection.CONTENT_EXPRESSION);
			}

			JSONArray stats = getExperimentStats(data, sampleList, sample);
			jsonResult.put(ExpressionDataCollection.CONTENT_EXPRESSION + "Total", stats.size());
			jsonResult.put(ExpressionDataCollection.CONTENT_EXPRESSION, stats);

			writer.write(jsonResult.toString());
			writer.close();
		}
		writer.write(jsonResult.toString());
		writer.close();
	}

	public void IDMap() throws InvalidFormatException {
		reader.runIDMappingStatistics();
	}

	public JSONArray getExperimentStats(JSONArray data, String samples, JSONArray sample_data) throws IOException {

		JSONArray results = new JSONArray();

		HashMap<String, ExpressionDataGene> genes = new HashMap<>();
		HashMap<String, String> sample = new HashMap<>();

		for (Object aSample_data : sample_data) {
			JSONObject a = (JSONObject) aSample_data;
			sample.put(a.get("pid").toString(), a.get("expname").toString());
		}

		for (Object aData : data) {

			JSONObject a = (JSONObject) aData;
			String id = a.get("feature_id").toString();
			ExpressionDataGene b;

			if (genes.containsKey(id)) {
				b = genes.get(id);
			}
			else {
				b = new ExpressionDataGene(a);
			}

			b.addSamplestoGene(a, sample); // Sample HashMap is used to create absence/presence string
			genes.put(id, b);
		}

		Iterator<?> it = genes.entrySet().iterator();
		String idList = "";
		JSONObject temp = new JSONObject();

		while (it.hasNext()) {

			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
			ExpressionDataGene value = (ExpressionDataGene) entry.getValue();
			JSONObject a = new JSONObject();

			a.put("refseq_locus_tag", value.getRefSeqLocusTag());
			a.put("feature_id", value.getFeatureID());
			value.setSampleBinary(samples);
			a.put("sample_binary", value.getSampleBinary());
			a.put("sample_size", value.getSampleCounts());
			a.put("samples", value.getSamples());

			idList += value.getFeatureID() + ",";
			temp.put(value.getFeatureID(), a);
		}

		/*
		 * Solr Call to get Feature attributes-----------------------------------
		 */
		Map<String, Object> condition = new HashMap<>();
		condition.put("feature_ids", idList.substring(0, idList.length() - 1));
		condition.put("startParam", "0");

		SolrInterface solr = new SolrInterface();
		JSONObject object = solr.getFeaturesByID(condition);
		JSONArray obj_array = (JSONArray) object.get("results");

		JSONObject a, b;

		for (Object anObj_array : obj_array) {
			a = (JSONObject) anObj_array;
			b = (JSONObject) temp.get(a.get("feature_id").toString());
			b.put("strand", a.get("strand"));
			b.put("patric_product", a.get("product"));
			b.put("patric_accession", a.get("accession"));
			b.put("start_max", a.get("start_max"));
			b.put("end_min", a.get("end_min"));
			b.put("patric_locus_tag", a.get("locus_tag"));
			results.add(b);
		}

		return results;
	}
}
