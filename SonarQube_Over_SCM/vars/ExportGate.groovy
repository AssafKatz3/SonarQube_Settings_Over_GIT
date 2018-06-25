@Grab(group = 'com.squareup.okio', module = 'okio', version = '1.9.0')
@Grab(group = 'com.squareup.okhttp3', module = 'okhttp', version = '3.4.1')

import groovy.json.JsonSlurperClassic
import groovy.json.JsonBuilder
import okhttp3.*


def call(String server, String workspace, String gateName, String outputFileName) {
    _getGateByName(server, workspace, gateName, outputFileName)
}


def _getGateByName(String server, String workspace, String gateName, String outputFileName) {

    // get the required quality gate:
    String gateContent = _getSonarGetResponse(server, "/api/qualitygates/show?name=" + gateName)
    String gateJsonContent = new JsonBuilder(new JsonSlurperClassic().parseText(gateContent)).toPrettyString()

    // update the variable 'outputFileName': if it is empty - use 'gateName'
    outputFileName = (outputFileName == "") ? gateName : outputFileName

    // create the json file and save it under the root folder of the project:
    _createGateFile(workspace, gateJsonContent, outputFileName)

}


def _createGateFile(String workspace, String jsonContent, String fileName) {

    // create quality gate json file:
    writeFile file: workspace + "/" + fileName + ".json", text: jsonContent

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