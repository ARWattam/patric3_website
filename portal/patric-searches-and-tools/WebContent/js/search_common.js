function loadComboFacets() {

	var newOption = "";

	for (var i = 0; i < configuration[name].search_fields.length; i++) {

		newOption = document.createElement('option');
		if (Ext.getDom("search_on").value == configuration[name].search_fields[i]) {
			newOption.selected = "selected";
		}

		newOption.text = configuration[name].search_fields[i].text;
		newOption.value = configuration[name].search_fields[i].value;
		document.getElementById("search_on").options.add(newOption);
	}

}

function searchbykeyword(cId, cType) {
	var object = {};
	var genomeId = "";

	if (name == "Feature") {
		object["annotation"] = Ext.getDom("annotation").value;
		object["feature_type"] = Ext.getDom("feature_type").value;

		if (Ext.getDom("keyword").value == "" || Ext.getDom("keyword").value == "*") {
			object["Keyword"] = "(*)";
		} else {
			object["Keyword"] = "(" + ConvertNewlineforSolrQuery(EncodeKeyword(Ext.getDom("keyword").value)) + ")";
		}
	}
	else if (name == "Genome") {
		if (Ext.getDom("keyword").value == "" || Ext.getDom("keyword").value == "*") {
			if (Ext.getDom("search_on").value == "Keyword") {
				object["Keyword"] = "(*)";
			} else {
				object[Ext.getDom("search_on").value] = "*";
			}
		}
		else {
			if (Ext.getDom("search_on").value == "Keyword") {
				object["Keyword"] = "(" + ConvertNewlineforSolrQuery(EncodeKeyword(Ext.getDom("keyword").value)) + ")";
			} else {
				object[Ext.getDom("search_on").value] = ConvertNewlineforSolrQuery(EncodeKeyword(Ext.getDom("keyword").value));
				object["Keyword"] = "(*)";
			}
		}
	}
	else if (name == "AntibioticResistanceGeneMapping") {
		var checkboxes = document.getElementsByName("source");
		var selectedSources = [];
		for (var i=0; i < checkboxes.length; i++) {
			if (checkboxes[i].checked) {
				selectedSources.push(checkboxes[i].value);
			}
		}
		if (selectedSources.length > 0) {
			object["source"] = selectedSources.join("##");
		}
		
		checkboxes = document.getElementsByName("evidence");
		var selectedEvidence = [];
		for (i = 0; i < checkboxes.length; i++) {
			if (checkboxes[i].checked) {
				selectedEvidence.push(checkboxes[i].value);
			}
		}
		if (selectedEvidence.length > 0) {
			object["evidence"] = selectedEvidence.join("##");
		}
		
		if (Ext.getDom("keyword").value == "" || Ext.getDom("keyword").value == "*") {
			object["Keyword"] = "(*)";
		} else {
			object["Keyword"] = "(" + ConvertNewlineforSolrQuery(EncodeKeyword(Ext.getDom("keyword").value)) + ")";
		}
		object["property"] = "\"Antibiotic Resistance\"";
	}
	else if (name == "SpecialtyGeneMapping") {
		var checkboxes = document.getElementsByName("property");
		var selectedProperties = [], i;
		for (i=0; i < checkboxes.length; i++) {
			if (checkboxes[i].checked) {
				selectedProperties.push(checkboxes[i].value);
			}
		}
		if (selectedProperties.length > 0) {
			object["property"] = selectedProperties.join("##");
		}

		checkboxes = document.getElementsByName("evidence");
		var selectedEvidence = [];
		for (i = 0; i < checkboxes.length; i++) {
			if (checkboxes[i].checked) {
				selectedEvidence.push(checkboxes[i].value);
			}
		}
		if (selectedEvidence.length > 0) {
			object["evidence"] = selectedEvidence.join("##");
		}
		
		if (Ext.getDom("keyword").value == "" || Ext.getDom("keyword").value == "*") {
			object["Keyword"] = "(*)";
		} else {
			object["Keyword"] = "(" + ConvertNewlineforSolrQuery(EncodeKeyword(Ext.getDom("keyword").value)) + ")";
		}
	}

	if (cType == "genome") {
		if (cId != "") {
			object["gid"] = cId;
			search_(constructKeyword(object, name, ""), cId, cType);
		}
	} else if (cType == "taxon") {
		if (cId != "" && cId != "131567") {
			var genomes = tabs.getSelectedInString().replace(/,/g, "##");
			if (genomes != null && genomes != "") {
				//object["gid"] = genomes;
				object["genome_id"] = genomes;
				search_(constructKeyword(object, name, ""), cId, cType);
			} else {
				genomeId = "";
				// need_genome_info_id = true;
				search_(constructKeyword(object, name, ""), cId, cType);
			}
		} else if (cId == "131567" || cId == "") {
			var genomes = tabs.getSelectedInString().replace(/,/g, "##");
			if (genomes != null && genomes != "") {
				//object["gid"] = genomes;
				object["genome_id"] = genomes;
				search_(constructKeyword(object, name, ""), cId, cType);
			} else {
				search_(constructKeyword(object, name, ""), cId, cType);
			}
		}
	}
}

function search_(keyword, cId, cType) {
	Ext.Ajax.request({
		url : url,
		method : 'POST',
		params : {
			cType : cType,
			cId : cId,
			sraction : "save_params",
			keyword : keyword.replace(/\"/g, "%22").replace(/'/g, "%27").trim(),
			exact_search_term : Ext.getDom("keyword").value.trim(),
			search_on : Ext.getDom("search_on") != null ? Ext.getDom("search_on").value : ''
		},
		success : function(rs) {
			if (name == "Genome") {
				document.location.href = "GenomeFinder?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
			else if (name == "Feature") {
				document.location.href = "GenomicFeature?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
			else if (name == "GO") {
				document.location.href = name + "Search?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
			else if (name == "EC") {
				document.location.href = name + "Search?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
			else if (name == "AntibioticResistanceGeneMapping") {
				document.location.href = "AntibioticResistanceGeneSearch?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
			else if (name == "SpecialtyGeneMapping") {
				document.location.href = "SpecialtyGeneSearch?cType=" + cType + "&cId=" + cId + "&dm=result&pk=" + rs.responseText;
			}
		}
	});

}

function updateFields() {
	var parts = window.location.href.split("#");

	if (parts[1] != null) {
		var hash = parts[1].split("&");

		for (var i = 0; i < hash.length; i++) {
			if (Ext.getDom(hash[i].split("=")[0]) != null) {
				Ext.getDom(hash[i].split("=")[0]).value = Ext.urlDecode("result=" + hash[i].split("=")[1]).result;
			}
		}
	}
}

function Combo_Change() {
	Ext.Ajax.request({
		url : "/patric-searches-and-tools/jsp/get_metadata_facet_desc.json.jsp",
		method : 'GET',
		params : {
			search_on : Ext.getDom("search_on").value
		},
		success : function(response, opts) {
			Ext.getDom("expander").innerHTML = response.responseText;
		}
	});
} 