package com.backend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Redirects /docs to the Swagger UI webjar, pre-pointed at this app's
 * openapi.json. The UI version must match the swagger-ui version in pom.xml.
 */
@WebServlet("/docs")
public class DocsRedirect extends HttpServlet {
    private static final String SWAGGER_UI_VERSION = "5.18.2";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String base = req.getContextPath();
        resp.sendRedirect(base + "/webjars/swagger-ui/" + SWAGGER_UI_VERSION
                + "/index.html?url=" + base + "/api/v1/openapi.json");
    }
}
