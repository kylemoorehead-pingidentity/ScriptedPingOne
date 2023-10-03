@Grapes([
        @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1'),
        @Grab(group='org.slf4j', module='slf4j-api', version='1.6.1'),
        @Grab(group='ch.qos.logback', module='logback-classic', version='0.9.28'),
        @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.13')
])
import groovy.json.JsonOutput
import groovy.transform.Field
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
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
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def id = id as String
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

println "Operation: ${operation}"


assert operation == OperationType.CREATE, 'Operation must be a CREATE'
// We only deal with users
assert objectClass.getObjectClassValue() == ObjectClass.ACCOUNT_NAME
// Password must be defined for create
def password = "Welcome1234!"
//= createAttributes.getPassword() as GuardedString;
//assert password != null, 'Password must be provided on CREATE'

def userName = null
def firstName = null
def lastName = null
def email = null
def environmentid = null
def populationid = null
def accountStatus = null
def lifecycleStatus = null
def groupMemberships = []
def roleAssignments = []
def enabled = null

userName = id
// Initialize the main map to store the data
def dataMap = [:]

// Initialize sub-maps for the name and population attributes
def nameMap = [:]
def populationMap = [:]
def accountMap = [:]
def identityProviderMap = [:]
def lifecycleMap = [:]

dataMap["username"] = userName

if (createAttributes.hasAttribute("givenName")) {
    firstName = createAttributes.findString("givenName")
}
if (createAttributes.hasAttribute("sn")) {
    lastName = createAttributes.findString("sn")
}
nameMap["given"] = firstName
nameMap["family"] = lastName

dataMap["name"] = nameMap

if (createAttributes.hasAttribute("email")) {
    email = createAttributes.findString("email")
}
dataMap["email"] = email
if (createAttributes.hasAttribute("environmentid")) {
    environmentid = createAttributes.findString("environmentid")
}

if (createAttributes.hasAttribute("populationid")) {
    populationid = createAttributes.findString("populationid")
}
populationMap["id"] = populationid
dataMap["population"] = populationMap

if (createAttributes.hasAttribute("accountStatus")) {
    accountStatus = createAttributes.findString("accountStatus")
}

if (createAttributes.hasAttribute("canAuthenticate")) {
    canAuthenticate = createAttributes.findString("canAuthenticate")
    accountMap["canAuthenticate"] = canAuthenticate
}

accountMap["status"] = accountStatus
dataMap["account"] = accountMap

if (createAttributes.hasAttribute("enabled")) {
    enabled = createAttributes.findString("enabled")
    if(enabled == "true") {
        dataMap["enabled"] = true
    } else {
        dataMap["enabled"] = false
    }
}


if (createAttributes.hasAttribute("identityProviderType")) {
    identityProviderType = createAttributes.findString("identityProviderType")
    identityProviderMap["type"] = identityProviderType
    dataMap["identityProvider"] = identityProviderMap
}


if (createAttributes.hasAttribute("lifecycleStatus")) {
    lifecycleStatus = createAttributes.findString("lifecycleStatus")
    lifecycleMap["status"] = lifecycleStatus
    dataMap["lifecycle"] = lifecycleMap
}

if (createAttributes.hasAttribute("mfaEnabled")) {
    mfaEnabled = createAttributes.findString("mfaEnabled")
    if(mfaEnabled == "true") {
        dataMap["mfaEnabled"] = true
    } else {
        dataMap["mfaEnabled"] = false
    }
}

// Serialize to JSON
def jsonData = JsonOutput.toJson(dataMap)

// Print the JSON data
println "Payload: ${jsonData}"

def access_token = null
access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
CloseableHttpClient httpClient = HttpClients.createDefault();
def userPost = new HttpPost(USER_URL)
userPost.addHeader("Content-Type", "application/json")
userPost.addHeader("Authorization", "Bearer ${access_token}")
userPost.entity = new StringEntity(jsonData)
def userResponse = httpClient.execute(userPost)
def userResponseData = new groovy.json.JsonSlurper().parseText(userResponse.entity.content.text)
def userId = null
userId = userResponseData.id
println userId

if(userId){
    def passwordURL = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/password"
    def passwordPut = new HttpPut(passwordURL)
    passwordPut.addHeader("Content-Type", "application/vnd.pingidentity.password.set+json")
    passwordPut.addHeader("Authorization", "Bearer ${access_token}")
    def passwordData = [:]
    passwordData["value"] = password
    passwordData["forceChange"] = true
    def passwordJsonData = JsonOutput.toJson(passwordData)

    passwordPut.entity = new StringEntity(passwordJsonData)

    def passwordResponse = httpClient.execute(passwordPut)
    def passwordResponseData = new groovy.json.JsonSlurper().parseText(passwordResponse.entity.content.text)

    if (createAttributes.hasAttribute("groupMemberships")) {
        def grpMemUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/memberOfGroups"
        def grpMemPost = null
        grpMemPost = new HttpPost(grpMemUrl)
        groupMemberships = createAttributes.findStringList("groupMemberships")
        if(groupMemberships.size() > 0){
            groupMemberships.each { groupID ->
                grpMemPost.addHeader("Content-Type", "application/json")
                grpMemPost.addHeader("Authorization", "Bearer ${access_token}")
                def grpMemData = [:]
                grpMemData["id"] = groupID
                def grpMemJsonData = JsonOutput.toJson(grpMemData)
                grpMemPost.entity = new StringEntity(grpMemJsonData)
                println "Group Membership Payload: ${grpMemJsonData}"
                def grpMemResponse = httpClient.execute(grpMemPost)
                def grpMemResponseData = new groovy.json.JsonSlurper().parseText(grpMemResponse.entity.content.text)
            }
        }
    }

    if (createAttributes.hasAttribute("roleAssignments")) {
        roleAssignments = createAttributes.findStringList("roleAssignments")
        def roleAssignMap = [:]
        def scopeMap = [:]
        def roleAssignmentMap = [:]
        def roleAssignUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${userId}/roleAssignments"
        def roleAssignPost = new HttpPost(roleAssignUrl)
        if(roleAssignments.size() > 0){
            def roleMap = [:]
            roleAssignments.each {roleId ->
                roleAssignMap["id"] = roleId
                roleMap["role"] = roleAssignMap
                scopeMap["id"] = ENV_ID
                scopeMap["type"] = "ENVIRONMENT"
                roleMap["scope"] = scopeMap
                roleAssignPost.addHeader("Content-Type", "application/json")
                roleAssignPost.addHeader("Authorization", "Bearer ${access_token}")
                def roleAssignJsonData = JsonOutput.toJson(roleMap)
                roleAssignPost.entity = new StringEntity(roleAssignJsonData)
                println "Role Assignment Payload: ${roleAssignJsonData}"
                def roleAssignResponse = httpClient.execute(roleAssignPost)
                def roleAssignResponseData = new groovy.json.JsonSlurper().parseText(roleAssignResponse.entity.content.text)
            }
        }
    }
}

return new Uid(userId)

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