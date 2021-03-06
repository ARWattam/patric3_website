<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects/>
<%
String contextType = (String) request.getAttribute("contextType");
String contextId = (String) request.getAttribute("contextId");
String pk = (String) request.getAttribute("pk");
String featureIds = (String) request.getAttribute("featureIds");
%>
<form id="fTableForm" action="#" method="post">
<input type="hidden" id="cType" name="cType" value="<%=contextType %>" />
<input type="hidden" id="cId" name="cId" value="<%=contextId %>" />
<input type="hidden" id="_tablesource" name="_tablesource" value="TranscriptomicsGeneFeature" />
<input type="hidden" id="fileFormat" name="fileFormat" value="" />

<!-- fasta download specific param -->
<input type="hidden" id="fastaaction" name="fastaaction" value="" />
<input type="hidden" id="fastatype" name="fastatype" value="" />
<input type="hidden" id="fastascope" name="fastascope" value="" />
<input type="hidden" id="fids" name="fids" value="" />
<input type="hidden" id="featureIds" name="featureIds" value="<%=featureIds %>" />
<input type="hidden" id="pk" name="pk" value="<%=pk%>" />
<input type="hidden" id="sort" name="sort" value="" />
<input type="hidden" id="dir" name="dir" value="" />
</form>

<div id="copy-button" style="display:none;"></div>
<div>
	<div id="grid_result_summary"></div>
	<p>
	Feature tables contain all of the identified features for all of the genomes in a particular genus.  
	Tables may be refined to show subsets of features via various user controls, as described in <a href="http://enews.patricbrc.org/faqs/feature-table-faqs/" target="_blank">Feature Table User Guide</a>.
	</p>
</div>
<div id='PATRICGrid'></div>

<script type="text/javascript" src="/patric-common/js/ZeroClipboard.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/copybutton.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/checkcolumn.js"></script>
<script type="text/javascript" src="/patric-common/js/parameters.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/loadgrid.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/pagingbar.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/toolbar.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/gridoptions.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/PATRICSelectionModel.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/PATRICGrid.js"></script>
<script type="text/javascript" src="/patric-common/js/grid/table_checkboxes.js"></script>
<script type="text/javascript" src="/patric/js/vbi/AddToWorkspace.min.js"></script>

<script type="text/javascript">
//<![CDATA[
var $Page;

Ext.onReady(function()
{
	var checkbox = createCheckBox("Feature");

	Ext.define('Feature', {
		extend: 'Ext.data.Model',
		fields: [
			{name:'genome_id',	type:'string'},
			{name:'genome_name',	type:'string'},
			{name:'accession',	type:'string'},
			{name:'patric_id',	type:'string'},
			{name:'alt_locus_tag',	type:'string'},
			{name:'refseq_locus_tag',	type:'string'},
			{name:'feature_id',	type:'string'},
			{name:'annotation',	type:'string'},
			{name:'feature_type',		type:'string'},
			{name:'start',	type:'int'},
			{name:'end',	type:'int'},
			{name:'na_length',	type:'int'},
			{name:'strand',		type:'string'},
			{name:'protein_id',	type:'string'},
			{name:'aa_length',	type:'int'},
			{name:'gene',		type:'string'},
			{name:'product',	type:'string'}
		]
	});
	
	var pageProperties = {
		name: "Feature",
		model: ["Feature"],
		items: 1,
		cart: true,
		cartType: "cart",
		plugin:true,
		plugintype:"checkbox",
		scm:[[checkbox,
				{text:'Genome Name',			dataIndex:'genome_name',		flex:2, renderer:renderGenomeName},
				{text:'Accession',				dataIndex:'accession',			flex:1, hidden:true, renderer:renderAccession},
				{text:'PATRIC ID',				dataIndex:'patric_id',			flex:1, renderer:renderSeedId},
				{text:'RefSeq Locus Tag',		dataIndex:'refseq_locus_tag',	flex:1, renderer:renderLocusTag},
				{text:'Alt Locus Tag',			dataIndex:'alt_locus_tag',		flex:1, renderer:renderLocusTag},
				{text:'Gene Symbol',			dataIndex:'gene',				flex:1, renderer:BasicRenderer},
				{text:'Genome Browser',			dataIndex:'',					flex:1, hidden:true, align:'center', sortable:false, renderer:renderGenomeBrowserByFeature},
				{text:'Annotation',				dataIndex:'annotation',			flex:1, hidden:true, renderer:BasicRenderer},
				{text:'Feature Type',			dataIndex:'feature_type',		flex:1,	hidden:true, renderer:BasicRenderer}, 
				{text:'Start',					dataIndex:'start',		    	flex:1, hidden:true, align:'right', renderer:BasicRenderer},
				{text:'End', 					dataIndex:'end',	    		flex:1, hidden:true, align:'right', renderer:BasicRenderer},
				{text:'Length (NT)',			dataIndex:'na_length',			flex:1, hidden:true, align:'right', renderer:BasicRenderer},
				{text:'Strand',					dataIndex:'strand',				flex:1, hidden:true, align:'right', renderer:BasicRenderer},
				{text:'Length (AA)',			dataIndex:'aa_length',			flex:1, hidden:true, align:'right', renderer:BasicRenderer},
				{text:'Product Description',	dataIndex:'product',			flex:3, renderer:BasicRenderer}]],
		extraParams:getExtraParams,
		callBackFn:CallBack,
		sort: [[{
			property: 'genome_name',
			direction: 'ASC'
		}, {
			property: 'accession',
			direction:'ASC'
		},{
			property: 'start',
			direction:'ASC'
		}]],
		hash:{
			aP: [1]
		},
		remoteSort:true,
		fids: [],
		gridType: "Feature",
		current_hash: window.location.hash?window.location.hash.substring(1):"",
		url: ['<portlet:resourceURL />'],
		loaderFunction: function(){SetLoadParameters();loadGrid();},
		stateId: ['TRhmfeaturelist']
	};
	
	SetPageProperties(pageProperties);
	$Page.checkbox = checkbox;
	SetIntervalOrAPI();
	Ext.QuickTips.init();
	if(Ext.get("tabs_explist"))
		Ext.get("tabs_explist").addCls("sel");
	overrideButtonActions(),
	loadGrid();
});

function getExtraParams(){	
	return {
		pk:Ext.getDom("pk").value
		,callType:'getFeatureTable'
	};
}

function CallBack(){
	
	var Page = $Page,
		property = Page.getPageProperties(),
		hash = property.hash,
		which = hash.aT?hash.aT:0;
	
	if(Page.getGrid().sortchangeOption)
		Page.getGrid().setSortDirectionColumnHeader();
	
	Ext.getDom("grid_result_summary").innerHTML = '<b>'+Page.getStore(which).getTotalCount()+' features found</b>';
}


function DownloadFile(type){
	"use strict";
	
	var form = Ext.getDom("fTableForm"), Page = $Page;
	
	form.action = "/portal/portal/patric/TranscriptomicsGeneFeature/TranscriptomicsGeneFeatureWindow?action=b&cacheability=PAGE&callType=download";
	form.target = "";
	form.fileFormat.value = arguments[0];
	getHashFieldsToDownload(form);

	var grid = Page.getGrid();
	form.sort.value = JSON.stringify(grid.store.sorters.items);
	form.submit();
}

function getSelectedFeatures() {
	"use strict";
	
	var Page = $Page,
		property = Page.getPageProperties(),
		sl = Page.getCheckBox().getSelections(),
		i,
		fids = property.fids;
	
	for (i=0; i<sl.length;i++) {
		fids.push(sl[i].data.feature_id);
	}
}
// ]]
</script>
