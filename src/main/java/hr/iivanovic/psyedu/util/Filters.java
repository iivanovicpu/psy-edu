package hr.iivanovic.psyedu.util;

import static hr.iivanovic.psyedu.util.RequestUtil.getQueryLocale;

import spark.Filter;
import spark.Request;
import spark.Response;

public class Filters {

    // If a user manually manipulates paths and forgets to add
    // a trailing slash, redirect the user to the correct path
    public static Filter addTrailingSlashes = (Request request, Response response) -> {
        if (!request.pathInfo().endsWith("/")) {
            response.redirect(request.pathInfo() + "/");
        }
    };

    // Locale change can be initiated from any page
    // The locale is extracted from the request and saved to the user's session
    public static Filter handleLocaleChange = (Request request, Response response) -> {
        if (getQueryLocale(request) != null) {
            request.session().attribute("locale", getQueryLocale(request));
            response.redirect(request.pathInfo());
        } else {
            // ovaj dio postavlja jezik hr i onemogućuje switchanje jezika todo: ukloniti else kad bude trebala višejezičnost
            request.session().attribute("locale", "hr");
        }
    };

    // Enable GZIP for all responses
    public static Filter addGzipHeader = (Request request, Response response) -> {
        response.header("Content-Encoding", "gzip");
    };

}
