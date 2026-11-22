package notaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Punto de entrada del backend (vista de desarrollo).
 * SpringApplication.run() arranca el servidor web y crea el Núcleo
 * (inicializarNucleo). Las ventanas ahora están en el frontend (React).
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    /**
     * CORS: permite que la página de React (en Vercel, otra dirección)
     * pueda llamar a este backend. FRONTEND_URL es la dirección de Vercel.
     */
    @Bean
    public WebMvcConfigurer permitirFrontend() {
        String frontend = System.getenv("FRONTEND_URL");
        String origen = (frontend == null || frontend.isBlank()) ? "*" : frontend;
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registro) {
                registro.addMapping("/api/**")
                        .allowedOriginPatterns(origen)
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
