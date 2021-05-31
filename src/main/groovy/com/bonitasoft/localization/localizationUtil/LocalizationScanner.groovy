package com.bonitasoft.localization.localizationUtil

import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import lombok.Builder
import lombok.Data
import lombok.NonNull
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component

@Component
@Data
@RequiredArgsConstructor
@Builder
public class LocalizationScanner {

    @NonNull
    String basePath

    JsonSlurper jsonSlurper = new JsonSlurper()

    def extractCrowdinKeys() {
//        def allProps = [:]
        TreeMap keyUsage = new TreeMap()
        TreeMap allKeys = new TreeMap()
        List<Object> localizationFile = getLocalizationFiles()
        localizationFile.each { file ->
            def props = readProps(file as File)
            def filteredLanguages = props.findAll { language, values ->
                isValidLanguage(language)
            }
            filteredLanguages.each { language, properties ->
//                if (!allProps["${language}"]) {
//                    allProps["${language}"] = new Properties()
//                }
                properties.each { entry, value ->
                    if (isValidProperty(entry)) {
//                        (allProps["${language}"] as Properties).putIfAbsent(entry, value)
                        allKeys.putIfAbsent(entry, entry)
                        def usageLabel = "${(file as File).getCanonicalPath()} [${language}]"
                        if (!keyUsage.containsKey(entry)) {
                            keyUsage.put(entry, [usageLabel])
                        } else {
                            keyUsage.get(entry).add(usageLabel)
                        }
                    }
                }
            }
//            allProps.each { language, properties ->
//                allKeys.each { key, value ->
//                    if (!properties.containsKey(key)) {
//                        println("add missing key [${key}] for language [${language}]")
//                        properties.put(key, key)
//                    }
//                }
//            }
//            allProps.each { language, properties ->
//                def jsonFile = new File("${basePath}/localization/localization_${language}.json")
//                jsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(properties))
//                def filteredProperties = (properties as Properties).findAll {
//                    it.getKey().toString().startsWith('$')
//                }
//                def localeJson = new File("${basePath}/localization/local_${language}.json")
//                localeJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(filteredProperties))
//            }
        }
        writePropertiesToFile("localization.json", allKeys)
        writePropertiesToFile("keyUsage.json", keyUsage)
    }

    private void writePropertiesToFile(String fileName, TreeMap keys) {
        def filePath = "${basePath}/localization/${fileName}"
        def keysJson = new File(filePath)
        keysJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(keys))
        println("write file ${filePath}")
    }


    private List<File> getLocalizationFiles() {
        def list = []

        def dir = new File(basePath)
        dir.eachFileRecurse(FileType.FILES) { file ->
            list << file
        }
        def filtered = list.findAll { file ->
            isLocalizationFile(file as File)
        }
        filtered
    }

    boolean isLocalizationFile(File file) {
        file.getName() == 'localization.json' && isUIDPage(file) && !isBonitaProvidedPage(file)
    }

    boolean isBonitaProvidedPage(File file) {
        def pageName = file.parentFile.parentFile.parentFile.getName()
        pageName.startsWith('admin')
    }

    boolean isUIDPage(file) {
        def folderName = file.parentFile.parentFile.parentFile.parentFile.getName()
        folderName.equals('web_page')
    }

    def readProps(File file) {
        println("parsing file ${file.getAbsolutePath()}")
        def props = jsonSlurper.parse(file)
        props
    }

    boolean isValidLanguage(String language) {
        boolean isValid = Locale.getISOLanguages().contains(language.substring(0, 2))
        def supportedLanguages = ['fr-FR', 'es-ES']
        isValid = isValid && supportedLanguages.contains(language)
        isValid
    }

    def deploy() {
        def list = []

        def dir = new File(basePath)
        dir.eachFileRecurse(FileType.FILES) { file ->
            list << file
        }
        def filtered = list.findAll { file ->
            isLocalizationFile(file as File)
        }
    }

    boolean isValidProperty(String property) {
        !property.startsWith('$')
    }
}
