package eu.sedimark.catalogue.handlers;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class QueryUIProcessor implements ActionProcessor {
    private final Dataset dataset;

    public QueryUIProcessor(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void process(HttpAction action) {
        action.getResponse().setContentType("text/html;charset=UTF-8");
        String method = action.getRequest().getMethod();
        String queryStr;
        boolean showResults = false;

        String actionParam = null;
        if ("POST".equalsIgnoreCase(method)) {
            queryStr = action.getRequest().getParameter("query");
            actionParam = action.getRequest().getParameter("action");
            showResults = queryStr != null && !queryStr.trim().isEmpty() && !"clear".equals(actionParam);
        } else {
            queryStr = DEFAULT_QUERY;
            showResults = false;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>SEDIMARK SPARQL Query UI</title>");
        html.append(
                "<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css' rel='stylesheet' crossorigin='anonymous'>");
        html.append(
                "<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js' crossorigin='anonymous'></script>");
        html.append(
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css' crossorigin='anonymous'>");

        html.append("<style>");
        html.append(
                "body { margin:0; font-family:Segoe UI,Arial,sans-serif; background: linear-gradient(135deg,#e9f0fa 0%,#f7fbff 100%); min-height:100vh; }");
        html.append(
                ".container { max-width:1500px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.06); padding:32px; }");
        html.append(".header { display:flex; align-items:center; margin-bottom:16px; }");
        html.append(".logo { width:56px; height:56px; margin-right:16px; }");
        html.append("h1 { font-size:1.6em; margin:0; color:#1a2a4a; }");
        html.append(".subtitle { color:#3a4a6a; margin-bottom:16px; font-size:1em; }");
        html.append(
                "form { margin-bottom:18px; } textarea { width:100%; font-size:0.97em; border-radius:6px; border:1px solid #bcd; padding:8px; resize:vertical; min-height:80px; max-height:220px; }");
        html.append(
                "input[type='submit'] { background:#1a2a4a; color:#fff; border:none; border-radius:6px; padding:8px 18px; font-size:0.97em; cursor:pointer; margin-top:8px; }");
        html.append("input[type='submit']:hover { background:#2d3e6b; }");
        html.append(
                ".results { background:#f4f8fc; border-radius:6px; padding:12px; margin-top:12px; font-size:0.95em; color:#222; overflow-x:auto; }");
        html.append(".error { color:#c00; font-weight:bold; }");
        // ...existing code...
        html.append(
                ".card-header { background: linear-gradient(90deg,#3ec6e0 0%,#6ad6e8 100%); color:#1a2a4a; font-weight:500; }");
        // ...existing code...
        html.append("</style></head><body>");
        html.append("<div class='container' style='max-width:1000px;'>");
        html.append("<div class='header'>");
        html.append(
                "<img class='logo' src='https://sedimark.eu/wp-content/uploads/2022/11/cropped-sedimark_logo_512x512.png' alt='SEDIMARK Logo'/>");
        html.append("<div>");
        html.append("<h1>SEDIMARK Catalogue Explorer</h1>");
        html.append(
                "<div class='subtitle'>Empowering Secure Data Exchange<br>Decentralised, intelligent data marketplace for European data spaces.</div>");
        html.append("</div>");
        html.append("</div>");
        html.append("<div class='accordion mb-4' id='queryAccordion'>");
        html.append("  <div class='accordion-item'>");
        html.append("    <h2 class='accordion-header' id='queryHeading'>");
        html.append(
                "      <button class='accordion-button' type='button' data-bs-toggle='collapse' data-bs-target='#queryCollapse' aria-expanded='true' aria-controls='queryCollapse'>");
        html.append("        SPARQL Query");
        html.append("      </button>");
        html.append("    </h2>");
        html.append(
                "    <div id='queryCollapse' class='accordion-collapse collapse show' aria-labelledby='queryHeading' data-bs-parent='#queryAccordion'>");
        html.append("      <div class='accordion-body'>");
        html.append("        <form method='POST' style='display:flex; flex-direction:column;'>");
        html.append(
                "          <textarea id='query' name='query' rows='6' placeholder='Enter your SPARQL query here...' class='form-control mb-3'>");
        html.append(queryStr);
        html.append("</textarea>");
        html.append("          <div style='display:flex; gap:10px;'>");
        html.append(
                "            <button type='submit' name='action' value='run' class='btn btn-primary'>Run Query</button>");
        html.append(
                "            <button type='submit' name='action' value='clear' class='btn btn-secondary'>Clear Results</button>");
        html.append("          </div>");
        html.append("        </form>");
        html.append("      </div>");
        html.append("    </div>");
        html.append("  </div>");
        html.append("</div>");
        html.append("</form>");

        if (showResults) {
            try {
                Query query = QueryFactory.create(queryStr);
                dataset.begin(org.apache.jena.query.ReadWrite.READ); // <-- Start transaction
                try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
                    if (query.isSelectType()) {
                        ResultSet results = qexec.execSelect();
                        html.append("<div class='results'><b>Results:</b><br>");
                        html.append(renderBootstrapTable(results));
                        html.append("</div>");
                    } else if (query.isAskType()) {
                        boolean result = qexec.execAsk();
                        html.append("<div class='results'><b>Result:</b><br><pre>").append(result)
                                .append("</pre></div>");
                    } else if (query.isConstructType()) {
                        html.append("<div class='results'><b>Result:</b><br><pre>");
                        qexec.execConstruct().write(action.getResponseOutputStream(), "TTL");
                        html.append("</pre></div>");
                    } else if (query.isDescribeType()) {
                        html.append("<div class='results'><b>Result:</b><br><pre>");
                        qexec.execDescribe().write(action.getResponseOutputStream(), "TTL");
                        html.append("</pre></div>");
                    } else {
                        html.append("<div class='error'>Unsupported query type.</div>");
                    }
                } finally {
                    dataset.end(); // <-- End transaction
                }
            } catch (Exception e) {
                html.append("<div class='error'>Error: ").append(e.getMessage()).append("</div>");
            }
        }

        html.append(
                "<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js' crossorigin='anonymous'></script>");

        html.append("</div></body></html>");
        try {
            action.getResponseOutputStream().write(html.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            action.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Helper to render SELECT results as a Bootstrap table
    private String renderBootstrapTable(ResultSet results) {
        StringBuilder html = new StringBuilder();
        int count = 0;
        while (results.hasNext()) {
            org.apache.jena.query.QuerySolution sol = results.nextSolution();
            String offering = sol.contains("offering") ? sol.get("offering").toString() : "";
            String asset = sol.contains("asset") ? sol.get("asset").toString() : "";
            String title = sol.contains("title") ? sol.get("title").toString() : "";
            String publisher = sol.contains("publisher") ? sol.get("publisher").toString() : "";
            String alternateName = sol.contains("alternateName") ? sol.get("alternateName").toString() : "";

            html.append("<div class='card mb-3'>");
            html.append("<div class='card-header'><b>")
                    .append(escapeHtml(title.isEmpty() ? "Offering " + (count + 1) : title))
                    .append("</b></div>");
            html.append("<div class='card-body'>");
            html.append("<dl class='row mb-0'>");
            html.append("<dt class='col-sm-3'>Offering URI</dt><dd class='col-sm-9'><a href='")
                    .append(escapeHtml(offering)).append("' target='_blank'>").append(escapeHtml(offering))
                    .append("</a></dd>");
            html.append("<dt class='col-sm-3'>Asset URI</dt><dd class='col-sm-9'><a href='")
                    .append(escapeHtml(asset)).append("' target='_blank'>").append(escapeHtml(asset))
                    .append("</a></dd>");
            html.append("<dt class='col-sm-3'>Publisher</dt><dd class='col-sm-9'>")
                    .append(escapeHtml(publisher)).append("</dd>");
            html.append("<dt class='col-sm-3'>Alternate Name</dt><dd class='col-sm-9'>")
                    .append(escapeHtml(alternateName)).append("</dd>");
            html.append("</dl>");
            html.append("</div></div>");
            count++;
        }
        if (count == 0) {
            html.append("<div class='alert alert-warning mt-3'>No offerings found.</div>");
        }
        return html.toString();
    }

    // Simple HTML escape helper
    private String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String DEFAULT_QUERY = loadDefaultQuery();

    private static String loadDefaultQuery() {
        try (java.io.InputStream in = QueryUIProcessor.class.getResourceAsStream("/sparql/mkt-ui-query.rq")) {
            if (in != null) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // fallback to empty
        }
        return "";
    }
}
