package eu.sedimark.catalogue.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Simple servlet that serves resources from the classpath under the /static/* URL.
 * Maps /static/foo/bar.css -> classpath resource /static/foo/bar.css
 */
public class ClasspathResourceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Map<String,String> CONTENT_TYPES = new HashMap<>();
    static {
        CONTENT_TYPES.put(".css", "text/css; charset=UTF-8");
        CONTENT_TYPES.put(".js", "application/javascript; charset=UTF-8");
        CONTENT_TYPES.put(".html", "text/html; charset=UTF-8");
        CONTENT_TYPES.put(".png", "image/png");
        CONTENT_TYPES.put(".jpg", "image/jpeg");
        CONTENT_TYPES.put(".jpeg", "image/jpeg");
        CONTENT_TYPES.put(".svg", "image/svg+xml");
        CONTENT_TYPES.put(".woff2", "font/woff2");
        CONTENT_TYPES.put(".woff", "font/woff");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI();
        // Expecting path starting with /static/
        int idx = path.indexOf("/static/");
        if (idx == -1) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String resourcePath = path.substring(idx); // includes /static/...
        // Remove leading slash for classloader resource lookup
        if (resourcePath.startsWith("/")) resourcePath = resourcePath.substring(1);

        // Use class resource lookup (leading slash) which works reliably from within JAR
        try (InputStream is = ClasspathResourceServlet.class.getResourceAsStream("/" + resourcePath)) {
            if (is == null) {
                System.err.println("ClasspathResourceServlet: resource not found: /" + resourcePath);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            // Read resource fully to determine length and compute a light ETag (CRC32).
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
            byte[] data = baos.toByteArray();

            // Compute a small ETag so clients can do conditional GETs
            CRC32 crc = new CRC32();
            crc.update(data);
            String etag = '"' + Long.toHexString(crc.getValue()) + '-' + data.length + '"';

            // Conditional GET - If-None-Match
            String ifNone = req.getHeader("If-None-Match");
            if (ifNone != null && ifNone.equals(etag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                resp.setHeader("ETag", etag);
                return;
            }

            // Set headers
            resp.setHeader("ETag", etag);
            resp.setHeader("Cache-Control", "public, max-age=3600");

            // Set content type based on extension
            String contentType = guessContentType(resourcePath);
            if (contentType != null) resp.setContentType(contentType);

            // Support HEAD: set headers but don't write body
            boolean isHead = "HEAD".equalsIgnoreCase(req.getMethod());
            resp.setContentLength(data.length);
            if (!isHead) {
                try (OutputStream os = resp.getOutputStream()) {
                    os.write(data);
                }
            }
            System.err.println("ClasspathResourceServlet: served resource: /" + resourcePath + " (" + data.length + " bytes)");
        }
    }

    private String guessContentType(String path) {
        String p = path.toLowerCase();
        // Check common extensions in deterministic order (longer extensions first)
        String[] exts = {".woff2", ".woff", ".jpeg", ".jpg", ".png", ".svg", ".css", ".js", ".html", ".json"};
        for (String ext : exts) {
            if (p.endsWith(ext)) return CONTENT_TYPES.get(ext);
        }
        return null;
    }
}
