package com.movieuniverse.hub.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(
        name = "app.seed.enabled",
        havingValue = "true"
)
public class SeedImportRunner implements ApplicationRunner {

    private final SeedImportService importer;
    private final Path path;
    private final org.springframework.context.ConfigurableApplicationContext context;
    private final boolean exitAfterImport;

    public SeedImportRunner(
            SeedImportService importer,
            @Value(
                    "${app.seed.path:dados/seed_playlists.json}"
            ) String path,
            org.springframework.context.ConfigurableApplicationContext context,
            @Value("${app.seed.exit:false}") boolean exitAfterImport
    ) {
        this.context = context;
        this.exitAfterImport = exitAfterImport;
        this.importer = importer;
        this.path = Path.of(path);
    }

    @Override
    public void run(ApplicationArguments arguments)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);

        SeedImportService.ImportReport report =
                importer.importBytes(bytes);

        System.out.println("Seed import: " + report);
        if (exitAfterImport) org.springframework.boot.SpringApplication.exit(context);
    }
}