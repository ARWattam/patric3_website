function createComboBoxes() {
	Ext.create('Ext.form.TextField', {
		id : 'tb_keyword',
		renderTo : 'f_keyword',
		width : 245,
		fieldLabel : 'Keyword',
		labelWidth : 60
	});
}

function filterTable() {
	var Page = $Page, property = Page.getPageProperties(), hash = property.hash;
	hash.aP[0] = 1, hash.kW = Ext.getCmp("tb_keyword").getValue(), createURL();
}

function getExtraParams() {

	var object = {}, Page = $Page, property = Page.getPageProperties(), hash = property.hash;

	object["genome_id"] = getGenomeIDs();
	object["Keyword"] = (!hash.kW) ? '(*)' : "(" + hash.kW + ")";
	var familyType = getFamilyType();
	var familyId = familyType + "_id";
	object[familyId] = getFamilyIds();

	Ext.getDom("download_keyword").value = constructKeyword(object, property.name);

	return {
		callType : "getData",
		keyword : constructKeyword(object, property.name)
	};
}

function CallBack() {
	var Page = $Page, property = Page.getPageProperties(), hash = property.hash, which = hash.aT ? hash.aT : 0, temp_string = "";

	if (Page.getGrid().sortchangeOption)
		Page.getGrid().setSortDirectionColumnHeader();

	Ext.getDom("grid_result_summary").innerHTML = Page.getStore(which).getTotalCount() + ' features found in ';

	if (getFamilyIds().split("##").length == 1)
		Ext.getDom("grid_result_summary").innerHTML += getFamilyIds();
	else
		Ext.getDom("grid_result_summary").innerHTML += getFamilyIds().split("##").length + " protein families";

	temp_string += "<b>";
	temp_string += Ext.getDom("grid_result_summary").innerHTML + "</b>";
	Ext.getDom("grid_result_summary").innerHTML = temp_string;
}

function getSelectedFeatures() {"use strict";

	var Page = $Page, property = Page.getPageProperties(), sl = Page.getCheckBox().getSelections(), i, fids = property.fids;

	for ( i = 0; i < sl.length; i++)
		fids.push(sl[i].data.feature_id);
}

function DownloadFile() {"use strict";

	var Page = $Page, property = Page.getPageProperties(), hash = property.hash, form = Ext.getDom("fTableForm");

	form.action = "/portal/portal/patric/SingleFIGfam/SingleFIGfamWindow?action=b&cacheability=PAGE&callType=download";
	form.target = "";
	form.fileformat.value = arguments[0];
	form.pk.value = hash.key;

	getHashFieldsToDownload(form);
	var grid = Page.getGrid();
	form.sort.value = JSON.stringify(grid.store.sorters.items);
	form.submit();
}
