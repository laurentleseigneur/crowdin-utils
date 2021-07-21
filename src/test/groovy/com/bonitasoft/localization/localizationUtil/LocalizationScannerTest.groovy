package com.bonitasoft.localization.localizationUtil

import spock.lang.Specification

class LocalizationScannerTest extends Specification {
    def "should export keys"(){
        given:
        def basePath = this.getClass().getResource("/").path
        LocalizationScanner localizationScanner=new  LocalizationScanner(basePath: basePath)

        when:
        localizationScanner.exportTranslationKeys()

        then:
        true
    }

}
