package com.backend;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

@WebServlet("/db-test")
public class DBtest extends HttpServlet{
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Find the .env file in the server's compiled classes directory
            String envPath = getServletContext().getRealPath("/WEB-INF/classes");
            Dotenv dotenv = Dotenv.configure().directory(envPath).load();

            String url = dotenv.get("DB_URL");
            String user = dotenv.get("DB_USER");
            String password = dotenv.get("DB_PASSWORD");

            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                resp.getWriter().println("Success! Connected to PostgreSQL using .env credentials.");
            }
        } catch (Exception e) {
            resp.getWriter().println("Connection failed: " + e.getMessage());
        }
    }
}
