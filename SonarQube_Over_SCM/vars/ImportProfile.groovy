@Grab(group = 'com.squareup.okio', module = 'okio', version = '1.9.0')
@Grab(group = 'com.squareup.okhttp3', module = 'okhttp', version = '3.4.1')

import groovy.json.JsonSlurperClassic
import okhttp3.*


def call(String server, String workspace, String fileName, String token) {
    _importProfile(server, workspace, fileName, token, "", "")
}


def call(String server, String workspace, String fileName, String username, String password) {
    _importProfile(server, workspace, fileName, "", username, password)
}


def _importProfile(String server, String workspace, String fileName, String token, String username, String password) {

    String xmlFile = readFile file: workspace + "/" + fileName

    // create the credential of our http request:
    String credential
    if (token != "") {                                          // authentication using token
        credential = Credentials.basic(token, "")
    } else {                                                    // authentication using username & password
        credential = Credentials.basic(username, password)
    }

    // create the body of our http request:
    RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("backup", fileName, RequestBody.create(MediaType.parse("text/xml"), xmlFile))
            .build()

    // importing the profile
    _getSonarPostResponse(server, "/api/qualityprofiles/restore", credential, requestBody)

}


@NonCPS
def _getSonarPostResponse(String server, String url, String credential, RequestBody requestBody) {

    // creating the http request:
    OkHttpClient client = new OkHttpClient()
    Request request = new Request.Builder()
            .header("Authorization", credential)
            .url(server + url)
            .post(requestBody)
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