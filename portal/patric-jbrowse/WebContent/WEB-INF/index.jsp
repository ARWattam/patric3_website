<%
String cType = request.getParameter("context_type");
String cId = request.getParameter("context_id");

String params = "";
if (cType!=null && cId!=null) {
	params = "&cType="+cType+"&cId="+cId;
}
String urlRoot = "/patric-jbrowse/jbrowse/";

%>
<link rel="stylesheet" type="text/css" href="<%=urlRoot%>genome.css">
<script type="text/javascript" src="<%=urlRoot%>src/dojo/dojo.js" data-dojo-config="async: 1, baseUrl: '/patric-jbrowse/jbrowse/src' "></script>
<script type="text/javascript" src="<%=urlRoot%>src/JBrowse/init.js"></script>
<script type="text/javascript">
//<![CDATA[ 
	window.onerror=function(msg){
	if( document.body )
		document.body.setAttribute("JSError",msg);
	}
	var JBrowse;
	require(['JBrowse/Browser', 'dojo/io-query', 'dojo/json' ],
		function (Browser,ioQuery,JSON) {
			var queryParams = ioQuery.queryToObject( window.location.search.slice(1) );
			var dataRoot = queryParams.data || '/patric-jbrowse/data';
			var config = {
				containerID: "GenomeBrowser",
				refSeqs: "/portal/portal/patric/GenomeBrowser/GBWindow?action=b&cacheability=PAGE&mode=getRefSeqs<%=params%>",
				baseUrl: dataRoot + '/',
				browserRoot: '<%=urlRoot%>',
				include: [
					'<%=urlRoot%>jbrowse_conf.json',
					dataRoot + "/trackList.jsp"
				],
				nameUrl: dataRoot + "/name.jsp",
				defaultTracks: "DNA,PATRICGenes",
				queryParams: queryParams,
				location: queryParams.loc,
				forceTracks: queryParams.tracks,
				initialHighlight: queryParams.highlight,
				show_nav: queryParams.nav,
				show_tracklist: queryParams.tracklist,
				show_overview: queryParams.overview,
				stores: { url: { type: "JBrowse/Store/SeqFeature/FromConfig", features: [] } },
				makeFullViewURL: function( browser ) {
					return browser.makeCurrentViewURL({ nav: 1, tracklist: 1, overview: 1 });
				},
				refSeqSelectorMaxSize: 100000,
				updateBrowserURL: true
			};

			//if there is ?addFeatures in the query params,
			//define a store for data from the URL
			if( queryParams.addFeatures ) {
				config.stores.url.features = JSON.parse( queryParams.addFeatures );
			}
			
			// if there is ?addTracks in the query params, add
			// those track configurations to our initial
			// configuration
			if( queryParams.addTracks ) {
				config.tracks = JSON.parse( queryParams.addTracks );
			}
			
			// if there is ?addStores in the query params, add
			// those store configurations to our initial
			// configuration
			if( queryParams.addStores ) {
				config.stores = JSON.parse( queryParams.addStores );
			}
			
			// create a JBrowse global variable holding the JBrowse instance
			JBrowse = new Browser( config );
		}
	);
//	var divTopNav, divTabs, divFooter, jbOriginalSize;
	Ext.onReady(function(){
		// window resize
		Ext.get('GenomeBrowser').setHeight(Math.max(600, Ext.getBody().getViewSize().height-500));
			
		// tab configuraion
		if (Ext.get("tabs_genomebrowser")!=null) {
			Ext.get("tabs_genomebrowser").addCls("sel");
		}
		
		// for resize
/*		divTopNav = Ext.get(Ext.query("body > div.main-header")[0]);
		divTabs = Ext.get(Ext.query("body > div.tabs-above")[0]);
		divFooter = Ext.get(Ext.query("footer")[0]);
		
		divTopNav.setVisibilityMode(2);
		divTabs.setVisibilityMode(2);
		divFooter.setVisibilityMode(2);
		jbOriginalSize = Ext.get('GenomeBrowser').getSize();*/
	});
/*	
	function viewFullScreen() {
		divTopNav.setVisible(false, true);
		divTabs.setVisible(false, true);
		divFooter.setVisible(false, true);

		Ext.get("GenomeBrowser").setSize(Ext.getBody().getViewSize());
		// fire resize
	}
	function viewOriginalScreen() {
		divTopNav.setVisible(true, true);
		divTabs.setVisible(true, true);
		divFooter.setVisible(true, true);

		Ext.get("GenomeBrowser").setSize(jbOriginalSize);
	}*/
//]]
</script>
	
	<div id="GenomeBrowser" style="height:600px"></div>
