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
        String queryStr = null;
        if ("POST".equalsIgnoreCase(method)) {
            queryStr = action.getRequest().getParameter("query");
        }
        StringBuilder html = new StringBuilder();
    html.append("<html><head><title>SEDIMARK SPARQL Query UI</title>");
    html.append("<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css' crossorigin='anonymous'>");
    html.append("<style>");
    html.append("body { margin:0; font-family:Segoe UI,Arial,sans-serif; background: linear-gradient(135deg,#e9f0fa 0%,#f7fbff 100%); min-height:100vh; }");
    html.append(".container { max-width:700px; margin:40px auto; background:#fff; border-radius:16px; box-shadow:0 4px 24px rgba(0,0,0,0.08); padding:32px; }");
    html.append(".logo { display:block; margin:0 auto 16px auto; width:96px; }");
    html.append("h1 { text-align:center; color:#1a2a4a; margin-bottom:8px; }");
    html.append(".subtitle { text-align:center; color:#3a4a6a; margin-bottom:24px; font-size:1.1em; }");
    html.append("form { margin-bottom:24px; } textarea { width:100%; font-size:1em; border-radius:8px; border:1px solid #bcd; padding:12px; resize:vertical; }");
    html.append("input[type='submit'] { background:#1a2a4a; color:#fff; border:none; border-radius:8px; padding:10px 24px; font-size:1em; cursor:pointer; margin-top:12px; }");
    html.append("input[type='submit']:hover { background:#2d3e6b; }");
    html.append(".results { background:#f4f8fc; border-radius:8px; padding:16px; margin-top:16px; font-size:0.97em; color:#222; overflow-x:auto; }");
    html.append(".error { color:#c00; font-weight:bold; }");
    html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<img class='logo' src='https://sedimark.eu/wp-content/uploads/2022/11/cropped-sedimark_logo_512x512.png' alt='SEDIMARK Logo'/>");
        html.append("<h1>SEDIMARK Catalogue Explorer</h1>");
        html.append("<div class='subtitle'>Empowering Secure Data Exchange<br>Decentralised, intelligent data marketplace for European data spaces.</div>");
        html.append("<form method='POST'>");
        html.append("<label for='query'><b>SPARQL Query:</b></label><br>");
        html.append("<textarea id='query' name='query' rows='10' placeholder='Enter your SPARQL query here...'>");
        html.append(queryStr != null ? queryStr : "");
        html.append("</textarea><br>");
        html.append("<input type='submit' value='Run Query'/>");
        html.append("</form>");
        if (queryStr != null && !queryStr.trim().isEmpty()) {
            try {
                Query query = QueryFactory.create(queryStr);
                try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
                    if (query.isSelectType()) {
                        ResultSet results = qexec.execSelect();
                        html.append("<div class='results'><b>Results:</b><br>");
                        html.append(renderBootstrapTable(results));
                        html.append("</div>");
                    } else if (query.isAskType()) {
                        boolean result = qexec.execAsk();
                        html.append("<div class='results'><b>Result:</b><br><pre>").append(result).append("</pre></div>");
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
                }
            } catch (Exception e) {
                html.append("<div class='error'>Error: ").append(e.getMessage()).append("</div>");
            }
        }
        html.append("</div></body></html>");
        try {
            action.getResponseOutputStream().write(html.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            action.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Helper to render SELECT results as a Bootstrap table
    private String renderBootstrapTable(ResultSet results) {
        StringBuilder table = new StringBuilder();
        table.append("<table class='table table-bordered table-sm table-hover'><thead><tr>");
        for (String var : results.getResultVars()) {
            table.append("<th scope='col'>").append(var).append("</th>");
        }
        table.append("</tr></thead><tbody>");
        while (results.hasNext()) {
            table.append("<tr>");
            org.apache.jena.query.QuerySolution sol = results.nextSolution();
            for (String var : results.getResultVars()) {
                String value = sol.contains(var) ? sol.get(var).toString() : "";
                table.append("<td>").append(escapeHtml(value)).append("</td>");
            }
            table.append("</tr>");
        }
        table.append("</tbody></table>");
        return table.toString();
    }

    // Simple HTML escape helper
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
