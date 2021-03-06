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
package edu.vt.vbi.patric.mashup.xmlHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PDBGOTermsHandler extends DefaultHandler {

	private List<Map<String, String>> result;

	private Map<String, String> goTerm;

	public PDBGOTermsHandler() {
	}

	public List<Map<String, String>> getParsedData() {
		return result;
	}

	@Override
	public void startDocument() throws SAXException {
		result = new ArrayList<>();
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (qName.equalsIgnoreCase("term")) {
			goTerm = new HashMap<>();
			goTerm.put("id", atts.getValue("id"));
			goTerm.put("chainId", atts.getValue("chainId"));
		}
		else if (qName.equalsIgnoreCase("detail")) {
			goTerm.put("name", atts.getValue("name"));
			goTerm.put("definition", atts.getValue("definition"));
			goTerm.put("synonyms", atts.getValue("synonyms"));
			goTerm.put("ontology", atts.getValue("ontology"));
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("term")) {
			result.add(goTerm);
			goTerm = null;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}
}
