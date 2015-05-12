<%@ page import="edu.vt.vbi.patric.common.ExcelHelper"%><%@ page 
	import="edu.vt.vbi.patric.common.SolrInterface"%><%@ page
	import="edu.vt.vbi.patric.common.SolrCore"%><%@ page 
	import="edu.vt.vbi.patric.common.DownloadHelper"%><%@ page
	import="edu.vt.vbi.patric.dao.*"%><%@ page
	import="edu.vt.vbi.patric.beans.*"%><%@ page
	import="org.json.simple.JSONArray"%><%@ page 
	import="org.json.simple.JSONObject"%><%@ page 
	import="org.json.simple.parser.JSONParser"%><%@ page 
	import="java.util.*"%><%@ page
	import="org.apache.solr.common.*"%><%@ page
	import="org.apache.solr.client.solrj.*"%><%@ page
	import="org.apache.solr.client.solrj.response.*"%><%@ page
	import="org.apache.solr.client.solrj.impl.LBHttpSolrServer"%><%@ page
	import="java.net.MalformedURLException"%><%@ page
	import="java.io.OutputStream"%><%@ page
	import="org.slf4j.Logger"%><%@ page
    import="org.slf4j.LoggerFactory"%><%@ page
	import="org.apache.commons.lang.StringUtils"%><%

	final Logger LOGGER = LoggerFactory.getLogger("GRID_DOWNLOAD_HANDLER.JSP");

	String _filename = "";

	List<String> _tbl_header = new ArrayList<String>();
	List<String> _tbl_field = new ArrayList<String>();
	JSONArray _tbl_source = null;

	// getting common params
	String _fileformat = request.getParameter("fileformat");
	String _tablesource = request.getParameter("tablesource");
	ResultType key = new ResultType();

	String sort_field;
	String sort_dir;
	HashMap<String, String> sort = null;

	if (_tablesource == null || _fileformat == null) {
		_fileformat = null;
	}

	ExcelHelper excel = null;

	if (_tablesource.equalsIgnoreCase("GlobalSearch")) {

		SolrInterface solr = new SolrInterface();
		String keyword = request.getParameter("download_keyword");
		String cat = request.getParameter("cat");

		key.put("keyword", keyword);

		if (cat.equals("2")) {
			solr.setCurrentInstance(SolrCore.TAXONOMY);
			_tbl_header.addAll(Arrays.asList("Taxon ID", "Taxon Name", "# of Genomes"));
			_tbl_field.addAll(Arrays.asList("taxon_id", "taxon_name", "genomes"));
		}
		else if (cat.equals("3")) {
			solr.setCurrentInstance(SolrCore.TRANSCRIPTOMICS_EXPERIMENT);
			_tbl_header.addAll(Arrays.asList("Experiment ID", "Title", "Comparisons", "Genes", "PubMed", "Accession", "Organism", "Strain",
					"Gene Modification", "Experimental Condition", "Time Series", "Release Date", "Author", "PI", "Institution"));
			_tbl_field.addAll(Arrays.asList("eid", "title", "samples", "genes", "pmid", "accession", "organism", "strain", "mutant",
					"condition", "timeseries", "release_date", "author", "pi", "institution"));
		}
		else if (cat.equals("1")) {
			solr.setCurrentInstance(SolrCore.GENOME);
			_tbl_header.addAll(DownloadHelper.getHeaderForGenomes());
			_tbl_field.addAll(DownloadHelper.getFieldsForGenomes());
		}
		else if (cat.equals("0")) {
			solr.setCurrentInstance(SolrCore.FEATURE);
			_tbl_header.addAll(DownloadHelper.getHeaderForFeatures());
			_tbl_field.addAll(DownloadHelper.getFieldsForFeatures());
		}

		JSONObject object = solr.getData(key, sort, null, 0, -1, false, false, false);
		JSONObject obj = (JSONObject) object.get("response");
		_tbl_source = (JSONArray) obj.get("docs");
		_tbl_header.addAll(Arrays.asList(new String[] {}));
		_tbl_field.addAll(Arrays.asList(new String[] {}));

		_filename = "GlobalSearch";
	}
	else if (_tablesource.equalsIgnoreCase("Proteomics_Experiment")) {

		SolrInterface solr = new SolrInterface();
		String keyword = request.getParameter("download_keyword");
		String experiment_id = request.getParameter("experiment_id");

		sort_field = request.getParameter("sort");
		sort_dir = request.getParameter("dir");

		if (sort_field != null && sort_dir != null) {
			sort = new HashMap<String, String>();
			sort.put("field", sort_field);
			sort.put("direction", sort_dir);
		}

		if (keyword != null) {
			key.put("keyword", keyword.trim());
		}

		if (request.getParameter("aT").equals("0")) {

			solr.setCurrentInstance(SolrCore.PROTEOMICS_EXPERIMENT);
			JSONObject object = solr.getData(key, sort, null, 0, -1, false, false, false);
			JSONObject obj = (JSONObject) object.get("response");
			_tbl_source = (JSONArray) obj.get("docs");
			_tbl_header.addAll(Arrays.asList("Sample Name", "Taxon Name", "Proteins", "Project Name", "Experiment Label", "Experiment Title",
					"Experiment Type", "Source", "Contact Name", "Institution"));

			_tbl_field.addAll(Arrays.asList("sample_name", "taxon_name", "proteins", "project_name", "experiment_label", "experiment_title",
					"experiment_type", "source", "contact_name", "institution"));
		}
		else if (request.getParameter("aT").equals("1")) {

			String solrId = "";
			solr.setCurrentInstance(SolrCore.PROTEOMICS_PROTEIN);

			if (experiment_id != null && !experiment_id.equals("")) {
				keyword += " AND experiment_id:(" + experiment_id + ")";
			}

			key.put("keyword", keyword.trim());

			JSONObject object_t = solr.getData(key, null, null, 0, -1, false, false, false);
			JSONObject obj_t = (JSONObject) object_t.get("response");
			_tbl_source = (JSONArray) obj_t.get("docs");

			_tbl_header.addAll(Arrays.asList("Experiment Title", "Experiment Label", "Source", "Genome Name", "Accession", "Locus Tag",
					"RefSeq Locus Tag", "Gene Symbol", "Description"));
			_tbl_field.addAll(Arrays.asList("experiment_title", "experiment_label", "source", "genome_name", "accession", "locus_tag",
					"refseq_locus_tag", "refseq_gene", "product"));
		}

		_filename = "Proteomics";
	}
	else if (_tablesource.equalsIgnoreCase("GeneExpression")) {

// TranscriptomicsGeneExp.java //

		String idList = request.getParameter("fids");
		JSONParser parser = new JSONParser();
		JSONObject fids = (JSONObject) parser.parse(idList);

		String paramFeatureId =  fids.get("feature_id").toString();
        String paramSampleId = fids.get("pid").toString();

        JSONObject jsonResult = new JSONObject();
		SolrInterface solr = new SolrInterface();
        try {
            LBHttpSolrServer lbHttpSolrServer = solr.getSolrServer(SolrCore.TRANSCRIPTOMICS_GENE);
            SolrQuery query = new SolrQuery();

            query.setQuery("feature_id:" + paramFeatureId);

            if (paramSampleId != null && !paramSampleId.equals("")) {
                String[] pids = paramSampleId.split(",");

                query.addFilterQuery("pid:(" + StringUtils.join(pids, " OR ") + ")");
            }

            LOGGER.debug("grid_download_handler.jsp::GeneExpression, {}", query.toString());

            QueryResponse qr = lbHttpSolrServer.query(query, SolrRequest.METHOD.POST);
            long numFound = qr.getResults().getNumFound();

            query.setRows((int) numFound);

            qr = lbHttpSolrServer.query(query, SolrRequest.METHOD.POST);

            // features
            JSONArray features = new JSONArray();
            SolrDocumentList sdl = qr.getResults();
            for (SolrDocument doc : sdl) {
                JSONObject feature = new JSONObject();
					feature.put("exp_accession", doc.get("accession"));
					// feature.put("exp_channels", doc.get(""));
					feature.put("exp_condition", doc.get("condition"));
					feature.put("exp_id", doc.get("eid"));
					feature.put("exp_locustag", doc.get("refseq_locus_tag"));
					feature.put("exp_mutant", doc.get("mutant"));
					feature.put("exp_name", doc.get("expname"));
					feature.put("exp_organism", doc.get("organism"));
					feature.put("exp_pavg", doc.get("avg_intensity"));
					feature.put("exp_platform", doc.get("")); // ??
					feature.put("exp_pratio", doc.get("log_ratio"));
					feature.put("exp_samples", doc.get("")); // ??
					feature.put("exp_strain", doc.get("")); // ??
					feature.put("exp_timepoint", doc.get("timepoint"));
					feature.put("exp_zscore", doc.get("z_score"));
					// feature.put("figfam_id", doc.get("")); // ??
					feature.put("locus_tag", doc.get("alt_locus_tag"));
					feature.put("feature_id", doc.get("feature_id"));
					feature.put("pid", doc.get("pid"));
					feature.put("pmid", doc.get("pmid"));

                features.add(feature);
            }
            jsonResult.put("features", features);
        }
        catch (MalformedURLException me) {
            LOGGER.error(me.getMessage(), me);
        }
        catch (SolrServerException e) {
            LOGGER.error(e.getMessage(), e);
        }
// TranscriptomicsGeneExp.java //

        _tbl_source = (JSONArray) jsonResult.get("features");

		_tbl_header.addAll(Arrays.asList("Platform", "Samples", "Locus Tag", "Title", "PubMed", "Accession", "Strain", "Gene Modification",
				"Experimental Condition", "Time Point", "Avg Intensity", "Log Ratio", "Z-score"));
		_tbl_field.addAll(Arrays.asList("exp_platform", "exp_samples", "exp_locustag", "exp_name", "pmid", "exp_accession", "exp_strain",
				"exp_mutant", "exp_condition", "exp_timepoint", "exp_pavg", "exp_pratio", "exp_zscore"));

		_filename = "GeneExpression";
	}
	else if (_tablesource.equalsIgnoreCase("Correlation")) {

		String cutoffValue = request.getParameter("cutoffValue");
		String cutoffDir = request.getParameter("cutoffDir");
		String featureId = request.getParameter("cId");

// TranscriptomicsGeneExp.java //
		JSONObject jsonResult = new JSONObject();
		SolrInterface solr = new SolrInterface();

		GenomeFeature feature = solr.getFeature(featureId);
		Map<String,Map<String, Object>> correlationMap = new HashMap<String,Map<String, Object>>();
		long numFound = 0;

		try {
			SolrQuery query = new SolrQuery("genome_id:" + feature.getGenomeId());
			query.setFilterQueries("{!correlation fieldId=refseq_locus_tag fieldCondition=pid fieldValue=log_ratio srcId=" + feature.getRefseqLocusTag() + " filterCutOff=" + cutoffValue + " filterDir=" + cutoffDir.substring(0,3) + " cost=101}");
			query.setRows(0);

			QueryResponse qr = solr.getSolrServer(SolrCore.TRANSCRIPTOMICS_GENE).query(query, SolrRequest.METHOD.POST);

			SolrDocumentList sdl = (SolrDocumentList) qr.getResponse().get("correlation");
			numFound = sdl.getNumFound();

			for (SolrDocument doc: sdl) {
				Map<String, Object> corr = new HashMap<String, Object>();
				corr.put("id", doc.get("id"));
				corr.put("correlation", doc.get("correlation"));
				corr.put("conditions", doc.get("conditions"));
				corr.put("p_value", doc.get("p_value"));

				correlationMap.put(doc.get("id").toString(), corr);
			}

		} catch (MalformedURLException me) {
	    	LOGGER.error(me.getMessage(), me);
		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}

		jsonResult.put("total", numFound);
		JSONArray results = new JSONArray();

		try {
			SolrQuery query = new SolrQuery("refseq_locus_tag:(" + StringUtils.join(correlationMap.keySet(), " OR ") + ")");
			query.setFilterQueries("annotation:PATRIC");
			query.setFields("genome_id,genome_name,accession,feature_id,start,end,strand,feature_type,annotation,alt_locus_tag,refseq_locus_tag,seed_id,na_length,aa_length,protein_id,gene,product");
			query.setRows((int) numFound);

			QueryResponse qr = solr.getSolrServer(SolrCore.FEATURE).query(query, SolrRequest.METHOD.POST);
			List<GenomeFeature> features = qr.getBeans(GenomeFeature.class);

			for (GenomeFeature f: features) {
				JSONObject obj = new JSONObject();
				obj.put("genome_id", f.getGenomeId());
				obj.put("genome_name", f.getGenomeName());
				obj.put("accession", f.getAccession());
				obj.put("feature_id", f.getId());
				obj.put("alt_locus_tag", f.getAltLocusTag());
				obj.put("refseq_locus_tag", f.getRefseqLocusTag());
				obj.put("seed_id", f.getSeedId());
				obj.put("gene", f.getGene());
				obj.put("annotation", f.getAnnotation());
				obj.put("feature_type", f.getFeatureType());
				obj.put("start", f.getStart());
				obj.put("end", f.getEnd());
				obj.put("na_length", f.getNaSequenceLength());
				obj.put("strand", f.getStrand());
				obj.put("protein_id", f.getProteinId());
				obj.put("aa_length", f.getProteinLength());
				obj.put("product", f.getProduct());

				Map<String, Object> corr = correlationMap.get(f.getRefseqLocusTag());
				obj.put("correlation", corr.get("correlation"));
				obj.put("count", corr.get("conditions"));

				results.add(obj);
			}
			jsonResult.put("results", results);

		} catch (MalformedURLException me) {
	    	LOGGER.error(me.getMessage(), me);
		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(), e);
		}
