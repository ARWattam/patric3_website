
var $Page,
	ZeroClipboard = null,
	pageProperties = {cart:true};
SetPageProperties(pageProperties);

function formatTaxonName(v, record) {
	return record.data.leaf ? record.data.name: record.data.name + ' (' + record.data.node_count + ')';
}

function loadonDemand() {
	var selector = Ext.getCmp("genome_selector");
	if(selector.getActiveTab().id == 'tab-mygroup'){

	} else {
		selector.storeTree.load();
	}
}

Ext.define('VBI.GenomeSelector.treeNode', {
	extend: 'Ext.ux.tree.tristate.Model',
	fields: [{
		name: 'taxon_name',
		convert: formatTaxonName
	}, {
		name: 'node_count',
		type: 'int'
	}, {
		name: 'parentId',
		type: 'int'
	}]
});

Ext.define('VBI.GenomeSelector.listNode', {
	extend: 'Ext.ux.tree.tristate.Model',
	fields: [{
		name: 'genome_id',
		type: 'string'
	}, {
		name: 'taxon_id',
		type: 'int'
	}, {
		name: 'parentId',
		type: 'int'
	}]
});

Ext.define('VBI.GenomeSelector.myNode', {
	extend: 'Ext.ux.tree.tristate.Model',
	fields: [{
		name: 'genome_id',
		type: 'string'
	}, {
		name: 'taxon_id',
		type: 'int'
	}, {
		name: 'parentId',
		type: 'int'
	}]
});

Ext.define('VBI.GenomeSelector.searchNode', {
	extend: 'Ext.data.Model',
	fields: [{
		name: 'taxon_id',
		type: 'int'
	}, {
		name: 'genome_id',
		type: 'string'
	}, {
		name: 'display_name',
		type: 'string'
	}]
});

