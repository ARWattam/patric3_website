<%@ page import="java.util.*"
%><%@ page import="edu.vt.vbi.patric.dao.DBPathways"
%><%@ page import="org.json.simple.JSONObject"
%><%@ page import="org.json.simple.JSONArray"
%><%@ page import="org.json.simple.parser.JSONParser"
%><%@ page import="org.slf4j.Logger"
%><%@ page import="org.slf4j.LoggerFactory"
%><%@ page import="edu.vt.vbi.patric.common.SolrCore"
%><%@ page import="edu.vt.vbi.patric.common.SolrInterface"
%><%@ page import="org.apache.commons.lang.StringUtils"
%><%@ page import="org.apache.solr.client.solrj.SolrQuery"
%><%@ page import="org.apache.solr.client.solrj.SolrServerException"
%><%@ page import="org.apache.solr.client.solrj.response.QueryResponse"
%><%@ page import="org.apache.solr.common.SolrDocument"
%><%@ page import="org.apache.solr.common.SolrDocumentList"
%><%@ page import="org.apache.solr.common.util.SimpleOrderedMap"
%><%@ page import="java.net.MalformedURLException"
%><%@ page import="com.google.gson.Gson"
%><%@ page import="edu.vt.vbi.patric.common.SessionHandler" %><%

    Logger LOGGER = LoggerFactory.getLogger(DBPathways.class);

	Map<String, String> key = new HashMap<String, String>();
	SolrInterface solr = new SolrInterface();
	JSONObject ret = new JSONObject();
	JSONParser a = new JSONParser();
	JSONObject val = (JSONObject) a.parse(request.getParameter("val").toString());

	String need = val.get("need").toString();
	String genomeId = "", taxonId = "", map = "", pk = "";
	
	if (need.equals("all")) {
		if(val.get("genomeId") != null) {
			key.put("genomeId", val.get("genomeId").toString());
			genomeId = val.get("genomeId").toString();
		}
		if(val.get("taxonId") != null) {
			key.put("taxonId", val.get("taxonId").toString());
			taxonId = val.get("taxonId").toString();
		}
		key.put("map", val.get("map").toString());
		map = val.get("map").toString();
        pk = val.get("pk").toString();
        if (pk != null) {
            Gson gson = new Gson();
            Map<String, String> sessKey = gson.fromJson(SessionHandler.getInstance().get(SessionHandler.PREFIX + pk), Map.class);
            if (sessKey != null && sessKey.containsKey("genomeId") && !sessKey.get("genomeId").equals("")) {
                genomeId = sessKey.get("genomeId");
            }
        }

        List<String> annotations = Arrays.asList("PATRIC", "RefSeq");

        // getting coordinates
		try {
            JSONArray listCoordinates = new JSONArray();

            for (String annotation : annotations) {
                Set<String> ecNumbers = new HashSet<String>();

                // step1. genome_count, feature_count
                // pathway/select?q=pathway_id:00053+AND+annotation:PATRIC&fq={!join+from=genome_id+to=genome_id+fromIndex=genome}taxon_lineage_ids:83332+AND+genome_status:(complete+OR+wgs)
                // &rows=0&facet=true&json.facet={stat:{field:{field:ec_number,limit:-1,facet:{genome_count:"unique(genome_id)",gene_count:"unique(feature_id)"}}}}

                SolrQuery query = new SolrQuery("pathway_id:" + map + " AND annotation:" + annotation);
                if (!taxonId.equals("")) {
                    query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + taxonId + " AND genome_status:(complete OR wgs)"));
                }
                if (!genomeId.equals("")) {
                    query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "genome_id:(" + genomeId.replaceAll(",", " OR ") + ") AND genome_status:(complete OR wgs)"));
                }
                query.setRows(0).setFacet(true);

                query.add("json.facet","{stat:{field:{field:ec_number,limit:-1,facet:{genome_count:\"unique(genome_id)\",gene_count:\"unique(feature_id)\"}}}}");

                LOGGER.debug("step 1. {}", query.toString());

                QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query);
                List<SimpleOrderedMap> buckets = (List) ((SimpleOrderedMap) ((SimpleOrderedMap) qr.getResponse().get("facets")).get("stat")).get("buckets");

                Map<String, SimpleOrderedMap> mapStat = new HashMap<String, SimpleOrderedMap>();
                for (SimpleOrderedMap value: buckets) {
                    if (Integer.parseInt(value.get("count").toString()) > 0) {
                        mapStat.put(value.get("val").toString(), value);
                        ecNumbers.add(value.get("val").toString());
                    }
                }

                // step2. coordinates, occurrence
                // pathway_ref/select?q=pathway_id:00010+AND+map_type:enzyme%+AND+ec_number:("1.2.1.3"+OR+"1.1.1.1")&fl=ec_number,ec_description,map_location,occurrence

                if (!ecNumbers.isEmpty()) {
                    query = new SolrQuery("pathway_id:" + map + " AND map_type:enzyme AND ec_number:(" + StringUtils.join(ecNumbers, " OR ") + ")");
                    query.setFields("ec_number,ec_description,map_location,occurrence");
                    query.setRows(1000000);

                    qr = solr.getSolrServer(SolrCore.PATHWAY_REF).query(query);
                    SolrDocumentList sdl = qr.getResults();

                    for (SolrDocument doc: sdl) {
                        String ecNumber = doc.get("ec_number").toString();
                        SimpleOrderedMap stat = mapStat.get(ecNumber);

                        if (!stat.get("gene_count").toString().equals("0")) {

                            List<String> locations = (List<String>) doc.get("map_location");
                            for (String location : locations) {

                                JSONObject coordinate = new JSONObject();
                                coordinate.put("algorithm", annotation);
                                coordinate.put("description", doc.get("ec_description"));
                                coordinate.put("ec_number", ecNumber);
                                coordinate.put("genome_count", stat.get("genome_count"));

                                String[] loc = location.split(",");
                                coordinate.put("x", loc[0]);
                                coordinate.put("y", loc[1]);

                                listCoordinates.add(coordinate);
                            }
                        }
                    }
                }
            }

			ret.put("genome_x_y", listCoordinates);
        }
		catch (MalformedURLException ex) {
		    LOGGER.error(ex.getMessage(), ex);
		}
		catch (SolrServerException e) {
        	LOGGER.error(e.getMessage(), e);
        }

        // get pathways
        try {

            SolrQuery query = new SolrQuery("annotation:(" + StringUtils.join(annotations, " OR ") + ")");
            if (!taxonId.equals("")) {
                query.addFilterQuery(SolrCore.GENOME.getSolrCoreJoin("genome_id", "genome_id", "taxon_lineage_ids:" + taxonId + " AND genome_status:(complete OR wgs)"));
            }
            if (!genomeId.equals("")) {
                query.addFilterQuery("genome_id:(" + genomeId.replaceAll(",", " OR ") + ")");
            }
            query.setFields("pathway_id,pathway_name,annotation").setRows(1000000);

            QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY).query(query);
            SolrDocumentList sdl = qr.getResults();

            JSONArray listEnzymes = new JSONArray();
            Set<String> hash = new HashSet<String>();

            for (SolrDocument doc: sdl) {
                // TODO: need to improve this using solr
                String hashKey = doc.get("pathway_id").toString() + ":" + doc.get("annotation").toString();

                if (hash.contains(hashKey) == false) {

                    hash.add(hashKey);

                    JSONObject enzyme = new JSONObject();
                    enzyme.put("algorithm", doc.get("annotation"));
                    enzyme.put("map_id", doc.get("pathway_id"));
                    enzyme.put("map_name", doc.get("pathway_name"));

                    listEnzymes.add(enzyme);
                 }
            }

			ret.put("genome_pathway_x_y", listEnzymes);
        }
		catch (MalformedURLException ex) {
		    LOGGER.error(ex.getMessage(), ex);
		}
		catch (SolrServerException e) {
        	LOGGER.error(e.getMessage(), e);
        }

        try {
			SolrQuery query = new SolrQuery("pathway_id:" + map + " AND map_type:path");
            query.setFields("ec_number,ec_description,map_location").setRows(10000);

            QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY_REF).query(query);
            SolrDocumentList sdl = qr.getResults();

            JSONArray listCoordinates = new JSONArray();
            for (SolrDocument doc: sdl) {
                List<String> locations = (List<String>) doc.get("map_location");
                for (String location : locations) {

                    JSONObject coordinate = new JSONObject();
                    coordinate.put("source_id", doc.get("ec_number"));

                    String[] loc = location.split(",");
                    coordinate.put("x", loc[0]);
                    coordinate.put("y", loc[1]);
                    coordinate.put("width", loc[2]);
                    coordinate.put("height", loc[3]);

                    listCoordinates.add(coordinate);
                }
            }

			ret.put("map_ids_in_map", listCoordinates);
        }
		catch (MalformedURLException ex) {
		    LOGGER.error(ex.getMessage(), ex);
		}
		catch (SolrServerException e) {
        	LOGGER.error(e.getMessage(), e);
        }

		// all coordinates
		try {
            SolrQuery query = new SolrQuery("pathway_id:" + map + " AND map_type:enzyme");
            query.setFields("ec_number,ec_description,map_location").setRows(10000);

            QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY_REF).query(query);
            SolrDocumentList sdl = qr.getResults();

            JSONArray listCoordinates = new JSONArray();
            for (SolrDocument doc: sdl) {
                List<String> locations = (List<String>) doc.get("map_location");
                for (String location : locations) {

                    JSONObject coordinate = new JSONObject();
                    coordinate.put("ec", doc.get("ec_number"));
                    coordinate.put("description", doc.get("ec_description"));

                    String[] loc = location.split(",");
                    coordinate.put("x", loc[0]);
                    coordinate.put("y", loc[1]);

                    listCoordinates.add(coordinate);
                }
            }

			ret.put("all_coordinates", listCoordinates);
		}
		catch (MalformedURLException ex) {
		    LOGGER.error(ex.getMessage(), ex);
		}
		catch (SolrServerException e) {
        	LOGGER.error(e.getMessage(), e);
        }
		
	}
	else {

        JSONArray coordinates = new JSONArray();

        if (need.equals("ec_number")) {
            try {
                SolrQuery query = new SolrQuery("*:*");
                if (map != null && !map.equals("")) {
                    query.addFilterQuery("pathway_id:(" + map + ")");
                }

                LOGGER.trace("need:{},{}", need, query.toString());

                QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY_REF).query(query);
                SolrDocumentList sdl = qr.getResults();

                for (SolrDocument doc : sdl) {
                    List<String> locations = (List<String>) doc.get("map_location");

                    for (String loc : locations) {
                        JSONObject coordinate = new JSONObject();
                        coordinate.put("ec_number", doc.get("ec_number"));
                        String[] xy = loc.split(",");
                        coordinate.put("x", xy[0]);
                        coordinate.put("y", xy[1]);

                        coordinates.add(coordinate);
                    }
                }
            }
            catch (MalformedURLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            catch (SolrServerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        else {
            // feature
            try {
                SolrQuery query = new SolrQuery("*:*");
                if (map != null && !map.equals("")) {
                    query.addFilterQuery("pathway_id:(" + map + ")");
                }
                if (!val.get("value").toString().equals("")) {
                    query.addFilterQuery(SolrCore.PATHWAY.getSolrCoreJoin("ec_number", "ec_number", "feature_id:(" + val.get("value").toString() + ")"));
                }

                LOGGER.trace("need:{},{}", need, query.toString());

                QueryResponse qr = solr.getSolrServer(SolrCore.PATHWAY_REF).query(query);
                SolrDocumentList sdl = qr.getResults();

                for (SolrDocument doc : sdl) {
                    List<String> locations =  (List<String>) doc.get("map_location");

                    for (String loc : locations) {
                        JSONObject coordinate = new JSONObject();
                        coordinate.put("ec_number", doc.get("ec_number"));
                        String[] xy = loc.split(",");
                        coordinate.put("x", xy[0]);
                        coordinate.put("y", xy[1]);

                        coordinates.add(coordinate);
                    }
                }
            }
            catch (MalformedURLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            catch (SolrServerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        ret.put("coordinates", coordinates);
	}

    response.setContentType("application/json");
    ret.writeJSONString(out);
%>