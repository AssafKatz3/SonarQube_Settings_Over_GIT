@Grab(group = 'com.squareup.okio', module = 'okio', version = '1.9.0')
@Grab(group = 'com.squareup.okhttp3', module = 'okhttp', version = '3.4.1')

import groovy.json.JsonSlurperClassic
import groovy.xml.XmlUtil
import okhttp3.*


def call(String server, String workspace, String projectKey) {
    _getProfilesByProjectKey(server, workspace, projectKey)
}


def call(String server, String workspace, String language, String profileName) {
    _getProfilesByLangAndName(server, workspace, language, profileName)
}


def _getProfilesByLangAndName(String server, String workspace, String profileLanguage, String profileName) {

    // get list of all quality profiles:
    String res = _getSonarGetResponse(server, "/api/qualityprofiles/search" + "")

    // parse text to json:
    def qualityProfilesJson = new JsonSlurperClassic().parseText(res)

    // get the required quality profile:
    def profile = qualityProfilesJson["profiles"].find {
        it["language"] == profileLanguage && it["name"] == profileName
    }

    // check if the quality profile exists:
    if (!profile) {
        throw new IOException("sonar: quality profile not found")
    }

    // create the content of the xml output:
    res = _getSonarGetResponse(server, "/api/qualityprofiles/backup?profileKey=" + (profile["key"] as String))

    // create the xml file and save it under the root folder of the project:
    _createProfileFile(workspace, res, profile["name"] as String, profileLanguage)

}


def _getProfilesByProjectKey(String server, String workspace, String projectKey) {
    String response

    // get the quality profiles of the last scan:
    try {
        response = _getSonarGetResponse(server, "/api/navigation/component?componentKey=" + projectKey)
    } catch (Exception) {
        response = _getSonarGetResponse(server, "/api/navigation/component?component=" + projectKey)
    }

    // export the quality profiles we used in the last scan by language and profile name:
    def projectDescriptionJson = new JsonSlurperClassic().parseText(response)
    def languages = []
    for (profile in projectDescriptionJson["qualityProfiles"]) {
        _getProfilesByLangAndName(server, workspace, profile.language as String, profile.name as String)
        languages.add(profile.language)
    }

    // get the current assigned quality profiles of the project:
    try {
        response = _getSonarGetResponse(server, "/api/qualityprofiles/search?projectKey=" + projectKey)
    } catch (Exception) {
        response = _getSonarGetResponse(server, "/api/qualityprofiles/search?project=" + projectKey)
    }

    // export only the profiles of the languages detected:
    def projectQualityProfilesJson = new JsonSlurperClassic().parseText(response)
    for (profile in projectQualityProfilesJson["profiles"]) {
        if (languages.contains(profile.language)) {
            _getProfilesByLangAndName(server, workspace, profile.language as String, profile.name as String)
        }
    }

}


def _createProfileFile(String workspace, String xmlContent, String profileName, String language) {

    // create quality profile xml file:
    def xmlOut = XmlUtil.serialize(xmlContent)
    writeFile file: workspace + "/" + profileName + "_" + language + ".xml", text: xmlOut.toString()

}


@NonCPS
def _getSonarGetResponse(String server, String url) {

    // creating the http request:
    OkHttpClient client = new OkHttpClient()
    Request request = new Request.Builder()
            .url(server + url)
            .get()
            .build()
    Response response = client.newCall(request).execute()
    string responseBodyAsString = response.body.string()

    // error checking:
    if (response.code == 200 || response.code == 204) {
        return responseBodyAsString
    } else if (responseBodyAsString) {
        def errorJson = new JsonSlurperClassic().parseText(responseBodyAsString)
        throw new IOException("sonar: " + errorJson.errors[0].msg)
    } else {
        if (response.code == 401) {
            throw new IOException("sonar: unauthorized")
        } else {
            throw new IOException("sonar: unknown error (error code = " + response.code + ")")
        }
    }

}


return this