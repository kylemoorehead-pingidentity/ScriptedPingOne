@Grapes([
        @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1'),
        @Grab(group='org.slf4j', module='slf4j-api', version='1.6.1'),
        @Grab(group='ch.qos.logback', module='logback-classic', version='0.9.28'),
        @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.13')
])
import groovy.json.JsonOutput
import groovy.transform.Field
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.*

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration

def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def uid = uid as Uid
def log = log as Log


@Field final CLIENT_ID = "__REPLACE_ME__"
@Field final CLIENT_SECRET ="__REPLACE_ME__"
@Field final ENV_ID = "__REPLACE_ME__"
@Field final API_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/"
@Field final TOKEN_URL = "https://auth.pingone.com/${ENV_ID}/as/token"
@Field final USER_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/users"
@Field final GROUP_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/groups"
@Field final POPULATION_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/populations"
@Field final ROLE_URL = "https://api.pingone.com/v1/roles"

def dataMap = [:]

// Initialize sub-maps for the name and population attributes
def nameMap = [:]
def populationMap = [:]
def accountMap = [:]
def identityProviderMap = [:]
def lifecycleMap = [:]
def access_token = null


println "Entering update script for " + objectClass + " with attributes: " + updateAttributes + " for Account "+ uid.uidValue
CloseableHttpClient httpClient = HttpClients.createDefault();
println "Updating Account for " + objectClass + ": " + uid.uidValue + " Account"
def userId = null
def userName = null
def firstName = null
def lastName = null
def email = null
def currentRoles = []

println "Update Payload: " + updateAttributes
if (updateAttributes.hasAttribute("userName")) {
    userName = updateAttributes.findString("userName")
}
dataMap["username"] = userName
if (updateAttributes.hasAttribute("givenName")) {
    firstName = updateAttributes.findString("givenName")
    nameMap["given"] = firstName
}
if (updateAttributes.hasAttribute("sn")) {
    lastName = updateAttributes.findString("sn")
    nameMap["family"] = lastName
}



dataMap["name"] = nameMap
if (updateAttributes.hasAttribute("email")) {
    email = updateAttributes.findString("email")
    dataMap["email"] = email
}


if (updateAttributes.hasAttribute("populationid")) {
    populationid = updateAttributes.findString("populationid")
    populationMap["id"] = populationid
    dataMap["population"] = populationMap
}


if (updateAttributes.hasAttribute("accountStatus")) {
    accountStatus = updateAttributes.findString("accountStatus")
    accountMap["status"] = accountStatus
}

if (updateAttributes.hasAttribute("canAuthenticate")) {
    canAuthenticate = updateAttributes.findString("canAuthenticate")
    accountMap["canAuthenticate"] = canAuthenticate
    dataMap["account"] = accountMap
}




if (updateAttributes.hasAttribute("enabled")) {
    enabled = updateAttributes.findString("enabled")
    if(enabled == "true") {
        dataMap["enabled"] = true
    } else {
        dataMap["enabled"] = false
    }
}


if (updateAttributes.hasAttribute("identityProviderType")) {
    identityProviderType = updateAttributes.findString("identityProviderType")
    identityProviderMap["type"] = identityProviderType
    dataMap["identityProvider"] = identityProviderMap
}
// Serialize to JSON
def jsonData = JsonOutput.toJson(dataMap)
//println "JSON Payload: ${jsonData}"
userId = uid.uidValue
access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
def userPatchUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}"
//println "User Patch URL: ${userPatchUrl}"
println "\n\n"
def userPatch = new HttpPatch(userPatchUrl)
userPatch.addHeader("Content-Type", "application/json")
userPatch.addHeader("Authorization", "Bearer ${access_token}")
userPatch.entity = new StringEntity(jsonData)
def userPatchResponse = httpClient.execute(userPatch)
def userResponseData = new groovy.json.JsonSlurper().parseText(userPatchResponse.entity.content.text)
//println "User Response Data: ${userResponseData}"
// println "\n\n"

// Handle Group Memberships separately
if (updateAttributes.hasAttribute("groupMemberships")) {
    def grpMemUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/memberOfGroups"
    println "Processing Group Memberships"
    //access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)

    def groupMemberships = []

    groupMemberships = updateAttributes.findStringList("groupMemberships")
    println "Received Group Memberships: ${groupMemberships}"
    if(groupMemberships.size == 0){
        groupMemberships = null
        println "Will revoke all group memberships"
        // revoke all group memberships
        def currentGroups = null
        currentGroups = getCurrentGroupsforUser(userId,access_token)
        if(currentGroups){
            currentGroups.each { currentGroup ->
                revokeGroupFromUser(userId,currentGroup,access_token)
            }
        }
    }
    if(groupMemberships){
        //compare current with new
        //revoke what is not in new

        //Get current group memberships
        def currentGroups = getCurrentGroupsforUser(userId,access_token)
        println "Current Group Memberships: ${currentGroups}"
        currentGroups.each { currentGroup ->
            if(!groupMemberships.contains(currentGroup)){
                println "Revoking Group Membership: ${currentGroup}"
                //revoke group
                revokeGroupFromUser(userId,currentGroup,access_token)
            } else {
                println "Group Membership: ${currentGroup} is already present"
            }
        }
        //Add new group memberships
        groupMemberships.each { groupID ->
            if(!currentGroups.contains(groupID)){
                println "Adding Group Membership: ${groupID}"
                //add group
                addGroupToUser(userId,groupID,access_token)
            }
        }
    }
}
return uid

