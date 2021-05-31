package com.bonitasoft.localization.localizationUtil

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@SpringBootApplication
class LocalizationUtilApplication {

    static void main(String[] args) {
        SpringApplication.run(LocalizationUtilApplication, args)
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return (args) -> {

            println "Arguments:"
            args.each { println it }
            LocalizationScanner localizationScanner = new LocalizationScanner()
            localizationScanner.setBasePath(args[0])
            def action = args[1]
            switch (action) {
                case 'scan':
                    localizationScanner.extractCrowdinKeys()
                    break
                case 'deploy':
                    localizationScanner.deploy()
                    break
            }


        }

    }
}


