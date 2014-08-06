<%@ page import="java.util.ArrayList" 
%><%@ page import="edu.vt.vbi.patric.beans.DNAFeature" 
%><%@ page import="edu.vt.vbi.patric.dao.DBSummary" 
%><%@ page import="edu.vt.vbi.patric.dao.ResultType" 
%><%@ page import="edu.vt.vbi.patric.common.SolrInterface" 
%><%@ page import="org.json.simple.JSONObject"
%><%

String fId = request.getParameter("context_id");
String refseqLocusTag = null;
DNAFeature feature = null;
ArrayList<ResultType> featureAnnotations = null;
SolrInterface solr = new SolrInterface();

if (fId!=null) {
	// getting feature info from Solr 
	feature = solr.getPATRICFeature(fId);
}

if (feature != null) {
	
	DBSummary conn_summary = new DBSummary();
		
	if (feature.getAnnotation().equals("PATRIC")) {
		refseqLocusTag = feature.getRefseqLocusTag();
	} 
	else if (feature.getAnnotation().equals("RefSeq")) {
		refseqLocusTag = feature.getLocusTag();
	}
	
	if (refseqLocusTag != null) {
		// get TB Jamboree annotation
		featureAnnotations = conn_summary.getTBAnnotation(refseqLocusTag);
	}
		
%>	
	<% if (featureAnnotations!=null && featureAnnotations.size() > 0) { %>
	<h3 class="section-title normal-case close2x"><span class="wrap">Comments</span></h3>
	<table class="basic stripe far2x">
	<thead>
		<tr>
			<th scope="col">Source</th>
			<th scope="col">Property</th>
			<th scope="col">Value</th>
			<th scope="col">Evidence Code</th>
			<th scope="col">Comment</th>
		</tr>
	</thead>
	<tbody>
	<% for (ResultType an: featureAnnotations) { %>
		<tr>
			<td><%=an.get("source") %>&nbsp;</td>
			<td><%=an.get("property") %>&nbsp;</td>
			<td><%=an.get("value") %>&nbsp;</td>
			<td><%=an.get("evidencecode") %>&nbsp;</td>
			<td><%=an.get("comment").replaceAll("\"\"","\"") %>&nbsp;</td>
		</tr>
	<% } %>
	</tbody>
	</table>
	<% } %>
	
<% } %>