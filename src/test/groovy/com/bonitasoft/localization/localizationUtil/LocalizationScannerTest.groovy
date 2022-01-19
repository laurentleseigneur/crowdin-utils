package com.bonitasoft.localization.localizationUtil

import groovy.json.JsonSlurper
import spock.lang.Specification

class LocalizationScannerTest extends Specification {

    JsonSlurper jsonSlurper = new JsonSlurper()


    def "should export keys"() {
        given:
        def basePath = this.getClass().getResource("/").path
        LocalizationScanner localizationScanner = new LocalizationScanner(basePath: basePath)

        when:
        localizationScanner.exportTranslationKeys()

        then:
        File localization = new File("${basePath}/localization/localization.json")
        def jsonContent = jsonSlurper.parse(localization)
        def expectedContent = jsonSlurper.parseText('''
        {         
            "key1": "key1",
            "key2": "key2"
        }''')
        jsonContent == expectedContent
    }

    def "should import keys"() {
        given:
        def basePath = this.getClass().getResource("/").path
        LocalizationScanner localizationScanner = new LocalizationScanner(basePath: basePath)

        when:
        localizationScanner.importTranslationKeys()

        then:
        File page1Localization = new File("${basePath}/web_page/page1/assets/json/localization.json")
        def page1JsonContent = jsonSlurper.parse(page1Localization)
        def page1ExpectedContent = jsonSlurper.parseText('''
        {
             "es-ES": {
                 "key1": "llave1"
             },
             "fr-FR": {
                 "key1": "cl\u00e91"
             }
        }''')
        page1JsonContent == page1ExpectedContent
        File page2Localization = new File("${basePath}/web_page/page2/assets/json/localization.json")
        def page2JsonContent = jsonSlurper.parse(page2Localization)
        def page2ExpectedContent = jsonSlurper.parseText('''
        {
             "es-ES": {
                 "key2": "llave2"
             },
             "fr-FR": {
                 "key2": "cl\u00e92"
             }
        }''')
        page2JsonContent == page2ExpectedContent

    }
}
