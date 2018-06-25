@Grab(group = 'com.squareup.okio', module = 'okio', version = '1.9.0')
@Grab(group = 'com.squareup.okhttp3', module = 'okhttp', version = '3.4.1')

import groovy.json.JsonSlurperClassic
import okhttp3.*


def call(String server, String workspace, String fileName, String token) {
    _importGate(server, workspace, fileName, token, "", "")
}


def call(String server, String workspace, String fileName, String username, String password) {
    _importGate(server, workspace, fileName, "", username, password)
}


def _importGate(String server, String workspace, String fileName, String token, String username, String password) {

    String jsonFile = readFile file: workspace + "/" + fileName

    // get the name of the required quality gate:
    Object GateContent = new JsonSlurperClassic().parseText(jsonFile)
    String gateName = GateContent.name

    // create the credential of our http request:
    String credential
    if (token != "") {                                          // authentication using token
        credential = Credentials.basic(token, "")
    } else {                                                    // authentication using username & password
        credential = Credentials.basic(username, password)
    }

    // check if another gate with the same name exist:
    //      if exist - delete its conditions
    //      else     - create new gate
    Object oldGateContent = null
    Integer gateId = null
    try {
        String res = _getSonarGetResponse(server, "/api/qualitygates/show?name=" + gateName)
        oldGateContent = new JsonSlurperClassic().parseText(res)
        gateId = oldGateContent.id
    }
    catch (IOException e) {
        if (e.getMessage() != "sonar: error 404") { // 404-gate not found. In that case - continue
            throw e
        }
    }

    if (gateId != null) {
        _deleteExistGateWithSameName(server, credential, oldGateContent)
    } else {
        gateId = _createNewGate(server, credential, gateName)
    }

    // update the gate according to the input file:
    _addConditionsToGate(server, credential, GateContent, gateId)

}


def _deleteExistGateWithSameName(String server, String credential, Object oldGateContent) {

    // iterate over the conditions of the exist gate and delete them:
    def conditions = oldGateContent.conditions
    conditions.each {

        // create the body of our http request:
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", String.valueOf(it.id))
                .build()

        // delete current condition:
        _getSonarPostResponse(server, "/api/qualitygates/delete_condition", credential, requestBody)

    }

}


def _createNewGate(String server, String credential, String gateName) {

    // create the body of our http request:
    RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", gateName)
            .build()

    // create a new quality gate with the required name:
    String res = _getSonarPostResponse(server, "/api/qualitygates/create", credential, requestBody)

    // return the Id of the new gate:
    return (new JsonSlurperClassic().parseText(res)).id as Integer

}


def _addConditionsToGate(String server, String credential, Object GateContent, Integer gateID) {

    // iterate over the conditions from the json file and add them to the gate:
    def conditions = GateContent.conditions
    conditions.each {

        String error = (it.error == null) ? "" : it.error
        String gateId = String.valueOf(gateID)
        String metric = (it.metric == null) ? "" : it.metric
        String op = (it.op == null) ? "" : it.op
        String period = (it.period == null) ? "" : it.period
        String warning = (it.warning == null) ? "" : it.warning

        // create the body of our http request:
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("error", error)
                .addFormDataPart("gateId", gateId)
                .addFormDataPart("metric", metric)
                .addFormDataPart("op", op)
                .addFormDataPart((period == "") ? "" : "period", period)
                .addFormDataPart("warning", warning)
                .build()

        // add current condition:
        _getSonarPostResponse(server, "/api/qualitygates/create_condition", credential, requestBody)

    }

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
    if (response.code == 200 || response.code == 204) {         // 200-OK   204-No Content
        return responseBodyAsString
    } else if (response.code == 404) {                          // 404-Not Found
        throw new IOException("sonar: error 404")
    } else if (responseBodyAsString) {
        def errorJson = new JsonSlurperClassic().parseText(responseBodyAsString)
        throw new IOException("sonar: " + errorJson.errors[0].msg)
    } else if (response.code == 401) {                          // 401-Unauthorized
        throw new IOException("sonar: unauthorized")
    } else {
        throw new IOException("sonar: unknown error (error code = " + response.code + ")")
    }


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