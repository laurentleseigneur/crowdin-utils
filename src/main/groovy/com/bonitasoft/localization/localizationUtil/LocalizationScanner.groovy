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

    def importTranslationKeys() {
        def all = [:]
        getSupportedLanguages().each { language ->
            println("get ${language} translations")
            TreeMap props = new TreeMap()
            def localeFilePath = "${basePath}/localization/locale_${language}.json"
            def localFile = new File(localeFilePath)
            def localeProps = readJsonPropertyFile(localFile)
            localeProps.each { k, v ->
                props.put(k, v)
            }
            def translationFile = new File("${basePath}/localization/localization_${language}.json")
            def translationProperties = readJsonPropertyFile(translationFile)
            translationProperties.each { k, v ->
                props.putIfAbsent(k, v)
            }

            all.put(language, props)
        }
        List<Object> localizationFiles = getLocalizationFiles()
        localizationFiles.each {
            writeToJsonFile(it, all)
        }
    }

    def exportTranslationKeys() {
        def allProps = [:]
        def localeProps = [:]
        TreeMap keyUsage = new TreeMap()
        TreeMap allKeys = new TreeMap()
        List<Object> localizationFile = getLocalizationFiles()
        localizationFile.each { file ->
            def props = readJsonPropertyFile(file as File)
            def filteredLanguages = props.findAll { language, values ->
                isValidLanguage(language)
            }
            filteredLanguages.each { language, properties ->
                if (!allProps["${language}"]) {
                    allProps["${language}"] = new Properties()
                }
                if (!localeProps["${language}"]) {
                    localeProps["${language}"] = new Properties()
                }
                properties.each { entry, value ->
                    if (isValidProperty(entry)) {
                        (allProps["${language}"] as Properties).putIfAbsent(entry, value)
                        allKeys.putIfAbsent(entry, entry)
                        def usageLabel = "${(file as File).getCanonicalPath()} [${language}]"
                        if (!keyUsage.containsKey(entry)) {
                            keyUsage.put(entry, [usageLabel])
                        } else {
                            keyUsage.get(entry).add(usageLabel)
                        }
                    } else {
                        (localeProps["${language}"] as Properties).putIfAbsent(entry, value)
                    }
                }
            }
        }
        localeProps.each { language, properties ->
            writeToJsonFile("${basePath}/localization/locale_${language}.json", properties)
        }

        allProps.each { language, properties ->
            def localShort = getLocaleShort(language)
            writeToPropertyFile("${basePath}/locales/${localShort}/localization.properties", properties)
        }

        writeToPropertyFile("${basePath}/templates/en/localization.properties", allKeys)
        writeToJsonFile("${basePath}/localization/localization.json", allKeys)
        writeToJsonFile("${basePath}/localization/keyUsage.json", keyUsage)
    }

    private void writeToJsonFile(def filePath, def keys) {
        createParentDirectory(filePath as String)
        def keysJson = new File(filePath as String)
        keysJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(keys))
        println("writing file ${keysJson.absolutePath}")
    }

    private void writeToPropertyFile(def filePath, def properties) {
        def fileName = filePath as String
        createParentDirectory(fileName)

        def propertyFile = new File(fileName)
        def fileWriter = new OutputStreamWriter(new FileOutputStream(fileName, false), 'UTF-8')
        Properties props = properties as Properties
        props.store(fileWriter, null)
        fileWriter.close()
        println("writing file ${propertyFile.getAbsolutePath()}")

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

    def readJsonPropertyFile(File file) {
        println("parsing file ${file.getAbsolutePath()}")
        def props = jsonSlurper.parse(file)
        props
    }

    boolean isValidLanguage(String language) {
        boolean isValid = Locale.getISOLanguages().contains(language.substring(0, 2))
        def supportedLanguages = getSupportedLanguages()
        isValid = isValid && supportedLanguages.contains(language)
        isValid
    }

    private ArrayList<String> getSupportedLanguages() {
        ['fr-FR', 'es-ES']
    }

    boolean isValidProperty(String property) {
        !property.startsWith('$')
    }


    void createParentDirectory(String fileName) {
        new File(fileName).parentFile.mkdirs()
    }

    def getLocaleShort(def locale) {
        def localesShort = ['fr-FR': 'fr', 'es-ES': 'es']
        localesShort[locale]
    }
}
