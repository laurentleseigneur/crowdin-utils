package com.bonitasoft.localization.localizationUtil

import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import lombok.Builder
import lombok.Data
import lombok.NonNull
import lombok.RequiredArgsConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@Data
@RequiredArgsConstructor
@Builder
class LocalizationScanner {

    @NonNull
    String basePath

    JsonSlurper jsonSlurper = new JsonSlurper()

    Logger logger = LoggerFactory.getLogger(LocalizationScanner.class)

    def importTranslationKeys() {
        def all = [:]
        getSupportedLanguages().each { language ->
            logger.info("get {} translations", language)
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
        def localizationFiles = getLocalizationFiles()
        localizationFiles.each {
            updateLocalizationFile(it, all)
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
            writeToJsonFile(new File("${basePath}/localization/locale_${language}.json"), properties)
        }
        writeToJsonFile(new File("${basePath}/localization/localization.json"), allKeys)
        writeToJsonFile(new File("${basePath}/localization/keyUsage.json"), keyUsage)
    }

    private void writeToJsonFile(File destinationFile, def keys) {
        createParentDirectory(destinationFile as String)
        def keysJson = new File(destinationFile as String)
        def fileProps = readJsonPropertyFile(destinationFile)
        fileProps.each { k, v ->
            logger.debug("found key.value {} - {} ", k, v)
        }
        keysJson.text = JsonOutput.prettyPrint(JsonOutput.toJson(keys))
        logger.info("writing file {}", keysJson.absolutePath)
    }

    def getLocalizationFiles() {
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

    static boolean isLocalizationFile(File file) {
        file.getName() == 'localization.json' && isUIDPage(file) && !isBonitaProvidedPage(file)
    }

    static boolean isBonitaProvidedPage(File file) {
        def pageName = file.parentFile.parentFile.parentFile.getName()
        pageName.startsWith('admin')
    }

    static boolean isUIDPage(file) {
        def folderName = file.parentFile.parentFile.parentFile.parentFile.getName()
        folderName.equals('web_page')
    }

    def readJsonPropertyFile(File file) {
        if (!file.exists() || !file.canRead()) {
            logger.error("file {} does not exist", file.getAbsolutePath())
            return null
        }
        logger.info("parsing file {}", file.getAbsolutePath())
        def props = jsonSlurper.parse(file)
        props
    }

    static boolean isValidLanguage(String language) {
        boolean isValid = Locale.getISOLanguages().contains(language.substring(0, 2))
        def supportedLanguages = getSupportedLanguages()
        isValid = isValid && supportedLanguages.contains(language)
        isValid
    }

    private static ArrayList<String> getSupportedLanguages() {
        ['fr-FR', 'es-ES']
    }

    static boolean isValidProperty(String property) {
        !property.startsWith('$')
    }


    static void createParentDirectory(String fileName) {
        new File(fileName).parentFile.mkdirs()
    }

    def updateLocalizationFile(def destinationFile, def keys) {
        logger.info("updating localization file {}", destinationFile.absolutePath)
        def fileProps = readJsonPropertyFile(destinationFile)
        TreeMap existingKeys = []
        TreeMap newKeys = []
        fileProps.each { language, values ->
            logger.debug("found language [{}] ", language)
            def languageKeys = values
            languageKeys.each { key, value ->
                logger.debug("adding translation key {} ", key)
                existingKeys.putIfAbsent(key, null)
            }
        }
        keys.each { language, values ->
            def languageKeys = [:]
            values.each { key, value ->
                if (existingKeys.containsKey(key)) {
                    languageKeys.put(key, value)
                }
            }
            newKeys.put(language, languageKeys)

        }
        logger.debug("found keys in file [{}]: {} ", destinationFile.absolutePath, existingKeys)
        logger.debug("update keys in file [{}]: {} ", destinationFile.absolutePath, newKeys)
        destinationFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(newKeys))
        logger.info("writing file {}", destinationFile.absolutePath)
    }
}


