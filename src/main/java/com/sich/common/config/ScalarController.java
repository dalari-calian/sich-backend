package com.sich.common.config;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ScalarController {

    private static final String SCALAR_HTML = """
            <!doctype html>
            <html lang="pt-br">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Sich Backend API</title>
            </head>
            <body>
              <script id="api-reference" data-url="/v3/api-docs"></script>
              <script>
                var configuration = {
                  theme: 'purple',
                  layout: 'modern',
                  hideDownloadButton: false,
                  authentication: { preferredSecurityScheme: 'bearerAuth' }
                };
                document.getElementById('api-reference').dataset.configuration =
                  JSON.stringify(configuration);
              </script>
              <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
            </body>
            </html>
            """;

    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String scalar() {
        return SCALAR_HTML;
    }
}
