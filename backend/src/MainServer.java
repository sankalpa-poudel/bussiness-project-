import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server started on http://localhost:" + port);

        Path dataDir = Paths.get("backend", "data");
        Files.createDirectories(dataDir);

        server.createContext("/api/packages", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
            String json = "["
                + "{\"id\":1,\"title\":\"Himalayan Escape\",\"location\":\"Nepal\",\"price\":1290,\"days\":6,\"description\":\"A luxury trek with mountain views, warm stays, and guided adventures.\",\"image\":\"/images/butan.webp\"},"
                + "{\"id\":2,\"title\":\"Tokyo City Glow\",\"location\":\"Japan\",\"price\":1680,\"days\":7,\"description\":\"Modern dining, rooftop views, and fashion-forward neighborhoods in one elegant trip.\",\"image\":\"/images/japan.webp\"},"
                + "{\"id\":3,\"title\":\"Portuguese Coast\",\"location\":\"Portugal\",\"price\":1490,\"days\":5,\"description\":\"Mediterranean villas, ocean views, and relaxed coastal culture.\",\"image\":\"/images/portugal.webp\"},"
                + "{\"id\":4,\"title\":\"Royal India\",\"location\":\"India\",\"price\":1850,\"days\":8,\"description\":\"Palace stays, heritage walks, and vibrant food journeys.\",\"image\":\"/images/india.webp\"}"
                + "]";
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });

        server.createContext("/api/bookings", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
            Path file = bookingsFile();
            if (!Files.exists(file)) {
                byte[] out = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
                exchange.close();
                return;
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(s -> !s.isBlank()).collect(Collectors.toList());
            String body = "[" + String.join(",", lines) + "]";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });

        server.createContext("/api/book", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (body.isEmpty()) { exchange.sendResponseHeaders(400, -1); return; }

                String name = extract(body, "name");
                String email = extract(body, "email");
                String packageId = extract(body, "packageId");

                Path file = bookingsFile();
                long id = Files.exists(file) ? Files.lines(file).count() + 1 : 1;
                String entry = "{\"id\":" + id + ",\"timestamp\":\"" + Instant.now().toString() + "\",\"name\":\"" + escapeJson(name) + "\",\"email\":\"" + escapeJson(email) + "\",\"packageId\":\"" + escapeJson(packageId) + "\"}";
                Files.write(file, (entry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                String message = "Thanks " + (name.isBlank() ? "traveler" : name) + "! Your request for package " + (packageId.isBlank() ? "selection" : packageId) + " is received. We will contact you at " + (email.isBlank() ? "your email" : email) + " shortly.";
                String outBody = "{\"status\":\"ok\",\"message\":\"" + escapeJson(message) + "\",\"bookingId\":" + id + "}";
                byte[] out = outBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
                exchange.close();
            } catch (Exception e) {
                e.printStackTrace();
                String msg = "{\"status\":\"error\",\"message\":\"internal\"}";
                exchange.sendResponseHeaders(500, msg.length());
                exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
                exchange.close();
            }
        });

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            Path file = Paths.get("frontend", path.startsWith("/") ? path.substring(1) : path);
            if (!Files.exists(file) || Files.isDirectory(file)) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            String ct = guessContentType(file.toString());
            exchange.getResponseHeaders().set("Content-Type", ct);
            byte[] data = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
    }

    static Path bookingsFile() {
        return Paths.get("backend", "data", "bookings.ndjson");
    }

    static String extract(String body, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    static String guessContentType(String f) {
        f = f.toLowerCase();
        if (f.endsWith(".html")) return "text/html; charset=utf-8";
        if (f.endsWith(".css")) return "text/css; charset=utf-8";
        if (f.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
