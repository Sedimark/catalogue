package eu.sedimark.catalogue.handlers;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.fuseki.servlets.HttpAction;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tailwind-based query UI for visual comparison with the Bootstrap UI.
 * Uses the Tailwind Play CDN for quick prototyping.
 */
public class QueryUITailwindProcessor implements ActionProcessor {
    private final Dataset dataset;
    private static final String DEFAULT_QUERY = loadDefaultQueryFromResource();

    public QueryUITailwindProcessor(Dataset dataset) {
        this.dataset = dataset;
    }

    private static String loadDefaultQueryFromResource() {
        String resourcePath = "sparql/mkt-ui-query.rq";
        try (InputStream is = QueryUITailwindProcessor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return "SELECT * WHERE { ?s ?p ?o } LIMIT 25";
            }
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "SELECT * WHERE { ?s ?p ?o } LIMIT 25";
        }
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
            // GET: show default query from file and no results
            queryStr = DEFAULT_QUERY;
            showResults = false;
        }

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang='en'><head><meta charset='utf-8'>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
    // Use Tailwind Play CDN for now (quick prototyping)
    html.append("<script src='https://cdn.tailwindcss.com'></script>");
        // Small custom tweaks
        html.append("<style>");
        html.append("  /* ensure textarea uses monospace for queries */");
        html.append("  .query-textarea { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, 'Roboto Mono', monospace; }");
        html.append("</style>");
        html.append("<title>SEDIMARK Catalogue - Tailwind UI</title></head><body class='bg-slate-50 text-slate-800'>");

        html.append("<div class='max-w-6xl mx-auto p-8'>");

        // Header: logo left + compact title
        html.append("<div class='flex items-center gap-4 mb-6'>");
        html.append("<img src='https://sedimark.eu/wp-content/uploads/2022/11/cropped-sedimark_logo_512x512.png' alt='logo' class='w-14 h-14 rounded'/>");
        html.append("<div>");
        html.append("<h1 class='text-xl font-semibold text-slate-900'>SEDIMARK Catalogue Explorer</h1>");
        html.append("<div class='text-sm text-slate-600'>Decentralised, intelligent data marketplace for European data spaces.</div>");
        html.append("</div></div>");

        // Accordion-style query box implemented with details/summary (open by default)
        html.append("<details open class='bg-white rounded-lg shadow-sm mb-6'>");
        html.append("<summary class='px-4 py-3 cursor-pointer select-none flex items-center justify-between'>");
        html.append("<span class='font-medium'>SPARQL Query</span>");
        html.append("</summary>");
        html.append("<div class='px-4 pb-4'>");
        html.append("<form method='POST' class='flex flex-col gap-3'>");
        html.append("<textarea id='query' name='query' rows='8' class='query-textarea block w-full rounded-md border border-slate-200 p-3 bg-slate-50 text-sm' placeholder='Enter your SPARQL query here...'>");
        html.append(escapeHtml(queryStr));
        html.append("</textarea>");

        // Buttons: unified size and spacing
        html.append("<div class='flex gap-3'>");
        html.append("<button type='submit' name='action' value='run' class='inline-flex items-center justify-center px-4 py-2 rounded-md bg-teal-600 text-white text-sm font-medium hover:bg-teal-700'>Run Query</button>");
        html.append("<button type='submit' name='action' value='clear' class='inline-flex items-center justify-center px-4 py-2 rounded-md bg-slate-200 text-slate-800 text-sm font-medium hover:bg-slate-300'>Clear Results</button>");
        html.append("</div>");

        html.append("</form>");
        html.append("</div>");
        html.append("</details>");

        // Results area (cards, no accordion)
        if (showResults) {
            try {
                Query query = QueryFactory.create(queryStr);
                dataset.begin(org.apache.jena.query.ReadWrite.READ);
                try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
                    if (query.isSelectType()) {
                        ResultSet results = qexec.execSelect();
                        html.append(renderCards(results));
                    } else if (query.isAskType()) {
                        boolean res = qexec.execAsk();
                        html.append("<div class='bg-white rounded-lg shadow-sm p-4'>");
                        html.append("<div class='font-medium mb-2'>ASK Result</div>");
                        html.append("<pre class='text-sm bg-slate-50 p-3 rounded'>").append(res).append("</pre>");
                        html.append("</div>");
                    } else {
                        html.append("<div class='bg-white rounded-lg shadow-sm p-4'>Unsupported query type.</div>");
                    }
                } finally {
                    dataset.end();
                }
            } catch (Exception e) {
                html.append("<div class='bg-white rounded-lg shadow-sm p-4 text-red-700'>Error: ").append(escapeHtml(e.getMessage())).append("</div>");
            }
        }

        // Footer / closing
        html.append("</div>"); // container
        html.append("</body></html>");

        try {
            action.getResponseOutputStream().write(html.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            action.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String renderCards(ResultSet results) {
        StringBuilder html = new StringBuilder();
        int count = 0;
        while (results.hasNext()) {
            var sol = results.nextSolution();
            String offering = sol.contains("offering") ? sol.get("offering").toString() : "";
            String asset = sol.contains("asset") ? sol.get("asset").toString() : "";
            String title = sol.contains("title") ? sol.get("title").toString() : ("Offering " + (count + 1));
            String publisher = sol.contains("publisher") ? sol.get("publisher").toString() : "";
            String alternateName = sol.contains("alternateName") ? sol.get("alternateName").toString() : "";

            html.append("<div class='bg-white rounded-lg shadow mb-4 overflow-hidden'>");
            // turquoise header
            html.append("<div class='px-4 py-2 bg-gradient-to-r from-teal-300 to-teal-100 text-slate-800 font-medium'>").append(escapeHtml(title)).append("</div>");
            html.append("<div class='p-4'>");

            // Present fields as an unordered list (each field on its own line)
            html.append("<ul class='space-y-2 list-none m-0 p-0'>");
            html.append("<li><span class='font-semibold'>Offering URI:</span> ");
            if (offering.isEmpty()) html.append("<span class='text-sm text-slate-600'>N/A</span>");
            else html.append("<a class='text-teal-600 underline' href='").append(escapeHtml(offering)).append("' target='_blank'>").append(escapeHtml(offering)).append("</a>");
            html.append("</li>");

            html.append("<li><span class='font-semibold'>Asset URI:</span> ");
            if (asset.isEmpty()) html.append("<span class='text-sm text-slate-600'>N/A</span>");
            else html.append("<a class='text-teal-600 underline' href='").append(escapeHtml(asset)).append("' target='_blank'>").append(escapeHtml(asset)).append("</a>");
            html.append("</li>");

            html.append("<li><span class='font-semibold'>Publisher:</span> <span class='text-sm'>").append(escapeHtml(publisher.isEmpty() ? "N/A" : publisher)).append("</span></li>");
            html.append("<li><span class='font-semibold'>Alternate Name:</span> <span class='text-sm'>").append(escapeHtml(alternateName.isEmpty() ? "N/A" : alternateName)).append("</span></li>");
            html.append("</ul>");

            html.append("</div></div>");
            count++;
        }
        if (count == 0) {
            html.append("<div class='bg-yellow-50 rounded-lg border border-yellow-200 p-4'>No offerings found.</div>");
        }
        return html.toString();
    }

    // Minimal HTML escaper for safety
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}