function loadFBCD() {
	var Page = $Page, property = Page.getPageProperties(), hash = property.hash, checkbox = Page.getCheckBox() || null, plugin = property.plugin, plugintype = property.plugintype, which = hash.hasOwnProperty('cat') ? hash.cat : hash.aT ? hash.aT : 0, header, requested_data;

	(plugin && plugintype == "checkbox" && !checkbox) ? checkbox = Page.checkbox = createCheckBox(property.name) : checkbox.updateCheckAllIcon();


	if (hash.to == "refseq_locus_tag") {
		header = "RefSeq Locus Tag";
		requested_data = hash.to;
	}
	else if (hash.to == "protein_id") {
	    header = "Protein ID";
	    requested_data = hash.to;
	}
	else if (hash.to == "gene_id") {
	    header = "Gene ID";
	    requested_data = hash.to;
	}
	else if (hash.to == "gi") {
	    header = "GI";
	    requested_data = hash.to;
	}
	else if (hash.to == "feature_id") {
		header = "PATRIC ID";
		requested_data = hash.to;
	}
    else if (hash.to == "alt_locus_tag") {
        header = "Alt Locus Tag";
        requested_data = hash.to;
    }
	else if (hash.to == "seed_id") {
		if (hash.from == "refseq_locus_tag") {
		    header = "RefSeq Locus Tag"
			requested_data = hash.from;
		}
		else if (hash.from == "protein_id") {
		    header = "RefSeq";
			requested_data = hash.from;
		}
		else if (hash.from == "gene_id") {
		    header = "Gene ID";
			requested_data = hash.from;;
		}
		else if (hash.from == "gi") {
		    header = "GI";
			requested_data = hash.from;
		}
		else if (hash.from == "feature_id") {
		    header = "PATRIC ID";
			requested_data = hash.from;
		}
		else if (hash.from == "alt_locus_tag") {
		    header = "Alt Locus Tag";
			requested_data = hash.from;
		}
		else if (hash.from == "seed_id") {
		    header = "Seed ID";
		    requested_data = hash.from;
		}
		else {
		    header = hash.from;
			requested_data = "target";
		}
	}
	else {
		header = hash.to;
		requested_data = "target";
	}

	if (!property.scm[which]) {
			property.scm[which] =  [checkbox,
			    {header:'Genome Name', flex:2, dataIndex: 'genome_name', renderer:renderGenomeName},
                {header:'Accession', flex:1, align:'center', hidden: true, dataIndex: 'accession', renderer:renderAccession},
			    {header:'Seed ID', flex:1, align:'center', dataIndex: 'seed_id', renderer:renderSeedId},
			    {header:'RefSeq Locus Tag', flex:1, align:'center', dataIndex: 'refseq_locus_tag', renderer:renderLocusTag},
			    {header:'Alt Locus Tag', flex:1, align:'center', dataIndex: 'alt_locus_tag', renderer:renderLocusTag},
			    {header: header, flex:1, align:'center', dataIndex: requested_data, renderer:renderURL},
			    {header:'Genome Browser', flex:1, hidden: true, dataIndex:'feature_id', align:'center', renderer:renderGenomeBrowserByFeatureIDMapping},
			    {header:'Start', flex:1, hidden: true, dataIndex:'start', align:'center', renderer:BasicRenderer},
			    {header:'End', flex:1, hidden: true, dataIndex:'end', align:'center', renderer:BasicRenderer},
			    {header:'Length (NT)', flex:1, hidden: true, dataIndex:'na_length', align:'center', renderer:BasicRenderer},
			    {header:'Strand', flex:1, hidden: true, dataIndex:'strand',align:'center', renderer:BasicRenderer},
			    {header:'Product', flex:2, dataIndex:'product', renderer:BasicRenderer}]
	}
	loadMemStore();
}

function getExtraParams() {

	var Page = $Page, property = Page.getPageProperties(), hash = property.hash;

	return {
		pk : hash.key
	};
}

function getSelectedFeatures() {"use strict";

	var Page = $Page, property = Page.getPageProperties(), sl = Page.getCheckBox().getSelections(), i, fids = property.fids;

	for ( i = 0; i < sl.length; i++)
		fids.push(sl[i].data.feature_id);

}

function DownloadFile() {"use strict";

	var form = Ext.getDom("fTableForm");

	form.action = "/patric-searches-and-tools/jsp/grid_download_handler.jsp";
	form.fileformat.value = arguments[0];
	form.target = "";
	getHashFieldsToDownload(form);
	form.submit();
}

function renderGenomeBrowserByFeatureIDMapping(value, p, record) {

	var tracks = "DNA,PATRICGenes,RefSeqGenes", Page = $Page, property = Page.getPageProperties(), hash = property.hash, window_start = Math.max(0, (record.data.start_max - 1000)), window_end = parseInt(record.data.end_min) + 1000;

	return Ext.String.format('<a href="GenomeBrowser?cType=feature&cId={0}&loc={1}:{2}..{3}&tracks={4}"><img src="/patric/images/icon_genome_browser.gif"  alt="Genome Browser" style="margin:-4px" /></a>', value, record.data.accession, window_start, window_end, tracks);
}

function CallBack() {
	var Page = $Page, property = Page.getPageProperties(), hash = property.hash, which = hash.hasOwnProperty('cat') ? hash.cat : hash.aT ? hash.aT : 0, store = Page.getStore(which), requested_data = "id";
/*
	if (hash.to == "UniProtKB-ID")
		requested_data = "uniprotkb_accession";
	else if (hash.to == "RefSeq Locus Tag")
		requested_data = "refseq_source_id";
	else if (hash.to == "RefSeq")
		requested_data = "rm.protein_id";
	else if (hash.to == "Gene ID")
		requested_data = "gene_id";
	else if (hash.to == "GI")
		requested_data = "gi_number";
	else if (hash.to == "PATRIC Locus Tag")
		requested_data = "source_id";
	else if (hash.to == "PATRIC ID")
		requested_data = "na_feature_id";
	else if (hash.to == "PSEED ID")
		requested_data = "pseed_id";
*/
//	Ext.Ajax.request({
//		url : "/patric-searches-and-tools/jsp/get_idmapping_to_count.json.jsp",
//		method : 'POST',
//		params : {
//			field : requested_data,
//			from : hash.from,
//			to : hash.to,
//			keyword : Ext.getDom("keyword").value
//		},
//		success : function(response, opts) {
//			if (store.getTotalCount() > property.keyword_size)
//				Ext.getDom('grid_result_summary').innerHTML = "<b>" + property.keyword_size + " out of " + property.keyword_size + " " + hash.from + "s mapped to " + Ext.JSON.decode(response.responseText).result + " " + hash.to + "s</b><br/>";
//			else
//				Ext.getDom('grid_result_summary').innerHTML = "<b>" + store.totalCount + " out of " + property.keyword_size + " " + hash.from + "s mapped to " + Ext.JSON.decode(response.responseText).result + " " + hash.to + "s</b><br/>";
//		}
//	});

	Ext.getDom('grid_result_summary').innerHTML = "<b>" + store.totalCount + " features found</b>"
}

function renderURL(value, p, record) {
	var Page = $Page, property = Page.getPageProperties();

	if (property.renderURL)
		return Ext.String.format("<a href=\"" + property.renderURL + "{0}\" target=\"_blank\">{0}</a>", value);
	else
		return value;
}