// TranscriptomicsGeneExp.java //

        _tbl_source = (JSONArray) jsonResult.get("results");

		_tbl_header.addAll(Arrays.asList("Genome Name", "Accession", "PATRIC ID", "Alt Locus Tag", "RefSeq Locus Tag", "Gene Symbol", "Annotation",
				"Feature Type", "Start", "End", "Length(NT)", "Strand", "Protein ID", "Length(AA)", "Product Description", "Correlations",
				"Comparisons"));
		_tbl_field.addAll(Arrays.asList("genome_name", "accession", "seed_id", "alt_locus_tag", "refseq_locus_tag", "gene", "annotation", "feature_type",
				"start", "end", "na_length", "strand", "protein_id", "aa_length", "product", "correlation", "count"));

		_filename = "Correlated Genes";
	}

	excel = new ExcelHelper("xssf", _tbl_header, _tbl_field, _tbl_source);
	excel.buildSpreadsheet();

	if (_fileformat.equalsIgnoreCase("xlsx")) {
		response.setContentType("application/octetstream");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + _filename + "." + _fileformat + "\"");

		OutputStream outs = response.getOutputStream();
		excel.writeSpreadsheettoBrowser(outs);
	}
	else if (_fileformat.equalsIgnoreCase("txt")) {

		response.setContentType("application/octetstream");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + _filename + "." + _fileformat + "\"");

		out.println(excel.writeToTextFile());
	}
%>
