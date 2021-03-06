<%@ page import="javax.portlet.ResourceURL" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%
String contextType = (String) request.getAttribute("contextType");
String contextId = (String) request.getAttribute("contextId");

%>
<div id="tbl_transcriptomics" class="table-container">
	<span style="float:right">Retrieving data...&nbsp;
		<img src="/patric/images/icon_please_wait.gif" alt="Please Wait" style="vertical-align:middle" />
	</span>
	<div style="clear:both"></div>
</div>

<script type="text/javascript">
//<![CDATA[
Ext.onReady(function () {
	Ext.Ajax.request({
		url: '<portlet:resourceURL />',
		method: 'GET',
		params: {cType:'<%=contextType%>',cId:'<%=contextId%>'},
		success: function(rs) {
			Ext.getDom("tbl_transcriptomics").innerHTML = rs.responseText;
		}
	});
});
//]]>
</script>
