package ge.comcom.anubis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableCaching
@SpringBootApplication
public class AnubisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnubisApplication.class, args);
    }

}
