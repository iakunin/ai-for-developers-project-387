package dev.iakunin.callcalendar.config;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  /** Prefix owned by the API; the SPA fallback must never answer under it. */
  private static final String API_PREFIX = "api";

  private final CalendarProperties properties;

  /** Needed when the frontend dev server runs separately; irrelevant in the combined image. */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  /**
   * Serves the built frontend when it is present. The directory only exists inside the Docker
   * image, so during development this handler finds nothing and the jar stays API-only.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requested = location.createRelative(resourcePath);
                if (requested.exists() && requested.isReadable()) {
                  return requested;
                }
                if (isApiPath(resourcePath)) {
                  return null;
                }
                Resource index = new ClassPathResource("static/index.html");
                return index.exists() ? index : null;
              }
            });
  }

  private static boolean isApiPath(String resourcePath) {
    return resourcePath.equals(API_PREFIX) || resourcePath.startsWith(API_PREFIX + "/");
  }
}
