package ge.comcom.anubis.util;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Регистрирует JAI ImageIO провайдеры (например, TIFF) при старте приложения.
 * JPEG поддерживается стандартной библиотекой Java (com.sun.imageio).
 */
@Slf4j
@Component
public class JaiImageioRegistrar {

    @PostConstruct
    public void registerJaiProviders() {
        try {
            // Стандартный вызов для перерегистрации всех плагинов
            ImageIO.scanForPlugins();

            // Регистрируем только TIFF, он отсутствует по умолчанию
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(new TIFFImageReaderSpi());

            log.info("✅ JAI ImageIO provider registered: TIFF");
            log.info("✅ JPEG provider is available by default via com.sun.imageio");
        } catch (Throwable e) {
            log.warn("⚠️ Failed to register JAI ImageIO providers: {}", e.getMessage());
        }
    }
}
