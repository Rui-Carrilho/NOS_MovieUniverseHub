package com.movieuniverse.hub.auth;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.account.enabled", havingValue = "true")
public class LocalAccountRunner implements ApplicationRunner {
    private final AccountService accounts;
    private final Environment environment;
    private final ConfigurableApplicationContext context;
    public LocalAccountRunner(AccountService accounts, Environment environment, ConfigurableApplicationContext context) {
        this.accounts = accounts; this.environment = environment; this.context = context;
    }
    @Override public void run(ApplicationArguments args) {
        accounts.setLocalPassword(environment.getProperty("HUB_ACCOUNT_USERNAME"),
                environment.getProperty("HUB_ACCOUNT_PASSWORD"));
        System.out.println("Account password set. No password was logged.");
        SpringApplication.exit(context);
    }
}
