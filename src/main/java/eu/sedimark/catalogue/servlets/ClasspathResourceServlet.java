package eu.sedimark.catalogue.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
            } else {
                System.err.println("ClasspathResourceServlet: serving resource: /" + resourcePath);
            }
            // Set content type based on extension
            String contentType = guessContentType(resourcePath);
            if (contentType != null) resp.setContentType(contentType);
            // Stream
            try (OutputStream os = resp.getOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            }
        }
    }

    private String guessContentType(String path) {
        String p = path.toLowerCase();
        for (String ext : CONTENT_TYPES.keySet()) {
            if (p.endsWith(ext)) return CONTENT_TYPES.get(ext);
        }
        return null;
    }
}