def addGroupToUser(String userId, String groupId, String access_token){
    def grpMemUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/memberOfGroups"
    def grpMemPost = null
    def grpMemData = [:]
    CloseableHttpClient httpClient = HttpClients.createDefault();
    grpMemPost = new HttpPost(grpMemUrl)
    //println "Add Group ToGroup Membership URL: ${grpMemUrl}"
    grpMemPost.addHeader("Content-Type", "application/json")
    grpMemPost.addHeader("Authorization", "Bearer ${access_token}")
    grpMemData["id"] = groupId
    def grpMemJsonData = JsonOutput.toJson(grpMemData)
    grpMemPost.entity = new StringEntity(grpMemJsonData)
    println "Group Membership Payload: ${grpMemJsonData}"
    def grpMemResponse = httpClient.execute(grpMemPost)
    def grpMemResponseData = new groovy.json.JsonSlurper().parseText(grpMemResponse.entity.content.text)
    println "Group Membership Response: ${grpMemResponseData}"

}
def revokeGroupFromUser(String userId, String groupId, String access_token){
    def grpMemUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/memberOfGroups/${groupId}"
    def grpMemDelete = null
    def grpMemData = [:]
    CloseableHttpClient httpClient = HttpClients.createDefault();
    grpMemDelete = new HttpDelete(grpMemUrl)
    //println "Group Membership URL: ${grpMemUrl}"
    grpMemDelete.addHeader("Content-Type", "application/json")
    grpMemDelete.addHeader("Authorization", "Bearer ${access_token}")
    //grpMemData["id"] = groupId
    //def grpMemJsonData = JsonOutput.toJson(grpMemData)
    //grpMemPost.entity = new StringEntity(grpMemJsonData)
    //println "Group Membership Payload: ${grpMemJsonData}"
    def grpMemResponse = httpClient.execute(grpMemDelete)
    println "Group Membership Response: ${grpMemResponse}"
    //def grpMemResponseData = new groovy.json.JsonSlurper().parseText(grpMemResponse.entity.content.text)
}

def getUserId(String userName){
    def userGet = null
    def userId = null
    def userUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users?filter=username%20eq%20%22${userName}%22"
    def access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
    CloseableHttpClient httpClient = HttpClients.createDefault();
    userGet = new HttpGet(userUrl)
    userGet.addHeader("Content-Type", "application/json")
    userGet.addHeader("Authorization", "Bearer ${access_token}")
    def response2 = httpClient.execute(userGet)
    def parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)
    parsedJson._embedded.users.each { user ->
        userId = user.id
    }
    return userId
}

def getCurrentGroupsforUser(String userId, String access_token){
    def userGet = null
    def currentGroups = []
    def userUrl ="https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/memberOfGroups?expand=group&filter=type%20eq%20%22DIRECT%22"
    CloseableHttpClient httpClient = HttpClients.createDefault();
    userGet = new HttpGet(userUrl)
    userGet.addHeader("Content-Type", "application/json")
    userGet.addHeader("Authorization", "Bearer ${access_token}")
    def response2 = httpClient.execute(userGet)
    def parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)
    println "Parsed Group Membership JSON:\n ${parsedJson}"
    if(parsedJson._embedded.groupMemberships != null){
        parsedJson._embedded.groupMemberships.each { membership ->
            currentGroups.add(membership._embedded.group.id)
        }
    }
    return currentGroups

}

def getCurrentRolesforUser(String userId, String access_token){
    def userGet = null
    def currentRoles = null
    def userUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/roleAssignments"
    CloseableHttpClient httpClient = HttpClients.createDefault();
    userGet = new HttpGet(userUrl)
    userGet.addHeader("Content-Type", "application/json")
    userGet.addHeader("Authorization", "Bearer ${access_token}")
    def response2 = httpClient.execute(userGet)
    def parsedJson = null
    parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)

    if(parsedJson._embedded.roleAssignments != null){
        parsedJson._embedded.roleAssignments.each { assignment ->
            //println "Role ID: ${assignment.role.id}"
            currentRoles.add(assignment.role.id)
        }
    }
    return currentRoles
}
def getAccessToken(String clientId,String clientSecret,String tokenUrl){
    CloseableHttpClient http =  HttpClients.createDefault();
    def post = new HttpPost(tokenUrl)
    post.addHeader("Content-Type", "application/x-www-form-urlencoded")
    post.entity = new UrlEncodedFormEntity([
            new BasicNameValuePair("grant_type", "client_credentials"),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", clientSecret)
    ] as List)
    def response = http.execute(post)
    def data = new groovy.json.JsonSlurper().parseText(response.entity.content.text)
    def access_token = data.access_token
    return access_token
}