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

public class PDBSequenceClusterHandler extends DefaultHandler {

	private List<Map<String, String>> result;

	private Map<String, String> chain;

	public PDBSequenceClusterHandler() {
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

		if (qName.equalsIgnoreCase("pdbChain")) {
			chain = new HashMap<>();

			chain.put("name", atts.getValue("name"));
			chain.put("rank", atts.getValue("rank"));
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("pdbChain")) {
			result.add(chain);
			chain = null;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// String tmpVal = new String(ch, start, length);
	}
}
