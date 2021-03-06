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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.vt.vbi.patric.mashup.PRIDEInterface;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.ExperimentDescription;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.InteractionType;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Organism;
import psidev.psi.mi.xml.model.Participant;

@SuppressWarnings("unchecked")
public class IntActHandler {

	private JSONArray list = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(PRIDEInterface.class);

	public IntActHandler(String url) {
		list = new JSONArray();

		try {
			PsimiXmlReader api = new PsimiXmlReader();
			EntrySet entrySet = api.read(new URL(url));
			Iterator<Interaction> intrnItr = null;
			Iterator<Interactor> intrrItr = null;
			Iterator<ExperimentDescription> expItr = null;
			for (Entry entry : entrySet.getEntries()) {
				expItr = entry.getExperiments().iterator();
				intrnItr = entry.getInteractions().iterator();
				intrrItr = entry.getInteractors().iterator();
			}
			HashMap<String, HashMap<String, String>> mapExp = new HashMap<>();
			HashMap<String, HashMap<String, String>> mapInteractor = new HashMap<>();

			// Parsing Interactors
			assert intrrItr != null;
			while (intrrItr.hasNext()) {
				HashMap<String, String> itrr = new HashMap<>();
				Interactor itrr_desc = intrrItr.next();
				itrr.put("id", "" + itrr_desc.getId());
				itrr.put("name", itrr_desc.getNames().getShortLabel());
				itrr.put("type", itrr_desc.getInteractorType().getNames().getShortLabel());
				if (itrr_desc.hasOrganism() && itrr_desc.getOrganism().hasNames()) {
					if (itrr_desc.getOrganism().getNames().hasShortLabel()) {
						itrr.put("organism", itrr_desc.getOrganism().getNames().getShortLabel());
					}
					else {
						itrr.put("organism", itrr_desc.getOrganism().getNames().getFullName());
					}
					itrr.put("organism_taxon_id", "" + itrr_desc.getOrganism().getNcbiTaxId());
				}
				else {
					itrr.put("organism", "");
					itrr.put("organism_taxon_id", "");
				}

				StringBuilder sb = new StringBuilder();
				sb.append("<table border=1>");
				sb.append("<tr><td>name:</td><td>").append(itrr.get("name")).append("</td></tr>");
				sb.append("<tr><td>type:</td><td>").append(itrr.get("type")).append("</td></tr>");
				sb.append("<tr><td>organism:</td><td>").append(itrr.get("organism")).append("</td></tr>");
				sb.append("</table>");

				itrr.put("html", sb.toString());
				mapInteractor.put(itrr.get("id"), itrr);
			}

			// Parsing Experiments
			while (expItr.hasNext()) {
				HashMap<String, String> exp = new HashMap<>();
				ExperimentDescription exp_desc = expItr.next();
				exp.put("id", "" + exp_desc.getId());
				exp.put("name", exp_desc.getNames().getShortLabel());
				ArrayList<Organism> hosts = (ArrayList<Organism>) exp_desc.getHostOrganisms();
				// for (int i=0; i< hosts.size(); i++) {
				int i = 0;
				exp.put("host_taxon_id", "" + hosts.get(i).getNcbiTaxId());
				exp.put("host_name", hosts.get(i).getNames().getShortLabel());
				// }
				exp.put("method", exp_desc.getInteractionDetectionMethod().getNames().getFullName());
				if (exp_desc.getBibref().getXref().getPrimaryRef().getDb().equals("pubmed")) {
					exp.put("pubmed_id", exp_desc.getBibref().getXref().getPrimaryRef().getId());
				}

				StringBuffer sb = new StringBuffer();
				sb.append("<table border=1>");
				sb.append("<tr><td>name:</td><td>").append(exp.get("name")).append("</td></tr>");
				sb.append("<tr><td>method:</td><td>").append(exp.get("method")).append("</td></tr>");
				sb.append("<tr><td>organism:</td><td>").append(exp.get("host_name")).append("</td></tr>");
				sb.append("<tr><td>publication:</td><td>").append(exp.get("pubmed_id")).append("</td></tr>");
				sb.append("</table>");

				exp.put("html", sb.toString());
				mapExp.put(exp.get("id"), exp);
			}

			// Parsing Interactions
			while (intrnItr.hasNext()) {
				JSONObject row = new JSONObject();
				Interaction itrn = intrnItr.next();
				ArrayList<ExperimentDescription> arrExpRef = (ArrayList<ExperimentDescription>) itrn.getExperiments();
				ArrayList<Participant> arrParticipants = (ArrayList<Participant>) itrn.getParticipants();
				ArrayList<InteractionType> arrItrnType = (ArrayList<InteractionType>) itrn.getInteractionTypes();

				row.put("id", itrn.getId());
				row.put("label", itrn.getNames().getShortLabel());
				row.put("interaction_type", arrItrnType.get(0).getNames().getShortLabel());
				row.put("interaction_ac", itrn.getXref().getPrimaryRef().getId());
				row.put("count_exp_ref", arrExpRef.size());
				StringBuilder exps = new StringBuilder();
				String exp_id = "";
				exps.append("<table><tr>");

				for (ExperimentDescription anArrExpRef : arrExpRef) {
					exp_id = "" + anArrExpRef.getId();
					exps.append("<td>");
					exps.append(mapExp.get(exp_id).get("html"));
					exps.append("</td>");
				}
				exps.append("</tr></table>");
				row.put("experiments", exps.toString());

				//
				row.put("exp_name", mapExp.get(exp_id).get("name"));
				row.put("exp_method", mapExp.get(exp_id).get("method"));
				row.put("exp_org", mapExp.get(exp_id).get("host_name"));
				row.put("exp_pubmed", mapExp.get(exp_id).get("pubmed_id"));
				row.put("count_participants", arrParticipants.size());
				StringBuilder participants = new StringBuilder();
				participants.append("<table><tr>");
				for (Participant arrParticipant : arrParticipants) {
					participants.append("<td>");
					participants.append(mapInteractor.get("" + arrParticipant.getInteractor().getId()).get("html"));
					participants.append("</td>");
				}
				participants.append("</tr></table>");
				row.put("participants", participants.toString());
				row.put("count_interaction_type", arrItrnType.size());

				list.add(row);
			}
		}
		catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
		}
	}

	public JSONArray getParsedJSON() {
		return list;
	}
}