Ext.define('VBI.GenomeSelector.Panel', {
	extend: 'Ext.tab.Panel',
	activeTab:1,
	id:'genome_selector',
	txTree: null,
	azList: null,
	tgm: null,
	gsearch: null,
	selectedGenomes: null,
	changing: false,
	isTreeLoaded: false,
	isListLoaded: false,
	storeTree: null,
	storeList: null,
	storeMy: null,
	organismName: 'Bacteria',
	listeners: {
		beforetabchange: function(tabs, newTab, currentTab) {
			
			this.gsearch.clearValue();
			this.gsearch.lastQuery = null;
			this.gsearch.blur();
		},
		tabchange: function(tabs, newTab, currentTab){
		
			if(newTab.id != "tab-mygroup" && this.isListLoaded == false){
				this.storeTree.load();
			}
			else if(newTab.id == "tab-mygroup") {
				this.storeMy.load();
				this.getDockedItems()[1].disable();
			}
			if(newTab.id != "tab-mygroup") {
				this.getDockedItems()[1].enable();
			}
			if (newTab.id == 'tab-txtree' || newTab.id == 'tab-azlist') {
				this.saveGenomesButton.setVisible(true);
			} else {
				this.saveGenomesButton.setVisible(false);
			}
		}
	},
	constructor: function(config) {
		this.organismName = config.organismName;
		var parentTaxon = config.parentTaxon;
		
		this.gsearch = Ext.create('Ext.form.ComboBox', {
			store: Ext.create('Ext.data.Store', {
				model: 'VBI.GenomeSelector.searchNode',
				proxy: {
					type: 'ajax',
					url: '/portal/portal/patric/TaxonomyTree/TaxonomyTreeWindow?action=b&cacheability=PAGE&mode=search&taxonId=' + parentTaxon,
					startParam: undefined,
					limitParam: undefined,
					pageParam: undefined,
					reader: {
						type: 'json',
						root: 'genomeList',
						totalProperty: 'totalCount'
					}
				}
			}),
			typeAhead: false,
			listConfig: {
				itemSelector: 'div.search-item',
				loadingText: 'Searching...',
				cls: 'no-decoration',
				getInnerTpl: function() {
					return '<div class="search-item">{display_name}</div>';
				}
			},
			width: 200,
			hideTrigger: true,
			listeners: {
				scope: this,
				beforequery: function(e) {
					this.gsearch.store.proxy.extraParams.searchon = this.getActiveTab().id.replace("tab-", "");
				},
				select: function(combo, record) {
					if (this.getActiveTab().id == 'tab-txtree') {
						this.openupAncestors(record[0].data.taxon_id);
						this.txTree.view.select(this.txTree.store.getNodeById(record[0].data.taxon_id));
					} else {
						this.azList.view.select(this.azList.store.getNodeById(record[0].data.genome_id));
					}
					combo.collapse();
				}
			}
		});
		
		this.saveGenomesButton = Ext.create('Ext.button.Button', {
			text:'Save to Workspace',
			handler:this.saveToCart,
			scope:this
		});
		
		config.bbar = ['->',  {
			id: 'numSel',
			text: '0',
			xtype: 'label'
		},
		'', {
			xtype: 'label',
			text: '   genome(s) selected'
		}, this.saveGenomesButton];
		
		config.tbar = ['->', {
			xtype: 'tbtext',
			text: 'Jump to: '
		},
		' ', this.gsearch];
		
		config.border = false;
		
		VBI.GenomeSelector.Panel.superclass.constructor.call(this, config);
		
		var myUrl = '/portal/portal/patric/BreadCrumb/WorkspaceWindow?action=b&cacheability=PAGE&action_type=WSSupport&action=getGenomeGroupList';
		var txUrl = '/portal/portal/patric/TaxonomyTree/TaxonomyTreeWindow?action=b&cacheability=PAGE&mode=txtree&taxonId=' + parentTaxon;
		var azUrl = '/portal/portal/patric/TaxonomyTree/TaxonomyTreeWindow?action=b&cacheability=PAGE&mode=azlist&taxonId=' + parentTaxon;
		var tgmUrl = '/portal/portal/patric/TaxonomyTree/TaxonomyTreeWindow?action=b&cacheability=PAGE&mode=tgm&taxonId=' + parentTaxon;
		
		if (parentTaxon == 131567) {
			txUrl = "/patric-common/txtree-bacteria.js";
			azUrl = "/patric-common/azlist-bacteria.js";
			tgmUrl = "/patric-common/tgm-bacteria.js";
		}
		
		this.storeTree = Ext.create('Ext.data.TreeStore', {
			model: 'VBI.GenomeSelector.treeNode',
			proxy: {
				type: 'ajax',
				url: txUrl,
				noCache: false
			},
			autoLoad:false,
			root: {
			    children : []
			},
			listeners:{
				scope:this,
				load: function() {
					var r = this.txTree.getRootNode();
					r.expand(false, false);
					r.firstChild.expand(false, false);
					this.isTreeLoaded = true;
						
					if(!this.isListLoaded){
						this.storeList.load();
					}		
				}
			}
		});
		
		this.storeList = Ext.create('Ext.data.TreeStore', {
			model: 'VBI.GenomeSelector.listNode',
			proxy: {
				type: 'ajax',
				url: azUrl,
				noCache: false
			},
			autoLoad:false,
			root: {
				children : []
			},
			listeners:{
				scope:this,
				load: function() {
					var r = this.azList.getRootNode();
					r.expand(false, false);
					r.firstChild.expand(false, false);
					this.isListLoaded = true;
					if(!this.isTreeLoaded){
						this.storeList.load();
					}
				}
			}
		});
		
		this.storeMy = Ext.create('Ext.data.TreeStore', {
			model: 'VBI.GenomeSelector.myNode',
			proxy: {
				type: 'ajax',
				url: myUrl,
				noCache: false
			},
			root: {
				children : []
			},
			autoLoad:true,
			listeners:{
				scope:this,
				load: function(){
					var r = this.myGroup.getRootNode();
					r.expand(false, false);
					Ext.get('myGroupPanelSouth').dom.childNodes[1].innerHTML = this.getSouthPanelBody();
					this.myGroup.doLayout();	
				}
			}
		});
		
		this.txTree = Ext.create('Ext.tree.Panel', {
			title: 'Taxonomy Tree',
			id: 'tab-txtree',
			width: config.width,
			height: config.height-77,
			store: this.storeTree,
			rootVisible: false,
			hideHeaders: true,
			viewConfig: {
				plugins: {
					ptype: 'tristatetreeplugin'
				}
			},
			columns: [{
				xtype: 'tristatetreecolumn',
				text: 'Name',
				flex: 1,
				dataIndex: 'taxon_name'
			}],
			listeners: {
				scope: this,
				checkchange: function() {
					this.countSelected("tree", this.txTree);
				}
			}
		});
		
		this.azList = Ext.create('Ext.tree.Panel', {
			title: 'A-Z List',
			id: 'tab-azlist',
			width: config.width,
			height: config.height-77,
			store: this.storeList,
			rootVisible: false,
			hideHeaders: true,
			viewConfig: {
				plugins: {
					ptype: 'tristatetreeplugin'
				}
			},
			columns: [{
				xtype: 'tristatetreecolumn',
				text: 'Name',
				flex: 1,
				dataIndex: 'name'
			}],
			listeners: {
				scope: this,
				checkchange: function() {
					this.countSelected("list", this.azList);
				}
			}
		});
		
		this.myGroup = Ext.create('Ext.tree.Panel', {
			//  title: 'My Group',
			id: 'mygroup_tree_panel',
			height:240,
			store: this.storeMy,
			rootVisible: false,
			hideHeaders: true,
			border:false,
			width: config.width-5,	
			viewConfig: {
				plugins: {
					ptype: 'tristatetreeplugin'
				}
			},
			columns: [{
				xtype: 'tristatetreecolumn',
				text: 'Name',
				flex: 1,
				dataIndex: 'name'
			}],
			listeners: {
				scope: this,
				checkchange: function() {
					this.countSelected("group", this.myGroup);
					this.updateMyGenomesPanel();
				},
				load:function(){
					
					if(!Ext.get(Ext.get('genome_selector').dom.childNodes[2].id).hasCls('x-docked-noborder-top')){
						Ext.get(Ext.get('genome_selector').dom.childNodes[2].id).addCls('x-docked-noborder-top');
					}
					Ext.get('mygroup_tree_panel-body').addCls('x-docked-noborder-top');
					
					if(window.location.href.indexOf("FIGfam") > 0){
						if(this.parentTaxon == '2' && this.storeMy.getRootNode().childNodes.length == 0){
							Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML = "<div style=\"padding-left: 5px;font-size: 14px;\">Select genomes from 'Taxonomy Tree' or 'A-Z List' tab<br/><span style=\"padding-left:15px;\"><img src=\"/patric/images/horizonal_rule_OR_302x9.png\"></span><br/> Create custom Genome Groups using <a href=\"GenomeFinder?cType=taxon&cId=&dm=\">Genome Finder</a></div>";	
						}else if(this.parentTaxon == '2' && this.storeMy.getRootNode().childNodes.length > 0){
							Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML = "<div style=\"padding-left: 5px;font-size: 12px;\">Select genomes from 'Taxonomy Tree' or 'A-Z List' tab <br/><span style=\"padding-left:15px;\"><img src=\"/patric/images/horizonal_rule_OR_302x9.png\"></span><br/> Select genomes from custom Genome Groups below </div>";
						}else if(this.parentTaxon != '2' && this.storeMy.getRootNode().childNodes.length == 0){
						Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML = "<div style=\"padding-left: 5px;font-size: 14px;font-weight: bold; \">Search within:<br/><span style=\"padding:15px;\"><i>"+this.organismName+"</i></span></div><div style=\"padding-left:5px;\">Alternatively, <ul><li>Select genomes using "+this.organismName+" 'Taxonomy Tree' or 'A-Z List' tab, or</li><li>Create custom Genome Groups using <a href=\"GenomeFinder?cType=taxon&cId=&dm=\">Genome Finder</a></li></ul></div>";
						
						}else{
							Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML ="<div style=\"padding-left: 5px;font-size: 14px;font-weight: bold; \">Search within:<br/><span style=\"padding:15px;\"><i>"+this.organismName+"</i></span></div><span style=\"padding-left:15px;\"><img src=\"/patric/images/horizonal_rule_OR_302x9.png\"></span><div style=\"padding-left:5px;\"> Select genomes from custom Genome Groups below </div>";
						}
					}
				}
			}
		});

		this.myGroupPanel = Ext.create('Ext.panel.Panel',{
			title:'My Groups',
			width: config.width,
			height: config.height - 68,
			id: 'tab-mygroup',
			layout:'border',
			items:[{
				id:'myGroupPanelNorth',
				region:'north',
				html:'<div style="padding: 5px;font-size: 14px;font-weight: bold; line-height: 20px;">Search within:<br/><span style="padding:15px;"><i>'+this.organismName+'</i></span></div>',
				border:false,
				collapsible:true,
				title:false,
				height:	125,
				listeners:{
					'collapse': function() {
						if(Ext.getCmp('myGroupPanelSouth').collapsed) {
							Ext.getCmp('mygroup_tree_panel').setHeight(355);
						} else {
							Ext.getCmp('mygroup_tree_panel').setHeight(275);
						}
					},
					'expand': function(){
						if(Ext.getCmp('myGroupPanelSouth').collapsed) {
							Ext.getCmp('mygroup_tree_panel').setHeight(295);
						} else {
							Ext.getCmp('mygroup_tree_panel').setHeight(215);
						}
					}
				}
			}, {
				id:'myGroupPanelCenter',
				region:'center',
				items:[this.myGroup],
				border:false,
				autoScroll:true
				},{
				id:'myGroupPanelSouth',
				region:'south',
				html:'',
				border:false,
				collapsible:true,
				height:	105,
				listeners:{
					'collapse': function(){
						if(Ext.getCmp('myGroupPanelNorth').collapsed) {
							Ext.getCmp('mygroup_tree_panel').setHeight(355);
						} else {
							Ext.getCmp('mygroup_tree_panel').setHeight(295);
						}
					},
					'expand': function(){
						if(Ext.getCmp('myGroupPanelNorth').collapsed) {
							Ext.getCmp('mygroup_tree_panel').setHeight(275);
						} else {
							Ext.getCmp('mygroup_tree_panel').setHeight(215);
						}
					}
				}
			}]
		});	
		
		this.add(this.myGroupPanel);
		this.add(this.txTree);
		this.add(this.azList);
		// need to find a way to fix this shifting problem.		
		Ext.get(Ext.getDom('genome_selector').childNodes[2].id).dom.style.top = '50px'
		this.tgm = new Ext.util.MixedCollection(true, function(el) {
			return el.genome_id;
		});
		var outer_scope = this;
		
		Ext.Ajax.request({
			url: tgmUrl,
			disableCaching: false,
			method: "GET",
			scope: outer_scope,
			success: function(rs) {
				this.tgm.addAll(Ext.JSON.decode(rs.responseText));
			}
		});
		
		this.setActiveTab(0);
	
	var btnGroupPopupSave = $Page.getCartSaveButton();
	
		btnGroupPopupSave.on('click', function() {
			var selector = Ext.getCmp("genome_selector");	
			saveToGroup(selector.getSelectedInString(), "Genome");
		});
	},
	getSouthPanelBody: function() {
		if(loggedIn){
			if (this.storeMy.getRootNode().childNodes != null && this.storeMy.getRootNode().childNodes.length > 0) {
				return '<div></div>';
			} else {
				return '<div style="padding: 3px;font-size: 16px;font-weight: bold; line-height: 18px;">WANT TO CREATE GENOME GROUPS?<br/><img src="/patric/images/toolbar_cart.png" alt="" style="padding: 10px 10px 5px 5px; float:left"/></div><div>Click the "Add Genomes" icon<br/>from any list of genomes<br/>throughout PATRIC<a target="_blank" href="http://enews.patricbrc.org/faqs/workspace-faqs/registration-faqs/" style="float: right;padding: 0px 20px;">Learn more</a></div>';
			}
		} else {
			if (this.storeMy.getRootNode().childNodes != null && this.storeMy.getRootNode().childNodes.length > 0) {
				return '<div style="padding: 3px;font-size: 16px;font-weight: bold; line-height: 18px;">WANT TO SAVE WORKSPACE GENOMES?<br/></div><div style="padding: 5px;"><a target="_blank" href="https://www.patricbrc.org/portal/portal/patric/MyAccount/PATRICUserPortletWindow?_jsfBridgeViewId=%2Fjsf%2Findex.xhtml&action=1">Sign up</a> for a PATRIC account<br/>to save custom sets of<br/>workspace genomes and more<a target="_blank" href="http://enews.patricbrc.org/faqs/workspace-faqs/registration-faqs/" style="float: right;padding: 0px 20px;">Learn more</a></div>';
			} else {
				return '<div style="padding: 3px;font-size: 16px;font-weight: bold; line-height: 18px;">WANT TO CREATE GENOME GROUPS?<br/><img src="/patric/images/toolbar_cart.png" alt="" style="padding: 10px 10px 5px 5px; float:left"/></div><div>Click the "Add Genomes" icon<br/>from any list of genomes<br/>throughout PATRIC<a target="_blank" href="http://enews.patricbrc.org/faqs/workspace-faqs/registration-faqs/" style="float: right;padding: 0px 20px;">Learn more</a></div>';
			}
		}
	},
	updateMyGenomesPanel: function() {
		
		if(this.selectedGenomes.length > 0){
			Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML = '<div style="padding: 5px;font-size: 14px;font-weight: bold; line-height: 20px;">Search within:<br/><span style="padding:15px;"><i>selected Workspace genomes</i></span></div>';
		}else{
			Ext.getCmp('myGroupPanelNorth').body.dom.innerHTML = '<div style="padding: 5px;font-size: 14px;font-weight: bold; line-height: 20px;">Search within:<br/><span style="padding:15px;"><i>'+this.organismName+'</i></span></div>';
		}
	},
	saveToCart: function() {
		if(this.selectedGenomes!= null && this.selectedGenomes.length > 0){
			addSelectedItems("Genome");
		}else{
			alert("Choose genomes");
		}
	},
	initializeTree: function() {
		if (this.isTreeLoaded == true && this.isListLoaded == true) {
			//this.loadingMask.hide();
		}
	},
	openupAncestors: function(id) 
	{
		while (this.storeTree.getNodeById(id).parentNode.isExpanded() == false)
		{
			this.storeTree.getNodeById(id).bubble(function(n) {
				if (n != null && n.isExpanded() == false && n.parentNode != null && n.parentNode.isExpanded() == true) {
					n.expand();
					return false;
				}
			});
		}
	},
	copy_to_azList: function() {
		if (this.txTree != null && this.azList != null) {
			//uncheck only deselected
			Ext.each(this.azList.getChecked(), function(node) {
				target = this.storeTree.getNodeById(node.data.taxon_id);
				if (target.get('checked') == false) {
					this.azList.getView().updateRecord(node, false);
				}
			}, this);
			
			//update from selectedGenomes
			if (this.selectedGenomes != null) {
				Ext.each(this.selectedGenomes, function(genomeID) {
					node = this.storeList.getNodeById(genomeID);
					if (node != null) {
						this.azList.getView().updateRecord(node, true);
					}
				}, this);
			}
		}
	},
	copy_to_txTree: function() {
		if (this.txTree != null && this.azList != null) {
			//uncheck only deselected
			Ext.each(this.txTree.getChecked(), function(node) {
				if (node.get('partial') != true) {
					var t = this.tgm.filterBy(function(o, k) {
						if (o.taxon_id == node.get('id')) {
							if (this.storeList.getNodeById(o.genome_id).get('checked') == false) {
								this.txTree.getView().updateRecord(node, false);
								node.updateInfo();
							}
							return true;
						}
					}, this);
				}
			}, this);
			
			//update from azList
			Ext.each(this.azList.getChecked(), function(node) {
				target = this.storeTree.getNodeById(node.get('taxon_id'));
				if (target != null
					&& node.get('genome_id') > 0
					&& target.get('partial') == false
					&& target.get('checked') == false)
				{
					this.openupAncestors(node.get('taxon_id'));
					this.txTree.getView().updateRecord(target, true);
				}
			},
			this);
		}
	},
	countSelected: function(name, src) {
		this.selectedGenomes = new Array();
		//console.log("counting selection");
		if (name == "tree") {
			Ext.each(src.getChecked(), function(node) {
				if (node.get('partial') != true) {
					var t = this.tgm.filterBy(function(o, k) {
						if (o.taxon_id == node.get('id')) {
							this.selectedGenomes.push(o.genome_id);
							return true;
						}
					},
					this);
				}
			},
			this);
		} else if (name == "list") {
			Ext.each(src.getChecked(), function(node) {
				if (node.get('genome_id') != '') {
					this.selectedGenomes.push(node.get('genome_id'));
				}
			},
			this);
		}else if (name == "group") {
			Ext.each(src.getChecked(), function(node) {
				if (node.get('genome_id') != '') {
					this.selectedGenomes.push(node.get('genome_id'));
				}
			},
			this);
		}
		
		Ext.getDom("numSel").innerHTML = this.selectedGenomes.length;
	},
	showSelected: function() {
		Ext.Msg.show({
			title: 'Selected Genomes',
			msg: this.selectedGenomes.length > 0 ? this.selectedGenomes.join(',') : 'None',
			icon: Ext.Msg.INFO,
			minWidth: 200,
			buttons: Ext.Msg.OK
		});
	},
	getSelected: function() {
		return this.selectedGenomes;
	},
	getSelectedInString: function() {
		if (this.selectedGenomes != null) {
			if (this.selectedGenomes != null && this.selectedGenomes.length > 0) {
				return this.selectedGenomes.join(',');
			}
		} else {
			return "";
		}
	}
})
